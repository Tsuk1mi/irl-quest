# ...existing code...
from app.db.session import AsyncSessionLocal
from app.models import Task
from app.repositories.user_repository import get_user_by_email
from sqlalchemy import select
from typing import List, Optional


async def seed_tasks() -> int:
    """Вставляет несколько тестовых задач, если таблица пуста. Возвращает число добавленных записей."""
    async with AsyncSessionLocal() as session:
        result = await session.execute(select(Task).limit(1))
        exists = result.scalar_one_or_none()
        if exists:
            return 0

        # Попробуем найти админа для назначения owner_id
        admin = await get_user_by_email(session, "admin@irlquest.local")
        owner_id: Optional[int] = getattr(admin, "id", None) if admin else None

        samples: List[Task] = [
            Task(title="Создать профиль", description="Заполнить информацию о себе", owner_id=owner_id),
            Task(title="Пройти туториал", description="Завершить первый квест", owner_id=owner_id),
            Task(title="Фокус-сессия 25 минут", description="Сделать pomodoro-сессию", owner_id=owner_id),
        ]
        session.add_all(samples)
        await session.commit()
        return len(samples)

# ...existing code...
