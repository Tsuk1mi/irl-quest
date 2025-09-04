# IRL Quest — Backend (Dev)

Инструкция для локальной разработки backend.

Требования
- Docker и docker-compose
- Python 3.10+ (если запускать локально без Docker)

Запуск в Docker (рекомендуемый)
1. Перейдите в директорию проекта и запустите:

```bash
cd server
docker-compose -f docker-compose.dev.yml up --build
```

2. API будет доступно на http://localhost:8000 (в эмуляторе Android используйте http://10.0.2.2:8000)
3. При старте приложение пытается прогнать alembic миграции и сидировать тестовые данные.

Локальный запуск без Docker
1. Создайте виртуальное окружение и установите зависимости:

```bash
cd server
python -m venv .venv
. .venv/bin/activate  # в Windows: .venv\Scripts\activate
python -m pip install -e ".[dev]"
```

2. Установите переменные окружения (пример для sqlite dev):

```bash
export DATABASE_URL=sqlite+aiosqlite:///./dev.db
export REDIS_URL=redis://localhost:6379/0
```

3. Запустите сервер:

```bash
uvicorn server.app.main:app --reload --port 8000
```

Миграции Alembic
- В dev образе миграции запускаются автоматически при старте приложения.
- Вручную можно прогнать миграции:

```bash
cd server
python -m alembic upgrade head
```

Тесты
- Запустить тесты pytest:

```bash
cd server
pytest -q
```

Полезные эндпоинты
- /api/v1/auth/register — регистрация
- /api/v1/auth/token — получение токена (OAuth2)
- /api/v1/tasks — CRUD задач
- /api/v1/quests — CRUD квестов
- /health, /ready — проверки состояния

Примечание
- В production используйте реальные секреты (SECRET_KEY) и миграции через CI, не создавайте таблицы через create_all.

