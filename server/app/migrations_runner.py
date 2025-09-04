from alembic.config import Config
from alembic import command
import os
import logging
from app.config import get_settings

logger = logging.getLogger(__name__)


def run_migrations() -> None:
    """Запускает alembic upgrade head, используя sqlalchemy.url из настроек.
    Использует абсолютный путь к alembic.ini и логирует ошибки вместо падения.
    """
    settings = get_settings()
    here = os.path.dirname(__file__)
    ini_path = os.path.abspath(os.path.join(here, '..', 'alembic.ini'))
    if not os.path.exists(ini_path):
        logger.error("Alembic ini not found at %s", ini_path)
        return

    try:
        alembic_cfg = Config(ini_path)
        # Переопределяем URL из настроек
        alembic_cfg.set_main_option('sqlalchemy.url', settings.DATABASE_URL)
        command.upgrade(alembic_cfg, 'head')
        logger.info("Alembic migrations applied from %s", ini_path)
    except Exception as exc:
        logger.exception("Failed to run alembic migrations: %s", exc)
