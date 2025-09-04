# IRL Quest

**IRL Quest** — платформа для геймифицированной продуктивности, обучения и совместных челленджей с использованием локальных ML-решений и векторной базы данных pgvector.

## Возможности

- Квесты и задачи с прогрессом и наградами
- Битвы с "боссами" — преодоление сложных целей
- Фокус-сессии (Pomodoro, статистики)
- Генерация тестов и материалов с помощью локальных ML и RAG
- Учебные режимы: тесты, документы, индивидуальная подготовка
- Кооператив: группы, рейды, совместные задания
- AR и геолокация для подтверждения достижений
- Система наград и инвентаря

## Технологии

- Backend: Python (FastAPI, Celery), PostgreSQL + pgvector, Redis
- Mobile: Kotlin (Android, Jetpack Compose)
- Инфраструктура: Docker, Kubernetes, Terraform
- AI: Локальные ML-модели, pgvector (RAG)
- CI/CD: GitHub Actions

## Быстрый старт

### Backend
```bash
cd server
make dev-up
```

### Мобильное приложение
Откройте проект в Android Studio, соберите и запустите на устройстве.

## Документация

- [Архитектура](docs/architecture.md)
- [Backend](docs/backend.md)
- [Мобильное приложение](docs/mobile.md)
- [AI-промпты](docs/ai-prompts.md)
- [RAG-дизайн](docs/rag-design.md)
- [Модель данных](docs/data-model.md)
- [Безопасность](docs/security-privacy.md)

## Вклад

Смотрите [CONTRIBUTING.md](CONTRIBUTING.md).

## Лицензия

[MIT](LICENSE)