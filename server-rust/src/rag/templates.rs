use crate::models::{GeneratedTask, QuestGenerationResponse, TaskEnhancementResponse};

pub struct QuestTemplates;

impl QuestTemplates {
    pub fn generate_quest_from_todo(
        todo_text: &str,
        _context: Option<&str>,
        difficulty: i32,
        user_level: i32,
    ) -> QuestGenerationResponse {
        // Всегда используем фэнтези тему
        let theme = "fantasy".to_string();
        // Всегда используем средневековое фэнтези
        let (title, description, story_context) = generate_fantasy_quest(todo_text, difficulty, user_level);


        let base_exp = calculate_base_experience(difficulty, user_level);
        let tasks = generate_quest_tasks(todo_text, difficulty, base_exp);
        let tags = generate_tags_for_quest(todo_text, &theme);
        if let Some(ctx) = _context { let _ = ctx; }

        QuestGenerationResponse {
            title,
            description,
            difficulty,
            reward_experience: base_exp * 3,
            reward_description: format!("Заверши это фэнтези приключение, чтобы заработать {} опыта и открыть новые способности!", base_exp * 3),
            tags,
            quest_type: "generated".to_string(),
            tasks,
            story_context: Some(story_context),
        }
    }

    pub fn enhance_task(
        task_text: &str,
        _context: Option<&str>,
        user_level: i32,
    ) -> TaskEnhancementResponse {
        let difficulty = calculate_task_difficulty(task_text, user_level);
        let experience = calculate_base_experience(difficulty, user_level);
        
        let (enhanced_title, enhanced_description, story_context) = 
            enhance_task_with_story(task_text, difficulty, user_level);
        
        let tags = generate_tags_for_task(task_text);

        TaskEnhancementResponse {
            enhanced_title,
            enhanced_description,
            suggested_difficulty: difficulty,
            suggested_experience: experience,
            story_context: Some(story_context),
            suggested_tags: tags,
        }
    }
}

fn generate_fantasy_quest(todo_text: &str, difficulty: i32, user_level: i32) -> (String, String, String) {
    let quest_titles = [
        "⚔️ Священная миссия: {}",
        "🏆 Поиски артефакта: {}", 
        "📜 Хроники: {}",
        "⭐ Легенда о герое: {}",
        "✨ Пророчество: {}"
    ];
    
    let task_essence = extract_task_essence(todo_text);
    let title_template = quest_titles[hash_string(todo_text) % quest_titles.len()];
    let title = title_template.replace("{}", &task_essence);
    
    let difficulty_name = match difficulty {
        1 => "простой",
        2 => "лёгкий", 
        3 => "средний",
        4 => "сложный",
        5 => "легендарный",
        _ => "неизвестный"
    };
    
    let description = format!(
        "В мистическом царстве продуктивности ждёт великое испытание. Древние свитки гласят о «{}». \
        Только герой твоего калибра (Уровень {}) может взяться за этот {} квест. \
        Королевство зависит от твоего успеха, отважный искатель приключений!",
        todo_text.to_lowercase(),
        user_level,
        difficulty_name
    );

    let story_context = format!(
        "Совет Старейшин возложил на тебя эту священную миссию. Твои деяния отзовутся эхом в залах истории. \
        Заверши этот квест, чтобы снискать расположение магических сил и открыть новые способности в путешествии к самосовершенствованию."
    );

    (title, description, story_context)
}

// Удалено - используется только фэнтези тема

// Удалено - используется только фэнтези тема

fn generate_medieval_quest(todo_text: &str, difficulty: i32, user_level: i32) -> (String, String, String) {
    let quest_titles = [
        "The {} Crusade",
        "Quest of the {} Knight", 
        "The {} Tournament",
        "The Royal {} Decree",
        "The {} Pilgrimage"
    ];
    
    let task_essence = extract_task_essence(todo_text);
    let title_template = quest_titles[hash_string(todo_text) % quest_titles.len()];
    let title = title_template.replace("{}", &task_essence);
    
    let description = format!(
        "Hark! Noble knight of Level {}, the King hath decreed that ye must undertake the sacred duty: '{}'. \
        This quest of {} difficulty shall test thy mettle and bring great honor to thy name. \
        May the blessing of the realm be upon thee!",
        user_level,
        todo_text,
        match difficulty {
            1 => "simple",
            2 => "modest", 
            3 => "worthy",
            4 => "perilous",
            5 => "legendary",
            _ => "mysterious"
        }
    );

    let story_context = format!(
        "In the grand halls of the castle, bards sing tales of heroes who complete such quests. \
        Your success shall be recorded in the annals of history for future generations to admire."
    );

    (title, description, story_context)
}

