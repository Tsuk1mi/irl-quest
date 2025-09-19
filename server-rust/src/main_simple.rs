use axum::{
    extract::{Json, Path, State},
    http::StatusCode,
    response::Json as ResponseJson,
    routing::{delete, get, post, put},
    Router,
};
use serde::{Deserialize, Serialize};
use std::{collections::HashMap, sync::Arc};
use tokio::sync::RwLock;
use tower_http::cors::CorsLayer;

// Simple in-memory storage
#[derive(Clone, Debug)]
pub struct AppState {
    users: Arc<RwLock<HashMap<i32, User>>>,
    tasks: Arc<RwLock<HashMap<i32, Task>>>,
    quests: Arc<RwLock<HashMap<i32, Quest>>>,
    next_id: Arc<RwLock<i32>>,
}

// Simplified models
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: i32,
    pub email: String,
    pub username: String,
    pub password: String, // In real app, this would be hashed
    pub level: i32,
    pub experience: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Task {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub completed: bool,
    pub user_id: i32,
    pub quest_id: Option<i32>,
    pub difficulty: i32,
    pub experience_reward: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Quest {
    pub id: i32,
    pub title: String,
    pub description: Option<String>,
    pub difficulty: i32,
    pub user_id: i32,
    pub completion_percentage: i32,
    pub reward_experience: i32,
}

// Request/Response DTOs
#[derive(Debug, Deserialize)]
pub struct UserCreate {
    pub email: String,
    pub username: String,
    pub password: String,
}

#[derive(Debug, Deserialize)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
}

#[derive(Debug, Serialize)]
pub struct LoginResponse {
    pub access_token: String,
    pub token_type: String,
    pub user: User,
}

#[derive(Debug, Deserialize)]
pub struct TaskCreate {
    pub title: String,
    pub description: Option<String>,
    pub quest_id: Option<i32>,
    pub difficulty: Option<i32>,
    pub experience_reward: Option<i32>,
}

#[derive(Debug, Deserialize)]
pub struct QuestCreate {
    pub title: String,
    pub description: Option<String>,
    pub difficulty: Option<i32>,
    pub reward_experience: Option<i32>,
}

// Handlers
async fn health() -> &'static str {
    "OK"
}

async fn register(
    State(state): State<AppState>,
    Json(user_data): Json<UserCreate>,
) -> Result<ResponseJson<User>, StatusCode> {
    let mut users = state.users.write().await;
    let mut next_id = state.next_id.write().await;
    
    // Check if user already exists
    for user in users.values() {
        if user.email == user_data.email || user.username == user_data.username {
            return Err(StatusCode::CONFLICT);
        }
    }

    let user = User {
        id: *next_id,
        email: user_data.email,
        username: user_data.username,
        password: user_data.password, // Should be hashed in production
        level: 1,
        experience: 0,
    };

    users.insert(*next_id, user.clone());
    *next_id += 1;

    Ok(ResponseJson(user))
}

async fn login(
    State(state): State<AppState>,
    Json(login_data): Json<LoginRequest>,
) -> Result<ResponseJson<LoginResponse>, StatusCode> {
    let users = state.users.read().await;
    
    for user in users.values() {
        if user.username == login_data.username && user.password == login_data.password {
            let response = LoginResponse {
                access_token: format!("fake_token_for_user_{}", user.id),
                token_type: "bearer".to_string(),
                user: user.clone(),
            };
            return Ok(ResponseJson(response));
        }
    }

    Err(StatusCode::UNAUTHORIZED)
}

async fn get_me(
    State(state): State<AppState>,
) -> Result<ResponseJson<User>, StatusCode> {
    let users = state.users.read().await;
    
    // For simplicity, return first user
    if let Some(user) = users.values().next() {
        Ok(ResponseJson(user.clone()))
    } else {
        Err(StatusCode::NOT_FOUND)
    }
}

async fn list_tasks(
    State(state): State<AppState>,
) -> Result<ResponseJson<Vec<Task>>, StatusCode> {
    let tasks = state.tasks.read().await;
    let task_list: Vec<Task> = tasks.values().cloned().collect();
    Ok(ResponseJson(task_list))
}

async fn create_task(
    State(state): State<AppState>,
    Json(task_data): Json<TaskCreate>,
) -> Result<ResponseJson<Task>, StatusCode> {
    let mut tasks = state.tasks.write().await;
    let mut next_id = state.next_id.write().await;

    let task = Task {
        id: *next_id,
        title: task_data.title,
        description: task_data.description,
        completed: false,
        user_id: 1, // Simplified - assuming user 1
        quest_id: task_data.quest_id,
        difficulty: task_data.difficulty.unwrap_or(1),
        experience_reward: task_data.experience_reward.unwrap_or(10),
    };

    tasks.insert(*next_id, task.clone());
    *next_id += 1;

    Ok(ResponseJson(task))
}

async fn get_task(
    Path(task_id): Path<i32>,
    State(state): State<AppState>,
) -> Result<ResponseJson<Task>, StatusCode> {
    let tasks = state.tasks.read().await;
    
    if let Some(task) = tasks.get(&task_id) {
        Ok(ResponseJson(task.clone()))
    } else {
        Err(StatusCode::NOT_FOUND)
    }
}

