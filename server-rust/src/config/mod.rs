use std::env;

#[derive(Debug, Clone)]
pub struct Settings {
    pub database_url: String,
    pub redis_url: String,
    pub pgvector_dim: i32,
    pub debug: bool,
    pub secret_key: String,
    pub jwt_algorithm: String,
    pub access_token_expire_minutes: i64,
    pub server_host: String,
    pub server_port: u16,
}

impl Settings {
    pub fn new() -> Self {
        Self {
            database_url: env::var("DATABASE_URL")
                .unwrap_or_else(|_| "postgresql://postgres:password@localhost:5432/irlquest".to_string()),
            redis_url: env::var("REDIS_URL")
                .unwrap_or_else(|_| "redis://localhost:6379/0".to_string()),
            pgvector_dim: env::var("PGVECTOR_DIM")
                .unwrap_or_else(|_| "1536".to_string())
                .parse()
                .unwrap_or(1536),
            debug: env::var("DEBUG")
                .unwrap_or_else(|_| "0".to_string())
                .parse::<i32>()
                .unwrap_or(0) == 1,
            secret_key: env::var("SECRET_KEY")
                .unwrap_or_else(|_| "change-me-for-prod".to_string()),
            jwt_algorithm: env::var("JWT_ALGORITHM")
                .unwrap_or_else(|_| "HS256".to_string()),
            access_token_expire_minutes: env::var("ACCESS_TOKEN_EXPIRE_MINUTES")
                .unwrap_or_else(|_| "60".to_string())
                .parse()
                .unwrap_or(60),
            server_host: env::var("SERVER_HOST")
                .unwrap_or_else(|_| "0.0.0.0".to_string()),
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8000".to_string())
                .parse()
                .unwrap_or(8000),
        }
    }
}

impl Default for Settings {
    fn default() -> Self {
        Self::new()
    }
}