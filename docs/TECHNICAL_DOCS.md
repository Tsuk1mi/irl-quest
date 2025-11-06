# 🔧 IRL Quest - Техническая документация

## 📋 Оглавление
1. [Архитектура приложения](#архитектура)
2. [Бизнес-процессы](#бизнес-процессы)
3. [Компоненты системы](#компоненты)
4. [API и интеграции](#api)
5. [База данных](#база-данных)
6. [Развёртывание](#развёртывание)

---

## 🏗️ Архитектура приложения {#архитектура}

### Общая структура

```
┌─────────────────────────────────────────────┐
│           МОБИЛЬНОЕ ПРИЛОЖЕНИЕ              │
│         (Android / Jetpack Compose)         │
└────────────┬────────────────────────────────┘
             │ REST API
             │
┌────────────▼────────────────────────────────┐
│          BACKEND СЕРВЕР (Rust)              │
│  ┌──────────────────────────────────────┐   │
│  │  Axum Web Framework                  │   │
│  ├──────────────────────────────────────┤   │
│  │  • Auth Handler (JWT)                │   │
│  │  • Quest Handler                     │   │
│  │  • Task Handler                      │   │
│  │  • ML Handler (RAG)                  │   │
│  │  • User Handler                      │   │
│  └──────────────────────────────────────┘   │
└────────────┬────────────────────────────────┘
             │
             ├──────────┬──────────────────┐
             │          │                  │
        ┌────▼─────┐ ┌───▼────┐      ┌─────▼─────┐
        │PostgreSQL│ │  Redis │      │  Ollama   │
        │    DB    │ │ Cache  │      │  (ML/AI)  │
        └──────────┘ └────────┘      └───────────┘
```

### Технологический стек

#### Мобильное приложение:
- **Язык**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Архитектура**: MVVM + Repository Pattern
- **Навигация**: Compose Navigation
- **State**: StateFlow + ViewModel
- **Сеть**: Retrofit + OkHttp
- **Логирование**: Timber

#### Бэкенд:
- **Язык**: Rust
- **Фреймворк**: Axum
- **БД**: PostgreSQL 15+ (SQLx)
- **Кэш**: Redis (опционально)
- **ML**: Ollama для embeddings
- **Auth**: JWT токены + Refresh tokens

---

## БИЗНЕС-ПРОЦЕССЫ {#бизнес-процессы}

### 1. Регистрация и аутентификация пользователя

#### 1.1. Процесс регистрации

**Входные данные**:
- Email (обязательно, уникальный, валидация RFC 5322)
- Username (обязательно, уникальный, 3-50 символов, буквы/цифры/подчеркивание)
- Password (обязательно, минимум 8 символов, проверка сложности)
- Timezone (опционально, по умолчанию UTC)

**Этапы обработки**:

1. **Валидация на клиенте**:
   - Проверка формата email через regex
   - Проверка длины username (3-50 символов)
   - Проверка сложности пароля (минимум 8 символов)
   - Валидация timezone (IANA формат)

2. **Отправка на сервер** (POST /api/auth/register):
   ```json
   {
     "email": "user@example.com",
     "username": "player123",
     "password": "SecurePass123",
     "timezone": "Europe/Moscow"
   }
   ```

3. **Серверная валидация**:
   - Проверка уникальности email в базе данных
   - Проверка уникальности username в базе данных
   - Валидация формата email (validator crate)
   - Проверка минимальной длины пароля (конфигурируемо через PASSWORD_MIN_LENGTH)
   - Санитизация входных данных

4. **Хеширование пароля**:
   - Используется Argon2id алгоритм
   - Генерация случайной соли (SaltString)
   - Cost factor: default (configurable)
   - Результат: хеш формата PHC string

5. **Создание пользователя в БД**:
   ```sql
   INSERT INTO users (
       email, username, hashed_password, is_active,
       level, experience, gold,
       strength, intelligence, charisma, dexterity, constitution, wisdom,
       character_class, character_race,
       timezone, settings, created_at
   ) VALUES ($1, ..., $18)
   RETURNING id, email, username, ...
   ```

6. **Начальные значения**:
   - level: 1
   - experience: 0
   - gold: 100
   - Все характеристики (strength, intelligence, etc): 10
   - character_class: "warrior"
   - character_race: "human"
   - is_active: true
   - settings: {} (пустой JSON)

7. **Генерация JWT токена**:
   - Алгоритм: HS256
   - Payload: user_id, username, email
   - Expiration: JWT_EXPIRATION_HOURS (default: 24 часа)
   - Secret: JWT_SECRET из конфигурации

8. **Генерация Refresh токена**:
   - Генерация случайной строки (UUID v4)
   - Срок действия: REFRESH_TOKEN_EXPIRATION_DAYS (default: 30 дней)
   - Сохранение в таблицу refresh_tokens
   - Привязка к user_id
   - device_info: опционально (User-Agent)

9. **Ответ клиенту**:
   ```json
   {
     "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refresh_token": "550e8400-e29b-41d4-a716-446655440000",
     "token_type": "Bearer",
     "expires_in": 86400,
     "user": {
       "id": 1,
       "email": "user@example.com",
       "username": "player123",
       "level": 1,
       "experience": 0,
       "gold": 100
     }
   }
   ```

10. **Сохранение на клиенте**:
    - Access token → Secure Storage (EncryptedSharedPreferences)
    - Refresh token → Secure Storage
    - User data → Local cache

**Обработка ошибок**:
- 400 Bad Request: невалидные данные (с деталями ошибки)
- 409 Conflict: email или username уже существует
- 500 Internal Server Error: ошибка БД или хеширования

#### 1.2. Процесс входа (Login)

**Входные данные**:
- username (или email)
- password
- device_info (опционально)

**Этапы**:

1. **Поиск пользователя** (SQL):
   ```sql
   SELECT id, email, username, hashed_password, is_active,
          level, experience, gold, ...
   FROM users
   WHERE email = $1 OR username = $1
   ```

2. **Проверка статуса аккаунта**:
   - is_active = false → возврат ошибки "Account disabled"
   - locked_until проверка (если поле существует)
   - failed_login_attempts проверка (защита от bruteforce)

3. **Верификация пароля**:
   - Парсинг PHC хеша из БД
   - Верификация через Argon2::verify_password
   - Время выполнения: ~100-500ms (защита от timing attacks)

4. **Обновление метаданных**:
   ```sql
   UPDATE users 
   SET last_login = NOW(),
       failed_login_attempts = 0
   WHERE id = $1
   ```

5. **Генерация токенов** (аналогично регистрации):
   - JWT access token (24 часа)
   - Refresh token (30 дней)
   - Сохранение refresh token в БД

6. **Логирование события**:
   - IP адрес клиента (из X-Forwarded-For или ConnectInfo)
   - User-Agent
   - Timestamp
   - Успех/неудача
   - (опционально: сохранение в audit_log таблицу)

**Защита от bruteforce**:
- Rate limiting: 60 запросов/минуту (конфигурируемо)
- IP blocking после N неудачных попыток
- Временная блокировка аккаунта после M неудачных попыток

#### 1.3. Обновление токена (Refresh)

**Входные данные**:
- refresh_token (UUID)

**Этапы**:

1. **Поиск токена в БД**:
   ```sql
   SELECT id, user_id, token, expires_at, created_at, revoked, device_info
   FROM refresh_tokens
   WHERE token = $1 AND revoked = false
   ```

2. **Валидация токена**:
   - Существование в БД
   - revoked = false
   - expires_at > NOW()
   - Принадлежность пользователю

3. **Загрузка пользователя**:
   ```sql
   SELECT id, email, username, ... FROM users WHERE id = $1
   ```

4. **Отзыв старого токена**:
   ```sql
   UPDATE refresh_tokens SET revoked = true WHERE token = $1
   ```

5. **Генерация новых токенов**:
   - Новый access token
   - Новый refresh token
   - Сохранение нового refresh token в БД

6. **Возврат токенов клиенту**

**Безопасность**:
- Одноразовое использование refresh токенов (revoked после использования)
- Rotation: каждый refresh создает новую пару токенов
- Возможность отзыва всех токенов пользователя

#### 1.4. Multi-Factor Authentication (MFA)

**Этап 1: Настройка MFA** (GET /api/auth/mfa/setup):

1. Генерация TOTP секрета (32 байта, base32)
2. Создание QR кода с otpauth URI:
   ```
   otpauth://totp/IRL Quest:username?secret=SECRET&issuer=IRL Quest
   ```
3. Возврат QR кода (PNG base64) и секрета
4. Сохранение секрета в БД (временно, до подтверждения)

**Этап 2: Включение MFA** (POST /api/auth/mfa/enable):

1. Пользователь вводит код из приложения (Google Authenticator)
2. Сервер верифицирует код:
   ```rust
   TOTP::new(secret).check_current(code)
   ```
3. При успехе: mfa_enabled = true, mfa_secret сохраняется
4. Генерация recovery codes (10 штук, одноразовые)

**Этап 3: Вход с MFA**:

1. Обычный логин (username + password)
2. Возврат промежуточного ответа: `mfa_required: true`
3. Клиент запрашивает код MFA
4. Верификация кода (POST /api/auth/mfa/verify)
5. При успехе: выдача access и refresh токенов

**Этап 4: Recovery**:
- Использование recovery code вместо TOTP
- После использования recovery code помечается как использованный
- Возможность сгенерировать новые recovery codes

#### 1.5. OAuth2 аутентификация

**Поддерживаемые провайдеры**:
- Google
- Apple (структура готова, требует настройки)

**Процесс (Google OAuth2)**:

1. **Инициация** (клиент):
   - Редирект на Google OAuth consent screen
   - Scope: email, profile, openid
   - redirect_uri: настраиваемый

2. **Callback обработка** (POST /api/auth/oauth/login):
   ```json
   {
     "provider": "google",
     "code": "authorization_code",
     "redirect_uri": "app://callback"
   }
   ```

3. **Обмен кода на токен**:
   - Запрос к Google Token endpoint
   - Получение access_token
   - Получение ID token (JWT)

4. **Получение user info**:
   - Декодирование ID token
   - Извлечение email, name, picture
   - Или запрос к UserInfo endpoint

5. **Поиск существующего аккаунта**:
   ```sql
   SELECT user_id FROM oauth_accounts
   WHERE provider = 'google' AND provider_user_id = $1
   ```

6. **Создание или связывание аккаунта**:
   - Если найден: загрузка user
   - Если нет: создание нового user
   - Генерация username из email
   - Проверка уникальности username
   - Создание записи в oauth_accounts

7. **Выдача токенов**:
   - Генерация JWT access token
   - Генерация refresh token
   - Возврат клиенту

**Таблица oauth_accounts**:
```sql
CREATE TABLE oauth_accounts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(provider, provider_user_id)
)
```
- JWT токен (сохраняется локально)

---

### 2. Создание задач и квестов

#### 2.1. Создание задачи (Task)

**Входные данные**:
- title (обязательно, string, 1-255 символов)
- description (опционально, text)
- difficulty (опционально, integer 1-5, default: auto или 2)
- priority (опционально, enum: low/medium/high/critical, default: medium)
- deadline (опционально, ISO 8601 datetime)
- quest_id (опционально, integer) - привязка к квесту
- tags (опционально, array of strings)

**Этапы обработки на клиенте**:

1. **Открытие формы создания**:
   - Пользователь нажимает кнопку "Добавить задачу"
   - Отображается диалог/экран с формой
   - Поля: title (обязательно), description, deadline picker

2. **Автоопределение параметров**:
   - При вводе текста в title: анализ ключевых слов
   - Определение difficulty на основе текста:
     ```
     Простые слова ("купить", "позвонить") → difficulty 1-2
     Сложные слова ("презентация", "экзамен") → difficulty 4-5
     Длинный текст (>20 слов) → +1 к difficulty
     ```
   - Определение тегов:
     ```
     "купить" → ["shopping"]
     "учеба" → ["study"]
     "спорт" → ["health", "sport"]
     "работа" → ["work"]
     ```

3. **Расчет наград**:
   ```
   XP = difficulty × 10 + priorityBonus
   Gold = difficulty × 10
   
   priorityBonus:
     critical: +20 XP
     high: +10 XP
     medium: +5 XP
     low: +0 XP
   ```

4. **Превью и подтверждение**:
   - Показ рассчитанных наград
   - Возможность изменить difficulty/priority
   - Кнопка "Создать"

5. **Отправка на сервер** (POST /api/tasks):
   ```json
   {
     "title": "Купить продукты",
     "description": "Молоко, хлеб, яйца",
     "difficulty": 2,
     "priority": "medium",
     "deadline": "2025-11-07T18:00:00Z",
     "tags": ["shopping"],
     "quest_id": null
   }
   ```

6. **Серверная обработка**:
   - Валидация всех полей
   - Проверка owner_id из JWT токена
   - Если difficulty не указан: вызов ML для определения
   - Если tags не указаны: вызов ML для тегов
   - Вставка в БД:
     ```sql
     INSERT INTO tasks (
         title, description, difficulty, status, priority,
         deadline, experience_reward, tags, owner_id, quest_id,
         created_at, metadata
     ) VALUES ($1, ..., $12)
     RETURNING id, title, ..., created_at
     ```

7. **Ответ клиенту**:
   ```json
   {
     "id": 123,
     "title": "Купить продукты",
     "description": "Молоко, хлеб, яйца",
     "difficulty": 2,
     "status": "pending",
     "priority": "medium",
     "deadline": "2025-11-07T18:00:00Z",
     "experience_reward": 25,
     "tags": ["shopping"],
     "quest_id": null,
     "owner_id": 1,
     "completed": false,
     "created_at": "2025-11-06T10:00:00Z"
   }
   ```

8. **Обновление UI**:
   - Добавление задачи в локальный список
   - Сортировка по priority и deadline
   - Анимация появления
   - Обновление счетчика задач

**ML интеграция (опционально)**:

Если ENABLE_ML = true, сервер может вызвать:

```rust
// POST /api/ml/difficulty
{
  "text": "Купить продукты",
  "context": "Молоко, хлеб, яйца"
}

Response:
{
  "difficulty": 2,
  "confidence": 0.85,
  "factors": ["simple_task", "shopping"]
}

// POST /api/ml/tags
{
  "text": "Купить продукты"
}

Response:
{
  "tags": ["shopping", "errands"],
  "confidence_scores": {
    "shopping": 0.95,
    "errands": 0.75
  }
}
```

#### 2.2. Выполнение задачи

**Этапы**:

1. **Пользователь отмечает задачу выполненной**:
   - Нажатие на checkbox или свайп
   - Локальное обновление UI (мгновенная обратная связь)
   - Анимация выполнения

2. **Отправка на сервер** (PUT /api/tasks/:id):
   ```json
   {
     "completed": true,
     "completion_proof": "optional text or image url"
   }
   ```

3. **Серверная обработка**:
   ```sql
   UPDATE tasks 
   SET completed = true,
       status = 'completed',
       completion_proof = $2,
       updated_at = NOW()
   WHERE id = $1 AND owner_id = $2
   ```

4. **Начисление наград**:
   ```sql
   UPDATE users
   SET experience = experience + $1,
       gold = gold + $2
   WHERE id = $3
   ```

5. **Проверка повышения уровня**:
   ```sql
   SELECT level, experience FROM users WHERE id = $1
   ```
   
   ```rust
   let xp_for_next = (level + 1) * 100;
   if experience >= xp_for_next {
       // Level up!
       new_level = level + 1;
       new_xp = experience - xp_for_next;
       stat_points_gained = if new_level % 5 == 0 { 2 } else { 1 };
   }
   ```

6. **Обновление прогресса квеста** (если задача привязана):
   ```sql
   -- Получить все задачи квеста
   SELECT COUNT(*) as total,
          COUNT(*) FILTER (WHERE completed = true) as completed
   FROM tasks
   WHERE quest_id = $1
   
   -- Обновить процент выполнения
   UPDATE quests
   SET completion_percentage = (completed * 100 / total)
   WHERE id = $1
   ```

7. **Проверка достижений**:
   - Первая выполненная задача
   - 10, 50, 100 выполненных задач
   - Выполнение за 1 день
   - Streak (выполнение N дней подряд)

8. **Ответ клиенту**:
   ```json
   {
     "success": true,
     "rewards": {
       "experience": 25,
       "gold": 20
     },
     "level_up": true,
     "new_level": 2,
     "stat_points_gained": 1,
     "achievements_unlocked": [
       {
         "id": "first_task",
         "title": "Первые шаги",
         "description": "Выполнена первая задача"
       }
     ]
   }
   ```

9. **UI обновления**:
   - Анимация получения наград
   - Обновление XP бара
   - Если level up: показ диалога с поздравлением
   - Обновление золота
   - Badge для новых достижений

#### 2.3. Создание квеста

**Входные данные**:
- title (обязательно)
- description (опционально)
- difficulty (1-10, default: средняя сложность задач)
- quest_type (personal/daily/weekly/epic/boss)
- deadline (опционально)
- is_public (boolean, default: false)
- tags (опционально)

**Этапы**:

1. **Ручное создание**:
   - Пользователь создает пустой квест
   - Добавляет существующие задачи к квесту
   - Или создает новые задачи внутри квеста

2. **Автогенерация через ML** (POST /api/ml/transform):
   ```json
   Request:
   {
     "title": "Подготовка к экзамену по математике",
     "context": "Экзамен через 2 недели, нужно повторить темы",
     "difficulty_preference": 4,
     "user_level": 5
   }
   
   Response:
   {
     "title": "Подготовка к экзамену по математике",
     "description": "Систематическое изучение материала...",
     "difficulty": 4,
     "reward_experience": 100,
     "reward_description": "+100 XP, +40 Gold",
     "tags": ["study", "math", "exam"],
     "quest_type": "epic",
     "tasks": [
       {
         "title": "Повторить тему: Производные",
         "description": "Изучить правила дифференцирования",
         "difficulty": 3,
         "experience_reward": 30,
         "estimated_duration": 60
       },
       {
         "title": "Решить 10 задач",
         "difficulty": 4,
         "experience_reward": 40
       },
       {
         "title": "Пройти пробный тест",
         "difficulty": 5,
         "experience_reward": 50,
         "is_boss": true
       }
     ]
   }
   ```

3. **Создание в БД**:
   ```sql
   -- Создать квест
   INSERT INTO quests (
       title, description, difficulty, status, priority,
       quest_type, deadline, reward_experience, reward_description,
       tags, is_public, owner_id, metadata, created_at
   ) VALUES ($1, ..., $14)
   RETURNING id
   
   -- Создать задачи квеста
   INSERT INTO tasks (
       title, description, difficulty, quest_id, owner_id, ...
   ) VALUES ($1, ..., $N)
   ```

4. **Трекинг прогресса**:
   - completion_percentage: автообновляется при выполнении задач
   - status: pending → in_progress → completed
   - Автоматический переход в completed когда все задачи выполнены

#### 2.4. Система тегов

**Автоматические теги** (определяются ML или эвристикой):
- work: "работа", "офис", "проект", "отчет"
- study: "учеба", "экзамен", "курс", "лекция"
- health: "спорт", "зал", "пробежка", "здоровье"
- shopping: "купить", "магазин", "продукты"
- home: "дом", "уборка", "ремонт"
- social: "друзья", "встреча", "вечеринка"
- finance: "оплата", "счет", "банк"
- creative: "рисовать", "писать", "создать"

**Фильтрация и поиск**:
```sql
-- Поиск задач по тегу
SELECT * FROM tasks 
WHERE owner_id = $1 AND $2 = ANY(tags)

-- Поиск по множественным тегам (OR)
SELECT * FROM tasks
WHERE owner_id = $1 
  AND tags && ARRAY[$2, $3, $4]

-- Поиск по множественным тегам (AND)
SELECT * FROM tasks
WHERE owner_id = $1
  AND tags @> ARRAY[$2, $3]
```

**Статистика по тегам**:
```sql
SELECT unnest(tags) as tag, 
       COUNT(*) as count,
       SUM(CASE WHEN completed THEN 1 ELSE 0 END) as completed_count
FROM tasks
WHERE owner_id = $1
GROUP BY tag
ORDER BY count DESC
```

### 2. Создание и выполнение задачи

```mermaid
graph TD
    A[Нажатие + на экране] --> B[Открытие диалога]
    B --> C[Заполнение формы]
    C --> D{ИИ включен?}
    D -->|Да| E[ИИ определяет сложность]
    D -->|Нет| F[Ручная установка]
    E --> G[Расчёт наград]
    F --> G
    G --> H[Показ превью наград]
    H --> I[Нажатие 'Принять']
    I --> J[Создание задачи]
    J --> K[Добавление в список]
    K --> L[Обновление UI]
```

**Поля задачи**:
- `title` (string, обязательно) - название
- `description` (string, опционально) - описание
- `priority` (enum) - приоритет
- `difficulty` (1-5) - сложность
- `deadline` (date, опционально) - дедлайн
- `experienceReward` (int) - награда опытом
- `tags` (array) - автотеги

**Расчёт наград**:
```kotlin
val xpReward = difficulty * 10 + priorityBonus
val goldReward = difficulty * 10

priorityBonus:
  CRITICAL = 20
  HIGH = 10
  MEDIUM = 5
  LOW = 0
```

**ИИ-анализ текста**:
```kotlin
fun calculateAIDifficulty(title, description):
  difficulty = 2 // base
  
  if contains("сложн", "трудн", "тяжёл"):
    difficulty += 2
  if contains("прост", "лёгк", "быстр"):
    difficulty -= 1
  if length > 15 words:
    difficulty += 1
  if contains("презентация", "экзамен", "защита"):
    difficulty += 2
    
  return difficulty.clamp(1, 5)
```

---

### 3. Система наград

```mermaid
graph TD
    A[Пользователь нажимает ✅] --> B[Проверка статуса]
    B --> C{Уже выполнено?}
    C -->|Да| D[Отмена выполнения]
    C -->|Нет| E[Пометка выполненным]
    E --> F[Расчёт наград]
    F --> G[Добавление XP к пользователю]
    G --> H[Добавление золота]
    H --> I{XP >= порог?}
    I -->|Да| J[Повышение уровня]
    I -->|Нет| K[Сохранение XP]
    J --> L[Показ диалога с Level Up]
    K --> M[Показ обычного диалога]
    L --> N[Обновление UI]
    M --> N
```

**Формулы**:
```
1. Расчёт награды:
   XP = (difficulty × 10) + priorityBonus
   Gold = difficulty × 10

2. Проверка уровня:
   XPForNextLevel = (currentLevel + 1) × 100
   
3. Повышение уровня:
   if (totalXP >= XPForNextLevel):
     newLevel = currentLevel + 1
     remainingXP = totalXP - XPForNextLevel
```

**Обновление состояния**:
```kotlin
// AuthViewModel
fun addExperienceAndGold(xp: Int, gold: Int) {
  val newXp = currentUser.experience + xp
  val newGold = currentUser.gold + gold
  
  val xpForNext = (currentUser.level + 1) * 100
  val levelUp = newXp >= xpForNext
  
  if (levelUp) {
    currentUser.level += 1
    currentUser.experience = newXp - xpForNext
  } else {
    currentUser.experience = newXp
  }
  
  currentUser.gold = newGold
  
  // TODO: Sync with server
  // api.updateUserProgress(xp, gold)
}
```

---

### 4. ML-генерация квестов

```mermaid
graph TD
    A[Пользователь создаёт TODO] --> B[Отправка на сервер]
    B --> C[ML Handler]
    C --> D[Определение темы]
    D --> E{Тема?}
    E -->|Fantasy| F[Генерация фэнтези-квеста]
    E -->|Modern| G[Генерация современного]
    E -->|Sci-Fi| H[Генерация sci-fi]
    F --> I[Генерация подзадач]
    G --> I
    H --> I
    I --> J[Присвоение тегов]
    J --> K[Расчёт наград]
    K --> L[Возврат QuestResponse]
    L --> M[Отображение в UI]
```

**Детекция темы** (`server-rust/src/rag/templates.rs`):
```rust
fn detect_theme(text: &str) -> String {
  let lower = text.to_lowercase();
  
  // Проверка ключевых слов
  if contains("экзамен", "курс", "study") { "modern" }
  else if contains("api", "deploy", "проект") { "sci-fi" }
  else if contains("дом", "уборка", "покупки") { "modern" }
  else { "fantasy" } // По умолчанию
}
```

**Генерация квеста**:
```rust
fn generate_fantasy_quest(text, difficulty, level):
  title = "⚔️ Священная миссия: {essence}"
  
  description = "В мистическом царстве продуктивности 
                 ждёт великое испытание. Древние свитки
                 гласят о «{text}». Только герой твоего
                 калибра (Уровень {level}) может взяться
                 за этот {difficulty_name} квест..."
  
  tasks = generate_subtasks(text, difficulty)
  tags = auto_tags(text)
  xp = difficulty * 50
  
  return Quest { title, description, tasks, tags, xp }
```

**Генерация подзадач**:
```rust
difficulty 1: ["✅ Завершить: {text}"]
difficulty 2-5:
  - "📋 Фаза подготовки"
  - "⚔️ Фаза выполнения 1"
  - "⚔️ Фаза выполнения 2"
  - ...
  - "✨ Завершение и проверка"
```

---

### 3. Система персонажей (RPG механика)

#### 3.1. Выбор класса и расы

**Доступные классы**:

1. **Warrior (Воин)**:
   - Strength: +2
   - Constitution: +2
   - Бонусы: +10% к физическим задачам
   - Рекомендовано: спорт, физические задачи

2. **Mage (Маг)**:
   - Intelligence: +3
   - Wisdom: +1
   - Бонусы: +15% к учебным задачам
   - Рекомендовано: учеба, исследования, анализ

3. **Rogue (Плут)**:
   - Dexterity: +2
   - Charisma: +1
   - Intelligence: +1
   - Бонусы: +10% к быстрым задачам
   - Рекомендовано: многозадачность, креатив

4. **Cleric (Жрец)**:
   - Wisdom: +2
   - Charisma: +2
   - Бонусы: +10% к социальным задачам
   - Рекомендовано: помощь другим, социальные квесты

**Доступные расы**:

1. **Human (Человек)**: базовые характеристики, универсален
2. **Elf (Эльф)**: +1 Intelligence, +1 Dexterity
3. **Dwarf (Дварф)**: +2 Constitution
4. **Orc (Орк)**: +2 Strength

**Процесс выбора**:

1. Запрос доступных классов (GET /api/character/classes)
2. Запрос доступных рас (GET /api/character/races)
3. Пользователь выбирает class и race
4. Отправка выбора (POST /api/character/select):
   ```json
   {
     "class": "mage",
     "race": "elf"
   }
   ```
5. Сервер рассчитывает начальные характеристики
6. Обновление БД:
   ```sql
   UPDATE users
   SET character_class = $1,
       character_race = $2,
       strength = $3,
       intelligence = $4,
       charisma = $5,
       dexterity = $6,
       wisdom = $7
   WHERE id = $8
   ```

#### 3.2. Повышение уровня (Level Up)

**Требования для повышения**:
- Level N требует (N + 1) × 100 XP
- Пример: Level 1 → 2 требует 200 XP

**Процесс** (POST /api/character/level-up):

1. Проверка достаточности XP
2. Повышение уровня:
   ```sql
   UPDATE users
   SET level = $1,
       experience = $2
   WHERE id = $3
   ```
3. Начисление stat points:
   - Обычные уровни: +1 stat point
   - Каждый 5-й уровень: +2 stat points
   ```sql
   INSERT INTO user_stat_points (user_id, available_points)
   VALUES ($1, $2)
   ON CONFLICT (user_id) DO UPDATE
   SET available_points = available_points + $2
   ```

4. Разблокировка возможностей:
   - Level 5: Эпические квесты
   - Level 10: Мультиплеер
   - Level 15: Гильдии
   - Level 20: Легендарные квесты

**Ответ сервера**:
```json
{
  "success": true,
  "new_level": 5,
  "stat_points_gained": 2,
  "unlocked_features": [
    "Доступ к эпическим квестам"
  ]
}
```

#### 3.3. Распределение характеристик

**Процесс** (POST /api/character/increase-stat):

1. Проверка доступных stat points
2. Выбор характеристики для улучшения:
   - strength (сила)
   - intelligence (интеллект)
   - charisma (харизма)
   - dexterity (ловкость)
   - constitution (выносливость)
   - wisdom (мудрость)

3. Увеличение характеристики:
   ```sql
   UPDATE users
   SET {stat_name} = {stat_name} + $1
   WHERE id = $2
   
   UPDATE user_stat_points
   SET available_points = available_points - $1
   WHERE user_id = $2
   ```

4. Применение бонусов к будущим задачам:
   - High strength: бонус к physical задачам
   - High intelligence: бонус к study задачам
   - High charisma: бонус к social задачам

#### 3.4. Dice система (D&D механика)

**Типы кубиков**:
- D4: 1-4 (простые проверки)
- D6: 1-6 (стандартные)
- D8: 1-8 (сложные)
- D10: 1-10 (очень сложные)
- D12: 1-12 (героические)
- D20: 1-20 (критические проверки)

**Skill check процесс** (POST /api/dice/skill-check):

1. Входные данные:
   ```json
   {
     "skill": "athletics",
     "difficulty_class": 15
   }
   ```

2. Получение характеристик пользователя:
   ```sql
   SELECT strength, intelligence, dexterity, charisma
   FROM users WHERE id = $1
   ```

3. Расчет модификатора:
   ```rust
   modifier = match skill {
       Athletics => (strength - 10) / 2,
       Arcana => (intelligence - 10) / 2,
       Persuasion => (charisma - 10) / 2,
       Acrobatics => (dexterity - 10) / 2,
   }
   ```

4. Бросок D20:
   ```rust
   let roll = random(1..=20);
   let total = roll + modifier;
   let success = total >= difficulty_class;
   let critical = roll == 20;
   let fumble = roll == 1;
   ```

5. Ответ:
   ```json
   {
     "roll": 18,
     "modifier": 3,
     "total": 21,
     "success": true,
     "critical": false,
     "difficulty_class": 15,
     "description": "Успешная проверка Athletics!"
   }
   ```

**Использование в квестах**:
- Boss battles: требуется успешная skill check
- Критические задачи: D20 проверка на успех
- Награды: критический успех = двойные награды

### 4. Геолокация и AR

#### 4.1. Создание геозон

**Процесс** (POST /api/geo/zones):

1. Входные данные:
   ```json
   {
     "name": "Дом",
     "latitude": 55.751244,
     "longitude": 37.618423,
     "radius_meters": 100,
     "zone_type": "home"
   }
   ```

2. Сохранение в БД:
   ```sql
   INSERT INTO geo_zones (
       user_id, name, latitude, longitude,
       radius_meters, zone_type, created_at
   ) VALUES ($1, $2, $3, $4, $5, $6, NOW())
   RETURNING id, name, latitude, longitude, radius_meters, zone_type, created_at
   ```

3. Конвертация в модель:
   ```rust
   GeoZone {
       id,
       name,
       center: Location { latitude, longitude },
       radius_meters,
       zone_type: parse_zone_type(zone_type),
       created_at
   }
   ```

**Типы геозон**:
- home: Домашняя зона
- work: Рабочая зона
- gym: Спортзал
- shop: Магазин
- park: Парк
- custom: Пользовательская

#### 4.2. Проверка локации

**Процесс** (POST /api/geo/check):

1. Входные данные:
   ```json
   {
     "latitude": 55.751300,
     "longitude": 37.618500
   }
   ```

2. Загрузка всех геозон пользователя:
   ```sql
   SELECT id, name, latitude, longitude, radius_meters, zone_type
   FROM geo_zones
   WHERE user_id = $1
   ```

3. Расчет расстояний (формула Haversine):
   ```rust
   fn haversine_distance(lat1, lon1, lat2, lon2) -> f64 {
       let r = 6371000.0; // Радиус Земли в метрах
       let dlat = (lat2 - lat1).to_radians();
       let dlon = (lon2 - lon1).to_radians();
       let a = (dlat/2).sin().powi(2) +
               lat1.to_radians().cos() *
               lat2.to_radians().cos() *
               (dlon/2).sin().powi(2);
       let c = 2 * a.sqrt().atan2((1-a).sqrt());
       r * c
   }
   ```

4. Определение активных зон:
   ```rust
   for zone in zones {
       let distance = haversine_distance(
           current.lat, current.lon,
           zone.lat, zone.lon
       );
       if distance <= zone.radius_meters {
           in_zones.push(zone);
       }
   }
   ```

5. Проверка триггеров:
   ```sql
   SELECT quest_id, trigger_type
   FROM geo_triggers
   WHERE zone_id = ANY($1) AND is_active = true
   ```

6. Ответ:
   ```json
   {
     "in_zones": [
       {
         "zone_id": 1,
         "zone_name": "Дом",
         "zone_type": "home",
         "distance_meters": 45.7
       }
     ],
     "triggered_quests": [2, 5]
   }
   ```

#### 4.3. AR обработка изображений

**Процесс** (POST /api/ar/process-image):

1. Проверка согласия на камеру:
   ```sql
   SELECT camera_consent FROM user_consents
   WHERE user_id = $1
   ```

2. Прием изображения:
   ```json
   {
     "quest_id": 123,
     "image_data": "data:image/jpeg;base64,..."
   }
   ```

3. Валидация:
   - Размер < 10MB
   - Формат: JPEG/PNG
   - Не содержит персональных данных (опционально: blur faces)

4. Обработка (заглушка, требует ML модели):
   - Декодирование base64
   - Распознавание объектов
   - Проверка соответствия quest требованиям
   - Генерация confidence score

5. Автоудаление:
   ```rust
   // Через IMAGE_RETENTION_MINUTES минут
   tokio::spawn(async move {
       sleep(Duration::from_minutes(retention)).await;
       fs::remove_file(image_path).ok();
   });
   ```

6. Ответ:
   ```json
   {
     "status": "verified",
     "ai_confidence": 0.85,
     "detected_objects": ["apple", "banana"],
     "verification_message": "Изображение соответствует квесту"
   }
   ```

### 5. Мультиплеер и кооперативные квесты

#### 5.1. WebSocket соединение

**Подключение**:
```
WS /ws?token=JWT_TOKEN
```

**Процесс**:

1. Валидация JWT токена из query параметра
2. Создание WebSocket соединения
3. Регистрация в WebSocketManager:
   ```rust
   manager.add_connection(user_id, ws_sender);
   ```

4. Heartbeat механизм:
   - Клиент отправляет Ping каждые 30 секунд
   - Сервер отвечает Pong
   - Disconnect после 90 секунд без активности

**Типы сообщений**:

```json
// Join room
{
  "type": "join_room",
  "room_id": "quest_123"
}

// Chat message
{
  "type": "chat",
  "room_id": "quest_123",
  "message": "Привет!"
}

// Quest progress update
{
  "type": "quest_progress",
  "quest_id": 123,
  "completion": 75
}

// User joined
{
  "type": "user_joined",
  "user_id": 456,
  "username": "player2"
}
```

#### 5.2. Создание кооперативного квеста

**Процесс**:

1. Создание квеста с is_public = true
2. Отправка пр

иглашений:
   ```sql
   INSERT INTO quest_invitations (
       quest_id, inviter_id, invitee_id, status
   ) VALUES ($1, $2, $3, 'pending')
   ```

3. Уведомление через WebSocket:
   ```json
   {
     "type": "quest_invitation",
     "quest_id": 123,
     "inviter": "player1",
     "quest_title": "Подготовка к экзамену"
   }
   ```

4. Принятие приглашения:
   ```sql
   INSERT INTO quest_participants (
       quest_id, user_id, role, joined_at
   ) VALUES ($1, $2, 'member', NOW())
   
   UPDATE quest_invitations
   SET status = 'accepted'
   WHERE id = $1
   ```

5. Синхронизация прогресса:
   - Каждое выполнение задачи → broadcast через WebSocket
   - Все участники видят обновления в реальном времени

### 6. Rate Limiting и безопасность

#### 6.1. Rate Limiting механизм

**Конфигурация**:
```env
RATE_LIMIT_PER_MINUTE=60
RATE_LIMIT_BURST=10
```

**Алгоритм Token Bucket**:

```rust
struct RateLimiter {
    tokens: HashMap<IpAddr, (u32, Instant)>,
    max_tokens: u32,
    refill_rate: Duration,
}

impl RateLimiter {
    fn check(&mut self, ip: IpAddr) -> bool {
        let (tokens, last_refill) = self.tokens
            .entry(ip)
            .or_insert((self.max_tokens, Instant::now()));
        
        // Refill tokens
        let elapsed = Instant::now() - *last_refill;
        let new_tokens = (elapsed.as_secs() / 60) as u32;
        *tokens = (*tokens + new_tokens).min(self.max_tokens);
        *last_refill = Instant::now();
        
        // Check and consume
        if *tokens > 0 {
            *tokens -= 1;
            true
        } else {
            false
        }
    }
}
```

**Обработка**:
- 429 Too Many Requests при превышении лимита
- Header: `Retry-After: 60`
- Временная блокировка на 1 минуту

#### 6.2. IP Blocking

**Автоматическая блокировка**:
- 10 неудачных логинов за 5 минут
- 100 запросов за 1 минуту
- Обнаружение SQL injection попыток
- Обнаружение XSS попыток

**Процесс**:

```rust
if failed_attempts >= 10 {
    ip_blacklist.add(ip, Duration::from_hours(1));
    log_security_event("IP blocked", ip, reason);
}
```

**Проверка каждого запроса**:
```rust
if ip_blacklist.is_blocked(ip) {
    return StatusCode::FORBIDDEN;
}
```

#### 6.3. CORS политика

**Конфигурация**:
```env
# Development
CORS_ORIGIN=*

# Production
CORS_ORIGIN=https://irlquest.com,https://app.irlquest.com
```

**Middleware**:
```rust
CorsLayer::new()
    .allow_origin(origins)
    .allow_methods([GET, POST, PUT, DELETE])
    .allow_headers([AUTHORIZATION, CONTENT_TYPE])
    .max_age(Duration::from_secs(3600))
```

### 7. Приватность и согласия пользователя

#### 7.1. Сбор согласий (GDPR / 152-ФЗ)

**Типы согласий**:

1. **camera_consent**: Использование камеры для AR
2. **location_consent**: Доступ к геолокации
3. **data_processing_consent**: Обработка персональных данных

**Процесс** (POST /api/privacy/consent):

```json
{
  "camera_consent": true,
  "location_consent": true,
  "data_processing_consent": true
}
```

**Сохранение**:
```sql
INSERT INTO user_consents (
    user_id, camera_consent, location_consent,
    data_processing_consent, consent_date
) VALUES ($1, $2, $3, $4, NOW())
ON CONFLICT (user_id) DO UPDATE
SET camera_consent = EXCLUDED.camera_consent,
    location_consent = EXCLUDED.location_consent,
    data_processing_consent = EXCLUDED.data_processing_consent,
    consent_date = NOW()
```

**Проверка перед операциями**:
```rust
// Перед использованием камеры
if !user_has_camera_consent(user_id) {
    return Err(AppError::Forbidden(
        "Camera consent required"
    ));
}

// Перед использованием геолокации
if !user_has_location_consent(user_id) {
    return Err(AppError::Forbidden(
        "Location consent required"
    ));
}
```

#### 7.2. Удаление данных (Right to be forgotten)

**Процесс** (DELETE /api/user/account):

1. Запрос подтверждения
2. Проверка пароля
3. Каскадное удаление:
   ```sql
   -- Удаление связанных данных
   DELETE FROM refresh_tokens WHERE user_id = $1;
   DELETE FROM oauth_accounts WHERE user_id = $1;
   DELETE FROM tasks WHERE owner_id = $1;
   DELETE FROM quests WHERE owner_id = $1;
   DELETE FROM user_consents WHERE user_id = $1;
   DELETE FROM geo_zones WHERE user_id = $1;
   
   -- Удаление пользователя
   DELETE FROM users WHERE id = $1;
   ```

4. Логирование (без персональных данных):
   ```
   [INFO] User account deleted: user_id=123, timestamp=2025-11-06
   ```

---

## ТЕСТИРОВАНИЕ {#тестирование}

### 1. Unit тестирование

#### 1.1. Backend (Rust)

**Тестирование бизнес-логики**:

```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_calculate_xp_reward() {
        assert_eq!(calculate_xp(difficulty: 1, priority: Medium), 15);
        assert_eq!(calculate_xp(difficulty: 5, priority: Critical), 70);
    }
    
    #[test]
    fn test_level_up_requirements() {
        assert_eq!(xp_for_level(1), 200);
        assert_eq!(xp_for_level(5), 600);
    }
    
    #[test]
    fn test_stat_modifier() {
        assert_eq!(calculate_modifier(10), 0);
        assert_eq!(calculate_modifier(16), 3);
        assert_eq!(calculate_modifier(8), -1);
    }
}
```

**Тестирование валидации**:

```rust
#[tokio::test]
async fn test_email_validation() {
    assert!(validate_email("user@example.com").is_ok());
    assert!(validate_email("invalid-email").is_err());
    assert!(validate_email("").is_err());
}

#[tokio::test]
async fn test_password_strength() {
    assert!(validate_password("12345").is_err()); // Слишком короткий
    assert!(validate_password("password123").is_ok());
}
```

**Запуск тестов**:
```bash
cd server-rust
cargo test --lib          # Только unit тесты
cargo test -- --nocapture # С выводом
cargo test test_name      # Конкретный тест
```

#### 1.2. Mobile (Kotlin)

**Тестирование ViewModels**:

```kotlin
@Test
fun `calculateXPReward returns correct value`() {
    val viewModel = TaskViewModel(repository)
    val xp = viewModel.calculateXPReward(
        difficulty = 3,
        priority = Priority.HIGH
    )
    assertEquals(40, xp) // 3*10 + 10
}

@Test
fun `level up logic works correctly`() {
    val user = User(level = 1, experience = 250)
    val result = user.tryLevelUp()
    assertTrue(result.leveledUp)
    assertEquals(2, result.newLevel)
    assertEquals(50, result.remainingXP) // 250 - 200
}
```

**Запуск**:
```bash
cd mobile
./gradlew test
./gradlew testDebugUnitTest --tests TaskViewModelTest
```

### 2. Интеграционное тестирование

#### 2.1. API тестирование

**Тестовый сценарий - регистрация и создание задачи**:

```rust
#[tokio::test]
async fn test_full_task_flow() {
    let app = create_test_app().await;
    
    // 1. Регистрация
    let register_response = app.post("/api/auth/register")
        .json(&json!({
            "email": "test@example.com",
            "username": "testuser",
            "password": "password123"
        }))
        .send()
        .await;
    
    assert_eq!(register_response.status(), 200);
    let auth: AuthResponse = register_response.json().await;
    let token = auth.access_token;
    
    // 2. Создание задачи
    let task_response = app.post("/api/tasks")
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({
            "title": "Test task",
            "difficulty": 3
        }))
        .send()
        .await;
    
    assert_eq!(task_response.status(), 201);
    let task: Task = task_response.json().await;
    assert_eq!(task.title, "Test task");
    assert_eq!(task.difficulty, 3);
    
    // 3. Выполнение задачи
    let complete_response = app.put(format!("/api/tasks/{}", task.id))
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({"completed": true}))
        .send()
        .await;
    
    assert_eq!(complete_response.status(), 200);
    
    // 4. Проверка наград
    let user_response = app.get("/api/auth/me")
        .header("Authorization", format!("Bearer {}", token))
        .send()
        .await;
    
    let user: User = user_response.json().await;
    assert_eq!(user.experience, 35); // 3*10 + 5 (medium priority)
    assert_eq!(user.gold, 130); // 100 initial + 30
}
```

**Тестирование с базой данных**:

```rust
async fn create_test_db() -> PgPool {
    let pool = PgPoolOptions::new()
        .connect("postgres://postgres:password@localhost:5432/test_db")
        .await
        .unwrap();
    
    // Применить миграции
    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .unwrap();
    
    pool
}

#[tokio::test]
async fn test_with_real_db() {
    let pool = create_test_db().await;
    
    // Тесты с реальной БД
    // ...
    
    // Очистка после теста
    sqlx::query("TRUNCATE users CASCADE").execute(&pool).await.ok();
}
```

#### 2.2. E2E тестирование

**Сценарий полного цикла**:

1. Запуск тестовой БД (testcontainers)
2. Запуск сервера
3. Выполнение HTTP запросов
4. Проверка состояния БД
5. Очистка

```rust
#[tokio::test]
async fn e2e_quest_completion() {
    let container = start_postgres_container().await;
    let server = start_test_server(&container).await;
    
    let client = reqwest::Client::new();
    
    // Регистрация
    let response = client.post(format!("{}/api/auth/register", server.url()))
        .json(&register_request)
        .send()
        .await
        .unwrap();
    assert_eq!(response.status(), 200);
    
    // ... остальные шаги
    
    // Проверка в БД
    let count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM tasks WHERE owner_id = $1")
        .bind(user_id)
        .fetch_one(&container.pool())
        .await
        .unwrap();
    assert_eq!(count, 3);
}
```

### 3. Performance тестирование

#### 3.1. Load testing (Apache Bench)

```bash
# 1000 запросов, 10 одновременно
ab -n 1000 -c 10 http://localhost:8003/health

# С авторизацией
ab -n 1000 -c 10 -H "Authorization: Bearer TOKEN" \
   http://localhost:8003/api/tasks

# POST запрос
ab -n 100 -c 5 -p task.json -T application/json \
   -H "Authorization: Bearer TOKEN" \
   http://localhost:8003/api/tasks
```

**Ожидаемые результаты**:
- /health: <10ms, 100% success
- /api/tasks GET: <50ms, 100% success
- /api/tasks POST: <100ms, 95%+ success
- /api/ml/*: <500ms, 90%+ success

#### 3.2. Stress testing

```rust
#[tokio::test]
async fn stress_test_concurrent_requests() {
    let mut tasks = vec![];
    
    for i in 0..1000 {
        let task = tokio::spawn(async move {
            let client = reqwest::Client::new();
            client.get("http://localhost:8003/health")
                .send()
                .await
        });
        tasks.push(task);
    }
    
    let results = futures::future::join_all(tasks).await;
    let success_count = results.iter()
        .filter(|r| r.is_ok())
        .count();
    
    assert!(success_count >= 950); // 95% success rate
}
```

### 4. Security тестирование

#### 4.1. Проверка аутентификации

```bash
# Попытка доступа без токена
curl -X GET http://localhost:8003/api/tasks
# Ожидается: 401 Unauthorized

# С невалидным токеном
curl -X GET -H "Authorization: Bearer invalid-token" \
     http://localhost:8003/api/tasks
# Ожидается: 401 Unauthorized

# С истекшим токеном
curl -X GET -H "Authorization: Bearer expired-token" \
     http://localhost:8003/api/tasks
# Ожидается: 401 Unauthorized
```

#### 4.2. SQL Injection testing

```bash
# Попытка SQL injection в параметрах
curl -X POST http://localhost:8003/api/tasks \
  -H "Authorization: Bearer TOKEN" \
  -d '{"title": "Test'; DROP TABLE users;--"}'

# Ожидается: 400 Bad Request или успешное экранирование
# В БД НЕ должно быть изменений в таблице users
```

**Защита через параметризованные запросы**:
```rust
// Безопасно - параметры экранируются
sqlx::query("SELECT * FROM users WHERE id = $1")
    .bind(user_id)
    .fetch_one(&pool)
    .await;

// НИКОГДА не делайте так:
// sqlx::query(&format!("SELECT * FROM users WHERE id = {}", user_id))
```

#### 4.3. XSS testing

```bash
# Попытка XSS через title
curl -X POST http://localhost:8003/api/tasks \
  -H "Authorization: Bearer TOKEN" \
  -d '{"title": "<script>alert(1)</script>"}'

# Ожидается: строка сохраняется как текст, не выполняется
```

**Защита**:
- Все данные сериализуются через serde (автоэкранирование)
- На клиенте: HTML encoding при отображении

### 5. Автоматизированное тестирование

#### 5.1. CI/CD pipeline

**GitHub Actions workflow**:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: password
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Setup Rust
        uses: actions-rs/toolchain@v1
        with:
          toolchain: stable
          
      - name: Run tests
        env:
          DATABASE_URL: postgres://postgres:password@localhost:5432/test_db
        run: |
          cd server-rust
          cargo test --all-features
      
      - name: Run clippy
        run: cargo clippy -- -D warnings
      
      - name: Check formatting
        run: cargo fmt -- --check
```

#### 5.2. Тестовые fixtures

**Создание тестовых данных**:

```rust
async fn create_test_user(pool: &PgPool) -> User {
    sqlx::query_as!(
        User,
        "INSERT INTO users (email, username, hashed_password, ...)
         VALUES ($1, $2, $3, ...)
         RETURNING *",
        "test@example.com",
        "testuser",
        hash_password("password")
    )
    .fetch_one(pool)
    .await
    .unwrap()
}

async fn create_test_quest(pool: &PgPool, owner_id: i32) -> Quest {
    sqlx::query_as!(
        Quest,
        "INSERT INTO quests (title, description, owner_id, ...)
         VALUES ($1, $2, $3, ...)
         RETURNING *",
        "Test Quest",
        "Test Description",
        owner_id
    )
    .fetch_one(pool)
    .await
    .unwrap()
}
```

### 6. Мониторинг и метрики

#### 6.1. Health checks

**Endpoint** (GET /health):

```rust
pub async fn health_check(State(state): State<AppState>) -> StatusCode {
    // Проверка БД
    match sqlx::query("SELECT 1")
        .fetch_one(&state.db)
        .await
    {
        Ok(_) => StatusCode::OK,
        Err(_) => StatusCode::SERVICE_UNAVAILABLE,
    }
}
```

**Readiness probe**:
```rust
pub async fn readiness() -> StatusCode {
    // Проверка всех зависимостей
    if db_healthy() && redis_healthy() && ml_healthy() {
        StatusCode::OK
    } else {
        StatusCode::SERVICE_UNAVAILABLE
    }
}
```

#### 6.2. Prometheus метрики

**Экспортируемые метрики**:

```
# HTTP запросы
http_requests_total{method="GET",path="/api/tasks",status="200"} 1523
http_request_duration_seconds{method="GET",path="/api/tasks"} 0.042

# База данных
db_connections_active 5
db_connections_idle 3
db_query_duration_seconds{query="select_tasks"} 0.015

# Бизнес-метрики
tasks_created_total 456
tasks_completed_total 342
quests_created_total 89
users_registered_total 234
level_ups_total 67
```

**Настройка**:
```yaml
# infra/prometheus/prometheus.yml
scrape_configs:
  - job_name: 'irl-quest-server'
    static_configs:
      - targets: ['localhost:8003']
    metrics_path: '/metrics'
    scrape_interval: 15s
```

---

## КОМПОНЕНТЫ СИСТЕМЫ {#компоненты}

### Мобильное приложение

#### Структура модулей:
```
mobile/app/src/main/java/com/irlquest/app/
├── data/
│   ├── network/
│   │   ├── dto/          # Data Transfer Objects
│   │   ├── ApiService.kt # Retrofit интерфейс
│   │   └── RetrofitClient.kt
│   ├── repository/       # Репозитории
│   │   ├── AuthRepository.kt
│   │   ├── QuestRepository.kt
│   │   └── TaskRepository.kt
│   └── ml/
│       └── MLGenerator.kt # Локальный ML
│
├── feature/              # Экраны (по фичам)
│   ├── auth/            # Аутентификация
│   ├── home/            # Главный экран (Таверна)
│   ├── quests/          # Квесты
│   ├── tasks/           # Задачи
│   ├── hero/            # Профиль героя
│   ├── worldmap/        # Карта мира
│   ├── stats/           # Статистика
│   └── rewards/         # Награды
│
├── ui/
│   ├── components/      # Переиспользуемые компоненты
│   │   ├── QuestCard.kt
│   │   └── RewardDialog.kt
│   ├── theme/           # Дизайн-система
│   │   ├── Color.kt
│   │   └── Theme.kt
│   ├── utils/           # Утилиты
│   │   └── QuestTextUtils.kt
│   ├── navigation/      # Навигация
│   │   └── MainScreen.kt
│   └── viewmodel/       # Shared ViewModels
│       └── AuthViewModel.kt
│
└── MainActivity.kt      # Entry point
```

#### MVVM Архитектура:

```
View (Composable)
    ↕
ViewModel (StateFlow)
    ↕
Repository (suspend functions)
    ↕
API Service (Retrofit)
    ↕
Backend Server
```

**Пример потока данных**:
```kotlin
// 1. View
@Composable
fun TasksScreen(viewModel: TasksViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  // UI отображает состояние
}

// 2. ViewModel
class TasksViewModel(private val authViewModel: AuthViewModel) {
  private val _uiState = MutableStateFlow(TasksUiState())
  
  fun toggleTask(id: Int) {
    // Обновление состояния
    // Вызов repository
    // Обновление наград через authViewModel
  }
}

// 3. Repository
class TaskRepository {
  suspend fun updateTask(id: Int, completed: Boolean) {
    api.updateTask(id, UpdateTaskRequest(completed))
  }
}

// 4. API
interface ApiService {
  @PATCH("/api/tasks/{id}")
  suspend fun updateTask(id: Int, body: UpdateTaskRequest)
}
```

---

### Бэкенд сервер

#### Структура:
```
server-rust/src/
├── handlers/           # HTTP обработчики
│   ├── auth_handlers.rs
│   ├── quest_handlers.rs
│   ├── task_handlers.rs
│   └── ml.rs
├── services/          # Бизнес-логика
│   ├── auth.rs
│   ├── quest.rs
│   └── task.rs
├── models/            # Модели данных
│   ├── user.rs
│   ├── quest.rs
│   └── task.rs
├── rag/               # ML генерация
│   ├── templates.rs   # Шаблоны квестов
│   └── embeddings.rs  # Векторные представления
├── middleware/        # Промежуточное ПО
│   ├── auth.rs        # JWT проверка
│   └── cors.rs
├── db.rs              # База данных
├── routes.rs          # Маршруты
└── main_full.rs       # Entry point
```

---

## 💼 Бизнес-процессы {#бизнес-процессы}

### Процесс 1: Жизненный цикл задачи

```
1. СОЗДАНИЕ
   ↓
   User вводит данные
   → ViewModel валидирует
   → Repository создаёт через API
   → Server сохраняет в БД
   → Возврат Task DTO
   → Обновление UI
   
2. ВЫПОЛНЕНИЕ
   ↓
   User нажимает ✅
   → ViewModel переключает статус
   → Расчёт наград
   → AuthViewModel.addExperienceAndGold()
   → Проверка level up
   → Показ RewardDialog
   → Обновление счётчиков
   → [Optional] Sync с сервером
   
3. УДАЛЕНИЕ
   ↓
   User нажимает 🗑️
   → Подтверждение
   → ViewModel удаляет из списка
   → [Optional] API delete request
   → Обновление UI
```

### Процесс 2: Система наград

```
Trigger: Task.completed = true

1. Расчёт наград:
   XP = (difficulty × 10) + priorityBonus
   Gold = difficulty × 10

2. Применение:
   currentUser.experience += XP
   currentUser.gold += Gold

3. Проверка level up:
   if (currentUser.experience >= nextLevelXP):
     currentUser.level += 1
     currentUser.experience -= nextLevelXP
     showLevelUpBanner = true

4. Анимация:
   - Показать RewardDialog
   - Анимация звёзд ✨
   - Показать полученные награды
   - Если level up → дополнительная карточка

5. Обновление:
   - Обновить XP бар в профиле
   - Обновить золото
   - Обновить уровень
   - Пересчитать статистику
```

### Процесс 3: ML-генерация квеста

```
Client                    Server                   ML
  │                         │                       │
  ├─ POST /api/ml/todo-to-quest                    │
  │  {todo: "Изучить Kotlin"}                      │
  │                         │                       │
  │                         ├─ detect_theme()       │
  │                         │  → "modern"           │
  │                         │                       │
  │                         ├─ generate_quest()     │
  │                         │  → title, desc        │
  │                         │                       │
  │                         ├─ generate_tasks()     │
  │                         │  → [task1, task2...]  │
  │                         │                       │
  │                         ├─ auto_tags()          │
  │                         │  → ["обучение", ...]  │
  │                         │                       │
  │                         ├─ calculate_rewards()  │
  │                         │  → XP, gold           │
  │                         │                       │
  │ ◄─────────────────────┤                       │
  │  QuestGenerationResponse                       │
  │                         │                       │
  ├─ Показать превью       │                       │
  ├─ User подтверждает     │                       │
  ├─ POST /api/quests      │                       │
  │                         │                       │
  │                         ├─ Save to DB           │
  │ ◄─────────────────────┤                       │
  │  Quest created          │                       │
  │                         │                       │
  └─ Обновить UI           │                       │
```

---

## 🔌 API Спецификация {#api}

### Аутентификация

#### POST `/api/auth/register`
Регистрация нового пользователя

**Request**:
```json
{
  "email": "hero@example.com",
  "username": "brave_hero",
  "password": "secret123"
}
```

**Response** (201):
```json
{
  "id": 1,
  "username": "brave_hero",
  "email": "hero@example.com"
}
```

#### POST `/api/auth/login`
Вход в систему

**Request**:
```json
{
  "username": "brave_hero",
  "password": "secret123"
}
```

**Response** (200):
```json
{
  "token": "eyJhbGc...",
  "user": {
    "id": 1,
    "username": "brave_hero",
    "email": "hero@example.com",
    "level": 5,
    "experience": 250,
    "gold": 1234,
    "characterClass": "warrior"
  }
}
```

#### GET `/api/auth/me`
Получение профиля (требует токен)

**Headers**:
```
Authorization: Bearer {token}
```

**Response** (200):
```json
{
  "id": 1,
  "username": "brave_hero",
  "email": "hero@example.com",
  "level": 5,
  "experience": 250,
  "gold": 1234,
  "characterClass": "warrior",
  "lastLogin": "2025-10-25T10:00:00Z"
}
```

---

### Задачи

#### GET `/api/tasks`
Список задач пользователя

**Response** (200):
```json
[
  {
    "id": 1,
    "title": "Изучить Kotlin",
    "description": "Основы языка",
    "completed": false,
    "priority": "high",
    "difficulty": 3,
    "experienceReward": 40,
    "deadline": "2025-10-25",
    "tags": ["обучение"],
    "createdAt": "2025-10-24T10:00:00Z"
  }
]
```

#### POST `/api/tasks`
Создание задачи

**Request**:
```json
{
  "title": "Изучить Kotlin",
  "description": "Основы языка",
  "priority": 2,
  "difficulty": 3,
  "deadline": "2025-10-25",
  "estimatedDuration": 120
}
```

#### PATCH `/api/tasks/{id}`
Обновление задачи

**Request**:
```json
{
  "completed": true
}
```

---

### Квесты

#### GET `/api/quests`
Список квестов

#### POST `/api/quests`
Создание квеста

**Request**:
```json
{
  "title": "⚔️ Священная миссия: Изучить Kotlin",
  "description": "В мистическом царстве...",
  "difficulty": 3,
  "experienceReward": 150,
  "tasks": [
    {
      "title": "📋 Фаза подготовки",
      "description": "Собрать ресурсы...",
      "difficulty": 1,
      "experienceReward": 30
    }
  ]
}
```

---

### ML Endpoints

#### POST `/api/ml/todo-to-quest`
Превращение TODO в квест

**Request**:
```json
{
  "todos": ["Изучить Kotlin", "Купить продукты"],
  "context": null,
  "difficulty_preference": 3
}
```

**Response**:
```json
[
  {
    "todo_text": "Изучить Kotlin",
    "quest": {
      "title": "⚔️ Священная миссия: Изучить Kotlin",
      "description": "В мистическом царстве продуктивности...",
      "difficulty": 3,
      "reward_experience": 150,
      "reward_description": "Заверши это фэнтези приключение...",
      "tags": ["обучение", "fantasy", "сложность:3"],
      "tasks": [
        {
          "title": "📋 Фаза подготовки",
          "description": "Собери ресурсы...",
          "difficulty": 1,
          "experience_reward": 30,
          "estimated_duration": 15
        }
      ]
    }
  }
]
```

---

## 🗄️ База данных {#база-данных}

### Схема таблиц

#### `users`
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    level INTEGER DEFAULT 1,
    experience INTEGER DEFAULT 0,
    gold INTEGER DEFAULT 0,
    character_class TEXT DEFAULT 'warrior',
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    last_login TEXT
);
```

#### `quests`
```sql
CREATE TABLE quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    difficulty INTEGER DEFAULT 1,
    priority INTEGER DEFAULT 2,
    status TEXT DEFAULT 'active',
    experience_reward INTEGER DEFAULT 0,
    completion_percentage INTEGER DEFAULT 0,
    quest_type TEXT DEFAULT 'manual',
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### `tasks`
```sql
CREATE TABLE tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    quest_id INTEGER,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    completed BOOLEAN DEFAULT 0,
    priority TEXT DEFAULT 'medium',
    difficulty INTEGER DEFAULT 1,
    experience_reward INTEGER DEFAULT 10,
    deadline TEXT,
    tags TEXT, -- JSON array
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    completed_at TEXT,
    FOREIGN KEY (quest_id) REFERENCES quests(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 🚀 Развёртывание {#развёртывание}

### Мобильное приложение

#### Сборка debug APK:
```bash
cd mobile
./gradlew assembleDebug
```

**Результат**: `mobile/app/build/outputs/apk/debug/app-debug.apk`

#### Сборка release APK:
```bash
./gradlew assembleRelease
```

#### Установка через ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### Сервер

#### Локальный запуск:
```bash
cd server-rust

# Установка зависимостей
cargo build

# Запуск
cargo run --release
```

#### С Docker:
```bash
docker-compose up -d
```

#### Переменные окружения:
```env
DATABASE_URL=sqlite://irlquest.db
JWT_SECRET=your-secret-key-here
REDIS_URL=redis://localhost:6379
OLLAMA_API_URL=http://localhost:11434
SERVER_PORT=8080
```

---

## 📊 Метрики и мониторинг

### Ключевые метрики:

#### Пользовательская активность:
- DAU (Daily Active Users)
- Задач создано за день
- Задач выполнено за день
- Средняя длительность сессии

#### Геймификация:
- Средний уровень пользователей
- Распределение по рангам
- Заработанный XP за день
- Заработанное золото за день

#### Производительность:
- Время отклика API (<200ms целевое)
- Успешность запросов (>99%)
- Размер APK (<20MB)
- Потребление памяти (<100MB)

---

## 🔐 Безопасность

### Аутентификация:
- JWT токены (срок: 30 дней)
- Пароли хешируются (bcrypt)
- HTTPS для всех запросов

### Хранение данных:
- JWT в SharedPreferences (encrypted)
- Локальный кэш очищается при logout
- Sensitive data не логируется

---

## 🧪 Тестирование

### Unit тесты:
```bash
# Android
cd mobile
./gradlew test

# Server
cd server-rust
cargo test
```

### UI тесты:
```bash
cd mobile
./gradlew connectedAndroidTest
```

---

## 📝 Changelog

### Version 2.0 - Фэнтези Таверна (25.10.2025)

**Новое**:
- ✨ Полная система наград (XP + золото)
- 🎉 Повышение уровня
- 🎨 Фэнтези-дизайн (золото, пергамент, дерево)
- 🤖 ИИ-определение сложности
- 🏷️ Автоматические теги
- 🌍 Русский язык в ML
- 🗺️ Интерактивная карта мира
- ⚔️ Профиль героя с характеристиками D&D
- 📊 Статистика и графики

**Исправлено**:
- ✅ Создание квестов
- ✅ Создание заданий
- ✅ Обновление XP и золота
- ✅ Расположение элементов
- ✅ Баланс Compose групп
- ✅ Миграция Material 2 → Material 3

---

*Документация обновлена: 25.10.2025*

