use axum::{extract::State, http::StatusCode, response::Json, Json as ExtractJson};
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{UserOut, UserUpdate},
    services::UserService,
    AppState,
};

pub async fn get_me(CurrentUser(user): CurrentUser) -> Json<UserOut> {
    Json(UserOut::from(user))
}

pub async fn update_me(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    ExtractJson(user_update): ExtractJson<UserUpdate>,
) -> Result<Json<UserOut>, StatusCode> {
    match UserService::update_user(&state.db, user.id, user_update).await {
        Ok(Some(updated_user)) => Ok(Json(updated_user)),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}