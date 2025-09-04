from typing import AsyncGenerator
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer

from app.db.session import get_db
from app.config import get_settings
from app.repositories.user_repository import get_user_by_email
from app.security import decode_access_token

settings = get_settings()

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/token")


async def get_db_session() -> AsyncGenerator:
    async for s in get_db():
        yield s


async def get_current_user(token: str = Depends(oauth2_scheme), db = Depends(get_db_session)):
    try:
        payload = decode_access_token(token)
        subject = payload.get("sub")
        if subject is None:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid authentication credentials")
    except Exception:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid authentication credentials")

    user = await get_user_by_email(db, subject)
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user
