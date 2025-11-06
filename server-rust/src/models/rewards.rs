/// Модели для системы наград и экономики
use serde::{Deserialize, Serialize};

/// Базовые награды
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Rewards {
    pub experience: u32,
    pub gold: u32,
    pub items: Vec<ItemReward>,
}

/// Предмет в награде
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ItemReward {
    pub item_id: String,
    pub item_name: String,
    pub quantity: u32,
    pub rarity: ItemRarity,
}

/// Редкость предметов
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum ItemRarity {
    Common,
    Uncommon,
    Rare,
    Epic,
    Legendary,
}

impl ItemRarity {
    pub fn multiplier(&self) -> f32 {
        match self {
            ItemRarity::Common => 1.0,
            ItemRarity::Uncommon => 1.5,
            ItemRarity::Rare => 2.0,
            ItemRarity::Epic => 3.0,
            ItemRarity::Legendary => 5.0,
        }
    }
}

/// Модификаторы наград
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RewardModifiers {
    /// Множитель от сложности (1.0 - 3.0)
    pub difficulty_multiplier: f32,
    
    /// Бонус от характеристик персонажа
    pub character_stat_bonus: f32,
    
    /// Множитель мультиплеера (1.0 - 2.0)
    pub multiplayer_bonus: f32,
    
    /// Бонус за первое выполнение
    pub first_completion_bonus: f32,
    
    /// Бонус за стрик (последовательное выполнение)
    pub streak_bonus: f32,
    
    /// Бонус временного события
    pub event_bonus: f32,
    
    /// Penalty за повторное выполнение (анти-фарм)
    pub repeat_penalty: f32,
}

impl Default for RewardModifiers {
    fn default() -> Self {
        Self {
            difficulty_multiplier: 1.0,
            character_stat_bonus: 0.0,
            multiplayer_bonus: 1.0,
            first_completion_bonus: 0.0,
            streak_bonus: 0.0,
            event_bonus: 0.0,
            repeat_penalty: 0.0,
        }
    }
}

impl RewardModifiers {
    /// Рассчитать итоговый множитель
    pub fn total_multiplier(&self) -> f32 {
        let base = self.difficulty_multiplier * self.multiplayer_bonus;
        let bonuses = self.character_stat_bonus 
            + self.first_completion_bonus 
            + self.streak_bonus 
            + self.event_bonus;
        let total = base + bonuses - self.repeat_penalty;
        total.max(0.1) // Минимум 10% от базовой награды
    }
}

/// Конфигурация наград для квеста
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QuestRewardConfig {
    pub base_experience: u32,
    pub base_gold: u32,
    pub guaranteed_items: Vec<ItemReward>,
    pub possible_items: Vec<ItemDropChance>,
    pub modifiers: RewardModifiers,
}

/// Шанс выпадения предмета
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ItemDropChance {
    pub item: ItemReward,
    pub drop_chance: f32, // 0.0 - 1.0
}

/// Итоговые награды после всех модификаторов
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FinalRewards {
    pub experience: u32,
    pub gold: u32,
    pub items: Vec<ItemReward>,
    pub breakdown: RewardBreakdown,
}

/// Разбивка расчета наград (для прозрачности)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RewardBreakdown {
    pub base_experience: u32,
    pub base_gold: u32,
    pub modifiers_applied: RewardModifiers,
    pub total_multiplier: f32,
    pub bonus_experience: u32,
    pub bonus_gold: u32,
}

/// Экономические лимиты (soft caps)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EconomyCaps {
    /// Максимум XP в день
    pub daily_experience_cap: u32,
    
    /// Максимум золота в день
    pub daily_gold_cap: u32,
    
    /// Максимум предметов в день
    pub daily_items_cap: u32,
    
    /// Множитель после достижения soft cap (0.0-1.0)
    pub soft_cap_multiplier: f32,
}

impl Default for EconomyCaps {
    fn default() -> Self {
        Self {
            daily_experience_cap: 10000,
            daily_gold_cap: 5000,
            daily_items_cap: 20,
            soft_cap_multiplier: 0.5, // После cap получаем 50%
        }
    }
}

/// Статистика заработанных наград за день
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DailyRewardStats {
    pub user_id: i32,
    pub date: String, // YYYY-MM-DD
    pub total_experience: u32,
    pub total_gold: u32,
    pub total_items: u32,
    pub quests_completed: u32,
}

impl DailyRewardStats {
    /// Проверить, достигнут ли soft cap
    pub fn check_soft_cap(&self, caps: &EconomyCaps) -> SoftCapStatus {
        SoftCapStatus {
            experience_capped: self.total_experience >= caps.daily_experience_cap,
            gold_capped: self.total_gold >= caps.daily_gold_cap,
            items_capped: self.total_items >= caps.daily_items_cap,
        }
    }
}

/// Статус soft cap
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SoftCapStatus {
    pub experience_capped: bool,
    pub gold_capped: bool,
    pub items_capped: bool,
}

impl SoftCapStatus {
    pub fn any_capped(&self) -> bool {
        self.experience_capped || self.gold_capped || self.items_capped
    }
}

/// Запрос на расчет наград
#[derive(Debug, Deserialize)]
pub struct CalculateRewardsRequest {
    pub quest_id: i32,
    pub difficulty: u8,
    pub party_size: Option<u8>,
    pub is_first_completion: Option<bool>,
    pub current_streak: Option<u32>,
}

/// Стрик (последовательное выполнение)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StreakInfo {
    pub current_streak: u32,
    pub best_streak: u32,
    pub last_completion_date: String,
}

impl StreakInfo {
    /// Рассчитать бонус от стрика
    pub fn calculate_bonus(&self) -> f32 {
        // 5% за каждый день стрика, максимум 50%
        (self.current_streak as f32 * 0.05).min(0.5)
    }
}

