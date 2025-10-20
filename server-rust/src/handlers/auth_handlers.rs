use axum::{
    extract::State,
    http::{StatusCode, HeaderMap},
    Json,
};
use argon2::{
    password_hash::{rand_core::OsRng, PasswordHasher, PasswordHash, PasswordVerifier, SaltString},
    Argon2,
};
use crate::{error::AppError, validation};
use jsonwebtoken::{encode, EncodingKey, Header};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use chrono::{Utc};
use crate::state::AppState;
use crate::utils_impl::ip::get_client_ip_from_headers;
use axum::extract::ConnectInfo;
use std::net::SocketAddr;

#[derive(Deserialize)]
pub struct RegisterRequest {
    pub username: String,
    pub email: String,
    pub password: String,
}

#[derive(Deserialize)]
pub struct LoginRequest {
    pub email: Option<String>,
    pub username: Option<String>,
    pub password: String,
}

#[derive(Serialize)]
pub struct AuthResponse {
    pub token: String,
    pub user_id: i32,
    pub username: String,
    pub client_ip: String,
}

#[derive(Serialize)]
struct Claims {
    sub: i32,  // user_id
    exp: i64,  // expiration time
    iat: i64,  // issued at
}

pub async fn register(
    State(state): State<AppState>,
    ConnectInfo(peer): ConnectInfo<SocketAddr>,
    headers: HeaderMap,
    Json(req): Json<RegisterRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    let pool: &PgPool = &state.db;

    // Debug logging: headers and incoming fields (mask password content)
    tracing::info!("[auth::register] headers={:?}", headers);
    tracing::info!("[auth::register] payload username='{}' email='{}' password_len={}'", req.username, req.email, req.password.len());
    tracing::info!("[auth::register] peer={}", peer);

    // Валидация входных данных
    validation::validate_email(&req.email)?;
    validation::validate_password(&req.password)?;
    validation::validate_username(&req.username)?;

    // Проверка на существующего пользователя
    let existing_user = sqlx::query!(
        "SELECT id FROM users WHERE email = $1 OR username = $2",
        req.email,
        req.username
    )
    .fetch_optional(pool)
    .await?;

    if existing_user.is_some() {
        return Err(AppError::Validation("User already exists".to_string()));
    }

    // Хеширование пароля
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    let password_hash = argon2
        .hash_password(req.password.as_bytes(), &salt)
        .map_err(|e| AppError::Internal(e.to_string()))?
        .to_string();

    // Создание пользователя
    let user = sqlx::query!(
        r#"
        INSERT INTO users (username, email, hashed_password)
        VALUES ($1, $2, $3::TEXT)
        RETURNING id, username
        "#,
        req.username,
        req.email,
        password_hash,
    )
    .fetch_one(pool)
    .await?;

    // Генерация JWT токена
    let token = create_token(user.id)
        .map_err(|e| AppError::Auth(e.1))?;

    // Extract client IP from headers if present, otherwise use peer address
    let client_ip = get_client_ip_from_headers(&headers)
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| peer.ip().to_string());
    tracing::info!("Register from IP: {} for user {}", client_ip, user.username);

    Ok(Json(AuthResponse {
        token,
        user_id: user.id,
        username: user.username,
        client_ip,
    }))
}

pub async fn login(
    State(state): State<AppState>,
    ConnectInfo(peer): ConnectInfo<SocketAddr>,
    headers: HeaderMap,
    Json(req): Json<LoginRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    let pool: &PgPool = &state.db;

    // Debug logging: headers and identifier
    tracing::info!("[auth::login] headers={:?}", headers);
    tracing::info!("[auth::login] payload email={:?} username={:?} password_len={}", req.email, req.username, req.password.len());
    tracing::info!("[auth::login] peer={}", peer);

    // Determine identifier: prefer email if provided, otherwise username
    let identifier = if let Some(ref e) = req.email {
        // validate email format
        validation::validate_email(e)?;
        e.clone()
    } else if let Some(ref u) = req.username {
        // validate username format
        validation::validate_username(u)?;
        u.clone()
    } else {
        return Err(AppError::BadRequest("Either email or username must be provided".to_string()));
    };

    // Fetch user by identifier
    let user_opt = sqlx::query!(
        r#"
        SELECT id, username, hashed_password
        FROM users
        WHERE email = $1 OR username = $1
        "#,
        identifier
    )
    .fetch_optional(pool)
    .await?;

    let user = match user_opt {
        Some(u) => u,
        None => {
            tracing::warn!("[auth::login] user not found for identifier={}", identifier);
            return Err(AppError::Auth("Invalid credentials".to_string()));
        }
    };

    // Проверка пароля
    let parsed_hash = PasswordHash::new(&user.hashed_password)
        .map_err(|e| AppError::Internal(e.to_string()))?;

    if let Err(_) = Argon2::default().verify_password(req.password.as_bytes(), &parsed_hash) {
        tracing::warn!("[auth::login] password verification failed for user_id={} identifier={}", user.id, identifier);
        return Err(AppError::Auth("Invalid credentials".to_string()));
    }

    // Генерация JWT токена
    let token = create_token(user.id)
        .map_err(|e| AppError::Internal(format!("Failed to create token: {}", e.1)))?;

    let client_ip = get_client_ip_from_headers(&headers)
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| peer.ip().to_string());
    tracing::info!("Login from IP: {} for user {} (identifier={})", client_ip, user.username, identifier);

    Ok(Json(AuthResponse {
        token,
        user_id: user.id,
        username: user.username,
        client_ip,
    }))
}

fn create_token(user_id: i32) -> Result<String, (StatusCode, String)> {
    let now = Utc::now();
    let exp = (now + chrono::Duration::days(7)).timestamp();

    let claims = Claims {
        sub: user_id,
        exp,
        iat: now.timestamp(),
    };

    encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(std::env::var("JWT_SECRET").unwrap_or_else(|_| "your-secret-key".to_string()).as_bytes()),
    )
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("Failed to create token: {}", e)))
}
