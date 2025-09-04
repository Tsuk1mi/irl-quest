from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.deps import get_db_session, get_current_user
from app.schemas.user import UserOut, UserUpdate
from app.services import user_service
from app.models.user import User

router = APIRouter()


@router.get("/me", response_model=UserOut)
async def get_me(current_user: User = Depends(get_current_user)):
    return current_user


@router.put("/me", response_model=UserOut)
async def update_me(payload: UserUpdate, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    user = await user_service.update_user_for_user(db, current_user.id, payload)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user

