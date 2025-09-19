#!/usr/bin/env python3
"""
Простой Python сервер для демонстрации RAG функциональности IRL Quest
Преобразует TODO задачи в D&D стиль квесты
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import uvicorn
import random
import json

app = FastAPI(
    title="IRL Quest RAG Server",
    description="Transform your boring TODO into epic D&D adventures!",
    version="1.0.0"
)

# Добавляем CORS для мобильного приложения
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Модели данных
class QuestGenerationRequest(BaseModel):
    todo_text: str
    context: Optional[str] = None
    difficulty_preference: Optional[int] = None
    theme_preference: Optional[str] = None
    user_level: Optional[int] = None

class GeneratedTask(BaseModel):
    title: str
    description: str
    difficulty: int
    experience_reward: int
    estimated_duration: Optional[int] = None

class QuestGenerationResponse(BaseModel):
    title: str
    description: str
    difficulty: int
    reward_experience: int
    reward_description: str
    tags: List[str]
    quest_type: str
    tasks: List[GeneratedTask]
    story_context: Optional[str] = None

class TaskEnhancementRequest(BaseModel):
    task_text: str
    context: Optional[str] = None
    user_level: Optional[int] = None

class TaskEnhancementResponse(BaseModel):
    enhanced_title: str
    enhanced_description: str
    suggested_difficulty: int
    suggested_experience: int
    story_context: Optional[str] = None
    suggested_tags: List[str]

class UserDto(BaseModel):
    id: int
    email: str
    username: str
    is_active: bool
    level: int
    experience: int
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    timezone: str
    last_login: Optional[str] = None
    settings: dict = {}
    created_at: str

class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserDto

class RegisterRequest(BaseModel):
    email: str
    username: str
    password: str
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    timezone: Optional[str] = None

class LoginRequest(BaseModel):
    username: str
    password: str

# Шаблоны для генерации квестов
FANTASY_TITLES = [
    "The Sacred Mission of {}",
    "Quest for the {} Artifact", 
    "The {} Chronicle",
    "Legend of the {} Hero",
    "The {} Prophecy"
]

SCIFI_TITLES = [
    "Mission: {}",
    "Protocol {}", 
    "Operation {}",
    "The {} Directive",
    "Project: {}"
]

MODERN_TITLES = [
    "The {} Challenge",
    "Project: {}", 
    "{} Goals",
    "The {} Initiative",
    "Mission: {}"
]

MEDIEVAL_TITLES = [
    "The {} Crusade",
    "Quest of the {} Knight", 
    "The {} Tournament",
    "The Royal {} Decree",
    "The {} Pilgrimage"
]

def extract_task_essence(text: str) -> str:
    """Извлекает суть задачи для заголовка"""
    words = text.split()[:3]
    return " ".join(word.capitalize() for word in words)

def calculate_difficulty(text: str, user_level: int = 1) -> int:
    """Вычисляет сложность задачи на основе текста"""
    words = len(text.split())
    difficulty = 2  # По умолчанию средняя
    
    if words < 3:
        difficulty = 1
    elif words > 10:
        difficulty = 3
        
    # Ключевые слова сложности
    if any(word in text.lower() for word in ["complex", "difficult", "challenging", "hard"]):
        difficulty += 1
    if any(word in text.lower() for word in ["simple", "easy", "quick", "basic"]):
        difficulty = max(1, difficulty - 1)
        
    # Учитываем уровень пользователя
    if user_level > 10:
        difficulty = min(5, difficulty + 1)
        
    return max(1, min(5, difficulty))

def calculate_experience(difficulty: int, user_level: int) -> int:
    """Вычисляет опыт за задачу"""
    base = {1: 10, 2: 25, 3: 50, 4: 100, 5: 200}.get(difficulty, 50)
    return base + (user_level * 5)

def generate_fantasy_quest(todo_text: str, difficulty: int, user_level: int) -> tuple:
    """Генерирует фэнтезийный квест"""
    essence = extract_task_essence(todo_text)
    title = random.choice(FANTASY_TITLES).format(essence)
    
    difficulty_name = {1: "trivial", 2: "easy", 3: "moderate", 4: "hard", 5: "legendary"}[difficulty]
    
    description = f"""In the mystical realm of productivity, a great challenge awaits. The ancient scrolls speak of {todo_text.lower()}. 
Only a hero of your caliber (Level {user_level}) can undertake this {difficulty_name} difficulty quest. 
The kingdom depends on your success, brave adventurer!"""

    story_context = """The Council of Elders has bestowed upon you this sacred mission. Your actions will echo through the halls of history. 
