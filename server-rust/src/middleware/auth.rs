use axum::{
    extract::{Request, State, FromRequestParts},
    http::{header, StatusCode, request::Parts},
    middleware::Next,
    response::Response,
};
use std::sync::Arc;

use crate::{models::User, services::AuthService, AppState};

#[derive(Clone)]
pub struct CurrentUser(pub User);

#[axum::async_trait]
impl<S> FromRequestParts<S> for CurrentUser
where
    S: Send + Sync,
{
    type Rejection = StatusCode;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        parts
            .extensions
            .get::<CurrentUser>()
            .cloned()
            .ok_or(StatusCode::UNAUTHORIZED)
    }
}

pub async fn auth_middleware(
    State(state): State<Arc<AppState>>,
    mut req: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    let auth_header = req
        .headers()
        .get(header::AUTHORIZATION)
        .and_then(|header| header.to_str().ok());

    let auth_header = match auth_header {
        Some(header) => header,
        None => return Err(StatusCode::UNAUTHORIZED),
    };

    if !auth_header.starts_with("Bearer ") {
        return Err(StatusCode::UNAUTHORIZED);
    }

    let token = &auth_header[7..]; // Remove "Bearer " prefix

    let auth_service = AuthService::new(state.settings.clone());
    
    let token_data = match auth_service.verify_token(token) {
        Ok(data) => data,
        Err(_) => return Err(StatusCode::UNAUTHORIZED),
    };

    let user_id: i32 = match token_data.claims.sub.parse() {
        Ok(id) => id,
        Err(_) => return Err(StatusCode::UNAUTHORIZED),
    };

    let user = match auth_service.get_user_by_id(&state.db, user_id).await {
        Ok(Some(user)) => user,
        _ => return Err(StatusCode::UNAUTHORIZED),
    };

    // Add user to request extensions
    req.extensions_mut().insert(CurrentUser(user));

    Ok(next.run(req).await)
}