/// Сервис обработки изображений с автоудалением (Privacy by Design)
use crate::config::Config;
use crate::models::geolocation::*;
use chrono::{Duration, Utc};
use sha2::{Digest, Sha256};
use std::path::PathBuf;
use tokio::fs;

pub struct ImageProcessor {
    config: Config,
    temp_storage_path: PathBuf,
}

impl ImageProcessor {
    pub fn new(config: Config) -> Self {
        let temp_storage_path = PathBuf::from("./temp_images");
        Self {
            config,
            temp_storage_path,
        }
    }

    /// Обработать изображение для верификации
    pub async fn process_image(
        &self,
        image_data: &str,  // Base64 encoded
        quest_id: i32,
        user_id: i32,
    ) -> Result<ImageVerificationResponse, Box<dyn std::error::Error>> {
        // Декодировать Base64
        use base64::Engine;
        let image_bytes = base64::engine::general_purpose::STANDARD.decode(image_data)?;

        // Рассчитать hash для идентификации (без хранения самого изображения)
        let mut hasher = Sha256::new();
        hasher.update(&image_bytes);
        let image_hash = format!("{:x}", hasher.finalize());

        // Временное сохранение для AI обработки
        let temp_file_path = self.temp_storage_path.join(format!("{}_{}.jpg", user_id, image_hash));
        fs::create_dir_all(&self.temp_storage_path).await?;
        fs::write(&temp_file_path, &image_bytes).await?;

        // AI верификация (object detection)
        let (detected_objects, ai_confidence) = self.verify_with_ai(&temp_file_path).await?;

        // Определить статус верификации
        let status = if ai_confidence > 0.85 {
            VerificationStatus::AiApproved
        } else if ai_confidence < 0.5 {
            VerificationStatus::AiRejected
        } else {
            VerificationStatus::NeedsReview  // Human-in-loop
        };

        let requires_review = status == VerificationStatus::NeedsReview;

        // Установить TTL для автоудаления
        let auto_delete_at = Utc::now() + Duration::minutes(self.config.image_retention_minutes as i64);

        // Запланировать удаление изображения
        let file_path_clone = temp_file_path.clone();
        let delete_at = auto_delete_at;
        tokio::spawn(async move {
            let wait_duration = (delete_at - Utc::now()).to_std().unwrap_or(std::time::Duration::from_secs(300));
            tokio::time::sleep(wait_duration).await;
            
            if let Err(e) = fs::remove_file(&file_path_clone).await {
                tracing::warn!("Failed to auto-delete image {}: {}", file_path_clone.display(), e);
            } else {
                tracing::info!("Auto-deleted image: {}", file_path_clone.display());
            }
        });

        // Вернуть только метаданные (изображение не сохраняем в БД)
        Ok(ImageVerificationResponse {
            verification_id: 0, // TODO: сохранить в БД
            status,
            ai_confidence,
            detected_objects,
            requires_review,
            auto_delete_at,
        })
    }

    /// AI верификация изображения (object detection)
    async fn verify_with_ai(
        &self,
        _image_path: &PathBuf,
    ) -> Result<(Vec<DetectedObject>, f32), Box<dyn std::error::Error>> {
        // Здесь должна быть интеграция с моделью object detection
        // Пока возвращаем моковые данные
        
        // TODO: Интеграция с:
        // - YOLO для object detection
        // - TensorFlow/PyTorch модель
        // - Cloud Vision API (Google, AWS Rekognition)

        let detected_objects = vec![
            DetectedObject {
                label: "person".to_string(),
                confidence: 0.92,
                bounding_box: Some(BoundingBox {
                    x: 100.0,
                    y: 100.0,
                    width: 200.0,
                    height: 300.0,
                }),
            },
        ];

        let ai_confidence = 0.92;

        Ok((detected_objects, ai_confidence))
    }

    /// Удалить изображение немедленно
    pub async fn delete_image(&self, image_hash: &str) -> Result<(), Box<dyn std::error::Error>> {
        // Найти файл по hash
        let pattern = format!("*_{}.jpg", image_hash);
        
        // Удалить все файлы с этим hash
        let mut entries = fs::read_dir(&self.temp_storage_path).await?;
        while let Some(entry) = entries.next_entry().await? {
            let path = entry.path();
            if let Some(filename) = path.file_name().and_then(|n| n.to_str()) {
                if filename.contains(image_hash) {
                    fs::remove_file(&path).await?;
                    tracing::info!("Deleted image: {}", path.display());
                }
            }
        }

        Ok(())
    }

    /// Очистить все истекшие изображения (периодическая задача)
    pub async fn cleanup_expired_images(&self) -> Result<u32, Box<dyn std::error::Error>> {
        let mut deleted_count = 0;
        let mut entries = fs::read_dir(&self.temp_storage_path).await?;

        while let Some(entry) = entries.next_entry().await? {
            let metadata = entry.metadata().await?;
            
            if let Ok(modified) = metadata.modified() {
                let age = std::time::SystemTime::now().duration_since(modified)?;
                let max_age = std::time::Duration::from_secs(self.config.image_retention_minutes * 60);

                if age > max_age {
                    fs::remove_file(entry.path()).await?;
                    deleted_count += 1;
                }
            }
        }

        if deleted_count > 0 {
            tracing::info!("Cleaned up {} expired images", deleted_count);
        }

        Ok(deleted_count)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_location_distance() {
        // Москва
        let moscow = Location {
            latitude: 55.7558,
            longitude: 37.6173,
        };

        // Санкт-Петербург
        let spb = Location {
            latitude: 59.9311,
            longitude: 30.3609,
        };

        let distance = moscow.distance_to(&spb);
        
        // Примерно 635 км
        assert!(distance > 600000.0 && distance < 650000.0);
    }

    #[test]
    fn test_within_radius() {
        let center = Location {
            latitude: 55.7558,
            longitude: 37.6173,
        };

        let nearby = Location {
            latitude: 55.7560,
            longitude: 37.6175,
        };

        assert!(center.is_within_radius(&nearby, 100.0));
    }
}