Complete this quest to gain favor with the magical forces and unlock new powers in your journey of self-improvement."""

    return title, description, story_context

def generate_scifi_quest(todo_text: str, difficulty: int, user_level: int) -> tuple:
    """Генерирует научно-фантастический квест"""
    essence = extract_task_essence(todo_text)
    title = random.choice(SCIFI_TITLES).format(essence)
    
    stardate = random.randint(1, 365)
    
    description = f"""Stardate 2024.{stardate}: Commander, your mission parameters are clear. The task '{todo_text}' is classified as Priority Level {difficulty}. 
Your current rank (Level {user_level}) qualifies you for this operation. The future of the galaxy may depend on its completion."""

    story_context = """The Galactic Council has transmitted this critical mission to your personal datapad. 
Success will advance your standing in the Space Fleet and unlock advanced technologies for future missions."""

    return title, description, story_context

def generate_modern_quest(todo_text: str, difficulty: int, user_level: int) -> tuple:
    """Генерирует современный квест"""
    essence = extract_task_essence(todo_text)
    title = random.choice(MODERN_TITLES).format(essence)
    
    description = f"""Welcome to your personal development journey! Today's challenge: '{todo_text}'. 
This is a Level {difficulty} difficulty task, perfect for someone at your current stage (Level {user_level}). 
Complete this to boost your productivity score and unlock new achievements!"""

    story_context = """You're part of an elite group of productivity ninjas. Each completed task brings you closer to 
mastering the art of getting things done and achieving your life goals."""

    return title, description, story_context

def generate_medieval_quest(todo_text: str, difficulty: int, user_level: int) -> tuple:
    """Генерирует средневековый квест"""
    essence = extract_task_essence(todo_text)
    title = random.choice(MEDIEVAL_TITLES).format(essence)
    
    difficulty_name = {1: "simple", 2: "modest", 3: "worthy", 4: "perilous", 5: "legendary"}[difficulty]
    
    description = f"""Hark! Noble knight of Level {user_level}, the King hath decreed that ye must undertake the sacred duty: '{todo_text}'. 
This quest of {difficulty_name} difficulty shall test thy mettle and bring great honor to thy name. 
May the blessing of the realm be upon thee!"""

    story_context = """In the grand halls of the castle, bards sing tales of heroes who complete such quests. 
