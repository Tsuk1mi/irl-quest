/// ML Inference Service - Алгоритмы определения тегов, сложности и трансформации
use crate::models::ml_inference::*;
use std::time::Instant;

pub struct MlInferenceService {
    config: MlConfig,
}

impl MlInferenceService {
    pub fn new(config: MlConfig) -> Self {
        Self { config }
    }

    /// Определение тегов из текста
    pub async fn predict_tags(&self, text: &str, max_tags: usize) -> TagsResponse {
        let start = Instant::now();
        
        let mut tags = Vec::new();
        let text_lower = text.to_lowercase();
        
        // Словарь тегов с ключевыми словами
        let tag_keywords = vec![
            ("работа", vec!["работа", "офис", "проект", "встреча", "дедлайн", "задача"]),
            ("учеба", vec!["учеба", "курс", "экзамен", "лекция", "домашка", "изучить"]),
            ("дом", vec!["дом", "уборка", "ремонт", "готовка", "стирка", "посуда"]),
            ("спорт", vec!["спорт", "тренировка", "зал", "бег", "фитнес", "йога"]),
            ("здоровье", vec!["врач", "лекарство", "анализы", "здоровье", "зубы", "аптека"]),
            ("покупки", vec!["купить", "магазин", "продукты", "заказ", "доставка"]),
            ("финансы", vec!["счет", "оплата", "банк", "деньги", "налоги", "платеж"]),
            ("социальное", vec!["встреча", "друзья", "звонок", "поздравить", "письмо"]),
            ("творчество", vec!["рисовать", "писать", "музыка", "фото", "творчество"]),
            ("развлечения", vec!["фильм", "игра", "книга", "хобби", "отдых"]),
        ];
        
        // Подсчет совпадений для каждого тега
        for (tag, keywords) in tag_keywords {
            let mut matches = 0;
            for keyword in keywords {
                if text_lower.contains(keyword) {
                    matches += 1;
                }
            }
            
            if matches > 0 {
                // Confidence на основе количества совпадений
                let confidence = (matches as f32 * 0.25).min(0.95);
                let requires_review = confidence < self.config.tags_confidence_threshold;
                
                tags.push(TagPrediction {
                    tag: tag.to_string(),
                    confidence,
                    requires_review,
                });
            }
        }
        
        // Сортировать по confidence
        tags.sort_by(|a, b| b.confidence.partial_cmp(&a.confidence).unwrap());
        tags.truncate(max_tags);
        
        // Если нет тегов, добавить "общее" с низкой уверенностью
        if tags.is_empty() {
            tags.push(TagPrediction {
                tag: "общее".to_string(),
                confidence: 0.3,
                requires_review: true,
            });
        }
        
        TagsResponse {
            tags,
            processing_time_ms: start.elapsed().as_millis() as u64,
        }
    }

