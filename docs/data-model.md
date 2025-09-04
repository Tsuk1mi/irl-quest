# Data Model IRL Quest

## Основные сущности
- User
- Quest / Task
- Boss / Progress
- FocusSession
- StudyDoc / Test / Question
- Party / Raid
- GeoProof / Loot / Notification

## Диаграмма
_Добавьте схему моделей и связей (ER-диаграмму)._

## Принципы
- Чёткая типизация (pydantic, DTO)
- Миграции через Alembic
- Векторное хранение для поиска в pgvector