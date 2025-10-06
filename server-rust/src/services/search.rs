use sqlx::PgPool;
use crate::error::AppError;

pub async fn search_quests_and_tasks(
    pool: &PgPool,
    user_id: i32,
    query: &str,
    tags: Option<Vec<String>>,
) -> Result<SearchResults, AppError> {
    let mut quests = sqlx::query_as!(
        QuestSearchResult,
        r#"
        SELECT id, title, description, difficulty, status,
               priority, deadline, completion_percentage
        FROM quests
        WHERE (user_id = $1 OR is_public = true)
        AND (
            title ILIKE $2
            OR description ILIKE $2
            OR $2 = ANY(tags)
        )
        AND ($3::text[] IS NULL OR tags && $3)
        ORDER BY created_at DESC
        LIMIT 10
        "#,
        user_id,
        format!("%{}%", query),
        tags.as_deref(),
    )
    .fetch_all(pool)
    .await?;

    let mut tasks = sqlx::query_as!(
        TaskSearchResult,
        r#"
        SELECT id, title, description, status,
               priority, deadline, completed
        FROM tasks
        WHERE user_id = $1
        AND (
            title ILIKE $2
            OR description ILIKE $2
            OR $2 = ANY(tags)
        )
        AND ($3::text[] IS NULL OR tags && $3)
        ORDER BY created_at DESC
        LIMIT 10
        "#,
        user_id,
        format!("%{}%", query),
        tags.as_deref(),
    )
    .fetch_all(pool)
    .await?;

    Ok(SearchResults { quests, tasks })
}

#[derive(serde::Serialize)]
pub struct SearchResults {
    quests: Vec<QuestSearchResult>,
    tasks: Vec<TaskSearchResult>,
}

#[derive(serde::Serialize)]
pub struct QuestSearchResult {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub status: String,
    pub priority: String,
    pub deadline: Option<chrono::DateTime<chrono::Utc>>,
    pub completion_percentage: i32,
}

#[derive(serde::Serialize)]
pub struct TaskSearchResult {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub status: String,
    pub priority: String,
    pub deadline: Option<chrono::DateTime<chrono::Utc>>,
    pub completed: bool,
}

pub async fn get_popular_tags(
    pool: &PgPool,
    user_id: i32,
) -> Result<Vec<TagCount>, AppError> {
    sqlx::query_as!(
        TagCount,
        r#"
        WITH combined_tags AS (
            SELECT unnest(tags) as tag
            FROM quests
            WHERE user_id = $1 OR is_public = true
            UNION ALL
            SELECT unnest(tags) as tag
            FROM tasks
            WHERE user_id = $1
        )
        SELECT tag, COUNT(*) as count
        FROM combined_tags
        GROUP BY tag
        ORDER BY count DESC
        LIMIT 20
        "#,
        user_id
    )
    .fetch_all(pool)
    .await
    .map_err(AppError::from)
}

#[derive(serde::Serialize)]
pub struct TagCount {
    pub tag: String,
    pub count: i64,
}
