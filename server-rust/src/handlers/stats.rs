use axum::{
    extract::{Extension, State},
    Json,
};
use chrono::NaiveDate;
use serde::Serialize;

use crate::{error::AppError, middleware::auth::CurrentUser, state::AppState};
use sqlx::Row;

const PG_UNDEFINED_TABLE: &str = "42P01";

#[derive(Debug, Serialize)]
pub struct DailyStatsResponse {
    pub date: NaiveDate,
    #[serde(rename = "tasks_completed")]
    pub tasks_completed: i32,
    #[serde(rename = "tasks_total")]
    pub tasks_total: i32,
    #[serde(rename = "quests_completed")]
    pub quests_completed: i32,
    #[serde(rename = "quests_total")]
    pub quests_total: i32,
    #[serde(rename = "experience_gained")]
    pub experience_gained: i32,
    #[serde(rename = "focus_time")]
    pub focus_time: i32,
    #[serde(rename = "study_time")]
    pub study_time: i32,
}

#[derive(Debug, Serialize)]
pub struct TotalStatsResponse {
    #[serde(rename = "total_tasks_completed")]
    pub total_tasks_completed: i32,
    #[serde(rename = "total_quests_completed")]
    pub total_quests_completed: i32,
    #[serde(rename = "total_experience")]
    pub total_experience: i32,
    #[serde(rename = "current_level")]
    pub current_level: i32,
    #[serde(rename = "next_level_experience")]
    pub next_level_experience: i32,
    #[serde(rename = "total_focus_time")]
    pub total_focus_time: i32,
    #[serde(rename = "total_study_time")]
    pub total_study_time: i32,
    #[serde(rename = "achievement_count")]
    pub achievement_count: i32,
}

fn map_db_error(err: sqlx::Error, feature: &str) -> AppError {
    if let sqlx::Error::Database(db_err) = &err {
        if let Some(code) = db_err.code() {
            if code == PG_UNDEFINED_TABLE {
                return AppError::NotImplemented(format!(
                    "{feature} недоступна: примените актуальные миграции сервера (sqlx migrate run)"
                ));
            }
        }
        return AppError::DatabaseError(db_err.message().to_string());
    }
    AppError::Database(err)
}

pub async fn get_daily_stats(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<Vec<DailyStatsResponse>>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для просмотра статистики".into())
    })?;

    let rows = sqlx::query(
        r#"
        SELECT
            date,
            tasks_completed,
            tasks_total,
            quests_completed,
            quests_total,
            experience_gained,
            focus_time,
            study_time
        FROM daily_stats
        WHERE user_id = $1
        ORDER BY date DESC
        LIMIT 30
        "#,
    )
    .bind(user.0.id)
    .fetch_all(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Статистика"))?;

    let stats = rows
        .into_iter()
        .map(|row| DailyStatsResponse {
            date: row.get("date"),
            tasks_completed: row.get("tasks_completed"),
            tasks_total: row.get("tasks_total"),
            quests_completed: row.get("quests_completed"),
            quests_total: row.get("quests_total"),
            experience_gained: row.get("experience_gained"),
            focus_time: row.get("focus_time"),
            study_time: row.get("study_time"),
        })
        .collect();

    Ok(Json(stats))
}

pub async fn get_total_stats(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<TotalStatsResponse>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для просмотра статистики".into())
    })?;

    let aggregates = sqlx::query(
        r#"
        SELECT
            COALESCE(SUM(tasks_completed), 0) AS total_tasks_completed,
            COALESCE(SUM(quests_completed), 0) AS total_quests_completed,
            COALESCE(SUM(experience_gained), 0) AS total_experience,
            COALESCE(SUM(focus_time), 0) AS total_focus_time,
            COALESCE(SUM(study_time), 0) AS total_study_time
        FROM daily_stats
        WHERE user_id = $1
        "#,
    )
    .bind(user.0.id)
    .fetch_one(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Статистика"))?;

    let total_tasks: i64 = aggregates.get("total_tasks_completed");
    let total_quests: i64 = aggregates.get("total_quests_completed");
    let total_experience: i64 = aggregates.get("total_experience");
    let total_focus_time: i64 = aggregates.get("total_focus_time");
    let total_study_time: i64 = aggregates.get("total_study_time");

    let achievements_row = sqlx::query(
        r#"
        SELECT COUNT(*)::INT AS count
        FROM user_achievements
        WHERE user_id = $1
        "#,
    )
    .bind(user.0.id)
    .fetch_one(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Статистика"))?;

    let achievements: i32 = achievements_row.get("count");

    let next_level_exp = ((user.0.level + 1).max(1)) * 150;

    Ok(Json(TotalStatsResponse {
        total_tasks_completed: total_tasks as i32,
        total_quests_completed: total_quests as i32,
        total_experience: total_experience as i32 + user.0.experience,
        current_level: user.0.level,
        next_level_experience: next_level_exp,
        total_focus_time: total_focus_time as i32,
        total_study_time: total_study_time as i32,
        achievement_count: achievements,
    }))
}
