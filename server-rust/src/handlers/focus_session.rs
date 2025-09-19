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
    models::{FocusSessionCreate, FocusSessionOut, FocusSessionUpdate},
    services::{FocusSessionService, StatsService},
    AppState,
};

#[derive(Debug, Deserialize)]
pub struct SessionQuery {
    limit: Option<i64>,
}

pub async fn create_focus_session(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    ExtractJson(session_create): ExtractJson<FocusSessionCreate>,
) -> Result<(StatusCode, Json<FocusSessionOut>), StatusCode> {
    match FocusSessionService::create_session(&state.db, user.id, session_create).await {
        Ok(session) => Ok((StatusCode::CREATED, Json(session))),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_focus_sessions(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Query(query): Query<SessionQuery>,
) -> Result<Json<Vec<FocusSessionOut>>, StatusCode> {
    match FocusSessionService::get_user_sessions(&state.db, user.id, query.limit).await {
        Ok(sessions) => Ok(Json(sessions)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_focus_session(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Path(session_id): Path<i32>,
) -> Result<Json<FocusSessionOut>, StatusCode> {
    match FocusSessionService::get_session(&state.db, user.id, session_id).await {
        Ok(Some(session)) => Ok(Json(session)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn update_focus_session(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Path(session_id): Path<i32>,
    ExtractJson(session_update): ExtractJson<FocusSessionUpdate>,
) -> Result<Json<FocusSessionOut>, StatusCode> {
    match FocusSessionService::update_session(&state.db, user.id, session_id, session_update).await {
        Ok(Some(session)) => Ok(Json(session)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

#[derive(Debug, Deserialize)]
pub struct EndSessionRequest {
    actual_duration: Option<i32>,
    productivity_rating: Option<i32>,
}

pub async fn end_focus_session(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Path(session_id): Path<i32>,
    ExtractJson(end_request): ExtractJson<EndSessionRequest>,
) -> Result<Json<FocusSessionOut>, StatusCode> {
    match FocusSessionService::end_session(
        &state.db,
        user.id,
        session_id,
        end_request.actual_duration,
        end_request.productivity_rating,
    )
    .await
    {
        Ok(Some(session)) => {
            // Update daily stats
            if let Some(duration) = session.actual_duration_minutes.or(Some(session.duration_minutes)) {
                let _ = StatsService::increment_focus_session(&state.db, user.id, duration).await;
            }
            Ok(Json(session))
        }
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_active_session(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
) -> Result<Json<Option<FocusSessionOut>>, StatusCode> {
    match FocusSessionService::get_active_session(&state.db, user.id).await {
        Ok(session) => Ok(Json(session)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}