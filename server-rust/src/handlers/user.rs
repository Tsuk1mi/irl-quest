use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{UserOut, UserUpdate, UserStats, UserAchievementOut},
    AppState,
};

pub async fn get_me(CurrentUser(user): CurrentUser) -> Json<UserOut> {
    Json(UserOut::from(user))
}

pub async fn update_me(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
    ExtractJson(_user_update): ExtractJson<UserUpdate>,
) -> Result<Json<UserOut>, StatusCode> {
    // Placeholder - implement actual user update
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn get_user_stats(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
) -> Result<Json<UserStats>, StatusCode> {
    // Placeholder - implement actual user stats
    Err(StatusCode::NOT_IMPLEMENTED)
}

pub async fn get_user_achievements(
    State(_state): State<Arc<AppState>>,
    CurrentUser(_user): CurrentUser,
) -> Result<Json<Vec<UserAchievementOut>>, StatusCode> {
    // Placeholder - implement actual user achievements
    Ok(Json(vec![]))
}