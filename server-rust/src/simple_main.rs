// Упрощенный основной сервер для быстрого запуска
mod config;
mod db;
mod models;

use std::sync::Arc;

use axum::{
    extract::{Path, State},
    http::StatusCode,
    middleware::{from_fn_with_state, Next},
    request::Request,
    response::{IntoResponse, Response},
    routing::{delete, get, post, put},
    Json, Router,
};
use bcrypt::{hash, verify, DEFAULT_COST};
use chrono::Utc;
use dotenv::dotenv;
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use sqlx::{PgPool, Row};
use tokio::net::TcpListener;
use tower::ServiceBuilder;
use tower_http::{cors::CorsLayer, trace::TraceLayer};
use tracing::{info, Level};
use tracing_subscriber;

use config::Settings;
use db::create_database_pool;
use models::simple::*;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub settings: Settings,
}

#[derive(Debug, Serialize, Deserialize)]
struct Claims {
    sub: String,
    user_id: i32,
    exp: usize,
}

// Middleware для аутентификации
async fn auth_middleware(
    State(state): State<Arc<AppState>>,
    mut req: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    let auth_header = req
        .headers()
        .get("Authorization")
        .and_then(|header| header.to_str().ok());

    let token = match auth_header {
        Some(header) if header.starts_with("Bearer ") => &header[7..],
        _ => return Err(StatusCode::UNAUTHORIZED),
    };

    let claims = decode::<Claims>(
        token,
        &DecodingKey::from_secret(state.settings.secret_key.as_ref()),
        &Validation::default(),
    )
    .map_err(|_| StatusCode::UNAUTHORIZED)?;

    req.extensions_mut().insert(claims.user_id);
    Ok(next.run(req).await)
}

// Health endpoints
async fn health() -> impl IntoResponse {
    Json(serde_json::json!({"status": "ok"}))
}

async fn ready(State(state): State<Arc<AppState>>) -> impl IntoResponse {
    // Проверяем соединение с базой данных
    match sqlx::query("SELECT 1").fetch_one(&state.db).await {
        Ok(_) => Json(serde_json::json!({"status": "ready", "database": "connected"})),
        Err(_) => {
            (StatusCode::SERVICE_UNAVAILABLE, Json(serde_json::json!({"status": "not ready", "database": "disconnected"})))
        }
    }
}

// Auth endpoints
async fn register(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<SimpleUserCreate>,
) -> Result<impl IntoResponse, StatusCode> {
    let hashed_password = hash(&payload.password, DEFAULT_COST)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let user = sqlx::query_as!(
        SimpleUser,
        "INSERT INTO users (email, username, hashed_password) VALUES ($1, $2, $3) RETURNING *",
        payload.email,
        payload.username,
        hashed_password
    )
    .fetch_one(&state.db)
    .await
    .map_err(|e| {
        tracing::error!("Database error: {}", e);
        StatusCode::BAD_REQUEST
    })?;

    Ok(Json(SimpleUserOut::from(user)))
}

async fn login(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<LoginRequest>,
) -> Result<impl IntoResponse, StatusCode> {
    let user = sqlx::query_as!(
        SimpleUser,
        "SELECT * FROM users WHERE username = $1",
        payload.username
    )
    .fetch_one(&state.db)
    .await
    .map_err(|_| StatusCode::UNAUTHORIZED)?;

    if !verify(&payload.password, &user.hashed_password)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
    {
        return Err(StatusCode::UNAUTHORIZED);
    }

    let claims = Claims {
        sub: user.username.clone(),
        user_id: user.id,
        exp: (Utc::now().timestamp() + 3600) as usize, // 1 час
    };

    let token = encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(state.settings.secret_key.as_ref()),
    )
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok(Json(SimpleToken::new(token, SimpleUserOut::from(user))))
}

async fn me(
    State(state): State<Arc<AppState>>,
    req: Request,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let user = sqlx::query_as!(
        SimpleUser,
        "SELECT * FROM users WHERE id = $1",
        user_id
    )
    .fetch_one(&state.db)
    .await
    .map_err(|_| StatusCode::NOT_FOUND)?;

    Ok(Json(SimpleUserOut::from(user)))
}

// Task endpoints
async fn list_tasks(
    State(state): State<Arc<AppState>>,
    req: Request,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let tasks = sqlx::query_as!(
        SimpleTask,
        "SELECT * FROM tasks WHERE owner_id = $1 ORDER BY created_at DESC",
        user_id
    )
    .fetch_all(&state.db)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let task_outs: Vec<SimpleTaskOut> = tasks.into_iter().map(SimpleTaskOut::from).collect();
    Ok(Json(task_outs))
}

