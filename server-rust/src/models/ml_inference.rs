/// Модели для ML Inference endpoints
use serde::{Deserialize, Serialize};

/// Запрос на определение тегов
#[derive(Debug, Deserialize)]
pub struct TagsRequest {
    pub text: String,
    pub max_tags: Option<usize>,
}

/// Ответ с тегами и confidence scores
#[derive(Debug, Serialize)]
pub struct TagsResponse {
    pub tags: Vec<TagPrediction>,
    pub processing_time_ms: u64,
}

#[derive(Debug, Serialize, Clone)]
pub struct TagPrediction {
    pub tag: String,
    pub confidence: f32,  // 0.0 - 1.0
    pub requires_review: bool,  // true если confidence < threshold
}

/// Запрос на оценку сложности
#[derive(Debug, Deserialize)]
pub struct DifficultyRequest {
    pub title: String,
    pub description: Option<String>,
}

/// Ответ с оценкой сложности
#[derive(Debug, Serialize)]
pub struct DifficultyResponse {
    pub difficulty: u8,  // 1-10
    pub confidence: f32,
    pub factors: Vec<DifficultyFactor>,
    pub requires_review: bool,
    pub processing_time_ms: u64,
}

#[derive(Debug, Serialize)]
pub struct DifficultyFactor {
    pub factor: String,
    pub impact: f32,  // -1.0 to 1.0
    pub explanation: String,
}

/// Запрос на трансформацию ToDo → Quest
#[derive(Debug, Deserialize)]
pub struct TransformRequest {
    pub title: String,
    pub description: Option<String>,
    pub difficulty: Option<u8>,
    pub user_level: Option<u8>,
    pub preferred_style: Option<QuestStyle>,
}

#[derive(Debug, Deserialize, Serialize, Clone)]
#[serde(rename_all = "lowercase")]
pub enum QuestStyle {
    Fantasy,
    SciFi,
    Modern,
    Horror,
    Adventure,
}

/// Ответ с трансформированным квестом
#[derive(Debug, Serialize)]
pub struct TransformResponse {
    pub fantasy_title: String,
    pub fantasy_description: String,
    pub suggested_rewards: Rewards,
    pub suggested_difficulty: u8,
    pub confidence: f32,
    pub requires_review: bool,
    pub style_used: QuestStyle,
    pub processing_time_ms: u64,
}

#[derive(Debug, Serialize)]
pub struct Rewards {
    pub experience: u32,
    pub gold: u32,
    pub items: Vec<String>,
}

/// Запрос на персональные рекомендации
#[derive(Debug, Deserialize)]
pub struct RecommendationsRequest {
    pub user_id: i32,
    pub limit: Option<usize>,
    pub exclude_completed: Option<bool>,
}

/// Ответ с рекомендациями
#[derive(Debug, Serialize)]
pub struct RecommendationsResponse {
    pub quests: Vec<QuestRecommendation>,
    pub reasoning: String,
    pub processing_time_ms: u64,
}

#[derive(Debug, Serialize)]
pub struct QuestRecommendation {
    pub title: String,
    pub description: String,
    pub difficulty: u8,
    pub estimated_time_minutes: u32,
    pub tags: Vec<String>,
    pub score: f32,  // Relevance score 0.0-1.0
    pub reasons: Vec<String>,  // Why this quest is recommended
}

/// Конфигурация ML inference
#[derive(Debug, Clone, Serialize)]
pub struct MlConfig {
    pub tags_confidence_threshold: f32,
    pub difficulty_confidence_threshold: f32,
    pub transform_confidence_threshold: f32,
    pub enable_human_in_loop: bool,
}

impl Default for MlConfig {
    fn default() -> Self {
        Self {
            tags_confidence_threshold: 0.7,
            difficulty_confidence_threshold: 0.6,
            transform_confidence_threshold: 0.5,
            enable_human_in_loop: true,
        }
    }
}

