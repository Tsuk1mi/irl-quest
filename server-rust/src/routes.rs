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

pub fn create_router(state: AppState) -> Router {
    // Публичные маршруты
    let public_routes = Router::new()
        .route("/api/auth/register", post(auth_handlers::register))
        .route("/api/auth/login", post(auth_handlers::login));

    // Защищенные маршруты
    let protected_routes = Router::new()
        // Квесты
        .route("/api/quests", post(quest_handlers::create_quest))
        .route("/api/quests", get(quest_handlers::list_quests))
        .route("/api/quests/:id", get(quest_handlers::get_quest))

        // Задачи
        .route("/api/tasks", post(task_handlers::create_task))
        .route("/api/tasks", get(task_handlers::list_tasks))
        .route("/api/tasks/:id/complete", post(task_handlers::complete_task))

        // Поиск и теги
        .route("/api/search", get(search_handlers::search))
        .route("/api/tags", get(search_handlers::get_tags))

        .layer(middleware::from_fn_with_state(state.clone(), auth_middleware));

    // Объединяем все маршруты
    Router::new()
        .merge(public_routes)
        .merge(protected_routes)
        .with_state(state)
}
