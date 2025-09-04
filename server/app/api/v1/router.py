from fastapi import APIRouter

from app.api.v1 import tasks, auth, quests, users

router = APIRouter()

# Подключаем маршруты для задач
router.include_router(tasks.router, prefix="/tasks", tags=["tasks"])
# Подключаем маршруты для аутентификации
router.include_router(auth.router, prefix="/auth", tags=["auth"])
# Подключаем маршруты для квестов
router.include_router(quests.router, prefix="/quests", tags=["quests"])
# Подключаем маршруты для пользователей
router.include_router(users.router, prefix="/users", tags=["users"])
