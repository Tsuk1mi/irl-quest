use chrono::{DateTime, Utc};
/// Модели для геолокации и AR квестов
use serde::{Deserialize, Serialize};

/// Геолокация (широта, долгота)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Location {
    pub latitude: f64,
    pub longitude: f64,
}

impl Location {
    /// Рассчитать расстояние между точками (формула Haversine, в метрах)
    pub fn distance_to(&self, other: &Location) -> f64 {
        let r = 6371000.0; // Радиус Земли в метрах
        let lat1 = self.latitude.to_radians();
        let lat2 = other.latitude.to_radians();
        let delta_lat = (other.latitude - self.latitude).to_radians();
        let delta_lon = (other.longitude - self.longitude).to_radians();

        let a = (delta_lat / 2.0).sin().powi(2)
            + lat1.cos() * lat2.cos() * (delta_lon / 2.0).sin().powi(2);
        let c = 2.0 * a.sqrt().atan2((1.0 - a).sqrt());

        r * c
    }

    /// Проверить, находится ли точка в радиусе
    pub fn is_within_radius(&self, other: &Location, radius_meters: f64) -> bool {
        self.distance_to(other) <= radius_meters
    }
}

/// Геозона для квестов
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GeoZone {
    pub id: i32,
    pub name: String,
    pub center: Location,
    pub radius_meters: f64,
    pub zone_type: GeoZoneType,
    pub created_at: DateTime<Utc>,
}

/// Тип геозоны
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum GeoZoneType {
    Home,   // Дом
    Work,   // Работа
    Gym,    // Спортзал
    Shop,   // Магазин
    Park,   // Парк
    Custom, // Пользовательская
}

/// Гео-триггер для квестов
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GeoTrigger {
    pub id: i32,
    pub quest_id: i32,
    pub zone_id: i32,
    pub trigger_type: TriggerType,
    pub is_active: bool,
    pub triggered_count: u32,
    pub created_at: DateTime<Utc>,
}

/// Тип триггера
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum TriggerType {
    OnEnter,     // При входе в зону
    OnExit,      // При выходе из зоны
    WhileInside, // Пока внутри зоны
}

/// Запрос на создание геозоны
#[derive(Debug, Deserialize)]
pub struct CreateGeoZoneRequest {
    pub name: String,
    pub latitude: f64,
    pub longitude: f64,
    pub radius_meters: f64,
    pub zone_type: GeoZoneType,
}

/// Запрос на проверку геолокации
#[derive(Debug, Deserialize)]
pub struct CheckLocationRequest {
    pub latitude: f64,
    pub longitude: f64,
    pub quest_id: Option<i32>,
}

/// Ответ с информацией о триггерах
#[derive(Debug, Serialize)]
pub struct LocationCheckResponse {
    pub in_zones: Vec<GeoZoneInfo>,
    pub triggered_quests: Vec<i32>,
    pub ar_markers: Vec<ARMarker>,
}

#[derive(Debug, Serialize)]
pub struct GeoZoneInfo {
    pub zone_id: i32,
    pub zone_name: String,
    pub zone_type: GeoZoneType,
    pub distance_meters: f64,
}

/// AR маркер на карте
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ARMarker {
    pub id: i32,
    pub name: String,
    pub description: Option<String>,
    pub location: Location,
    pub marker_type: ARMarkerType,
    pub quest_id: Option<i32>,
    pub is_collected: bool,
}

/// Тип AR маркера
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum ARMarkerType {
    Artifact, // Артефакт
    Chest,    // Сундук
    Npc,      // NPC персонаж
    Waypoint, // Точка интереса
}

/// Изображение для верификации
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImageVerification {
    pub id: i32,
    pub quest_id: i32,
    pub user_id: i32,
    pub image_hash: String, // SHA256 hash для проверки
    pub verification_status: VerificationStatus,
    pub ai_confidence: Option<f32>,
    pub ai_detected_objects: Option<Vec<String>>,
    pub moderator_id: Option<i32>,
    pub created_at: DateTime<Utc>,
    pub verified_at: Option<DateTime<Utc>>,
    pub auto_delete_at: DateTime<Utc>, // TTL для автоудаления
}

/// Статус верификации
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum VerificationStatus {
    Pending,     // Ожидает проверки
    AiApproved,  // Одобрено ИИ (высокий confidence)
    AiRejected,  // Отклонено ИИ
    NeedsReview, // Требует ручной модерации (низкий confidence)
    Approved,    // Одобрено модератором
    Rejected,    // Отклонено модератором
}

/// Запрос на загрузку изображения для верификации
#[derive(Debug, Deserialize)]
pub struct UploadImageRequest {
    pub quest_id: i32,
    pub image_data: String, // Base64 encoded image
    pub latitude: Option<f64>,
    pub longitude: Option<f64>,
}

/// Ответ с результатами верификации изображения
#[derive(Debug, Serialize)]
pub struct ImageVerificationResponse {
    pub verification_id: i32,
    pub status: VerificationStatus,
    pub ai_confidence: f32,
    pub detected_objects: Vec<DetectedObject>,
    pub requires_review: bool,
    pub auto_delete_at: DateTime<Utc>,
}

/// Обнаруженный объект на изображении
#[derive(Debug, Serialize)]
pub struct DetectedObject {
    pub label: String,
    pub confidence: f32,
    pub bounding_box: Option<BoundingBox>,
}

/// Bounding box для обнаруженного объекта
#[derive(Debug, Serialize, Deserialize)]
pub struct BoundingBox {
    pub x: f32,
    pub y: f32,
    pub width: f32,
    pub height: f32,
}

/// Метаданные обработки изображения (хранится после удаления изображения)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImageProcessingMetadata {
    pub id: i32,
    pub quest_id: i32,
    pub user_id: i32,
    pub image_hash: String,
    pub processed_at: DateTime<Utc>,
    pub verification_result: VerificationStatus,
    pub ai_confidence: Option<f32>,
    pub location: Option<Location>,
}
