use axum::{extract::{State, Extension}, http::StatusCode, response::Json, Json as ExtractJson};
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{LoginRequest, Token, UserCreate, UserOut},
    services::AuthService,
    AppState,
};

pub async fn register(
    State(state): State<Arc<AppState>>,
    ExtractJson(user_create): ExtractJson<UserCreate>,
) -> Result<(StatusCode, Json<UserOut>), StatusCode> {
    let auth_service = AuthService::new(state.settings.clone());

    match auth_service.register_user(&state.db, user_create).await {
        Ok(user) => Ok((StatusCode::CREATED, Json(user))),
        Err(_) => Err(StatusCode::BAD_REQUEST),
    }
}

pub async fn login(
    State(state): State<Arc<AppState>>,
    ExtractJson(login_request): ExtractJson<LoginRequest>,
) -> Result<Json<Token>, StatusCode> {
    let auth_service = AuthService::new(state.settings.clone());

    match auth_service
        .authenticate_and_issue_token(&state.db, &login_request.username, &login_request.password)
        .await
    {
        Ok((access_token, user)) => Ok(Json(Token::new(access_token, user))),
        Err(_) => Err(StatusCode::UNAUTHORIZED),
    }
}

pub async fn me(Extension(CurrentUser(user)): Extension<CurrentUser>) -> Json<UserOut> {
    Json(UserOut::from(user))
}