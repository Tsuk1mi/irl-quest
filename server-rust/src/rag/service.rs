use anyhow::Result;
use sqlx::PgPool;

use crate::models::{
    QuestGenerationRequest, QuestGenerationResponse,
    TaskEnhancementRequest, TaskEnhancementResponse,
    RagKnowledge, RagKnowledgeCreate, RagKnowledgeOut,
    GeneratedTask,
};
use super::templates::QuestTemplates;
use crate::ml::MlClient;
use serde_json::Value;

pub struct RagService {
    pool: PgPool,
    ml_client: MlClient,
}

impl RagService {
    pub fn new(pool: PgPool, ml_client: MlClient) -> Self {
        Self { pool, ml_client }
    }

    pub async fn generate_quest_from_todo(
        &self,
        request: QuestGenerationRequest,
    ) -> Result<QuestGenerationResponse> {
        // First, try to call ML model if available
        let prompt = serde_json::json!({
            "instruction": "Generate a game-like quest from a todo item.",
            "todo_text": request.todo_text,
            "context": request.context,
            "difficulty_preference": request.difficulty_preference,
            "user_level": request.user_level,
            "notes": "Return JSON with fields: title, description, difficulty (int 1-5), reward_experience (int), reward_description, tags (array of strings), quest_type, tasks (array of {title,description,difficulty,experience_reward,estimated_duration,is_boss}), story_context (optional)"
        });

        if let Ok(llm_output) = self.ml_client.infer(prompt.to_string()).await {
            // Try to parse JSON from model output
            if let Ok(val) = serde_json::from_str::<Value>(&llm_output) {
                // Map JSON to QuestGenerationResponse, with safe fallbacks
                if let Some(obj) = val.as_object() {
                    let title = obj.get("title").and_then(|v| v.as_str()).unwrap_or("Generated Quest").to_string();
                    let description = obj.get("description").and_then(|v| v.as_str()).map(|s| s.to_string()).unwrap_or_default();
                    let difficulty = obj.get("difficulty").and_then(|v| v.as_i64()).unwrap_or(3) as i32;
                    let reward_experience = obj.get("reward_experience").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                    let reward_description = obj.get("reward_description").and_then(|v| v.as_str()).unwrap_or("").to_string();
                    let tags = obj.get("tags").and_then(|v| v.as_array()).map(|arr| arr.iter().filter_map(|x| x.as_str().map(|s| s.to_string())).collect()).unwrap_or_else(|| vec![]);
                    let quest_type = obj.get("quest_type").and_then(|v| v.as_str()).unwrap_or("personal").to_string();
                    let story_context = obj.get("story_context").and_then(|v| v.as_str()).map(|s| s.to_string());

                    // Parse tasks
                    let mut tasks: Vec<GeneratedTask> = Vec::new();
                    if let Some(arr) = obj.get("tasks").and_then(|v| v.as_array()) {
                        for item in arr {
                            if let Some(itobj) = item.as_object() {
                                let ttitle = itobj.get("title").and_then(|v| v.as_str()).unwrap_or("").to_string();
                                let tdesc = itobj.get("description").and_then(|v| v.as_str()).unwrap_or("").to_string();
                                let tdifficulty = itobj.get("difficulty").and_then(|v| v.as_i64()).unwrap_or(1) as i32;
                                let texp = itobj.get("experience_reward").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                                let tested = itobj.get("estimated_duration").and_then(|v| v.as_i64()).map(|v| v as i32);
                                let is_boss = itobj.get("is_boss").and_then(|v| v.as_bool()).unwrap_or(false);
                                tasks.push(GeneratedTask {
                                    title: ttitle,
                                    description: tdesc,
                                    difficulty: tdifficulty,
                                    experience_reward: texp,
                                    estimated_duration: tested,
                                    is_boss,
                                });
                            }
                        }
                    }

                    let quest_response = QuestGenerationResponse {
                        title,
                        description,
                        difficulty: difficulty.clamp(1,5),
                        reward_experience,
                        reward_description,
                        tags,
                        quest_type,
                        tasks,
                        story_context,
                    };
                    // Store request for analysis
                    self.store_generation_request(&request).await?;
                    return Ok(quest_response);
                }
            }
            tracing::warn!("LLM output couldn't be parsed as JSON, falling back to templates");
        }

        // Fallback: use template-based generation
        let difficulty = request.difficulty_preference.unwrap_or_else(|| {
            self.calculate_difficulty_from_text(&request.todo_text)
        });
        let user_level = request.user_level.unwrap_or(1);
        self.store_generation_request(&request).await?;
        let mut quest_response = QuestTemplates::generate_quest_from_todo(
            &request.todo_text,
            request.context.as_deref(),
            difficulty,
            user_level,
        );
        if let Some(tags) = &request.tags_override { quest_response.tags = tags.clone(); }
        Ok(quest_response)
    }

