use crate::config::Config;
/// Middleware для Rate Limiting и IP-blocking
use axum::{
    extract::{ConnectInfo, Request, State},
    http::StatusCode,
    middleware::Next,
    response::{IntoResponse, Response},
};
use std::collections::HashMap;
use std::net::SocketAddr;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;

/// Информация о клиенте для rate limiting
#[derive(Debug, Clone)]
struct ClientInfo {
    request_count: u32,
    window_start: Instant,
    blocked_until: Option<Instant>,
}

/// Rate limiter state
#[derive(Clone)]
pub struct RateLimiter {
    clients: Arc<RwLock<HashMap<String, ClientInfo>>>,
    config: Config,
}

impl RateLimiter {
    pub fn new(config: Config) -> Self {
        Self {
            clients: Arc::new(RwLock::new(HashMap::new())),
            config,
        }
    }

    /// Проверить, можно ли выполнить запрос
    async fn check_rate_limit(&self, client_ip: &str) -> Result<(), String> {
        let mut clients = self.clients.write().await;
        let now = Instant::now();

        // Получить или создать информацию о клиенте
        let client_info = clients
            .entry(client_ip.to_string())
            .or_insert_with(|| ClientInfo {
                request_count: 0,
                window_start: now,
                blocked_until: None,
            });

        // Проверить, заблокирован ли клиент
        if let Some(blocked_until) = client_info.blocked_until {
            if now < blocked_until {
                let remaining = (blocked_until - now).as_secs();
                return Err(format!(
                    "Too many requests. Try again in {} seconds",
                    remaining
                ));
            } else {
                // Разблокировать
                client_info.blocked_until = None;
                client_info.request_count = 0;
                client_info.window_start = now;
            }
        }

        // Проверить окно времени (1 минута)
        let window_duration = Duration::from_secs(60);
        if now.duration_since(client_info.window_start) > window_duration {
            // Новое окно
            client_info.request_count = 0;
            client_info.window_start = now;
        }

        // Проверить лимит
        client_info.request_count += 1;

        if client_info.request_count > self.config.rate_limit_per_minute {
            // Превышен лимит - заблокировать на 5 минут
            client_info.blocked_until = Some(now + Duration::from_secs(300));
            tracing::warn!("Rate limit exceeded for IP: {}", client_ip);
            return Err("Rate limit exceeded. You are temporarily blocked.".to_string());
        }

        // Проверить burst лимит (слишком быстрые запросы)
        if client_info.request_count > self.config.rate_limit_burst
            && now.duration_since(client_info.window_start) < Duration::from_secs(1)
        {
            client_info.blocked_until = Some(now + Duration::from_secs(60));
            tracing::warn!("Burst limit exceeded for IP: {}", client_ip);
            return Err("Too many requests too quickly. Please slow down.".to_string());
        }

        Ok(())
    }

    /// Очистить старые записи (вызывается периодически)
    pub async fn cleanup(&self) {
        let mut clients = self.clients.write().await;
        let now = Instant::now();
        let retention = Duration::from_secs(600); // 10 минут

        clients.retain(|_, info| {
            // Удалить клиентов, которые не делали запросов больше 10 минут
            now.duration_since(info.window_start) < retention
        });

        tracing::debug!("Rate limiter cleanup: {} active clients", clients.len());
    }
}

/// Middleware функция для rate limiting
pub async fn rate_limit_middleware(
    State(rate_limiter): State<RateLimiter>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Response {
    // Получить IP клиента (из заголовков или из ConnectInfo)
    let client_ip = crate::utils_impl::ip::get_client_ip_from_headers(request.headers())
        .unwrap_or_else(|| addr.ip().to_string());

    // Проверить rate limit
    match rate_limiter.check_rate_limit(&client_ip).await {
        Ok(_) => next.run(request).await,
        Err(message) => (
            StatusCode::TOO_MANY_REQUESTS,
            [(
                "Retry-After",
                "60", // Попробовать через 60 секунд
            )],
            message,
        )
            .into_response(),
    }
}

/// IP Blacklist для блокировки вредоносных IP
#[derive(Clone)]
pub struct IpBlocklist {
    blocked_ips: Arc<RwLock<HashMap<String, Instant>>>,
}

impl IpBlocklist {
    pub fn new() -> Self {
        Self {
            blocked_ips: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    /// Заблокировать IP
    pub async fn block_ip(&self, ip: &str, duration: Duration) {
        let mut blocked = self.blocked_ips.write().await;
        let blocked_until = Instant::now() + duration;
        blocked.insert(ip.to_string(), blocked_until);
        tracing::warn!("Blocked IP: {} until {:?}", ip, blocked_until);
    }

    /// Проверить, заблокирован ли IP
    pub async fn is_blocked(&self, ip: &str) -> bool {
        let blocked = self.blocked_ips.read().await;
        if let Some(blocked_until) = blocked.get(ip) {
            if Instant::now() < *blocked_until {
                return true;
            }
        }
        false
    }

    /// Разблокировать IP
    pub async fn unblock_ip(&self, ip: &str) {
        let mut blocked = self.blocked_ips.write().await;
        blocked.remove(ip);
        tracing::info!("Unblocked IP: {}", ip);
    }

    /// Очистить истекшие блокировки
    pub async fn cleanup(&self) {
        let mut blocked = self.blocked_ips.write().await;
        let now = Instant::now();
        blocked.retain(|_, blocked_until| now < *blocked_until);
    }
}

/// Middleware для проверки IP blacklist
pub async fn ip_blacklist_middleware(
    State(blacklist): State<IpBlocklist>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Response {
    let client_ip = crate::utils_impl::ip::get_client_ip_from_headers(request.headers())
        .unwrap_or_else(|| addr.ip().to_string());

    if blacklist.is_blocked(&client_ip).await {
        tracing::warn!("Blocked request from blacklisted IP: {}", client_ip);
        return (
            StatusCode::FORBIDDEN,
            "Your IP address is blocked due to suspicious activity.",
        )
            .into_response();
    }

    next.run(request).await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_rate_limiter() {
        let config = Config {
            database_url: "".to_string(),
            port: 8003,
            jwt_secret: "test".to_string(),
            jwt_expiration_hours: 24,
            refresh_token_expiration_days: 30,
            cors_origin: "*".to_string(),
            ml_base_url: "".to_string(),
            ml_model_path: None,
            ml_infer_cmd: None,
            ml_embed_cmd: None,
            rate_limit_per_minute: 10,
            rate_limit_burst: 5,
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
        };

        let limiter = RateLimiter::new(config);

        // Должно пройти первые 10 запросов
        for _ in 0..10 {
            assert!(limiter.check_rate_limit("127.0.0.1").await.is_ok());
        }

        // 11-й запрос должен быть заблокирован
        assert!(limiter.check_rate_limit("127.0.0.1").await.is_err());
    }

    #[tokio::test]
    async fn test_ip_blacklist() {
        let blacklist = IpBlocklist::new();

        // IP не заблокирован изначально
        assert!(!blacklist.is_blocked("192.168.1.1").await);

        // Заблокировать IP
        blacklist
            .block_ip("192.168.1.1", Duration::from_secs(60))
            .await;

        // Теперь заблокирован
        assert!(blacklist.is_blocked("192.168.1.1").await);

        // Разблокировать
        blacklist.unblock_ip("192.168.1.1").await;

        // Больше не заблокирован
        assert!(!blacklist.is_blocked("192.168.1.1").await);
    }
}
