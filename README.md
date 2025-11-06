#  IRL Quest - Превратите жизнь в приключение!

> **RPG task-менеджер с системой наград, уровнями и квестами**

Мобильное приложение, которое превращает скучные задачи в эпические приключения в стиле Dungeons & Dragons.

**Версия**: 2.1.0  
**Статус**: Production Ready  
**БД**: PostgreSQL 15+

---

##  Особенности

-  **Система прогрессии**: Выполняйте задачи → получайте опыт → повышайте уровень
-  **Награды**: XP и золото за каждую задачу
-  **Квесты**: Группировка задач в эпические приключения
-  **ML-генерация**: Превращение обычных дел в D&D квесты
-  **ИИ**: Автоопределение сложности и тегов
-  **RPG система**: 4 класса, 4 расы, D&D характеристики
-  **Система кубиков**: D4-D20 броски
- ️ **Геолокация**: AR маркеры и геозоны
-  **Мультиплеер**: WebSocket кооператив
-  **На русском**: Полная локализация

---

##  Технологический стек

### Backend (Rust)
- **Web**: Axum 0.7 (async)
- **БД**: PostgreSQL 15+ (SQLx)
- **Auth**: JWT + Refresh tokens, OAuth2, MFA
- **ML**: Ollama интеграция
- **Cache**: Redis (опционально)
- **Endpoints**: 70+

### Mobile (Android)
- **Язык**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Архитектура**: MVVM + Repository
- **Min SDK**: Android 7.0 (API 24)

---

##  Быстрый старт

### 1️⃣ Настройка конфигурации

```bash
# Скопируйте .env.example в .env
cp .env.example .env

# Отредактируйте .env (настройте DATABASE_URL и JWT_SECRET)
```

**Важно**: Весь проект использует **один** `.env` файл в корне!

### 2️⃣ Запуск через Docker (Рекомендуется)

```bash
# Запустить весь стек (PostgreSQL + Redis + Server + Ollama + Monitoring)
docker-compose up -d

# Проверка
curl http://localhost:8003/health
```

**Доступно**:
-  API Server: http://localhost:8003
-  PostgreSQL: localhost:5432
-  Redis: localhost:6379
-  Ollama: http://localhost:11434
-  Prometheus: http://localhost:9090
-  Grafana: http://localhost:3000

### 3️⃣ Локальный запуск сервера

```powershell
# 1. Создать базу PostgreSQL
psql -U postgres -c "CREATE DATABASE irl_quest;"

# 2. Запустить сервер
cd server-rust
cargo run --release

# Сервер на http://localhost:8003
```

При запуске автоматически:
- 🔌 Подключится к PostgreSQL
- 📦 Применит все миграции
- 🌱 Создаст тестового пользователя (testuser / password)
- 🚀 Запустится на порту 8003

### 4️⃣ Мобильное приложение

```bash
# Сборка APK
cd mobile
./gradlew assembleDebug

# APK: mobile/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📚 Документация

### 🎯 Быстрые ссылки
- **[Конфигурация сервера](server-rust/CONFIG.md)** - все параметры .env
- **[Техническая документация](docs/TECHNICAL_DOCS.md)** - API и архитектура
- **[Резюме реализации](РЕЗЮМЕ_РЕАЛИЗАЦИИ.md)** - текущий статус (85%)

### Для пользователей
- [Руководство пользователя](docs/USER_GUIDE.md)
- [Политика конфиденциальности](docs/PRIVACY_POLICY_RU.md)
- [Пользовательское соглашение](docs/USER_AGREEMENT_RU.md)

### Для разработчиков
- [ML Endpoints](ML_ENDPOINTS.md) - описание ML API
- [Финальная реализация](ФИНАЛЬНАЯ_РЕАЛИЗАЦИЯ.md) - детальный отчет

---

## 🎯 API Endpoints (70+)

### Аутентификация
- `POST /api/auth/register` - Регистрация
- `POST /api/auth/login` - Вход
- `POST /api/auth/refresh` - Обновление токена
- `POST /api/auth/oauth/login` - OAuth2 вход
- `GET /api/auth/mfa/setup` - Настройка MFA
- `GET /api/auth/sessions` - Активные сессии

### Квесты
- `GET /api/quests` - Список квестов
- `POST /api/quests` - Создать квест
- `GET /api/quests/:id` - Детали квеста
- `PUT /api/quests/:id` - Обновить квест
- `DELETE /api/quests/:id` - Удалить квест

### ML Inference
- `POST /api/ml/tags` - Определить теги
- `POST /api/ml/difficulty` - Оценить сложность
- `POST /api/ml/transform` - ToDo → Quest
- `POST /api/ml/recommendations` - Персональные рекомендации

### Персонаж
- `GET /api/character/profile` - Профиль персонажа
- `POST /api/character/select` - Выбрать класс и расу
- `POST /api/character/level-up` - Повысить уровень
- `GET /api/character/classes` - Доступные классы

### Кубики (D&D)
- `POST /api/dice/roll` - Бросить кубик
- `POST /api/dice/skill-check` - Проверка навыка
- `GET /api/dice/types` - Типы кубиков
- `GET /api/dice/skills` - Список навыков

### Геолокация & AR
- `POST /api/geo/zones` - Создать геозону
- `POST /api/geo/check` - Проверить локацию
- `POST /api/ar/process-image` - Обработать AR изображение

### Другие
- `GET /health` - Проверка здоровья
- `GET /api/config` - Конфигурация для клиента
- `WS /ws` - WebSocket соединение

**Всего**: 70+ endpoints

Полный список: [docs/TECHNICAL_DOCS.md](docs/TECHNICAL_DOCS.md)

---

## 🔧 Конфигурация (.env)

**Единый файл** `.env` в корне проекта:

```env
# База данных
DATABASE_URL=postgres://postgres:tsukimi@localhost:5432/irl_quest

