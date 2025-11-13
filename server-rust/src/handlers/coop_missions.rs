use axum::{
    extract::{Extension, Path, State},
    Json,
};
use chrono::{DateTime, Utc};
use serde::Serialize;
use sqlx::{postgres::PgRow, Pool, Postgres, Row};

use crate::{
    error::AppError,
    middleware::auth::CurrentUser,
    models::multiplayer::{CoopMission, CreateCoopMissionRequest, JoinMissionRequest},
    state::AppState,
};

const PG_UNDEFINED_TABLE: &str = "42P01";

#[derive(Debug, Serialize)]
pub struct MissionQuestSummary {
    pub id: i32,
    pub title: Option<String>,
    pub difficulty: Option<i32>,
    pub reward_experience: Option<i32>,
    pub location_name: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct PartyMemberResponse {
    pub id: i32,
    pub mission_id: i32,
    pub user_id: i32,
    pub role: String,
    pub contribution: i32,
    pub joined_at: DateTime<Utc>,
    pub username: Option<String>,
    pub level: Option<i32>,
    pub character_class: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct CoopMissionResponse {
    pub id: i32,
    pub quest_id: i32,
    pub party_size: i32,
    pub max_party_size: i32,
    pub leader_id: i32,
    pub status: String,
    pub is_public: bool,
    pub created_at: DateTime<Utc>,
    pub quest: Option<MissionQuestSummary>,
    pub members: Vec<PartyMemberResponse>,
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

fn row_to_mission(row: &PgRow) -> CoopMission {
    CoopMission {
        id: row.get("id"),
        quest_id: row.get("quest_id"),
        party_size: row.get::<i32, _>("party_size"),
        max_party_size: row.get::<i32, _>("max_party_size"),
        leader_id: row.get("leader_id"),
        status: crate::models::multiplayer::MissionStatus::from(row.get::<String, _>("status")),
        is_public: row.get("is_public"),
        created_at: row.get("created_at"),
    }
}

fn row_to_member(row: &PgRow) -> PartyMemberResponse {
    PartyMemberResponse {
        id: row.get("id"),
        mission_id: row.get("mission_id"),
        user_id: row.get("user_id"),
        role: row.get::<String, _>("role"),
        contribution: row.get::<i32, _>("contribution"),
        joined_at: row.get("joined_at"),
        username: row.try_get::<String, _>("username").ok(),
        level: row.try_get::<i32, _>("level").ok(),
        character_class: row.try_get::<String, _>("character_class").ok(),
    }
}

impl From<String> for crate::models::multiplayer::MissionStatus {
    fn from(value: String) -> Self {
        match value.as_str() {
            "in_progress" => Self::InProgress,
            "completed" => Self::Completed,
            "failed" => Self::Failed,
            _ => Self::Recruiting,
        }
    }
}

impl From<&crate::models::multiplayer::MissionStatus> for String {
    fn from(status: &crate::models::multiplayer::MissionStatus) -> Self {
        match status {
            crate::models::multiplayer::MissionStatus::Recruiting => "recruiting",
            crate::models::multiplayer::MissionStatus::InProgress => "in_progress",
            crate::models::multiplayer::MissionStatus::Completed => "completed",
            crate::models::multiplayer::MissionStatus::Failed => "failed",
        }
        .to_string()
    }
}

impl From<String> for crate::models::multiplayer::PartyRole {
    fn from(value: String) -> Self {
        match value.as_str() {
            "tank" => Self::Tank,
            "dps" => Self::Dps,
            "healer" => Self::Healer,
            _ => Self::Support,
        }
    }
}

impl From<&crate::models::multiplayer::PartyRole> for String {
    fn from(role: &crate::models::multiplayer::PartyRole) -> Self {
        match role {
            crate::models::multiplayer::PartyRole::Tank => "tank",
            crate::models::multiplayer::PartyRole::Dps => "dps",
            crate::models::multiplayer::PartyRole::Healer => "healer",
            crate::models::multiplayer::PartyRole::Support => "support",
        }
        .to_string()
    }
}

async fn fetch_mission_members(
    pool: &Pool<Postgres>,
    mission_id: i32,
) -> Result<Vec<PartyMemberResponse>, AppError> {
    let rows = sqlx::query(
        r#"
        SELECT
            pm.id,
            pm.mission_id,
            pm.user_id,
            pm.role,
            pm.contribution,
            pm.joined_at,
            u.username,
            u.level,
            u.character_class
        FROM party_members pm
        JOIN users u ON u.id = pm.user_id
        WHERE pm.mission_id = $1
        ORDER BY pm.joined_at ASC
        "#,
    )
    .bind(mission_id)
    .fetch_all(pool)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    Ok(rows.iter().map(row_to_member).collect())
}

async fn mission_response_from_row(
    pool: &Pool<Postgres>,
    row: PgRow,
) -> Result<CoopMissionResponse, AppError> {
    let mission = row_to_mission(&row);
    let quest_summary = MissionQuestSummary {
        id: mission.quest_id,
        title: row.try_get::<String, _>("quest_title").ok(),
        difficulty: row.try_get::<i32, _>("quest_difficulty").ok(),
        reward_experience: row.try_get::<i32, _>("quest_reward_experience").ok(),
        location_name: row.try_get::<String, _>("quest_location").ok(),
    };

    let quest = if quest_summary.title.is_none()
        && quest_summary.difficulty.is_none()
        && quest_summary.reward_experience.is_none()
        && quest_summary.location_name.is_none()
    {
        None
    } else {
        Some(quest_summary)
    };

    let members = fetch_mission_members(pool, mission.id).await?;

    Ok(CoopMissionResponse {
        id: mission.id,
        quest_id: mission.quest_id,
        party_size: mission.party_size,
        max_party_size: mission.max_party_size,
        leader_id: mission.leader_id,
        status: String::from(&mission.status),
        is_public: mission.is_public,
        created_at: mission.created_at,
        quest,
        members,
    })
}

async fn load_coop_mission(
    pool: &Pool<Postgres>,
    mission_id: i32,
) -> Result<CoopMissionResponse, AppError> {
    let row = sqlx::query(
        r#"
        SELECT
            cm.id,
            cm.quest_id,
            cm.party_size,
            cm.max_party_size,
            cm.leader_id,
            cm.status,
            cm.is_public,
            cm.created_at,
            q.title as quest_title,
            q.difficulty as quest_difficulty,
            q.reward_experience as quest_reward_experience,
            q.location_name as quest_location
        FROM coop_missions cm
        LEFT JOIN quests q ON q.id = cm.quest_id
        WHERE cm.id = $1
        "#,
    )
    .bind(mission_id)
    .fetch_optional(pool)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?
    .ok_or_else(|| AppError::NotFound("Миссия не найдена".into()))?;

    mission_response_from_row(pool, row).await
}

pub async fn list_coop_missions(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<Vec<CoopMissionResponse>>, AppError> {
    let rows = sqlx::query(
        r#"
        SELECT
            cm.id,
            cm.quest_id,
            cm.party_size,
            cm.max_party_size,
            cm.leader_id,
            cm.status,
            cm.is_public,
            cm.created_at,
            q.title as quest_title,
            q.difficulty as quest_difficulty,
            q.reward_experience as quest_reward_experience,
            q.location_name as quest_location
        FROM coop_missions cm
        LEFT JOIN quests q ON q.id = cm.quest_id
        ORDER BY cm.created_at DESC
        LIMIT 50
        "#,
    )
    .fetch_all(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    let mut missions = Vec::with_capacity(rows.len());
    for row in rows {
        missions.push(mission_response_from_row(&state.db, row).await?);
    }

    Ok(Json(missions))
}

pub async fn create_coop_mission(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<CreateCoopMissionRequest>,
) -> Result<Json<CoopMissionResponse>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для создания кооп-миссии".into())
    })?;

    if req.max_party_size < 2 {
        return Err(AppError::Validation(
            "Размер группы должен быть не менее 2".into(),
        ));
    }

    let mut tx = state
        .db
        .begin()
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    let row = sqlx::query(
        r#"
        INSERT INTO coop_missions (quest_id, party_size, max_party_size, leader_id, status, is_public)
        VALUES ($1, 1, $2, $3, 'recruiting', $4)
        RETURNING
            id,
            quest_id,
            party_size,
            max_party_size,
            leader_id,
            status,
            is_public,
            created_at,
            NULL::text as quest_title,
            NULL::integer as quest_difficulty,
            NULL::integer as quest_reward_experience,
            NULL::text as quest_location
        "#,
    )
    .bind(req.quest_id)
    .bind(req.max_party_size)
    .bind(user.0.id)
    .bind(req.is_public)
    .fetch_one(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    sqlx::query(
        r#"
        INSERT INTO party_members (mission_id, user_id, role, contribution)
        VALUES ($1, $2, 'tank', 0)
        "#,
    )
    .bind(row.get::<i32, _>("id"))
    .bind(user.0.id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    let mission_id: i32 = row.get("id");

    tx.commit()
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    let mission = load_coop_mission(&state.db, mission_id).await?;

    Ok(Json(mission))
}

pub async fn get_coop_mission(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
    Path(mission_id): Path<i32>,
) -> Result<Json<CoopMissionResponse>, AppError> {
    let mission = load_coop_mission(&state.db, mission_id).await?;
    Ok(Json(mission))
}

pub async fn join_coop_mission(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<JoinMissionRequest>,
) -> Result<Json<CoopMissionResponse>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для участия в миссии".into())
    })?;

    let mut tx = state
        .db
        .begin()
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    let mission_row = sqlx::query(
        r#"
        SELECT id, max_party_size, party_size, status
        FROM coop_missions
        WHERE id = $1
        FOR UPDATE
        "#,
    )
    .bind(req.mission_id)
    .fetch_optional(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?
    .ok_or_else(|| AppError::NotFound("Миссия не найдена".into()))?;

    let mission_max_party: i32 = mission_row.get("max_party_size");
    let mission_party_size: i32 = mission_row.get("party_size");
    let mission_status: String = mission_row.get("status");

    if mission_status == "completed" || mission_status == "failed" {
        return Err(AppError::BadRequest(
            "Нельзя присоединиться к завершённой миссии".into(),
        ));
    }

    if mission_party_size >= mission_max_party {
        return Err(AppError::BadRequest(
            "Группа для миссии уже заполнена".into(),
        ));
    }

    let existing = sqlx::query(
        r#"
        SELECT 1
        FROM party_members
        WHERE mission_id = $1 AND user_id = $2
        "#,
    )
    .bind(req.mission_id)
    .bind(user.0.id)
    .fetch_optional(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?
    .is_some();

    if !existing {
        let role = String::from(&req.preferred_role);

        sqlx::query(
            r#"
            INSERT INTO party_members (mission_id, user_id, role, contribution)
            VALUES ($1, $2, $3, 0)
            ON CONFLICT (mission_id, user_id) DO NOTHING
            "#,
        )
        .bind(req.mission_id)
        .bind(user.0.id)
        .bind(role)
        .execute(&mut *tx)
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

        sqlx::query(
            r#"
            UPDATE coop_missions
            SET party_size = party_size + 1
            WHERE id = $1
            "#,
        )
        .bind(req.mission_id)
        .execute(&mut *tx)
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;
    }

    tx.commit()
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    let mission = load_coop_mission(&state.db, req.mission_id).await?;

    Ok(Json(mission))
}

pub async fn leave_coop_mission(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Path(mission_id): Path<i32>,
) -> Result<(), AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для выхода из миссии".into())
    })?;

    let mut tx = state
        .db
        .begin()
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    let mission = sqlx::query(
        r#"
        SELECT leader_id
        FROM coop_missions
        WHERE id = $1
        "#,
    )
    .bind(mission_id)
    .fetch_optional(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?
    .ok_or_else(|| AppError::NotFound("Миссия не найдена".into()))?;

    if mission.get::<i32, _>("leader_id") == user.0.id {
        return Err(AppError::BadRequest(
            "Лидер не может покинуть миссию. Передайте лидерство другому участнику.".into(),
        ));
    }

    let deleted = sqlx::query(
        r#"
        DELETE FROM party_members
        WHERE mission_id = $1 AND user_id = $2
        "#,
    )
    .bind(mission_id)
    .bind(user.0.id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    if deleted.rows_affected() == 0 {
        return Err(AppError::NotFound("Вы не участвуете в этой миссии".into()));
    }

    sqlx::query(
        r#"
        UPDATE coop_missions
        SET party_size = GREATEST(party_size - 1, 0)
        WHERE id = $1
        "#,
    )
    .bind(mission_id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    tx.commit()
        .await
        .map_err(|err| map_db_error(err, "Кооп-миссии"))?;

    Ok(())
}
