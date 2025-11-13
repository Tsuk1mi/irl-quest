use anyhow::{anyhow, Result};
use argon2::{password_hash::SaltString, Argon2, PasswordHash, PasswordHasher, PasswordVerifier};
use chrono::{Duration, Utc};
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, TokenData, Validation};
use rand::rngs::OsRng;
use serde::{Deserialize, Serialize};
use sqlx::PgPool;

use crate::config::Config;
use crate::models::{User, UserCreate, UserOut};

const ACCESS_TOKEN_EXPIRE_MINUTES: i64 = 60; // 1 hour

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: String, // user id
    pub exp: usize,  // expiration time
}

pub struct AuthService {
    config: Config,
}

impl AuthService {
    pub fn new(config: Config) -> Self {
        Self { config }
    }

    pub async fn register_user(&self, pool: &PgPool, user_create: UserCreate) -> Result<UserOut> {
        // Check if user already exists
        let existing_user: Option<(i32,)> =
            sqlx::query_as("SELECT id FROM users WHERE email = $1 OR username = $2")
                .bind(&user_create.email)
                .bind(&user_create.username)
                .fetch_optional(pool)
                .await?;

        if existing_user.is_some() {
            return Err(anyhow!("User with this email or username already exists"));
        }

        // Hash password
        let salt = SaltString::generate(&mut OsRng);
        let hashed_password = Argon2::default()
            .hash_password(user_create.password.as_bytes(), &salt)
            .map_err(|e| anyhow!("Failed to hash password: {}", e))?
            .to_string();

        // Create user
        let user: User = sqlx::query_as::<_, User>(
            r#"
            INSERT INTO users (
                email, username, hashed_password, is_active, 
                level, experience, gold, 
                strength, intelligence, charisma, dexterity, constitution, wisdom,
                character_class, character_race,
                timezone, settings, created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18)
            RETURNING id, email, username, hashed_password, is_active,
                      level, experience, gold, avatar_url, bio,
                      timezone, last_login, settings,
                      strength, intelligence, charisma, dexterity, constitution, wisdom,
                      character_class, character_race, created_at
            "#,
        )
        .bind(&user_create.email)
        .bind(&user_create.username)
        .bind(&hashed_password)
        .bind(true)
        .bind(1i32) // level
        .bind(0i32) // experience
        .bind(100i32) // gold
        .bind(10i32) // strength
        .bind(10i32) // intelligence
        .bind(10i32) // charisma
        .bind(10i32) // dexterity
        .bind(10i32) // constitution
        .bind(10i32) // wisdom
        .bind("warrior") // character_class
        .bind("human") // character_race
        .bind(user_create.timezone.as_deref().unwrap_or("UTC"))
        .bind(serde_json::json!({}))
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
    ) -> Result<(String, UserOut)> {
        // Find user by email or username
        let user: Option<User> = sqlx::query_as::<_, User>(
            r#"SELECT id, email, username, hashed_password, is_active, level, experience, gold,
                      avatar_url, bio, timezone, last_login, settings,
                      strength, intelligence, charisma, dexterity, constitution, wisdom,
                      character_class, character_race, created_at
               FROM users WHERE email = $1 OR username = $1"#,
        )
        .bind(username_or_email)
        .fetch_optional(pool)
        .await?;

        let user = user.ok_or_else(|| anyhow!("Invalid credentials"))?;

        // Verify password
        let parsed_hash = PasswordHash::new(&user.hashed_password)
            .map_err(|e| anyhow!("Invalid password hash stored for user: {}", e))?;
        Argon2::default()
            .verify_password(password.as_bytes(), &parsed_hash)
            .map_err(|_| anyhow!("Invalid credentials"))?;

        // Update last login
        sqlx::query("UPDATE users SET last_login = $1 WHERE id = $2")
            .bind(Utc::now())
            .bind(user.id)
            .execute(pool)
            .await?;

        // Generate token
        let token = self.create_access_token(&user.id.to_string())?;
        let user_out = UserOut::from(user);

        Ok((token, user_out))
    }

    pub fn create_access_token(&self, user_id: &str) -> Result<String> {
        let expiration = Utc::now() + Duration::minutes(ACCESS_TOKEN_EXPIRE_MINUTES);

        let claims = Claims {
            sub: user_id.to_owned(),
            exp: expiration.timestamp() as usize,
        };

        let token = encode(
            &Header::default(),
            &claims,
            &EncodingKey::from_secret(self.config.jwt_secret.as_ref()),
        )?;

        Ok(token)
    }

    pub fn verify_token(&self, token: &str) -> Result<TokenData<Claims>> {
        let token_data = decode::<Claims>(
            token,
            &DecodingKey::from_secret(self.config.jwt_secret.as_ref()),
            &Validation::default(),
        )?;

        Ok(token_data)
    }

    pub async fn get_user_by_id(&self, pool: &PgPool, user_id: i32) -> Result<Option<User>> {
        let user: Option<User> = sqlx::query_as::<_, User>(
            r#"SELECT id, email, username, hashed_password, is_active, level, experience, gold,
                      avatar_url, bio, timezone, last_login, settings,
                      strength, intelligence, charisma, dexterity, constitution, wisdom,
                      character_class, character_race, created_at
               FROM users WHERE id = $1"#,
        )
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(user)
    }
}
