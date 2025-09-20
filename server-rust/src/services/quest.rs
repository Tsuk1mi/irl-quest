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
        let quests: Vec<Quest> = sqlx::query_as::<_, Quest>(
            "SELECT * FROM quests WHERE owner_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3",
        )
        .bind(user_id)
        .bind(limit)
        .bind(skip)
        .fetch_all(pool)
        .await?;

        Ok(quests.into_iter().map(QuestOut::from).collect())
    }

    pub async fn create_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_create: QuestCreate,
    ) -> Result<QuestOut> {
        let quest: Quest = sqlx::query_as::<_, Quest>(
            r#"
            INSERT INTO quests (
                title, description, difficulty, status, priority, deadline,
                completion_percentage, reward_experience, reward_description, tags,
                is_public, location_name, quest_type, metadata, created_at, owner_id
            )
            VALUES (
                $1, $2, COALESCE($3,1), COALESCE($4,'active'), COALESCE($5,'medium'), $6,
                0, COALESCE($7,0), $8, COALESCE($9, ARRAY[]::TEXT[]),
                COALESCE($10,false), $11, COALESCE($12,'personal'), COALESCE($13, '{}'::jsonb), $14, $15
            )
            RETURNING *
            "#,
        )
        .bind(&quest_create.title)
        .bind(&quest_create.description)
        .bind(quest_create.difficulty)
        .bind(&quest_create.priority)
        .bind(&quest_create.priority)
        .bind(&quest_create.deadline)
        .bind(&quest_create.reward_experience)
        .bind(&quest_create.reward_description)
        .bind(&quest_create.tags)
        .bind(&quest_create.is_public)
        .bind(&quest_create.location_name)
        .bind(&quest_create.quest_type)
        .bind(&quest_create.metadata)
        .bind(Utc::now())
        .bind(user_id)
        .fetch_one(pool)
        .await?;

        Ok(QuestOut::from(quest))
    }

    pub async fn get_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_id: i32,
    ) -> Result<Option<QuestOut>> {
        let quest: Option<Quest> = sqlx::query_as::<_, Quest>(
            "SELECT * FROM quests WHERE id = $1 AND owner_id = $2",
        )
        .bind(quest_id)
        .bind(user_id)
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
        // Load existing
        let mut quest: Option<Quest> = sqlx::query_as::<_, Quest>(
            "SELECT * FROM quests WHERE id = $1 AND owner_id = $2",
        )
        .bind(quest_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        let mut quest = match quest { Some(q) => q, None => return Ok(None) };

        if let Some(title) = quest_update.title { quest.title = title; }
        if let Some(description) = quest_update.description { quest.description = Some(description); }
        if let Some(difficulty) = quest_update.difficulty { quest.difficulty = difficulty; }
        if let Some(status) = quest_update.status { quest.status = status; }
        if let Some(priority) = quest_update.priority { quest.priority = priority; }
        if let Some(deadline) = quest_update.deadline { quest.deadline = Some(deadline); }
        if let Some(reward_experience) = quest_update.reward_experience { quest.reward_experience = reward_experience; }
        if let Some(reward_description) = quest_update.reward_description { quest.reward_description = Some(reward_description); }
        if let Some(tags) = quest_update.tags { quest.tags = tags; }
        if let Some(is_public) = quest_update.is_public { quest.is_public = is_public; }
        if let Some(location_name) = quest_update.location_name { quest.location_name = Some(location_name); }
        if let Some(quest_type) = quest_update.quest_type { quest.quest_type = quest_type; }
        if let Some(metadata) = quest_update.metadata { quest.metadata = metadata; }

        let updated: Quest = sqlx::query_as::<_, Quest>(
            r#"
            UPDATE quests SET 
                title = $1, description = $2, difficulty = $3, status = $4, priority = $5,
                deadline = $6, reward_experience = $7, reward_description = $8,
                tags = $9, is_public = $10, location_name = $11, quest_type = $12, metadata = $13
            WHERE id = $14 AND owner_id = $15
            RETURNING *
            "#,
        )
        .bind(&quest.title)
        .bind(&quest.description)
        .bind(quest.difficulty)
        .bind(&quest.status)
        .bind(&quest.priority)
        .bind(&quest.deadline)
        .bind(quest.reward_experience)
        .bind(&quest.reward_description)
        .bind(&quest.tags)
        .bind(quest.is_public)
        .bind(&quest.location_name)
        .bind(&quest.quest_type)
        .bind(&quest.metadata)
        .bind(quest_id)
        .bind(user_id)
        .fetch_one(pool)
        .await?;

        Ok(Some(QuestOut::from(updated)))
    }

    pub async fn delete_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_id: i32,
    ) -> Result<bool> {
        let result = sqlx::query(
            "DELETE FROM quests WHERE id = $1 AND owner_id = $2",
        )
        .bind(quest_id)
        .bind(user_id)
        .execute(pool)
        .await?;

        Ok(result.rows_affected() > 0)
    }

    pub async fn complete_quest_for_user(
        pool: &PgPool,
        user_id: i32,
        quest_id: i32,
    ) -> Result<Option<QuestOut>> {
        let updated: Option<Quest> = sqlx::query_as::<_, Quest>(
            r#"
            UPDATE quests 
            SET status = 'completed', completion_percentage = 100
            WHERE id = $1 AND owner_id = $2
            RETURNING *
            "#,
        )
        .bind(quest_id)
        .bind(user_id)
        .fetch_optional(pool)
        .await?;

        Ok(updated.map(QuestOut::from))
    }
}
