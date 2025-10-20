use sqlx::PgPool;

use crate::ml::MlClient;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub ml_client: MlClient,
    pub ml_base_url: String,
    pub ml_model_path: Option<String>,
    pub ml_infer_cmd: Option<String>,
    pub ml_embed_cmd: Option<String>,
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
        Self { db, ml_client, ml_base_url, ml_model_path, ml_infer_cmd, ml_embed_cmd }
    }
}
