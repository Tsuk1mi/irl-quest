/// Расширенная система аутентификации
/// Включает: JWT + Refresh tokens, OAuth2, MFA (TOTP)

use crate::config::Config;
use crate::error::AppError;
use crate::models::auth::*;
use crate::models::User;
use chrono::{Duration, Utc};
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, TokenData, Validation};
use sqlx::PgPool;
use totp_rs::{Algorithm, Secret, TOTP};
use uuid::Uuid;

pub struct AuthService {
    config: Config,
    jwt_secret: String,
}

impl AuthService {
    pub fn new(config: Config) -> Self {
        let jwt_secret = config.jwt_secret.clone();
        Self { config, jwt_secret }
    }

    /// Создать access token
    pub fn create_access_token(&self, user: &User) -> Result<String, AppError> {
        let expiration = Utc::now()
            .checked_add_signed(Duration::hours(self.config.jwt_expiration_hours as i64))
            .ok_or(AppError::InternalServerError("Invalid expiration time".to_string()))?
            .timestamp() as usize;

        let claims = Claims {
            sub: user.id.to_string(),
            exp: expiration,
            iat: Utc::now().timestamp() as usize,
            user_id: user.id,
            username: user.username.clone(),
        };

        encode(
            &Header::default(),
            &claims,
            &EncodingKey::from_secret(self.jwt_secret.as_bytes()),
        )
        .map_err(|e| AppError::InternalServerError(format!("Failed to create token: {}", e)))
    }

    /// Создать refresh token и сохранить в БД
    pub async fn create_refresh_token(
        &self,
        pool: &PgPool,
        user_id: i32,
        device_info: Option<String>,
    ) -> Result<String, AppError> {
        let token = Uuid::new_v4().to_string();
        let expires_at = Utc::now()
            .checked_add_signed(Duration::days(self.config.refresh_token_expiration_days as i64))
            .ok_or(AppError::InternalServerError("Invalid expiration time".to_string()))?;

        sqlx::query(
            r#"
            INSERT INTO refresh_tokens (user_id, token, expires_at, device_info)
            VALUES ($1, $2, $3, $4)
            "#
        )
        .bind(user_id)
        .bind(&token)
        .bind(expires_at)
        .bind(device_info)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to create refresh token: {}", e)))?;

        Ok(token)
    }

