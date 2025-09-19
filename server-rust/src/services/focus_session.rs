use anyhow::Result;
use chrono::Utc;
use sqlx::PgPool;

use crate::models::{FocusSession, FocusSessionCreate, FocusSessionOut, FocusSessionUpdate};

pub struct FocusSessionService;

impl FocusSessionService {
    pub async fn create_session(
        pool: &PgPool,
        user_id: i32,
        session_create: FocusSessionCreate,
    ) -> Result<FocusSessionOut> {
        let session: FocusSession = sqlx::query_as(
            r#"
            INSERT INTO focus_sessions (user_id, task_id, duration_minutes, started_at, session_type, notes)
            VALUES ($1, $2, $3, $4, $5, $6)
            RETURNING *
            "#,
        )
        .bind(user_id)
        .bind(session_create.task_id)
        .bind(session_create.duration_minutes)
        .bind(Utc::now())
        .bind(session_create.session_type.unwrap_or_else(|| "work".to_string()))
        .bind(session_create.notes)
        .fetch_one(pool)
        .await?;

        Ok(FocusSessionOut::from(session))
    }

    pub async fn update_session(
        pool: &PgPool,
        user_id: i32,
        session_id: i32,
        session_update: FocusSessionUpdate,
    ) -> Result<Option<FocusSessionOut>> {
        let session: Option<FocusSession> = sqlx::query_as(
            r#"
            UPDATE focus_sessions 
            SET actual_duration_minutes = COALESCE($1, actual_duration_minutes),
                ended_at = COALESCE($2, ended_at),
                notes = COALESCE($3, notes),
                interruptions = COALESCE($4, interruptions),
                productivity_rating = COALESCE($5, productivity_rating)
            WHERE id = $6 AND user_id = $7
            RETURNING *
            "#,
        )
        .bind(session_update.actual_duration_minutes)
        .bind(session_update.ended_at)
        .bind(session_update.notes)
        .bind(session_update.interruptions)
        .bind(session_update.productivity_rating)
        .bind(session_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(session.map(FocusSessionOut::from))
    }

    pub async fn get_user_sessions(
        pool: &PgPool,
        user_id: i32,
        limit: Option<i64>,
    ) -> Result<Vec<FocusSessionOut>> {
        let limit = limit.unwrap_or(50);
        
        let sessions: Vec<FocusSession> = sqlx::query_as(
            r#"
            SELECT * FROM focus_sessions 
            WHERE user_id = $1 
            ORDER BY started_at DESC 
            LIMIT $2
            "#,
        )
        .bind(user_id)
        .bind(limit)
        .fetch_all(pool)
        .await?;

        Ok(sessions.into_iter().map(FocusSessionOut::from).collect())
    }

    pub async fn get_session(
        pool: &PgPool,
        user_id: i32,
        session_id: i32,
    ) -> Result<Option<FocusSessionOut>> {
        let session: Option<FocusSession> = sqlx::query_as(
            "SELECT * FROM focus_sessions WHERE id = $1 AND user_id = $2",
        )
        .bind(session_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(session.map(FocusSessionOut::from))
    }

    pub async fn end_session(
        pool: &PgPool,
        user_id: i32,
        session_id: i32,
        actual_duration: Option<i32>,
        productivity_rating: Option<i32>,
    ) -> Result<Option<FocusSessionOut>> {
        let session: Option<FocusSession> = sqlx::query_as(
            r#"
            UPDATE focus_sessions 
            SET ended_at = $1,
                actual_duration_minutes = COALESCE($2, actual_duration_minutes),
                productivity_rating = COALESCE($3, productivity_rating)
            WHERE id = $4 AND user_id = $5 AND ended_at IS NULL
            RETURNING *
            "#,
        )
        .bind(Utc::now())
        .bind(actual_duration)
        .bind(productivity_rating)
        .bind(session_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(session.map(FocusSessionOut::from))
    }

    pub async fn get_active_session(
        pool: &PgPool,
        user_id: i32,
    ) -> Result<Option<FocusSessionOut>> {
        let session: Option<FocusSession> = sqlx::query_as(
            "SELECT * FROM focus_sessions WHERE user_id = $1 AND ended_at IS NULL ORDER BY started_at DESC LIMIT 1",
        )
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(session.map(FocusSessionOut::from))
    }
}