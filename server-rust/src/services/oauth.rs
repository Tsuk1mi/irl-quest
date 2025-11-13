/// OAuth2 сервис для интеграции с Google и Apple
use crate::config::Config;
use crate::error::AppError;
use crate::models::auth::{OAuthProvider, OAuthUserInfo};
use crate::models::User;
use argon2::{Argon2, PasswordHasher};
use chrono::Utc;
use password_hash::SaltString;
use rand::rngs::OsRng;
use reqwest::Client;
use serde::Deserialize;
use sqlx::PgPool;

pub struct OAuthService {
    config: Config,
    http_client: Client,
}

#[derive(Debug, Deserialize)]
struct GoogleTokenResponse {
    access_token: String,
    expires_in: i64,
    refresh_token: Option<String>,
    id_token: String,
}

#[derive(Debug, Deserialize)]
struct GoogleUserInfo {
    sub: String, // Google user ID
    email: String,
    name: Option<String>,
    picture: Option<String>,
    email_verified: bool,
}

#[derive(Debug, Deserialize)]
struct AppleTokenResponse {
    access_token: String,
    expires_in: i64,
    refresh_token: Option<String>,
    id_token: String,
}

#[derive(Debug, Deserialize)]
struct AppleUserInfo {
    sub: String, // Apple user ID
    email: String,
    email_verified: Option<bool>,
}

impl OAuthService {
    pub fn new(config: Config) -> Self {
        Self {
            config,
            http_client: Client::new(),
        }
    }

    /// Обменять authorization code на access token (Google)
    async fn exchange_google_code(
        &self,
        code: &str,
        redirect_uri: &str,
    ) -> Result<GoogleTokenResponse, AppError> {
        let google_client_id = std::env::var("GOOGLE_CLIENT_ID").map_err(|_| {
            AppError::InternalServerError("GOOGLE_CLIENT_ID not configured".to_string())
        })?;
        let google_client_secret = std::env::var("GOOGLE_CLIENT_SECRET").map_err(|_| {
            AppError::InternalServerError("GOOGLE_CLIENT_SECRET not configured".to_string())
        })?;

        let params = [
            ("code", code),
            ("client_id", &google_client_id),
            ("client_secret", &google_client_secret),
            ("redirect_uri", redirect_uri),
            ("grant_type", "authorization_code"),
        ];

        let response = self
            .http_client
            .post("https://oauth2.googleapis.com/token")
            .form(&params)
            .send()
            .await
            .map_err(|e| {
                AppError::ExternalServiceError(format!("Failed to exchange Google code: {}", e))
            })?;

        if !response.status().is_success() {
            let error_text = response.text().await.unwrap_or_default();
            return Err(AppError::ExternalServiceError(format!(
                "Google OAuth error: {}",
                error_text
            )));
        }

        response.json::<GoogleTokenResponse>().await.map_err(|e| {
            AppError::ExternalServiceError(format!("Failed to parse Google response: {}", e))
        })
    }

    /// Получить информацию о пользователе Google
    async fn get_google_user_info(&self, access_token: &str) -> Result<GoogleUserInfo, AppError> {
        let response = self
            .http_client
            .get("https://www.googleapis.com/oauth2/v3/userinfo")
            .bearer_auth(access_token)
            .send()
            .await
            .map_err(|e| {
                AppError::ExternalServiceError(format!("Failed to get Google user info: {}", e))
            })?;

        if !response.status().is_success() {
            return Err(AppError::ExternalServiceError(
                "Failed to get Google user info".to_string(),
            ));
        }

        response.json::<GoogleUserInfo>().await.map_err(|e| {
            AppError::ExternalServiceError(format!("Failed to parse Google user info: {}", e))
        })
    }

    /// Аутентификация через Google
    pub async fn authenticate_google(
        &self,
        pool: &PgPool,
        code: &str,
        redirect_uri: &str,
    ) -> Result<OAuthUserInfo, AppError> {
        // Обменять код на токены
        let token_response = self.exchange_google_code(code, redirect_uri).await?;

        // Получить информацию о пользователе
        let user_info = self
            .get_google_user_info(&token_response.access_token)
            .await?;

        if !user_info.email_verified {
            return Err(AppError::BadRequest("Email not verified".to_string()));
        }

        let oauth_user_info = OAuthUserInfo {
            provider: OAuthProvider::Google,
            provider_user_id: user_info.sub,
            email: user_info.email,
            name: user_info.name,
            picture: user_info.picture,
        };

        // Сохранить или обновить OAuth account
        self.save_oauth_account(
            pool,
            &oauth_user_info,
            &token_response.access_token,
            token_response.refresh_token.as_deref(),
            token_response.expires_in,
        )
        .await?;

        Ok(oauth_user_info)
    }

    /// Аутентификация через Apple (упрощенная реализация)
    pub async fn authenticate_apple(
        &self,
        pool: &PgPool,
        code: &str,
        redirect_uri: &str,
    ) -> Result<OAuthUserInfo, AppError> {
        // Apple Sign In требует более сложной настройки с JWT и приватными ключами
        // Здесь базовая структура, которую нужно расширить

        let apple_client_id = std::env::var("APPLE_CLIENT_ID").map_err(|_| {
            AppError::InternalServerError("APPLE_CLIENT_ID not configured".to_string())
        })?;

        // TODO: Реализовать полный Apple Sign In flow
        // Требуется:
        // 1. Создать client_secret как JWT с приватным ключом Apple
        // 2. Обменять code на tokens
        // 3. Декодировать id_token для получения информации о пользователе

        Err(AppError::NotImplemented(
            "Apple Sign In not fully implemented yet".to_string(),
        ))
    }

