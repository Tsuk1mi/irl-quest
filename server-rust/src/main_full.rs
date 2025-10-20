use std::net::SocketAddr;
use axum::serve;
use tokio::net::TcpListener;
use dotenv::dotenv;
use tower_http::cors::CorsLayer;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

mod config;
mod db;
mod error;
mod handlers;
mod middleware;
mod models;
mod routes;
mod services;
mod validation;
mod state;
mod rag;
mod utils_impl;

use state::AppState;

pub fn setup_logging() {
    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info,tower_http=debug".into()),
        )
        .with(tracing_subscriber::fmt::layer())
        .init();
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Загрузка переменных окружения
    dotenv().ok();

    // Настройка логирования
    setup_logging();

    // Загрузка конфигурации
    let config = config::load_config()?;
    tracing::info!("Loaded configuration");

    // Подключение к базе данных
    tracing::info!("Connecting to database at {}", config.database_url);
    let pool = db::create_database_pool(&config.database_url)
        .await
        .expect("Failed to create database pool");
    tracing::info!("Successfully connected to database");

    // Seed test user (non-fatal)
    match crate::db::seed_test_user(&pool).await {
        Ok(_) => tracing::info!("Test user ensured in DB"),
        Err(e) => tracing::warn!("Failed to seed test user: {:?}", e),
    }

    // Создание состояния приложения
    let state = AppState::new(pool);

    // Создание роутера с настроенными маршрутами
    let app = routes::app_router(state)
        .layer(
            CorsLayer::new()
                .allow_origin(tower_http::cors::Any)
                .allow_methods(tower_http::cors::Any)
                .allow_headers(tower_http::cors::Any)
        );

    // Запуск сервера
    let addr = SocketAddr::from(([0, 0, 0, 0], config.port));
    tracing::info!("Starting server on {}", addr);
    let listener = TcpListener::bind(addr).await?;
    serve(listener, app).await?;

    Ok(())
}
