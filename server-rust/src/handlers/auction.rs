use std::cmp::min;

use axum::{
    extract::{Extension, State},
    Json,
};
use chrono::{Duration, Utc};
use serde::{Deserialize, Serialize};
use sqlx::Row;

use crate::{error::AppError, middleware::auth::CurrentUser, state::AppState};

const PG_UNDEFINED_TABLE: &str = "42P01";

#[derive(Debug, Clone, Serialize)]
pub struct AuctionItemResponse {
    pub id: i32,
    pub name: String,
    pub quality: String,
    pub price: i32,
    pub quantity: i32,
    pub seller: String,
    pub description: Option<String>,
    pub thumbnail_url: Option<String>,
    pub expires_at: Option<String>,
    pub created_at: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct CreateAuctionListingRequest {
    pub name: String,
    pub quality: String,
    pub price: i32,
    pub quantity: i32,
    pub description: Option<String>,
    pub thumbnail_url: Option<String>,
    pub duration_hours: Option<i64>,
}

#[derive(Debug, Deserialize)]
pub struct AuctionPurchaseRequest {
    pub listing_id: i32,
    pub quantity: i32,
}

#[derive(Debug, Serialize)]
pub struct AuctionPurchaseResult {
    pub success: bool,
    pub message: String,
    pub remaining_balance: Option<i32>,
}

fn map_db_error(err: sqlx::Error, feature: &str) -> AppError {
    if let sqlx::Error::Database(db_err) = &err {
        if let Some(code) = db_err.code() {
            if code == PG_UNDEFINED_TABLE {
                return AppError::NotImplemented(format!(
                    "{feature} недоступен: примените актуальные миграции сервера (sqlx migrate run)"
                ));
            }
        }
        return AppError::DatabaseError(db_err.message().to_string());
    }
    AppError::Database(err)
}

fn normalize_quality(quality: &str) -> Result<String, AppError> {
    let q = quality.trim().to_lowercase();
    let normalized = match q.as_str() {
        "common" | "обычный" => "common",
        "uncommon" | "необычный" => "uncommon",
        "rare" | "редкий" => "rare",
        "epic" | "эпический" => "epic",
        "legendary" | "легендарный" => "legendary",
        other => {
            return Err(AppError::Validation(format!(
                "Неизвестное качество предмета: {other}"
            )))
        }
    };
    Ok(normalized.to_string())
}

pub async fn get_auction_listings(
    State(state): State<AppState>,
    Extension(_current_user): Extension<Option<CurrentUser>>,
) -> Result<Json<Vec<AuctionItemResponse>>, AppError> {
    let rows = sqlx::query(
        r#"
        SELECT
            l.id,
            l.item_name,
            l.item_description,
            l.quality::text as quality,
            l.price,
            l.quantity,
            l.created_at,
            l.expires_at,
            u.username as seller
        FROM auction_listings l
        JOIN users u ON u.id = l.seller_id
        WHERE l.expires_at > NOW()
        ORDER BY l.created_at DESC
        LIMIT 50
        "#,
    )
    .fetch_all(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Аукцион"))?;

    let items = rows
        .into_iter()
        .map(|row| {
            let id: i32 = row.get("id");
            let name: String = row.get("item_name");
            let quality: String = row.get("quality");
            let price: i32 = row.get("price");
            let quantity: i32 = row.get("quantity");
            let seller: String = row.get("seller");
            let description: Option<String> = row
                .try_get::<Option<String>, _>("item_description")
                .unwrap_or(None);
            let expires_at: chrono::DateTime<Utc> = row.get("expires_at");
            let created_at: chrono::DateTime<Utc> = row.get("created_at");

            AuctionItemResponse {
                id,
                name,
                quality,
                price,
                quantity,
                seller,
                description,
                thumbnail_url: None,
                expires_at: Some(expires_at.to_rfc3339()),
                created_at: Some(created_at.to_rfc3339()),
            }
        })
        .collect();

    Ok(Json(items))
}

pub async fn create_auction_listing(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<CreateAuctionListingRequest>,
) -> Result<Json<AuctionItemResponse>, AppError> {
    let user = current_user.ok_or_else(|| {
        AppError::Unauthorized("Требуется авторизация для размещения лота".into())
    })?;

    if req.name.trim().is_empty() {
        return Err(AppError::Validation(
            "Название предмета не может быть пустым".into(),
        ));
    }
    if req.price <= 0 {
        return Err(AppError::Validation(
            "Цена должна быть положительной".into(),
        ));
    }
    if req.quantity <= 0 {
        return Err(AppError::Validation(
            "Количество должно быть положительным".into(),
        ));
    }

    let CreateAuctionListingRequest {
        name,
        quality,
        price,
        quantity,
        description,
        thumbnail_url,
        duration_hours,
    } = req;

    let quality = normalize_quality(&quality)?;
    let duration_hours = duration_hours.unwrap_or(24).clamp(1, 168);
    let expires_at = Utc::now() + Duration::hours(duration_hours);

    let row = sqlx::query(
        r#"
        INSERT INTO auction_listings (
            seller_id,
            item_name,
            item_description,
            quality,
            price,
            quantity,
            expires_at
        )
        VALUES ($1, $2, $3, $4::item_quality, $5, $6, $7)
        RETURNING
            id,
            item_name,
            item_description,
            quality::text as quality,
            price,
            quantity,
            created_at,
            expires_at
        "#,
    )
    .bind(user.0.id)
    .bind(&name)
    .bind(&description)
    .bind(&quality)
    .bind(price)
    .bind(quantity)
    .bind(expires_at)
    .fetch_one(&state.db)
    .await
    .map_err(|err| map_db_error(err, "Аукцион"))?;

    let id: i32 = row.get("id");
    let stored_name: String = row.get("item_name");
    let stored_quality: String = row.get("quality");
    let stored_price: i32 = row.get("price");
    let stored_quantity: i32 = row.get("quantity");
    let stored_description: Option<String> = row
        .try_get::<Option<String>, _>("item_description")
        .unwrap_or(None);
    let stored_created_at: chrono::DateTime<Utc> = row.get("created_at");
    let stored_expires_at: chrono::DateTime<Utc> = row.get("expires_at");

    Ok(Json(AuctionItemResponse {
        id,
        name: stored_name,
        quality: stored_quality,
        price: stored_price,
        quantity: stored_quantity,
        seller: user.0.username.clone(),
        description: stored_description,
        thumbnail_url,
        expires_at: Some(stored_expires_at.to_rfc3339()),
        created_at: Some(stored_created_at.to_rfc3339()),
    }))
}

pub async fn purchase_auction_listing(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(req): Json<AuctionPurchaseRequest>,
) -> Result<Json<AuctionPurchaseResult>, AppError> {
    let user = current_user
        .ok_or_else(|| AppError::Unauthorized("Требуется авторизация для покупки лота".into()))?;

    if req.quantity <= 0 {
        return Err(AppError::Validation(
            "Количество должно быть положительным".into(),
        ));
    }

    let mut tx = state
        .db
        .begin()
        .await
        .map_err(|err| map_db_error(err, "Аукцион"))?;

    let listing_row = sqlx::query(
        r#"
        SELECT
            id,
            seller_id,
            item_name,
            item_description,
            quality::text as "quality!",
            price,
            quantity,
            expires_at
        FROM auction_listings
        WHERE id = $1
        FOR UPDATE
        "#,
    )
    .bind(req.listing_id)
    .fetch_optional(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Аукцион"))?
    .ok_or_else(|| AppError::NotFound("Лот не найден".into()))?;

    let listing_id: i32 = listing_row.get("id");
    let listing_seller_id: i32 = listing_row.get("seller_id");
    let listing_item_name: String = listing_row.get("item_name");
    let listing_quality: String = listing_row.get("quality");
    let listing_price: i32 = listing_row.get("price");
    let listing_quantity: i32 = listing_row.get("quantity");
    let listing_expires_at: chrono::DateTime<Utc> = listing_row.get("expires_at");

    if listing_seller_id == user.0.id {
        return Err(AppError::BadRequest("Нельзя купить собственный лот".into()));
    }

    if listing_expires_at < Utc::now() {
        return Err(AppError::BadRequest(
            "Лот уже истёк и недоступен для покупки".into(),
        ));
    }

    let quantity_to_buy = min(listing_quantity, req.quantity);
    if quantity_to_buy <= 0 {
        return Err(AppError::BadRequest(
            "Лот уже недоступен для покупки".into(),
        ));
    }

    let total_price = listing_price * quantity_to_buy;

    let buyer_row = sqlx::query(
        r#"
        SELECT gold
        FROM users
        WHERE id = $1
        FOR UPDATE
        "#,
    )
    .bind(user.0.id)
    .fetch_one(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Аукцион"))?;

    let buyer_gold: i32 = buyer_row.get("gold");

    if buyer_gold < total_price {
        return Err(AppError::BadRequest(
            "Недостаточно золота для покупки".into(),
        ));
    }

    sqlx::query(
        r#"
        UPDATE users
        SET gold = gold - $1
        WHERE id = $2
        "#,
    )
    .bind(total_price)
    .bind(user.0.id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Аукцион"))?;

    sqlx::query(
        r#"
        UPDATE users
        SET gold = gold + $1
        WHERE id = $2
        "#,
    )
    .bind(total_price)
    .bind(listing_seller_id)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Аукцион"))?;

    if listing_quantity == quantity_to_buy {
        sqlx::query(
            r#"
            DELETE FROM auction_listings
            WHERE id = $1
            "#,
        )
        .bind(listing_id)
        .execute(&mut *tx)
        .await
        .map_err(|err| map_db_error(err, "Аукцион"))?;
    } else {
        sqlx::query(
            r#"
            UPDATE auction_listings
            SET quantity = quantity - $1
            WHERE id = $2
            "#,
        )
        .bind(quantity_to_buy)
        .bind(listing_id)
        .execute(&mut *tx)
        .await
        .map_err(|err| map_db_error(err, "Аукцион"))?;
    }

    sqlx::query(
        r#"
        INSERT INTO auction_purchases (
            listing_id,
            buyer_id,
            seller_id,
            item_name,
            quality,
            price,
            quantity
        )
        VALUES ($1, $2, $3, $4, $5::item_quality, $6, $7)
        "#,
    )
    .bind(listing_id)
    .bind(user.0.id)
    .bind(listing_seller_id)
    .bind(listing_item_name.clone())
    .bind(&listing_quality)
    .bind(listing_price)
    .bind(quantity_to_buy)
    .execute(&mut *tx)
    .await
    .map_err(|err| map_db_error(err, "Аукцион"))?;

    let remaining = buyer_gold - total_price;

    tx.commit()
        .await
        .map_err(|err| map_db_error(err, "Аукцион"))?;

    Ok(Json(AuctionPurchaseResult {
        success: true,
        message: format!(
            "Вы приобрели {} × {} за {} золота",
            quantity_to_buy, listing_item_name, total_price
        ),
        remaining_balance: Some(remaining),
    }))
}
