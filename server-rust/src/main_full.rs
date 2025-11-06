use std::net::SocketAddr;
use std::path::PathBuf;
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
mod rag; // RAG service stubs
mod utils_impl;
mod ml;
mod utils;

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
    // Настройка логирования сначала
    setup_logging();

    // Загрузка переменных окружения из корня проекта
    // Проверяем несколько возможных путей
    let loaded = if let Ok(current) = std::env::current_dir() {
        // Путь 1: ../.env (если запуск из server-rust/)
        let parent_env = current.join("../.env");
        if parent_env.exists() {
            dotenv::from_path(&parent_env).ok();
            tracing::info!("Loaded .env from: {}", parent_env.display());
            true
        } 
        // Путь 2: .env (если запуск из корня проекта)
        else if current.join(".env").exists() {
            dotenv().ok();
            tracing::info!("Loaded .env from current directory");
            true
        }
        // Путь 3: server-rust/.env (локальный)
        else if current.join("server-rust/.env").exists() {
            dotenv::from_path(current.join("server-rust/.env")).ok();
            tracing::info!("Loaded .env from server-rust/");
            true
        }
        else {
            false
        }
    } else {
        false
    };

    if !loaded {
        tracing::warn!("No .env file found, using environment variables and defaults");
        dotenv().ok(); // Попытка загрузить из текущей директории
    }

    // Загрузка конфигурации
    let config = config::load_config()?;
    tracing::info!("Loaded configuration");
    
    // Логируем информацию об IP адресах
    if let Some(public_ip) = &config.public_ip {
        tracing::info!("🌐 Public IP detected: {}", public_ip);
        tracing::info!("📱 Mobile clients should use: http://{}:{}", public_ip, config.port);
    }
    if let Some(local_ip) = &config.local_ip {
        tracing::info!("🏠 Local IP detected: {}", local_ip);
    }
    
    // Логируем включенные фичи
    tracing::info!("🔐 Security features:");
    tracing::info!("  - Rate limiting: {}/min (burst: {})", 
        config.rate_limit_per_minute, config.rate_limit_burst);
    tracing::info!("  - MFA: {}", if config.enable_mfa { "enabled" } else { "disabled" });
    tracing::info!("🎮 Gameplay features:");
    tracing::info!("  - OAuth: {}", if config.enable_oauth { "enabled" } else { "disabled" });
    tracing::info!("  - AR: {}", if config.enable_ar { "enabled" } else { "disabled" });
    tracing::info!("  - Multiplayer: {}", if config.enable_multiplayer { "enabled" } else { "disabled" });
    tracing::info!("  - Image processing: {}", if config.enable_image_processing { "enabled" } else { "disabled" });

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

    // Создание состояния приложения с конфигурацией
    let state = AppState::new_with_config(pool, config.clone());

    // Запустить фоновую задачу для очистки rate limiter, blacklist и WebSocket
    {
        let rate_limiter = state.rate_limiter.clone();
        let ip_blacklist = state.ip_blacklist.clone();
        let ws_manager = state.ws_manager.clone();
        
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(tokio::time::Duration::from_secs(300)); // 5 минут
            loop {
                interval.tick().await;
                tracing::debug!("Running background cleanup tasks");
                rate_limiter.cleanup().await;
                ip_blacklist.cleanup().await;
                ws_manager.cleanup_empty_rooms().await;
                
                let rooms_count = ws_manager.active_rooms_count().await;
                tracing::info!("WebSocket active rooms: {}", rooms_count);
            }
        });
    }

    // Создание роутера с настроенными маршрутами
    let app = routes::app_router(state)
        .layer(
            CorsLayer::new()
                .allow_origin(tower_http::cors::Any)
                .allow_methods(tower_http::cors::Any)
                .allow_headers(tower_http::cors::Any)
        );

    // Подготавливаем MakeService, чтобы axum добавил ConnectInfo<SocketAddr> в каждое обращение
    let make_svc = app.into_make_service_with_connect_info::<SocketAddr>();

    // Запуск сервера
    // Bind to 0.0.0.0 so the server listens on all interfaces (localhost, LAN, docker, etc.)
    let addr = SocketAddr::from(([0, 0, 0, 0], config.port));
    tracing::info!("Starting server on {}", addr);
    let listener = TcpListener::bind(addr).await?;
    serve(listener, make_svc).await?;

    Ok(())
}
