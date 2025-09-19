use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize)]
pub struct Quest {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub status: String,
    pub priority: String,
    pub deadline: Option<DateTime<Utc>>,
    pub completion_percentage: i32,
    pub reward_experience: i32,
    pub reward_description: Option<String>,
    pub tags: Vec<String>,
    pub is_public: bool,
    pub location_name: Option<String>,
    pub quest_type: String,
    pub metadata: JsonValue,
    pub created_at: DateTime<Utc>,
    pub owner_id: Option<i32>,
}

#[derive(Debug, Deserialize)]
pub struct QuestCreate {
    pub title: String,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
    pub priority: Option<String>,
    pub deadline: Option<DateTime<Utc>>,
    pub reward_experience: Option<i32>,
    pub reward_description: Option<String>,
    pub tags: Option<Vec<String>>,
    pub is_public: Option<bool>,
    pub location_name: Option<String>,
    pub quest_type: Option<String>,
    pub metadata: Option<JsonValue>,
}

#[derive(Debug, Serialize)]
pub struct QuestOut {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub status: String,
    pub priority: String,
    pub deadline: Option<DateTime<Utc>>,
    pub completion_percentage: i32,
    pub reward_experience: i32,
    pub reward_description: Option<String>,
    pub tags: Vec<String>,
    pub is_public: bool,
    pub location_name: Option<String>,
    pub quest_type: String,
    pub metadata: JsonValue,
    pub created_at: DateTime<Utc>,
    pub tasks_count: Option<i64>,
    pub completed_tasks_count: Option<i64>,
}

impl From<Quest> for QuestOut {
    fn from(quest: Quest) -> Self {
        Self {
            id: quest.id,
            title: quest.title,
            description: quest.description,
            difficulty: quest.difficulty,
            status: quest.status,
            priority: quest.priority,
            deadline: quest.deadline,
            completion_percentage: quest.completion_percentage,
            reward_experience: quest.reward_experience,
            reward_description: quest.reward_description,
            tags: quest.tags,
            is_public: quest.is_public,
            location_name: quest.location_name,
            quest_type: quest.quest_type,
            metadata: quest.metadata,
            created_at: quest.created_at,
            tasks_count: None,
            completed_tasks_count: None,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct QuestUpdate {
    pub title: Option<String>,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
    pub status: Option<String>,
    pub priority: Option<String>,
    pub deadline: Option<DateTime<Utc>>,
    pub reward_experience: Option<i32>,
    pub reward_description: Option<String>,
    pub tags: Option<Vec<String>>,
    pub is_public: Option<bool>,
    pub location_name: Option<String>,
    pub quest_type: Option<String>,
    pub metadata: Option<JsonValue>,
}

#[derive(Debug, Deserialize)]
pub struct TodoToQuestRequest {
    pub todo_text: String,
    pub context: Option<String>,
    pub difficulty_preference: Option<i32>,
    pub theme_preference: Option<String>,
}