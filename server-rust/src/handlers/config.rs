use axum::{
    extract::State,
    http::StatusCode,
    Json,
};
use crate::config::ClientConfig;
use crate::state::AppState;

/// GET /api/config - Получить клиентскую конфигурацию
/// 
/// Возвращает конфигурацию сервера для мобильного клиента
/// (без секретных данных, только публичные настройки)
pub async fn get_client_config(
    State(state): State<AppState>,
) -> Result<Json<ClientConfig>, StatusCode> {
    let config = &state.config;
    
    // Формируем URL сервера
    let server_url = if let Some(public_ip) = &config.public_ip {
        format!("http://{}:{}", public_ip, config.port)
    } else if let Some(local_ip) = &config.local_ip {
        format!("http://{}:{}", local_ip, config.port)
    } else {
        format!("http://localhost:{}", config.port)
    };
    
    let client_config = config.to_client_config(server_url);
    
    tracing::info!("Client config requested: {:?}", client_config);
    
    Ok(Json(client_config))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::Config;
    use crate::state::AppState;
    use sqlx::postgres::PgPoolOptions;

    #[tokio::test]
    #[ignore] // Требует запущенную PostgreSQL базу для теста
    async fn test_get_client_config() {
        // Для юнит-тестов лучше использовать моки или testcontainers
        let pool = PgPoolOptions::new()
            .max_connections(1)
            .connect("postgres://postgres:tsukimi@localhost:5432/irl_quest")
            .await
            .unwrap();
            
        let state = AppState::new_with_config(
            pool,
            Config {
                database_url: "".to_string(),
                port: 8003,
                jwt_secret: "secret".to_string(),
                jwt_expiration_hours: 24,
                refresh_token_expiration_days: 30,
                cors_origin: "*".to_string(),
                ml_base_url: "http://localhost:8080".to_string(),
                ml_model_path: None,
                ml_infer_cmd: None,
                ml_embed_cmd: None,
                rate_limit_per_minute: 60,
                rate_limit_burst: 10,
                enable_mfa: false,
                password_min_length: 8,
                enable_oauth: false,
                enable_image_processing: false,
                image_retention_minutes: 5,
                enable_ar: false,
                enable_multiplayer: false,
                public_ip: Some("1.2.3.4".to_string()),
                local_ip: Some("192.168.1.1".to_string()),
                client_config_endpoint: true,
            },
        );

        let result = get_client_config(State(state)).await;
        assert!(result.is_ok());
        
        let config = result.unwrap().0;
        assert_eq!(config.api_version, "2.1.0");
        assert!(config.server_url.contains("1.2.3.4"));
    }
}

