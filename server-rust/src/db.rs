use anyhow::Result;
use sqlx::postgres::PgPoolOptions;
use sqlx::PgPool;
use sqlx::Row;
use std::time::Duration;
use tokio::time::sleep;
use tracing::{info, warn};

pub async fn create_database_pool(database_url: &str) -> Result<PgPool> {
    // Увеличил число попыток и временные интервалы, чтобы надёжнее подключаться к БД при старте
    let max_retries = 10;
    let mut attempt = 0;
    let retry_delay = Duration::from_secs(5);

    loop {
        attempt += 1;
        info!(attempt, max_retries, "Attempting to connect to database");

        let pool_result = PgPoolOptions::new()
            .max_connections(10)
            // Увеличил таймаут ожидания соединения из пула
            .acquire_timeout(Duration::from_secs(30))
            .connect(database_url)
            .await;

        match pool_result {
            Ok(pool) => {
                info!("Successfully created database pool, running migrations and health check");

                // Попытка прогнать миграции и проверить здоровье перед возвратом пула
                if let Err(e) = run_migrations(&pool).await {
                    warn!("Failed to run migrations: {:#?}", e);
                    if attempt >= max_retries {
                        return Err(e);
                    }
                    warn!("Retrying after migration failure in {:?}...", retry_delay);
                    sleep(retry_delay).await;
                    continue;
                }

                if let Err(e) = check_health(&pool).await {
                    warn!("Database health check failed: {:#?}", e);
                    if attempt >= max_retries {
                        return Err(e);
                    }
                    warn!("Retrying after failed health check in {:?}...", retry_delay);
                    sleep(retry_delay).await;
                    continue;
                }

                info!("Database pool is healthy and ready");
                return Ok(pool);
            }
            Err(e) => {
                if attempt >= max_retries {
                    warn!(
                        "Failed to connect to database after {} attempts",
                        max_retries
                    );
                    return Err(e.into());
                }
                warn!(
                    "Failed to connect to database: {}. Retrying in {:?}...",
                    e, retry_delay
                );
                sleep(retry_delay).await;
            }
        }
    }
}