    /// Оценка сложности задачи
    pub async fn predict_difficulty(
        &self,
        title: &str,
        description: Option<&str>,
    ) -> DifficultyResponse {
        let start = Instant::now();
        
        let full_text = format!(
            "{} {}",
            title,
            description.unwrap_or("")
        ).to_lowercase();
        
        let mut difficulty_score = 5.0; // Базовая сложность
        let mut factors = Vec::new();
        
        // Фактор 1: Длина текста
        let word_count = full_text.split_whitespace().count();
        if word_count > 20 {
            difficulty_score += 1.5;
            factors.push(DifficultyFactor {
                factor: "Длина описания".to_string(),
                impact: 0.3,
                explanation: "Длинное описание обычно означает более сложную задачу".to_string(),
            });
        }
        
        // Фактор 2: Ключевые слова сложности
        let complexity_words = vec![
            ("сложн", 2.0),
            ("трудн", 2.0),
            ("изучить", 1.5),
            ("разобраться", 1.5),
            ("проект", 1.5),
            ("написать", 1.0),
            ("создать", 1.0),
            ("разработать", 2.0),
            ("исследовать", 1.5),
        ];
        
        for (word, impact) in complexity_words {
            if full_text.contains(word) {
                difficulty_score += impact * 0.5;
                factors.push(DifficultyFactor {
                    factor: format!("Ключевое слово: '{}'", word),
                    impact: impact * 0.1,
                    explanation: "Указывает на повышенную сложность".to_string(),
                });
            }
        }
        
        // Фактор 3: Ключевые слова простоты
        let simplicity_words = vec!["просто", "быстро", "легко", "купить", "позвонить"];
        for word in simplicity_words {
            if full_text.contains(word) {
                difficulty_score -= 1.0;
                factors.push(DifficultyFactor {
                    factor: format!("Ключевое слово: '{}'", word),
                    impact: -0.2,
                    explanation: "Указывает на простоту задачи".to_string(),
                });
            }
        }
        
        // Фактор 4: Множественные этапы
        if full_text.contains(" и ") || full_text.contains(",") {
            let separator_count = full_text.matches(" и ").count() + full_text.matches(",").count();
            if separator_count > 2 {
                difficulty_score += separator_count as f32 * 0.5;
                factors.push(DifficultyFactor {
                    factor: "Множественные этапы".to_string(),
                    impact: 0.25,
                    explanation: format!("Обнаружено {} подзадач", separator_count),
                });
            }
        }
        
        // Ограничить диапазон 1-10
        let difficulty = difficulty_score.max(1.0).min(10.0).round() as u8;
        
        // Confidence на основе количества факторов
        let confidence = if factors.len() >= 3 {
            0.85
        } else if factors.len() >= 2 {
            0.70
        } else {
            0.55
        };
        
        let requires_review = confidence < self.config.difficulty_confidence_threshold;
        
        DifficultyResponse {
            difficulty,
            confidence,
            factors,
            requires_review,
            processing_time_ms: start.elapsed().as_millis() as u64,
        }
    }

    /// Трансформация ToDo в квест
    pub async fn transform_to_quest(
        &self,
        title: &str,
        description: Option<&str>,
        difficulty: Option<u8>,
        user_level: Option<u8>,
        style: Option<QuestStyle>,
    ) -> TransformResponse {
        let start = Instant::now();
        
        let style = style.unwrap_or(QuestStyle::Fantasy);
        let difficulty = difficulty.unwrap_or(5);
        let user_level = user_level.unwrap_or(1);
        
        // Генерация фэнтези названия и описания
        let (fantasy_title, fantasy_description) = match style {
            QuestStyle::Fantasy => self.generate_fantasy_quest(title, description, difficulty),
            QuestStyle::SciFi => self.generate_scifi_quest(title, description, difficulty),
            QuestStyle::Modern => self.generate_modern_quest(title, description, difficulty),
            QuestStyle::Horror => self.generate_horror_quest(title, description, difficulty),
            QuestStyle::Adventure => self.generate_adventure_quest(title, description, difficulty),
        };
        
        // Рассчитать награды на основе сложности и уровня
        let base_exp = difficulty as u32 * 10;
        let level_bonus = (user_level as u32).saturating_sub(1) * 5;
        let experience = base_exp + level_bonus;
        let gold = difficulty as u32 * 10;
        
        // Предложить предметы для высокой сложности
        let items = if difficulty >= 8 {
            vec!["Эпический сундук".to_string(), "Зелье опыта".to_string()]
        } else if difficulty >= 5 {
            vec!["Редкий артефакт".to_string()]
        } else {
            vec![]
        };
        
        let rewards = Rewards {
            experience,
            gold,
            items,
        };
        
        // Confidence выше для простых трансформаций
        let confidence = if title.len() > 10 && !title.chars().all(|c| c.is_ascii()) {
            0.85
        } else {
            0.70
        };
        
        let requires_review = confidence < self.config.transform_confidence_threshold;
        
        TransformResponse {
            fantasy_title,
            fantasy_description,
            suggested_rewards: rewards,
            suggested_difficulty: difficulty,
            confidence,
            requires_review,
            style_used: style,
            processing_time_ms: start.elapsed().as_millis() as u64,
        }
    }

