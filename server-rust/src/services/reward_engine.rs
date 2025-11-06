/// Reward Engine - Система расчета наград с модификаторами
use crate::models::rewards::*;
use chrono::Utc;
use rand::Rng;

pub struct RewardEngine {
    economy_caps: EconomyCaps,
}

impl RewardEngine {
    pub fn new(economy_caps: EconomyCaps) -> Self {
        Self { economy_caps }
    }

    /// Создать конфигурацию наград на основе сложности
    pub fn create_reward_config(
        &self,
        difficulty: u8,
        quest_type: QuestType,
    ) -> QuestRewardConfig {
        // Базовые награды на основе сложности
        let base_experience = (difficulty as u32) * 50;
        let base_gold = (difficulty as u32) * 20;

        // Гарантированные предметы для высокой сложности
        let guaranteed_items = if difficulty >= 8 {
            vec![ItemReward {
                item_id: "epic_chest".to_string(),
                item_name: "Эпический сундук".to_string(),
                quantity: 1,
                rarity: ItemRarity::Epic,
            }]
        } else if difficulty >= 5 {
            vec![ItemReward {
                item_id: "rare_artifact".to_string(),
                item_name: "Редкий артефакт".to_string(),
                quantity: 1,
                rarity: ItemRarity::Rare,
            }]
        } else {
            vec![]
        };

        // Возможные выпадения предметов
        let possible_items = vec![
            ItemDropChance {
                item: ItemReward {
                    item_id: "health_potion".to_string(),
                    item_name: "Зелье здоровья".to_string(),
                    quantity: 1,
                    rarity: ItemRarity::Common,
                },
                drop_chance: 0.5,
            },
            ItemDropChance {
                item: ItemReward {
                    item_id: "experience_boost".to_string(),
                    item_name: "Усилитель опыта".to_string(),
                    quantity: 1,
                    rarity: ItemRarity::Uncommon,
                },
                drop_chance: 0.3,
            },
            ItemDropChance {
                item: ItemReward {
                    item_id: "legendary_item".to_string(),
                    item_name: "Легендарный предмет".to_string(),
                    quantity: 1,
                    rarity: ItemRarity::Legendary,
                },
                drop_chance: 0.05,
            },
        ];

        QuestRewardConfig {
            base_experience,
            base_gold,
            guaranteed_items,
            possible_items,
            modifiers: RewardModifiers::default(),
        }
    }

    /// Рассчитать модификаторы наград
    pub fn calculate_modifiers(
        &self,
        difficulty: u8,
        party_size: u8,
        character_stats: &CharacterStats,
        is_first_completion: bool,
        streak: u32,
        is_event_active: bool,
        times_completed: u32,
    ) -> RewardModifiers {
        // Модификатор сложности (1.0 - 3.0)
        let difficulty_multiplier = 1.0 + (difficulty as f32 - 1.0) * 0.2;

        // Бонус от характеристик (до +30%)
        let character_stat_bonus = self.calculate_character_bonus(character_stats, difficulty);

        // Мультиплеер бонус (1.1x за каждого игрока сверх одного)
        let multiplayer_bonus = if party_size > 1 {
            1.0 + (party_size as f32 - 1.0) * 0.1
        } else {
            1.0
        };

        // Бонус за первое выполнение (+50%)
        let first_completion_bonus = if is_first_completion { 0.5 } else { 0.0 };

        // Бонус от стрика (5% за каждый день, макс 50%)
        let streak_bonus = (streak as f32 * 0.05).min(0.5);

        // Бонус события (+25%)
        let event_bonus = if is_event_active { 0.25 } else { 0.0 };

        // Penalty за повторное выполнение (анти-фарм)
        let repeat_penalty = if times_completed > 3 {
            (times_completed as f32 - 3.0) * 0.1
        } else {
            0.0
        };

        RewardModifiers {
            difficulty_multiplier,
            character_stat_bonus,
            multiplayer_bonus,
            first_completion_bonus,
            streak_bonus,
            event_bonus,
            repeat_penalty,
        }
    }

    /// Рассчитать итоговые награды
    pub fn calculate_final_rewards(
        &self,
        config: &QuestRewardConfig,
        daily_stats: &DailyRewardStats,
    ) -> FinalRewards {
        let total_multiplier = config.modifiers.total_multiplier();

        // Рассчитать базовые награды с модификаторами
        let mut experience = (config.base_experience as f32 * total_multiplier) as u32;
        let mut gold = (config.base_gold as f32 * total_multiplier) as u32;

        // Проверить soft caps
        let soft_cap_status = daily_stats.check_soft_cap(&self.economy_caps);

        // Применить soft cap если достигнут
        if soft_cap_status.experience_capped {
            experience = (experience as f32 * self.economy_caps.soft_cap_multiplier) as u32;
        }
        if soft_cap_status.gold_capped {
            gold = (gold as f32 * self.economy_caps.soft_cap_multiplier) as u32;
        }

        // Собрать все предметы
        let mut items = config.guaranteed_items.clone();

        // Случайные предметы (roll)
        if !soft_cap_status.items_capped {
            for drop in &config.possible_items {
                if self.roll_item_drop(drop.drop_chance) {
                    items.push(drop.item.clone());
                }
            }
        }

        let breakdown = RewardBreakdown {
            base_experience: config.base_experience,
            base_gold: config.base_gold,
            modifiers_applied: config.modifiers.clone(),
            total_multiplier,
            bonus_experience: experience.saturating_sub(config.base_experience),
            bonus_gold: gold.saturating_sub(config.base_gold),
        };

        FinalRewards {
            experience,
            gold,
            items,
            breakdown,
        }
    }

