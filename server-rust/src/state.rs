use sqlx::PgPool;
use crate::cache::Cache;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub cache: Cache,
}

impl AppState {
    pub fn new(db: PgPool, cache: Cache) -> Self {
        Self { db, cache }
    }
}
