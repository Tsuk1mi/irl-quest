use rand::Rng;
/// Система кубиков (D&D dice system)
use serde::{Deserialize, Serialize};

/// Тип кубика
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum DiceType {
    D4,  // Тетраэдр
    D6,  // Стандартный куб
    D8,  // Октаэдр
    D10, // Десятигранник
    D12, // Додекаэдр
    D20, // Икосаэдр (классический D&D)
}

impl DiceType {
    pub fn sides(&self) -> u8 {
        match self {
            DiceType::D4 => 4,
            DiceType::D6 => 6,
            DiceType::D8 => 8,
            DiceType::D10 => 10,
            DiceType::D12 => 12,
            DiceType::D20 => 20,
        }
    }

    pub fn emoji(&self) -> &str {
        match self {
            DiceType::D4 => "D4",
            DiceType::D6 => "D6",
            DiceType::D8 => "D8",
            DiceType::D10 => "D10",
            DiceType::D12 => "D12",
            DiceType::D20 => "D20",
        }
    }
}

/// Результат броска кубика
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiceRoll {
    pub dice_type: DiceType,
    pub result: u8,
    pub modifier: i8,
    pub total: i16,
    pub is_critical_success: bool, // Максимальное значение
    pub is_critical_failure: bool, // Минимальное значение
    pub timestamp: i64,
}

impl DiceRoll {
    /// Бросить кубик
    pub fn roll(dice_type: DiceType, modifier: i8) -> Self {
        let mut rng = rand::thread_rng();
        let result = rng.gen_range(1..=dice_type.sides());
        let total = result as i16 + modifier as i16;

        let is_critical_success = result == dice_type.sides();
        let is_critical_failure = result == 1;

        Self {
            dice_type,
            result,
            modifier,
            total,
            is_critical_success,
            is_critical_failure,
            timestamp: chrono::Utc::now().timestamp(),
        }
    }

    /// Проверить успех против сложности
    pub fn check_success(&self, difficulty: u8) -> bool {
        self.total >= difficulty as i16
    }
}

/// Запрос на бросок кубика
#[derive(Debug, Deserialize)]
pub struct RollDiceRequest {
    pub dice_type: DiceType,
    pub modifier: Option<i8>,
    pub quest_id: Option<i32>,
    pub action_description: Option<String>,
}

/// Запрос на проверку навыка (skill check)
#[derive(Debug, Deserialize)]
pub struct SkillCheckRequest {
    pub skill: SkillType,
    pub difficulty: u8, // DC (Difficulty Class) 1-30
    pub quest_id: Option<i32>,
}

/// Типы навыков (привязаны к характеристикам)
#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum SkillType {
    // На основе Силы
    Athletics, // Атлетика
    // На основе Ловкости
    Acrobatics,    // Акробатика
    SleightOfHand, // Ловкость рук
    Stealth,       // Скрытность
    // На основе Интеллекта
    Investigation, // Анализ
    History,       // История
    Arcana,        // Магия
    // На основе Харизмы
    Persuasion,  // Убеждение
    Deception,   // Обман
    Performance, // Выступление
}

impl SkillType {
    /// Получить бонус от характеристики
    pub fn get_modifier(&self, strength: u8, intelligence: u8, dexterity: u8, charisma: u8) -> i8 {
        let stat = match self {
            SkillType::Athletics => strength,
            SkillType::Acrobatics | SkillType::SleightOfHand | SkillType::Stealth => dexterity,
            SkillType::Investigation | SkillType::History | SkillType::Arcana => intelligence,
            SkillType::Persuasion | SkillType::Deception | SkillType::Performance => charisma,
        };

        // D&D формула: (Stat - 10) / 2
        ((stat as i16 - 10) / 2) as i8
    }

    pub fn description(&self) -> &str {
        match self {
            SkillType::Athletics => "Физическая сила и выносливость",
            SkillType::Acrobatics => "Ловкость и равновесие",
            SkillType::SleightOfHand => "Тонкая моторика",
            SkillType::Stealth => "Незаметность",
            SkillType::Investigation => "Логика и анализ",
            SkillType::History => "Знание истории",
            SkillType::Arcana => "Знание магии",
            SkillType::Persuasion => "Убеждение",
            SkillType::Deception => "Обман",
            SkillType::Performance => "Выступление",
        }
    }
}

/// Результат проверки навыка
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SkillCheckResult {
    pub skill: SkillType,
    pub roll: DiceRoll,
    pub difficulty: u8,
    pub success: bool,
    pub degree_of_success: SuccessDegree,
    pub description: String,
}

