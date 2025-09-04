# Архитектура IRL Quest

## Основные компоненты
- Backend (FastAPI, Celery)
- Mobile (Android, Kotlin)
- AI Gateway: локальные ML-модели и RAG
- Векторная база данных: PostgreSQL + pgvector
- Инфраструктура: Docker, Kubernetes, Terraform
- Кэш и очереди: Redis

## Взаимодействие
Мобильное приложение общается с backend через REST и WebSocket API. Backend управляет бизнес-логикой, хранит данные, интегрируется с локальными ML-модулями и pgvector для поиска и генерации.

## Диаграмма
_Добавьте схему архитектуры (например, через mermaid или PNG)._