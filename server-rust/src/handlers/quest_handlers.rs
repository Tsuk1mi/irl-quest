use axum::{
    extract::{Path, State, Extension},
    Json,
    http::StatusCode,
};
use crate::{
    models::quest::*,
    state::AppState,
    error::AppError,
    middleware::auth::CurrentUser,
};

pub async fn create_quest(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(quest_data): Json<QuestCreate>,
) -> Result<Json<Quest>, AppError> {
    let user = match current_user {
        Some(u) => u,
        None => return Err(AppError::Unauthorized("Not authenticated".to_string())),
    };
    let quest = quest_data.into_quest(user.0.id);

    let result = sqlx::query_as::<_, Quest>(
        r#"
        INSERT INTO quests (
            title, description, difficulty, status,
            priority, deadline, completion_percentage,
            reward_experience, reward_description,
            tags, is_public, location_name, quest_type,
            metadata, created_at, owner_id
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)
        RETURNING *
        "#,
    )
    .bind(quest.title)
    .bind(quest.description)
    .bind(quest.difficulty)
    .bind(quest.status)
    .bind(quest.priority)
    .bind(quest.deadline)
    .bind(quest.completion_percentage)
    .bind(quest.reward_experience)
    .bind(quest.reward_description)
    .bind(&quest.tags)
    .bind(quest.is_public)
    .bind(quest.location_name)
    .bind(quest.quest_type)
    .bind(quest.metadata)
    .bind(quest.created_at)
    .bind(quest.owner_id)
    .fetch_one(&state.db)
    .await?;

    Ok(Json(result))
}

pub async fn get_quest(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Path(quest_id): Path<i32>,
) -> Result<Json<Quest>, AppError> {
    let user = match current_user {
        Some(u) => u,
        None => return Err(AppError::Unauthorized("Not authenticated".to_string())),
    };
    let user_id = user.0.id;
    let quest = sqlx::query_as::<_, Quest>(
        r#"
        SELECT * FROM quests
        WHERE id = $1 AND owner_id = $2
        "#,
    )
    .bind(quest_id)
    .bind(user_id)
    .fetch_optional(&state.db)
    .await?
    .ok_or_else(|| AppError::NotFound("Quest not found".into()))?;

    Ok(Json(quest))
}

pub async fn list_quests(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<Vec<Quest>>, AppError> {
    let user = match current_user {
        Some(u) => u,
        None => return Err(AppError::Unauthorized("Not authenticated".to_string())),
    };
    let user_id = user.0.id;
    let quests = sqlx::query_as::<_, Quest>(
        r#"
        SELECT * FROM quests
        WHERE owner_id = $1
        ORDER BY created_at DESC
        "#,
    )
    .bind(user_id)
    .fetch_all(&state.db)
    .await?;

    Ok(Json(quests))
}

pub async fn update_quest(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Path(quest_id): Path<i32>,
    Json(update): Json<QuestUpdate>,
) -> Result<Json<Quest>, AppError> {
    let user = match current_user {
        Some(u) => u,
        None => return Err(AppError::Unauthorized("Not authenticated".to_string())),
    };
    let user_id = user.0.id;
    
    let mut quest = sqlx::query_as::<_, Quest>(
        r#"
        SELECT * FROM quests
        WHERE id = $1 AND owner_id = $2
        "#,
    )
    .bind(quest_id)
    .bind(user_id)
    .fetch_optional(&state.db)
    .await?
    .ok_or_else(|| AppError::NotFound("Quest not found".into()))?;

    quest.apply_update(update);

    let updated_quest = sqlx::query_as::<_, Quest>(
        r#"
        UPDATE quests
        SET title = $1, description = $2, difficulty = $3,
            status = $4, priority = $5, deadline = $6,
            completion_percentage = $7, reward_experience = $8,
            reward_description = $9, tags = $10, is_public = $11,
            location_name = $12, quest_type = $13, metadata = $14
        WHERE id = $15 AND owner_id = $16
        RETURNING *
        "#,
    )
    .bind(quest.title)
    .bind(quest.description)
    .bind(quest.difficulty)
    .bind(quest.status)
    .bind(quest.priority)
    .bind(quest.deadline)
    .bind(quest.completion_percentage)
    .bind(quest.reward_experience)
    .bind(quest.reward_description)
    .bind(&quest.tags)
    .bind(quest.is_public)
    .bind(quest.location_name)
    .bind(quest.quest_type)
    .bind(quest.metadata)
    .bind(quest.id)
    .bind(quest.owner_id)
    .fetch_one(&state.db)
    .await?;

    Ok(Json(updated_quest))
}
