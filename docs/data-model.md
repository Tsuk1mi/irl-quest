# Data Model IRL Quest

## Основные сущности
- User
- Quest / Task
- Boss / Progress
- FocusSession
- StudyDoc / Test / Question
- Party / Raid
- GeoProof / Loot / Notification

## ER-диаграмма (описание)
- User <--> Quest (1:M)
- Quest <--> Task (1:M)
- Task <--> Progress (1:1)
- User <--> FocusSession (1:M)
- User <--> StudyDoc/Test (1:M)
- Party <--> User (M:N)
- Raid <--> Party (1:M)
- Task/Quest <--> GeoProof (1:M)
- User <--> Notification (1:M)

## Принципы
- Чёткая типизация (pydantic, DTO, Rust structs)
- Миграции через Alembic (Python) и sqlx (Rust)
- Векторное хранение для поиска в pgvector
- Используйте nullable поля для необязательных связей


