use anyhow::Result;
use chrono::Utc;
use sqlx::PgPool;

use crate::models::{Quest, QuestCreate, QuestOut, QuestUpdate};

pub struct QuestService;

impl QuestService {
    pub async fn list_quests_for_user(
        pool: &PgPool,
        user_id: i32,
        skip: i64,
        limit: i64,
    ) -> Result<Vec<QuestOut>> {
        let quests = sqlx::query_as!(
            Quest,
            "SELECT * FROM quests WHERE owner_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3",
            user_id,
            limit,
            skip
        )
        .fetch_all(pool)
        .await?;

        Ok(quests.into_iter().map(QuestOut::from).collect())
    }

    pub async fn create_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_create: QuestCreate,
    ) -> Result<QuestOut> {
        let quest = sqlx::query_as!(
            Quest,
            r#"
            INSERT INTO quests (title, description, difficulty, created_at, owner_id)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING id, title, description, difficulty, created_at, owner_id
            "#,
            quest_create.title,
            quest_create.description,
            quest_create.difficulty.unwrap_or(1),
            Utc::now(),
            user_id
        )
        .fetch_one(pool)
        .await?;

        Ok(QuestOut::from(quest))
    }

    pub async fn get_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_id: i32,
    ) -> Result<Option<QuestOut>> {
        let quest = sqlx::query_as!(
            Quest,
            "SELECT * FROM quests WHERE id = $1 AND owner_id = $2",
            quest_id,
            user_id
        )
        .fetch_optional(pool)
        .await?;

        Ok(quest.map(QuestOut::from))
    }

    pub async fn update_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_id: i32,
        quest_update: QuestUpdate,
    ) -> Result<Option<QuestOut>> {
        // First check if quest exists and belongs to user
        let existing_quest = sqlx::query_as!(
            Quest,
            "SELECT * FROM quests WHERE id = $1 AND owner_id = $2",
            quest_id,
            user_id
        )
        .fetch_optional(pool)
        .await?;

        let mut quest = match existing_quest {
            Some(quest) => quest,
            None => return Ok(None),
        };

        // Update fields if provided
        if let Some(title) = quest_update.title {
            quest.title = title;
        }
        if let Some(description) = quest_update.description {
            quest.description = description;
        }
        if let Some(difficulty) = quest_update.difficulty {
            quest.difficulty = difficulty;
        }

        // Save updated quest
        let updated_quest = sqlx::query_as!(
            Quest,
            r#"
            UPDATE quests 
            SET title = $1, description = $2, difficulty = $3
            WHERE id = $4 AND owner_id = $5
            RETURNING id, title, description, difficulty, created_at, owner_id
            "#,
            quest.title,
            quest.description,
            quest.difficulty,
            quest_id,
            user_id
        )
        .fetch_one(pool)
        .await?;

        Ok(Some(QuestOut::from(updated_quest)))
    }

    pub async fn delete_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_id: i32,
    ) -> Result<bool> {
        let result = sqlx::query!(
            "DELETE FROM quests WHERE id = $1 AND owner_id = $2",
            quest_id,
            user_id
        )
        .execute(pool)
        .await?;

        Ok(result.rows_affected() > 0)
    }
}