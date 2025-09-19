use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow, Serialize, Deserialize)]
pub struct Team {
    pub id: i32,
    pub name: String,
    pub description: Option<String>,
    pub avatar_url: Option<String>,
    pub owner_id: i32,
    pub is_public: bool,
    pub max_members: i32,
    pub created_at: DateTime<Utc>,
    pub settings: JsonValue,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct TeamCreate {
    pub name: String,
    pub description: Option<String>,
    pub avatar_url: Option<String>,
    pub is_public: Option<bool>,
    pub max_members: Option<i32>,
    pub settings: Option<JsonValue>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct TeamUpdate {
    pub name: Option<String>,
    pub description: Option<String>,
    pub avatar_url: Option<String>,
    pub is_public: Option<bool>,
    pub max_members: Option<i32>,
    pub settings: Option<JsonValue>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct TeamOut {
    pub id: i32,
    pub name: String,
    pub description: Option<String>,
    pub avatar_url: Option<String>,
    pub owner_id: i32,
    pub is_public: bool,
    pub max_members: i32,
    pub created_at: DateTime<Utc>,
    pub member_count: Option<i64>,
}

#[derive(Debug, Clone, FromRow, Serialize, Deserialize)]
pub struct TeamMembership {
    pub id: i32,
    pub team_id: i32,
    pub user_id: i32,
    pub role: String,
    pub joined_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct TeamMembershipCreate {
    pub user_id: i32,
    pub role: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct TeamMemberOut {
    pub user_id: i32,
    pub username: String,
    pub role: String,
    pub joined_at: DateTime<Utc>,
}

impl From<Team> for TeamOut {
    fn from(team: Team) -> Self {
        Self {
            id: team.id,
            name: team.name,
            description: team.description,
            avatar_url: team.avatar_url,
            owner_id: team.owner_id,
            is_public: team.is_public,
            max_members: team.max_members,
            created_at: team.created_at,
            member_count: None,
        }
    }
}