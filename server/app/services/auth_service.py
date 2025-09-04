# Новый файл: сервис для аутентификации
from datetime import timedelta
from typing import Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.user_repository import get_user_by_email, create_user, authenticate_user
from app.security import create_access_token
from app.config import get_settings
from app.schemas.user import UserCreate

settings = get_settings()


async def register_user(db: AsyncSession, user_in: UserCreate):
    existing = await get_user_by_email(db, user_in.email)
    if existing:
        raise ValueError("Email already registered")
    user = await create_user(db, user_in)
    return user


async def authenticate_and_issue_token(db: AsyncSession, username_or_email: str, password: str) -> str:
    # authenticate_user expects email; try as email first, then try lookup by username
    user = await authenticate_user(db, username_or_email, password)
    if not user:
        # try lookup by username
        # repository provides get_user_by_username, but to avoid import cycles we can call authenticate_user with username lookup
        # For simplicity, attempt to authenticate using username by searching user by email/username
        from app.repositories.user_repository import get_user_by_username, get_user_by_email as _get_by_email
        u = await get_user_by_username(db, username_or_email)
        if u:
            # authenticate by email of found user
            user = await authenticate_user(db, u.email, password)

    if not user:
        raise ValueError("Invalid credentials")

    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    token = create_access_token(subject=user.email, expires_delta=access_token_expires)
    return token

