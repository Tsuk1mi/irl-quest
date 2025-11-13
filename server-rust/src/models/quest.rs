use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::json;
use serde_json::Value as JsonValue;
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize, Deserialize)]
pub struct Quest {
    #[serde(default)]
    pub id: i32,
    #[serde(default)]
    pub title: String,
    pub description: Option<String>,
    #[serde(default)]
    pub difficulty: i32,
    #[serde(default = "default_status")]
    pub status: String,
    #[serde(default = "default_priority")]
    pub priority: String,
    pub deadline: Option<DateTime<Utc>>,
    #[serde(default)]
    pub completion_percentage: i32,
    pub reward_experience: Option<i32>,
    pub reward_description: Option<String>,
    #[serde(default)]
    pub tags: Vec<String>,
    #[serde(default)]
    pub is_public: bool,
    pub location_name: Option<String>,
    #[serde(default = "default_quest_type")]
    pub quest_type: String,
    #[serde(default = "default_json")]
    pub metadata: JsonValue,
    #[serde(default = "chrono::Utc::now")]
    pub created_at: DateTime<Utc>,
    #[serde(default)]
    pub owner_id: i32,
}

fn default_status() -> String {
    "pending".to_string()
}

fn default_priority() -> String {
    "medium".to_string()
}

fn default_quest_type() -> String {
    "personal".to_string()
}

fn default_json() -> JsonValue {
    json!({})
}

#[derive(Debug, Deserialize)]
pub struct QuestCreate {
    pub title: String,
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

impl QuestCreate {
    pub fn into_quest(self, owner_id: i32) -> Quest {
        Quest {
            id: 0, // Will be set by database
            title: self.title,
            description: self.description,
            difficulty: self.difficulty.unwrap_or(1),
            status: self.status.unwrap_or_else(|| "active".to_string()),
            priority: self.priority.unwrap_or_else(|| "medium".to_string()),
            deadline: self.deadline,
            completion_percentage: 0,
            reward_experience: self.reward_experience,
            reward_description: self.reward_description,
            tags: self.tags.unwrap_or_default(),
            is_public: self.is_public.unwrap_or(false),
            location_name: self.location_name,
            quest_type: self.quest_type.unwrap_or_else(|| "personal".to_string()),
            metadata: self.metadata.unwrap_or_else(|| json!({})),
            created_at: Utc::now(),
            owner_id,
        }
    }
}

impl Default for Quest {
    fn default() -> Self {
        Self {
            id: 0,
            title: String::new(),
            description: None,
            difficulty: 1,
            status: "active".to_string(),
            priority: "medium".to_string(),
            deadline: None,
            completion_percentage: 0,
            reward_experience: None,
            reward_description: None,
            tags: Vec::new(),
            is_public: false,
            location_name: None,
            quest_type: "personal".to_string(),
            metadata: json!({}),
            created_at: Utc::now(),
            owner_id: 0,
        }
    }
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
    pub reward_experience: Option<i32>,
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
#[allow(dead_code)]
pub struct QuestUpdate {
    pub title: Option<String>,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
    pub status: Option<String>,
    pub priority: Option<String>,
    pub deadline: Option<DateTime<Utc>>,
    pub completion_percentage: Option<i32>,
    pub reward_experience: Option<i32>,
    pub reward_description: Option<String>,
    pub tags: Option<Vec<String>>,
    pub is_public: Option<bool>,
    pub location_name: Option<String>,
    pub quest_type: Option<String>,
    pub metadata: Option<JsonValue>,
}

#[derive(Debug, Deserialize)]
#[allow(dead_code)]
pub struct TodoToQuestRequest {
    pub todo_text: String,
    pub context: Option<String>,
    pub difficulty_preference: Option<i32>,
}

impl Quest {
    pub fn apply_update(&mut self, update: QuestUpdate) {
        if let Some(title) = update.title {
            self.title = title;
        }
        if let Some(description) = update.description {
            self.description = Some(description);
        }
        if let Some(difficulty) = update.difficulty {
            self.difficulty = difficulty;
        }
        if let Some(status) = update.status {
            self.status = status;
        }
        if let Some(priority) = update.priority {
            self.priority = priority;
        }
        if let Some(deadline) = update.deadline {
            self.deadline = Some(deadline);
        }
        if let Some(completion_percentage) = update.completion_percentage {
            self.completion_percentage = completion_percentage;
        }
        if let Some(reward_experience) = update.reward_experience {
            self.reward_experience = Some(reward_experience);
        }
        if let Some(reward_description) = update.reward_description {
            self.reward_description = Some(reward_description);
        }
        if let Some(tags) = update.tags {
            self.tags = tags;
        }
        if let Some(is_public) = update.is_public {
            self.is_public = is_public;
        }
        if let Some(location_name) = update.location_name {
            self.location_name = Some(location_name);
        }
        if let Some(quest_type) = update.quest_type {
            self.quest_type = quest_type;
        }
        if let Some(metadata) = update.metadata {
            self.metadata = metadata;
        }
    }
}
