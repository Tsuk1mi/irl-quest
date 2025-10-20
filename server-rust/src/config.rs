use serde::Deserialize;
use std::env;

#[derive(Debug, Deserialize)]
pub struct Config {
    pub database_url: String,
    pub port: u16,
    pub jwt_secret: String,
    pub cors_origin: String,
    pub ml_base_url: String,
}

pub fn load_config() -> Result<Config, Box<dyn std::error::Error>> {
    Ok(Config {
        database_url: env::var("DATABASE_URL")
            .unwrap_or_else(|_| "postgresql://postgres:password@localhost:5432/irl_quest".to_string()),
        port: env::var("PORT")
            .unwrap_or_else(|_| "8003".to_string())
            .parse()?,
        jwt_secret: env::var("JWT_SECRET")
            .unwrap_or_else(|_| "your-secret-key".to_string()),
        cors_origin: env::var("CORS_ORIGIN")
            .unwrap_or_else(|_| "*".to_string()),
        ml_base_url: env::var("ML_BASE_URL")
            .unwrap_or_else(|_| "http://localhost:8080".to_string()),
    })
}
