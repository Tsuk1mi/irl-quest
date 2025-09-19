use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::Json,
};
use chrono::{NaiveDate, Utc};
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{DailyStatsOut, WeeklyStatsOut},
    services::StatsService,
    AppState,
};

pub async fn get_daily_stats(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Path(date_str): Path<String>,
) -> Result<Json<DailyStatsOut>, StatusCode> {
    let date = date_str
        .parse::<NaiveDate>()
        .map_err(|_| StatusCode::BAD_REQUEST)?;

    match StatsService::get_or_create_daily_stats(&state.db, user.id, date).await {
        Ok(stats) => Ok(Json(stats)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_today_stats(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
) -> Result<Json<DailyStatsOut>, StatusCode> {
    let today = Utc::now().date_naive();

    match StatsService::get_or_create_daily_stats(&state.db, user.id, today).await {
        Ok(stats) => Ok(Json(stats)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_weekly_stats(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    Path(week_start_str): Path<String>,
) -> Result<Json<WeeklyStatsOut>, StatusCode> {
    let week_start = week_start_str
        .parse::<NaiveDate>()
        .map_err(|_| StatusCode::BAD_REQUEST)?;

    match StatsService::get_weekly_stats(&state.db, user.id, week_start).await {
        Ok(stats) => Ok(Json(stats)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_current_week_stats(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
) -> Result<Json<WeeklyStatsOut>, StatusCode> {
    let today = Utc::now().date_naive();
    let weekday = today.weekday().num_days_from_monday();
    let week_start = today - chrono::Duration::days(weekday as i64);

    match StatsService::get_weekly_stats(&state.db, user.id, week_start).await {
        Ok(stats) => Ok(Json(stats)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn get_user_summary(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
) -> Result<Json<serde_json::Value>, StatusCode> {
    match StatsService::get_user_stats_summary(&state.db, user.id).await {
        Ok(summary) => Ok(Json(summary)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}