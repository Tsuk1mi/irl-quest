use anyhow::Result;
use sqlx::{PgPool, Row};

pub async fn create_database_pool(database_url: &str) -> Result<PgPool> {
    let pool = PgPool::connect(database_url).await?;

    // Run basic migrations
    run_migrations(&pool).await?;

    Ok(pool)
}

async fn run_migrations(pool: &PgPool) -> Result<()> {
    // Enable extensions
    sqlx::query("CREATE EXTENSION IF NOT EXISTS vector")
        .execute(pool)
        .await?;

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
            embedding vector(384),
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