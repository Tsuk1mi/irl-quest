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
    models::{TaskCreate, TaskOut, TaskUpdate},
    services::TaskService,
    AppState,
};

#[derive(Deserialize)]
pub struct ListQuery {
    skip: Option<i64>,
    limit: Option<i64>,
}

pub async fn list_tasks(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Query(params): Query<ListQuery>,
) -> Result<Json<Vec<TaskOut>>, StatusCode> {
    let skip = params.skip.unwrap_or(0);
    let limit = params.limit.unwrap_or(100);

    match TaskService::list_tasks_for_user(&state.db, user.id, skip, limit).await {
        Ok(tasks) => Ok(Json(tasks)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn create_task(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    ExtractJson(task_create): ExtractJson<TaskCreate>,
) -> Result<(StatusCode, Json<TaskOut>), StatusCode> {
    match TaskService::create_task_for_user(&state.db, user.id, task_create).await {
        Ok(task) => Ok((StatusCode::CREATED, Json(task))),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_task(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
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
    CurrentUser(user): CurrentUser,
    Path(task_id): Path<i32>,
    ExtractJson(task_update): ExtractJson<TaskUpdate>,
) -> Result<Json<TaskOut>, StatusCode> {
    match TaskService::update_task_for_user(&state.db, user.id, task_id, task_update).await {
        Ok(Some(task)) => Ok(Json(task)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn delete_task(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Path(task_id): Path<i32>,
) -> Result<StatusCode, StatusCode> {
    match TaskService::delete_task_for_user(&state.db, user.id, task_id).await {
        Ok(true) => Ok(StatusCode::NO_CONTENT),
        Ok(false) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}