    /// Найти или создать пользователя по OAuth информации
    pub async fn find_or_create_user(
        &self,
        pool: &PgPool,
        oauth_info: &OAuthUserInfo,
    ) -> Result<User, AppError> {
        // Проверить, существует ли OAuth account
        #[derive(sqlx::FromRow)]
        struct OAuthAccountRecord {
            user_id: i32,
        }

        let existing_account = sqlx::query_as::<_, OAuthAccountRecord>(
            r#"
            SELECT user_id
            FROM oauth_accounts
            WHERE provider = $1 AND provider_user_id = $2
            "#,
        )
        .bind(oauth_info.provider.as_str())
        .bind(&oauth_info.provider_user_id)
        .fetch_optional(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch OAuth account: {}", e)))?;

        if let Some(account) = existing_account {
            // Найти существующего пользователя
            let user = sqlx::query_as::<_, User>(
                r#"
                SELECT id, username, email, hashed_password, is_active,
                       level, experience, gold, avatar_url, bio, timezone, last_login, settings,
                       strength, intelligence, charisma, dexterity, constitution, wisdom,
                       character_class, character_race, created_at
                FROM users
                WHERE id = $1
                "#,
            )
            .bind(account.user_id)
            .fetch_one(pool)
            .await
            .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user: {}", e)))?;

            return Ok(user);
        }

        // Создать нового пользователя
        let username = self.generate_username_from_email(&oauth_info.email);

        // Проверить, что username уникален
        let mut final_username = username.clone();
        let mut counter = 1;
        loop {
            let exists: (i64,) = sqlx::query_as("SELECT COUNT(*) FROM users WHERE username = $1")
                .bind(&final_username)
                .fetch_one(pool)
                .await
                .map_err(|e| AppError::DatabaseError(format!("Failed to check username: {}", e)))?;

            if exists.0 == 0 {
                break;
            }

            counter += 1;
            final_username = format!("{}{}", username, counter);
        }

        // Создать пользователя без пароля (OAuth only)
        let salt = SaltString::generate(&mut OsRng);
        let hashed_password = Argon2::default()
            .hash_password("".as_bytes(), &salt)
            .map_err(|e| AppError::InternalServerError(format!("Failed to hash password: {}", e)))?
            .to_string();

        #[derive(sqlx::FromRow)]
        struct NewUser {
            id: i32,
        }

        let result = sqlx::query_as::<_, NewUser>(
            r#"
            INSERT INTO users (username, email, hashed_password, level, experience, gold, character_class)
            VALUES ($1, $2, $3, 1, 0, 0, 'warrior')
            RETURNING id
            "#
        )
        .bind(&final_username)
        .bind(&oauth_info.email)
        .bind(&hashed_password)
        .fetch_one(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to create user: {}", e)))?;

        let user_id = result.id;

        // Создать OAuth account
        sqlx::query(
            r#"
            INSERT INTO oauth_accounts (user_id, provider, provider_user_id)
            VALUES ($1, $2, $3)
            "#,
        )
        .bind(user_id)
        .bind(oauth_info.provider.as_str())
        .bind(&oauth_info.provider_user_id)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to create OAuth account: {}", e)))?;

        // Получить созданного пользователя
        let user = sqlx::query_as::<_, User>(
            r#"
            SELECT id, username, email, hashed_password, is_active,
                   level, experience, gold, avatar_url, bio, timezone, last_login, settings,
                   strength, intelligence, charisma, dexterity, constitution, wisdom,
                   character_class, character_race, created_at
            FROM users
            WHERE id = $1
            "#,
        )
        .bind(user_id)
        .fetch_one(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch created user: {}", e)))?;

        Ok(user)
    }

    /// Сохранить или обновить OAuth account
    async fn save_oauth_account(
        &self,
        pool: &PgPool,
        oauth_info: &OAuthUserInfo,
        access_token: &str,
        refresh_token: Option<&str>,
        expires_in: i64,
    ) -> Result<(), AppError> {
        let expires_at = Utc::now() + chrono::Duration::seconds(expires_in);
        let provider_str = format!("{:?}", oauth_info.provider).to_lowercase();

        sqlx::query(
            r#"
            UPDATE oauth_accounts
            SET access_token = $1, refresh_token = $2, expires_at = $3
            WHERE provider = $4 AND provider_user_id = $5
            "#,
        )
        .bind(access_token)
        .bind(refresh_token)
        .bind(expires_at)
        .bind(&provider_str)
        .bind(&oauth_info.provider_user_id)
        .execute(pool)
        .await
        .ok();

        Ok(())
    }

    /// Генерировать username из email
    fn generate_username_from_email(&self, email: &str) -> String {
        email
            .split('@')
            .next()
            .unwrap_or("user")
            .chars()
            .filter(|c| c.is_alphanumeric() || *c == '_')
            .collect::<String>()
            .to_lowercase()
    }
}