    /// Рассчитать бонус от характеристик персонажа
    fn calculate_character_bonus(&self, stats: &CharacterStats, difficulty: u8) -> f32 {
        // Сила влияет на физические квесты
        // Интеллект на умственные
        // Ловкость на скорость
        // Харизма на социальные
        
        let relevant_stat = match difficulty {
            1..=3 => stats.strength,
            4..=6 => stats.intelligence,
            7..=9 => stats.dexterity,
            _ => stats.charisma,
        };

        // Бонус: 1% за каждую единицу характеристики, макс 30%
        (relevant_stat as f32 * 0.01).min(0.3)
    }

    /// Roll для выпадения предмета
    fn roll_item_drop(&self, chance: f32) -> bool {
        let mut rng = rand::thread_rng();
        let roll: f32 = rng.gen();
        roll < chance
    }

    /// Обновить ежедневную статистику
    pub fn update_daily_stats(
        &self,
        stats: &mut DailyRewardStats,
        rewards: &FinalRewards,
    ) {
        stats.total_experience += rewards.experience;
        stats.total_gold += rewards.gold;
        stats.total_items += rewards.items.len() as u32;
        stats.quests_completed += 1;
    }

    /// Проверить и предупредить о приближении к soft cap
    pub fn check_cap_warning(&self, stats: &DailyRewardStats) -> Option<CapWarning> {
        let exp_percent = (stats.total_experience as f32 / self.economy_caps.daily_experience_cap as f32) * 100.0;
        let gold_percent = (stats.total_gold as f32 / self.economy_caps.daily_gold_cap as f32) * 100.0;

        if exp_percent >= 80.0 || gold_percent >= 80.0 {
            Some(CapWarning {
                experience_percent: exp_percent,
                gold_percent: gold_percent,
                message: "Вы приближаетесь к дневному лимиту наград!".to_string(),
            })
        } else {
            None
        }
    }
}

/// Тип квеста (влияет на награды)
#[derive(Debug, Clone, Copy)]
pub enum QuestType {
    Daily,
    Weekly,
    Story,
    Event,
    Repeatable,
}

/// Характеристики персонажа
#[derive(Debug, Clone)]
pub struct CharacterStats {
    pub strength: u8,
    pub intelligence: u8,
    pub dexterity: u8,
    pub charisma: u8,
    pub luck: u8,
}

impl Default for CharacterStats {
    fn default() -> Self {
        Self {
            strength: 10,
            intelligence: 10,
            dexterity: 10,
            charisma: 10,
            luck: 10,
        }
    }
}

/// Предупреждение о приближении к cap
#[derive(Debug, Clone)]
pub struct CapWarning {
    pub experience_percent: f32,
    pub gold_percent: f32,
    pub message: String,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_reward_calculation() {
        let engine = RewardEngine::new(EconomyCaps::default());
        let config = engine.create_reward_config(5, QuestType::Daily);
        
        assert_eq!(config.base_experience, 250); // 5 * 50
        assert_eq!(config.base_gold, 100); // 5 * 20
    }

    #[test]
    fn test_modifiers() {
        let engine = RewardEngine::new(EconomyCaps::default());
        let stats = CharacterStats::default();
        
        let modifiers = engine.calculate_modifiers(
            5,  // difficulty
            1,  // solo
            &stats,
            true,  // first completion
            0,     // no streak
            false, // no event
            0,     // first time
        );

        assert!(modifiers.difficulty_multiplier > 1.0);
        assert_eq!(modifiers.first_completion_bonus, 0.5);
    }

    #[test]
    fn test_soft_cap() {
        let engine = RewardEngine::new(EconomyCaps::default());
        let stats = DailyRewardStats {
            user_id: 1,
            date: "2025-10-31".to_string(),
            total_experience: 15000, // Выше cap (10000)
            total_gold: 3000,
            total_items: 5,
            quests_completed: 10,
        };

        let soft_cap = stats.check_soft_cap(&engine.economy_caps);
        assert!(soft_cap.experience_capped);
        assert!(!soft_cap.gold_capped);
    }
}

