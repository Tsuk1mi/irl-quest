use axum::{
    extract::{Request, State},
    http::{header::AUTHORIZATION, StatusCode},
    middleware::Next,
    response::Response,
};
use std::sync::Arc;

use crate::{config::Settings, models::User, services::AuthService, AppState};

pub async fn auth_middleware(
    State(state): State<Arc<AppState>>,
    mut req: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    let auth_header = req
        .headers()
        .get(AUTHORIZATION)
        .and_then(|header| header.to_str().ok());

    if let Some(auth_header) = auth_header {
        if let Some(token) = auth_header.strip_prefix("Bearer ") {
            let auth_service = AuthService::new(state.settings.clone());
            
            match auth_service.verify_token(token) {
                Ok(claims) => {
                    let user_id: i32 = claims.claims.sub.parse().map_err(|_| StatusCode::UNAUTHORIZED)?;
                    
                    match auth_service.get_user_by_id(&state.db, user_id).await {
                        Ok(Some(user)) => {
                            // Add user to request extensions
                            req.extensions_mut().insert(user);
                            return Ok(next.run(req).await);
                        }
                        _ => return Err(StatusCode::UNAUTHORIZED),
                    }
                }
                Err(_) => return Err(StatusCode::UNAUTHORIZED),
            }
        }
    }

    Err(StatusCode::UNAUTHORIZED)
}

// Extractor for getting current user from request
use axum::{async_trait, extract::FromRequestParts, http::request::Parts};

#[derive(Debug)]
pub struct CurrentUser(pub User);

#[async_trait]
impl<S> FromRequestParts<S> for CurrentUser
where
    S: Send + Sync,
{
    type Rejection = StatusCode;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        parts
            .extensions
            .get::<User>()
            .cloned()
            .map(CurrentUser)
            .ok_or(StatusCode::UNAUTHORIZED)
    }
}