async fn create_task(
    State(state): State<Arc<AppState>>,
    req: Request,
    Json(payload): Json<SimpleTaskCreate>,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let task = sqlx::query_as!(
        SimpleTask,
        "INSERT INTO tasks (title, description, owner_id) VALUES ($1, $2, $3) RETURNING *",
        payload.title,
        payload.description,
        user_id
    )
    .fetch_one(&state.db)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok((StatusCode::CREATED, Json(SimpleTaskOut::from(task))))
}

async fn get_task(
    State(state): State<Arc<AppState>>,
    Path(task_id): Path<i32>,
    req: Request,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let task = sqlx::query_as!(
        SimpleTask,
        "SELECT * FROM tasks WHERE id = $1 AND owner_id = $2",
        task_id,
        user_id
    )
    .fetch_one(&state.db)
    .await
    .map_err(|_| StatusCode::NOT_FOUND)?;

    Ok(Json(SimpleTaskOut::from(task)))
}

async fn update_task(
    State(state): State<Arc<AppState>>,
    Path(task_id): Path<i32>,
    req: Request,
    Json(payload): Json<SimpleTaskUpdate>,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let task = sqlx::query_as!(
        SimpleTask,
        r#"
        UPDATE tasks 
        SET title = COALESCE($1, title),
            description = COALESCE($2, description),
            completed = COALESCE($3, completed)
        WHERE id = $4 AND owner_id = $5
        RETURNING *
        "#,
        payload.title,
        payload.description,
        payload.completed,
        task_id,
        user_id
    )
    .fetch_one(&state.db)
    .await
    .map_err(|_| StatusCode::NOT_FOUND)?;

    Ok(Json(SimpleTaskOut::from(task)))
}

async fn delete_task(
    State(state): State<Arc<AppState>>,
    Path(task_id): Path<i32>,
    req: Request,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let result = sqlx::query!(
        "DELETE FROM tasks WHERE id = $1 AND owner_id = $2",
        task_id,
        user_id
    )
    .execute(&state.db)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    if result.rows_affected() == 0 {
        return Err(StatusCode::NOT_FOUND);
    }

    Ok(StatusCode::NO_CONTENT)
}

// Quest endpoints (аналогично tasks)
async fn list_quests(
    State(state): State<Arc<AppState>>,
    req: Request,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let quests = sqlx::query_as!(
        SimpleQuest,
        "SELECT * FROM quests WHERE owner_id = $1 ORDER BY created_at DESC",
        user_id
    )
    .fetch_all(&state.db)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let quest_outs: Vec<SimpleQuestOut> = quests.into_iter().map(SimpleQuestOut::from).collect();
    Ok(Json(quest_outs))
}

async fn create_quest(
    State(state): State<Arc<AppState>>,
    req: Request,
    Json(payload): Json<SimpleQuestCreate>,
) -> Result<impl IntoResponse, StatusCode> {
    let user_id = req.extensions().get::<i32>().copied()
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let quest = sqlx::query_as!(
        SimpleQuest,
        "INSERT INTO quests (title, description, difficulty, owner_id) VALUES ($1, $2, $3, $4) RETURNING *",
        payload.title,
        payload.description,
        payload.difficulty.unwrap_or(1),
        user_id
    )
    .fetch_one(&state.db)
    .await
    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok((StatusCode::CREATED, Json(SimpleQuestOut::from(quest))))
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenv().ok();

    tracing_subscriber::fmt()
        .with_max_level(Level::INFO)
        .init();

    let settings = Settings::new();
    info!("Starting simple server with settings: {:?}", settings);

    let pool = create_database_pool(&settings.database_url).await?;
    info!("Database connection established");

    let state = Arc::new(AppState {
        db: pool,
        settings: settings.clone(),
    });

    let public_auth_routes = Router::new()
        .route("/register", post(register))
        .route("/token", post(login));

    let protected_auth_routes = Router::new()
        .route("/me", get(me))
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let task_routes = Router::new()
        .route("/", get(list_tasks).post(create_task))
        .route("/:task_id", get(get_task).put(update_task).delete(delete_task))
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let quest_routes = Router::new()
        .route("/", get(list_quests).post(create_quest))
        .route_layer(from_fn_with_state(state.clone(), auth_middleware));

    let api_routes = Router::new()
        .nest("/auth", public_auth_routes.merge(protected_auth_routes))
        .nest("/tasks", task_routes)
        .nest("/quests", quest_routes);

    let app = Router::new()
        .route("/", get(|| async { "IRL Quest API Server" }))
        .route("/health", get(health))
        .route("/ready", get(ready))
        .nest("/api/v1", api_routes)
        .layer(
            ServiceBuilder::new()
                .layer(TraceLayer::new_for_http())
                .layer(CorsLayer::permissive()),
        )
        .with_state(state);

    let addr = format!("{}:{}", settings.server_host, settings.server_port);
    let listener = TcpListener::bind(&addr).await?;
    info!("Server listening on {}", addr);

    axum::serve(listener, app).await?;

    Ok(())
}