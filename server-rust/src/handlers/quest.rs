use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use serde::Deserialize;
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{QuestCreate, QuestOut, QuestUpdate},
    services::QuestService,
    AppState,
};

#[derive(Deserialize)]
pub struct ListQuery {
    skip: Option<i64>,
    limit: Option<i64>,
}

pub async fn list_quests(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Query(params): Query<ListQuery>,
) -> Result<Json<Vec<QuestOut>>, StatusCode> {
    let skip = params.skip.unwrap_or(0);
    let limit = params.limit.unwrap_or(100);

    match QuestService::list_quests_for_user(&state.db, user.id, skip, limit).await {
        Ok(quests) => Ok(Json(quests)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn create_quest(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    ExtractJson(quest_create): ExtractJson<QuestCreate>,
) -> Result<(StatusCode, Json<QuestOut>), StatusCode> {
    match QuestService::create_quest_for_user(&state.db, user.id, quest_create).await {
        Ok(quest) => Ok((StatusCode::CREATED, Json(quest))),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_quest(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
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
    CurrentUser(user): CurrentUser,
    Path(quest_id): Path<i32>,
    ExtractJson(quest_update): ExtractJson<QuestUpdate>,
) -> Result<Json<QuestOut>, StatusCode> {
    match QuestService::update_quest_for_user(&state.db, user.id, quest_id, quest_update).await {
        Ok(Some(quest)) => Ok(Json(quest)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn delete_quest(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Path(quest_id): Path<i32>,
) -> Result<StatusCode, StatusCode> {
    match QuestService::delete_quest_for_user(&state.db, user.id, quest_id).await {
        Ok(true) => Ok(StatusCode::NO_CONTENT),
        Ok(false) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}