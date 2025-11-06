/// Handlers для ML Inference endpoints
use axum::{
    extract::{Extension, State},
    http::StatusCode,
    Json,
};
use crate::error::AppError;
use crate::models::ml_inference::*;
use crate::middleware::auth::CurrentUser;
use crate::services::MlInferenceService;
use crate::state::AppState;

/// POST /api/ml/tags - Определить теги для текста
pub async fn predict_tags(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<TagsRequest>,
) -> Result<Json<TagsResponse>, AppError> {
    let ml_service = MlInferenceService::new(MlConfig::default());
    
    let max_tags = request.max_tags.unwrap_or(5);
    let response = ml_service.predict_tags(&request.text, max_tags).await;
    
    tracing::info!(
        "Tags predicted: {} tags, processing time: {}ms",
        response.tags.len(),
        response.processing_time_ms
    );
    
    Ok(Json(response))
}

/// POST /api/ml/difficulty - Оценить сложность задачи
pub async fn predict_difficulty(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<DifficultyRequest>,
) -> Result<Json<DifficultyResponse>, AppError> {
    let ml_service = MlInferenceService::new(MlConfig::default());
    
    let response = ml_service
        .predict_difficulty(&request.title, request.description.as_deref())
        .await;
    
    tracing::info!(
        "Difficulty predicted: {} (confidence: {:.2}), requires_review: {}",
        response.difficulty,
        response.confidence,
        response.requires_review
    );
    
    Ok(Json(response))
}

/// POST /api/ml/transform - Трансформировать ToDo в квест
pub async fn transform_to_quest(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<TransformRequest>,
) -> Result<Json<TransformResponse>, AppError> {
    let ml_service = MlInferenceService::new(MlConfig::default());
    
    // Получить уровень пользователя если доступен
    let user_level = if let Some(user) = current_user {
        Some(user.0.level.try_into().unwrap_or(1))
    } else {
        request.user_level
    };
    
    let response = ml_service
        .transform_to_quest(
            &request.title,
            request.description.as_deref(),
            request.difficulty,
            user_level,
            request.preferred_style,
        )
        .await;
    
    tracing::info!(
        "Quest transformed: '{}' (confidence: {:.2}), requires_review: {}",
        response.fantasy_title,
        response.confidence,
        response.requires_review
    );
    
    Ok(Json(response))
}

/// POST /api/ml/recommendations - Получить персональные рекомендации
pub async fn get_recommendations(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<RecommendationsRequest>,
) -> Result<Json<RecommendationsResponse>, AppError> {
    // Проверить что пользователь авторизован или user_id в запросе совпадает
    let user_id = if let Some(user) = current_user {
        user.0.id
    } else {
        request.user_id
    };
    
    let ml_service = MlInferenceService::new(MlConfig::default());
    
    let limit = request.limit.unwrap_or(10);
    let response = ml_service.get_recommendations(user_id, limit).await;
    
    tracing::info!(
        "Recommendations generated for user {}: {} quests",
        user_id,
        response.quests.len()
    );
    
    Ok(Json(response))
}

/// GET /api/ml/health - Проверить работоспособность ML сервиса
pub async fn ml_health_check() -> Result<StatusCode, AppError> {
    // Простая проверка что ML сервис работает
    let ml_service = MlInferenceService::new(MlConfig::default());
    
    // Быстрый тест
    let _test_response = ml_service.predict_tags("тест", 1).await;
    
    Ok(StatusCode::OK)
}

/// GET /api/ml/config - Получить конфигурацию ML
pub async fn get_ml_config() -> Result<Json<MlConfig>, AppError> {
    let config = MlConfig::default();
    Ok(Json(config))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::User;

    #[tokio::test]
    async fn test_predict_tags_handler() {
        let request = TagsRequest {
            text: "купить продукты в магазине".to_string(),
            max_tags: Some(5),
        };
        
        // Тест логики сервиса
        let ml_service = MlInferenceService::new(MlConfig::default());
        let response = ml_service.predict_tags(&request.text, 5).await;
        
        assert!(!response.tags.is_empty());
    }
}

