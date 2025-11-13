use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// JWT Claims для access token
#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: String, // user_id
    pub exp: usize,  // expiration time
    pub iat: usize,  // issued at
    pub user_id: i32,
    pub username: String,
}

/// Refresh token в базе данных
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct RefreshToken {
    pub id: i32,
    pub user_id: i32,
    pub token: String,
    pub expires_at: DateTime<Utc>,
    pub created_at: DateTime<Utc>,
    pub revoked: bool,
    pub device_info: Option<String>,
}

/// Запрос на обновление токена
#[derive(Debug, Deserialize)]
pub struct RefreshTokenRequest {
    pub refresh_token: String,
}

/// Ответ с токенами
#[derive(Debug, Serialize)]
pub struct TokenResponse {
    pub access_token: String,
    pub refresh_token: String,
    pub token_type: String,
    pub expires_in: i64,
}

/// OAuth2 провайдер
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum OAuthProvider {
    Google,
    Apple,
}

impl OAuthProvider {
    pub fn as_str(&self) -> &str {
        match self {
            OAuthProvider::Google => "google",
            OAuthProvider::Apple => "apple",
        }
    }
}

/// OAuth2 login request
#[derive(Debug, Deserialize)]
pub struct OAuthLoginRequest {
    pub provider: OAuthProvider,
    pub code: String,
    pub redirect_uri: Option<String>,
}

/// OAuth2 user info (generic)
#[derive(Debug, Serialize, Deserialize)]
pub struct OAuthUserInfo {
    pub provider: OAuthProvider,
    pub provider_user_id: String,
    pub email: String,
    pub name: Option<String>,
    pub picture: Option<String>,
}

/// OAuth2 account связь в БД
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OAuthAccount {
    pub id: i32,
    pub user_id: i32,
    pub provider: String,
    pub provider_user_id: String,
    pub access_token: Option<String>,
    pub refresh_token: Option<String>,
    pub expires_at: Option<DateTime<Utc>>,
    pub created_at: DateTime<Utc>,
}

/// MFA Secret для TOTP
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MfaSecret {
    pub id: i32,
    pub user_id: i32,
    pub secret: String,
    pub enabled: bool,
    pub backup_codes: Vec<String>,
    pub created_at: DateTime<Utc>,
}

/// Запрос на включение MFA
#[derive(Debug, Deserialize)]
pub struct EnableMfaRequest {
    pub code: String, // TOTP code для верификации
}

/// Ответ с QR кодом для MFA
#[derive(Debug, Serialize)]
pub struct MfaSetupResponse {
    pub secret: String,
    pub qr_code: String, // base64 encoded QR code image
    pub backup_codes: Vec<String>,
    pub otpauth_url: String,
}

/// Запрос на логин с MFA
#[derive(Debug, Deserialize)]
pub struct LoginWithMfaRequest {
    pub username: String,
    pub password: String,
    pub mfa_code: Option<String>,
}

/// Ответ на логин (может требовать MFA)
#[derive(Debug, Serialize)]
pub struct LoginResponse {
    #[serde(flatten)]
    pub tokens: Option<TokenResponse>,
    pub requires_mfa: bool,
    pub mfa_session_token: Option<String>,
}

/// MFA session для двухэтапной аутентификации
#[derive(Debug, Serialize, Deserialize)]
pub struct MfaSession {
    pub session_id: String,
    pub user_id: i32,
    pub expires_at: DateTime<Utc>,
}

/// Запрос на подтверждение MFA кода
#[derive(Debug, Deserialize)]
pub struct VerifyMfaRequest {
    pub session_token: String,
    pub mfa_code: String,
}

/// Модели для регистрации
#[derive(Debug, Deserialize)]
pub struct RegisterRequest {
    pub username: String,
    pub email: String,
    pub password: String,
}

#[derive(Debug, Serialize)]
pub struct RegisterResponse {
    pub user_id: i32,
    pub username: String,
    pub email: String,
}

/// User для JWT
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JwtUser {
    pub id: i32,
    pub username: String,
    pub email: String,
}
