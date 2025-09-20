mod config;
mod db;
mod handlers;
mod middleware;
mod models;
mod services;
mod rag;

use std::sync::Arc;

use axum::{
    middleware::from_fn_with_state,
    routing::{get, post, put, delete},
    Router,
};
use dotenv::dotenv;
use sqlx::PgPool;
use tokio::net::TcpListener;
use tower::ServiceBuilder;
use tower_http::{cors::CorsLayer, trace::TraceLayer};
use tracing::{info, Level};
use tracing_subscriber;

use config::Settings;
use db::create_database_pool;
use handlers::{auth, health, quest, task, user, rag as rag_handler, ml as ml_handler};
use middleware::auth_middleware;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub settings: Settings,
}

async fn create_app(state: Arc<AppState>) -> Router {
    // Public routes (no auth required)
    let public_auth_routes = Router::new()
        .route("/register", post(auth::register))
        .route("/login", post(auth::login));

    // Protected auth routes
    let protected_auth_routes = Router::new()
        .route("/me", get(auth::me).route_layer(from_fn_with_state(state.clone(), auth_middleware)))
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let auth_routes = Router::new()
        .merge(public_auth_routes)
        .merge(protected_auth_routes);

    // Task routes (all protected)
    let task_routes = Router::new()
        .route("/", get(task::list_tasks).post(task::create_task))
        .route("/:task_id", get(task::get_task).put(task::update_task).delete(task::delete_task))
        .route("/:task_id/complete", post(task::complete_task));

    // Quest routes (all protected)
    let quest_routes = Router::new()
        .route("/", get(quest::list_quests).post(quest::create_quest))
        .route("/:quest_id", get(quest::get_quest).put(quest::update_quest).delete(quest::delete_quest))
        .route("/:quest_id/complete", post(quest::complete_quest))
        .route("/generate", post(quest::generate_quest_from_todo));

    // User routes (all protected)
    let user_routes = Router::new()
        .route("/me", get(user::get_me).put(user::update_me))
        .route("/me/stats", get(user::get_user_stats))
        .route("/me/achievements", get(user::get_user_achievements));

    // RAG routes (all protected)
    let rag_routes = Router::new()
        .route("/generate-quest", post(rag_handler::generate_quest))
        .route("/enhance-task", post(rag_handler::enhance_task));

    // ML dataset routes (public for now; can protect later if needed)
    let ml_routes = Router::new()
        .route("/dataset/todo_to_quest", post(ml_handler::dataset_todo_to_quest))
        .route("/dataset/task_tags", post(ml_handler::dataset_task_tags));

    // API routes
    let api_routes = Router::new()
        .nest("/auth", auth_routes)
        .nest("/tasks", task_routes.route_layer(from_fn_with_state(state.clone(), auth_middleware)))
        .nest("/quests", quest_routes.route_layer(from_fn_with_state(state.clone(), auth_middleware)))
        .nest("/users", user_routes.route_layer(from_fn_with_state(state.clone(), auth_middleware)))
        .nest("/rag", rag_routes.route_layer(from_fn_with_state(state.clone(), auth_middleware)))
        .nest("/ml", ml_routes);

    // Main app
    Router::new()
        .route("/", get(health::root))
        .route("/health", get(health::health))
        .route("/ready", get(health::ready))
        .nest("/api/v1", api_routes)
        .layer(
            ServiceBuilder::new()
                .layer(TraceLayer::new_for_http())
                .layer(CorsLayer::permissive()),
        )
        .with_state(state)
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Load environment variables
    dotenv().ok();

    // Initialize tracing
    tracing_subscriber::fmt()
        .with_max_level(Level::INFO)
        .init();

    // Load settings
    let settings = Settings::new();
    info!("Starting server with settings: {:?}", settings);

    // Create database pool and run migrations
    let pool = create_database_pool(&settings.database_url).await?;
    info!("Database connection established");

    // Create application state
    let state = Arc::new(AppState {
        db: pool,
        settings: settings.clone(),
    });

    // Create application
    let app = create_app(state).await;

    // Create listener
    let addr = format!("{}:{}", settings.server_host, settings.server_port);
    let listener = TcpListener::bind(&addr).await?;
    info!("Server listening on {}", addr);

    // Start server
    axum::serve(listener, app).await?;

    Ok(())
}