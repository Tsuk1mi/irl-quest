use anyhow::Result;
use sqlx::PgPool;

use crate::models::{
    QuestGenerationRequest, QuestGenerationResponse, 
    TaskEnhancementRequest, TaskEnhancementResponse,
    RagKnowledge, RagKnowledgeCreate, RagKnowledgeOut
};
use super::templates::QuestTemplates;

pub struct RagService {
    pool: PgPool,
}

impl RagService {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    pub async fn generate_quest_from_todo(
        &self,
        request: QuestGenerationRequest,
    ) -> Result<QuestGenerationResponse> {
        // For MVP, we use template-based generation
        // In production, this would use actual LLM integration
        
        let difficulty = request.difficulty_preference.unwrap_or_else(|| {
            self.calculate_difficulty_from_text(&request.todo_text)
        });
        
        let user_level = request.user_level.unwrap_or(1);
        
        // Store the request for future training/analysis
        self.store_generation_request(&request).await?;
        
        let quest_response = QuestTemplates::generate_quest_from_todo(
            &request.todo_text,
            request.context.as_deref(),
            difficulty,
            request.theme_preference.as_deref(),
            user_level,
        );

        Ok(quest_response)
    }

    pub async fn enhance_task(
        &self,
        request: TaskEnhancementRequest,
    ) -> Result<TaskEnhancementResponse> {
        let user_level = request.user_level.unwrap_or(1);
        
        // Store the request for future analysis
        self.store_enhancement_request(&request).await?;
        
        let enhancement_response = QuestTemplates::enhance_task(
            &request.task_text,
            request.context.as_deref(),
            user_level,
        );

        Ok(enhancement_response)
    }

    pub async fn add_knowledge(
        &self,
        knowledge: RagKnowledgeCreate,
    ) -> Result<RagKnowledgeOut> {
        let record: RagKnowledge = sqlx::query_as(
            r#"
            INSERT INTO rag_knowledge (content, content_type, tags, metadata)
            VALUES ($1, $2, $3, $4)
            RETURNING id, content, content_type, tags, embedding, metadata, created_at
            "#,
        )
        .bind(&knowledge.content)
        .bind(&knowledge.content_type)
        .bind(knowledge.tags.as_deref().unwrap_or(&vec![]))
        .bind(knowledge.metadata.as_ref().unwrap_or(&serde_json::json!({})))
        .fetch_one(&self.pool)
        .await?;

        Ok(RagKnowledgeOut::from(record))
    }

    pub async fn search_knowledge(
        &self,
        query: &str,
        limit: Option<i64>,
    ) -> Result<Vec<RagKnowledgeOut>> {
        let limit = limit.unwrap_or(10);
        
        let records: Vec<RagKnowledge> = sqlx::query_as(
            r#"
            SELECT id, content, content_type, tags, embedding, metadata, created_at
            FROM rag_knowledge
            WHERE content ILIKE $1 OR $1 = ANY(tags)
            ORDER BY created_at DESC
            LIMIT $2
            "#,
        )
        .bind(format!("%{}%", query))
        .bind(limit)
        .fetch_all(&self.pool)
        .await?;

        Ok(records.into_iter().map(RagKnowledgeOut::from).collect())
    }

    async fn store_generation_request(&self, request: &QuestGenerationRequest) -> Result<()> {
        let metadata = serde_json::json!({
            "context": request.context,
            "difficulty_preference": request.difficulty_preference,
            "theme_preference": request.theme_preference,
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
        
        let mut difficulty = 2; // Default medium
        
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

    pub async fn initialize_default_knowledge(&self) -> Result<()> {
        // Add some default quest templates and knowledge
        let default_knowledge = vec![
            RagKnowledgeCreate {
                content: "Fantasy quest templates for turning everyday tasks into epic adventures".to_string(),
                content_type: "template".to_string(),
                tags: Some(vec!["fantasy", "template", "quest".to_string()]),
                metadata: Some(serde_json::json!({
                    "category": "quest_template",
                    "theme": "fantasy"
                })),
            },
            RagKnowledgeCreate {
                content: "Sci-fi themed quest generation for futuristic task enhancement".to_string(),
                content_type: "template".to_string(),
                tags: Some(vec!["sci-fi", "template", "quest".to_string()]),
                metadata: Some(serde_json::json!({
                    "category": "quest_template", 
                    "theme": "sci-fi"
                })),
            },
            RagKnowledgeCreate {
                content: "Modern productivity themes for realistic task gamification".to_string(),
                content_type: "template".to_string(),
                tags: Some(vec!["modern", "template", "productivity".to_string()]),
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