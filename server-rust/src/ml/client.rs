use anyhow::Result;
use sha2::{Digest, Sha256};
use std::path::Path;
use tokio::process::Command;
use serde_json::Value;

#[derive(Clone)]
pub struct MlClient {
    pub base_url: String, // оставляем поле для совместимости
    pub model_path: Option<String>,
    pub infer_cmd: Option<String>,
    pub embed_cmd: Option<String>,
}

impl MlClient {
    pub fn new(
        base_url: String,
        model_path: Option<String>,
        infer_cmd: Option<String>,
        embed_cmd: Option<String>,
    ) -> Self {
        // Логируем факт наличия пути
        if let Some(ref p) = model_path {
            if Path::new(p).exists() {
                tracing::info!("ML client: model path set and exists: {}", p);
            } else {
                tracing::warn!("ML client: model path set but file not found: {}", p);
            }
        } else {
            tracing::info!("ML client: no model path configured, using builtin Rust stubs");
        }
        Self { base_url, model_path, infer_cmd, embed_cmd }
    }

    async fn run_command_and_parse_embeddings(&self, cmd_template: &str, texts: &Vec<String>) -> Result<Vec<Vec<f32>>> {
        // Replace placeholders: {model} and {input}
        let model_placeholder = self.model_path.clone().unwrap_or_default();
        let input_json = serde_json::to_string(texts)?;
        let cmd_str = cmd_template
            .replace("{model}", &model_placeholder)
            .replace("{input}", &input_json);

        tracing::info!("Running embed command: {}", cmd_str);

        // Execute via Windows shell
        let output = Command::new("cmd").args(&["/C", &cmd_str]).output().await?;
        if !output.status.success() {
            let stderr = String::from_utf8_lossy(&output.stderr);
            tracing::warn!("Embed command failed: {}", stderr);
            anyhow::bail!("Embed command failed");
        }
        let stdout = String::from_utf8_lossy(&output.stdout).to_string();
        // Try parse JSON array of arrays
        match serde_json::from_str::<Vec<Vec<f32>>>(&stdout) {
            Ok(v) => Ok(v),
            Err(_) => {
                // Try parsing as generic JSON and extract numbers
                let val: Value = serde_json::from_str(&stdout)?;
                if let Some(arr) = val.as_array() {
                    let mut out = Vec::with_capacity(arr.len());
                    for item in arr.iter() {
                        if let Some(vecv) = item.as_array() {
                            let mut row = Vec::with_capacity(vecv.len());
                            for n in vecv.iter() {
                                if let Some(f) = n.as_f64() {
                                    row.push(f as f32);
                                }
                            }
                            out.push(row);
                        }
                    }
                    return Ok(out);
                }
                anyhow::bail!("Unexpected embed command output format");
            }
        }
    }

    async fn run_command_and_parse_infer(&self, cmd_template: &str, prompt: &str) -> Result<String> {
        let model_placeholder = self.model_path.clone().unwrap_or_default();
        let cmd_str = cmd_template
            .replace("{model}", &model_placeholder)
            .replace("{input}", prompt);

        tracing::info!("Running infer command: {}", cmd_str);

        let output = Command::new("cmd").args(&["/C", &cmd_str]).output().await?;
        if !output.status.success() {
            let stderr = String::from_utf8_lossy(&output.stderr);
            tracing::warn!("Infer command failed: {}", stderr);
            anyhow::bail!("Infer command failed");
        }
        let stdout = String::from_utf8_lossy(&output.stdout).to_string();
        Ok(stdout)
    }

    /// Генерирует простые детерминированные эмбеддинги на основе SHA256 снимка текста.
    /// Каждый embedding имеет фиксированную длину 64.
    pub async fn embed_texts(&self, texts: Vec<String>) -> Result<Vec<Vec<f32>>> {
        if let Some(ref cmd_template) = self.embed_cmd {
            match self.run_command_and_parse_embeddings(cmd_template, &texts).await {
                Ok(v) => return Ok(v),
                Err(e) => tracing::warn!("Embed command failed, falling back to stub: {:?}", e),
            }
        }

        // fallback stub
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

    /// Простая inference-стратегия: вызывает команду, если указана, иначе использует эвристику.
    pub async fn infer(&self, prompt: String) -> Result<String> {
        if let Some(ref cmd_template) = self.infer_cmd {
            match self.run_command_and_parse_infer(cmd_template, &prompt).await {
                Ok(s) => return Ok(s),
                Err(e) => tracing::warn!("Infer command failed, falling back to stub: {:?}", e),
            }
        }

        // fallback stub
        let lower = prompt.to_lowercase();
        if lower.contains("quest") || lower.contains("todo") {
            let res = format!("Generated short quest from prompt: {}", prompt);
            Ok(res)
        } else if lower.len() < 60 {
            Ok(format!("Echo: {}", prompt))
        } else {
            let snippet = prompt.chars().take(120).collect::<String>();
            Ok(format!("Summary: {}...", snippet))
        }
    }
}
