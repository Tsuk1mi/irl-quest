use axum::{
    extract::{Query, State, Extension},
    Json,
};
use serde::Deserialize;
use sqlx::Row;
use crate::{
    models::search::*,
    state::AppState,
    error::AppError,
};

#[derive(Debug, Deserialize)]
pub struct SearchQuery {
    pub q: String,
    pub tags: Option<Vec<String>>,
    pub limit: Option<i32>,
}

pub async fn search(
    State(state): State<AppState>,
    Extension(user_id): Extension<i32>,
    Query(query): Query<SearchQuery>,
) -> Result<Json<SearchResults>, AppError> {
    let limit = query.limit.unwrap_or(20).min(100);
    let tags = query.tags.clone();

    // Поиск квестов
    let quests = sqlx::query_as::<_, QuestSearchResult>(
        r#"
        SELECT id, title, description, status, created_at
        FROM quests
        WHERE owner_id = $1
        AND (
            title ILIKE $2
            OR description ILIKE $2
            OR $2 = ''
        )
        AND (
            CASE WHEN $3::text[] IS NULL THEN true
            ELSE tags && $3::text[] END
        )
        ORDER BY created_at DESC
        LIMIT $4
        "#,
    )
    .bind(user_id)
    .bind(format!("%{}%", query.q))
    .bind(tags.clone())
    .bind(limit as i64)
    .fetch_all(&state.db)
    .await?;

    // Поиск задач
    let tasks = sqlx::query_as::<_, TaskSearchResult>(
        r#"
        SELECT id, title, description, status, created_at
        FROM tasks
        WHERE owner_id = $1
        AND (
            title ILIKE $2
            OR description ILIKE $2
            OR $2 = ''
        )
        AND (
            CASE WHEN $3::text[] IS NULL THEN true
            ELSE tags && $3::text[] END
        )
        ORDER BY created_at DESC
        LIMIT $4
        "#,
    )
    .bind(user_id)
    .bind(format!("%{}%", query.q))
    .bind(tags)
    .bind(limit as i64)
    .fetch_all(&state.db)
    .await?;

    Ok(Json(SearchResults { quests, tasks }))
}

pub async fn get_tags(
    State(state): State<AppState>,
    Extension(user_id): Extension<i32>,
) -> Result<Json<Vec<TagCount>>, AppError> {
    let tags = sqlx::query_as::<_, TagCount>(
        r#"
        WITH combined_tags AS (
            SELECT unnest(tags) as tag FROM quests WHERE owner_id = $1
            UNION ALL
            SELECT unnest(tags) as tag FROM tasks WHERE owner_id = $1
        )
        SELECT
            tag as tag,
            COUNT(*) as count
        FROM combined_tags
        WHERE tag IS NOT NULL
        GROUP BY tag
        ORDER BY count DESC, tag
        LIMIT 50
        "#,
    )
    .bind(user_id)
    .fetch_all(&state.db)
    .await?;

    Ok(Json(tags))
}
