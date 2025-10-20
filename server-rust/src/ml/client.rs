use anyhow::Result;
use sha2::{Digest, Sha256};

#[derive(Clone)]
pub struct MlClient {
    pub base_url: String, // оставляем поле для совместимости
}

impl MlClient {
    pub fn new(base_url: String) -> Self {
        Self { base_url }
    }

    /// Генерирует простые детерминированные эмбеддинги на основе SHA256 снимка текста.
    /// Каждый embedding имеет фиксированную длину 64.
    pub async fn embed_texts(&self, texts: Vec<String>) -> Result<Vec<Vec<f32>>> {
        let mut result = Vec::with_capacity(texts.len());
        for t in texts {
            let mut hasher = Sha256::new();
            hasher.update(t.as_bytes());
            let hash = hasher.finalize();
            // Преобразуем байты хеша в последовательность f32 в диапазоне [-1,1]
            let mut emb = Vec::with_capacity(64);
            for i in 0..64 {
                let idx = i % hash.len();
                let byte = hash[idx];
                // Нормализация: байт 0..255 -> -1.0..1.0
                let v = (byte as f32 / 127.5) - 1.0;
                emb.push(v);
            }
            result.push(emb);
        }
        Ok(result)
    }

    /// Простая inference-стратегия: делает минимальную трансформацию/обобщение
    /// для целей демонстрации. Можно заменить реальной моделью позже.
    pub async fn infer(&self, prompt: String) -> Result<String> {
        // Короткая эвристика: если в prompt есть слово "quest" — вернуть шаблон квеста
        let lower = prompt.to_lowercase();
        if lower.contains("quest") || lower.contains("todo") {
            let res = format!("Generated short quest from prompt: {}", prompt);
            Ok(res)
        } else if lower.len() < 60 {
            Ok(format!("Echo: {}", prompt))
        } else {
            // Возвращаем сокращённую версию
            let snippet = prompt.chars().take(120).collect::<String>();
            Ok(format!("Summary: {}...", snippet))
        }
    }
}
