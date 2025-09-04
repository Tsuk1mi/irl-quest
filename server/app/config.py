import os
from typing import Optional


class Settings:
    """Простейший контейнер настроек, значениями по умолчанию можно переопределить через окружение."""
    def __init__(self):
        self.DATABASE_URL: str = os.getenv(
            "DATABASE_URL",
            "postgresql+asyncpg://postgres:password@localhost:5432/irlquest",
        )
        self.REDIS_URL: str = os.getenv("REDIS_URL", "redis://localhost:6379/0")
        self.PGVECTOR_DIM: int = int(os.getenv("PGVECTOR_DIM", "1536"))
        self.DEBUG: bool = os.getenv("DEBUG", "0") in ("1", "true", "True")
        # Security / JWT
        self.SECRET_KEY: str = os.getenv("SECRET_KEY", "change-me-for-prod")
        self.ALGORITHM: str = os.getenv("JWT_ALGORITHM", "HS256")
        self.ACCESS_TOKEN_EXPIRE_MINUTES: int = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "60"))


_settings: Optional[Settings] = None


def get_settings() -> Settings:
    global _settings
    if _settings is None:
        _settings = Settings()
    return _settings
