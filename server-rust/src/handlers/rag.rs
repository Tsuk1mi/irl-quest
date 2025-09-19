use axum::{
    extract::State,
    http::StatusCode,
    response::Json,
    Json as ExtractJson,
};
use std::sync::Arc;

use crate::{
    middleware::CurrentUser,
    models::{
        QuestGenerationRequest, QuestGenerationResponse,
        TaskEnhancementRequest, TaskEnhancementResponse,
        User
    },
    rag::RagService,
    AppState,
};

pub async fn generate_quest(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    ExtractJson(mut request): ExtractJson<QuestGenerationRequest>,
) -> Result<Json<QuestGenerationResponse>, StatusCode> {
    // Add user level to request
    request.user_level = Some(user.level);
    
    let rag_service = RagService::new(state.db.clone());
    
    match rag_service.generate_quest_from_todo(request).await {
        Ok(response) => Ok(Json(response)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

pub async fn enhance_task(
    State(state): State<Arc<AppState>>,
    CurrentUser(user): CurrentUser,
    ExtractJson(mut request): ExtractJson<TaskEnhancementRequest>,
) -> Result<Json<TaskEnhancementResponse>, StatusCode> {
    // Add user level to request
    request.user_level = Some(user.level);
    
    let rag_service = RagService::new(state.db.clone());
    
    match rag_service.enhance_task(request).await {
        Ok(response) => Ok(Json(response)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}