Your success shall be recorded in the annals of history for future generations to admire."""

    return title, description, story_context

def generate_quest_tasks(todo_text: str, difficulty: int, base_exp: int) -> List[GeneratedTask]:
    """Генерирует подзадачи для квеста"""
    task_count = min(difficulty, 3)  # От 1 до 3 задач
    
    if task_count == 1:
        return [GeneratedTask(
            title=f"Complete: {todo_text}",
            description=f"Execute the main objective: {todo_text}",
            difficulty=difficulty,
            experience_reward=base_exp,
            estimated_duration=30 * difficulty
        )]
    
    tasks = []
    exp_per_task = base_exp // task_count
    
    # Подготовка
    tasks.append(GeneratedTask(
        title="Preparation Phase",
        description=f"Gather resources and prepare for: {todo_text}",
        difficulty=1,
        experience_reward=exp_per_task,
        estimated_duration=15
    ))
    
    # Выполнение
    if task_count > 2:
        tasks.append(GeneratedTask(
            title="Execution Phase",
            description=f"Progress on objective: {todo_text}",
            difficulty=difficulty - 1,
            experience_reward=exp_per_task,
            estimated_duration=20 * difficulty
        ))
    
    # Завершение
    tasks.append(GeneratedTask(
        title="Completion & Review",
        description=f"Finalize and verify: {todo_text}",
        difficulty=2,
        experience_reward=exp_per_task,
        estimated_duration=10
    ))
    
    return tasks

# API эндпоинты
@app.get("/")
async def root():
    return {"message": "IRL Quest RAG Server - Transform your TODO into epic adventures!"}

@app.get("/health")
async def health():
    return {"status": "ok", "version": "1.0.0"}

# Мок аутентификация для демонстрации
@app.post("/api/v1/auth/register", response_model=UserDto)
async def register(request: RegisterRequest):
    # Мок пользователь
    return UserDto(
        id=1,
        email=request.email,
        username=request.username,
        is_active=True,
        level=1,
        experience=0,
        avatar_url=request.avatar_url,
        bio=request.bio,
        timezone=request.timezone or "UTC",
        created_at="2024-01-01T00:00:00Z"
    )

@app.post("/api/v1/auth/login", response_model=LoginResponse)
async def login(request: LoginRequest):
    # Мок авторизация
    user = UserDto(
        id=1,
        email="demo@irlquest.com",
        username=request.username,
        is_active=True,
        level=5,
        experience=1250,
        timezone="UTC",
        created_at="2024-01-01T00:00:00Z"
    )
    
    return LoginResponse(
        access_token="demo_token_12345",
        user=user
    )

@app.get("/api/v1/auth/me", response_model=UserDto)
async def get_me():
    return UserDto(
        id=1,
        email="demo@irlquest.com", 
        username="demo_user",
        is_active=True,
        level=5,
        experience=1250,
        timezone="UTC",
        created_at="2024-01-01T00:00:00Z"
    )

@app.post("/api/v1/rag/generate-quest", response_model=QuestGenerationResponse)
async def generate_quest(request: QuestGenerationRequest):
    """Основная функция - генерация квеста из TODO"""
    
    user_level = request.user_level or 1
    difficulty = request.difficulty_preference or calculate_difficulty(request.todo_text, user_level)
    theme = request.theme_preference or "fantasy"
    
    # Генерируем квест в зависимости от темы
    generators = {
        "fantasy": generate_fantasy_quest,
        "sci-fi": generate_scifi_quest, 
        "modern": generate_modern_quest,
        "medieval": generate_medieval_quest
    }
    
    generator = generators.get(theme, generate_fantasy_quest)
    title, description, story_context = generator(request.todo_text, difficulty, user_level)
    
    base_exp = calculate_experience(difficulty, user_level)
    tasks = generate_quest_tasks(request.todo_text, difficulty, base_exp)
    
    # Генерируем теги
    tags = [theme, "generated"]
    text_lower = request.todo_text.lower()
    if any(word in text_lower for word in ["work", "job", "office"]):
        tags.append("work")
    if any(word in text_lower for word in ["study", "learn", "read"]):
        tags.append("learning") 
    if any(word in text_lower for word in ["exercise", "gym", "health"]):
        tags.append("health")
    if any(word in text_lower for word in ["clean", "organize", "tidy"]):
        tags.append("home")
    
    return QuestGenerationResponse(
        title=title,
        description=description,
        difficulty=difficulty,
        reward_experience=base_exp * 3,
        reward_description=f"Complete this {theme} adventure to earn {base_exp * 3} experience points and unlock new abilities!",
        tags=tags,
        quest_type="generated",
        tasks=tasks,
        story_context=story_context
    )

@app.post("/api/v1/rag/enhance-task", response_model=TaskEnhancementResponse)
async def enhance_task(request: TaskEnhancementRequest):
    """Улучшение задачи с добавлением сюжета"""
    
    user_level = request.user_level or 1
    difficulty = calculate_difficulty(request.task_text, user_level)
    experience = calculate_experience(difficulty, user_level)
    
    essence = extract_task_essence(request.task_text)
    
    difficulty_name = {1: "Errand", 2: "Task", 3: "Mission", 4: "Quest", 5: "Legendary Feat"}[difficulty]
    
    enhanced_title = f"Epic {difficulty_name}: {essence}"
    
    challenge_name = {1: "simple", 2: "moderate", 3: "challenging", 4: "formidable", 5: "legendary"}[difficulty]
    
    enhanced_description = f"""Behold, Level {user_level} adventurer! Your mission: {request.task_text}. 
This {challenge_name} challenge will test your skills and grant you valuable experience upon completion. 
Prepare yourself for an epic journey of productivity!"""

    story_context = """In the grand adventure of life, every task completed brings you one step closer to mastering your destiny. 
This particular challenge has been crafted by the gods of productivity to help you grow stronger."""

    # Генерируем теги
    tags = ["enhanced"]
    text_lower = request.task_text.lower()
    if any(word in text_lower for word in ["work", "job"]):
        tags.append("work")
    if any(word in text_lower for word in ["study", "learn"]):
        tags.append("learning")
    if any(word in text_lower for word in ["exercise", "health"]):
        tags.append("health")
    
    return TaskEnhancementResponse(
        enhanced_title=enhanced_title,
        enhanced_description=enhanced_description,
        suggested_difficulty=difficulty,
        suggested_experience=experience,
        story_context=story_context,
        suggested_tags=tags
    )

# Placeholder endpoints для мобильного приложения
@app.get("/api/v1/quests")
async def get_quests():
    return []

@app.get("/api/v1/tasks") 
async def get_tasks():
    return []

@app.get("/api/v1/users/me/stats")
async def get_user_stats():
    return {
        "level": 5,
        "experience": 1250,
        "total_quests": 15,
        "completed_quests": 12,
        "total_tasks": 47,
        "completed_tasks": 39,
        "achievements_count": 8
    }

@app.get("/api/v1/users/me/achievements")
async def get_user_achievements():
    return []

if __name__ == "__main__":
    print("🎮 Starting IRL Quest RAG Server...")
    print("🚀 Transform your boring TODO into epic D&D adventures!")
    uvicorn.run(app, host="0.0.0.0", port=8004, log_level="info")