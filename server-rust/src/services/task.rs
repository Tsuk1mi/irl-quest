use anyhow::Result;
use chrono::Utc;
use sqlx::PgPool;

use crate::models::{Task, TaskCreate, TaskOut, TaskUpdate};

pub struct TaskService;

impl TaskService {
    pub async fn list_tasks_for_user(
        pool: &PgPool,
        user_id: i32,
        skip: i64,
        limit: i64,
    ) -> Result<Vec<TaskOut>> {
        let tasks: Vec<Task> = sqlx::query_as::<_, Task>(
            "SELECT * FROM tasks WHERE owner_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3",
        )
        .bind(user_id)
        .bind(limit)
        .bind(skip)
        .fetch_all(pool)
        .await?;

        Ok(tasks.into_iter().map(TaskOut::from).collect())
    }

    pub async fn create_task_for_user(
        pool: &PgPool,
        user_id: i32,
        task_create: TaskCreate,
    ) -> Result<TaskOut> {
        let task: Task = sqlx::query_as::<_, Task>(
            r#"
            INSERT INTO tasks (
                title, description, completed, status, priority, deadline, estimated_duration,
                actual_duration, difficulty, experience_reward, tags, location_name, subtasks,
                notes, attachments, completion_proof, metadata, created_at, quest_id, owner_id
            )
            VALUES (
                $1, $2, false, COALESCE($3,'pending'), COALESCE($4,'medium'), $5, $6,
                NULL, COALESCE($7,1), COALESCE($8,0), COALESCE($9, ARRAY[]::TEXT[]), $10, COALESCE($11,'[]'::jsonb),
                $12, COALESCE($13, ARRAY[]::TEXT[]), NULL, COALESCE($14,'{}'::jsonb), $15, $16, $17
            )
            RETURNING *
            "#,
        )
        .bind(&task_create.title)
        .bind(&task_create.description)
        .bind(&task_create.priority)
        .bind(&task_create.priority)
        .bind(&task_create.deadline)
        .bind(&task_create.estimated_duration)
        .bind(&task_create.difficulty)
        .bind(&task_create.experience_reward)
        .bind(&task_create.tags)
        .bind(&task_create.location_name)
        .bind(&task_create.subtasks)
        .bind(&task_create.notes)
        .bind(&task_create.attachments)
        .bind(&task_create.metadata)
        .bind(Utc::now())
        .bind(&task_create.quest_id)
        .bind(user_id)
        .fetch_one(pool)
        .await?;

        Ok(TaskOut::from(task))
    }

    pub async fn get_task_for_user(
        pool: &PgPool,
        user_id: i32,
        task_id: i32,
    ) -> Result<Option<TaskOut>> {
        let task: Option<Task> = sqlx::query_as::<_, Task>(
            "SELECT * FROM tasks WHERE id = $1 AND owner_id = $2",
        )
        .bind(task_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(task.map(TaskOut::from))
    }

    pub async fn update_task_for_user(
        pool: &PgPool,
        user_id: i32,
        task_id: i32,
        task_update: TaskUpdate,
    ) -> Result<Option<TaskOut>> {
        // Load existing
        let mut task: Option<Task> = sqlx::query_as::<_, Task>(
            "SELECT * FROM tasks WHERE id = $1 AND owner_id = $2",
        )
        .bind(task_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        let mut task = match task { Some(t) => t, None => return Ok(None) };

        if let Some(title) = task_update.title { task.title = title; }
        if let Some(description) = task_update.description { task.description = Some(description); }
        if let Some(completed) = task_update.completed { task.completed = completed; }
        if let Some(status) = task_update.status { task.status = status; }
        if let Some(priority) = task_update.priority { task.priority = priority; }
        if let Some(deadline) = task_update.deadline { task.deadline = Some(deadline); }
        if let Some(estimated_duration) = task_update.estimated_duration { task.estimated_duration = Some(estimated_duration); }
        if let Some(actual_duration) = task_update.actual_duration { task.actual_duration = Some(actual_duration); }
        if let Some(difficulty) = task_update.difficulty { task.difficulty = difficulty; }
        if let Some(experience_reward) = task_update.experience_reward { task.experience_reward = experience_reward; }
        if let Some(tags) = task_update.tags { task.tags = tags; }
        if let Some(location_name) = task_update.location_name { task.location_name = Some(location_name); }
        if let Some(subtasks) = task_update.subtasks { task.subtasks = subtasks; }
        if let Some(notes) = task_update.notes { task.notes = Some(notes); }
        if let Some(attachments) = task_update.attachments { task.attachments = attachments; }
        if let Some(completion_proof) = task_update.completion_proof { task.completion_proof = Some(completion_proof); }
        if let Some(metadata) = task_update.metadata { task.metadata = metadata; }

        let updated: Task = sqlx::query_as::<_, Task>(
            r#"
            UPDATE tasks SET 
                title = $1, description = $2, completed = $3, status = $4, priority = $5,
                deadline = $6, estimated_duration = $7, actual_duration = $8,
                difficulty = $9, experience_reward = $10, tags = $11, location_name = $12,
                subtasks = $13, notes = $14, attachments = $15, completion_proof = $16, metadata = $17
            WHERE id = $18 AND owner_id = $19
            RETURNING *
            "#,
        )
        .bind(&task.title)
        .bind(&task.description)
        .bind(task.completed)
        .bind(&task.status)
        .bind(&task.priority)
        .bind(&task.deadline)
        .bind(&task.estimated_duration)
        .bind(&task.actual_duration)
        .bind(task.difficulty)
        .bind(task.experience_reward)
        .bind(&task.tags)
        .bind(&task.location_name)
        .bind(&task.subtasks)
        .bind(&task.notes)
        .bind(&task.attachments)
        .bind(&task.completion_proof)
        .bind(&task.metadata)
        .bind(task_id)
        .bind(user_id)
        .fetch_one(pool)
        .await?;

        Ok(Some(TaskOut::from(updated)))
    }

    pub async fn delete_task_for_user(
        pool: &PgPool,
        user_id: i32,
        task_id: i32,
    ) -> Result<bool> {
        let result = sqlx::query(
            "DELETE FROM tasks WHERE id = $1 AND owner_id = $2",
        )
        .bind(task_id)
        .bind(user_id)
        .execute(pool)
        .await?;

        Ok(result.rows_affected() > 0)
    }

    pub async fn complete_task_for_user(
        pool: &PgPool,
        user_id: i32,
        task_id: i32,
    ) -> Result<Option<TaskOut>> {
        let updated: Option<Task> = sqlx::query_as::<_, Task>(
            r#"
            UPDATE tasks 
            SET completed = true, status = 'completed'
            WHERE id = $1 AND owner_id = $2
            RETURNING *
            "#,
        )
        .bind(task_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(updated.map(TaskOut::from))
    }
}
