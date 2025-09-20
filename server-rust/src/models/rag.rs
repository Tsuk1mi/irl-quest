use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize)]
pub struct RagKnowledge {
    pub id: i32,
    pub content: String,
    pub content_type: String,
    pub tags: Vec<String>,
    #[sqlx(rename = "embedding")]
    pub embedding: Option<Vec<f32>>,
    pub metadata: JsonValue,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize)]
pub struct RagKnowledgeCreate {
    pub content: String,
    pub content_type: String,
    pub tags: Option<Vec<String>>,
    pub metadata: Option<JsonValue>,
}

#[derive(Debug, Serialize)]
pub struct RagKnowledgeOut {
    pub id: i32,
    pub content: String,
    pub content_type: String,
    pub tags: Vec<String>,
    pub metadata: JsonValue,
    pub created_at: DateTime<Utc>,
}

impl From<RagKnowledge> for RagKnowledgeOut {
    fn from(knowledge: RagKnowledge) -> Self {
        Self {
            id: knowledge.id,
            content: knowledge.content,
            content_type: knowledge.content_type,
            tags: knowledge.tags,
            metadata: knowledge.metadata,
            created_at: knowledge.created_at,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct QuestGenerationRequest {
    pub todo_text: String,
    pub context: Option<String>,
    pub difficulty_preference: Option<i32>,
    pub user_level: Option<i32>,
    pub tags_override: Option<Vec<String>>, // allow manual tags override
}

#[derive(Debug, Serialize)]
pub struct QuestGenerationResponse {
    pub title: String,
    pub description: String,
    pub difficulty: i32,
    pub reward_experience: i32,
    pub reward_description: String,
    pub tags: Vec<String>,
    pub quest_type: String,
    pub tasks: Vec<GeneratedTask>,
    pub story_context: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct GeneratedTask {
    pub title: String,
    pub description: String,
    pub difficulty: i32,
    pub experience_reward: i32,
    pub estimated_duration: Option<i32>,
    pub is_boss: bool,
}

#[derive(Debug, Deserialize)]
pub struct TaskEnhancementRequest {
    pub task_text: String,
    pub context: Option<String>,
    pub user_level: Option<i32>,
}

#[derive(Debug, Serialize)]
pub struct TaskEnhancementResponse {
    pub enhanced_title: String,
    pub enhanced_description: String,
    pub suggested_difficulty: i32,
    pub suggested_experience: i32,
    pub story_context: Option<String>,
    pub suggested_tags: Vec<String>,
}