async fn run_migrations(pool: &PgPool) -> Result<()> {
    // Enable extensions if available (ignore errors where not supported)
    let _ = sqlx::query("CREATE EXTENSION IF NOT EXISTS vector")
        .execute(pool)
        .await;

    // Users table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            email VARCHAR(255) NOT NULL UNIQUE,
            username VARCHAR(50) NOT NULL UNIQUE,
            hashed_password TEXT NOT NULL,
            is_active BOOLEAN NOT NULL DEFAULT TRUE,
            level INTEGER DEFAULT 1,
            experience INTEGER DEFAULT 0,
            gold INTEGER DEFAULT 100,
            avatar_url TEXT,
            bio TEXT,
            timezone VARCHAR(50) DEFAULT 'UTC',
            last_login TIMESTAMPTZ,
            settings JSONB DEFAULT '{}'::jsonb,
            strength INTEGER DEFAULT 10,
            intelligence INTEGER DEFAULT 10,
            charisma INTEGER DEFAULT 10,
            dexterity INTEGER DEFAULT 10,
            constitution INTEGER DEFAULT 10,
            wisdom INTEGER DEFAULT 10,
            character_class VARCHAR(50) DEFAULT 'warrior',
            character_race VARCHAR(50) DEFAULT 'human',
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
        "#,
    )
    .execute(pool)
    .await?;

    // Quests table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS quests (
            id SERIAL PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            description TEXT,
            difficulty INTEGER NOT NULL DEFAULT 1,
            status VARCHAR(20) DEFAULT 'active',
            priority VARCHAR(20) DEFAULT 'medium',
            deadline TIMESTAMPTZ,
            completion_percentage INTEGER DEFAULT 0,
            reward_experience INTEGER DEFAULT 0,
            reward_description TEXT,
            tags TEXT[] DEFAULT '{}',
            is_public BOOLEAN DEFAULT FALSE,
            location_name TEXT,
            quest_type VARCHAR(50) DEFAULT 'personal',
            metadata JSONB DEFAULT '{}'::jsonb,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE
        )
        "#,
    )
    .execute(pool)
    .await?;

    // Tasks table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS tasks (
            id SERIAL PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            description TEXT,
            completed BOOLEAN NOT NULL DEFAULT FALSE,
            status VARCHAR(20) DEFAULT 'pending',
            priority VARCHAR(20) DEFAULT 'medium',
            deadline TIMESTAMPTZ,
            estimated_duration INTEGER,
            actual_duration INTEGER,
            difficulty INTEGER DEFAULT 1,
            experience_reward INTEGER DEFAULT 0,
            tags TEXT[] DEFAULT '{}',
            location_name TEXT,
            subtasks JSONB DEFAULT '[]'::jsonb,
            notes TEXT,
            attachments TEXT[] DEFAULT '{}',
            completion_proof TEXT,
            metadata JSONB DEFAULT '{}'::jsonb,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            quest_id INTEGER REFERENCES quests(id) ON DELETE SET NULL,
            owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE
        )
        "#,
    )
    .execute(pool)
    .await?;

    // Ensure required columns exist on older databases
    sqlx::query(
        r#"
        ALTER TABLE tasks 
            ADD COLUMN IF NOT EXISTS quest_id INTEGER REFERENCES quests(id) ON DELETE SET NULL,
            ADD COLUMN IF NOT EXISTS owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE
        "#,
    )
    .execute(pool)
    .await?;

    // Update users table to add new D&D columns if they don't exist
    // PostgreSQL doesn't support multiple ADD COLUMN IF NOT EXISTS in one statement
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS gold INTEGER DEFAULT 100")
        .execute(pool)
        .await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS strength INTEGER DEFAULT 10")
        .execute(pool)
        .await;
    let _ =
        sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS intelligence INTEGER DEFAULT 10")
            .execute(pool)
            .await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS charisma INTEGER DEFAULT 10")
        .execute(pool)
        .await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS dexterity INTEGER DEFAULT 10")
        .execute(pool)
        .await;
    let _ =
        sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS constitution INTEGER DEFAULT 10")
            .execute(pool)
            .await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS wisdom INTEGER DEFAULT 10")
        .execute(pool)
        .await;
    let _ = sqlx::query(
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS character_class VARCHAR(50) DEFAULT 'warrior'",
    )
    .execute(pool)
    .await;
    let _ = sqlx::query(
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS character_race VARCHAR(50) DEFAULT 'human'",
    )
    .execute(pool)
    .await;

    // User achievements table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS user_achievements (
            id SERIAL PRIMARY KEY,
            user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            achievement_type VARCHAR(100) NOT NULL,
            achievement_data JSONB DEFAULT '{}'::jsonb,
            earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            UNIQUE(user_id, achievement_type)
        )
        "#,
    )
    .execute(pool)
    .await?;

    // RAG knowledge base table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS rag_knowledge (
            id SERIAL PRIMARY KEY,
            content TEXT NOT NULL,
            content_type VARCHAR(50) NOT NULL,
            tags TEXT[] DEFAULT '{}',
            embedding REAL[],
            metadata JSONB DEFAULT '{}'::jsonb,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
        "#,
    )
    .execute(pool)
    .await?;

    // Create indices
    sqlx::query("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)")
        .execute(pool)
        .await?;
    sqlx::query("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)")
        .execute(pool)
        .await?;
    sqlx::query("CREATE INDEX IF NOT EXISTS idx_quests_owner ON quests(owner_id)")
        .execute(pool)
        .await?;
    sqlx::query("CREATE INDEX IF NOT EXISTS idx_tasks_owner ON tasks(owner_id)")
        .execute(pool)
        .await?;
    sqlx::query("CREATE INDEX IF NOT EXISTS idx_tasks_quest ON tasks(quest_id)")
        .execute(pool)
        .await?;

    // MFA tables (from migration 007_auth_extensions.sql)
    // Refresh tokens table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS refresh_tokens (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL,
            token TEXT NOT NULL UNIQUE,
            expires_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            revoked BOOLEAN NOT NULL DEFAULT FALSE,
            device_info TEXT,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        "#,
    )
    .execute(pool)
    .await?;

    sqlx::query("CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id)")
        .execute(pool)
        .await?;
    sqlx::query("CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token)")
        .execute(pool)
        .await?;
    sqlx::query(
        "CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at)",
    )
    .execute(pool)
    .await?;

    // MFA secrets table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS mfa_secrets (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL UNIQUE,
            secret TEXT NOT NULL,
            enabled BOOLEAN NOT NULL DEFAULT FALSE,
            backup_codes TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        "#,
    )
    .execute(pool)
    .await?;

    sqlx::query("CREATE INDEX IF NOT EXISTS idx_mfa_secrets_user_id ON mfa_secrets(user_id)")
        .execute(pool)
        .await?;

    // MFA sessions table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS mfa_sessions (
            id SERIAL PRIMARY KEY,
            session_id TEXT NOT NULL UNIQUE,
            user_id INTEGER NOT NULL,
            expires_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        "#,
    )
    .execute(pool)
    .await?;

    sqlx::query(
        "CREATE INDEX IF NOT EXISTS idx_mfa_sessions_session_id ON mfa_sessions(session_id)",
    )
    .execute(pool)
    .await?;
    sqlx::query(
        "CREATE INDEX IF NOT EXISTS idx_mfa_sessions_expires_at ON mfa_sessions(expires_at)",
    )
    .execute(pool)
    .await?;

    // Login attempts table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS login_attempts (
            id SERIAL PRIMARY KEY,
            user_id INTEGER,
            username TEXT,
            success BOOLEAN NOT NULL,
            ip_address TEXT,
            user_agent TEXT,
            failure_reason TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        "#,
    )
    .execute(pool)
    .await?;

    sqlx::query("CREATE INDEX IF NOT EXISTS idx_login_attempts_user_id ON login_attempts(user_id)")
        .execute(pool)
        .await?;
    sqlx::query(
        "CREATE INDEX IF NOT EXISTS idx_login_attempts_created_at ON login_attempts(created_at)",
    )
    .execute(pool)
    .await?;

    // Add optional columns to users table for MFA
    let _ = sqlx::query(
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE",
    )
    .execute(pool)
    .await;
    let _ =
        sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN DEFAULT FALSE")
            .execute(pool)
            .await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP")
        .execute(pool)
        .await;
    let _ = sqlx::query(
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0",
    )
    .execute(pool)
    .await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP")
        .execute(pool)
        .await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP")
        .execute(pool)
        .await;

    // Add race column to users table if not exists
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN IF NOT EXISTS race TEXT DEFAULT 'human'")
        .execute(pool)
        .await;

    // Character system tables
    // User stat points table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS user_stat_points (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL UNIQUE,
            available_points INTEGER NOT NULL DEFAULT 0,
            total_earned INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        "#,
    )
    .execute(pool)
    .await?;

    sqlx::query(
        "CREATE INDEX IF NOT EXISTS idx_user_stat_points_user_id ON user_stat_points(user_id)",
    )
    .execute(pool)
    .await?;

    // Stat increase history table
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS stat_increase_history (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL,
            stat_name TEXT NOT NULL,
            old_value INTEGER NOT NULL,
            new_value INTEGER NOT NULL,
            source TEXT NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
        "#,
    )
    .execute(pool)
    .await?;

    sqlx::query("CREATE INDEX IF NOT EXISTS idx_stat_increase_history_user_id ON stat_increase_history(user_id)")
        .execute(pool)
        .await?;

    Ok(())
}

