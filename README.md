# IRL Quest - RPG Task Manager

Превратите свою жизнь в приключение с системой наград, уровнями и квестами в стиле Dungeons & Dragons.

**Версия**: 2.2.0  
**Статус**: Production Ready  
**БД**: PostgreSQL 15+

---

## Особенности

- **Система прогрессии**: Выполняйте задачи → получайте опыт → повышайте уровень
- **Награды**: XP и золото за каждую задачу
- **Квесты**: Группировка задач в эпические приключения
- **ML-генерация**: Превращение обычных дел в D&D квесты
- **RPG система**: 4 класса, 4 расы, D&D характеристики
- **Система кубиков**: D4-D20 броски
- **Геолокация**: AR маркеры и геозоны
- **Мультиплеер**: WebSocket кооператив
- **Карта мира**: Интерактивная карта с квестами в значимых местах
- **На русском**: Полная локализация

---

## Технологический стек

### Backend (Rust)
- **Web**: Axum 0.7 (async)
- **БД**: PostgreSQL 15+ (SQLx)
- **Auth**: JWT + Refresh tokens, OAuth2, MFA
- **ML**: Ollama интеграция для генерации квестов
- **Cache**: Redis (опционально)
- **Endpoints**: 70+

### Mobile (Kotlin Multiplatform)
- **Язык**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Архитектура**: MVVM + Repository
- **KMP**: Shared модуль для Android и iOS
- **Min SDK**: Android 7.0 (API 24)

---

## Быстрый старт

### Настройка конфигурации

```bash
# Скопируйте .env.example в .env
cp .env.example .env

# Отредактируйте .env (настройте DATABASE_URL и JWT_SECRET)
```

### Запуск сервера

```bash
cd server-rust
cargo run --bin main_full
```

### Запуск мобильного приложения

```bash
cd mobile
./gradlew installDebug
```

---

## API Endpoints

### Аутентификация
- `POST /api/v1/auth/register` - Регистрация
- `POST /api/v1/auth/login` - Вход
- `GET /api/v1/auth/me` - Текущий пользователь

### Квесты
- `GET /api/v1/quests` - Список квестов
- `POST /api/v1/quests` - Создать квест
- `GET /api/v1/quests/{id}` - Детали квеста
- `POST /api/v1/quests/{id}` - Обновить квест

### Задачи
- `GET /api/v1/tasks` - Список задач
- `POST /api/v1/tasks` - Создать задачу
- `POST /api/v1/tasks/{id}/complete` - Завершить задачу

### ML Inference
- `POST /api/v1/ml/tags` - Определить теги
- `POST /api/v1/ml/transform` - Трансформировать в квест
- `POST /api/v1/ml/recommendations` - Рекомендации

### Геолокация
- `POST /api/v1/geo/zones` - Создать геозону
- `POST /api/v1/geo/check` - Проверить локацию

### Мультиплеер
- `GET /api/v1/guilds` - Список гильдий
- `POST /api/v1/guilds` - Создать гильдию
- `GET /api/v1/coop/missions` - Кооп-миссии

**Всего**: 70+ endpoints

Полный список: [docs/TECHNICAL_DOCS.md](docs/TECHNICAL_DOCS.md)

---

## Конфигурация (.env)

**Единый файл** `.env` в корне проекта:

```env
# База данных
DATABASE_URL=postgres://postgres:password@localhost:5432/irl_quest

# Сервер
PORT=8003
JWT_SECRET=your-secret-here

# Функции
ENABLE_MULTIPLAYER=true
ENABLE_AR=false

# ML
ML_BASE_URL=http://localhost:11434
```

---

## Структура проекта

```
irl-quest/
├── .env.example          # ЕДИНЫЙ конфиг для всего проекта
├── .env                  # Создайте из .env.example
├── docker-compose.yml    # Полный стек
├── README.md             # Этот файл
│
├── server-rust/          # Backend (Rust)
│   ├── src/
│   │   ├── handlers/     # 70+ endpoints
│   │   ├── services/     # Бизнес-логика
│   │   ├── models/       # Модели данных
│   │   ├── middleware/   # Auth, CORS, Rate limit
│   │   └── main_full.rs
│   ├── migrations/       # PostgreSQL миграции
│   └── Cargo.toml
│
├── mobile/               # Mobile приложение (KMP)
│   ├── app/              # Android приложение
│   └── shared/           # Kotlin Multiplatform модуль
│
└── docs/                 # Документация
    ├── TECHNICAL_DOCS.md
    └── USER_GUIDE.md
```

---

## Разработка

### Требования

- Rust 1.70+
- PostgreSQL 15+
- Kotlin 1.9+
- Android SDK 24+
- Gradle 8.0+

### Запуск тестов

```bash
# Backend
cd server-rust
cargo test

# Mobile
cd mobile
./gradlew test
```

## Контакты

Для вопросов и предложений создайте issue в репозитории.

