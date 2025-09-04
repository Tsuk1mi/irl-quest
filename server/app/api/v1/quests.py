from typing import List
from fastapi import APIRouter, Depends, HTTPException, status, Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.deps import get_db_session, get_current_user
from app.schemas.quest import QuestCreate, QuestUpdate, QuestOut
from app.services import quest_service
from app.models.user import User

router = APIRouter()


@router.get("/", response_model=List[QuestOut])
async def list_quests(skip: int = 0, limit: int = 100, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    return await quest_service.list_quests_for_user(db, current_user.id, skip=skip, limit=limit)


@router.post("/", response_model=QuestOut, status_code=status.HTTP_201_CREATED)
async def create_quest(quest_in: QuestCreate, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    return await quest_service.create_quest_for_user(db, current_user.id, quest_in)


@router.get("/{quest_id}", response_model=QuestOut)
async def get_quest(quest_id: int, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    quest = await quest_service.get_quest_for_user(db, current_user.id, quest_id)
    if not quest:
        raise HTTPException(status_code=404, detail="Quest not found")
    return quest


@router.put("/{quest_id}", response_model=QuestOut)
async def update_quest(quest_id: int, quest_in: QuestUpdate, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    quest = await quest_service.update_quest_for_user(db, current_user.id, quest_id, quest_in)
    if not quest:
        raise HTTPException(status_code=404, detail="Quest not found")
    return quest


@router.delete("/{quest_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_quest(quest_id: int, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    ok = await quest_service.delete_quest_for_user(db, current_user.id, quest_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Quest not found")
    return Response(status_code=status.HTTP_204_NO_CONTENT)