fn generate_quest_tasks(todo_text: &str, difficulty: i32, base_exp: i32) -> Vec<GeneratedTask> {
    let task_count = match difficulty {
        1 => 1,
        2 => 2,
        3 => 3,
        4 => 4,
        5 => 5,
        _ => 3,
    };

    let mut tasks = Vec::new();
    
    if task_count == 1 {
        tasks.push(GeneratedTask {
            title: format!("✅ Завершить: {}", todo_text),
            description: format!("Выполни основную цель: {}", todo_text),
            difficulty,
            experience_reward: base_exp,
            estimated_duration: Some(30 * difficulty),
            is_boss: is_boss_task(todo_text),
        });
    } else {
        // Разбиваем на подзадачи (на русском)
        tasks.push(GeneratedTask {
            title: "📋 Фаза подготовки".to_string(),
            description: format!("Собери ресурсы и подготовься к: {}", todo_text),
            difficulty: 1,
            experience_reward: base_exp / task_count,
            estimated_duration: Some(15),
            is_boss: false,
        });

        for i in 1..task_count-1 {
            tasks.push(GeneratedTask {
                title: format!("⚔️ Фаза выполнения {}", i),
                description: format!("Продвигайся к цели: {}", todo_text),
                difficulty: difficulty - 1,
                experience_reward: base_exp / task_count,
                estimated_duration: Some(20 * difficulty),
                is_boss: false,
            });
        }

        tasks.push(GeneratedTask {
            title: "✨ Завершение и проверка".to_string(),
            description: format!("Финализируй и проверь: {}", todo_text),
            difficulty: 2,
            experience_reward: base_exp / task_count,
            estimated_duration: Some(10),
            is_boss: is_boss_task(todo_text),
        });
    }

    tasks
}

fn enhance_task_with_story(task_text: &str, difficulty: i32, user_level: i32) -> (String, String, String) {
    let task_essence = extract_task_essence(task_text);
    
    let enhanced_title = format!("{} {}", 
        match difficulty {
            1 => "📝 Поручение:",
            2 => "📋 Задание:", 
            3 => "⚔️ Миссия:",
            4 => "🏆 Квест:",
            5 => "⚡ Легендарный подвиг:",
            _ => "📍 Задача:"
        },
        task_essence
    );
    
    let enhanced_description = format!(
        "Внимание, искатель приключений {} уровня! Твоя миссия: {}. \
        Это {} испытание проверит твои навыки и дарует ценный опыт по завершении. \
        Подготовься к эпическому путешествию продуктивности!",
        user_level,
        task_text,
        match difficulty {
            1 => "простое",
            2 => "умеренное", 
            3 => "сложное",
            4 => "тяжёлое",
            5 => "легендарное",
            _ => "таинственное"
        }
    );

    let story_context = format!(
        "В великом приключении жизни каждая выполненная задача приближает тебя на шаг к овладению своей судьбой. \
        Это конкретное испытание создано богами продуктивности, чтобы помочь тебе стать сильнее."
    );

    (enhanced_title, enhanced_description, story_context)
}

fn extract_task_essence(text: &str) -> String {
    let words: Vec<&str> = text.split_whitespace().take(3).collect();
    words.join(" ").to_title_case()
}

fn calculate_base_experience(difficulty: i32, user_level: i32) -> i32 {
    let base = match difficulty {
        1 => 10,
        2 => 25,
        3 => 50,
        4 => 100,
        5 => 200,
        _ => 50,
    };
    
    // Scale with user level
    base + (user_level * 5)
}

fn calculate_task_difficulty(task_text: &str, user_level: i32) -> i32 {
    let mut difficulty: i32 = 2; // Default
    
    // Simple heuristics based on text analysis
    let words = task_text.split_whitespace().count();
    let complexity_keywords = [
        "complex", "difficult", "challenging", "hard", "advanced", "expert",
        "сложн", "трудн", "тяжёл", "тяжел", "продвинут", "эксперт"
    ];
    let simple_keywords = [
        "simple", "easy", "quick", "basic", "straightforward",
        "прост", "лёгк", "легк", "быстр", "базов"
    ];
    
    if words < 3 {
        difficulty = 1;
    } else if words > 10 {
        difficulty = 3;
    }
    
    for keyword in complexity_keywords.iter() {
        if task_text.to_lowercase().contains(keyword) {
            difficulty += 1;
            break;
        }
    }
    
    for keyword in simple_keywords.iter() {
        if task_text.to_lowercase().contains(keyword) {
            difficulty = difficulty.saturating_sub(1);
            break;
        }
    }
    
    // Adjust for user level
    if user_level > 10 {
        difficulty = (difficulty + 1).min(5);
    }
    
    difficulty.clamp(1, 5)
}

