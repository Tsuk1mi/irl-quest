use axum::{
    extract::{Path, State},
    Json,
};
use serde::{Deserialize, Serialize};
use std::time::Duration;
use crate::{state::AppState, error::AppError};

#[derive(Serialize, Deserialize)]
pub struct CreateTaskRequest {
    pub title: String,
    pub description: Option<String>,
    pub priority: Option<String>,
    pub deadline: Option<chrono::DateTime<chrono::Utc>>,
    pub estimated_duration: Option<i32>,
    pub difficulty: Option<i32>,
    pub tags: Option<Vec<String>>,
    pub location_name: Option<String>,
    pub quest_id: Option<i32>,
}

#[derive(Serialize)]
pub struct Task {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub status: String,
    pub priority: String,
    pub deadline: Option<chrono::DateTime<chrono::Utc>>,
    pub estimated_duration: Option<i32>,
    pub difficulty: i32,
    pub tags: Vec<String>,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub user_id: i32,
}

pub async fn create_task(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
    Json(task): Json<CreateTaskRequest>,
) -> Result<Json<Task>, AppError> {
    // Проверяем права доступа к квесту
    if let Some(quest_id) = task.quest_id {
        let quest_access = sqlx::query!(
            "SELECT id FROM quests WHERE id = $1 AND user_id = $2",
            quest_id,
            *user_id
        )
        .fetch_optional(&state.db)
        .await?;

        if quest_access.is_none() {
            return Err(AppError::Forbidden("No access to the specified quest".to_string()));
        }
    }

    let result = sqlx::query_as!(
        Task,
        r#"
        INSERT INTO tasks (
            title, description, priority, deadline, 
            estimated_duration, difficulty, tags, location_name, quest_id, user_id
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        RETURNING id, title, description, completed, status, priority,
                  deadline, estimated_duration, difficulty, tags, created_at, user_id
        "#,
        task.title,
        task.description,
        task.priority.unwrap_or_else(|| "medium".to_string()),
        task.deadline,
        task.estimated_duration,
        task.difficulty.unwrap_or(1),
        task.tags.unwrap_or_default(),
        task.location_name,
        task.quest_id,
        *user_id
    )
    .fetch_one(&state.db)
    .await?;

    // Инвалидируем кеш списка задач пользователя
    let cache_key = format!("tasks:user:{}", user_id);
    let _ = state.cache.delete(&cache_key).await;

    // Если задача привязана к квесту, инвалидируем кеш квеста
    if let Some(quest_id) = task.quest_id {
        let quest_cache_key = format!("quest:{}:{}", quest_id, user_id);
        let _ = state.cache.delete(&quest_cache_key).await;
    }

    Ok(Json(result))
}

pub async fn complete_task(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
    Path(task_id): Path<i32>,
) -> Result<Json<Task>, AppError> {
    // Получаем информацию о задаче для проверки прав доступа и связанного квеста
    let task_info = sqlx::query!(
        "SELECT quest_id FROM tasks WHERE id = $1 AND user_id = $2",
        task_id,
        *user_id
    )
    .fetch_optional(&state.db)
    .await?
    .ok_or_else(|| AppError::NotFound("Task not found".to_string()))?;

    let task = sqlx::query_as!(
        Task,
        r#"
        UPDATE tasks
        SET completed = true,
            status = 'completed'
        WHERE id = $1 AND user_id = $2
        RETURNING id, title, description, completed, status, priority,
                  deadline, estimated_duration, difficulty, tags, created_at, user_id
        "#,
        task_id,
        *user_id
    )
    .fetch_one(&state.db)
    .await?;

    // Инвалидируем кеши
    let tasks_cache_key = format!("tasks:user:{}", user_id);
    let _ = state.cache.delete(&tasks_cache_key).await;

    // Если задача была частью квеста, инвалидируем его кеш
    if let Some(quest_id) = task_info.quest_id {
        let quest_cache_key = format!("quest:{}:{}", quest_id, user_id);
        let _ = state.cache.delete(&quest_cache_key).await;
    }

    Ok(Json(task))
}

pub async fn list_tasks(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
) -> Result<Json<Vec<Task>>, AppError> {
    // Пробуем получить список из кеша
    let cache_key = format!("tasks:user:{}", user_id);
    if let Ok(Some(cached_tasks)) = state.cache.get(&cache_key).await {
        return Ok(Json(cached_tasks));
    }

    let tasks = sqlx::query_as!(
        Task,
        r#"
        SELECT id, title, description, completed, status, priority,
               deadline, estimated_duration, difficulty, tags, created_at, user_id
        FROM tasks
        WHERE user_id = $1
        ORDER BY created_at DESC
        "#,
        *user_id
    )
    .fetch_all(&state.db)
    .await?;

    // Кешируем результат на 1 минуту
    let _ = state.cache.set(&cache_key, &tasks, Duration::from_secs(60)).await;

    Ok(Json(tasks))
}
