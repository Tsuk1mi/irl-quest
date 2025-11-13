/// Модели для системы персонажей (классы, расы, характеристики)
use serde::{Deserialize, Serialize};

/// Класс персонажа
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum CharacterClass {
    Warrior, // Воин
    Mage,    // Маг
    Rogue,   // Вор
    Cleric,  // Жрец/Целитель
}

impl CharacterClass {
    pub fn name_ru(&self) -> &str {
        match self {
            CharacterClass::Warrior => "Воин",
            CharacterClass::Mage => "Маг",
            CharacterClass::Rogue => "Вор",
            CharacterClass::Cleric => "Жрец",
        }
    }

    pub fn description(&self) -> &str {
        match self {
            CharacterClass::Warrior => "Мастер ближнего боя, высокая сила и выносливость",
            CharacterClass::Mage => "Повелитель магии, высокий интеллект",
            CharacterClass::Rogue => "Ловкий и хитрый, мастер скрытности",
            CharacterClass::Cleric => "Целитель и защитник, высокая мудрость",
        }
    }

    /// Базовые бонусы к характеристикам при создании
    pub fn stat_bonuses(&self) -> StatBonuses {
        match self {
            CharacterClass::Warrior => StatBonuses {
                strength: 3,
                intelligence: -1,
                dexterity: 1,
                charisma: 0,
                luck: 0,
            },
            CharacterClass::Mage => StatBonuses {
                strength: -1,
                intelligence: 3,
                dexterity: 0,
                charisma: 1,
                luck: 0,
            },
            CharacterClass::Rogue => StatBonuses {
                strength: 0,
                intelligence: 1,
                dexterity: 3,
                charisma: 0,
                luck: 1,
            },
            CharacterClass::Cleric => StatBonuses {
                strength: 1,
                intelligence: 1,
                dexterity: 0,
                charisma: 2,
                luck: 0,
            },
        }
    }
}

/// Раса персонажа
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum CharacterRace {
    Human, // Человек
    Elf,   // Эльф
    Dwarf, // Дворф
    Orc,   // Орк
}

impl CharacterRace {
    pub fn name_ru(&self) -> &str {
        match self {
            CharacterRace::Human => "Человек",
            CharacterRace::Elf => "Эльф",
            CharacterRace::Dwarf => "Дворф",
            CharacterRace::Orc => "Орк",
        }
    }

    pub fn description(&self) -> &str {
        match self {
            CharacterRace::Human => "Универсальные и адаптивные, бонус к харизме",
            CharacterRace::Elf => "Грациозные и умные, бонус к ловкости и интеллекту",
            CharacterRace::Dwarf => "Крепкие и выносливые, бонус к силе",
            CharacterRace::Orc => "Сильные и свирепые, большой бонус к силе",
        }
    }

    pub fn stat_bonuses(&self) -> StatBonuses {
        match self {
            CharacterRace::Human => StatBonuses {
                strength: 1,
                intelligence: 1,
                dexterity: 1,
                charisma: 1,
                luck: 1,
            },
            CharacterRace::Elf => StatBonuses {
                strength: 0,
                intelligence: 2,
                dexterity: 2,
                charisma: 1,
                luck: 0,
            },
            CharacterRace::Dwarf => StatBonuses {
                strength: 2,
                intelligence: 0,
                dexterity: -1,
                charisma: 0,
                luck: 1,
            },
            CharacterRace::Orc => StatBonuses {
                strength: 3,
                intelligence: -1,
                dexterity: 1,
                charisma: -1,
                luck: 0,
            },
        }
    }
}

/// Бонусы к характеристикам
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StatBonuses {
    pub strength: i8,
    pub intelligence: i8,
    pub dexterity: i8,
    pub charisma: i8,
    pub luck: i8,
}