    /// Валидировать refresh token и создать новую пару токенов
    pub async fn refresh_tokens(
        &self,
        pool: &PgPool,
        refresh_token: &str,
    ) -> Result<TokenResponse, AppError> {
        // Найти и валидировать refresh token
        let token_record = sqlx::query_as::<_, RefreshToken>(
            r#"
            SELECT id, user_id, token, expires_at, created_at, 
                   revoked, device_info
            FROM refresh_tokens
            WHERE token = $1 AND revoked = false
            "#
        )
        .bind(refresh_token)
        .fetch_optional(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch refresh token: {}", e)))?
        .ok_or(AppError::Unauthorized("Invalid refresh token".to_string()))?;

        // Проверить не истек ли токен
        if token_record.expires_at < Utc::now() {
            return Err(AppError::Unauthorized("Refresh token expired".to_string()));
        }

        // Получить пользователя
        let user = sqlx::query_as::<_, User>(
            r#"
            SELECT id, username, email, hashed_password, is_active,
                   level, experience, gold, avatar_url, bio, timezone, last_login, settings,
                   strength, intelligence, charisma, dexterity, constitution, wisdom,
                   character_class, character_race, created_at
            FROM users
            WHERE id = $1
            "#
        )
        .bind(token_record.user_id)
        .fetch_one(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user: {}", e)))?;

        // Отозвать старый refresh token
        sqlx::query(
            "UPDATE refresh_tokens SET revoked = true WHERE token = $1"
        )
        .bind(refresh_token)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to revoke token: {}", e)))?;

        // Создать новые токены
        let access_token = self.create_access_token(&user)?;
        let new_refresh_token = self.create_refresh_token(pool, user.id, token_record.device_info).await?;

        Ok(TokenResponse {
            access_token,
            refresh_token: new_refresh_token,
            token_type: "Bearer".to_string(),
            expires_in: (self.config.jwt_expiration_hours * 3600) as i64,
        })
    }

    /// Валидировать access token
    pub fn verify_token(&self, token: &str) -> Result<TokenData<Claims>, AppError> {
        decode::<Claims>(
            token,
            &DecodingKey::from_secret(self.jwt_secret.as_bytes()),
            &Validation::default(),
        )
        .map_err(|e| AppError::Unauthorized(format!("Invalid token: {}", e)))
    }

    /// Отозвать все refresh tokens пользователя
    pub async fn revoke_all_refresh_tokens(&self, pool: &PgPool, user_id: i32) -> Result<(), AppError> {
        sqlx::query(
            "UPDATE refresh_tokens SET revoked = true WHERE user_id = $1"
        )
        .bind(user_id)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to revoke tokens: {}", e)))?;

        Ok(())
    }

    /// Логин с username/password
    pub async fn login(
        &self,
        pool: &PgPool,
        username: &str,
        password: &str,
        device_info: Option<String>,
    ) -> Result<LoginResponse, AppError> {
        // Найти пользователя
        let user = sqlx::query_as::<_, User>(
            r#"
            SELECT id, username, email, hashed_password, is_active,
                   level, experience, gold, avatar_url, bio, timezone, last_login, settings,
                   strength, intelligence, charisma, dexterity, constitution, wisdom,
                   character_class, character_race, created_at
            FROM users
            WHERE username = $1
            "#
        )
        .bind(username)
        .fetch_optional(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user: {}", e)))?
        .ok_or(AppError::Unauthorized("Invalid credentials".to_string()))?;

        // Проверить пароль
        let valid = bcrypt::verify(password, &user.hashed_password)
            .map_err(|e| AppError::InternalServerError(format!("Failed to verify password: {}", e)))?;

        if !valid {
            // Логировать неудачную попытку
            self.log_login_attempt(pool, Some(user.id), username, false, "Invalid password").await?;
            return Err(AppError::Unauthorized("Invalid credentials".to_string()));
        }

        // Проверить, включен ли MFA
        let mfa_enabled = self.is_mfa_enabled(pool, user.id).await?;

        if mfa_enabled {
            // Создать MFA session
            let session_id = Uuid::new_v4().to_string();
            let expires_at = Utc::now()
                .checked_add_signed(Duration::minutes(5))
                .ok_or(AppError::InternalServerError("Invalid time".to_string()))?;

            sqlx::query(
                r#"
                INSERT INTO mfa_sessions (session_id, user_id, expires_at)
                VALUES ($1, $2, $3)
                "#
            )
            .bind(&session_id)
            .bind(user.id)
            .bind(expires_at)
            .execute(pool)
            .await
            .map_err(|e| AppError::DatabaseError(format!("Failed to create MFA session: {}", e)))?;

            return Ok(LoginResponse {
                tokens: None,
                requires_mfa: true,
                mfa_session_token: Some(session_id),
            });
        }

        // Создать токены
        let access_token = self.create_access_token(&user)?;
        let refresh_token = self.create_refresh_token(pool, user.id, device_info).await?;

        // Обновить last_login
        sqlx::query("UPDATE users SET last_login = $1 WHERE id = $2")
            .bind(Utc::now())
            .bind(user.id)
            .execute(pool)
            .await
            .ok();

        // Логировать успешный вход
        self.log_login_attempt(pool, Some(user.id), username, true, "").await?;

        Ok(LoginResponse {
            tokens: Some(TokenResponse {
                access_token,
                refresh_token,
                token_type: "Bearer".to_string(),
                expires_in: (self.config.jwt_expiration_hours * 3600) as i64,
            }),
            requires_mfa: false,
            mfa_session_token: None,
        })
    }

    /// Проверить MFA код и завершить логин
    pub async fn verify_mfa_and_login(
        &self,
        pool: &PgPool,
        session_token: &str,
        mfa_code: &str,
        device_info: Option<String>,
    ) -> Result<TokenResponse, AppError> {
        // Найти MFA сессию
        #[derive(sqlx::FromRow)]
        struct MfaSession {
            user_id: i32,
            expires_at: chrono::DateTime<Utc>,
        }
        
        let session = sqlx::query_as::<_, MfaSession>(
            r#"
            SELECT user_id, expires_at
            FROM mfa_sessions
            WHERE session_id = $1
            "#
        )
        .bind(session_token)
        .fetch_optional(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch MFA session: {}", e)))?
        .ok_or(AppError::Unauthorized("Invalid MFA session".to_string()))?;

        // Проверить не истекла ли сессия
        if session.expires_at < Utc::now() {
            return Err(AppError::Unauthorized("MFA session expired".to_string()));
        }

        // Проверить MFA код
        let valid = self.verify_mfa_code(pool, session.user_id, mfa_code).await?;
        if !valid {
            return Err(AppError::Unauthorized("Invalid MFA code".to_string()));
        }

        // Удалить MFA сессию
        sqlx::query("DELETE FROM mfa_sessions WHERE session_id = $1")
            .bind(session_token)
            .execute(pool)
            .await
            .ok();

        // Получить пользователя
        let user = sqlx::query_as::<_, User>(
            r#"
            SELECT id, username, email, hashed_password, is_active,
                   level, experience, gold, avatar_url, bio, timezone, last_login, settings,
                   strength, intelligence, charisma, dexterity, constitution, wisdom,
                   character_class, character_race, created_at
            FROM users
            WHERE id = $1
            "#
        )
        .bind(session.user_id)
        .fetch_one(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user: {}", e)))?;

        // Создать токены
        let access_token = self.create_access_token(&user)?;
        let refresh_token = self.create_refresh_token(pool, user.id, device_info).await?;

        // Обновить last_login
        sqlx::query("UPDATE users SET last_login = $1 WHERE id = $2")
            .bind(Utc::now())
            .bind(user.id)
            .execute(pool)
            .await
            .ok();

        Ok(TokenResponse {
            access_token,
            refresh_token,
            token_type: "Bearer".to_string(),
            expires_in: (self.config.jwt_expiration_hours * 3600) as i64,
        })
    }

    /// Проверить, включен ли MFA для пользователя
    async fn is_mfa_enabled(&self, pool: &PgPool, user_id: i32) -> Result<bool, AppError> {
        #[derive(sqlx::FromRow)]
        struct MfaEnabled {
            enabled: bool,
        }
        
        let result = sqlx::query_as::<_, MfaEnabled>(
            r#"
            SELECT enabled
            FROM mfa_secrets
            WHERE user_id = $1
            "#
        )
        .bind(user_id)
        .fetch_optional(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to check MFA: {}", e)))?;

        Ok(result.map(|r| r.enabled).unwrap_or(false))
    }

    /// Логировать попытку входа
    async fn log_login_attempt(
        &self,
        pool: &PgPool,
        user_id: Option<i32>,
        username: &str,
        success: bool,
        failure_reason: &str,
    ) -> Result<(), AppError> {
        sqlx::query(
            r#"
            INSERT INTO login_attempts (user_id, username, success, failure_reason)
            VALUES ($1, $2, $3, $4)
            "#
        )
        .bind(user_id)
        .bind(username)
        .bind(success)
        .bind(failure_reason)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to log login attempt: {}", e)))?;

        Ok(())
    }

    /// Настроить MFA для пользователя (генерация секрета)
    pub async fn setup_mfa(&self, pool: &PgPool, user_id: i32) -> Result<MfaSetupResponse, AppError> {
        // Получить пользователя
        let user = sqlx::query_as::<_, User>(
            r#"SELECT id, username, email, hashed_password, is_active,
                      level, experience, gold, avatar_url, bio, timezone, last_login, settings,
                      strength, intelligence, charisma, dexterity, constitution, wisdom,
                      character_class, character_race, created_at
               FROM users WHERE id = $1"#
        )
        .bind(user_id)
        .fetch_one(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user: {}", e)))?;

        // Генерировать TOTP секрет (32 байта base32)
        use rand::Rng;
        let mut rng = rand::thread_rng();
        let secret_bytes: Vec<u8> = (0..20).map(|_| rng.gen()).collect();
        let secret = Secret::Raw(secret_bytes.clone());
        let secret_str = secret.to_encoded().to_string();

        // Создать TOTP
        let totp = TOTP::new(
            Algorithm::SHA1,
            6,
            1,
            30,
            secret.to_bytes().unwrap(),
            Some("IRL Quest".to_string()),
            user.username.clone(),
        )
        .map_err(|e| AppError::InternalServerError(format!("Failed to create TOTP: {}", e)))?;

        // Генерировать QR код
        let qr_code_url = totp.get_qr_base64()
            .map_err(|e| AppError::InternalServerError(format!("Failed to generate QR code: {}", e)))?;

        // Генерировать backup коды
        let backup_codes: Vec<String> = (0..10)
            .map(|_| {
                use rand::Rng;
                let mut rng = rand::thread_rng();
                format!("{:04}-{:04}", rng.gen_range(0..10000), rng.gen_range(0..10000))
            })
            .collect();

        let backup_codes_json = serde_json::to_string(&backup_codes)
            .map_err(|e| AppError::InternalServerError(format!("Failed to serialize backup codes: {}", e)))?;

        // Сохранить в БД (disabled до подтверждения)
        sqlx::query(
            r#"
            INSERT INTO mfa_secrets (user_id, secret, enabled, backup_codes)
            VALUES ($1, $2, false, $3)
            ON CONFLICT(user_id) DO UPDATE SET
                secret = excluded.secret,
                backup_codes = excluded.backup_codes
            "#
        )
        .bind(user_id)
        .bind(&secret_str)
        .bind(&backup_codes_json)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to save MFA secret: {}", e)))?;

        Ok(MfaSetupResponse {
            secret: secret_str,
            qr_code: qr_code_url,
            backup_codes,
            otpauth_url: totp.get_url(),
        })
    }

    /// Включить MFA после верификации кода
    pub async fn enable_mfa(&self, pool: &PgPool, user_id: i32, code: &str) -> Result<(), AppError> {
        // Проверить код
        let valid = self.verify_mfa_code(pool, user_id, code).await?;
        if !valid {
            return Err(AppError::BadRequest("Invalid MFA code".to_string()));
        }

        // Включить MFA
        sqlx::query(
            "UPDATE mfa_secrets SET enabled = true WHERE user_id = $1"
        )
        .bind(user_id)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to enable MFA: {}", e)))?;

        Ok(())
    }

    /// Отключить MFA
    pub async fn disable_mfa(&self, pool: &PgPool, user_id: i32) -> Result<(), AppError> {
        sqlx::query("DELETE FROM mfa_secrets WHERE user_id = $1")
            .bind(user_id)
            .execute(pool)
            .await
            .map_err(|e| AppError::DatabaseError(format!("Failed to disable MFA: {}", e)))?;

        Ok(())
    }

    /// Проверить MFA код
    pub async fn verify_mfa_code(&self, pool: &PgPool, user_id: i32, code: &str) -> Result<bool, AppError> {
        // Получить секрет
        #[derive(sqlx::FromRow)]
        struct MfaSecret {
            secret: String,
            backup_codes: Option<String>,
        }
        
        let mfa_secret = sqlx::query_as::<_, MfaSecret>(
            r#"
            SELECT secret, backup_codes
            FROM mfa_secrets
            WHERE user_id = $1
            "#
        )
        .bind(user_id)
        .fetch_optional(pool)
        .await
        .map_err(|e| AppError::DatabaseError(format!("Failed to fetch MFA secret: {}", e)))?
        .ok_or(AppError::NotFound("MFA not configured".to_string()))?;

        // Проверить TOTP код
        let secret = Secret::Encoded(mfa_secret.secret.clone())
            .to_bytes()
            .map_err(|e| AppError::InternalServerError(format!("Invalid secret: {}", e)))?;

        let totp = TOTP::new(
            Algorithm::SHA1,
            6,
            1,
            30,
            secret,
            Some("IRL Quest".to_string()),
            "user".to_string(),
        )
        .map_err(|e| AppError::InternalServerError(format!("Failed to create TOTP: {}", e)))?;

        // Проверить текущий код
        let current_time = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map_err(|e| AppError::InternalServerError(format!("Time error: {}", e)))?
            .as_secs();
        
        if totp.check(code, current_time) {
            return Ok(true);
        }

        // Проверить backup коды
        if let Some(backup_codes_json) = &mfa_secret.backup_codes {
            let backup_codes: Vec<String> = serde_json::from_str(backup_codes_json)
                .map_err(|e| AppError::InternalServerError(format!("Failed to parse backup codes: {}", e)))?;

            let code_string = code.to_string();
            if backup_codes.contains(&code_string) {
                // Удалить использованный backup код
                let new_backup_codes: Vec<String> = backup_codes
                    .into_iter()
                    .filter(|c| c != &code_string)
                    .collect();

                let new_backup_codes_json = serde_json::to_string(&new_backup_codes)
                    .map_err(|e| AppError::InternalServerError(format!("Failed to serialize backup codes: {}", e)))?;

                sqlx::query(
                    "UPDATE mfa_secrets SET backup_codes = $1 WHERE user_id = $2"
                )
                .bind(&new_backup_codes_json)
                .bind(user_id)
                .execute(pool)
                .await
                .ok();

                return Ok(true);
            }
        }

        Ok(false)
    }
}

// Добавить зависимость rand в Cargo.toml
use rand;

