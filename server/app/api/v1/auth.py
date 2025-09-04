from datetime import timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.ext.asyncio import AsyncSession

from app.deps import get_db_session, get_current_user
from app.schemas.user import UserCreate, UserOut, Token
from app.services import auth_service
from app.config import get_settings

router = APIRouter()
settings = get_settings()


@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
async def register(user_in: UserCreate, db: AsyncSession = Depends(get_db_session)):
    try:
        user = await auth_service.register_user(db, user_in)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    return user


@router.post("/token", response_model=Token)
async def token(form_data: OAuth2PasswordRequestForm = Depends(), db: AsyncSession = Depends(get_db_session)):
    try:
        access_token = await auth_service.authenticate_and_issue_token(db, form_data.username, form_data.password)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Incorrect email or password")
    return {"access_token": access_token, "token_type": "bearer"}


@router.get("/me", response_model=UserOut)
async def me(current_user=Depends(get_current_user)):
    return current_user
