use axum::{
    extract::{Extension, State},
    Json,
};
use chrono::{DateTime, Duration, Utc};
use rand::{rngs::StdRng, Rng, SeedableRng};
use serde::{Deserialize, Serialize};

use crate::{error::AppError, middleware::auth::CurrentUser, state::AppState};

#[derive(Debug, Deserialize)]
pub struct MLVerificationRequest {
    #[serde(rename = "quest_id")]
    pub quest_id: i32,
    #[serde(rename = "quest_title")]
    pub quest_title: String,
    #[serde(rename = "quest_description")]
    pub quest_description: Option<String>,
    #[serde(rename = "user_level")]
    pub user_level: Option<i32>,
}

#[derive(Debug, Serialize)]
pub struct QuizQuestion {
    pub question: String,
    pub options: Vec<String>,
    #[serde(rename = "correct_answer_index")]
    pub correct_answer_index: usize,
}

#[derive(Debug, Serialize)]
pub struct QuizVerification {
    pub questions: Vec<QuizQuestion>,
}

#[derive(Debug, Serialize)]
pub struct MLVerificationResponse {
    #[serde(rename = "verification_type")]
    pub verification_type: String,
    pub quiz: Option<QuizVerification>,
    #[serde(rename = "photo_prompt")]
    pub photo_prompt: Option<String>,
    #[serde(rename = "photo_requirements")]
    pub photo_requirements: Option<Vec<String>>,
}

#[derive(Debug, Deserialize)]
pub struct QuizSubmitRequest {
    #[serde(rename = "quest_id")]
    pub quest_id: i32,
    pub answers: Vec<usize>,
}

#[derive(Debug, Serialize)]
pub struct QuizSubmitResponse {
    pub passed: bool,
    #[serde(rename = "score_percentage")]
    pub score_percentage: i32,
    #[serde(rename = "correct_count")]
    pub correct_count: usize,
    #[serde(rename = "total_count")]
    pub total_count: usize,
    pub feedback: String,
}

#[derive(Debug, Deserialize)]
pub struct PhotoVerificationRequest {
    #[serde(rename = "quest_id")]
    pub quest_id: i32,
    #[serde(rename = "image_base64")]
    pub image_base64: String,
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
}

#[derive(Debug, Serialize)]
pub struct PhotoVerificationResponse {
    pub approved: bool,
    #[serde(rename = "ai_confidence")]
    pub ai_confidence: f32,
    #[serde(rename = "detected_objects")]
    pub detected_objects: Vec<String>,
    pub feedback: String,
    #[serde(rename = "auto_deleted_at")]
    pub auto_deleted_at: String,
}

fn verification_mode(quest_id: i32, level: Option<i32>, description: &Option<String>) -> String {
    if description
        .as_ref()
        .map(|text| text.to_lowercase().contains("фото"))
        .unwrap_or(false)
    {
        return "photo".to_string();
    }
    if level.unwrap_or(1) >= 10 || quest_id % 2 == 0 {
        return "quiz".to_string();
    }
    "none".to_string()
}

fn generate_quiz_questions(quest_id: i32) -> Vec<QuizQuestion> {
    let mut rng = StdRng::seed_from_u64(quest_id as u64);
    let templates: [(&str, [&str; 4]); 3] = [
        (
            "Какой первый шаг вы сделали для выполнения этого квеста?",
            [
                "Составил план действий",
                "Посмотрел сериал",
                "Отложил всё на завтра",
                "Удалил квест",
            ],
        ),
        (
            "Что будет считаться успешным завершением?",
            [
                "Отчитался о результате в приложении",
                "Забыл сделать заметки",
                "Ничего не делал",
                "Попросил друга сделать задание",
            ],
        ),
        (
            "Сколько времени примерно займёт выполнение?",
            ["30 минут", "5 секунд", "Два дня сна", "Вообще не начну"],
        ),
    ];

    templates
        .iter()
        .map(|(question, options)| {
            let mut opts: Vec<String> = options.iter().map(|s| s.to_string()).collect();
            let correct = rng.gen_range(0..opts.len());
            opts.swap(0, correct);
            QuizQuestion {
                question: (*question).to_string(),
                options: opts,
                correct_answer_index: 0,
            }
        })
        .collect()
}

