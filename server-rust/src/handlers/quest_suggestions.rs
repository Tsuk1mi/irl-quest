use axum::{extract::State, http::StatusCode, response::IntoResponse, Extension, Json};
use serde::{Deserialize, Serialize};

use crate::middleware::auth::CurrentUser;
use crate::models::Quest;
use crate::services::quest_analyzer::QuestAnalyzer;
use crate::state::AppState;

#[derive(Serialize)]
pub struct QuestSuggestionResponse {
    pub quest: Option<Quest>,
    pub message: String,
}

#[derive(Serialize)]
pub struct MergeSuggestionsResponse {
    pub suggestions: Vec<MergeSuggestion>,
}

#[derive(Serialize)]
pub struct MergeSuggestion {
    pub quest_ids: Vec<i32>,
    pub suggested_title: String,
}

#[derive(Deserialize)]
pub struct AcceptQuestRequest {
    pub title: String,
    pub description: Option<String>,
}

pub async fn get_daily_quest_suggestion(
    State(state): State<AppState>,
    Extension(user): Extension<Option<CurrentUser>>,
) -> impl IntoResponse {
    let user = match user {
        Some(u) => u,
        None => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(QuestSuggestionResponse {
                    quest: None,
                    message: "Unauthorized".to_string(),
                }),
            )
                .into_response()
        }
    };
    match QuestAnalyzer::generate_daily_quest(&state.db, user.0.id).await {
        Ok(quest) => (
            StatusCode::OK,
            Json(QuestSuggestionResponse {
                quest,
                message: "Suggestion generated".to_string(),
            }),
        )
            .into_response(),
        Err(e) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({
                "error": format!("Failed to generate suggestion: {}", e)
            })),
        )
            .into_response(),
    }
}

pub async fn get_weekly_quest_suggestion(
    State(state): State<AppState>,
    Extension(user): Extension<Option<CurrentUser>>,
) -> impl IntoResponse {
    let user = match user {
        Some(u) => u,
        None => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(QuestSuggestionResponse {
                    quest: None,
                    message: "Unauthorized".to_string(),
                }),
            )
                .into_response()
        }
    };
    match QuestAnalyzer::generate_weekly_quest(&state.db, user.0.id).await {
        Ok(quest) => (
            StatusCode::OK,
            Json(QuestSuggestionResponse {
                quest,
                message: "Weekly suggestion generated".to_string(),
            }),
        )
            .into_response(),
        Err(e) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({
                "error": format!("Failed to generate suggestion: {}", e)
            })),
        )
            .into_response(),
    }
}

pub async fn get_merge_suggestions(
    State(state): State<AppState>,
    Extension(user): Extension<Option<CurrentUser>>,
) -> impl IntoResponse {
    let user = match user {
        Some(u) => u,
        None => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(MergeSuggestionsResponse {
                    suggestions: vec![],
                }),
            )
                .into_response()
        }
    };
    match QuestAnalyzer::get_merge_suggestions(&state.db, user.0.id).await {
        Ok(suggestions) => {
            let response = MergeSuggestionsResponse {
                suggestions: suggestions
                    .into_iter()
                    .map(|(ids, title)| MergeSuggestion {
                        quest_ids: ids,
                        suggested_title: title,
                    })
                    .collect(),
            };
            (StatusCode::OK, Json(response)).into_response()
        }
        Err(e) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({
                "error": format!("Failed to get merge suggestions: {}", e)
            })),
        )
            .into_response(),
    }
}

pub async fn accept_daily_quest(
    State(_state): State<AppState>,
    Extension(_user): Extension<Option<CurrentUser>>,
    Json(_request): Json<AcceptQuestRequest>,
) -> impl IntoResponse {
    // TODO: Реализовать принятие предложенного квеста
    (
        StatusCode::NOT_IMPLEMENTED,
        Json(serde_json::json!({
            "message": "Quest acceptance not yet implemented"
        })),
    )
}
