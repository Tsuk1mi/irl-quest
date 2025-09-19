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
        let tasks: Vec<Task> = sqlx::query_as(
            "SELECT * FROM tasks WHERE owner_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3"
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
        let task: Task = sqlx::query_as(
            r#"
            INSERT INTO tasks (title, description, completed, created_at, owner_id)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING id, title, description, completed, created_at, owner_id
            "#,
        )
        .bind(&task_create.title)
        .bind(&task_create.description)
        .bind(false)
        .bind(Utc::now())
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
        let task: Option<Task> = sqlx::query_as(
            "SELECT * FROM tasks WHERE id = $1 AND owner_id = $2"
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
        // First check if task exists and belongs to user
        let existing_task: Option<Task> = sqlx::query_as(
            "SELECT * FROM tasks WHERE id = $1 AND owner_id = $2"
        )
        .bind(task_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        let mut task = match existing_task {
            Some(task) => task,
            None => return Ok(None),
        };

        // Update fields if provided
        if let Some(title) = task_update.title {
            task.title = title;
        }
        if let Some(description) = task_update.description {
            task.description = Some(description);
        }
        if let Some(completed) = task_update.completed {
            task.completed = completed;
        }

        // Save updated task
        let updated_task: Task = sqlx::query_as(
            r#"
            UPDATE tasks 
            SET title = $1, description = $2, completed = $3
            WHERE id = $4 AND owner_id = $5
            RETURNING id, title, description, completed, created_at, owner_id
            "#,
        )
        .bind(&task.title)
        .bind(&task.description)
        .bind(task.completed)
        .bind(task_id)
        .bind(user_id)
        .fetch_one(pool)
        .await?;

        Ok(Some(TaskOut::from(updated_task)))
    }

    pub async fn delete_task_for_user(
        pool: &PgPool,
        user_id: i32,
        task_id: i32,
    ) -> Result<bool> {
        let result = sqlx::query(
            "DELETE FROM tasks WHERE id = $1 AND owner_id = $2"
        )
        .bind(task_id)
        .bind(user_id)
        .execute(pool)
        .await?;

        Ok(result.rows_affected() > 0)
    }
}