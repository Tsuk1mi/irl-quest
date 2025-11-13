use axum::{
    extract::{Extension, State},
    Json,
};
use rand::{seq::SliceRandom, Rng};
use serde::{Deserialize, Serialize};

use crate::{error::AppError, middleware::auth::CurrentUser, state::AppState};

#[derive(Debug, Deserialize)]
pub struct MLQuestGenerationRequest {
    #[serde(rename = "todo_text")]
    pub todo_text: String,
    pub context: Option<String>,
    #[serde(rename = "user_level")]
    pub user_level: Option<i32>,
    #[serde(rename = "tags_override")]
    pub tags_override: Option<Vec<String>>,
}

#[derive(Debug, Serialize)]
pub struct MLGeneratedTask {
    pub title: String,
    pub description: String,
    pub difficulty: i32,
    #[serde(rename = "experience_reward")]
    pub experience_reward: i32,
    #[serde(rename = "estimated_duration")]
    pub estimated_duration: Option<i32>,
    #[serde(rename = "is_boss")]
    pub is_boss: bool,
}

#[derive(Debug, Serialize)]
pub struct MLQuestGenerationResponse {
    pub title: String,
    pub description: String,
    pub difficulty: i32,
    #[serde(rename = "reward_experience")]
    pub reward_experience: i32,
    #[serde(rename = "reward_description")]
    pub reward_description: String,
    pub tags: Vec<String>,
    #[serde(rename = "quest_type")]
    pub quest_type: String,
    pub tasks: Vec<MLGeneratedTask>,
    #[serde(rename = "story_context")]
    pub story_context: Option<String>,
    #[serde(rename = "estimated_time")]
    pub estimated_time: Option<i32>,
}

fn estimate_difficulty(todo: &str, user_level: Option<i32>) -> i32 {
    let base = (todo.split_whitespace().count() as i32 / 3).clamp(1, 6);
    let level_bonus = user_level.unwrap_or(1) / 5;
    (base + level_bonus).clamp(1, 10)
}

fn generate_tasks(todo: &str, context: Option<&str>, difficulty: i32) -> Vec<MLGeneratedTask> {
    let mut rng = rand::thread_rng();
    let sentences: Vec<&str> = todo
        .split(|c: char| c == '.' || c == '!' || c == '?')
        .map(|s| s.trim())
        .filter(|s| !s.is_empty())
        .collect();

    let fallback = if sentences.is_empty() {
        vec![todo.trim()]
    } else {
        sentences
    };

    let mut tasks = Vec::new();
    for (idx, chunk) in fallback.iter().enumerate() {
        let title = match idx {
            0 => "Разведка цели",
            1 => "Подготовка отряда",
            2 => "Основная миссия",
            _ => "Дополнительная задача",
        };

        let extra_hint = context.unwrap_or("Используй сильные стороны героя");
        let duration = 20 + rng.gen_range(0..=20) + difficulty * 5;
        tasks.push(MLGeneratedTask {
            title: format!("{}: {}", title, chunk),
            description: format!(
                "{}. {}. {}.",
                chunk, extra_hint, "Запиши прогресс в дневник приключений"
            ),
            difficulty: (difficulty + idx as i32).clamp(1, 10),
            experience_reward: 50 + difficulty * 25,
            estimated_duration: Some(duration),
            is_boss: idx == fallback.len().saturating_sub(1),
        });
    }

    if tasks.len() > 3 {
        tasks.truncate(3);
    }

    tasks
}

fn build_reward_description(difficulty: i32) -> String {
    let loot_table = [
        "шанс найти редкий артефакт",
        "повышение репутации у гильдии",
        "ускоренный рост навыков",
        "секретная комната с сокровищами",
    ];
    let mut rng = rand::thread_rng();
    let bonus = loot_table.choose(&mut rng).unwrap_or(&loot_table[0]);
    format!(
        "Опыт героя: {} XP, добыча: {}, бонус к удаче: +{}%",
        100 + difficulty * 40,
        bonus,
        difficulty * 3
    )
}

fn choose_tags(request: &MLQuestGenerationRequest, difficulty: i32) -> Vec<String> {
    if let Some(tags) = &request.tags_override {
        if !tags.is_empty() {
            return tags.clone();
        }
    }
    let mut tags = vec!["daily".to_string(), "focus".to_string()];
    if difficulty >= 7 {
        tags.push("epic".to_string());
    } else if difficulty >= 4 {
        tags.push("team".to_string());
    } else {
        tags.push("solo".to_string());
    }
    tags
}

pub async fn ml_generate_quest(
    State(_state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<MLQuestGenerationRequest>,
) -> Result<Json<MLQuestGenerationResponse>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для генерации квестов ИИ".into())
    })?;

    let todo = req.todo_text.trim();
    if todo.is_empty() {
        return Err(AppError::Validation(
            "Опишите задание, чтобы ИИ смог построить квест".into(),
        ));
    }

    let difficulty = estimate_difficulty(todo, req.user_level.or(Some(user.0.level)));
    let tasks = generate_tasks(todo, req.context.as_deref(), difficulty);
    let reward_description = build_reward_description(difficulty);
    let tags = choose_tags(&req, difficulty);

    let story_context = req.context.as_ref().map(|ctx| {
        format!(
            "{}. Вдохновляйтесь и выполняйте шаги по очереди. Отметьте выполнение в приложении.",
            ctx.trim()
        )
    });

    let response = MLQuestGenerationResponse {
        title: format!("Квест: {}", todo.chars().take(40).collect::<String>()),
        description: format!("Герой {} берётся за задачу: {}", user.0.username, todo),
        difficulty,
        reward_experience: 150 + difficulty * 50,
        reward_description,
        tags,
        quest_type: "personal".to_string(),
        tasks,
        story_context,
        estimated_time: Some(difficulty * 35),
    };

    Ok(Json(response))
}
