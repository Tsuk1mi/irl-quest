use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{QuestCreate, QuestOut, QuestUpdate, TodoToQuestRequest},
    AppState,
};

pub async fn list_quests(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
) -> Result<Json<Vec<QuestOut>>, StatusCode> {
    // Placeholder - implement actual quest listing
    Ok(Json(vec![]))
}

pub async fn create_quest(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    ExtractJson(_quest_create): ExtractJson<QuestCreate>,
) -> Result<(StatusCode, Json<QuestOut>), StatusCode> {
    // Placeholder - implement actual quest creation
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn get_quest(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_quest_id): Path<i32>,
) -> Result<Json<QuestOut>, StatusCode> {
    // Placeholder - implement actual quest retrieval
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn update_quest(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_quest_id): Path<i32>,
    ExtractJson(_quest_update): ExtractJson<QuestUpdate>,
) -> Result<Json<QuestOut>, StatusCode> {
    // Placeholder - implement actual quest update
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn delete_quest(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_quest_id): Path<i32>,
) -> Result<StatusCode, StatusCode> {
    // Placeholder - implement actual quest deletion
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn complete_quest(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_quest_id): Path<i32>,
) -> Result<Json<QuestOut>, StatusCode> {
    // Placeholder - implement actual quest completion
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn generate_quest_from_todo(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    ExtractJson(_todo_request): ExtractJson<TodoToQuestRequest>,
) -> Result<Json<QuestOut>, StatusCode> {
    // Placeholder - implement actual quest generation from TODO
    Err(StatusCode::NOT_IMPLEMENTED)
}