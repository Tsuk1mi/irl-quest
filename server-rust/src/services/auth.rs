use anyhow::{anyhow, Result};
use bcrypt::{hash, verify, DEFAULT_COST};
use chrono::{Duration, Utc};
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, TokenData, Validation};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;

use crate::config::Settings;
use crate::models::{User, UserCreate, UserOut};

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: String,  // user id
    pub exp: usize,   // expiration time
}

pub struct AuthService {
    settings: Settings,
}

impl AuthService {
    pub fn new(settings: Settings) -> Self {
        Self { settings }
    }

    pub async fn register_user(&self, pool: &PgPool, user_create: UserCreate) -> Result<UserOut> {
        // Check if user already exists
        let existing_user: Option<(i32,)> = sqlx::query_as(
            "SELECT id FROM users WHERE email = $1 OR username = $2"
        )
        .bind(&user_create.email)
        .bind(&user_create.username)
        .fetch_optional(pool)
        .await?;

        if existing_user.is_some() {
            return Err(anyhow!("User with this email or username already exists"));
        }

        // Hash password
        let hashed_password = hash(&user_create.password, DEFAULT_COST)?;

        // Create user
        let user: User = sqlx::query_as(
            r#"
            INSERT INTO users (email, username, hashed_password, is_active, created_at)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING id, email, username, hashed_password, is_active, created_at
            "#,
        )
        .bind(&user_create.email)
        .bind(&user_create.username)
        .bind(&hashed_password)
        .bind(true)
        .bind(Utc::now())
        .fetch_one(pool)
        .await?;

        Ok(UserOut::from(user))
    }

    pub async fn authenticate_and_issue_token(
        &self,
        pool: &PgPool,
        username_or_email: &str,
        password: &str,
    ) -> Result<String> {
        // Find user by email or username
        let user: Option<User> = sqlx::query_as(
            "SELECT * FROM users WHERE email = $1 OR username = $1"
        )
        .bind(username_or_email)
        .fetch_optional(pool)
        .await?;

        let user = user.ok_or_else(|| anyhow!("Invalid credentials"))?;

        // Verify password
        if !verify(password, &user.hashed_password)? {
            return Err(anyhow!("Invalid credentials"));
        }

        // Generate token
        self.create_access_token(&user.id.to_string())
    }

    pub fn create_access_token(&self, user_id: &str) -> Result<String> {
        let expiration = Utc::now()
            + Duration::minutes(self.settings.access_token_expire_minutes);

        let claims = Claims {
            sub: user_id.to_owned(),
            exp: expiration.timestamp() as usize,
        };

        let token = encode(
            &Header::default(),
            &claims,
            &EncodingKey::from_secret(self.settings.secret_key.as_ref()),
        )?;

        Ok(token)
    }

    pub fn verify_token(&self, token: &str) -> Result<TokenData<Claims>> {
        let token_data = decode::<Claims>(
            token,
            &DecodingKey::from_secret(self.settings.secret_key.as_ref()),
            &Validation::default(),
        )?;

        Ok(token_data)
    }

    pub async fn get_user_by_id(&self, pool: &PgPool, user_id: i32) -> Result<Option<User>> {
        let user: Option<User> = sqlx::query_as("SELECT * FROM users WHERE id = $1")
            .bind(user_id)
            .fetch_optional(pool)
            .await?;

        Ok(user)
    }
}