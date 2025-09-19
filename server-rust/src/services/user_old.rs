use anyhow::Result;
use bcrypt::{hash, DEFAULT_COST};
use sqlx::PgPool;

use crate::models::{User, UserOut, UserUpdate};

pub struct UserService;

impl UserService {
    pub async fn get_user_by_id(pool: &PgPool, user_id: i32) -> Result<Option<UserOut>> {
        let user = sqlx::query_as!(User, "SELECT * FROM users WHERE id = $1", user_id)
            .fetch_optional(pool)
            .await?;

        Ok(user.map(UserOut::from))
    }

    pub async fn update_user(
        pool: &PgPool,
        user_id: i32,
        user_update: UserUpdate,
    ) -> Result<Option<UserOut>> {
        // First get existing user
        let existing_user = sqlx::query_as!(User, "SELECT * FROM users WHERE id = $1", user_id)
            .fetch_optional(pool)
            .await?;

        let mut user = match existing_user {
            Some(user) => user,
            None => return Ok(None),
        };

        // Update fields if provided
        if let Some(username) = user_update.username {
            user.username = username;
        }

        if let Some(password) = user_update.password {
            user.hashed_password = hash(&password, DEFAULT_COST)?;
        }

        // Save updated user
        let updated_user = sqlx::query_as!(
            User,
            r#"
            UPDATE users 
            SET username = $1, hashed_password = $2
            WHERE id = $3
            RETURNING id, email, username, hashed_password, is_active, created_at
            "#,
            user.username,
            user.hashed_password,
            user_id
        )
        .fetch_one(pool)
        .await?;

        Ok(Some(UserOut::from(updated_user)))
    }
}