use crate::error::AppError;
use crate::middleware::auth::CurrentUser;
use crate::models::auth::*;
use crate::services::{AuthService, OAuthService};
use crate::state::AppState;
/// Handlers для расширенной аутентификации
/// JWT + Refresh tokens, OAuth2, MFA (TOTP)
use axum::{
    extract::{Extension, State},
    http::StatusCode,
    Json,
};

/// POST /api/auth/refresh - Обновить access token используя refresh token
pub async fn refresh_token(
    State(state): State<AppState>,
    Json(request): Json<RefreshTokenRequest>,
) -> Result<Json<TokenResponse>, AppError> {
    let auth_service = AuthService::new(state.config.clone());

    let tokens = auth_service
        .refresh_tokens(&state.db, &request.refresh_token)
        .await?;

    tracing::info!("Tokens refreshed successfully");
    Ok(Json(tokens))
}

/// POST /api/auth/login - Логин с username/password (с поддержкой MFA)
pub async fn login_extended(
    State(state): State<AppState>,
    Json(request): Json<LoginWithMfaRequest>,
) -> Result<Json<LoginResponse>, AppError> {
    let auth_service = AuthService::new(state.config.clone());

    let response = auth_service
        .login(&state.db, &request.username, &request.password, None)
        .await?;

    if response.requires_mfa {
        tracing::info!("MFA required for user: {}", request.username);
    } else {
        tracing::info!("User logged in successfully: {}", request.username);
    }

    Ok(Json(response))
}

/// POST /api/auth/mfa/verify - Верифицировать MFA код и завершить логин
pub async fn verify_mfa(
    State(state): State<AppState>,
    Json(request): Json<VerifyMfaRequest>,
) -> Result<Json<TokenResponse>, AppError> {
    let auth_service = AuthService::new(state.config.clone());

    let tokens = auth_service
        .verify_mfa_and_login(&state.db, &request.session_token, &request.mfa_code, None)
        .await?;

    tracing::info!("MFA verification successful");
    Ok(Json(tokens))
}

/// GET /api/auth/mfa/setup - Настроить MFA (генерация QR кода)
pub async fn setup_mfa(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<MfaSetupResponse>, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    let auth_service = AuthService::new(state.config.clone());

    let setup_response = auth_service.setup_mfa(&state.db, user.0.id).await?;

    tracing::info!("MFA setup initiated for user: {}", user.0.id);
    Ok(Json(setup_response))
}

/// POST /api/auth/mfa/enable - Включить MFA после верификации кода
pub async fn enable_mfa(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<EnableMfaRequest>,
) -> Result<StatusCode, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    let auth_service = AuthService::new(state.config.clone());

    auth_service
        .enable_mfa(&state.db, user.0.id, &request.code)
        .await?;

    tracing::info!("MFA enabled for user: {}", user.0.id);
    Ok(StatusCode::OK)
}

/// POST /api/auth/mfa/disable - Отключить MFA
pub async fn disable_mfa(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<StatusCode, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    let auth_service = AuthService::new(state.config.clone());

    auth_service.disable_mfa(&state.db, user.0.id).await?;

    tracing::info!("MFA disabled for user: {}", user.0.id);
    Ok(StatusCode::OK)
}

/// POST /api/auth/oauth/login - OAuth2 логин (Google или Apple)
pub async fn oauth_login(
    State(state): State<AppState>,
    Json(request): Json<OAuthLoginRequest>,
) -> Result<Json<TokenResponse>, AppError> {
    if !state.config.enable_oauth {
        return Err(AppError::NotImplemented("OAuth is disabled".to_string()));
    }

    let oauth_service = OAuthService::new(state.config.clone());
    let auth_service = AuthService::new(state.config.clone());

    // Аутентификация через OAuth провайдер
    let oauth_info = match request.provider {
        OAuthProvider::Google => {
            let redirect_uri = request
                .redirect_uri
                .as_deref()
                .unwrap_or("http://localhost:8003/oauth/callback");
            oauth_service
                .authenticate_google(&state.db, &request.code, redirect_uri)
                .await?
        }
        OAuthProvider::Apple => {
            let redirect_uri = request
                .redirect_uri
                .as_deref()
                .unwrap_or("http://localhost:8003/oauth/callback");
            oauth_service
                .authenticate_apple(&state.db, &request.code, redirect_uri)
                .await?
        }
    };

    // Найти или создать пользователя
    let user = oauth_service
        .find_or_create_user(&state.db, &oauth_info)
        .await?;

    // Создать токены
    let access_token = auth_service.create_access_token(&user)?;
    let refresh_token = auth_service
        .create_refresh_token(&state.db, user.id, None)
        .await?;

    tracing::info!("OAuth login successful for user: {}", user.id);

    Ok(Json(TokenResponse {
        access_token,
        refresh_token,
        token_type: "Bearer".to_string(),
        expires_in: (state.config.jwt_expiration_hours * 3600) as i64,
    }))
}

/// POST /api/auth/logout - Выйти и отозвать refresh tokens
pub async fn logout(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<StatusCode, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    let auth_service = AuthService::new(state.config.clone());

    // Отозвать все refresh tokens пользователя
    auth_service
        .revoke_all_refresh_tokens(&state.db, user.0.id)
        .await?;

    tracing::info!("User logged out: {}", user.0.id);
    Ok(StatusCode::OK)
}

/// GET /api/auth/sessions - Получить активные сессии (refresh tokens)
pub async fn get_active_sessions(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<Vec<RefreshToken>>, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    let sessions = sqlx::query_as::<_, RefreshToken>(
        r#"
        SELECT id, user_id, token, expires_at, created_at, 
               revoked, device_info
        FROM refresh_tokens
        WHERE user_id = $1 AND revoked = false AND expires_at > $2
        ORDER BY created_at DESC
        "#,
    )
    .bind(user.0.id)
    .bind(chrono::Utc::now().naive_utc())
    .fetch_all(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch sessions: {}", e)))?;

    Ok(Json(sessions))
}

/// DELETE /api/auth/sessions/:token - Отозвать конкретную сессию
pub async fn revoke_session(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    axum::extract::Path(token): axum::extract::Path<String>,
) -> Result<StatusCode, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    // Проверить, что токен принадлежит пользователю
    let result =
        sqlx::query("UPDATE refresh_tokens SET revoked = 1 WHERE token = $1 AND user_id = $2")
            .bind(&token)
            .bind(user.0.id)
            .execute(&state.db)
            .await
            .map_err(|e| AppError::DatabaseError(format!("Failed to revoke session: {}", e)))?;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFound("Session not found".to_string()));
    }

    tracing::info!("Session revoked for user: {}", user.0.id);
    Ok(StatusCode::OK)
}
