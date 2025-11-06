/// Handlers для системы кубиков (D&D dice)
use axum::{
    extract::{Extension, State},
    Json,
};
use sqlx::Row;
use crate::error::AppError;
use crate::models::dice::*;
use crate::middleware::auth::CurrentUser;
use crate::state::AppState;

/// POST /api/dice/roll - Бросить кубик
pub async fn roll_dice(
    State(_state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<RollDiceRequest>,
) -> Result<Json<DiceRoll>, AppError> {
    let modifier = request.modifier.unwrap_or(0);
    let roll = DiceRoll::roll(request.dice_type, modifier);

    if let Some(user) = current_user {
        tracing::info!(
            "User {} rolled {} {}: {} (modifier: {:+}, total: {})",
            user.0.id,
            request.dice_type.emoji(),
            format!("{:?}", request.dice_type),
            roll.result,
            roll.modifier,
            roll.total
        );
    }

    Ok(Json(roll))
}

/// POST /api/dice/roll/multi - Множественный бросок
pub async fn roll_multi_dice(
    State(_state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<MultiRollRequest>,
) -> Result<Json<MultiRollResult>, AppError> {
    let result = MultiRollResult::roll(&request);

    tracing::info!(
        "Multi-roll: {}x{:?}, total: {}",
        request.count,
        request.dice_type,
        result.total
    );

    Ok(Json(result))
}

/// POST /api/dice/skill-check - Проверка навыка
pub async fn skill_check(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<SkillCheckRequest>,
) -> Result<Json<SkillCheckResult>, AppError> {
    let user = current_user
        .ok_or(AppError::Unauthorized("Authentication required".to_string()))?;

    // Получить характеристики пользователя из БД
    let user_data = sqlx::query(
        r#"
        SELECT strength, intelligence, dexterity, charisma
        FROM users
        WHERE id = $1
        "#
    )
    .bind(user.0.id)
    .fetch_one(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user stats: {}", e)))?;

    let strength = user_data.try_get::<i32, _>("strength").unwrap_or(10) as u8;
    let intelligence = user_data.try_get::<i32, _>("intelligence").unwrap_or(10) as u8;
    let dexterity = user_data.try_get::<i32, _>("dexterity").unwrap_or(10) as u8;
    let charisma = user_data.try_get::<i32, _>("charisma").unwrap_or(10) as u8;

    // Рассчитать модификатор на основе навыка и характеристик
    let modifier = request.skill.get_modifier(strength, intelligence, dexterity, charisma);

    // Бросить d20 (стандарт для skill checks в D&D)
    let roll = DiceRoll::roll(DiceType::D20, modifier);

    // Создать результат проверки
    let result = SkillCheckResult::new(request.skill, roll, request.difficulty);

    tracing::info!(
        "User {} skill check {:?}: {} vs DC {} = {}",
        user.0.id,
        request.skill,
        result.roll.total,
        request.difficulty,
        if result.success { "SUCCESS" } else { "FAILURE" }
    );

    Ok(Json(result))
}

/// GET /api/dice/types - Получить список доступных кубиков
pub async fn get_dice_types() -> Result<Json<Vec<DiceInfo>>, AppError> {
    let dice_types = vec![
        DiceInfo {
            dice_type: DiceType::D4,
            sides: 4,
            emoji: "🔺".to_string(),
            name: "D4 - Тетраэдр".to_string(),
            description: "Используется для малых повреждений и простых проверок".to_string(),
        },
        DiceInfo {
            dice_type: DiceType::D6,
            sides: 6,
            emoji: "🎲".to_string(),
            name: "D6 - Куб".to_string(),
            description: "Стандартный игральный кубик".to_string(),
        },
        DiceInfo {
            dice_type: DiceType::D8,
            sides: 8,
            emoji: "🔷".to_string(),
            name: "D8 - Октаэдр".to_string(),
            description: "Средние повреждения оружия".to_string(),
        },
        DiceInfo {
            dice_type: DiceType::D10,
            sides: 10,
            emoji: "🔟".to_string(),
            name: "D10 - Десятигранник".to_string(),
            description: "Процентные проверки и большие повреждения".to_string(),
        },
        DiceInfo {
            dice_type: DiceType::D12,
            sides: 12,
            emoji: "⬡".to_string(),
            name: "D12 - Додекаэдр".to_string(),
            description: "Максимальные повреждения оружия".to_string(),
        },
        DiceInfo {
            dice_type: DiceType::D20,
            sides: 20,
            emoji: "🎯".to_string(),
            name: "D20 - Икосаэдр".to_string(),
            description: "Классический D&D кубик для проверок навыков и атак".to_string(),
        },
    ];

    Ok(Json(dice_types))
}

#[derive(serde::Serialize)]
pub struct DiceInfo {
    dice_type: DiceType,
    sides: u8,
    emoji: String,
    name: String,
    description: String,
}

/// GET /api/dice/skills - Получить список навыков
pub async fn get_skills() -> Result<Json<Vec<SkillInfo>>, AppError> {
    let skills = vec![
        SkillInfo {
            skill: SkillType::Athletics,
            name: "Атлетика".to_string(),
            description: SkillType::Athletics.description().to_string(),
            stat: "Сила".to_string(),
        },
        SkillInfo {
            skill: SkillType::Acrobatics,
            name: "Акробатика".to_string(),
            description: SkillType::Acrobatics.description().to_string(),
            stat: "Ловкость".to_string(),
        },
        SkillInfo {
            skill: SkillType::SleightOfHand,
            name: "Ловкость рук".to_string(),
            description: SkillType::SleightOfHand.description().to_string(),
            stat: "Ловкость".to_string(),
        },
        SkillInfo {
            skill: SkillType::Stealth,
            name: "Скрытность".to_string(),
            description: SkillType::Stealth.description().to_string(),
            stat: "Ловкость".to_string(),
        },
        SkillInfo {
            skill: SkillType::Investigation,
            name: "Анализ".to_string(),
            description: SkillType::Investigation.description().to_string(),
            stat: "Интеллект".to_string(),
        },
        SkillInfo {
            skill: SkillType::History,
            name: "История".to_string(),
            description: SkillType::History.description().to_string(),
            stat: "Интеллект".to_string(),
        },
        SkillInfo {
            skill: SkillType::Arcana,
            name: "Магия".to_string(),
            description: SkillType::Arcana.description().to_string(),
            stat: "Интеллект".to_string(),
        },
        SkillInfo {
            skill: SkillType::Persuasion,
            name: "Убеждение".to_string(),
            description: SkillType::Persuasion.description().to_string(),
            stat: "Харизма".to_string(),
        },
        SkillInfo {
            skill: SkillType::Deception,
            name: "Обман".to_string(),
            description: SkillType::Deception.description().to_string(),
            stat: "Харизма".to_string(),
        },
        SkillInfo {
            skill: SkillType::Performance,
            name: "Выступление".to_string(),
            description: SkillType::Performance.description().to_string(),
            stat: "Харизма".to_string(),
        },
    ];

    Ok(Json(skills))
}

#[derive(serde::Serialize)]
pub struct SkillInfo {
    skill: SkillType,
    name: String,
    description: String,
    stat: String,
}

