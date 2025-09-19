use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{TaskCreate, TaskOut, TaskUpdate},
    AppState,
};

pub async fn list_tasks(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
) -> Result<Json<Vec<TaskOut>>, StatusCode> {
    // Placeholder - implement actual task listing
    Ok(Json(vec![]))
}

pub async fn create_task(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    ExtractJson(_task_create): ExtractJson<TaskCreate>,
) -> Result<(StatusCode, Json<TaskOut>), StatusCode> {
    // Placeholder - implement actual task creation
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn get_task(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_task_id): Path<i32>,
) -> Result<Json<TaskOut>, StatusCode> {
    // Placeholder - implement actual task retrieval
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn update_task(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_task_id): Path<i32>,
    ExtractJson(_task_update): ExtractJson<TaskUpdate>,
) -> Result<Json<TaskOut>, StatusCode> {
    // Placeholder - implement actual task update
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn delete_task(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_task_id): Path<i32>,
) -> Result<StatusCode, StatusCode> {
    // Placeholder - implement actual task deletion
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn complete_task(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    Path(_task_id): Path<i32>,
) -> Result<Json<TaskOut>, StatusCode> {
    // Placeholder - implement actual task completion
    Err(StatusCode::NOT_IMPLEMENTED)
}