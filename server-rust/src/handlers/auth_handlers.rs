use axum::{
    extract::State,
    http::StatusCode,
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
use time::{Duration, OffsetDateTime};

#[derive(Deserialize)]
pub struct RegisterRequest {
    pub username: String,
    pub email: String,
    pub password: String,
}

#[derive(Deserialize)]
pub struct LoginRequest {
    pub email: String,
    pub password: String,
}

#[derive(Serialize)]
pub struct AuthResponse {
    pub token: String,
    pub user_id: i32,
    pub username: String,
}

#[derive(Serialize)]
struct Claims {
    sub: i32,  // user_id
    exp: i64,  // expiration time
    iat: i64,  // issued at
}

pub async fn register(
    State(pool): State<PgPool>,
    Json(req): Json<RegisterRequest>,
) -> Result<Json<AuthResponse>, AppError> {
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
    .fetch_optional(&pool)
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
        INSERT INTO users (username, email, password_hash)
        VALUES ($1, $2, $3::TEXT)
        RETURNING id, username
        "#,
        req.username,
        req.email,
        password_hash,
    )
    .fetch_one(&pool)
    .await?;

    // Генерация JWT токена
    let token = create_token(user.id)
        .map_err(|e| AppError::Auth(e.1))?;

    Ok(Json(AuthResponse {
        token,
        user_id: user.id,
        username: user.username,
    }))
}

pub async fn login(
    State(pool): State<PgPool>,
    Json(req): Json<LoginRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    // Валидация email
    validation::validate_email(&req.email)?;

    let user = sqlx::query!(
        r#"
        SELECT id, username, password_hash
        FROM users
        WHERE email = $1
        "#,
        req.email
    )
    .fetch_optional(&pool)
    .await?
    .ok_or_else(|| AppError::Auth("Invalid credentials".to_string()))?;

    // Проверка пароля
    let parsed_hash = PasswordHash::new(&user.password_hash)
        .map_err(|e| AppError::Internal(e.to_string()))?;

    Argon2::default()
        .verify_password(req.password.as_bytes(), &parsed_hash)
        .map_err(|_| AppError::Auth("Invalid credentials".to_string()))?;

    // Генерация JWT токена
    let token = create_token(user.id)
        .map_err(|e| AppError::Internal(format!("Failed to create token: {}", e.1)))?;

    Ok(Json(AuthResponse {
        token,
        user_id: user.id,
        username: user.username,
    }))
}

fn create_token(user_id: i32) -> Result<String, (StatusCode, String)> {
    let now = OffsetDateTime::now_utc();
    let exp = (now + Duration::days(7)).unix_timestamp();

    let claims = Claims {
        sub: user_id,
        exp,
        iat: now.unix_timestamp(),
    };

    encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(std::env::var("JWT_SECRET").unwrap_or_else(|_| "your-secret-key".to_string()).as_bytes()),
    )
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("Failed to create token: {}", e)))
}
