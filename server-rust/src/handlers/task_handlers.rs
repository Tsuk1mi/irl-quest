use axum::{extract::{Path, State, Extension},
    Json,
};
use crate::{
    models::task::*,
    state::AppState,
    error::AppError,
    middleware::auth::CurrentUser,
};

pub async fn create_task(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(task_data): Json<TaskCreate>,
) -> Result<Json<Task>, AppError> {
    let user = match current_user {
        Some(u) => u,
        None => return Err(AppError::Unauthorized("Not authenticated".to_string())),
    };
    let user_id = user.0.id;
    
    // Проверяем доступ к квесту, если он указан
    if let Some(quest_id) = task_data.quest_id {
        let quest_exists_row = sqlx::query(
            "SELECT id FROM quests WHERE id = $1 AND owner_id = $2",
        )
        .bind(quest_id)
        .bind(user_id)
        .fetch_optional(&state.db)
        .await?;

        if quest_exists_row.is_none() {
            return Err(AppError::NotFound("Quest not found or access denied".into()));
        }
    }

    let task = task_data.into_task(user_id);

    let result = sqlx::query_as::<_, Task>(
        r#"
        INSERT INTO tasks (
            title, description, completed, status,
            priority, deadline, estimated_duration,
            actual_duration, difficulty, experience_reward,
            tags, location_name, subtasks, notes,
            attachments, completion_proof, metadata,
            created_at, quest_id, owner_id
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20)
        RETURNING *
        "#,
    )
    .bind(task.title)
    .bind(task.description)
    .bind(task.completed)
    .bind(task.status)
    .bind(task.priority)
    .bind(task.deadline)
    .bind(task.estimated_duration)
    .bind(task.actual_duration)
    .bind(task.difficulty)
    .bind(task.experience_reward)
    .bind(&task.tags)
    .bind(task.location_name)
    .bind(task.subtasks)
    .bind(task.notes)
    .bind(&task.attachments)
    .bind(task.completion_proof)
    .bind(task.metadata)
    .bind(task.created_at)
    .bind(task.quest_id)
    .bind(task.owner_id)
    .fetch_one(&state.db)
    .await?;

    Ok(Json(result))
}

pub async fn list_tasks(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<Vec<Task>>, AppError> {
    let user = match current_user {
        Some(u) => u,
        None => return Err(AppError::Unauthorized("Not authenticated".to_string())),
    };
    let owner_id = user.0.id;
    let tasks = sqlx::query_as::<_, Task>(
        r#"
        SELECT * FROM tasks
        WHERE owner_id = $1
        ORDER BY created_at DESC
        "#,
    )
    .bind(owner_id)
    .fetch_all(&state.db)
    .await?;

    Ok(Json(tasks))
}

pub async fn complete_task(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Path(task_id): Path<i32>,
) -> Result<Json<Task>, AppError> {
    let user = match current_user {
        Some(u) => u,
        None => return Err(AppError::Unauthorized("Not authenticated".to_string())),
    };
    let owner_id = user.0.id;
    let task = sqlx::query_as::<_, Task>(
        r#"
        UPDATE tasks
        SET completed = true,
            status = 'completed',
            actual_duration = EXTRACT(EPOCH FROM NOW() - created_at)::INTEGER
        WHERE id = $1 AND owner_id = $2
        RETURNING *
        "#,
    )
    .bind(task_id)
    .bind(owner_id)
    .fetch_optional(&state.db)
    .await?
    .ok_or_else(|| AppError::NotFound("Task not found".into()))?;

    Ok(Json(task))
}
