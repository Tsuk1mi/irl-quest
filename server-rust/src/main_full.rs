use axum::serve;
use std::net::SocketAddr;
use dotenv::dotenv;
use tokio::net::TcpListener;
use tower_http::trace::TraceLayer;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

mod config;
mod db;
mod error;
mod handlers;
mod middleware;
mod routes;
mod validation;
mod cache;
mod state;
mod services;

use state::AppState;

pub fn setup_logging() {
    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "example_tracing_aka_logging=debug,tower_http=debug".into()),
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

    // Подключение к базе данных
    let pool = db::create_database_pool(&config.database_url)
        .await
        .expect("Failed to create database pool");

    // Инициализация кеша
    let cache = cache::Cache::new(&config.redis_url)
        .expect("Failed to create Redis cache");

    // Создание состояния приложения
    let state = AppState::new(pool, cache);

    // Создание роутера с настроенными маршрутами
    let app = routes::app_router(state)
        .layer(middleware::cors::create_cors_layer())
        .layer(TraceLayer::new_for_http());

    // Запуск сервера
    let addr = SocketAddr::from(([127, 0, 0, 1], 8000));
    tracing::debug!("listening on {}", addr);
    let listener = TcpListener::bind(addr).await?;
    serve(listener, app).await?;

    Ok(())
}
