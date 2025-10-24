# 🎮 IRL-Quest

> Gamify your life with D&D-style quests and character progression!

Мобильное приложение, которое превращает повседневные задачи в квесты в стиле Dungeons & Dragons. Зарабатывайте опыт, прокачивайте персонажа и становитесь легендой реального мира!

## ✨ Особенности

- 🎭 **D&D система характеристик** - Сила, Интеллект, Харизма, Ловкость, Выносливость, Мудрость
- ⚔️ **Фэнтези тема** - Таверна героя, доска квестов, карта мира
- 📈 **Система прогрессии** - Уровни, опыт, золото, достижения
- 🗺️ **Интерактивная карта** - Зоны жизни (Работа, Обучение, Здоровье, Отношения)
- 🎁 **Анимированные награды** - Визуальная обратная связь при выполнении квестов
- 🤖 **AI-ассистент** - Помощь в создании квестов и мотивация

## 🚀 Быстрый старт

### 1. Запустить сервер

```powershell
# Собрать и запустить
.\scripts\Run.ps1
```

Сервер запустится на `http://0.0.0.0:8003`

### 2. Установить мобильное приложение

**APK собран и находится здесь:**
```
mobile\app\build\outputs\apk\debug\app-debug.apk
```

**Установка:**
- Скопируйте `app-debug.apk` на телефон
- Откройте файл на телефоне
- Разрешите установку из неизвестных источников
- Установите

**ИЛИ через ADB:**
```powershell
adb install mobile\app\build\outputs\apk\debug\app-debug.apk
```

### 3. Войти в приложение

- Username: `testuser`
- Password: `password`

## ⚙️ Требования

### Backend
- Rust 1.70+
- PostgreSQL 14+ (database: `irl_quest`, user: `postgres`, password: `tsukimi`)
- Порт 8003

### Mobile
- Android 8.0+ (API 26+)
- Kotlin 1.9.10
- Jetpack Compose

### Сеть
- Wi-Fi сеть должна быть настроена как "Частная" (не "Публичная")
- Файрвол должен разрешать порт 8003

## 📜 Скрипты PowerShell

| Скрипт | Описание |
|--------|----------|
| `Build.ps1` | Собрать Rust сервер |
| `Start.ps1` | Запустить сервер |
| `Stop.ps1` | Остановить сервер |
| `Run.ps1` | Собрать и запустить сервер |
| `Test.ps1` | Проверить API сервера |
| `SETUP_FIREWALL.bat` | Настроить файрвол (запустить от администратора) |
| `FIX_NETWORK_PROFILE.bat` | Изменить профиль сети на "Частная" |

## 🏗️ Архитектура

```
irl-quest/
├── server-rust/              # Backend (Rust + Axum + PostgreSQL)
│   ├── src/
│   │   ├── handlers/         # API endpoints
│   │   ├── models/           # Data models (User, Quest, Task)
│   │   ├── services/         # Business logic
│   │   ├── middleware/       # Auth, CORS
│   │   └── ml/               # ML stubs
│   └── migrations/           # Database migrations
│
├── mobile/                   # Android app (Kotlin + Compose)
│   └── app/src/main/
│       ├── java/com/irlquest/app/
│       │   ├── feature/      # UI screens (Home, Hero, Quests, WorldMap)
│       │   ├── data/         # Repositories, Network, DTOs
│       │   └── ui/           # Theme, Navigation
│       └── res/              # Resources
│
└── scripts/                  # PowerShell & SQL scripts
```

## 🎨 Технологии

### Backend
- **Rust** - Axum, Tokio, SQLx
- **PostgreSQL** - База данных с D&D характеристиками
- **JWT** - Аутентификация
- **Argon2** - Хеширование паролей

### Mobile
- **Kotlin 1.9.10** - Язык программирования
- **Jetpack Compose** - Современный UI фреймворк
- **Material3** - Дизайн система
- **Retrofit** - HTTP клиент
- **Kotlinx Serialization** - JSON парсинг

## 🔑 API Endpoints

### Публичные
- `POST /api/v1/auth/register` - Регистрация
- `POST /api/v1/auth/login` - Вход

### Защищенные (требуют JWT)
- `GET /api/v1/auth/me` - Получить текущего пользователя
- `GET /api/v1/quests` - Список квестов
- `POST /api/v1/quests` - Создать квест
- `GET /api/v1/tasks` - Список задач
- `POST /api/v1/tasks` - Создать задачу
- `POST /api/v1/tasks/:id/complete` - Завершить задачу

## 🐛 Устранение проблем

### Мобильное приложение не подключается

1. **Настройте файрвол (от администратора):**
   ```
   Правой кнопкой на scripts\SETUP_FIREWALL.bat → "Запуск от имени администратора"
   ```

2. **Убедитесь, что профиль сети "Частная":**
   ```
   Правой кнопкой на scripts\FIX_NETWORK_PROFILE.bat → "Запуск от имени администратора"
   ```

3. **Проверьте IP в `mobile/build.gradle.kts`:**
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.67:8003/api/v1/\"")
   ```
   Замените на IP вашего компьютера (найдите через `ipconfig`)

### Сервер не запускается

```powershell
# Остановите старые процессы
.\scripts\Stop.ps1

# Проверьте, что PostgreSQL запущен
# Убедитесь, что база данных irl_quest существует

# Запустите снова
.\scripts\Run.ps1
```

## 📝 Разработка

### Пересборка всего проекта

**Backend:**
```powershell
.\scripts\Build.ps1
.\scripts\Start.ps1
```

**Mobile:**
```powershell
cd mobile
.\gradlew clean assembleDebug
```

### Добавление нового экрана

1. Создать файл в `mobile/app/src/main/java/com/irlquest/app/feature/`
2. Добавить в `ui/navigation/MainScreen.kt`
3. Обновить BottomNavItem если нужно

### Добавление нового API endpoint

1. Создать handler в `server-rust/src/handlers/`
2. Добавить route в `server-rust/src/routes.rs`
3. Обновить DTO в мобильном приложении

## 🔑 Учетные данные

- **Тестовый пользователь:** `testuser` / `password`
- **База данных:** `irl_quest`
- **PostgreSQL:** `postgres` / `tsukimi`
- **Порт:** 8003

## 📊 Статус проекта

- ✅ Backend работает
- ✅ Database настроена
- ✅ Mobile собирается
- ✅ Network настроена
- ✅ Firewall настроен
- ⏳ ML система (в разработке - используются заглушки)

## 📚 Дополнительная документация

- `DONE.md` - Список выполненных задач
- `scripts/README.md` - Документация по скриптам
- `docs/` - Детальная архитектурная документация

## 🤝 Вклад

Проект в активной разработке. Приветствуются pull requests!

## 📄 Лицензия

MIT License

## 👤 Автор

Tsukimi

---

**Статус:** 🟢 Готов к использованию  
**Версия:** 0.1.0  
**Последнее обновление:** 2025-10-24

**APK:** `mobile\app\build\outputs\apk\debug\app-debug.apk` ✅
