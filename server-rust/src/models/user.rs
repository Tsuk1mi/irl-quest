use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize)]
pub struct User {
    pub id: i32,
    pub email: String,
    pub username: String,
    pub hashed_password: String,
    pub is_active: bool,
    pub level: i32,
    pub experience: i32,
    pub avatar_url: Option<String>,
    pub bio: Option<String>,
    pub timezone: String,
    pub last_login: Option<DateTime<Utc>>,
    pub settings: JsonValue,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize)]
pub struct UserCreate {
    pub email: String,
    pub username: String,
    pub password: String,
    pub avatar_url: Option<String>,
    pub bio: Option<String>,
    pub timezone: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct UserOut {
    pub id: i32,
    pub email: String,
    pub username: String,
    pub is_active: bool,
    pub level: i32,
    pub experience: i32,
    pub avatar_url: Option<String>,
    pub bio: Option<String>,
    pub timezone: String,
    pub last_login: Option<DateTime<Utc>>,
    pub settings: JsonValue,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub struct UserStats {
    pub level: i32,
    pub experience: i32,
    pub total_quests: i64,
    pub completed_quests: i64,
    pub total_tasks: i64,
    pub completed_tasks: i64,
    pub achievements_count: i64,
}

impl From<User> for UserOut {
    fn from(user: User) -> Self {
        Self {
            id: user.id,
            email: user.email,
            username: user.username,
            is_active: user.is_active,
            level: user.level,
            experience: user.experience,
            avatar_url: user.avatar_url,
            bio: user.bio,
            timezone: user.timezone,
            last_login: user.last_login,
            settings: user.settings,
            created_at: user.created_at,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct UserUpdate {
    pub username: Option<String>,
    pub password: Option<String>,
    pub avatar_url: Option<String>,
    pub bio: Option<String>,
    pub timezone: Option<String>,
    pub settings: Option<JsonValue>,
}

#[derive(Debug, Serialize)]
pub struct Token {
    pub access_token: String,
    pub token_type: String,
    pub user: UserOut,
}

impl Token {
    pub fn new(access_token: String, user: UserOut) -> Self {
        Self {
            access_token,
            token_type: "bearer".to_string(),
            user,
        }
    }
}

#[derive(Debug, Deserialize)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
}

#[derive(Debug, Clone, FromRow, Serialize, Deserialize)]
pub struct UserAchievement {
    pub id: i32,
    pub user_id: i32,
    pub achievement_type: String,
    pub achievement_data: JsonValue,
    pub earned_at: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub struct UserAchievementOut {
    pub id: i32,
    pub achievement_type: String,
    pub achievement_data: JsonValue,
    pub earned_at: DateTime<Utc>,
}

impl From<UserAchievement> for UserAchievementOut {
    fn from(achievement: UserAchievement) -> Self {
        Self {
            id: achievement.id,
            achievement_type: achievement.achievement_type,
            achievement_data: achievement.achievement_data,
            earned_at: achievement.earned_at,
        }
    }
}