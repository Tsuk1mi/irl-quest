use axum::serve;
use dotenv::dotenv;
use std::net::SocketAddr;
use tokio::net::TcpListener;
use tower_http::cors::CorsLayer;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

mod config;
mod db;
mod error;
mod handlers;
mod middleware;
mod ml;
mod models;
mod rag; // RAG service stubs
mod routes;
mod services;
mod state;
mod utils;
mod utils_impl;
mod validation;

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
    // Проверяем несколько возможных путей в порядке приоритета
    let current_dir = std::env::current_dir().unwrap_or_else(|_| std::path::PathBuf::from("."));
    tracing::debug!("Current working directory: {}", current_dir.display());

    let mut loaded = false;
    let mut env_paths = Vec::new();

    // Путь 1: server-rust/.env (локальный, приоритетный)
    let local_env = current_dir.join(".env");
    if local_env.exists() {
        env_paths.push(local_env.clone());
    }

    // Путь 2: ../.env (если запуск из server-rust/)
    let parent_env = current_dir.join("../.env");
    if parent_env.exists() {
        env_paths.push(parent_env.clone());
    }

    // Путь 3: server-rust/.env (если запуск из корня проекта)
    let server_rust_env = current_dir.join("server-rust/.env");
    if server_rust_env.exists() {
        env_paths.push(server_rust_env.clone());
    }

    // Путь 4: .env в корне (если запуск из корня проекта)
    let root_env = current_dir.join(".env");
    if root_env.exists() && !env_paths.contains(&root_env) {
        env_paths.push(root_env.clone());
    }

    // Попробовать загрузить первый найденный файл
    for env_path in &env_paths {
        // Нормализовать путь
        let normalized_path = if let Ok(canonical) = env_path.canonicalize() {
            canonical
        } else {
            env_path.clone()
        };

        match dotenv::from_path(&normalized_path) {
            Ok(_) => {
                tracing::info!("Loaded .env from: {}", normalized_path.display());
                loaded = true;
                break;
            }
            Err(e) => {
                tracing::debug!(
                    "Failed to load .env from {}: {}",
                    normalized_path.display(),
                    e
                );
            }
        }
    }

    // Если ничего не загрузилось, попробовать стандартный dotenv()
    if !loaded {
        match dotenv() {
            Ok(path) => {
                tracing::info!("Loaded .env using dotenv() from: {}", path.display());
            }
            Err(e) => {
                tracing::warn!("No .env file found. Searched in:");
                for path in &env_paths {
                    tracing::warn!("   - {}", path.display());
                }
                tracing::warn!("   Error: {}. Using environment variables and defaults.", e);
            }
        }
    }

    // Загрузка конфигурации
    let config = config::load_config()?;
    tracing::info!("Loaded configuration");

    // Логируем информацию об IP адресах
    if let Some(public_ip) = &config.public_ip {
        tracing::info!("Public IP detected: {}", public_ip);
        tracing::info!(
            "Mobile clients should use: http://{}:{}",
            public_ip,
            config.port
        );
    }
    if let Some(local_ip) = &config.local_ip {
        tracing::info!("Local IP detected: {}", local_ip);
    }

    // Логируем включенные фичи
    tracing::info!("Security features:");
    tracing::info!(
        "  - Rate limiting: {}/min (burst: {})",
        config.rate_limit_per_minute,
        config.rate_limit_burst
    );
    tracing::info!(
        "  - MFA: {}",
        if config.enable_mfa {
            "enabled"
        } else {
            "disabled"
        }
    );
    tracing::info!("Gameplay features:");
    tracing::info!(
        "  - OAuth: {}",
        if config.enable_oauth {
            "enabled"
        } else {
            "disabled"
        }
    );
    tracing::info!(
        "  - AR: {}",
        if config.enable_ar {
            "enabled"
        } else {
            "disabled"
        }
    );
    tracing::info!(
        "  - Multiplayer: {}",
        if config.enable_multiplayer {
            "enabled"
        } else {
            "disabled"
        }
    );
    tracing::info!(
        "  - Image processing: {}",
        if config.enable_image_processing {
            "enabled"
        } else {
            "disabled"
        }
    );

    // Подключение к базе данных
    tracing::info!("Connecting to database at {}", config.database_url);
    let pool = db::create_database_pool(&config.database_url)
        .await
        .expect("Failed to create database pool");
    tracing::info!("Successfully connected to database");

    // Автоматически применяем миграции при старте
    tracing::info!("Running database migrations...");
    if let Err(e) = sqlx::migrate!("./migrations").run(&pool).await {
        tracing::error!("Failed to run database migrations: {e}");
        return Err(Box::new(e) as Box<dyn std::error::Error>);
    }
    tracing::info!("Database migrations completed");

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
    let app = routes::app_router(state).layer(
        CorsLayer::new()
            .allow_origin(tower_http::cors::Any)
            .allow_methods(tower_http::cors::Any)
            .allow_headers(tower_http::cors::Any),
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
