# Интеграционный тест полного потока (auth + tasks + quests)
import os
import pytest

from httpx import AsyncClient


@pytest.mark.asyncio
async def test_full_flow():
    os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"
    os.environ["REDIS_URL"] = "redis://localhost:6379/1"

    from app.main import app

    async with AsyncClient(app=app, base_url="http://test") as ac:
        # Регистрация
        reg = {"email": "flow@irl.local", "username": "flowuser", "password": "flowpass"}
        r = await ac.post("/api/v1/auth/register", json=reg)
        assert r.status_code == 201

        # Токен
        r = await ac.post("/api/v1/auth/token", data={"username": reg["email"], "password": reg["password"]})
        assert r.status_code == 200
        token = r.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # Create task
        task_payload = {"title": "Flow task", "description": "desc"}
        r = await ac.post("/api/v1/tasks/", json=task_payload, headers=headers)
        assert r.status_code == 201
        task = r.json()
        task_id = task["id"]

        # Update task - mark completed
        r = await ac.put(f"/api/v1/tasks/{task_id}", json={"completed": True}, headers=headers)
        assert r.status_code == 200
        assert r.json()["completed"] is True

        # Delete task
        r = await ac.delete(f"/api/v1/tasks/{task_id}", headers=headers)
        assert r.status_code == 204

        # Create quest
        quest_payload = {"title": "Flow quest", "description": "qdesc", "difficulty": 2}
        r = await ac.post("/api/v1/quests/", json=quest_payload, headers=headers)
        assert r.status_code == 201
        quest = r.json()
        qid = quest["id"]

        # Get quest
        r = await ac.get(f"/api/v1/quests/{qid}", headers=headers)
        assert r.status_code == 200
        assert r.json()["title"] == quest_payload["title"]

        # Update quest
        r = await ac.put(f"/api/v1/quests/{qid}", json={"difficulty": 3}, headers=headers)
        assert r.status_code == 200
        assert r.json()["difficulty"] == 3

        # Delete quest
        r = await ac.delete(f"/api/v1/quests/{qid}", headers=headers)
        assert r.status_code == 204

