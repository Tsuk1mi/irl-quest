from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class QuestBase(BaseModel):
    title: str
    description: Optional[str] = None
    difficulty: Optional[int] = 1


class QuestCreate(QuestBase):
    pass


class QuestUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    difficulty: Optional[int] = None


class QuestOut(QuestBase):
    id: int
    created_at: datetime
    owner_id: Optional[int] = None

    class Config:
        orm_mode = True

