use axum::{
    middleware,
    routing::{get, post},
    Router,
};

use crate::{
    handlers::{auth_handlers, quest_handlers, task_handlers, search_handlers},
    middleware::auth::auth_middleware,
    state::AppState,
};

pub fn app_router(state: AppState) -> Router {
    let public_routes = Router::new()
        .route("/api/auth/register", post(auth_handlers::register))
        .route("/api/auth/login", post(auth_handlers::login))
        // версии с префиксом /api/v1
        .route("/api/v1/auth/register", post(auth_handlers::register))
        .route("/api/v1/auth/login", post(auth_handlers::login))
        .with_state(state.clone());

    let protected_routes = Router::new()
        // Квесты
        .route("/api/quests", post(quest_handlers::create_quest))
        .route("/api/quests", get(quest_handlers::list_quests))
        .route("/api/quests/:id", get(quest_handlers::get_quest))
        .route("/api/quests/:id", post(quest_handlers::update_quest))
        // версии с префиксом /api/v1
        .route("/api/v1/quests", post(quest_handlers::create_quest))
        .route("/api/v1/quests", get(quest_handlers::list_quests))
        .route("/api/v1/quests/:id", get(quest_handlers::get_quest))
        .route("/api/v1/quests/:id", post(quest_handlers::update_quest))

        // Задачи
        .route("/api/tasks", post(task_handlers::create_task))
        .route("/api/tasks", get(task_handlers::list_tasks))
        .route("/api/tasks/:id/complete", post(task_handlers::complete_task))
        // /api/v1 tasks
        .route("/api/v1/tasks", post(task_handlers::create_task))
        .route("/api/v1/tasks", get(task_handlers::list_tasks))
        .route("/api/v1/tasks/:id/complete", post(task_handlers::complete_task))

        // Поиск и теги
        .route("/api/search", get(search_handlers::search))
        .route("/api/tags", get(search_handlers::get_tags))
        // /api/v1 search/tags
        .route("/api/v1/search", get(search_handlers::search))
        .route("/api/v1/tags", get(search_handlers::get_tags))

        .layer(middleware::from_fn_with_state(state.clone(), auth_middleware))
        .with_state(state.clone());

    Router::new()
        .merge(public_routes)
        .merge(protected_routes)
        .with_state(state)
}
