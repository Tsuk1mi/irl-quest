use serde::Deserialize;
use std::env;

#[derive(Debug, Deserialize)]
pub struct Config {
    pub database_url: String,
    pub port: u16,
    pub jwt_secret: String,
    pub cors_origin: String,
    pub redis_url: String,
}

pub fn load_config() -> Result<Config, Box<dyn std::error::Error>> {
    Ok(Config {
        database_url: env::var("DATABASE_URL")
            .unwrap_or_else(|_| "postgres://postgres:postgres@localhost:5432/irlquest".to_string()),
        port: env::var("PORT")
            .unwrap_or_else(|_| "3000".to_string())
            .parse()?,
        jwt_secret: env::var("JWT_SECRET")
            .unwrap_or_else(|_| "your-secret-key".to_string()),
        cors_origin: env::var("CORS_ORIGIN")
            .unwrap_or_else(|_| "http://localhost:3000".to_string()),
        redis_url: env::var("REDIS_URL")
            .unwrap_or_else(|_| "redis://localhost:6379".to_string()),
    })
}
