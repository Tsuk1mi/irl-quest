use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize)]
pub struct User {
    pub id: i32,
    pub email: String,
    pub username: String,
    pub hashed_password: String,
    pub is_active: bool,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize)]
pub struct UserCreate {
    pub email: String,
    pub username: String,
    pub password: String,
}

#[derive(Debug, Serialize)]
pub struct UserOut {
    pub id: i32,
    pub email: String,
    pub username: String,
    pub is_active: bool,
    pub created_at: DateTime<Utc>,
}

impl From<User> for UserOut {
    fn from(user: User) -> Self {
        Self {
            id: user.id,
            email: user.email,
            username: user.username,
            is_active: user.is_active,
            created_at: user.created_at,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct UserUpdate {
    pub username: Option<String>,
    pub password: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct Token {
    pub access_token: String,
    pub token_type: String,
}

impl Token {
    pub fn new(access_token: String) -> Self {
        Self {
            access_token,
            token_type: "bearer".to_string(),
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
}