    /// Персональные рекомендации квестов
    pub async fn get_recommendations(
        &self,
        user_id: i32,
        limit: usize,
    ) -> RecommendationsResponse {
        let start = Instant::now();
        
        // Здесь должна быть логика на основе истории пользователя
        // Пока генерируем базовые рекомендации
        
        let quests = vec![
            QuestRecommendation {
                title: "Ежедневная тренировка".to_string(),
                description: "Выполните утреннюю зарядку для поддержания формы".to_string(),
                difficulty: 2,
                estimated_time_minutes: 15,
                tags: vec!["спорт".to_string(), "здоровье".to_string()],
                score: 0.92,
                reasons: vec![
                    "Вы часто выполняете спортивные задачи".to_string(),
                    "Подходит для вашего уровня".to_string(),
                ],
            },
            QuestRecommendation {
                title: "Изучить новую технологию".to_string(),
                description: "Прочитайте документацию и создайте тестовый проект".to_string(),
                difficulty: 6,
                estimated_time_minutes: 120,
                tags: vec!["учеба".to_string(), "работа".to_string()],
                score: 0.85,
                reasons: vec![
                    "Соответствует вашим интересам".to_string(),
                    "Поможет в карьерном росте".to_string(),
                ],
            },
        ];
        
        RecommendationsResponse {
            quests: quests.into_iter().take(limit).collect(),
            reasoning: format!("Рекомендации на основе активности пользователя {}", user_id),
            processing_time_ms: start.elapsed().as_millis() as u64,
        }
    }

    // Вспомогательные функции для разных стилей

    fn generate_fantasy_quest(&self, title: &str, description: Option<&str>, difficulty: u8) -> (String, String) {
        let prefixes = vec![
            "🏰 Легендарный квест:",
            "⚔️ Эпическое задание:",
            "🗡️ Героическая миссия:",
            "🛡️ Испытание героя:",
            "⭐ Задание от гильдии:",
        ];
        
        let fantasy_title = format!(
            "{} {}",
            prefixes[difficulty as usize % prefixes.len()],
            title
        );
        
        let intro = match difficulty {
            1..=3 => "В мирной деревне требуется помощь.",
            4..=6 => "Древние руины хранят опасные тайны.",
            7..=9 => "Тёмные силы угрожают королевству.",
            _ => "Судьба мира висит на волоске.",
        };
        
        let fantasy_description = format!(
            "{} {}\n\nТребуется смелый герой для выполнения задания.",
            intro,
            description.unwrap_or(title)
        );
        
        (fantasy_title, fantasy_description)
    }

    fn generate_scifi_quest(&self, title: &str, _description: Option<&str>, _difficulty: u8) -> (String, String) {
        (
            format!("🚀 Космическая миссия: {}", title),
            "Космическая станция нуждается в вашей помощи.".to_string(),
        )
    }

    fn generate_modern_quest(&self, title: &str, _description: Option<&str>, _difficulty: u8) -> (String, String) {
        (
            format!("📱 Задача дня: {}", title),
            "Современная жизнь требует действий.".to_string(),
        )
    }

    fn generate_horror_quest(&self, title: &str, _description: Option<&str>, _difficulty: u8) -> (String, String) {
        (
            format!("🌙 Мрачное испытание: {}", title),
            "Тени сгущаются... Осмелишься ли ты?".to_string(),
        )
    }

    fn generate_adventure_quest(&self, title: &str, _description: Option<&str>, _difficulty: u8) -> (String, String) {
        (
            format!("🗺️ Приключение: {}", title),
            "Новые земли ждут исследователей!".to_string(),
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_predict_tags() {
        let service = MlInferenceService::new(MlConfig::default());
        let response = service.predict_tags("купить продукты в магазине", 5).await;
        
        assert!(!response.tags.is_empty());
        assert!(response.tags.iter().any(|t| t.tag == "покупки"));
    }

    #[tokio::test]
    async fn test_predict_difficulty() {
        let service = MlInferenceService::new(MlConfig::default());
        let response = service.predict_difficulty(
            "Разработать сложный проект",
            Some("Требует глубокого изучения и множество этапов"),
        ).await;
        
        assert!(response.difficulty >= 6);
        assert!(!response.factors.is_empty());
    }

    #[tokio::test]
    async fn test_transform_to_quest() {
        let service = MlInferenceService::new(MlConfig::default());
        let response = service.transform_to_quest(
            "Купить молоко",
            None,
            Some(2),
            Some(5),
            None,
        ).await;
        
        assert!(response.fantasy_title.contains("Купить молоко"));
        assert!(response.suggested_rewards.experience > 0);
    }
}


