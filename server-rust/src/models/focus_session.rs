use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize, Deserialize)]
pub struct FocusSession {
    pub id: i32,
    pub user_id: i32,
    pub task_id: Option<i32>,
    pub duration_minutes: i32,
    pub actual_duration_minutes: Option<i32>,
    pub started_at: DateTime<Utc>,
    pub ended_at: Option<DateTime<Utc>>,
    pub session_type: String,
    pub notes: Option<String>,
    pub interruptions: i32,
    pub productivity_rating: Option<i32>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct FocusSessionCreate {
    pub task_id: Option<i32>,
    pub duration_minutes: i32,
    pub session_type: Option<String>,
    pub notes: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct FocusSessionUpdate {
    pub actual_duration_minutes: Option<i32>,
    pub ended_at: Option<DateTime<Utc>>,
    pub notes: Option<String>,
    pub interruptions: Option<i32>,
    pub productivity_rating: Option<i32>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct FocusSessionOut {
    pub id: i32,
    pub task_id: Option<i32>,
    pub duration_minutes: i32,
    pub actual_duration_minutes: Option<i32>,
    pub started_at: DateTime<Utc>,
    pub ended_at: Option<DateTime<Utc>>,
    pub session_type: String,
    pub notes: Option<String>,
    pub interruptions: i32,
    pub productivity_rating: Option<i32>,
}

impl From<FocusSession> for FocusSessionOut {
    fn from(session: FocusSession) -> Self {
        Self {
            id: session.id,
            task_id: session.task_id,
            duration_minutes: session.duration_minutes,
            actual_duration_minutes: session.actual_duration_minutes,
            started_at: session.started_at,
            ended_at: session.ended_at,
            session_type: session.session_type,
            notes: session.notes,
            interruptions: session.interruptions,
            productivity_rating: session.productivity_rating,
        }
    }
}