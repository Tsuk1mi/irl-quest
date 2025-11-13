use axum::{
    extract::{Extension, State},
    Json,
};
use serde::Deserialize;

use crate::{
    error::AppError,
    middleware::auth::CurrentUser,
    models::user::{User, UserOut},
    state::AppState,
};

#[derive(Debug, Deserialize)]
pub struct ProfileUpdateRequest {
    pub username: Option<String>,
    #[serde(rename = "avatar_url")]
    pub avatar_url: Option<String>,
    pub bio: Option<String>,
}

fn map_db_error(err: sqlx::Error) -> AppError {
    if let sqlx::Error::Database(db_err) = &err {
        if db_err.code().as_deref() == Some("23505") {
            return AppError::BadRequest("Это имя уже занято другим героем".into());
        }
        return AppError::DatabaseError(db_err.message().to_string());
    }
    AppError::Database(err)
}

pub async fn update_profile(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<ProfileUpdateRequest>,
) -> Result<Json<UserOut>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для изменения профиля".into())
    })?;

    let mut profile = sqlx::query_as::<_, User>(
        r#"
        SELECT *
        FROM users
        WHERE id = $1
        "#,
    )
    .bind(user.0.id)
    .fetch_optional(&state.db)
    .await
    .map_err(map_db_error)?
    .ok_or_else(|| AppError::NotFound("Пользователь не найден".into()))?;

    if let Some(username) = req.username {
        let trimmed = username.trim();
        if trimmed.len() < 3 {
            return Err(AppError::Validation(
                "Имя должно содержать минимум 3 символа".into(),
            ));
        }
        profile.username = trimmed.to_string();
    }

    if let Some(avatar_url) = req.avatar_url {
        if !avatar_url.is_empty() && !avatar_url.starts_with("http") {
            return Err(AppError::Validation(
                "Укажите ссылку на изображение (http/https)".into(),
            ));
        }
        profile.avatar_url = if avatar_url.trim().is_empty() {
            None
        } else {
            Some(avatar_url.trim().to_string())
        };
    }

    if let Some(bio) = req.bio {
        profile.bio = if bio.trim().is_empty() {
            None
        } else {
            Some(bio.trim().to_string())
        };
    }

    let updated = sqlx::query_as::<_, User>(
        r#"
        UPDATE users
        SET username = $1,
            avatar_url = $2,
            bio = $3
        WHERE id = $4
        RETURNING *
        "#,
    )
    .bind(&profile.username)
    .bind(&profile.avatar_url)
    .bind(&profile.bio)
    .bind(profile.id)
    .fetch_one(&state.db)
    .await
    .map_err(map_db_error)?;

    Ok(Json(UserOut::from(updated)))
}
