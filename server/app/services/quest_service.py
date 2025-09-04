from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.quest_repository import (
    list_quests as repo_list_quests,
    get_quest as repo_get_quest,
    create_quest as repo_create_quest,
    update_quest as repo_update_quest,
    delete_quest as repo_delete_quest,
)
from app.models.quest import Quest
from app.schemas.quest import QuestCreate, QuestUpdate


async def list_quests_for_user(db: AsyncSession, user_id: int, skip: int = 0, limit: int = 100) -> List[Quest]:
    return await repo_list_quests(db, skip=skip, limit=limit, owner_id=user_id)


async def get_quest_for_user(db: AsyncSession, user_id: int, quest_id: int) -> Optional[Quest]:
    quest = await repo_get_quest(db, quest_id)
    if not quest or quest.owner_id != user_id:
        return None
    return quest


async def create_quest_for_user(db: AsyncSession, user_id: int, quest_in: QuestCreate) -> Quest:
    return await repo_create_quest(db, quest_in, owner_id=user_id)


async def update_quest_for_user(db: AsyncSession, user_id: int, quest_id: int, quest_in: QuestUpdate) -> Optional[Quest]:
    quest = await repo_get_quest(db, quest_id)
    if not quest or quest.owner_id != user_id:
        return None
    return await repo_update_quest(db, quest_id, quest_in)


async def delete_quest_for_user(db: AsyncSession, user_id: int, quest_id: int) -> bool:
    quest = await repo_get_quest(db, quest_id)
    if not quest or quest.owner_id != user_id:
        return False
    return await repo_delete_quest(db, quest_id)

