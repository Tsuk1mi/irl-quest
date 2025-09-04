from typing import List, Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.quest import Quest
from app.schemas.quest import QuestCreate, QuestUpdate


async def list_quests(db: AsyncSession, skip: int = 0, limit: int = 100, owner_id: Optional[int] = None) -> List[Quest]:
    q = select(Quest)
    if owner_id is not None:
        q = q.where(Quest.owner_id == owner_id)
    q = q.offset(skip).limit(limit)
    result = await db.execute(q)
    return result.scalars().all()


async def get_quest(db: AsyncSession, quest_id: int) -> Optional[Quest]:
    result = await db.execute(select(Quest).where(Quest.id == quest_id))
    return result.scalar_one_or_none()


async def create_quest(db: AsyncSession, quest_in: QuestCreate, owner_id: Optional[int] = None) -> Quest:
    quest = Quest(title=quest_in.title, description=quest_in.description, difficulty=quest_in.difficulty or 1, owner_id=owner_id)
    db.add(quest)
    await db.commit()
    await db.refresh(quest)
    return quest


async def update_quest(db: AsyncSession, quest_id: int, quest_in: QuestUpdate) -> Optional[Quest]:
    quest = await get_quest(db, quest_id)
    if not quest:
        return None
    if quest_in.title is not None:
        quest.title = quest_in.title
    if quest_in.description is not None:
        quest.description = quest_in.description
    if quest_in.difficulty is not None:
        quest.difficulty = quest_in.difficulty
    db.add(quest)
    await db.commit()
    await db.refresh(quest)
    return quest


async def delete_quest(db: AsyncSession, quest_id: int) -> bool:
    quest = await get_quest(db, quest_id)
    if not quest:
        return False
    await db.delete(quest)
    await db.commit()
    return True