/// Характеристики персонажа
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CharacterStats {
    pub strength: u8,     // Сила
    pub intelligence: u8, // Интеллект
    pub dexterity: u8,    // Ловкость
    pub charisma: u8,     // Харизма
    pub luck: u8,         // Удача
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

impl CharacterStats {
    /// Создать характеристики с бонусами от класса и расы
    pub fn new(class: &CharacterClass, race: &CharacterRace) -> Self {
        let base = Self::default();
        let class_bonuses = class.stat_bonuses();
        let race_bonuses = race.stat_bonuses();

        Self {
            strength: Self::apply_bonus(
                base.strength,
                class_bonuses.strength + race_bonuses.strength,
            ),
            intelligence: Self::apply_bonus(
                base.intelligence,
                class_bonuses.intelligence + race_bonuses.intelligence,
            ),
            dexterity: Self::apply_bonus(
                base.dexterity,
                class_bonuses.dexterity + race_bonuses.dexterity,
            ),
            charisma: Self::apply_bonus(
                base.charisma,
                class_bonuses.charisma + race_bonuses.charisma,
            ),
            luck: Self::apply_bonus(base.luck, class_bonuses.luck + race_bonuses.luck),
        }
    }

    fn apply_bonus(base: u8, bonus: i8) -> u8 {
        let result = base as i16 + bonus as i16;
        result.max(1).min(20) as u8
    }

    /// Получить модификатор для характеристики (D&D формула)
    pub fn get_modifier(&self, stat_name: &str) -> i8 {
        let stat = match stat_name {
            "strength" => self.strength,
            "intelligence" => self.intelligence,
            "dexterity" => self.dexterity,
            "charisma" => self.charisma,
            "luck" => self.luck,
            _ => 10,
        };

        ((stat as i16 - 10) / 2) as i8
    }

    /// Повысить характеристику при level up
    pub fn increase_stat(&mut self, stat_name: &str, amount: u8) {
        let stat = match stat_name {
            "strength" => &mut self.strength,
            "intelligence" => &mut self.intelligence,
            "dexterity" => &mut self.dexterity,
            "charisma" => &mut self.charisma,
            "luck" => &mut self.luck,
            _ => return,
        };

        *stat = (*stat + amount).min(20); // Максимум 20
    }
}

/// Полная информация о персонаже
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Character {
    pub user_id: i32,
    pub class: CharacterClass,
    pub race: CharacterRace,
    pub stats: CharacterStats,
    pub level: u8,
    pub experience: u32,
    pub gold: u32,
}

/// Запрос на создание персонажа
#[derive(Debug, Deserialize)]
pub struct CreateCharacterRequest {
    pub class: CharacterClass,
    pub race: CharacterRace,
    pub name: Option<String>,
}

/// Запрос на повышение характеристики
#[derive(Debug, Deserialize)]
pub struct IncreaseStatRequest {
    pub stat_name: String, // "strength", "intelligence", etc.
    pub amount: Option<u8>,
}

/// Влияние характеристик на квесты
#[derive(Debug, Clone, Serialize)]
pub struct StatInfluence {
    pub stat_name: String,
    pub current_value: u8,
    pub modifier: i8,
    pub quest_type_bonuses: Vec<QuestTypeBonus>,
    pub success_chance_bonus: f32,
    pub reward_bonus: f32,
    pub time_reduction: f32, // В процентах
}

#[derive(Debug, Clone, Serialize)]
pub struct QuestTypeBonus {
    pub quest_type: String,
    pub bonus_description: String,
}

impl Character {
    /// Получить влияние характеристик на квесты
    pub fn get_stat_influence(&self) -> Vec<StatInfluence> {
        let mut influences = Vec::new();

        // Сила
        influences.push(StatInfluence {
            stat_name: "strength".to_string(),
            current_value: self.stats.strength,
            modifier: self.stats.get_modifier("strength"),
            quest_type_bonuses: vec![QuestTypeBonus {
                quest_type: "физические".to_string(),
                bonus_description: "+15% к наградам".to_string(),
            }],
            success_chance_bonus: self.stats.strength as f32 * 0.01,
            reward_bonus: self.stats.strength as f32 * 0.015,
            time_reduction: self.stats.strength as f32 * 0.01,
        });

        // Интеллект
        influences.push(StatInfluence {
            stat_name: "intelligence".to_string(),
            current_value: self.stats.intelligence,
            modifier: self.stats.get_modifier("intelligence"),
            quest_type_bonuses: vec![QuestTypeBonus {
                quest_type: "умственные".to_string(),
                bonus_description: "+20% к XP".to_string(),
            }],
            success_chance_bonus: self.stats.intelligence as f32 * 0.015,
            reward_bonus: self.stats.intelligence as f32 * 0.02,
            time_reduction: self.stats.intelligence as f32 * 0.015,
        });

        // Ловкость
        influences.push(StatInfluence {
            stat_name: "dexterity".to_string(),
            current_value: self.stats.dexterity,
            modifier: self.stats.get_modifier("dexterity"),
            quest_type_bonuses: vec![QuestTypeBonus {
                quest_type: "быстрые".to_string(),
                bonus_description: "Уменьшение времени на 15%".to_string(),
            }],
            success_chance_bonus: self.stats.dexterity as f32 * 0.01,
            reward_bonus: self.stats.dexterity as f32 * 0.01,
            time_reduction: self.stats.dexterity as f32 * 0.02,
        });

        // Харизма
        influences.push(StatInfluence {
            stat_name: "charisma".to_string(),
            current_value: self.stats.charisma,
            modifier: self.stats.get_modifier("charisma"),
            quest_type_bonuses: vec![QuestTypeBonus {
                quest_type: "социальные".to_string(),
                bonus_description: "+25% к наградам".to_string(),
            }],
            success_chance_bonus: self.stats.charisma as f32 * 0.012,
            reward_bonus: self.stats.charisma as f32 * 0.025,
            time_reduction: self.stats.charisma as f32 * 0.005,
        });

        influences
    }

    /// Проверить можно ли повысить уровень
    pub fn can_level_up(&self) -> bool {
        let required_exp = self.level as u32 * 100;
        self.experience >= required_exp
    }

    /// Повысить уровень
    pub fn level_up(&mut self) -> LevelUpResult {
        if !self.can_level_up() {
            return LevelUpResult {
                success: false,
                new_level: self.level,
                stat_points_gained: 0,
                unlocked_features: vec![],
            };
        }

        let required_exp = self.level as u32 * 100;
        self.experience -= required_exp;
        self.level += 1;

        // Получить очки характеристик каждые 5 уровней
        let stat_points = if self.level % 5 == 0 { 2 } else { 1 };

        // Разблокировать новые возможности
        let mut unlocked_features = vec![];
        match self.level {
            5 => unlocked_features.push("Доступ к эпическим квестам".to_string()),
            10 => unlocked_features.push("Доступ к мультиплееру".to_string()),
            15 => unlocked_features.push("Доступ к гильдиям".to_string()),
            20 => unlocked_features.push("Доступ к легендарным квестам".to_string()),
            _ => {}
        }

        LevelUpResult {
            success: true,
            new_level: self.level,
            stat_points_gained: stat_points,
            unlocked_features,
        }
    }
}

/// Результат повышения уровня
#[derive(Debug, Clone, Serialize)]
pub struct LevelUpResult {
    pub success: bool,
    pub new_level: u8,
    pub stat_points_gained: u8,
    pub unlocked_features: Vec<String>,
}

/// Запрос на выбор класса и расы
#[derive(Debug, Deserialize)]
pub struct SelectClassRaceRequest {
    pub class: CharacterClass,
    pub race: CharacterRace,
}

/// Информация о доступных классах
#[derive(Debug, Serialize)]
pub struct ClassInfo {
    pub class: CharacterClass,
    pub name_ru: String,
    pub description: String,
    pub stat_bonuses: StatBonuses,
    pub recommended_for: Vec<String>,
}

/// Информация о доступных расах
#[derive(Debug, Serialize)]
pub struct RaceInfo {
    pub race: CharacterRace,
    pub name_ru: String,
    pub description: String,
    pub stat_bonuses: StatBonuses,
}

/// Полная информация о персонаже для UI
#[derive(Debug, Serialize)]
pub struct CharacterProfile {
    pub character: Character,
    pub stat_influences: Vec<StatInfluence>,
    pub can_level_up: bool,
    pub experience_to_next_level: u32,
    pub available_stat_points: u8,
}

impl CharacterProfile {
    pub fn from_character(character: Character, available_stat_points: u8) -> Self {
        let stat_influences = character.get_stat_influence();
        let can_level_up = character.can_level_up();
        let required_exp = character.level as u32 * 100;
        let experience_to_next_level = required_exp.saturating_sub(character.experience);

        Self {
            character,
            stat_influences,
            can_level_up,
            experience_to_next_level,
            available_stat_points,
        }
    }
}