pub async fn check_health(pool: &PgPool) -> Result<()> {
    let row = sqlx::query("SELECT 1 as result").fetch_one(pool).await?;

    let result: i32 = row.try_get("result")?;
    if result == 1 {
        Ok(())
    } else {
        Err(anyhow::anyhow!("Database health check failed"))
    }
}

/// Seed a test user if it doesn't exist. Uses Argon2 hashing to be compatible with handlers.
pub async fn seed_test_user(pool: &PgPool) -> Result<()> {
    // Local imports so we don't change global file imports
    use argon2::{Argon2, PasswordHasher};
    use password_hash::{rand_core::OsRng, SaltString};

    // Check if test user exists
    let existing: Option<(i32,)> = sqlx::query_as("SELECT id FROM users WHERE username = $1")
        .bind("testuser")
        .fetch_optional(pool)
        .await?;

    if existing.is_some() {
        tracing::info!("Test user already exists");
        return Ok(());
    }

    // Create password hash for password = "password"
    let salt = SaltString::generate(&mut OsRng);
    let hashed_password = Argon2::default()
        .hash_password("password".as_bytes(), &salt)
        .map_err(|e| anyhow::anyhow!("Failed to hash seed password: {}", e))?
        .to_string();

    // Insert test user
    sqlx::query(
        r#"
        INSERT INTO users (username, email, hashed_password, is_active, level, experience, gold, 
                          strength, intelligence, charisma, dexterity, constitution, wisdom,
                          character_class, character_race, timezone, settings, created_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18)
        "#,
    )
    .bind("testuser")
    .bind("testuser@example.com")
    .bind(hashed_password)
    .bind(true)
    .bind(1) // level
    .bind(0) // experience
    .bind(100) // gold
    .bind(12) // strength
    .bind(10) // intelligence
    .bind(8) // charisma
    .bind(10) // dexterity
    .bind(10) // constitution
    .bind(10) // wisdom
    .bind("warrior") // character_class
    .bind("human") // character_race
    .bind("UTC")
    .bind(serde_json::json!({}))
    .bind(chrono::Utc::now())
    .execute(pool)
    .await?;

    tracing::info!("Seeded test user 'testuser'");
    Ok(())
}