    pub async fn enhance_task(
        &self,
        request: TaskEnhancementRequest,
    ) -> Result<TaskEnhancementResponse> {
        // Try LLM-based enhancement
        let prompt = serde_json::json!({
            "instruction": "Enhance the given task text by producing enhanced title, description, suggested difficulty (int), suggested_experience (int), story_context (optional), suggested_tags (array)",
            "task_text": request.task_text,
            "context": request.context,
            "user_level": request.user_level,
        });

        if let Ok(llm_output) = self.ml_client.infer(prompt.to_string()).await {
            if let Ok(val) = serde_json::from_str::<Value>(&llm_output) {
                if let Some(obj) = val.as_object() {
                    let enhanced_title = obj.get("enhanced_title").and_then(|v| v.as_str()).unwrap_or("").to_string();
                    let enhanced_description = obj.get("enhanced_description").and_then(|v| v.as_str()).unwrap_or("").to_string();
                    let suggested_difficulty = obj.get("suggested_difficulty").and_then(|v| v.as_i64()).unwrap_or(1) as i32;
                    let suggested_experience = obj.get("suggested_experience").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                    let story_context = obj.get("story_context").and_then(|v| v.as_str()).map(|s| s.to_string());
                    let suggested_tags = obj.get("suggested_tags").and_then(|v| v.as_array()).map(|arr| arr.iter().filter_map(|x| x.as_str().map(|s| s.to_string())).collect()).unwrap_or_else(|| vec![]);

                    let resp = TaskEnhancementResponse {
                        enhanced_title,
                        enhanced_description,
                        suggested_difficulty: suggested_difficulty.clamp(1,5),
                        suggested_experience,
                        story_context,
                        suggested_tags,
                    };
                    self.store_enhancement_request(&request).await?;
                    return Ok(resp);
                }
            }
            tracing::warn!("LLM output couldn't be parsed for task enhancement, falling back to templates");
        }

        // Fallback: use template-based enhancement
        let user_level = request.user_level.unwrap_or(1);
        self.store_enhancement_request(&request).await?;
        let enhancement_response = QuestTemplates::enhance_task(
            &request.task_text,
            request.context.as_deref(),
            user_level,
        );
        Ok(enhancement_response)
    }

    /// New: classify task (tags + difficulty) and generate exam-style tasks
    pub async fn classify_task_and_generate_exam(
        &self,
        task_text: &str,
        context: Option<&str>,
        user_level: i32,
    ) -> Result<(Vec<String>, i32, Vec<GeneratedTask>)> {
        let prompt = serde_json::json!({
            "instruction": "For the provided task text, return JSON with fields: tags (array of short tag strings), estimated_difficulty (int 1-5), exam_tasks (array of tasks with title, description, difficulty, experience_reward, estimated_duration, is_boss).",
            "task_text": task_text,
            "context": context,
            "user_level": user_level,
        });

        if let Ok(llm_output) = self.ml_client.infer(prompt.to_string()).await {
            if let Ok(val) = serde_json::from_str::<Value>(&llm_output) {
                if let Some(obj) = val.as_object() {
                    let tags = obj.get("tags").and_then(|v| v.as_array()).map(|arr| arr.iter().filter_map(|x| x.as_str().map(|s| s.to_string())).collect()).unwrap_or_else(|| vec![]);
                    let estimated_difficulty = obj.get("estimated_difficulty").and_then(|v| v.as_i64()).unwrap_or(1) as i32;
                    let mut exam_tasks = Vec::new();
                    if let Some(arr) = obj.get("exam_tasks").and_then(|v| v.as_array()) {
                        for item in arr {
                            if let Some(itobj) = item.as_object() {
                                let ttitle = itobj.get("title").and_then(|v| v.as_str()).unwrap_or("").to_string();
                                let tdesc = itobj.get("description").and_then(|v| v.as_str()).unwrap_or("").to_string();
                                let tdifficulty = itobj.get("difficulty").and_then(|v| v.as_i64()).unwrap_or(1) as i32;
                                let texp = itobj.get("experience_reward").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                                let tested = itobj.get("estimated_duration").and_then(|v| v.as_i64()).map(|v| v as i32);
                                let is_boss = itobj.get("is_boss").and_then(|v| v.as_bool()).unwrap_or(false);
                                exam_tasks.push(GeneratedTask {
                                    title: ttitle,
                                    description: tdesc,
                                    difficulty: tdifficulty,
                                    experience_reward: texp,
                                    estimated_duration: tested,
                                    is_boss,
                                });
                            }
                        }
                    }
                    return Ok((tags, estimated_difficulty.clamp(1,5), exam_tasks));
                }
            }
            tracing::warn!("LLM output couldn't be parsed for classification, falling back to heuristics");
        }

        // Fallback heuristics
        let tags = super::templates::auto_tags_for_text(task_text);
        let difficulty = self.calculate_difficulty_from_text(task_text);
        // Create simple exam tasks (one or two short tasks)
        let exam_tasks = vec![
            GeneratedTask {
                title: format!("Practice: {} (step 1)", &task_text.chars().take(30).collect::<String>()),
                description: format!("Breakdown of task: {}", task_text),
                difficulty: difficulty,
                experience_reward: 0,
                estimated_duration: Some(10),
                is_boss: false,
            }
        ];
        Ok((tags, difficulty, exam_tasks))
    }

