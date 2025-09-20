use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use serde::{Deserialize, Serialize};
use std::sync::Arc;

use crate::{
    rag::templates::{auto_difficulty_for_text, auto_tags_for_text, is_boss_marker},
    rag::templates::QuestTemplates,
    AppState,
};

#[derive(Debug, Deserialize)]
pub struct DatasetTodos {
    pub todos: Vec<String>,
    pub context: Option<String>,
    pub difficulty_preference: Option<i32>,
}

#[derive(Debug, Serialize)]
pub struct TodoQuestPair {
    pub todo_text: String,
    pub quest: crate::models::QuestGenerationResponse,
}

#[derive(Debug, Deserialize)]
pub struct TagDatasetRequest {
    pub tasks: Vec<String>,
}

#[derive(Debug, Serialize)]
pub struct TagRecord {
    pub task_text: String,
    pub tags: Vec<String>,
    pub estimated_difficulty: i32,
    pub is_boss: bool,
}

pub async fn dataset_todo_to_quest(
    State(_state): State<Arc<AppState>>,
    ExtractJson(req): ExtractJson<DatasetTodos>,
) -> Result<Json<Vec<TodoQuestPair>>, StatusCode> {
    let mut pairs = Vec::with_capacity(req.todos.len());
    for todo in req.todos {
        // Use template-based generator from RAG templates (auto-detects theme)
        let quest = QuestTemplates::generate_quest_from_todo(
            &todo,
            req.context.as_deref(),
            req.difficulty_preference.unwrap_or(3).clamp(1, 5),
            1,
        );
        pairs.push(TodoQuestPair { todo_text: todo, quest });
    }
    Ok(Json(pairs))
}

pub async fn dataset_task_tags(
    State(_state): State<Arc<AppState>>,
    ExtractJson(req): ExtractJson<TagDatasetRequest>,
) -> Result<Json<Vec<TagRecord>>, StatusCode> {
    let mut records = Vec::with_capacity(req.tasks.len());
    for task in req.tasks {
        let diff = auto_difficulty_for_text(&task);
        let tags = auto_tags_for_text(&task);
        let is_boss = is_boss_marker(&task);
        records.push(TagRecord { task_text: task, tags, estimated_difficulty: diff, is_boss });
    }
    Ok(Json(records))
}