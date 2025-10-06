use axum::{
    extract::{Query, State},
    Json,
};
use serde::Deserialize;
use crate::{state::AppState, error::AppError, services::search};

#[derive(Deserialize)]
pub struct SearchQuery {
    pub q: String,
    pub tags: Option<Vec<String>>,
}

pub async fn search(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
    Query(query): Query<SearchQuery>,
) -> Result<Json<search::SearchResults>, AppError> {
    // Попробуем получить результаты из кеша
    let cache_key = format!("search:{}:{}:{:?}", user_id, query.q, query.tags);
    if let Ok(Some(cached_results)) = state.cache.get(&cache_key).await {
        return Ok(Json(cached_results));
    }

    let results = search::search_quests_and_tasks(
        &state.db,
        *user_id,
        &query.q,
        query.tags,
    ).await?;

    // Кешируем результаты на короткое время
    let _ = state.cache.set(&cache_key, &results, std::time::Duration::from_secs(30)).await;

    Ok(Json(results))
}

pub async fn get_tags(
    State(state): State<AppState>,
    user_id: axum::Extension<i32>,
) -> Result<Json<Vec<search::TagCount>>, AppError> {
    // Попробуем получить теги из кеша
    let cache_key = format!("tags:user:{}", user_id);
    if let Ok(Some(cached_tags)) = state.cache.get(&cache_key).await {
        return Ok(Json(cached_tags));
    }

    let tags = search::get_popular_tags(&state.db, *user_id).await?;

    // Кешируем теги на 5 минут
    let _ = state.cache.set(&cache_key, &tags, std::time::Duration::from_secs(300)).await;

    Ok(Json(tags))
}