    async fn store_generation_request(&self, request: &QuestGenerationRequest) -> Result<()> {
        let metadata = serde_json::json!({
            "context": request.context,
            "difficulty_preference": request.difficulty_preference,

            "user_level": request.user_level,
            "type": "quest_generation"
        });

        sqlx::query(
            r#"
            INSERT INTO rag_knowledge (content, content_type, tags, metadata)
            VALUES ($1, $2, $3, $4)
            "#,
        )
        .bind(&request.todo_text)
        .bind("quest_generation_request")
        .bind(&vec!["request", "todo", "generation"])
        .bind(&metadata)
        .execute(&self.pool)
        .await?;

        Ok(())
    }

    async fn store_enhancement_request(&self, request: &TaskEnhancementRequest) -> Result<()> {
        let metadata = serde_json::json!({
            "context": request.context,
            "user_level": request.user_level,
            "type": "task_enhancement"
        });

        sqlx::query(
            r#"
            INSERT INTO rag_knowledge (content, content_type, tags, metadata)
            VALUES ($1, $2, $3, $4)
            "#,
        )
        .bind(&request.task_text)
        .bind("task_enhancement_request")
        .bind(&vec!["request", "task", "enhancement"])
        .bind(&metadata)
        .execute(&self.pool)
        .await?;

        Ok(())
    }

    fn calculate_difficulty_from_text(&self, text: &str) -> i32 {
        let words = text.split_whitespace().count();
        let complexity_indicators = ["project", "complete", "finish", "develop", "create", "build"];
        let simple_indicators = ["check", "call", "email", "buy", "read"];
        
        let mut difficulty: i32 = 2; // Default medium
        
        if words < 3 {
            difficulty = 1;
        } else if words > 10 {
            difficulty += 1;
        }
        
        let text_lower = text.to_lowercase();
        
        for indicator in complexity_indicators.iter() {
            if text_lower.contains(indicator) {
                difficulty += 1;
                break;
            }
        }
        
        for indicator in simple_indicators.iter() {
            if text_lower.contains(indicator) {
                difficulty = difficulty.saturating_sub(1);
                break;
            }
        }
        
        difficulty.clamp(1, 5)
    }

    // Добавленный метод: сохраняет знание и возвращает созданную запись
    pub async fn add_knowledge(&self, knowledge: RagKnowledgeCreate) -> Result<RagKnowledgeOut> {
        let tags = knowledge.tags.unwrap_or_else(|| vec![]);
        let metadata = knowledge.metadata.unwrap_or_else(|| serde_json::json!({}));

        let rec: RagKnowledge = sqlx::query_as::<_, RagKnowledge>(
            r#"
            INSERT INTO rag_knowledge (content, content_type, tags, metadata)
            VALUES ($1, $2, $3, $4)
            RETURNING id, content, content_type, tags, embedding, metadata, created_at
            "#,
        )
        .bind(&knowledge.content)
        .bind(&knowledge.content_type)
        .bind(&tags)
        .bind(&metadata)
        .fetch_one(&self.pool)
        .await?;

        Ok(RagKnowledgeOut::from(rec))
    }

    #[allow(dead_code)]
    pub async fn initialize_default_knowledge(&self) -> Result<()> {
        // Add some default quest templates and knowledge
        let default_knowledge = vec![
            RagKnowledgeCreate {
                content: "Fantasy quest templates for turning everyday tasks into epic adventures".to_string(),
                content_type: "template".to_string(),
                tags: Some(vec!["fantasy".to_string(), "template".to_string(), "quest".to_string()]),
                metadata: Some(serde_json::json!({
                    "category": "quest_template",
                    "theme": "fantasy"
                })),
            },
            RagKnowledgeCreate {
                content: "Sci-fi themed quest generation for futuristic task enhancement".to_string(),
                content_type: "template".to_string(),
                tags: Some(vec!["sci-fi".to_string(), "template".to_string(), "quest".to_string()]),
                metadata: Some(serde_json::json!({
                    "category": "quest_template", 
                    "theme": "sci-fi"
                })),
            },
            RagKnowledgeCreate {
                content: "Modern productivity themes for realistic task gamification".to_string(),
                content_type: "template".to_string(),
                tags: Some(vec!["modern".to_string(), "template".to_string(), "productivity".to_string()]),
                metadata: Some(serde_json::json!({
                    "category": "quest_template",
                    "theme": "modern"
                })),
            },
        ];

        for knowledge in default_knowledge {
            // Check if already exists
            let existing: Option<(i32,)> = sqlx::query_as(
                "SELECT id FROM rag_knowledge WHERE content = $1"
            )
            .bind(&knowledge.content)
            .fetch_optional(&self.pool)
            .await?;

            if existing.is_none() {
                self.add_knowledge(knowledge).await?;
            }
        }

        Ok(())
    }
}