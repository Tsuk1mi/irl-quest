use crate::models::{GeneratedTask, QuestGenerationResponse, TaskEnhancementResponse};

pub struct QuestTemplates;

impl QuestTemplates {
    pub fn generate_quest_from_todo(
        todo_text: &str,
        context: Option<&str>,
        difficulty: i32,
        user_level: i32,
    ) -> QuestGenerationResponse {
        // Авто-детекция темы по содержимому TODO
        let theme = detect_theme(todo_text);
        let (title, description, story_context) = match theme.as_str() {
            "fantasy" => generate_fantasy_quest(todo_text, difficulty, user_level),
            "sci-fi" => generate_scifi_quest(todo_text, difficulty, user_level),
            "modern" => generate_modern_quest(todo_text, difficulty, user_level),
            "medieval" => generate_medieval_quest(todo_text, difficulty, user_level),
            _ => generate_modern_quest(todo_text, difficulty, user_level),
        };


        let base_exp = calculate_base_experience(difficulty, user_level);
        let tasks = generate_quest_tasks(todo_text, difficulty, base_exp);
        let tags = generate_tags_for_quest(todo_text, &theme);

        QuestGenerationResponse {
            title,
            description,
            difficulty,
            reward_experience: base_exp * 3,
            reward_description: format!("Complete this {} adventure to earn {} experience points and unlock new abilities!", 
                theme, base_exp * 3),
            tags,
            quest_type: "generated".to_string(),
            tasks,
            story_context: Some(story_context),
        }
    }

    pub fn enhance_task(
        task_text: &str,
        context: Option<&str>,
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
        "The Sacred Mission of {}",
        "Quest for the {} Artifact", 
        "The {} Chronicle",
        "Legend of the {} Hero",
        "The {} Prophecy"
    ];
    
    let task_essence = extract_task_essence(todo_text);
    let title_template = quest_titles[hash_string(todo_text) % quest_titles.len()];
    let title = title_template.replace("{}", &task_essence);
    
    let description = format!(
        "In the mystical realm of productivity, a great challenge awaits. The ancient scrolls speak of {}. \
        Only a hero of your caliber (Level {}) can undertake this {} difficulty quest. \
        The kingdom depends on your success, brave adventurer!",
        todo_text.to_lowercase(),
        user_level,
        match difficulty {
            1 => "trivial",
            2 => "easy", 
            3 => "moderate",
            4 => "hard",
            5 => "legendary",
            _ => "unknown"
        }
    );

    let story_context = format!(
        "The Council of Elders has bestowed upon you this sacred mission. Your actions will echo through the halls of history. \
        Complete this quest to gain favor with the magical forces and unlock new powers in your journey of self-improvement."
    );

    (title, description, story_context)
}

fn generate_scifi_quest(todo_text: &str, difficulty: i32, user_level: i32) -> (String, String, String) {
    let quest_titles = [
        "Mission: {}",
        "Protocol {}", 
        "Operation {}",
        "The {} Directive",
        "Project: {}"
    ];
    
    let task_essence = extract_task_essence(todo_text);
    let title_template = quest_titles[hash_string(todo_text) % quest_titles.len()];
    let title = title_template.replace("{}", &task_essence);
    
    let description = format!(
        "Stardate 2024.{}: Commander, your mission parameters are clear. The task '{}' is classified as Priority Level {}. \
        Your current rank (Level {}) qualifies you for this operation. The future of the galaxy may depend on its completion.",
        hash_string(todo_text) % 365 + 1,
        todo_text,
        difficulty,
        user_level
    );

    let story_context = format!(
        "The Galactic Council has transmitted this critical mission to your personal datapad. \
        Success will advance your standing in the Space Fleet and unlock advanced technologies for future missions."
    );

    (title, description, story_context)
}