fn generate_tags_for_quest(todo_text: &str, theme: &str) -> Vec<String> {
    let mut tags = vec![theme.to_string(), "generated".to_string()];
    
    let text_lower = todo_text.to_lowercase();
    
    // Поддержка русского и английского
    if text_lower.contains("work") || text_lower.contains("job") || text_lower.contains("office") 
        || text_lower.contains("работ") || text_lower.contains("проект") || text_lower.contains("офис") {
        tags.push("работа".to_string());
    }
    if text_lower.contains("study") || text_lower.contains("learn") || text_lower.contains("read") 
        || text_lower.contains("учи") || text_lower.contains("изучи") || text_lower.contains("прочита") || text_lower.contains("курс") {
        tags.push("обучение".to_string());
    }
    if text_lower.contains("exercise") || text_lower.contains("gym") || text_lower.contains("health") 
        || text_lower.contains("трениро") || text_lower.contains("спорт") || text_lower.contains("здоров") {
        tags.push("здоровье".to_string());
    }
    if text_lower.contains("clean") || text_lower.contains("organize") || text_lower.contains("tidy") 
        || text_lower.contains("убор") || text_lower.contains("чист") || text_lower.contains("дом") {
        tags.push("дом".to_string());
    }
    if text_lower.contains("магазин") || text_lower.contains("купи") || text_lower.contains("shop") || text_lower.contains("buy") {
        tags.push("покупки".to_string());
    }
    if text_lower.contains("готов") || text_lower.contains("пригото") || text_lower.contains("cook") {
        tags.push("готовка".to_string());
    }
    
    // ML-тренировочные метки
    if is_boss_task(todo_text) {
        tags.push("босс".to_string());
    }
    let est_diff = calculate_task_difficulty(todo_text, 1);
    tags.push(format!("сложность:{}", est_diff));

    tags
}

fn generate_tags_for_task(task_text: &str) -> Vec<String> {
    generate_tags_for_quest(task_text, "enhanced")
}

// Экспорт вспомогательных функций для подготовки датасетов
pub fn auto_tags_for_text(text: &str) -> Vec<String> { generate_tags_for_task(text) }
pub fn auto_difficulty_for_text(text: &str) -> i32 { calculate_task_difficulty(text, 1) }
pub fn is_boss_marker(text: &str) -> bool { is_boss_task(text) }

fn hash_string(s: &str) -> usize {
    let mut hash = 0usize;
    for byte in s.bytes() {
        hash = hash.wrapping_mul(31).wrapping_add(byte as usize);
    }
    hash
}

trait ToTitleCase {
    fn to_title_case(&self) -> String;
}

impl ToTitleCase for str {
    fn to_title_case(&self) -> String {
        self.split_whitespace()
            .map(|word| {
                let mut chars = word.chars();
                match chars.next() {
                    None => String::new(),
                    Some(first) => first.to_uppercase().collect::<String>() + &chars.as_str().to_lowercase(),
                }
            })
            .collect::<Vec<_>>()
            .join(" ")
    }
}

fn detect_theme(todo_text: &str) -> String {
    let text = todo_text.to_lowercase();
    if text.contains("экзамен") || text.contains("зачет") || text.contains("лекция") || text.contains("курс") || text.contains("study") {
        return "modern".to_string();
    }
    if text.contains("данные") || text.contains("api") || text.contains("deploy") || text.contains("cloud") || text.contains("project") {
        return "sci-fi".to_string();
    }
    if text.contains("уборк") || text.contains("дом") || text.contains("покупк") || text.contains("домашн") {
        return "modern".to_string();
    }
    "fantasy".to_string()
}

fn is_boss_task(text: &str) -> bool {
    let t = text.to_lowercase();
    let boss_markers = [
        "дедлайн", "deadline", "экзамен", "зачет", "защита", "презентация", "release", "релиз", "собеседование",
    ];
    boss_markers.iter().any(|m| t.contains(m))
}