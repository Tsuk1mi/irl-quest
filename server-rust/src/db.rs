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
                    warn!("Failed to connect to database after {} attempts", max_retries);
                    return Err(e.into());
                }
                warn!("Failed to connect to database: {}. Retrying in {:?}...", e, retry_delay);
                sleep(retry_delay).await;
            }
        }
    }
}

async fn run_migrations(pool: &PgPool) -> Result<()> {
    // Enable extensions if available (ignore errors where not supported)
    let _ = sqlx::query("CREATE EXTENSION IF NOT EXISTS vector").execute(pool).await;

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
            avatar_url TEXT,
            bio TEXT,
            timezone VARCHAR(50) DEFAULT 'UTC',
            last_login TIMESTAMPTZ,
            settings JSONB DEFAULT '{}'::jsonb,
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

    Ok(())
}

pub async fn check_health(pool: &PgPool) -> Result<()> {
    let row = sqlx::query("SELECT 1 as result")
        .fetch_one(pool)
        .await?;

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
    use argon2::{Argon2};
    use argon2::password_hash::{SaltString, PasswordHasher};
    use argon2::password_hash::rand_core::OsRng;

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
    let password_hash = Argon2::default()
        .hash_password("password".as_bytes(), &salt)
        .map_err(|e| anyhow::anyhow!("Failed to hash seed password: {}", e))?
        .to_string();

    // Insert test user
    sqlx::query(
        r#"
        INSERT INTO users (username, email, hashed_password, is_active, level, experience, timezone, settings, created_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
        "#,
    )
    .bind("testuser")
    .bind("testuser@example.com")
    .bind(password_hash)
    .bind(true)
    .bind(1)
    .bind(0)
    .bind("UTC")
    .bind(serde_json::json!({}))
    .bind(chrono::Utc::now())
    .execute(pool)
    .await?;

    tracing::info!("Seeded test user 'testuser'");
    Ok(())
}