async fn update_task(
    Path(task_id): Path<i32>,
    State(state): State<AppState>,
    Json(task_data): Json<TaskCreate>,
) -> Result<ResponseJson<Task>, StatusCode> {
    let mut tasks = state.tasks.write().await;
    
    if let Some(task) = tasks.get_mut(&task_id) {
        task.title = task_data.title;
        task.description = task_data.description;
        if let Some(difficulty) = task_data.difficulty {
            task.difficulty = difficulty;
        }
        if let Some(reward) = task_data.experience_reward {
            task.experience_reward = reward;
        }
        Ok(ResponseJson(task.clone()))
    } else {
        Err(StatusCode::NOT_FOUND)
    }
}

async fn delete_task(
    Path(task_id): Path<i32>,
    State(state): State<AppState>,
) -> Result<StatusCode, StatusCode> {
    let mut tasks = state.tasks.write().await;
    
    if tasks.remove(&task_id).is_some() {
        Ok(StatusCode::NO_CONTENT)
    } else {
        Err(StatusCode::NOT_FOUND)
    }
}

async fn list_quests(
    State(state): State<AppState>,
) -> Result<ResponseJson<Vec<Quest>>, StatusCode> {
    let quests = state.quests.read().await;
    let quest_list: Vec<Quest> = quests.values().cloned().collect();
    Ok(ResponseJson(quest_list))
}

async fn create_quest(
    State(state): State<AppState>,
    Json(quest_data): Json<QuestCreate>,
) -> Result<ResponseJson<Quest>, StatusCode> {
    let mut quests = state.quests.write().await;
    let mut next_id = state.next_id.write().await;

    let quest = Quest {
        id: *next_id,
        title: quest_data.title,
        description: quest_data.description,
        difficulty: quest_data.difficulty.unwrap_or(1),
        user_id: 1, // Simplified
        completion_percentage: 0,
        reward_experience: quest_data.reward_experience.unwrap_or(50),
    };

    quests.insert(*next_id, quest.clone());
    *next_id += 1;

    Ok(ResponseJson(quest))
}

async fn get_quest(
    Path(quest_id): Path<i32>,
    State(state): State<AppState>,
) -> Result<ResponseJson<Quest>, StatusCode> {
    let quests = state.quests.read().await;
    
    if let Some(quest) = quests.get(&quest_id) {
        Ok(ResponseJson(quest.clone()))
    } else {
        Err(StatusCode::NOT_FOUND)
    }
}

#[tokio::main]
async fn main() {
    // Initialize tracing
    tracing_subscriber::fmt::init();

    // Create app state
    let state = AppState {
        users: Arc::new(RwLock::new(HashMap::new())),
        tasks: Arc::new(RwLock::new(HashMap::new())),
        quests: Arc::new(RwLock::new(HashMap::new())),
        next_id: Arc::new(RwLock::new(1)),
    };

    // Create demo data
    {
        let mut users = state.users.write().await;
        users.insert(1, User {
            id: 1,
            email: "demo@example.com".to_string(),
            username: "demo".to_string(),
            password: "password".to_string(),
            level: 1,
            experience: 0,
        });

        let mut tasks = state.tasks.write().await;
        tasks.insert(1, Task {
            id: 1,
            title: "Изучить Rust".to_string(),
            description: Some("Освоить основы программирования на Rust".to_string()),
            completed: false,
            user_id: 1,
            quest_id: Some(1),
            difficulty: 3,
            experience_reward: 50,
        });

        let mut quests = state.quests.write().await;
        quests.insert(1, Quest {
            id: 1,
            title: "Стать Rust разработчиком".to_string(),
            description: Some("Изучить Rust и создать свой первый проект".to_string()),
            difficulty: 4,
            user_id: 1,
            completion_percentage: 25,
            reward_experience: 200,
        });

        *state.next_id.write().await = 2;
    }

    // Build our application with routes
    let app = Router::new()
        .route("/", get(|| async { "IRL Quest Server Running!" }))
        .route("/health", get(health))
        .nest(
            "/api/v1",
            Router::new()
                .nest(
                    "/auth",
                    Router::new()
                        .route("/register", post(register))
                        .route("/token", post(login))
                        .route("/me", get(get_me)),
                )
                .nest(
                    "/tasks",
                    Router::new()
                        .route("/", get(list_tasks).post(create_task))
                        .route("/:id", get(get_task).put(update_task).delete(delete_task)),
                )
                .nest(
                    "/quests",
                    Router::new()
                        .route("/", get(list_quests).post(create_quest))
                        .route("/:id", get(get_quest)),
                ),
        )
        .layer(CorsLayer::permissive())
        .with_state(state);

    // Run server
    let listener = tokio::net::TcpListener::bind("0.0.0.0:8002").await.unwrap();
    println!("🚀 Server running on http://0.0.0.0:8002");
    axum::serve(listener, app).await.unwrap();
}