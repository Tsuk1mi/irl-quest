use axum::{
    extract::{Extension, Path, State},
    Json,
};
use chrono::{DateTime, Utc};
use serde::Serialize;
use sqlx::{postgres::PgRow, Postgres, Row, Transaction};

use crate::{
    error::AppError,
    middleware::auth::CurrentUser,
    models::multiplayer::{CreateGuildRequest, Guild, GuildMember, GuildRole},
    state::AppState,
};

const PG_UNDEFINED_TABLE: &str = "42P01";

#[derive(Debug, Serialize)]
pub struct GuildMemberExtended {
    pub id: i32,
    pub guild_id: i32,
    pub user_id: i32,
    pub role: String,
    pub joined_at: DateTime<Utc>,
    pub username: Option<String>,
    pub level: Option<i32>,
}

fn map_db_error(err: sqlx::Error, feature: &str) -> AppError {
    if let sqlx::Error::Database(db_err) = &err {
        if let Some(code) = db_err.code() {
            if code == PG_UNDEFINED_TABLE {
                return AppError::NotImplemented(format!(
                    "{feature} недоступны: примените актуальные миграции сервера (sqlx migrate run)"
                ));
            }
        }
        return AppError::DatabaseError(db_err.message().to_string());
    }
    AppError::Database(err)
}

fn row_to_guild(row: &PgRow) -> Guild {
    Guild {
        id: row.get("id"),
        name: row.get("name"),
        description: row.get("description"),
        leader_id: row.get("leader_id"),
        level: row.get::<i32, _>("level"),
        experience: row.get::<i32, _>("experience"),
        member_count: row.get::<i32, _>("member_count"),
        max_members: row.get::<i32, _>("max_members"),
        created_at: row.get("created_at"),
    }
}

fn row_to_member(row: &PgRow) -> GuildMemberExtended {
    GuildMemberExtended {
        id: row.get("id"),
        guild_id: row.get("guild_id"),
        user_id: row.get("user_id"),
        role: row.get::<String, _>("role"),
        joined_at: row.get("joined_at"),
        username: row.try_get::<String, _>("username").ok(),
        level: row.try_get::<i32, _>("level").ok(),
    }
}

