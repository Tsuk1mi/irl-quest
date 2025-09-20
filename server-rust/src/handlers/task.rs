use axum::{
    extract::{Path, State},
    Extension,
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use std::sync::Arc;

use crate::services::TaskService;

use crate::{
    middleware::CurrentUser,
    models::{TaskCreate, TaskOut, TaskUpdate},
    AppState,
};

pub async fn list_tasks(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
) -> Result<Json<Vec<TaskOut>>, StatusCode> {
    let result = TaskService::list_tasks_for_user(&state.db, user.id, 0, 100).await;
    match result {
        Ok(tasks) => Ok(Json(tasks)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn create_task(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    ExtractJson(task_create): ExtractJson<TaskCreate>,
) -> Result<(StatusCode, Json<TaskOut>), StatusCode> {
    match TaskService::create_task_for_user(&state.db, user.id, task_create).await {
        Ok(task) => Ok((StatusCode::CREATED, Json(task))),
        Err(_) => Err(StatusCode::BAD_REQUEST),
    }
}

pub async fn get_task(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    Path(task_id): Path<i32>,
) -> Result<Json<TaskOut>, StatusCode> {
    match TaskService::get_task_for_user(&state.db, user.id, task_id).await {
        Ok(Some(task)) => Ok(Json(task)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn update_task(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    Path(task_id): Path<i32>,
    ExtractJson(task_update): ExtractJson<TaskUpdate>,
) -> Result<Json<TaskOut>, StatusCode> {
    match TaskService::update_task_for_user(&state.db, user.id, task_id, task_update).await {
        Ok(Some(task)) => Ok(Json(task)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::BAD_REQUEST),
    }
}

pub async fn delete_task(
    State(state): State<Arc<AppState>>,
    Extension(CurrentUser(user)): Extension<crate::middleware::CurrentUser>,
    Path(task_id): Path<i32>,
) -> Result<StatusCode, StatusCode> {
    match TaskService::delete_task_for_user(&state.db, user.id, task_id).await {
        Ok(true) => Ok(StatusCode::NO_CONTENT),
        Ok(false) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn complete_task(
    State(_state): State<Arc<AppState>>,
    Extension(CurrentUser(_user)): Extension<crate::middleware::CurrentUser>,
    Path(_task_id): Path<i32>,
) -> Result<Json<TaskOut>, StatusCode> {
    // Placeholder - implement actual task completion
    Err(StatusCode::NOT_IMPLEMENTED)
}