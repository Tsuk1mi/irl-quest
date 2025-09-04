# ADR-0001: Выбор технологического стека

## Статус
Принято

## Контекст
IRL Quest требует современный, расширяемый и независимый стек для масштабируемости, интеграции ML/AI и мобильной разработки.

## Решение
- Backend: Python (FastAPI, Celery)
- Mobile: Kotlin (Android)
- Database: PostgreSQL + pgvector, Redis
- Инфраструктура: Docker, Kubernetes, Terraform
- AI/ML: локальные ML-модели, интеграция RAG через pgvector
- CI/CD: GitHub Actions

## Последствия
Стек способствует гибкости, независимости от внешних AI-провайдеров и быстрому развитию функционала.