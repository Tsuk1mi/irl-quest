mod config;
mod db;
mod handlers;
mod middleware;
mod models;
mod services;

use std::sync::Arc;

use axum::{
    middleware::from_fn_with_state,
    routing::{delete, get, post, put},
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
use handlers::{auth, health, quest, task, user};
use middleware::auth_middleware;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub settings: Settings,
}

async fn create_app(state: Arc<AppState>) -> Router {
    let public_auth_routes = Router::new()
        .route("/register", post(auth::register))
        .route("/token", post(auth::login));

    let protected_auth_routes = Router::new()
        .route("/me", get(auth::me))
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let auth_routes = Router::new()
        .merge(public_auth_routes)
        .merge(protected_auth_routes);

    let task_routes = Router::new()
        .route("/", get(task::list_tasks).post(task::create_task))
        .route(
            "/:task_id",
            get(task::get_task)
                .put(task::update_task)
                .delete(task::delete_task),
        )
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let quest_routes = Router::new()
        .route("/", get(quest::list_quests).post(quest::create_quest))
        .route(
            "/:quest_id",
            get(quest::get_quest)
                .put(quest::update_quest)
                .delete(quest::delete_quest),
        )
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let user_routes = Router::new()
        .route("/me", get(user::get_me).put(user::update_me))
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let api_routes = Router::new()
        .nest("/auth", auth_routes)
        .nest("/tasks", task_routes)
        .nest("/quests", quest_routes)
        .nest("/users", user_routes);

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

    // Create database pool
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