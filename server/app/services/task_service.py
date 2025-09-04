from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.task_repository import (
    list_tasks as repo_list_tasks,
    get_task as repo_get_task,
    create_task as repo_create_task,
    update_task as repo_update_task,
    delete_task as repo_delete_task,
)
from app.models import Task
from app.schemas.task import TaskCreate, TaskUpdate


async def list_tasks_for_user(db: AsyncSession, user_id: int, skip: int = 0, limit: int = 100) -> List[Task]:
    return await repo_list_tasks(db, skip=skip, limit=limit, owner_id=user_id)


async def get_task_for_user(db: AsyncSession, user_id: int, task_id: int) -> Optional[Task]:
    task = await repo_get_task(db, task_id)
    if not task or task.owner_id != user_id:
        return None
    return task


async def create_task_for_user(db: AsyncSession, user_id: int, task_in: TaskCreate) -> Task:
    return await repo_create_task(db, task_in, owner_id=user_id)


async def update_task_for_user(db: AsyncSession, user_id: int, task_id: int, task_in: TaskUpdate) -> Optional[Task]:
    task = await repo_get_task(db, task_id)
    if not task or task.owner_id != user_id:
        return None
    return await repo_update_task(db, task_id, task_in)


async def delete_task_for_user(db: AsyncSession, user_id: int, task_id: int) -> bool:
    task = await repo_get_task(db, task_id)
    if not task or task.owner_id != user_id:
        return False
    return await repo_delete_task(db, task_id)

