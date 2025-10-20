use sqlx::PgPool;

use crate::ml::MlClient;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub ml_client: MlClient,
    pub ml_base_url: String,
}

impl AppState {
    pub fn new(db: PgPool, ml_base_url: String) -> Self {
        let ml_client = MlClient::new(ml_base_url.clone());
        Self { db, ml_client, ml_base_url }
    }
}