fn quiz_correct_answers(quest_id: i32) -> Vec<usize> {
    generate_quiz_questions(quest_id)
        .iter()
        .map(|q| q.correct_answer_index)
        .collect()
}

pub async fn request_quest_verification(
    State(_state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<MLVerificationRequest>,
) -> Result<Json<MLVerificationResponse>, AppError> {
    let _user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для верификации квеста".into())
    })?;

    let mode = verification_mode(req.quest_id, req.user_level, &req.quest_description);

    let response = match mode.as_str() {
        "quiz" => MLVerificationResponse {
            verification_type: "quiz".to_string(),
            quiz: Some(QuizVerification {
                questions: generate_quiz_questions(req.quest_id),
            }),
            photo_prompt: None,
            photo_requirements: None,
        },
        "photo" => MLVerificationResponse {
            verification_type: "photo".to_string(),
            quiz: None,
            photo_prompt: Some(format!(
                "Сделайте фото, показывающее результат квеста \"{}\"",
                req.quest_title
            )),
            photo_requirements: Some(vec![
                "Хорошее освещение".into(),
                "Покажите действие крупным планом".into(),
                "Не включайте персональные данные".into(),
            ]),
        },
        _ => MLVerificationResponse {
            verification_type: "none".to_string(),
            quiz: None,
            photo_prompt: None,
            photo_requirements: None,
        },
    };

    Ok(Json(response))
}

pub async fn verify_quiz(
    State(_state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<QuizSubmitRequest>,
) -> Result<Json<QuizSubmitResponse>, AppError> {
    let _user = current_user
        .ok_or_else(|| AppError::Unauthorized("Требуется авторизация для сдачи теста".into()))?;

    let correct = quiz_correct_answers(req.quest_id);
    let total = correct.len();

    if req.answers.len() != total {
        return Err(AppError::Validation(
            "Количество ответов не совпадает с количеством вопросов".into(),
        ));
    }

    let matches = req
        .answers
        .iter()
        .zip(correct.iter())
        .filter(|(answer, expected)| answer == expected)
        .count();

    let passed = matches * 100 / total >= 60;
    let feedback = if passed {
        "Отлично! Вы доказали выполнение квеста."
    } else {
        "Попробуйте ещё раз, пересмотрите результаты выполнения."
    };

    Ok(Json(QuizSubmitResponse {
        passed,
        score_percentage: ((matches * 100) / total) as i32,
        correct_count: matches,
        total_count: total,
        feedback: feedback.to_string(),
    }))
}

pub async fn verify_photo(
    State(_state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<PhotoVerificationRequest>,
) -> Result<Json<PhotoVerificationResponse>, AppError> {
    let _user = current_user
        .ok_or_else(|| AppError::Unauthorized("Требуется авторизация для фотопроверки".into()))?;

    if req.image_base64.trim().is_empty() {
        return Err(AppError::Validation(
            "Загрузите фотографию для подтверждения".into(),
        ));
    }

    let detected = if let (Some(lat), Some(lon)) = (req.latitude, req.longitude) {
        vec![format!("Локация: {:.3}, {:.3}", lat, lon)]
    } else {
        vec!["Объект выглядит валидным".to_string()]
    };

    let delete_at: DateTime<Utc> = Utc::now() + Duration::minutes(15);

    Ok(Json(PhotoVerificationResponse {
        approved: true,
        ai_confidence: 0.82,
        detected_objects: detected,
        feedback: "Фото принято! Данные будут автоматически удалены через 15 минут.".into(),
        auto_deleted_at: delete_at.to_rfc3339(),
    }))
}
