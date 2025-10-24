use axum::{
    body::Body,
    extract::State,
    http::{Request, StatusCode},
    middleware::Next,
    response::Response,
};
use jsonwebtoken::{decode, DecodingKey, Validation};
use serde::{Deserialize, Serialize};
use crate::state::AppState;
use tracing::{info, warn};
use axum::body::Body as AxumBody;
use serde_json::json;
use sqlx::Row;
use crate::models::User;

// Public wrapper type to store authenticated user in request extensions
#[derive(Clone, Debug)]
pub struct CurrentUser(pub User);

fn json_error_response(status: StatusCode, message: &str) -> Response {
    let body = json!({ "error": message }).to_string();
    Response::builder()
        .status(status)
        .header("content-type", "application/json")
        .body(AxumBody::from(body))
        .unwrap()
}

#[derive(Debug, Serialize, Deserialize)]
struct Claims {
    sub: i32,
    exp: i64,
    iat: i64,
}

pub async fn auth_middleware(
    State(state): State<AppState>,
    mut req: Request<Body>,
    next: Next,
) -> Result<Response, StatusCode> {
    // Log incoming auth-related headers for debugging (don't log sensitive headers fully)
    info!("[auth::middleware] headers={:?}", req.headers());

    // Extract Authorization header
    let auth_header = match req.headers().get("Authorization") {
        Some(h) => match h.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => {
                warn!("[auth::middleware] Authorization header present but invalid utf8");
                // Treat as anonymous: insert None and continue
                req.extensions_mut().insert::<Option<CurrentUser>>(None);
                return Ok(next.run(req).await);
            }
        },
        None => {
            warn!("[auth::middleware] Authorization header missing");
            // Treat as anonymous: insert None and continue
            req.extensions_mut().insert::<Option<CurrentUser>>(None);
            return Ok(next.run(req).await);
        }
    };

    // Expect Bearer token
    let token = if let Some(t) = auth_header.strip_prefix("Bearer ") {
        t
    } else {
        warn!("[auth::middleware] Authorization header does not contain Bearer token");
        // Treat as anonymous: insert None and continue
        req.extensions_mut().insert::<Option<CurrentUser>>(None);
        return Ok(next.run(req).await);
    };

    let jwt_secret = std::env::var("JWT_SECRET").unwrap_or_else(|_| "your-secret-key".to_string());

    let token_data = match decode::<Claims>(
        token,
        &DecodingKey::from_secret(jwt_secret.as_bytes()),
        &Validation::default(),
    ) {
        Ok(data) => data,
        Err(e) => {
            warn!("[auth::middleware] Failed to decode JWT: {:?}", e);
            return Ok(json_error_response(StatusCode::UNAUTHORIZED, "Invalid or expired token"));
        }
    };

    // Проверяем существование пользователя и загружаем полную запись
    let user_row_opt = match sqlx::query_as::<_, User>(
        "SELECT id, email, username, hashed_password, is_active, level, experience, gold, \
         avatar_url, bio, timezone, last_login, settings, \
         strength, intelligence, charisma, dexterity, constitution, wisdom, \
         character_class, character_race, created_at \
         FROM users WHERE id = $1",
    )
    .bind(token_data.claims.sub)
    .fetch_optional(&state.db)
    .await
    {
        Ok(opt) => opt,
        Err(e) => {
            warn!("[auth::middleware] DB error when fetching user: {:?}", e);
            return Ok(json_error_response(StatusCode::INTERNAL_SERVER_ERROR, "Database error while verifying token"));
        }
    };

    let current_user = match user_row_opt {
        Some(user) => user,
        None => {
            warn!("[auth::middleware] No user found with id={} (from token)", token_data.claims.sub);
            return Ok(json_error_response(StatusCode::UNAUTHORIZED, "User from token not found"));
        }
    };

    // Insert CurrentUser (wrapped in Some) into extensions so handlers can extract Option<CurrentUser>
    req.extensions_mut().insert::<Option<CurrentUser>>(Some(CurrentUser(current_user)));

    Ok(next.run(req).await)
}
