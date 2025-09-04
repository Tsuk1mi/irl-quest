FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1
WORKDIR /app

# Устанавливаем системные зависимости для сборки бинарных модулей
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Копируем метаданные и устанавливаем зависимости
COPY pyproject.toml pyproject.toml
COPY setup.cfg setup.cfg 2>/dev/null || true

RUN pip install --upgrade pip setuptools wheel
# Устанавливаем основные зависимости и dev extras (alembic для миграций в dev образе)
RUN pip install ".[dev]"

# Копируем код приложения
COPY . /app

# Экспонируем порт
EXPOSE 8000

# Команда запуска (в dev можно заменить на --reload)
CMD ["uvicorn", "server.app.main:app", "--host", "0.0.0.0", "--port", "8000"]
