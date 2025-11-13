use sqlx::PgPool;

use crate::config::Config;
use crate::middleware::{IpBlocklist, RateLimiter};
use crate::ml::MlClient;
use crate::services::WebSocketManager;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub ml_client: MlClient,
    pub ml_base_url: String,
    pub ml_model_path: Option<String>,
    pub ml_infer_cmd: Option<String>,
    pub ml_embed_cmd: Option<String>,
    pub config: Config,
    pub rate_limiter: RateLimiter,
    pub ip_blacklist: IpBlocklist,
    pub ws_manager: WebSocketManager,
}

impl AppState {
    pub fn new(
        db: PgPool,
        ml_base_url: String,
        ml_model_path: Option<String>,
        ml_infer_cmd: Option<String>,
        ml_embed_cmd: Option<String>,
    ) -> Self {
        let ml_client = MlClient::new(
            ml_base_url.clone(),
            ml_model_path.clone(),
            ml_infer_cmd.clone(),
            ml_embed_cmd.clone(),
        );

        // Загружаем конфигурацию или создаем дефолтную
        let config = crate::config::load_config().unwrap_or_else(|_| Config {
            database_url: "postgres://postgres:tsukimi@localhost:5432/irl_quest".to_string(),
            port: 8003,
            jwt_secret: "change-me".to_string(),
            jwt_expiration_hours: 24,
            refresh_token_expiration_days: 30,
            cors_origin: "*".to_string(),
            ml_base_url: ml_base_url.clone(),
            ml_model_path: ml_model_path.clone(),
            ml_infer_cmd: ml_infer_cmd.clone(),
            ml_embed_cmd: ml_embed_cmd.clone(),
            rate_limit_per_minute: 60,
            rate_limit_burst: 10,
            enable_mfa: false,
            password_min_length: 8,
            enable_oauth: false,
            enable_image_processing: false,
            image_retention_minutes: 5,
            enable_ar: false,
            enable_multiplayer: false,
            public_ip: None,
            local_ip: None,
            client_config_endpoint: true,
        });

        let rate_limiter = RateLimiter::new(config.clone());
        let ip_blacklist = IpBlocklist::new();
        let ws_manager = WebSocketManager::new();

        Self {
            db,
            ml_client,
            ml_base_url,
            ml_model_path,
            ml_infer_cmd,
            ml_embed_cmd,
            config,
            rate_limiter,
            ip_blacklist,
            ws_manager,
        }
    }

    pub fn new_with_config(db: PgPool, config: Config) -> Self {
        let ml_client = MlClient::new(
            config.ml_base_url.clone(),
            config.ml_model_path.clone(),
            config.ml_infer_cmd.clone(),
            config.ml_embed_cmd.clone(),
        );

        // Создать rate limiter и IP blacklist
        let rate_limiter = RateLimiter::new(config.clone());
        let ip_blacklist = IpBlocklist::new();

        // Создать WebSocket manager
        let ws_manager = WebSocketManager::new();

        Self {
            db,
            ml_client,
            ml_base_url: config.ml_base_url.clone(),
            ml_model_path: config.ml_model_path.clone(),
            ml_infer_cmd: config.ml_infer_cmd.clone(),
            ml_embed_cmd: config.ml_embed_cmd.clone(),
            config,
            rate_limiter,
            ip_blacklist,
            ws_manager,
        }
    }
}
