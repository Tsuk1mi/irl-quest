use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize)]
pub struct Task {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub created_at: DateTime<Utc>,
    pub owner_id: Option<i32>,
}

#[derive(Debug, Deserialize)]
pub struct TaskCreate {
    pub title: String,
    pub description: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct TaskOut {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub created_at: DateTime<Utc>,
    pub owner_id: Option<i32>,
}

impl From<Task> for TaskOut {
    fn from(task: Task) -> Self {
        Self {
            id: task.id,
            title: task.title,
            description: task.description,
            completed: task.completed,
            created_at: task.created_at,
            owner_id: task.owner_id,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct TaskUpdate {
    pub title: Option<String>,
    pub description: Option<String>,
    pub completed: Option<bool>,
}