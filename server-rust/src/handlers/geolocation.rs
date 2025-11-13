use crate::error::AppError;
use crate::middleware::auth::CurrentUser;
use crate::models::geolocation::*;
use crate::services::ImageProcessor;
use crate::state::AppState;
/// Handlers для геолокации и AR
use axum::{
    extract::{Extension, State},
    http::StatusCode,
    Json,
};
use sqlx::Row;

/// POST /api/geo/zones - Создать геозону
pub async fn create_geo_zone(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<CreateGeoZoneRequest>,
) -> Result<Json<GeoZone>, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    let zone_record = sqlx::query(
        r#"
        INSERT INTO geo_zones (user_id, name, latitude, longitude, radius_meters, zone_type)
        VALUES ($1, $2, $3, $4, $5, $6)
        RETURNING id, name, latitude, longitude, radius_meters, zone_type, created_at
        "#,
    )
    .bind(user.0.id)
    .bind(&request.name)
    .bind(request.latitude as f32)
    .bind(request.longitude as f32)
    .bind(request.radius_meters as f32)
    .bind(format!("{:?}", request.zone_type).to_lowercase())
    .fetch_one(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to create geo zone: {}", e)))?;

    let zone = GeoZone {
        id: zone_record.try_get("id").unwrap_or(0),
        name: zone_record.try_get("name").unwrap_or_default(),
        center: Location {
            latitude: zone_record.try_get::<f32, _>("latitude").unwrap_or(0.0) as f64,
            longitude: zone_record.try_get::<f32, _>("longitude").unwrap_or(0.0) as f64,
        },
        radius_meters: zone_record
            .try_get::<f32, _>("radius_meters")
            .unwrap_or(0.0) as f64,
        zone_type: request.zone_type,
        created_at: zone_record
            .try_get("created_at")
            .unwrap_or_else(|_| chrono::Utc::now()),
    };

    tracing::info!("Created geo zone {} for user {}", zone.id, user.0.id);

    Ok(Json(zone))
}

/// POST /api/geo/check - Проверить текущую геолокацию
pub async fn check_location(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<CheckLocationRequest>,
) -> Result<Json<LocationCheckResponse>, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    let current_location = Location {
        latitude: request.latitude,
        longitude: request.longitude,
    };

    // Получить все геозоны пользователя
    let zone_records = sqlx::query(
        "SELECT id, name, latitude, longitude, radius_meters, zone_type, created_at FROM geo_zones WHERE user_id = $1"
    )
    .bind(user.0.id)
    .fetch_all(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to fetch zones: {}", e)))?;

    // Проверить в каких зонах находится пользователь
    let mut in_zones = Vec::new();
    for record in zone_records {
        let latitude: f32 = record.try_get("latitude").unwrap_or(0.0);
        let longitude: f32 = record.try_get("longitude").unwrap_or(0.0);
        let radius: f32 = record.try_get("radius_meters").unwrap_or(0.0);

        let zone_location = Location {
            latitude: latitude as f64,
            longitude: longitude as f64,
        };

        let distance = current_location.distance_to(&zone_location);
        if distance <= radius as f64 {
            let zone_type_str: String = record.try_get("zone_type").unwrap_or_default();
            let zone_type = match zone_type_str.to_lowercase().as_str() {
                "home" => GeoZoneType::Home,
                "work" => GeoZoneType::Work,
                "gym" => GeoZoneType::Gym,
                "shop" => GeoZoneType::Shop,
                "park" => GeoZoneType::Park,
                _ => GeoZoneType::Custom,
            };

            in_zones.push(GeoZoneInfo {
                zone_id: record.try_get("id").unwrap_or(0),
                zone_name: record.try_get("name").unwrap_or_default(),
                zone_type,
                distance_meters: distance,
            });
        }
    }

    // Проверить триггеры (заглушка)
    let triggered_quests = vec![];

    // Получить близкие AR маркеры
    let ar_markers = vec![]; // TODO: реализовать поиск маркеров в радиусе

    tracing::info!(
        "Location check for user {}: {} zones active",
        user.0.id,
        in_zones.len()
    );

    Ok(Json(LocationCheckResponse {
        in_zones,
        triggered_quests,
        ar_markers,
    }))
}

/// POST /api/images/verify - Загрузить изображение для верификации
pub async fn upload_verification_image(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(request): Json<UploadImageRequest>,
) -> Result<Json<ImageVerificationResponse>, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    if !state.config.enable_image_processing {
        return Err(AppError::NotImplemented(
            "Image processing is disabled".to_string(),
        ));
    }

    // Проверить согласие на обработку изображений
    let consent = sqlx::query(
        r#"
        SELECT camera_consent
        FROM user_consents
        WHERE user_id = $1
        "#,
    )
    .bind(user.0.id)
    .fetch_optional(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to check consent: {}", e)))?;

    let camera_consent = consent
        .as_ref()
        .and_then(|r| r.try_get::<bool, _>("camera_consent").ok())
        .unwrap_or(false);

    if !camera_consent {
        return Err(AppError::Forbidden(
            "Camera consent required. Please accept privacy policy.".to_string(),
        ));
    }

    // Обработать изображение
    let image_processor = ImageProcessor::new(state.config.clone());
    let response = image_processor
        .process_image(&request.image_data, request.quest_id, user.0.id)
        .await
        .map_err(|e| AppError::InternalServerError(format!("Failed to process image: {}", e)))?;

    tracing::info!(
        "Image processed for quest {}: status={:?}, confidence={:.2}",
        request.quest_id,
        response.status,
        response.ai_confidence
    );

    Ok(Json(response))
}

/// POST /api/privacy/consent - Дать согласие на обработку данных
pub async fn give_consent(
    State(state): State<AppState>,
    Extension(current_user): Extension<Option<CurrentUser>>,
    Json(consent): Json<ConsentRequest>,
) -> Result<StatusCode, AppError> {
    let user = current_user.ok_or(AppError::Unauthorized("Not authenticated".to_string()))?;

    sqlx::query(
        r#"
        INSERT INTO user_consents (user_id, camera_consent, location_consent, data_processing_consent)
        VALUES ($1, $2, $3, $4)
        ON CONFLICT(user_id) DO UPDATE SET
            camera_consent = excluded.camera_consent,
            location_consent = excluded.location_consent,
            data_processing_consent = excluded.data_processing_consent,
            consent_date = CURRENT_TIMESTAMP
        "#
    )
    .bind(user.0.id)
    .bind(consent.camera_consent)
    .bind(consent.location_consent)
    .bind(consent.data_processing_consent)
    .execute(&state.db)
    .await
    .map_err(|e| AppError::DatabaseError(format!("Failed to save consent: {}", e)))?;

    tracing::info!("User {} updated privacy consents", user.0.id);

    Ok(StatusCode::OK)
}

#[derive(Debug, serde::Deserialize)]
pub struct ConsentRequest {
    camera_consent: bool,
    location_consent: bool,
    data_processing_consent: bool,
}
