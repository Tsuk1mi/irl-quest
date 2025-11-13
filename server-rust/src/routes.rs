use axum::{
    http::StatusCode,
    middleware,
    routing::{get, patch, post, put},
    Router,
};

use crate::{
    handlers::{
        auction, auth_extended, auth_handlers, character, config, coop_missions, dice, geolocation,
        guilds, ml, ml_inference, ml_quest_generation, ml_verification, profile, quest_handlers,
        quest_suggestions, rag, search_handlers, stats, task_handlers, websocket,
    },
    middleware::{auth::auth_middleware, ip_blacklist_middleware, rate_limit_middleware},
    state::AppState,
};

pub fn app_router(state: AppState) -> Router {
    let public_routes = Router::new()
        .route("/api/auth/register", post(auth_handlers::register))
        .route("/api/auth/login", post(auth_extended::login_extended))
        .route("/api/auth/refresh", post(auth_extended::refresh_token))
        .route("/api/auth/mfa/verify", post(auth_extended::verify_mfa))
        .route("/api/auth/oauth/login", post(auth_extended::oauth_login))
        // Simple health check for network/debugging
        .route("/health", get(|| async { StatusCode::OK }))
        // Client configuration endpoint
        .route("/api/config", get(config::get_client_config))
        // версии с префиксом /api/v1
        .route("/api/v1/auth/register", post(auth_handlers::register))
        .route("/api/v1/auth/login", post(auth_extended::login_extended))
        .route("/api/v1/auth/refresh", post(auth_extended::refresh_token))
        .route("/api/v1/auth/mfa/verify", post(auth_extended::verify_mfa))
        .route("/api/v1/auth/oauth/login", post(auth_extended::oauth_login))
        .route("/api/v1/config", get(config::get_client_config))
        // ML public endpoints
        .route("/api/ml/embeddings", post(ml::embeddings))
        .route("/api/ml/infer", post(ml::infer))
        .route("/api/ml/export_rag", get(ml::export_rag))
        .route(
            "/api/ml/dataset/todo_to_quest",
            post(ml::dataset_todo_to_quest),
        )
        .route("/api/ml/dataset/task_tags", post(ml::dataset_task_tags))
        // v1 aliases
        .route("/api/v1/ml/embeddings", post(ml::embeddings))
        .route("/api/v1/ml/infer", post(ml::infer))
        .route("/api/v1/ml/export_rag", get(ml::export_rag))
        .route(
            "/api/v1/ml/dataset/todo_to_quest",
            post(ml::dataset_todo_to_quest),
        )
        .route("/api/v1/ml/dataset/task_tags", post(ml::dataset_task_tags))
        .with_state(state.clone());

    let protected_routes = Router::new()
        // Auth endpoints
        .route("/api/auth/me", get(auth_handlers::me))
        .route("/api/auth/logout", post(auth_extended::logout))
        .route(
            "/api/auth/sessions",
            get(auth_extended::get_active_sessions),
        )
        .route(
            "/api/auth/sessions/:token",
            axum::routing::delete(auth_extended::revoke_session),
        )
        // MFA endpoints - требуют аутентификации
        // .route("/api/auth/mfa/setup", get(auth_extended::setup_mfa))
        // .route("/api/auth/mfa/enable", post(auth_extended::enable_mfa))
        // .route("/api/auth/mfa/disable", post(auth_extended::disable_mfa))
        // /api/v1 aliases
        .route("/api/v1/auth/me", get(auth_handlers::me))
        .route("/api/v1/auth/logout", post(auth_extended::logout))
        .route(
            "/api/v1/auth/sessions",
            get(auth_extended::get_active_sessions),
        )
        // .route("/api/v1/auth/mfa/setup", get(auth_extended::setup_mfa))
        // .route("/api/v1/auth/mfa/enable", post(auth_extended::enable_mfa))
        // .route("/api/v1/auth/mfa/disable", post(auth_extended::disable_mfa))
        // Квесты
        .route("/api/quests", post(quest_handlers::create_quest))
        .route("/api/quests", get(quest_handlers::list_quests))
        .route("/api/quests/:id", get(quest_handlers::get_quest))
        .route("/api/quests/:id", post(quest_handlers::update_quest))
        // Автогенерация и предложения квестов
        .route(
            "/api/quests/suggestions/daily",
            get(quest_suggestions::get_daily_quest_suggestion),
        )
        .route(
            "/api/quests/suggestions/weekly",
            get(quest_suggestions::get_weekly_quest_suggestion),
        )
        .route(
            "/api/quests/suggestions/merge",
            get(quest_suggestions::get_merge_suggestions),
        )
        .route(
            "/api/quests/suggestions/daily/accept",
            post(quest_suggestions::accept_daily_quest),
        )
        // версии с префиксом /api/v1
        .route("/api/v1/quests", post(quest_handlers::create_quest))
        .route("/api/v1/quests", get(quest_handlers::list_quests))
        .route("/api/v1/quests/:id", get(quest_handlers::get_quest))
        .route("/api/v1/quests/:id", post(quest_handlers::update_quest))
        .route(
            "/api/v1/quests/suggestions/daily",
            get(quest_suggestions::get_daily_quest_suggestion),
        )
        .route(
            "/api/v1/quests/suggestions/weekly",
            get(quest_suggestions::get_weekly_quest_suggestion),
        )
        .route(
            "/api/v1/quests/suggestions/merge",
            get(quest_suggestions::get_merge_suggestions),
        )
        // RAG - protected endpoints
        .route("/api/rag/generate_quest", post(rag::generate_quest))
        .route("/api/rag/enhance_task", post(rag::enhance_task))
        .route("/api/rag/classify_task", post(rag::classify_task))
        // /api/v1 aliases
        .route("/api/v1/rag/generate_quest", post(rag::generate_quest))
        .route("/api/v1/rag/enhance_task", post(rag::enhance_task))
        .route("/api/v1/rag/classify_task", post(rag::classify_task))
        // Задачи
        .route("/api/tasks", post(task_handlers::create_task))
        .route("/api/tasks", get(task_handlers::list_tasks))
        .route(
            "/api/tasks/:id/complete",
            post(task_handlers::complete_task),
        )
        // /api/v1 tasks
        .route("/api/v1/tasks", post(task_handlers::create_task))
        .route("/api/v1/tasks", get(task_handlers::list_tasks))
        .route(
            "/api/v1/tasks/:id/complete",
            post(task_handlers::complete_task),
        )
        // Поиск и теги
        .route("/api/search", get(search_handlers::search))
        .route("/api/tags", get(search_handlers::get_tags))
        // /api/v1 search/tags
        .route("/api/v1/search", get(search_handlers::search))
        .route("/api/v1/tags", get(search_handlers::get_tags))
        // ML Inference endpoints (новые)
        .route("/api/ml/tags", post(ml_inference::predict_tags))
        .route("/api/ml/difficulty", post(ml_inference::predict_difficulty))
        .route("/api/ml/transform", post(ml_inference::transform_to_quest))
        .route(
            "/api/ml/recommendations",
            post(ml_inference::get_recommendations),
        )
        .route("/api/ml/health", get(ml_inference::ml_health_check))
        .route("/api/ml/config", get(ml_inference::get_ml_config))
        // /api/v1 aliases
        .route("/api/v1/ml/tags", post(ml_inference::predict_tags))
        .route(
            "/api/v1/ml/difficulty",
            post(ml_inference::predict_difficulty),
        )
        .route(
            "/api/v1/ml/transform",
            post(ml_inference::transform_to_quest),
        )
        .route(
            "/api/v1/ml/recommendations",
            post(ml_inference::get_recommendations),
        )
        // Dice system endpoints
        .route("/api/dice/roll", post(dice::roll_dice))
        .route("/api/dice/roll/multi", post(dice::roll_multi_dice))
        .route("/api/dice/skill-check", post(dice::skill_check))
        .route("/api/dice/types", get(dice::get_dice_types))
        .route("/api/dice/skills", get(dice::get_skills))
        // /api/v1 aliases
        .route("/api/v1/dice/roll", post(dice::roll_dice))
        .route("/api/v1/dice/roll/multi", post(dice::roll_multi_dice))
        .route("/api/v1/dice/skill-check", post(dice::skill_check))
        .route("/api/v1/dice/types", get(dice::get_dice_types))
        .route("/api/v1/dice/skills", get(dice::get_skills))
        // Character system endpoints
        .route(
            "/api/character/profile",
            get(character::get_character_profile),
        )
        .route("/api/character/select", post(character::select_class_race))
        .route(
            "/api/character/increase-stat",
            post(character::increase_stat),
        )
        .route("/api/character/level-up", post(character::level_up))
        // /api/v1 aliases
        .route(
            "/api/v1/character/profile",
            get(character::get_character_profile),
        )
        .route(
            "/api/v1/character/select",
            post(character::select_class_race),
        )
        .route(
            "/api/v1/character/increase-stat",
            post(character::increase_stat),
        )
        .route("/api/v1/character/level-up", post(character::level_up))
        // WebSocket endpoint для realtime коммуникации
        .route("/ws", get(websocket::websocket_handler))
        .route("/api/v1/ws", get(websocket::websocket_handler))
        // Геолокация и AR
        .route("/api/geo/zones", post(geolocation::create_geo_zone))
        .route("/api/geo/check", post(geolocation::check_location))
        .route(
            "/api/images/verify",
            post(geolocation::upload_verification_image),
        )
        .route("/api/privacy/consent", post(geolocation::give_consent))
        // /api/v1 aliases
        .route("/api/v1/geo/zones", post(geolocation::create_geo_zone))
        .route("/api/v1/geo/check", post(geolocation::check_location))
        .route(
            "/api/v1/images/verify",
            post(geolocation::upload_verification_image),
        )
        .route("/api/v1/privacy/consent", post(geolocation::give_consent))
        // Гильдии
        .route("/api/guilds", get(guilds::list_guilds))
        .route("/api/guilds", post(guilds::create_guild))
        .route("/api/guilds/:id", get(guilds::get_guild))
        .route("/api/guilds/:id/members", get(guilds::get_guild_members))
        .route("/api/guilds/:id/join", post(guilds::join_guild))
        .route("/api/guilds/:id/leave", post(guilds::leave_guild))
        .route("/api/v1/guilds", get(guilds::list_guilds))
        .route("/api/v1/guilds", post(guilds::create_guild))
        .route("/api/v1/guilds/:id", get(guilds::get_guild))
        .route("/api/v1/guilds/:id/members", get(guilds::get_guild_members))
        .route("/api/v1/guilds/:id/join", post(guilds::join_guild))
        .route("/api/v1/guilds/:id/leave", post(guilds::leave_guild))
        // Кооперативные миссии
        .route("/api/coop/missions", get(coop_missions::list_coop_missions))
        .route(
            "/api/coop/missions",
            post(coop_missions::create_coop_mission),
        )
        .route(
            "/api/coop/missions/:id",
            get(coop_missions::get_coop_mission),
        )
        .route(
            "/api/coop/missions/join",
            post(coop_missions::join_coop_mission),
        )
        .route(
            "/api/coop/missions/:id/leave",
            post(coop_missions::leave_coop_mission),
        )
        .route(
            "/api/v1/coop/missions",
            get(coop_missions::list_coop_missions),
        )
        .route(
            "/api/v1/coop/missions",
            post(coop_missions::create_coop_mission),
        )
        .route(
            "/api/v1/coop/missions/:id",
            get(coop_missions::get_coop_mission),
        )
        .route(
            "/api/v1/coop/missions/join",
            post(coop_missions::join_coop_mission),
        )
        .route(
            "/api/v1/coop/missions/:id/leave",
            post(coop_missions::leave_coop_mission),
        )
        // Аукцион
        .route("/api/auction/listings", get(auction::get_auction_listings))
        .route(
            "/api/auction/listings",
            post(auction::create_auction_listing),
        )
        .route(
            "/api/auction/purchase",
            post(auction::purchase_auction_listing),
        )
        .route(
            "/api/v1/auction/listings",
            get(auction::get_auction_listings),
        )
        .route(
            "/api/v1/auction/listings",
            post(auction::create_auction_listing),
        )
        .route(
            "/api/v1/auction/purchase",
            post(auction::purchase_auction_listing),
        )
        // ML генерация и верификация квестов
        .route(
            "/api/ml/generate-quest",
            post(ml_quest_generation::ml_generate_quest),
        )
        .route(
            "/api/v1/ml/generate-quest",
            post(ml_quest_generation::ml_generate_quest),
        )
        .route(
            "/api/ml/quest-verification",
            post(ml_verification::request_quest_verification),
        )
        .route("/api/ml/verify-quiz", post(ml_verification::verify_quiz))
        .route("/api/ml/verify-photo", post(ml_verification::verify_photo))
        .route(
            "/api/v1/ml/quest-verification",
            post(ml_verification::request_quest_verification),
        )
        .route("/api/v1/ml/verify-quiz", post(ml_verification::verify_quiz))
        .route(
            "/api/v1/ml/verify-photo",
            post(ml_verification::verify_photo),
        )
        // Статистика
        .route("/api/stats/daily", get(stats::get_daily_stats))
        .route("/api/stats/total", get(stats::get_total_stats))
        .route("/api/v1/stats/daily", get(stats::get_daily_stats))
        .route("/api/v1/stats/total", get(stats::get_total_stats))
        // Профиль
        .route("/api/auth/profile", put(profile::update_profile))
        .route("/api/auth/profile", patch(profile::update_profile))
        .route("/api/v1/auth/profile", put(profile::update_profile))
        .route("/api/v1/auth/profile", patch(profile::update_profile))
        .layer(middleware::from_fn_with_state(
            state.clone(),
            auth_middleware,
        ))
        .with_state(state.clone());

    // Public character info endpoints
    let character_public = Router::new()
        .route(
            "/api/character/classes",
            get(character::get_available_classes),
        )
        .route("/api/character/races", get(character::get_available_races))
        .route(
            "/api/v1/character/classes",
            get(character::get_available_classes),
        )
        .route(
            "/api/v1/character/races",
            get(character::get_available_races),
        )
        .with_state(state.clone());

    Router::new()
        .merge(public_routes)
        .merge(protected_routes)
        .merge(character_public)
        // Применить rate limiting ко всем роутам
        .layer(middleware::from_fn_with_state(
            state.rate_limiter.clone(),
            rate_limit_middleware,
        ))
        // Применить IP blacklist проверку
        .layer(middleware::from_fn_with_state(
            state.ip_blacklist.clone(),
            ip_blacklist_middleware,
        ))
        .with_state(state)
}