fn generate_modern_quest(todo_text: &str, difficulty: i32, user_level: i32) -> (String, String, String) {
    let quest_titles = [
        "The {} Challenge",
        "Project: {}", 
        "{} Goals",
        "The {} Initiative",
        "Mission: {}"
    ];
    
    let task_essence = extract_task_essence(todo_text);
    let title_template = quest_titles[hash_string(todo_text) % quest_titles.len()];
    let title = title_template.replace("{}", &task_essence);
    
    let description = format!(
        "Welcome to your personal development journey! Today's challenge: '{}'. \
        This is a Level {} difficulty task, perfect for someone at your current stage (Level {}). \
        Complete this to boost your productivity score and unlock new achievements!",
        todo_text,
        difficulty,
        user_level
    );

    let story_context = format!(
        "You're part of an elite group of productivity ninjas. Each completed task brings you closer to \
        mastering the art of getting things done and achieving your life goals."
    );

    (title, description, story_context)
}

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
            title: format!("Complete: {}", todo_text),
            description: format!("Execute the main objective: {}", todo_text),
            difficulty,
            experience_reward: base_exp,
            estimated_duration: Some(30 * difficulty),
            is_boss: is_boss_task(todo_text),
        });
    } else {
        // Break down into subtasks
        tasks.push(GeneratedTask {
            title: "Preparation Phase".to_string(),
            description: format!("Gather resources and prepare for: {}", todo_text),
            difficulty: 1,
            experience_reward: base_exp / task_count,
            estimated_duration: Some(15),
            is_boss: false,
        });

        for i in 1..task_count-1 {
            tasks.push(GeneratedTask {
                title: format!("Execution Phase {}", i),
                description: format!("Progress on objective: {}", todo_text),
                difficulty: difficulty - 1,
                experience_reward: base_exp / task_count,
                estimated_duration: Some(20 * difficulty),
                is_boss: false,
            });
        }

        tasks.push(GeneratedTask {
            title: "Completion & Review".to_string(),
            description: format!("Finalize and verify: {}", todo_text),
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
    
    let enhanced_title = format!("Epic {}: {}", 
        match difficulty {
            1 => "Errand",
            2 => "Task", 
            3 => "Mission",
            4 => "Quest",
            5 => "Legendary Feat",
            _ => "Challenge"
        },
        task_essence
    );
    
    let enhanced_description = format!(
        "Behold, Level {} adventurer! Your mission: {}. \
        This {} challenge will test your skills and grant you valuable experience upon completion. \
        Prepare yourself for an epic journey of productivity!",
        user_level,
        task_text,
        match difficulty {
            1 => "simple",
            2 => "moderate", 
            3 => "challenging",
            4 => "formidable",
            5 => "legendary",
            _ => "mysterious"
        }
    );

    let story_context = format!(
        "In the grand adventure of life, every task completed brings you one step closer to mastering your destiny. \
        This particular challenge has been crafted by the gods of productivity to help you grow stronger."
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
    let mut difficulty = 2; // Default
    
    // Simple heuristics based on text analysis
    let words = task_text.split_whitespace().count();
    let complexity_keywords = ["complex", "difficult", "challenging", "hard", "advanced", "expert"];
    let simple_keywords = ["simple", "easy", "quick", "basic", "straightforward"];
    
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
    
    if text_lower.contains("work") || text_lower.contains("job") || text_lower.contains("office") {
        tags.push("work".to_string());
    }
    if text_lower.contains("study") || text_lower.contains("learn") || text_lower.contains("read") {
        tags.push("learning".to_string());
    }
    if text_lower.contains("exercise") || text_lower.contains("gym") || text_lower.contains("health") {
        tags.push("health".to_string());
    }
    if text_lower.contains("clean") || text_lower.contains("organize") || text_lower.contains("tidy") {
        tags.push("home".to_string());
    }
    
    // ML-тренировочные метки
    if is_boss_task(todo_text) {
        tags.push("boss".to_string());
    }
    let est_diff = calculate_task_difficulty(todo_text, 1);
    tags.push(format!("difficulty:{}", est_diff));

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