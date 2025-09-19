use axum::{extract::State, http::StatusCode, response::Json};
use serde_json::json;
use std::sync::Arc;

use crate::{db, AppState};

pub async fn root() -> &'static str {
    "IRL Quest API Server - Transform your TODO into epic D&D adventures!"
}

pub async fn health(State(state): State<Arc<AppState>>) -> Result<Json<serde_json::Value>, StatusCode> {
    match db::check_health(&state.db).await {
        Ok(_) => Ok(Json(json!({"status": "ok", "database": "connected"}))),
        Err(_) => Err(StatusCode::SERVICE_UNAVAILABLE),
    }
}

pub async fn ready(State(state): State<Arc<AppState>>) -> Result<Json<serde_json::Value>, StatusCode> {
    match db::check_health(&state.db).await {
        Ok(_) => Ok(Json(json!({
            "status": "ready",
            "database": "connected",
            "version": env!("CARGO_PKG_VERSION"),
            "features": ["quest_generation", "task_enhancement", "rag_system"]
        }))),
        Err(_) => Err(StatusCode::SERVICE_UNAVAILABLE),
    }
}