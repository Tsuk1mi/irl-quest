use axum::{
    extract::{Path, State},
    Json,
};
use serde::{Deserialize, Serialize};
use std::time::Duration;

use crate::{error::AppError, state::AppState, validation};

#[derive(Serialize, Deserialize)]
pub struct CreateQuestRequest {
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub priority: Option<String>,
    pub deadline: Option<chrono::DateTime<chrono::Utc>>,
    pub tags: Option<Vec<String>>,
    pub is_public: Option<bool>,
    pub location_name: Option<String>,
    pub quest_type: Option<String>,
}

#[derive(Serialize)]
pub struct Quest {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub status: String,
    pub priority: String,
    pub deadline: Option<chrono::DateTime<chrono::Utc>>,
    pub completion_percentage: i32,
    pub tags: Vec<String>,
    pub is_public: bool,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub user_id: i32,
}

pub async fn create_quest(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
    Json(quest): Json<CreateQuestRequest>,
) -> Result<Json<Quest>, AppError> {
    // Валидация данных
    validation::validate_quest_title(&quest.title)?;
    validation::validate_difficulty(quest.difficulty)?;
    if let Some(ref priority) = quest.priority {
        validation::validate_priority(priority)?;
    }

    let result = sqlx::query_as!(
        Quest,
        r#"
        INSERT INTO quests (
            title, description, difficulty, priority, deadline,
            tags, is_public, location_name, quest_type, user_id
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        RETURNING id, title, description, difficulty, status, priority,
                  deadline, completion_percentage, tags, is_public, created_at, user_id
        "#,
        quest.title,
        quest.description,
        quest.difficulty,
        quest.priority.unwrap_or_else(|| "medium".to_string()),
        quest.deadline,
        quest.tags.unwrap_or_default(),
        quest.is_public.unwrap_or(false),
        quest.location_name,
        quest.quest_type.unwrap_or_else(|| "personal".to_string()),
        *user_id
    )
    .fetch_one(&state.db)
    .await?;

    // Инвалидируем кеш списка квестов пользователя
    let cache_key = format!("quests:user:{}", user_id);
    let _ = state.cache.delete(&cache_key).await;

    Ok(Json(result))
}

pub async fn get_quest(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
    Path(quest_id): Path<i32>,
) -> Result<Json<Quest>, AppError> {
    // Пробуем получить квест из кеша
    let cache_key = format!("quest:{}:{}", quest_id, user_id);
    if let Ok(Some(cached_quest)) = state.cache.get(&cache_key).await {
        return Ok(Json(cached_quest));
    }

    // Если в кеше нет, получаем из БД
    let quest = sqlx::query_as!(
        Quest,
        r#"
        SELECT id, title, description, difficulty, status, priority,
               deadline, completion_percentage, tags, is_public, created_at, user_id
        FROM quests
        WHERE id = $1 AND (user_id = $2 OR is_public = true)
        "#,
        quest_id,
        *user_id
    )
    .fetch_optional(&state.db)
    .await?
    .ok_or_else(|| AppError::NotFound("Quest not found or access denied".to_string()))?;

    // Кешируем результат на 5 минут
    let _ = state.cache.set(&cache_key, &quest, Duration::from_secs(300)).await;

    Ok(Json(quest))
}

pub async fn list_quests(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
) -> Result<Json<Vec<Quest>>, AppError> {
    // Пробуем получить список из кеша
    let cache_key = format!("quests:user:{}", user_id);
    if let Ok(Some(cached_quests)) = state.cache.get(&cache_key).await {
        return Ok(Json(cached_quests));
    }

    let quests = sqlx::query_as!(
        Quest,
        r#"
        SELECT id, title, description, difficulty, status, priority,
               deadline, completion_percentage, tags, is_public, created_at, user_id
        FROM quests
        WHERE user_id = $1 OR is_public = true
        ORDER BY created_at DESC
        "#,
        *user_id
    )
    .fetch_all(&state.db)
    .await?;

    // Кешируем результат на 1 минуту
    let _ = state.cache.set(&cache_key, &quests, Duration::from_secs(60)).await;

    Ok(Json(quests))
}
