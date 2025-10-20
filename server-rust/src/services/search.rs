use sqlx::PgPool;
use crate::error::AppError;
use sqlx::FromRow;

pub async fn search_quests_and_tasks(
    pool: &PgPool,
    owner_id: i32,
    query: &str,
    tags: Option<Vec<String>>,
) -> Result<SearchResults, AppError> {
    let mut quests = sqlx::query_as::<_, QuestSearchResult>(
        r#"
        SELECT id, title, description, difficulty, status,
               priority, deadline, completion_percentage
        FROM quests
        WHERE (owner_id = $1 OR is_public = true)
        AND (
            title ILIKE $2
            OR description ILIKE $2
            OR $2 = ANY(tags)
        )
        AND ($3::text[] IS NULL OR tags && $3)
        ORDER BY created_at DESC
        LIMIT 10
        "#,
    )
    .bind(owner_id)
    .bind(format!("%{}%", query))
    .bind(tags.clone())
    .fetch_all(pool)
    .await?;

    let mut tasks = sqlx::query_as::<_, TaskSearchResult>(
        r#"
        SELECT id, title, description, status,
               priority, deadline, completed
        FROM tasks
        WHERE owner_id = $1
        AND (
            title ILIKE $2
            OR description ILIKE $2
            OR $2 = ANY(tags)
        )
        AND ($3::text[] IS NULL OR tags && $3)
        ORDER BY created_at DESC
        LIMIT 10
        "#,
    )
    .bind(owner_id)
    .bind(format!("%{}%", query))
    .bind(tags)
    .fetch_all(pool)
    .await?;

    Ok(SearchResults { quests, tasks })
}

#[derive(serde::Serialize)]
pub struct SearchResults {
    quests: Vec<QuestSearchResult>,
    tasks: Vec<TaskSearchResult>,
}

#[derive(serde::Serialize, FromRow)]
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

#[derive(serde::Serialize, FromRow)]
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
    owner_id: i32,
) -> Result<Vec<TagCount>, AppError> {
    sqlx::query_as::<_, TagCount>(
        r#"
        WITH combined_tags AS (
            SELECT unnest(tags) as tag
            FROM quests
            WHERE owner_id = $1 OR is_public = true
            UNION ALL
            SELECT unnest(tags) as tag
            FROM tasks
            WHERE owner_id = $1
        )
        SELECT tag, COUNT(*) as count
        FROM combined_tags
        GROUP BY tag
        ORDER BY count DESC
        LIMIT 20
        "#,
    )
    .bind(owner_id)
    .fetch_all(pool)
    .await
    .map_err(AppError::from)
}

#[derive(serde::Serialize, FromRow)]
pub struct TagCount {
    pub tag: String,
    pub count: i64,
}
