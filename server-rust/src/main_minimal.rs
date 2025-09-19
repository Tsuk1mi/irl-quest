use axum::{
    http::StatusCode,
    response::Json,
    routing::{get, post},
    Router,
};
use serde::{Deserialize, Serialize};
use tower_http::cors::CorsLayer;

#[derive(Debug, Deserialize)]
struct QuestRequest {
    todo_text: String,
    // theme_preference removed; theme now auto-detected
    difficulty_preference: Option<u8>,
    context: Option<String>,
}

#[derive(Debug, Serialize)]
struct QuestResponse {
    title: String,
    description: String,
    difficulty: u8,
    reward_experience: u32,
    reward_description: String,
    tags: Vec<String>,
    quest_type: String,
    tasks: Vec<TaskResponse>,
    story_context: String,
}

#[derive(Debug, Serialize)]
struct TaskResponse {
    title: String,
    description: String,
    difficulty: u8,
    experience_reward: u32,
    estimated_duration: u32,
}

// Встроенные шаблоны для генерации квестов
fn generate_quest(req: QuestRequest) -> QuestResponse {
    let theme = detect_theme(&req.todo_text);
    let difficulty = req.difficulty_preference.unwrap_or(3).min(5).max(1);
    let base_exp = (difficulty as u32) * 50 + 100;
    
    let (title, description, story_context) = match theme.as_str() {
        "fantasy" => generate_fantasy_quest(&req.todo_text, difficulty),
        "sci-fi" => generate_scifi_quest(&req.todo_text, difficulty),
        "modern" => generate_modern_quest(&req.todo_text, difficulty),
        "medieval" => generate_medieval_quest(&req.todo_text, difficulty),
        _ => generate_fantasy_quest(&req.todo_text, difficulty),
    };

    let tasks = vec![
        TaskResponse {
            title: "Подготовительная фаза".to_string(),
            description: format!("Собери ресурсы и подготовься к: {}", req.todo_text),
            difficulty: (difficulty - 1).max(1),
            experience_reward: base_exp / 4,
            estimated_duration: 15,
        },
        TaskResponse {
            title: "Выполнение миссии".to_string(), 
            description: format!("Прогресс по цели: {}", req.todo_text),
            difficulty,
            experience_reward: base_exp / 2,
            estimated_duration: 60,
        },
        TaskResponse {
            title: "Завершение и проверка".to_string(),
            description: format!("Финализируй и проверь: {}", req.todo_text),
            difficulty: (difficulty - 1).max(1),
            experience_reward: base_exp / 4,
            estimated_duration: 10,
        },
    ];

    QuestResponse {
        title,
        description,
        difficulty,
        reward_experience: base_exp,
        reward_description: format!("Завершите это {} приключение, чтобы заработать {} очков опыта!", theme, base_exp),
        tags: vec![theme.clone(), "generated".to_string()],
        quest_type: "generated".to_string(),
        tasks,
        story_context,
    }
}

fn generate_fantasy_quest(todo_text: &str, difficulty: u8) -> (String, String, String) {
    let titles = vec![
        format!("Священная миссия {}", extract_essence(todo_text)),
        format!("Древнее пророчество о {}", extract_essence(todo_text)),
        format!("Квест мага: {}", extract_essence(todo_text)),
    ];
    
    let descriptions = vec![
        format!("В мистическом царстве продуктивности ждет великий вызов. Древние свитки говорят о {}. \nТолько герой твоего калибра может взяться за это {} сложности задание. \nКоролевство зависит от твоего успеха, отважный искатель приключений!", 
                todo_text, difficulty_name(difficulty)),
        format!("Кристальный шар показывает видение: {}. Мудрые эльфы предрекают, что только воин {} уровня сможет справиться с этой задачей сложности {}.", 
                todo_text, 1, difficulty),
    ];

    let story = format!("Совет Старейшин даровал тебе эту священную миссию. Твои действия отзовутся эхом в залах истории. \nВыполни этот квест, чтобы заслужить благосклонность магических сил и открыть новые силы в своем путешествии самосовершенствования.");

    (
        titles[0].clone(),
        descriptions[0].clone(),
        story,
    )
}

fn generate_scifi_quest(todo_text: &str, difficulty: u8) -> (String, String, String) {
    let title = format!("Протокол {}", extract_essence(todo_text));
    let description = format!("Звездная дата 2024.168: Командир, параметры миссии ясны. Задача '{}' классифицирована как приоритет уровня {}. \nВаш текущий ранг (Уровень 1) квалифицирует вас для этой операции. Будущее галактики может зависеть от успеха миссии.", 
                            todo_text, difficulty);
    let story = format!("Центральное Командование выбрало вас для этой критически важной операции. Ваши навыки и решимость - последняя надежда человечества.");

    (title, description, story)
}

