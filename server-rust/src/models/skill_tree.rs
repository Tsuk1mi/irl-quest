/// Система дерева навыков (Skill Tree)
use serde::{Deserialize, Serialize};

/// Навык в дереве
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Skill {
    pub id: i32,
    pub code: String,  // Уникальный код
    pub name: String,
    pub description: String,
    pub icon: String,
    pub tier: u8,  // Уровень в дереве (1-5)
    pub max_level: u8,
    pub cost_per_level: u8,  // Стоимость в skill points
    pub prerequisites: Vec<String>,  // Коды требуемых навыков
    pub effects: SkillEffects,
}

/// Эффекты навыка
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SkillEffects {
    pub stat_bonuses: Option<StatBonus>,
    pub reward_multiplier: Option<f32>,
    pub unlock_feature: Option<String>,
    pub passive_bonus: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StatBonus {
    pub strength: i8,
    pub intelligence: i8,
    pub dexterity: i8,
    pub charisma: i8,
}

/// Навык пользователя
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserSkill {
    pub user_id: i32,
    pub skill_id: i32,
    pub current_level: u8,
    pub is_unlocked: bool,
}

/// Информация о дереве навыков
#[derive(Debug, Serialize)]
pub struct SkillTreeInfo {
    pub available_points: u8,
    pub total_skills: usize,
    pub unlocked_skills: usize,
    pub skills: Vec<SkillWithProgress>,
}

#[derive(Debug, Serialize)]
pub struct SkillWithProgress {
    pub skill: Skill,
    pub current_level: u8,
    pub is_unlocked: bool,
    pub can_unlock: bool,  // Выполнены ли prerequisites
}

