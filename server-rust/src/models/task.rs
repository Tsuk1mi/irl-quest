use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize)]
pub struct Task {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub status: String,
    pub priority: String,
    pub deadline: Option<DateTime<Utc>>,
    pub estimated_duration: Option<i32>,
    pub actual_duration: Option<i32>,
    pub difficulty: i32,
    pub experience_reward: i32,
    pub tags: Vec<String>,
    pub location_name: Option<String>,
    pub subtasks: JsonValue,
    pub notes: Option<String>,
    pub attachments: Vec<String>,
    pub completion_proof: Option<String>,
    pub metadata: JsonValue,
    pub created_at: DateTime<Utc>,
    pub quest_id: Option<i32>,
    pub owner_id: Option<i32>,
}

#[derive(Debug, Deserialize)]
pub struct TaskCreate {
    pub title: String,
    pub description: Option<String>,
    pub quest_id: Option<i32>,
    pub priority: Option<String>,
    pub deadline: Option<DateTime<Utc>>,
    pub estimated_duration: Option<i32>,
    pub difficulty: Option<i32>,
    pub experience_reward: Option<i32>,
    pub tags: Option<Vec<String>>,
    pub location_name: Option<String>,
    pub subtasks: Option<JsonValue>,
    pub notes: Option<String>,
    pub attachments: Option<Vec<String>>,
    pub metadata: Option<JsonValue>,
}

#[derive(Debug, Serialize)]
pub struct TaskOut {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub status: String,
    pub priority: String,
    pub deadline: Option<DateTime<Utc>>,
    pub estimated_duration: Option<i32>,
    pub actual_duration: Option<i32>,
    pub difficulty: i32,
    pub experience_reward: i32,
    pub tags: Vec<String>,
    pub location_name: Option<String>,
    pub subtasks: JsonValue,
    pub notes: Option<String>,
    pub attachments: Vec<String>,
    pub completion_proof: Option<String>,
    pub metadata: JsonValue,
    pub created_at: DateTime<Utc>,
    pub quest_id: Option<i32>,
}

impl From<Task> for TaskOut {
    fn from(task: Task) -> Self {
        Self {
            id: task.id,
            title: task.title,
            description: task.description,
            completed: task.completed,
            status: task.status,
            priority: task.priority,
            deadline: task.deadline,
            estimated_duration: task.estimated_duration,
            actual_duration: task.actual_duration,
            difficulty: task.difficulty,
            experience_reward: task.experience_reward,
            tags: task.tags,
            location_name: task.location_name,
            subtasks: task.subtasks,
            notes: task.notes,
            attachments: task.attachments,
            completion_proof: task.completion_proof,
            metadata: task.metadata,
            created_at: task.created_at,
            quest_id: task.quest_id,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct TaskUpdate {
    pub title: Option<String>,
    pub description: Option<String>,
    pub completed: Option<bool>,
    pub status: Option<String>,
    pub priority: Option<String>,
    pub deadline: Option<DateTime<Utc>>,
    pub estimated_duration: Option<i32>,
    pub actual_duration: Option<i32>,
    pub difficulty: Option<i32>,
    pub experience_reward: Option<i32>,
    pub tags: Option<Vec<String>>,
    pub location_name: Option<String>,
    pub subtasks: Option<JsonValue>,
    pub notes: Option<String>,
    pub attachments: Option<Vec<String>>,
    pub completion_proof: Option<String>,
    pub metadata: Option<JsonValue>,
}