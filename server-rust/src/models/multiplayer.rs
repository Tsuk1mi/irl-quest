use chrono::{DateTime, Utc};
/// Модели для мультиплеера (гильдии, кооп-миссии, чаты)
use serde::{Deserialize, Serialize};

/// Гильдия
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Guild {
    pub id: i32,
    pub name: String,
    pub description: Option<String>,
    pub leader_id: i32,
    pub level: i32,
    pub experience: i32,
    pub member_count: i32,
    pub max_members: i32,
    pub created_at: DateTime<Utc>,
}

/// Запрос на создание гильдии
#[derive(Debug, Deserialize)]
pub struct CreateGuildRequest {
    pub name: String,
    pub description: Option<String>,
    pub max_members: Option<u32>,
}

/// Член гильдии
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GuildMember {
    pub id: i32,
    pub guild_id: i32,
    pub user_id: i32,
    pub role: GuildRole,
    pub joined_at: DateTime<Utc>,
}

/// Роль в гильдии
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum GuildRole {
    Leader,
    Officer,
    Member,
}

/// Кооп-миссия
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CoopMission {
    pub id: i32,
    pub quest_id: i32,
    pub party_size: i32,
    pub max_party_size: i32,
    pub leader_id: i32,
    pub status: MissionStatus,
    pub is_public: bool,
    pub created_at: DateTime<Utc>,
}

/// Статус миссии
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum MissionStatus {
    Recruiting, // Набор участников
    InProgress, // В процессе
    Completed,  // Завершена
    Failed,     // Провалена
}

/// Участник миссии
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PartyMember {
    pub id: i32,
    pub mission_id: i32,
    pub user_id: i32,
    pub role: PartyRole,
    pub contribution: i32, // Вклад в выполнение
    pub joined_at: DateTime<Utc>,
}

/// Роль в группе (Tank, DPS, Healer)
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum PartyRole {
    Tank,    // Танк - защитник
    Dps,     // DPS - урон
    Healer,  // Хилер - поддержка
    Support, // Саппорт - универсал
}

impl PartyRole {
    pub fn name_ru(&self) -> &str {
        match self {
            PartyRole::Tank => "Танк",
            PartyRole::Dps => "Боец",
            PartyRole::Healer => "Целитель",
            PartyRole::Support => "Поддержка",
        }
    }
}

/// Сообщение в WebSocket
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum WsMessage {
    // Client -> Server
    #[serde(rename = "join_room")]
    JoinRoom { room_id: String },

    #[serde(rename = "leave_room")]
    LeaveRoom { room_id: String },

    #[serde(rename = "chat_message")]
    ChatMessage { room_id: String, message: String },

    #[serde(rename = "quest_update")]
    QuestUpdate { quest_id: i32, progress: u8 },

    #[serde(rename = "ping")]
    Ping,

    // Server -> Client
    #[serde(rename = "pong")]
    Pong,

    #[serde(rename = "user_joined")]
    UserJoined {
        room_id: String,
        user_id: i32,
        username: String,
    },

    #[serde(rename = "user_left")]
    UserLeft { room_id: String, user_id: i32 },

    #[serde(rename = "chat")]
    Chat {
        room_id: String,
        user_id: i32,
        username: String,
        message: String,
        timestamp: i64,
    },

    #[serde(rename = "quest_progress")]
    QuestProgress {
        quest_id: i32,
        progress: u8,
        user_id: i32,
    },

    #[serde(rename = "party_update")]
    PartyUpdate { party: Vec<PartyMemberInfo> },

    #[serde(rename = "error")]
    Error { message: String },
}

/// Информация об участнике группы для UI
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PartyMemberInfo {
    pub user_id: i32,
    pub username: String,
    pub level: i32,
    pub class: String,
    pub role: PartyRole,
    pub is_ready: bool,
}

/// Matchmaking запрос
#[derive(Debug, Deserialize)]
pub struct MatchmakingRequest {
    pub quest_difficulty: i32,
    pub preferred_role: PartyRole,
    pub min_party_size: i32,
    pub max_party_size: i32,
}

/// Matchmaking результат
#[derive(Debug, Serialize)]
pub struct MatchmakingResult {
    pub mission_id: Option<i32>,
    pub status: MatchmakingStatus,
    pub estimated_wait_time_seconds: u32,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum MatchmakingStatus {
    Searching,
    Found,
    Failed,
}

/// Приглашение в группу
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PartyInvitation {
    pub id: i32,
    pub mission_id: i32,
    pub from_user_id: i32,
    pub to_user_id: i32,
    pub status: InvitationStatus,
    pub created_at: DateTime<Utc>,
    pub expires_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum InvitationStatus {
    Pending,
    Accepted,
    Declined,
    Expired,
}

/// Запрос на создание кооп-миссии
#[derive(Debug, Deserialize)]
pub struct CreateCoopMissionRequest {
    pub quest_id: i32,
    pub max_party_size: i32,
    pub is_public: bool,
}

/// Запрос на присоединение к миссии
#[derive(Debug, Deserialize)]
pub struct JoinMissionRequest {
    pub mission_id: i32,
    pub preferred_role: PartyRole,
}
