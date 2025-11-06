/// WebSocket handler для realtime коммуникации
use axum::{
    extract::{
        ws::{WebSocket, WebSocketUpgrade},
        Extension, State,
    },
    response::Response,
};
use crate::error::AppError;
use crate::middleware::auth::CurrentUser;
use crate::services::WebSocketHandler;
use crate::state::AppState;

/// GET /ws - WebSocket endpoint
pub async fn websocket_handler(
    ws: WebSocketUpgrade,
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Response, AppError> {
    let user = current_user
        .ok_or(AppError::Unauthorized("WebSocket requires authentication".to_string()))?;

    tracing::info!("WebSocket upgrade requested by user: {}", user.0.id);

    Ok(ws.on_upgrade(move |socket| {
        handle_socket(socket, user.0.id, user.0.username.clone(), state)
    }))
}

/// Обработка WebSocket соединения
async fn handle_socket(
    socket: WebSocket,
    user_id: i32,
    username: String,
    state: AppState,
) {
    let handler = WebSocketHandler::new(user_id, username, state.ws_manager.clone());
    handler.handle_socket(socket).await;
}

