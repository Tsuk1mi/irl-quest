use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize)]
pub struct Quest {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub created_at: DateTime<Utc>,
    pub owner_id: Option<i32>,
}

#[derive(Debug, Deserialize)]
pub struct QuestCreate {
    pub title: String,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
}

#[derive(Debug, Serialize)]
pub struct QuestOut {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub created_at: DateTime<Utc>,
    pub owner_id: Option<i32>,
}

impl From<Quest> for QuestOut {
    fn from(quest: Quest) -> Self {
        Self {
            id: quest.id,
            title: quest.title,
            description: quest.description,
            difficulty: quest.difficulty,
            created_at: quest.created_at,
            owner_id: quest.owner_id,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct QuestUpdate {
    pub title: Option<String>,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
}