# New file: repository for Task
from typing import List, Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Task
from app.schemas.task import TaskCreate, TaskUpdate


async def list_tasks(db: AsyncSession, skip: int = 0, limit: int = 100, owner_id: Optional[int] = None) -> List[Task]:
    q = select(Task)
    if owner_id is not None:
        q = q.where(Task.owner_id == owner_id)
    q = q.offset(skip).limit(limit)
    result = await db.execute(q)
    return result.scalars().all()


async def get_task(db: AsyncSession, task_id: int) -> Optional[Task]:
    result = await db.execute(select(Task).where(Task.id == task_id))
    return result.scalar_one_or_none()


async def create_task(db: AsyncSession, task_in: TaskCreate, owner_id: Optional[int] = None) -> Task:
    task = Task(title=task_in.title, description=task_in.description, owner_id=owner_id)
    db.add(task)
    await db.commit()
    await db.refresh(task)
    return task


async def update_task(db: AsyncSession, task_id: int, task_in: TaskUpdate) -> Optional[Task]:
    task = await get_task(db, task_id)
    if not task:
        return None
    if task_in.title is not None:
        task.title = task_in.title
    if task_in.description is not None:
        task.description = task_in.description
    if task_in.completed is not None:
        task.completed = task_in.completed
    db.add(task)
    await db.commit()
    await db.refresh(task)
    return task


async def delete_task(db: AsyncSession, task_id: int) -> bool:
    task = await get_task(db, task_id)
    if not task:
        return False
    await db.delete(task)
    await db.commit()
    return True
