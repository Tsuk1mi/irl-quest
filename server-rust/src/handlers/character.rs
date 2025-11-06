/// Handlers для системы персонажей (классы, расы, характеристики)
use axum::{
    extract::{Extension, State},
    http::StatusCode,
    Json,
};
use sqlx::Row;
use crate::error::AppError;
use crate::models::character::*;
use crate::middleware::auth::CurrentUser;
use crate::state::AppState;

/// GET /api/character/profile - Получить профиль персонажа
pub async fn get_character_profile(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<CharacterProfile>, AppError> {
    let user = current_user
        .ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    // Получить данные пользователя
    let user_data = sqlx::query(
        r#"
        SELECT level, experience, gold, character_class, character_race as race,
               strength, intelligence, dexterity, charisma, wisdom as luck
        FROM users
        WHERE id = $1
        "#
    )
    .bind(user.0.id)
    .fetch_one(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user: {}", e)))?;

    // Получить доступные очки характеристик
    let stat_points = sqlx::query(
        "SELECT available_points FROM user_stat_points WHERE user_id = $1"
    )
    .bind(user.0.id)
    .fetch_optional(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch stat points: {}", e)))?
    .and_then(|r| r.try_get::<i32, _>("available_points").ok())
    .unwrap_or(0);

    // Создать Character объект
    let character = Character {
        user_id: user.0.id,
        class: match user_data.try_get::<String, _>("character_class").ok().as_deref() {
            Some("warrior") => CharacterClass::Warrior,
            Some("mage") => CharacterClass::Mage,
            Some("rogue") => CharacterClass::Rogue,
            Some("cleric") => CharacterClass::Cleric,
            _ => CharacterClass::Warrior,
        },
        race: match user_data.try_get::<String, _>("race").ok().as_deref() {
            Some("human") => CharacterRace::Human,
            Some("elf") => CharacterRace::Elf,
            Some("dwarf") => CharacterRace::Dwarf,
            Some("orc") => CharacterRace::Orc,
            _ => CharacterRace::Human,
        },
        stats: CharacterStats {
            strength: user_data.try_get::<i32, _>("strength").unwrap_or(10) as u8,
            intelligence: user_data.try_get::<i32, _>("intelligence").unwrap_or(10) as u8,
            dexterity: user_data.try_get::<i32, _>("dexterity").unwrap_or(10) as u8,
            charisma: user_data.try_get::<i32, _>("charisma").unwrap_or(10) as u8,
            luck: user_data.try_get::<i32, _>("luck").unwrap_or(10) as u8,
        },
        level: user_data.try_get::<i32, _>("level").unwrap_or(1) as u8,
        experience: user_data.try_get::<i32, _>("experience").unwrap_or(0) as u32,
        gold: user_data.try_get::<i32, _>("gold").unwrap_or(0) as u32,
    };

    let profile = CharacterProfile::from_character(character, stat_points as u8);

    Ok(Json(profile))
}

/// POST /api/character/select - Выбрать класс и расу
pub async fn select_class_race(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<SelectClassRaceRequest>,
) -> Result<StatusCode, AppError> {
    let user = current_user
        .ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    // Рассчитать начальные характеристики
    let stats = CharacterStats::new(&request.class, &request.race);

    // Обновить пользователя
    sqlx::query(
        r#"
        UPDATE users
        SET character_class = $1, character_race = $2,
            strength = $3, intelligence = $4, dexterity = $5, charisma = $6, wisdom = $7
        WHERE id = $8
        "#
    )
    .bind(format!("{:?}", request.class).to_lowercase())
    .bind(format!("{:?}", request.race).to_lowercase())
    .bind(stats.strength as i32)
    .bind(stats.intelligence as i32)
    .bind(stats.dexterity as i32)
    .bind(stats.charisma as i32)
    .bind(stats.luck as i32)
    .bind(user.0.id)
    .execute(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to update character: {}", e)))?;

    tracing::info!(
        "User {} selected class {:?} and race {:?}",
        user.0.id,
        request.class,
        request.race
    );

    Ok(StatusCode::OK)
}

/// POST /api/character/increase-stat - Повысить характеристику
pub async fn increase_stat(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<IncreaseStatRequest>,
) -> Result<StatusCode, AppError> {
    let user = current_user
        .ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    // Проверить доступные очки
    let stat_points = sqlx::query(
        "SELECT available_points FROM user_stat_points WHERE user_id = $1"
    )
    .bind(user.0.id)
    .fetch_optional(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch stat points: {}", e)))?;

    let available = stat_points.and_then(|r| r.try_get::<i32, _>("available_points").ok()).unwrap_or(0);
    let amount = request.amount.unwrap_or(1);

    if available < amount as i32 {
        return Err(AppError::BadRequest("Not enough stat points".to_string()));
    }

    // Получить текущее значение характеристики
    let current_value = sqlx::query(&format!(
        "SELECT {} FROM users WHERE id = ?",
        request.stat_name
    ))
    .bind(user.0.id)
    .fetch_one(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch stat: {}", e)))?;

    // TODO: Извлечь значение и обновить

    // Записать в историю
    // Обновить доступные очки

    tracing::info!("User {} increased {} by {}", user.0.id, request.stat_name, amount);

    Ok(StatusCode::OK)
}

/// GET /api/character/classes - Получить список доступных классов
pub async fn get_available_classes() -> Result<Json<Vec<ClassInfo>>, AppError> {
    let classes = vec![
        ClassInfo {
            class: CharacterClass::Warrior,
            name_ru: CharacterClass::Warrior.name_ru().to_string(),
            description: CharacterClass::Warrior.description().to_string(),
            stat_bonuses: CharacterClass::Warrior.stat_bonuses(),
            recommended_for: vec!["Новички".to_string(), "Физические квесты".to_string()],
        },
        ClassInfo {
            class: CharacterClass::Mage,
            name_ru: CharacterClass::Mage.name_ru().to_string(),
            description: CharacterClass::Mage.description().to_string(),
            stat_bonuses: CharacterClass::Mage.stat_bonuses(),
            recommended_for: vec!["Умственные задачи".to_string(), "Учеба".to_string()],
        },
        ClassInfo {
            class: CharacterClass::Rogue,
            name_ru: CharacterClass::Rogue.name_ru().to_string(),
            description: CharacterClass::Rogue.description().to_string(),
            stat_bonuses: CharacterClass::Rogue.stat_bonuses(),
            recommended_for: vec!["Быстрые задачи".to_string(), "Скрытность".to_string()],
        },
        ClassInfo {
            class: CharacterClass::Cleric,
            name_ru: CharacterClass::Cleric.name_ru().to_string(),
            description: CharacterClass::Cleric.description().to_string(),
            stat_bonuses: CharacterClass::Cleric.stat_bonuses(),
            recommended_for: vec!["Социальные квесты".to_string(), "Поддержка".to_string()],
        },
    ];

    Ok(Json(classes))
}

/// GET /api/character/races - Получить список доступных рас
pub async fn get_available_races() -> Result<Json<Vec<RaceInfo>>, AppError> {
    let races = vec![
        RaceInfo {
            race: CharacterRace::Human,
            name_ru: CharacterRace::Human.name_ru().to_string(),
            description: CharacterRace::Human.description().to_string(),
            stat_bonuses: CharacterRace::Human.stat_bonuses(),
        },
        RaceInfo {
            race: CharacterRace::Elf,
            name_ru: CharacterRace::Elf.name_ru().to_string(),
            description: CharacterRace::Elf.description().to_string(),
            stat_bonuses: CharacterRace::Elf.stat_bonuses(),
        },
        RaceInfo {
            race: CharacterRace::Dwarf,
            name_ru: CharacterRace::Dwarf.name_ru().to_string(),
            description: CharacterRace::Dwarf.description().to_string(),
            stat_bonuses: CharacterRace::Dwarf.stat_bonuses(),
        },
        RaceInfo {
            race: CharacterRace::Orc,
            name_ru: CharacterRace::Orc.name_ru().to_string(),
            description: CharacterRace::Orc.description().to_string(),
            stat_bonuses: CharacterRace::Orc.stat_bonuses(),
        },
    ];

    Ok(Json(races))
}

/// POST /api/character/level-up - Повысить уровень
pub async fn level_up(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<LevelUpResult>, AppError> {
    let user = current_user
        .ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    // Получить данные пользователя
    let user_data = sqlx::query(
        "SELECT level, experience FROM users WHERE id = $1"
    )
    .bind(user.0.id)
    .fetch_one(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch user: {}", e)))?;

    let level = user_data.try_get::<i32, _>("level").unwrap_or(1) as u8;
    let experience = user_data.try_get::<i32, _>("experience").unwrap_or(0) as u32;
    let required_exp = level as u32 * 100;

    if experience < required_exp {
        return Err(AppError::BadRequest("Not enough experience to level up".to_string()));
    }

    // Повысить уровень
    let new_level = level + 1;
    let new_experience = experience - required_exp;
    let stat_points_gained = if new_level % 5 == 0 { 2 } else { 1 };

    sqlx::query(
        "UPDATE users SET level = $1, experience = $2 WHERE id = $3"
    )
    .bind(new_level as i32)
    .bind(new_experience as i32)
    .bind(user.0.id)
    .execute(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to level up: {}", e)))?;

    // Добавить очки характеристик
    sqlx::query(
        r#"
        INSERT INTO user_stat_points (user_id, available_points, total_earned)
        VALUES ($1, $2, $3)
        ON CONFLICT(user_id) DO UPDATE SET
            available_points = available_points + $4,
            total_earned = total_earned + $5
        "#
    )
    .bind(user.0.id)
    .bind(stat_points_gained as i32)
    .bind(stat_points_gained as i32)
    .bind(stat_points_gained as i32)
    .bind(stat_points_gained as i32)
    .execute(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to add stat points: {}", e)))?;

    // Разблокировать новые возможности
    let mut unlocked_features = vec![];
    match new_level {
        5 => unlocked_features.push("Доступ к эпическим квестам".to_string()),
        10 => unlocked_features.push("Доступ к мультиплееру".to_string()),
        15 => unlocked_features.push("Доступ к гильдиям".to_string()),
        20 => unlocked_features.push("Доступ к легендарным квестам".to_string()),
        _ => {}
    }

    tracing::info!(
        "User {} leveled up to level {} (+{} stat points)",
        user.0.id,
        new_level,
        stat_points_gained
    );

    Ok(Json(LevelUpResult {
        success: true,
        new_level,
        stat_points_gained,
        unlocked_features,
    }))
}

