from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from typing import AsyncGenerator
from app.config import get_settings

settings = get_settings()

# Создаем асинхронный движок
engine = create_async_engine(
    settings.DATABASE_URL,
    future=True,
    echo=settings.DEBUG,
)

# Фабрика сессий
AsyncSessionLocal = async_sessionmaker(bind=engine, expire_on_commit=False, class_=AsyncSession)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency - выдает асинхронную сессию SQLAlchemy."""
    async with AsyncSessionLocal() as session:
        yield session
