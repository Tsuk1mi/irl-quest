use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use sqlx::{types::BigDecimal, FromRow};

#[derive(Debug, Clone, FromRow, Serialize, Deserialize)]
pub struct DailyStats {
    pub id: i32,
    pub user_id: i32,
    pub date: NaiveDate,
    pub tasks_completed: i32,
    pub focus_sessions: i32,
    pub total_focus_time: i32, // in minutes
    pub experience_gained: i32,
    pub quests_completed: i32,
    pub productivity_score: Option<BigDecimal>,
    pub notes: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct DailyStatsCreate {
    pub date: NaiveDate,
    pub tasks_completed: Option<i32>,
    pub focus_sessions: Option<i32>,
    pub total_focus_time: Option<i32>,
    pub experience_gained: Option<i32>,
    pub quests_completed: Option<i32>,
    pub productivity_score: Option<f32>,
    pub notes: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct DailyStatsUpdate {
    pub tasks_completed: Option<i32>,
    pub focus_sessions: Option<i32>,
    pub total_focus_time: Option<i32>,
    pub experience_gained: Option<i32>,
    pub quests_completed: Option<i32>,
    pub productivity_score: Option<f32>,
    pub notes: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct DailyStatsOut {
    pub id: i32,
    pub date: NaiveDate,
    pub tasks_completed: i32,
    pub focus_sessions: i32,
    pub total_focus_time: i32,
    pub experience_gained: i32,
    pub quests_completed: i32,
    pub productivity_score: Option<f32>,
    pub notes: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct WeeklyStatsOut {
    pub week_start: NaiveDate,
    pub total_tasks_completed: i64,
    pub total_focus_sessions: i64,
    pub total_focus_time: i64,
    pub total_experience_gained: i64,
    pub total_quests_completed: i64,
    pub average_productivity_score: Option<f32>,
    pub daily_stats: Vec<DailyStatsOut>,
}

impl From<DailyStats> for DailyStatsOut {
    fn from(stats: DailyStats) -> Self {
        Self {
            id: stats.id,
            date: stats.date,
            tasks_completed: stats.tasks_completed,
            focus_sessions: stats.focus_sessions,
            total_focus_time: stats.total_focus_time,
            experience_gained: stats.experience_gained,
            quests_completed: stats.quests_completed,
            productivity_score: stats.productivity_score.map(|s| s.to_string().parse::<f32>().unwrap_or(0.0)),
            notes: stats.notes,
        }
    }
}