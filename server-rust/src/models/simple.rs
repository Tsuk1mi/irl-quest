use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

// Упрощенная модель пользователя для быстрого запуска
#[derive(Debug, Clone, FromRow, Serialize)]
pub struct SimpleUser {
    pub id: i32,
    pub email: String,
    pub username: String,
    pub hashed_password: String,
    pub is_active: bool,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize)]
pub struct SimpleUserCreate {
    pub email: String,
    pub username: String,
    pub password: String,
}

#[derive(Debug, Serialize)]
pub struct SimpleUserOut {
    pub id: i32,
    pub email: String,
    pub username: String,
    pub is_active: bool,
    pub created_at: DateTime<Utc>,
}

impl From<SimpleUser> for SimpleUserOut {
    fn from(user: SimpleUser) -> Self {
        Self {
            id: user.id,
            email: user.email,
            username: user.username,
            is_active: user.is_active,
            created_at: user.created_at,
        }
    }
}

// Упрощенная модель задач
#[derive(Debug, Clone, FromRow, Serialize)]
pub struct SimpleTask {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub created_at: DateTime<Utc>,
    pub owner_id: i32,
}

#[derive(Debug, Deserialize)]
pub struct SimpleTaskCreate {
    pub title: String,
    pub description: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct SimpleTaskUpdate {
    pub title: Option<String>,
    pub description: Option<String>,
    pub completed: Option<bool>,
}

#[derive(Debug, Serialize)]
pub struct SimpleTaskOut {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub created_at: DateTime<Utc>,
    pub owner_id: i32,
}

impl From<SimpleTask> for SimpleTaskOut {
    fn from(task: SimpleTask) -> Self {
        Self {
            id: task.id,
            title: task.title,
            description: task.description,
            completed: task.completed,
            created_at: task.created_at,
            owner_id: task.owner_id,
        }
    }
}

// Упрощенная модель квестов
#[derive(Debug, Clone, FromRow, Serialize)]
pub struct SimpleQuest {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub created_at: DateTime<Utc>,
    pub owner_id: i32,
}

#[derive(Debug, Deserialize)]
pub struct SimpleQuestCreate {
    pub title: String,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
}

#[derive(Debug, Deserialize)]
pub struct SimpleQuestUpdate {
    pub title: Option<String>,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
}

#[derive(Debug, Serialize)]
pub struct SimpleQuestOut {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub created_at: DateTime<Utc>,
    pub owner_id: i32,
}

impl From<SimpleQuest> for SimpleQuestOut {
    fn from(quest: SimpleQuest) -> Self {
        Self {
            id: quest.id,
            title: quest.title,
            description: quest.description,
            difficulty: quest.difficulty,
            created_at: quest.created_at,
            owner_id: quest.owner_id,
        }
    }
}

// Токен и аутентификация
#[derive(Debug, Serialize)]
pub struct SimpleToken {
    pub access_token: String,
    pub token_type: String,
    pub user: SimpleUserOut,
}

impl SimpleToken {
    pub fn new(access_token: String, user: SimpleUserOut) -> Self {
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