/// Степень успеха
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum SuccessDegree {
    CriticalFailure, // Критический провал (1 на кубике)
    Failure,         // Провал
    Success,         // Успех
    CriticalSuccess, // Критический успех (макс на кубике или превышение на 10+)
}

impl SkillCheckResult {
    pub fn new(skill: SkillType, roll: DiceRoll, difficulty: u8) -> Self {
        let success = roll.check_success(difficulty);
        let margin = roll.total - difficulty as i16;

        let degree_of_success = if roll.is_critical_failure {
            SuccessDegree::CriticalFailure
        } else if roll.is_critical_success || margin >= 10 {
            SuccessDegree::CriticalSuccess
        } else if success {
            SuccessDegree::Success
        } else {
            SuccessDegree::Failure
        };

        let description = match degree_of_success {
            SuccessDegree::CriticalFailure => {
                "Критический провал! Что-то пошло очень не так...".to_string()
            }
            SuccessDegree::Failure => {
                format!("Провал. Не хватило {} очков.", margin.abs())
            }
            SuccessDegree::Success => {
                format!("Успех! Превышение на {} очков.", margin)
            }
            SuccessDegree::CriticalSuccess => {
                "Критический успех! Невероятное достижение!".to_string()
            }
        };

        Self {
            skill,
            roll,
            difficulty,
            success,
            degree_of_success,
            description,
        }
    }
}

/// Множественный бросок (например, 3d6 - три кубика d6)
#[derive(Debug, Deserialize)]
pub struct MultiRollRequest {
    pub dice_type: DiceType,
    pub count: u8, // Количество кубиков (1-10)
    pub modifier: Option<i8>,
    pub keep_highest: Option<u8>, // Оставить N лучших результатов
    pub keep_lowest: Option<u8>,  // Оставить N худших результатов
}

/// Результат множественного броска
#[derive(Debug, Serialize)]
pub struct MultiRollResult {
    pub dice_type: DiceType,
    pub rolls: Vec<u8>,
    pub kept_rolls: Vec<u8>,
    pub modifier: i8,
    pub total: i16,
    pub average: f32,
}

impl MultiRollResult {
    pub fn roll(request: &MultiRollRequest) -> Self {
        let mut rng = rand::thread_rng();
        let count = request.count.min(10); // Максимум 10 кубиков

        let mut rolls: Vec<u8> = (0..count)
            .map(|_| rng.gen_range(1..=request.dice_type.sides()))
            .collect();

        // Применить keep_highest или keep_lowest
        let kept_rolls = if let Some(keep_high) = request.keep_highest {
            rolls.sort_by(|a, b| b.cmp(a)); // Сортировка по убыванию
            rolls.iter().take(keep_high as usize).copied().collect()
        } else if let Some(keep_low) = request.keep_lowest {
            rolls.sort(); // Сортировка по возрастанию
            rolls.iter().take(keep_low as usize).copied().collect()
        } else {
            rolls.clone()
        };

        let modifier = request.modifier.unwrap_or(0);
        let sum: u16 = kept_rolls.iter().map(|&r| r as u16).sum();
        let total = sum as i16 + modifier as i16;
        let average = kept_rolls.iter().map(|&r| r as f32).sum::<f32>() / kept_rolls.len() as f32;

        Self {
            dice_type: request.dice_type,
            rolls,
            kept_rolls,
            modifier,
            total,
            average,
        }
    }
}

/// История бросков пользователя
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiceHistory {
    pub user_id: i32,
    pub rolls: Vec<DiceRoll>,
    pub total_rolls: u32,
    pub critical_successes: u32,
    pub critical_failures: u32,
}

impl DiceHistory {
    pub fn new(user_id: i32) -> Self {
        Self {
            user_id,
            rolls: Vec::new(),
            total_rolls: 0,
            critical_successes: 0,
            critical_failures: 0,
        }
    }

    pub fn add_roll(&mut self, roll: DiceRoll) {
        self.total_rolls += 1;
        if roll.is_critical_success {
            self.critical_successes += 1;
        }
        if roll.is_critical_failure {
            self.critical_failures += 1;
        }

        // Хранить последние 100 бросков
        self.rolls.push(roll);
        if self.rolls.len() > 100 {
            self.rolls.remove(0);
        }
    }

    pub fn average_roll(&self, dice_type: DiceType) -> f32 {
        let rolls: Vec<&DiceRoll> = self
            .rolls
            .iter()
            .filter(|r| r.dice_type == dice_type)
            .collect();

        if rolls.is_empty() {
            return 0.0;
        }

        let sum: u16 = rolls.iter().map(|r| r.result as u16).sum();
        sum as f32 / rolls.len() as f32
    }
}