# Сервер
PORT=8003
JWT_SECRET=your-secret-here

# Функции
ENABLE_MULTIPLAYER=true
ENABLE_AR=false

# ML
ML_BASE_URL=http://localhost:11434
```

Подробнее: [server-rust/CONFIG.md](server-rust/CONFIG.md)

---

## 🏗️ Структура проекта

```
irl-quest/
├── .env.example          # 👈 ЕДИНЫЙ конфиг для всего проекта
├── .env                  # 👈 Создайте из .env.example
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
│   ├── migrations/       # PostgreSQL миграции (11 файлов)
│   └── Cargo.toml
│
├── mobile/               # Android приложение
│   └── app/src/main/
│
├── docs/                 # Документация
│   ├── TECHNICAL_DOCS.md
│   ├── USER_GUIDE.md
│   └── ...
│
└── infra/                # Infrastructure
    ├── kubernetes/
    ├── prometheus/
    └── terraform/
```

---

## 🎮 Реализованные системы

### ✅ Основные (85%)
- 🔐 JWT + Refresh + OAuth2 + MFA аутентификация
- 📊 PostgreSQL с 11 миграциями
- 🤖 ML Inference (теги, сложность, генерация)
- ⚡ Rate limiting & IP-blocking
- 💎 Reward Engine с модификаторами
- 🎭 Система персонажей (классы, расы, статы)
- 🎲 D&D Dice система (d4-d20)
- 🔌 WebSocket мультиплеер
- 🗺️ Геолокация и AR
- 📈 Автогенерация квестов
- 🐳 Docker + Kubernetes готовность
- 📜 Соответствие 152-ФЗ РФ

### ⏳ В разработке
- Мобильное приложение (Kotlin/Compose)
- Полная RAG система
- Push-уведомления
- Achievement tracking UI

---

## 🧪 Тестирование

```bash
# Backend тесты
cd server-rust
cargo test

# Проверка здоровья
curl http://localhost:8003/health

# Тестовый пользователь
# Username: testuser
# Password: password
```

---

## 📦 Production готовность

### ✅ Готово
- Docker images
- Kubernetes manifests
- Prometheus метрики
- Grafana дашборды
- Health checks
- Автомиграции БД
- Rate limiting
- CORS настраиваемый

### ⚠️ Перед деплоем
1. Измените `JWT_SECRET` на безопасный
2. Настройте `CORS_ORIGIN` для вашего домена
3. Установите `ENABLE_MFA=true`
4. Настройте TLS/HTTPS
5. Настройте бэкапы PostgreSQL

---

## 🔄 Миграция на PostgreSQL (06.11.2025)

**Завершено**: ✅ SQLite полностью удален

**Что изменилось**:
- 🗄️ БД: SQLite → PostgreSQL 15+
- 🔧 SQL: обновлен синтаксис (? → $1, $2, ...)
- 📦 Зависимости: убрана sqlite feature
- 📝 Документация: полностью обновлена
- ⚙️ Конфиг: единый .env в корне проекта

**Как запустить**:
1. Скопируйте `.env.example` → `.env`
2. Создайте БД: `psql -U postgres -c "CREATE DATABASE irl_quest;"`
3. Запустите: `cd server-rust && cargo run --release`

---

## 🗺️ Roadmap

### Version 2.1.0 (Текущая) ✅
- [x] PostgreSQL миграция
- [x] 70+ API endpoints
- [x] ML Inference
- [x] OAuth2 + MFA
- [x] WebSocket
- [x] Dice & Character системы
- [x] Геолокация & AR
- [x] Docker & Kubernetes

### Version 2.2 (Q1 2026)
- [ ] Полная RAG реализация
- [ ] TLS/HTTPS
- [ ] Achievement UI
- [ ] Skill tree визуализация
- [ ] Advanced ML models

### Version 3.0 (Q2 2026)
- [ ] Гильдии
- [ ] Telegram/Discord бот
- [ ] PWA версия
- [ ] Seasonal events

---

## 🤝 Вклад в проект

Приветствуются pull requests! Пожалуйста:
1. Форкните репозиторий
2. Создайте feature ветку
3. Коммитьте изменения
4. Пушьте в ветку
5. Откройте Pull Request

---

## 📞 Поддержка

- 📖 [Документация](docs/)
- 🐛 [Issues](issues/)
- 💬 [Discussions](discussions/)

---

## 📄 Лицензия

MIT License

---

## 🎉 Благодарности

- Rust community
- Axum framework
- SQLx maintainers
- Jetpack Compose team
- Ollama project

---

**Превратите жизнь в приключение! 🏰⚔️**
