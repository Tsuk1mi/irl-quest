# Интеграционный тест для CRUD задач
import os
import asyncio
import pytest

from httpx import AsyncClient


@pytest.mark.asyncio
async def test_create_get_list_tasks(monkeypatch):
    # Используем in-memory sqlite для теста
    os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"
    os.environ["REDIS_URL"] = "redis://localhost:6379/1"

    # импорт приложения после установки переменных окружения
    from app.main import app

    async with AsyncClient(app=app, base_url="http://test") as ac:
        # Регистрируем пользователя
        reg_payload = {"email": "test@irl.local", "username": "testuser", "password": "testpass"}
        r = await ac.post("/api/v1/auth/register", json=reg_payload)
        assert r.status_code == 201

        # Получаем токен
        form = {"username": reg_payload["email"], "password": reg_payload["password"]}
        r = await ac.post("/api/v1/auth/token", data=form)
        assert r.status_code == 200
        token = r.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # Создать задачу
        payload = {"title": "Тестовая задача", "description": "Описание теста"}
        r = await ac.post("/api/v1/tasks/", json=payload, headers=headers)
        assert r.status_code == 201
        data = r.json()
        assert data["title"] == payload["title"]
        task_id = data["id"]

        # Получить задачу
        r = await ac.get(f"/api/v1/tasks/{task_id}", headers=headers)
        assert r.status_code == 200
        data = r.json()
        assert data["id"] == task_id

        # Листинг
        r = await ac.get("/api/v1/tasks/", headers=headers)
        assert r.status_code == 200
        arr = r.json()
        assert isinstance(arr, list)
        assert any(t["id"] == task_id for t in arr)