fn generate_modern_quest(todo_text: &str, difficulty: u8) -> (String, String, String) {
    let title = format!("Вызов современного героя: {}", extract_essence(todo_text));
    let description = format!("🏆 ВЫЗОВ ПРИНЯТ! Твоя миссия: {}. \nУровень сложности: {} из 5 ⭐\nВремя показать, из какого ты теста сделан! Этот челлендж проверит все твои навыки.", 
                            todo_text, difficulty);
    let story = format!("В мире бесконечных возможностей ты выбираешь стать лучшей версией себя. Каждое выполненное задание приближает тебя к цели.");

    (title, description, story)
}

fn generate_medieval_quest(todo_text: &str, difficulty: u8) -> (String, String, String) {
    let title = format!("Рыцарская честь: {}", extract_essence(todo_text));
    let description = format!("Услышь, благородный рыцарь! Король поручает тебе выполнить: {}. \nСложность сего деяния оценивается как {} из V уровней. \nТвоя честь и слава зависят от успешного завершения сей благородной миссии!", 
                            todo_text, difficulty);
    let story = format!("В эпоху рыцарства и чести твои деяния будут воспеты менестрелями. Докажи свою доблесть!");

    (title, description, story)
}

fn extract_essence(text: &str) -> String {
    let words: Vec<&str> = text.split_whitespace().collect();
    if words.len() > 3 {
        words[0..3].join(" ")
    } else {
        text.to_string()
    }
}

fn difficulty_name(difficulty: u8) -> &'static str {
    match difficulty {
        1 => "очень легкой",
        2 => "легкой", 
        3 => "умеренной",
        4 => "сложной",
        5 => "эпической",
        _ => "умеренной",
    }
}

// API Handlers
async fn health() -> Json<serde_json::Value> {
    Json(serde_json::json!({
        "message": "IRL Quest Rust Server - Ready to transform TODO into epic adventures!",
        "version": "0.1.0",
        "status": "healthy"
    }))
}

async fn generate_quest_handler(Json(request): Json<QuestRequest>) -> Result<Json<QuestResponse>, StatusCode> {
    let quest = generate_quest(request);
    Ok(Json(quest))
}

async fn list_quests() -> Json<Vec<serde_json::Value>> {
    Json(vec![
        serde_json::json!({
            "id": 1,
            "title": "Sample Quest",
            "description": "This is a sample quest",
            "difficulty": 3,
            "status": "completed"
        })
    ])
}

async fn get_user_profile() -> Json<serde_json::Value> {
    Json(serde_json::json!({
        "id": 1,
        "username": "hero",
        "level": 1,
        "experience": 0,
        "total_quests": 0
    }))
}

async fn get_achievements() -> Json<Vec<serde_json::Value>> {
    Json(vec![
        serde_json::json!({
            "id": 1,
            "title": "First Steps",
            "description": "Complete your first quest",
            "unlocked": false
        })
    ])
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize tracing
    tracing_subscriber::fmt::init();

    // Create CORS layer
    let cors = CorsLayer::permissive();

    // Build the router
    let app = Router::new()
        .route("/", get(health))
        .route("/api/v1/health", get(health))
        .route("/api/v1/rag/generate-quest", post(generate_quest_handler))
        .route("/api/v1/quests", get(list_quests))
        .route("/api/v1/users/me", get(get_user_profile))
        .route("/api/v1/users/me/achievements", get(get_achievements))
        .layer(cors);

    println!("🦀 Starting IRL Quest Rust Server...");
    println!("🚀 Transform your boring TODO into epic D&D adventures!");
    println!("🌐 Server running at http://0.0.0.0:8006");

    fn detect_theme(todo_text: &str) -> String {
        let text = todo_text.to_lowercase();
        if text.contains("экзамен") || text.contains("зачет") || text.contains("lecture") || text.contains("course") || text.contains("study") {
            return "modern".to_string();
        }
        if text.contains("api") || text.contains("deploy") || text.contains("cloud") || text.contains("project") {
            return "sci-fi".to_string();
        }
        if text.contains("уборк") || text.contains("дом") || text.contains("покупк") || text.contains("домашн") {
            return "modern".to_string();
        }
        "fantasy".to_string()
    }

    // Run the server
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8006").await?;
    axum::serve(listener, app).await?;

    Ok(())
}

fn detect_theme(todo_text: &str) -> String {
    let text = todo_text.to_lowercase();
    if text.contains("экзамен") || text.contains("зачет") || text.contains("lecture") || text.contains("course") || text.contains("study") {
        return "modern".to_string();
    }
    if text.contains("api") || text.contains("deploy") || text.contains("cloud") || text.contains("project") {
        return "sci-fi".to_string();
    }
    if text.contains("уборк") || text.contains("дом") || text.contains("покупк") || text.contains("домашн") {
        return "modern".to_string();
    }
    "fantasy".to_string()
}