use axum::{
    extract::{Path, State},
    Extension,
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use std::sync::Arc;

use crate::services::QuestService;

use crate::{
    middleware::CurrentUser,
    models::{QuestCreate, QuestOut, QuestUpdate, TodoToQuestRequest},
    AppState,
};

pub async fn list_quests(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
) -> Result<Json<Vec<QuestOut>>, StatusCode> {
    let result = QuestService::list_quests_for_user(&state.db, user.id, 0, 100).await;
    match result {
        Ok(quests) => Ok(Json(quests)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn create_quest(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    ExtractJson(quest_create): ExtractJson<QuestCreate>,
) -> Result<(StatusCode, Json<QuestOut>), StatusCode> {
    match QuestService::create_quest_for_user(&state.db, user.id, quest_create).await {
        Ok(quest) => Ok((StatusCode::CREATED, Json(quest))),
        Err(_) => Err(StatusCode::BAD_REQUEST),
    }
}

pub async fn get_quest(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    Path(quest_id): Path<i32>,
) -> Result<Json<QuestOut>, StatusCode> {
    match QuestService::get_quest_for_user(&state.db, user.id, quest_id).await {
        Ok(Some(quest)) => Ok(Json(quest)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn update_quest(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    Path(quest_id): Path<i32>,
    ExtractJson(quest_update): ExtractJson<QuestUpdate>,
) -> Result<Json<QuestOut>, StatusCode> {
    match QuestService::update_quest_for_user(&state.db, user.id, quest_id, quest_update).await {
        Ok(Some(quest)) => Ok(Json(quest)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::BAD_REQUEST),
    }
}

pub async fn delete_quest(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    Path(quest_id): Path<i32>,
) -> Result<StatusCode, StatusCode> {
    match QuestService::delete_quest_for_user(&state.db, user.id, quest_id).await {
        Ok(true) => Ok(StatusCode::NO_CONTENT),
        Ok(false) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn complete_quest(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    Path(quest_id): Path<i32>,
) -> Result<Json<QuestOut>, StatusCode> {
    match QuestService::complete_quest_for_user(&state.db, user.id, quest_id).await {
        Ok(Some(quest)) => Ok(Json(quest)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn generate_quest_from_todo(
    State(_state): State<Arc<AppState>>,
    Extension(CurrentUser(_user)): Extension<crate::middleware::CurrentUser>,
    ExtractJson(_todo_request): ExtractJson<TodoToQuestRequest>,
) -> Result<Json<QuestOut>, StatusCode> {
    // Placeholder - implement actual quest generation from TODO
    Err(StatusCode::NOT_IMPLEMENTED)
}