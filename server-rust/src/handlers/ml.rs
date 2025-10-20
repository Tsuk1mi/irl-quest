use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use serde::{Deserialize, Serialize};

use crate::{
    rag::templates::{auto_difficulty_for_text, auto_tags_for_text, is_boss_marker},
    rag::templates::QuestTemplates,
    AppState,
};

use crate::models::RagKnowledgeOut;
use sqlx::query_as;

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

// New structs for ML endpoints
#[derive(Debug, Deserialize)]
pub struct EmbeddingsRequest {
    pub texts: Vec<String>,
}

#[derive(Debug, Serialize)]
pub struct EmbeddingsResponse {
    pub embeddings: Vec<Vec<f32>>,
}

#[derive(Debug, Deserialize)]
pub struct InferRequest {
    pub prompt: String,
}

#[derive(Debug, Serialize)]
pub struct InferResponse {
    pub result: String,
}

pub async fn dataset_todo_to_quest(
    State(_state): State<AppState>,
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
    State(_state): State<AppState>,
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

pub async fn embeddings(
    State(state): State<AppState>,
    ExtractJson(req): ExtractJson<EmbeddingsRequest>,
) -> Result<Json<EmbeddingsResponse>, StatusCode> {
    match state.ml_client.embed_texts(req.texts).await {
        Ok(emb) => Ok(Json(EmbeddingsResponse { embeddings: emb })),
        Err(e) => {
            tracing::error!("Embedding error: {:?}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

pub async fn infer(
    State(state): State<AppState>,
    ExtractJson(req): ExtractJson<InferRequest>,
) -> Result<Json<InferResponse>, StatusCode> {
    match state.ml_client.infer(req.prompt).await {
        Ok(res) => Ok(Json(InferResponse { result: res })),
        Err(e) => {
            tracing::error!("Infer error: {:?}", e);
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

pub async fn export_rag(
    State(state): State<AppState>,
) -> Result<Json<Vec<RagKnowledgeOut>>, StatusCode> {
    let records: Vec<crate::models::RagKnowledge> = query_as(
        "SELECT id, content, content_type, tags, embedding, metadata, created_at FROM rag_knowledge",
    )
    .fetch_all(&state.db)
    .await
    .map_err(|e| {
        tracing::error!("DB export_rag error: {:?}", e);
        StatusCode::INTERNAL_SERVER_ERROR
    })?;

    Ok(Json(records.into_iter().map(RagKnowledgeOut::from).collect()))
}
