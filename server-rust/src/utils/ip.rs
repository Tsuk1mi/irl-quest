use axum::http::HeaderMap;

// Try to get client IP from X-Forwarded-For, X-Real-IP or from an optional `Forwarded` header.
pub fn get_client_ip_from_headers(headers: &HeaderMap) -> Option<String> {
    // X-Forwarded-For may contain multiple IPs, comma separated; take the first
    if let Some(v) = headers.get("x-forwarded-for") {
        if let Ok(s) = v.to_str() {
            let first = s.split(',').next().map(|s| s.trim().to_string());
            if first.is_some() && !first.as_ref().unwrap().is_empty() {
                return first;
            }
        }
    }

    if let Some(v) = headers.get("x-real-ip") {
        if let Ok(s) = v.to_str() {
            let s = s.trim();
            if !s.is_empty() {
                return Some(s.to_string());
            }
        }
    }

    if let Some(v) = headers.get("forwarded") {
        if let Ok(s) = v.to_str() {
            // Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
            for part in s.split(';') {
                let part = part.trim();
                if part.starts_with("for=") {
                    let ip = part.trim_start_matches("for=").trim_matches('\"').to_string();
                    return Some(ip);
                }
            }
        }
    }

    None
}

