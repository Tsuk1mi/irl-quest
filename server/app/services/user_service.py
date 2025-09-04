from typing import Optional
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.user_repository import get_user, update_user
from app.models.user import User
from app.schemas.user import UserUpdate


async def get_user_by_id(db: AsyncSession, user_id: int) -> Optional[User]:
    return await get_user(db, user_id)


async def update_user_for_user(db: AsyncSession, user_id: int, payload: UserUpdate) -> Optional[User]:
    username = payload.username
    password = payload.password
    return await update_user(db, user_id, username=username, password=password
                              )

