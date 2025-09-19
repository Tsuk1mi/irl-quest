use std::env;

#[derive(Debug, Clone)]
pub struct Settings {
    pub database_url: String,
    pub server_host: String,
    pub server_port: u16,
    pub secret_key: String,
    pub jwt_algorithm: String,
    pub access_token_expire_minutes: i64,
    pub openai_api_key: Option<String>,
    pub openai_base_url: String,
}

impl Settings {
    pub fn new() -> Self {
        Self {
            database_url: env::var("DATABASE_URL")
                .unwrap_or_else(|_| "postgresql://postgres:password@localhost:5432/irl_quest".to_string()),
            server_host: env::var("SERVER_HOST").unwrap_or_else(|_| "0.0.0.0".to_string()),
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8003".to_string())
                .parse()
                .expect("SERVER_PORT must be a valid number"),
            secret_key: env::var("SECRET_KEY")
                .unwrap_or_else(|_| "rust-secret-key-for-irl-quest-dev".to_string()),
            jwt_algorithm: env::var("JWT_ALGORITHM").unwrap_or_else(|_| "HS256".to_string()),
            access_token_expire_minutes: env::var("ACCESS_TOKEN_EXPIRE_MINUTES")
                .unwrap_or_else(|_| "60".to_string())
                .parse()
                .expect("ACCESS_TOKEN_EXPIRE_MINUTES must be a valid number"),
            openai_api_key: env::var("OPENAI_API_KEY").ok(),
            openai_base_url: env::var("OPENAI_BASE_URL")
                .unwrap_or_else(|_| "https://api.openai.com/v1".to_string()),
        }
    }
}