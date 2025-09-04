from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging

from app.api.v1.router import router as api_v1_router
from app.config import get_settings
from app.db.session import engine
from app.db.base import Base
from app.logging import get_logger
from app.migrations_runner import run_migrations
from app.seeds.tasks import seed_tasks
from app.seeds.users import seed_admin_user

settings = get_settings()

app = FastAPI(title="IRL Quest API", version="0.1.0")

# Простая CORS-конфигурация для разработки
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

logger = get_logger("irl-quest")

# Подключаем API v1
app.include_router(api_v1_router, prefix="/api/v1")


@app.on_event("startup")
async def on_startup():
    try:
        # В проде использовать миграции вручную; в dev — прогоняем их автоматически
        try:
            run_migrations()
            logger.info("Alembic migrations applied")
        except Exception:
            logger.exception("Failed to run migrations, falling back to create_all")
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            logger.info("Database tables ensured (create_all)")

        # Сидаем тестового админа и данные
        try:
            admin_added = await seed_admin_user()
            if admin_added:
                logger.info("Seeded admin user")
        except Exception:
            logger.exception("Failed to seed admin user")

        try:
            added = await seed_tasks()
            if added:
                logger.info("Seeded %d tasks", added)
        except Exception:
            logger.exception("Failed to seed initial tasks")

    except Exception as exc:
        logger.exception("Startup failure: %s", exc)


@app.on_event("shutdown")
async def on_shutdown():
    try:
        await engine.dispose()
    except Exception:
        pass


@app.get("/")
async def root():
    return {"message": "IRL Quest API", "status": "ok"}


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/ready")
async def ready():
    """Проверка готовности: проверяем подключение к БД и Redis (если доступен)."""
    ready = {"db": False, "redis": None}
    # Проверка БД
    try:
        async with engine.connect() as conn:
            await conn.execute("SELECT 1")
        ready["db"] = True
    except Exception as exc:
        logger.warning("DB readiness check failed: %s", exc)

    # Проверка Redis (если установлен)
    try:
        import redis.asyncio as aioredis
        settings = get_settings()
        r = aiorededis.from_url(settings.REDIS_URL)
        pong = await r.ping()
        ready["redis"] = bool(pong)
        await r.close()
    except Exception as exc:
        # Redis необязателен для старта
        logger.info("Redis readiness check skipped or failed: %s", exc)
        ready["redis"] = None

    overall = ready["db"] and (ready["redis"] is not False)
    return {"ready": overall, "details": ready}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("server.app.main:app", host="0.0.0.0", port=8000, log_level="info")
