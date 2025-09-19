use axum::{extract::State, http::StatusCode, response::Json};
use serde_json::{json, Value};
use std::sync::Arc;

use crate::{db::health_check, AppState};

pub async fn root() -> Json<Value> {
    Json(json!({
        "message": "IRL Quest API (Rust)",
        "status": "ok"
    }))
}

pub async fn health() -> Json<Value> {
    Json(json!({"status": "ok"}))
}

pub async fn ready(State(state): State<Arc<AppState>>) -> Result<Json<Value>, StatusCode> {
    let db_ready = health_check(&state.db).await.unwrap_or(false);
    
    let ready_status = json!({
        "ready": db_ready,
        "details": {
            "db": db_ready,
            "redis": null
        }
    });

    if db_ready {
        Ok(Json(ready_status))
    } else {
        Err(StatusCode::SERVICE_UNAVAILABLE)
    }
}