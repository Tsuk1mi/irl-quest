/// Система достижений (Achievements)
use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

/// Достижение
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Achievement {
    pub id: i32,
    pub code: String,  // Уникальный код (quest_master, speed_demon, etc.)
    pub name: String,
    pub description: String,
    pub icon: String,  // Emoji или URL
    pub category: AchievementCategory,
    pub rarity: AchievementRarity,
    pub reward_experience: u32,
    pub reward_gold: u32,
    pub hidden: bool,  // Скрытые достижения
}

/// Категория достижений
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum AchievementCategory {
    Quests,      // Квесты
    Combat,      // Бои/вызовы
    Social,      // Социальные
    Exploration, // Исследование
    Collection,  // Коллекционирование
    Progression, // Прогрессия
}

/// Редкость достижения
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum AchievementRarity {
    Common,
    Rare,
    Epic,
    Legendary,
}

/// Прогресс достижения пользователя
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserAchievement {
    pub id: i32,
    pub user_id: i32,
    pub achievement_id: i32,
    pub progress: u32,
    pub required_progress: u32,
    pub completed: bool,
    pub completed_at: Option<DateTime<Utc>>,
    pub created_at: DateTime<Utc>,
}

impl UserAchievement {
    pub fn completion_percentage(&self) -> f32 {
        (self.progress as f32 / self.required_progress as f32 * 100.0).min(100.0)
    }
}

/// Информация о достижении для UI
#[derive(Debug, Serialize)]
pub struct AchievementInfo {
    pub achievement: Achievement,
    pub progress: Option<UserAchievement>,
    pub is_unlocked: bool,
}

/// Событие для триггера достижения
#[derive(Debug, Clone)]
pub enum AchievementEvent {
    QuestCompleted { difficulty: u8 },
    TaskCompleted { count: u32 },
    LevelReached { level: u8 },
    GoldEarned { amount: u32 },
    StreakAchieved { days: u32 },
    PartyJoined,
    FirstLogin,
}

