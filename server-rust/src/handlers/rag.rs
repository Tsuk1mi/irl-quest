use axum::{
    extract::State,
    Extension,
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};

use crate::{
    models::{
        QuestGenerationRequest, QuestGenerationResponse,
        TaskEnhancementRequest, TaskEnhancementResponse
    },
    rag::RagService,
    AppState,
};
use crate::middleware::auth::CurrentUser;
use serde::{Deserialize, Serialize};

#[derive(Debug, Deserialize)]
pub struct ClassifyRequest {
    pub task_text: String,
    pub context: Option<String>,
    pub user_level: Option<i32>,
}

#[derive(Debug, Serialize)]
pub struct ClassifyResponse {
    pub tags: Vec<String>,
    pub estimated_difficulty: i32,
    pub exam_tasks: Vec<crate::models::GeneratedTask>,
}

pub async fn generate_quest(
    State(state): State<AppState>,
    Extension(CurrentUser(user)): Extension<CurrentUser>,
    ExtractJson(mut request): ExtractJson<QuestGenerationRequest>,
) -> Result<Json<QuestGenerationResponse>, StatusCode> {
    // Add user level to request
    request.user_level = Some(user.level);
    
    let rag_service = RagService::new(state.db.clone(), state.ml_client.clone());

    match rag_service.generate_quest_from_todo(request).await {
        Ok(response) => Ok(Json(response)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn enhance_task(
    State(state): State<AppState>,
    Extension(CurrentUser(user)): Extension<CurrentUser>,
    ExtractJson(mut request): ExtractJson<TaskEnhancementRequest>,
) -> Result<Json<TaskEnhancementResponse>, StatusCode> {
    // Add user level to request
    request.user_level = Some(user.level);
    
    let rag_service = RagService::new(state.db.clone(), state.ml_client.clone());

    match rag_service.enhance_task(request).await {
        Ok(response) => Ok(Json(response)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn classify_task(
    State(state): State<AppState>,
    Extension(CurrentUser(user)): Extension<CurrentUser>,
    ExtractJson(req): ExtractJson<ClassifyRequest>,
) -> Result<Json<ClassifyResponse>, StatusCode> {
    let user_level = req.user_level.unwrap_or(user.level);
    let rag_service = RagService::new(state.db.clone(), state.ml_client.clone());

    match rag_service.classify_task_and_generate_exam(&req.task_text, req.context.as_deref(), user_level).await {
        Ok((tags, difficulty, exam_tasks)) => Ok(Json(ClassifyResponse { tags, estimated_difficulty: difficulty, exam_tasks })),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}
