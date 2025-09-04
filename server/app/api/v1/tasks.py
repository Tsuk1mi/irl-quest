# New file: CRUD endpoints for tasks
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status, Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.deps import get_db_session, get_current_user
from app.schemas.task import TaskCreate, TaskUpdate, TaskOut
from app.services import task_service
from app.models.user import User

router = APIRouter()


@router.get("/", response_model=List[TaskOut])
async def list_tasks(skip: int = 0, limit: int = 100, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    return await task_service.list_tasks_for_user(db, current_user.id, skip=skip, limit=limit)


@router.post("/", response_model=TaskOut, status_code=status.HTTP_201_CREATED)
async def create_task(task_in: TaskCreate, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    return await task_service.create_task_for_user(db, current_user.id, task_in)


@router.get("/{task_id}", response_model=TaskOut)
async def get_task(task_id: int, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    task = await task_service.get_task_for_user(db, current_user.id, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task


@router.put("/{task_id}", response_model=TaskOut)
async def update_task(task_id: int, task_in: TaskUpdate, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    task = await task_service.update_task_for_user(db, current_user.id, task_id, task_in)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task


@router.delete("/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_task(task_id: int, db: AsyncSession = Depends(get_db_session), current_user: User = Depends(get_current_user)):
    ok = await task_service.delete_task_for_user(db, current_user.id, task_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Task not found")
    return Response(status_code=status.HTTP_204_NO_CONTENT)
