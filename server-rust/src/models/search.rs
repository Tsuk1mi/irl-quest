use chrono::{DateTime, Utc};
use serde::Serialize;
use sqlx::FromRow;

#[derive(Debug, Serialize)]
pub struct SearchResults {
    pub quests: Vec<QuestSearchResult>,
    pub tasks: Vec<TaskSearchResult>,
}

#[derive(Debug, Serialize, FromRow)]
pub struct QuestSearchResult {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub status: String,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, FromRow)]
pub struct TaskSearchResult {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub status: String,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, FromRow)]
pub struct TagCount {
    pub tag: String,
    pub count: i64,
}
