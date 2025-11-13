use serde::{Deserialize, Serialize};
use std::env;
use std::net::IpAddr;

#[derive(Debug, Clone, Deserialize)]
pub struct Config {
    pub database_url: String,
    pub port: u16,
    pub jwt_secret: String,
    pub jwt_expiration_hours: u64,
    pub refresh_token_expiration_days: u64,
    pub cors_origin: String,
    pub ml_base_url: String,
    pub ml_model_path: Option<String>,
    pub ml_infer_cmd: Option<String>,
    pub ml_embed_cmd: Option<String>,

    // Security
    pub rate_limit_per_minute: u32,
    pub rate_limit_burst: u32,
    pub enable_mfa: bool,
    pub password_min_length: usize,

    // Features
    pub enable_oauth: bool,
    pub enable_image_processing: bool,
    pub image_retention_minutes: u64,
    pub enable_ar: bool,
    pub enable_multiplayer: bool,

    // Server info (auto-detected)
    pub public_ip: Option<String>,
    pub local_ip: Option<String>,

    // Client-safe config endpoint
    pub client_config_endpoint: bool,
}

/// Конфигурация для отправки клиенту (без секретов)
#[derive(Debug, Clone, Serialize)]
pub struct ClientConfig {
    pub api_version: String,
    pub server_url: String,
    pub features: ClientFeatures,
    pub limits: ClientLimits,
}

#[derive(Debug, Clone, Serialize)]
pub struct ClientFeatures {
    pub oauth_enabled: bool,
    pub mfa_enabled: bool,
    pub ar_enabled: bool,
    pub multiplayer_enabled: bool,
    pub image_processing_enabled: bool,
}

#[derive(Debug, Clone, Serialize)]
pub struct ClientLimits {
    pub max_quest_difficulty: u8,
    pub max_party_size: u8,
    pub max_inventory_size: u32,
}

impl Config {
    /// Получить клиентскую конфигурацию (без секретов)
    pub fn to_client_config(&self, server_url: String) -> ClientConfig {
        ClientConfig {
            api_version: "2.1.0".to_string(),
            server_url,
            features: ClientFeatures {
                oauth_enabled: self.enable_oauth,
                mfa_enabled: self.enable_mfa,
                ar_enabled: self.enable_ar,
                multiplayer_enabled: self.enable_multiplayer,
                image_processing_enabled: self.enable_image_processing,
            },
            limits: ClientLimits {
                max_quest_difficulty: 10,
                max_party_size: 5,
                max_inventory_size: 100,
            },
        }
    }
}

pub fn load_config() -> Result<Config, Box<dyn std::error::Error>> {
    // Автодетект IP адресов
    let (public_ip, local_ip) = detect_ip_addresses();

    let config = Config {
        database_url: env::var("DATABASE_URL")
            .unwrap_or_else(|_| "postgres://postgres:tsukimi@localhost:5432/irl_quest".to_string()),
        port: env::var("PORT")
            .unwrap_or_else(|_| "8003".to_string())
            .parse()?,
        jwt_secret: env::var("JWT_SECRET")
            .unwrap_or_else(|_| "change-me-in-production".to_string()),
        jwt_expiration_hours: env::var("JWT_EXPIRATION_HOURS")
            .unwrap_or_else(|_| "24".to_string())
            .parse()?,
        refresh_token_expiration_days: env::var("REFRESH_TOKEN_EXPIRATION_DAYS")
            .unwrap_or_else(|_| "30".to_string())
            .parse()?,
        cors_origin: env::var("CORS_ORIGIN").unwrap_or_else(|_| "*".to_string()),
        ml_base_url: env::var("ML_BASE_URL")
            .unwrap_or_else(|_| "http://localhost:8080".to_string()),
        ml_model_path: env::var("ML_MODEL_PATH").ok(),
        ml_infer_cmd: env::var("ML_INFER_CMD").ok(),
        ml_embed_cmd: env::var("ML_EMBED_CMD").ok(),

        // Security
        rate_limit_per_minute: env::var("RATE_LIMIT_PER_MINUTE")
            .unwrap_or_else(|_| "60".to_string())
            .parse()?,
        rate_limit_burst: env::var("RATE_LIMIT_BURST")
            .unwrap_or_else(|_| "10".to_string())
            .parse()?,
        enable_mfa: env::var("ENABLE_MFA")
            .unwrap_or_else(|_| "false".to_string())
            .parse()?,
        password_min_length: env::var("PASSWORD_MIN_LENGTH")
            .unwrap_or_else(|_| "8".to_string())
            .parse()?,

        // Features
        enable_oauth: env::var("ENABLE_OAUTH")
            .unwrap_or_else(|_| "false".to_string())
            .parse()?,
        enable_image_processing: env::var("ENABLE_IMAGE_PROCESSING")
            .unwrap_or_else(|_| "false".to_string())
            .parse()?,
        image_retention_minutes: env::var("IMAGE_RETENTION_MINUTES")
            .unwrap_or_else(|_| "5".to_string())
            .parse()?,
        enable_ar: env::var("ENABLE_AR")
            .unwrap_or_else(|_| "false".to_string())
            .parse()?,
        enable_multiplayer: env::var("ENABLE_MULTIPLAYER")
            .unwrap_or_else(|_| "false".to_string())
            .parse()?,

        // Server info
        public_ip,
        local_ip,

        client_config_endpoint: env::var("CLIENT_CONFIG_ENDPOINT")
            .unwrap_or_else(|_| "true".to_string())
            .parse()?,
    };

    Ok(config)
}

/// Автоматическое определение публичного и локального IP
fn detect_ip_addresses() -> (Option<String>, Option<String>) {
    let local_ip = detect_local_ip();
    let public_ip = env::var("PUBLIC_IP").ok();

    // Не пытаемся автоматически определить публичный IP при загрузке конфига
    // так как это вызывает проблемы с blocking вызовами в async контексте
    // Используйте переменную окружения PUBLIC_IP если нужно

    (public_ip, local_ip)
}

/// Определение локального IP адреса
fn detect_local_ip() -> Option<String> {
    use std::net::UdpSocket;

    // Трюк: подключаемся к внешнему адресу (не отправляя данные)
    // чтобы узнать, какой локальный интерфейс будет использоваться
    let socket = UdpSocket::bind("0.0.0.0:0").ok()?;
    socket.connect("8.8.8.8:80").ok()?;
    let local_addr = socket.local_addr().ok()?;

    Some(local_addr.ip().to_string())
}

/// Определение публичного IP через внешний сервис (async версия)
/// Вызывается отдельно после инициализации, не при загрузке конфига
#[allow(dead_code)]
pub async fn detect_public_ip_async() -> Option<String> {
    use std::time::Duration;

    // Используем async HTTP клиент
    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(5))
        .build()
        .ok()?;

    // Пробуем несколько сервисов
    let services = vec![
        "https://api.ipify.org",
        "https://ifconfig.me/ip",
        "https://icanhazip.com",
    ];

    for service in services {
        if let Ok(response) = client.get(service).send().await {
            if let Ok(ip_str) = response.text().await {
                let ip_trimmed = ip_str.trim();
                if ip_trimmed.parse::<IpAddr>().is_ok() {
                    tracing::info!("Detected public IP from {}: {}", service, ip_trimmed);
                    return Some(ip_trimmed.to_string());
                }
            }
        }
    }

    tracing::warn!("Could not detect public IP from external services");
    None
}
