use redis::{Client, Commands, RedisError};
use serde::{de::DeserializeOwned, Serialize};
use std::time::Duration;

pub struct Cache {
    client: Client,
}

impl Cache {
    pub fn new(redis_url: &str) -> Result<Self, RedisError> {
        let client = Client::open(redis_url)?;
        Ok(Cache { client })
    }

    pub async fn set<T: Serialize>(
        &self,
        key: &str,
        value: &T,
        expiration: Duration,
    ) -> Result<(), RedisError> {
        let mut conn = self.client.get_connection()?;
        let serialized = serde_json::to_string(value).map_err(|e| {
            RedisError::from((
                redis::ErrorKind::InvalidClientConfig,
                "Serialization error",
                e.to_string(),
            ))
        })?;

        conn.set_ex(key, serialized, expiration.as_secs() as usize)?;
        Ok(())
    }

    pub async fn get<T: DeserializeOwned>(&self, key: &str) -> Result<Option<T>, RedisError> {
        let mut conn = self.client.get_connection()?;
        let value: Option<String> = conn.get(key)?;

        match value {
            Some(val) => {
                let deserialized = serde_json::from_str(&val).map_err(|e| {
                    RedisError::from((
                        redis::ErrorKind::InvalidClientConfig,
                        "Deserialization error",
                        e.to_string(),
                    ))
                })?;
                Ok(Some(deserialized))
            }
            None => Ok(None),
        }
    }

    pub async fn delete(&self, key: &str) -> Result<(), RedisError> {
        let mut conn = self.client.get_connection()?;
        conn.del(key)?;
        Ok(())
    }
}