async fn ensure_guild_capacity(
    tx: &mut Transaction<'_, Postgres>,
    guild_id: i32,
) -> Result<(), AppError> {
    let record = sqlx::query(
        r#"
        SELECT max_members, member_count
        FROM guilds
        WHERE id = $1
        "#,
    )
    .bind(guild_id)
    .fetch_optional(&mut **tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?
    .ok_or_else(|| AppError::NotFound("Гильдия не найдена".into()))?;

    let max_members: i32 = record.get("max_members");
    let member_count: i32 = record.get("member_count");

    if member_count >= max_members {
        return Err(AppError::BadRequest(
            "Лимит участников гильдии исчерпан".into(),
        ));
    }
    Ok(())
}

fn parse_guild_role(role: &str) -> GuildRole {
    match role {
        "leader" => GuildRole::Leader,
        "officer" => GuildRole::Officer,
        _ => GuildRole::Member,
    }
}

pub async fn list_guilds(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<Vec<Guild>>, AppError> {
    let rows = sqlx::query(
        r#"
        SELECT id, name, description, leader_id, level, experience, member_count, max_members, created_at
        FROM guilds
        ORDER BY level DESC, experience DESC, created_at ASC
        LIMIT 100
        "#,
    )
    .fetch_all(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    let guilds = rows.iter().map(row_to_guild).collect();
    Ok(Json(guilds))
}

pub async fn create_guild(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<CreateGuildRequest>,
) -> Result<Json<Guild>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для создания гильдии".into())
    })?;

    if req.name.trim().is_empty() {
        return Err(AppError::Validation(
            "Название гильдии не может быть пустым".into(),
        ));
    }

    let max_members = req.max_members.unwrap_or(50).clamp(5, 200) as i32;

    let mut tx = state
        .db
        .begin()
        .await
        .map_err(|err| map_db_error(err, "Гильдии"))?;

    let guild_row = sqlx::query(
        r#"
        INSERT INTO guilds (name, description, leader_id, member_count, max_members)
        VALUES ($1, $2, $3, 1, $4)
        RETURNING id, name, description, leader_id, level, experience, member_count, max_members, created_at
        "#,
    )
    .bind(&req.name)
    .bind(&req.description)
    .bind(user.0.id)
    .bind(max_members)
    .fetch_one(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    let guild = row_to_guild(&guild_row);

    sqlx::query(
        r#"
        INSERT INTO guild_members (guild_id, user_id, role)
        VALUES ($1, $2, 'leader')
        ON CONFLICT (guild_id, user_id) DO NOTHING
        "#,
    )
    .bind(guild.id)
    .bind(user.0.id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    tx.commit()
        .await
        .map_err(|err| map_db_error(err, "Гильдии"))?;

    Ok(Json(guild))
}

pub async fn get_guild(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
    Path(guild_id): Path<i32>,
) -> Result<Json<Guild>, AppError> {
    let row = sqlx::query(
        r#"
        SELECT id, name, description, leader_id, level, experience, member_count, max_members, created_at
        FROM guilds
        WHERE id = $1
        "#,
    )
    .bind(guild_id)
    .fetch_optional(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?
    .ok_or_else(|| AppError::NotFound("Гильдия не найдена".into()))?;

    Ok(Json(row_to_guild(&row)))
}

pub async fn get_guild_members(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
    Path(guild_id): Path<i32>,
) -> Result<Json<Vec<GuildMemberExtended>>, AppError> {
    let rows = sqlx::query(
        r#"
        SELECT
            gm.id,
            gm.guild_id,
            gm.user_id,
            gm.role,
            gm.joined_at,
            u.username,
            u.level
        FROM guild_members gm
        JOIN users u ON u.id = gm.user_id
        WHERE gm.guild_id = $1
        ORDER BY gm.joined_at ASC
        "#,
    )
    .bind(guild_id)
    .fetch_all(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    let members = rows.iter().map(row_to_member).collect();
    Ok(Json(members))
}

pub async fn join_guild(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Path(guild_id): Path<i32>,
) -> Result<Json<GuildMember>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для вступления в гильдию".into())
    })?;

    let mut tx = state
        .db
        .begin()
        .await
        .map_err(|err| map_db_error(err, "Гильдии"))?;

    ensure_guild_capacity(&mut tx, guild_id).await?;

    let existing = sqlx::query(
        r#"
        SELECT id, guild_id, user_id, role, joined_at
        FROM guild_members
        WHERE guild_id = $1 AND user_id = $2
        "#,
    )
    .bind(guild_id)
    .bind(user.0.id)
    .fetch_optional(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    if let Some(member) = existing {
        let existing_member = GuildMember {
            id: member.get("id"),
            guild_id: member.get("guild_id"),
            user_id: member.get("user_id"),
            role: parse_guild_role(member.get::<String, _>("role").as_str()),
            joined_at: member.get("joined_at"),
        };
        tx.commit()
            .await
            .map_err(|err| map_db_error(err, "Гильдии"))?;
        return Ok(Json(existing_member));
    }

    let inserted = sqlx::query(
        r#"
        INSERT INTO guild_members (guild_id, user_id, role)
        VALUES ($1, $2, 'member')
        RETURNING id, guild_id, user_id, role, joined_at
        "#,
    )
    .bind(guild_id)
    .bind(user.0.id)
    .fetch_one(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    sqlx::query(
        r#"
        UPDATE guilds
        SET member_count = member_count + 1
        WHERE id = $1
        "#,
    )
    .bind(guild_id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    tx.commit()
        .await
        .map_err(|err| map_db_error(err, "Гильдии"))?;

    Ok(Json(GuildMember {
        id: inserted.get("id"),
        guild_id,
        user_id: user.0.id,
        role: GuildRole::Member,
        joined_at: inserted.get("joined_at"),
    }))
}

pub async fn leave_guild(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Path(guild_id): Path<i32>,
) -> Result<(), AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для выхода из гильдии".into())
    })?;

    let mut tx = state
        .db
        .begin()
        .await
        .map_err(|err| map_db_error(err, "Гильдии"))?;

    // Проверяем, является ли пользователь лидером
    let guild = sqlx::query(
        r#"
        SELECT leader_id
        FROM guilds
        WHERE id = $1
        "#,
    )
    .bind(guild_id)
    .fetch_optional(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?
    .ok_or_else(|| AppError::NotFound("Гильдия не найдена".into()))?;

    if guild.get::<i32, _>("leader_id") == user.0.id {
        return Err(AppError::BadRequest(
            "Лидер не может покинуть гильдию. Передайте лидерство другому игроку.".into(),
        ));
    }

    let deleted = sqlx::query(
        r#"
        DELETE FROM guild_members
        WHERE guild_id = $1 AND user_id = $2
        "#,
    )
    .bind(guild_id)
    .bind(user.0.id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    if deleted.rows_affected() == 0 {
        return Err(AppError::NotFound("Вы не состоите в этой гильдии".into()));
    }

    sqlx::query(
        r#"
        UPDATE guilds
        SET member_count = GREATEST(member_count - 1, 0)
        WHERE id = $1
        "#,
    )
    .bind(guild_id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Гильдии"))?;

    tx.commit()
        .await
        .map_err(|err| map_db_error(err, "Гильдии"))?;

    Ok(())
}
