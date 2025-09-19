use anyhow::Result;
use chrono::{DateTime, NaiveDate, Utc};
use sqlx::PgPool;

use crate::models::{DailyStats, DailyStatsCreate, DailyStatsOut, DailyStatsUpdate, WeeklyStatsOut};

pub struct StatsService;

impl StatsService {
    pub async fn get_or_create_daily_stats(
        pool: &PgPool,
        user_id: i32,
        date: NaiveDate,
    ) -> Result<DailyStatsOut> {
        // Try to get existing stats
        let existing_stats: Option<DailyStats> = sqlx::query_as(
            "SELECT * FROM daily_stats WHERE user_id = $1 AND date = $2",
        )
        .bind(user_id)
        .bind(date)
        .fetch_optional(pool)
        .await?;

        if let Some(stats) = existing_stats {
            return Ok(DailyStatsOut::from(stats));
        }

        // Create new stats if not exists
        let stats: DailyStats = sqlx::query_as(
            r#"
            INSERT INTO daily_stats (user_id, date, tasks_completed, focus_sessions, total_focus_time, experience_gained, quests_completed)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            RETURNING *
            "#,
        )
        .bind(user_id)
        .bind(date)
        .bind(0) // tasks_completed
        .bind(0) // focus_sessions
        .bind(0) // total_focus_time
        .bind(0) // experience_gained
        .bind(0) // quests_completed
        .fetch_one(pool)
        .await?;

        Ok(DailyStatsOut::from(stats))
    }

    pub async fn update_daily_stats(
        pool: &PgPool,
        user_id: i32,
        date: NaiveDate,
        update: DailyStatsUpdate,
    ) -> Result<DailyStatsOut> {
        let stats: DailyStats = sqlx::query_as(
            r#"
            UPDATE daily_stats 
            SET tasks_completed = COALESCE($1, tasks_completed),
                focus_sessions = COALESCE($2, focus_sessions),
                total_focus_time = COALESCE($3, total_focus_time),
                experience_gained = COALESCE($4, experience_gained),
                quests_completed = COALESCE($5, quests_completed),
                productivity_score = COALESCE($6, productivity_score),
                notes = COALESCE($7, notes)
            WHERE user_id = $8 AND date = $9
            RETURNING *
            "#,
        )
        .bind(update.tasks_completed)
        .bind(update.focus_sessions)
        .bind(update.total_focus_time)
        .bind(update.experience_gained)
        .bind(update.quests_completed)
        .bind(update.productivity_score.map(|s| s.to_string())) 
        .bind(update.notes)
        .bind(user_id)
        .bind(date)
        .fetch_one(pool)
        .await?;

        Ok(DailyStatsOut::from(stats))
    }

    pub async fn increment_task_completion(
        pool: &PgPool,
        user_id: i32,
        experience_gained: i32,
    ) -> Result<()> {
        let today = Utc::now().date_naive();
        
        sqlx::query(
            r#"
            INSERT INTO daily_stats (user_id, date, tasks_completed, experience_gained)
            VALUES ($1, $2, 1, $3)
            ON CONFLICT (user_id, date)
            DO UPDATE SET 
                tasks_completed = daily_stats.tasks_completed + 1,
                experience_gained = daily_stats.experience_gained + $3
            "#,
        )
        .bind(user_id)
        .bind(today)
        .bind(experience_gained)
        .execute(pool)
        .await?;

        Ok(())
    }

    pub async fn increment_focus_session(
        pool: &PgPool,
        user_id: i32,
        focus_time_minutes: i32,
    ) -> Result<()> {
        let today = Utc::now().date_naive();
        
        sqlx::query(
            r#"
            INSERT INTO daily_stats (user_id, date, focus_sessions, total_focus_time)
            VALUES ($1, $2, 1, $3)
            ON CONFLICT (user_id, date)
            DO UPDATE SET 
                focus_sessions = daily_stats.focus_sessions + 1,
                total_focus_time = daily_stats.total_focus_time + $3
            "#,
        )
        .bind(user_id)
        .bind(today)
        .bind(focus_time_minutes)
        .execute(pool)
        .await?;

        Ok(())
    }

    pub async fn increment_quest_completion(
        pool: &PgPool,
        user_id: i32,
        experience_gained: i32,
    ) -> Result<()> {
        let today = Utc::now().date_naive();
        
        sqlx::query(
            r#"
            INSERT INTO daily_stats (user_id, date, quests_completed, experience_gained)
            VALUES ($1, $2, 1, $3)
            ON CONFLICT (user_id, date)
            DO UPDATE SET 
                quests_completed = daily_stats.quests_completed + 1,
                experience_gained = daily_stats.experience_gained + $3
            "#,
        )
        .bind(user_id)
        .bind(today)
        .bind(experience_gained)
        .execute(pool)
        .await?;

        Ok(())
    }

    pub async fn get_weekly_stats(
        pool: &PgPool,
        user_id: i32,
        week_start: NaiveDate,
    ) -> Result<WeeklyStatsOut> {
        let week_end = week_start + chrono::Duration::days(6);
        
        let daily_stats: Vec<DailyStats> = sqlx::query_as(
            r#"
            SELECT * FROM daily_stats 
            WHERE user_id = $1 AND date >= $2 AND date <= $3
            ORDER BY date
            "#,
        )
        .bind(user_id)
        .bind(week_start)
        .bind(week_end)
        .fetch_all(pool)
        .await?;

        let total_tasks_completed = daily_stats.iter().map(|s| s.tasks_completed as i64).sum();
        let total_focus_sessions = daily_stats.iter().map(|s| s.focus_sessions as i64).sum();
        let total_focus_time = daily_stats.iter().map(|s| s.total_focus_time as i64).sum();
        let total_experience_gained = daily_stats.iter().map(|s| s.experience_gained as i64).sum();
        let total_quests_completed = daily_stats.iter().map(|s| s.quests_completed as i64).sum();

        let productivity_scores: Vec<f32> = daily_stats
            .iter()
            .filter_map(|s| s.productivity_score.as_ref())
            .filter_map(|s| s.to_string().parse().ok())
            .collect();

        let average_productivity_score = if !productivity_scores.is_empty() {
            Some(productivity_scores.iter().sum::<f32>() / productivity_scores.len() as f32)
        } else {
            None
        };

        Ok(WeeklyStatsOut {
            week_start,
            total_tasks_completed,
            total_focus_sessions,
            total_focus_time,
            total_experience_gained,
            total_quests_completed,
            average_productivity_score,
            daily_stats: daily_stats.into_iter().map(DailyStatsOut::from).collect(),
        })
    }

    pub async fn get_user_stats_summary(
        pool: &PgPool,
        user_id: i32,
    ) -> Result<serde_json::Value> {
        let total_stats: (i64, i64, i64, i64, i64) = sqlx::query_as(
            r#"
            SELECT 
                COALESCE(SUM(tasks_completed), 0) as total_tasks,
                COALESCE(SUM(focus_sessions), 0) as total_sessions,
                COALESCE(SUM(total_focus_time), 0) as total_focus_time,
                COALESCE(SUM(experience_gained), 0) as total_experience,
                COALESCE(SUM(quests_completed), 0) as total_quests
            FROM daily_stats 
            WHERE user_id = $1
            "#,
        )
        .bind(user_id)
        .fetch_one(pool)
        .await?;

        Ok(serde_json::json!({
            "total_tasks_completed": total_stats.0,
            "total_focus_sessions": total_stats.1,
            "total_focus_time_minutes": total_stats.2,
            "total_experience_gained": total_stats.3,
            "total_quests_completed": total_stats.4,
        }))
    }
}