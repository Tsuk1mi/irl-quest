# ...existing code...
from app.db.session import AsyncSessionLocal
from app.repositories.user_repository import get_user_by_email, create_user
from app.schemas.user import UserCreate
from typing import Optional


async def seed_admin_user() -> Optional[int]:
    """Создает тестового администратора, если не существует. Возвращает id или None."""
    async with AsyncSessionLocal() as session:
        admin_email = "admin@example.com"
        exists = await get_user_by_email(session, admin_email)
        if exists:
            return None
        admin = UserCreate(email=admin_email, username="admin", password="adminpass")
        user = await create_user(session, admin)
        return getattr(user, "id", None)

# ...existing code...
