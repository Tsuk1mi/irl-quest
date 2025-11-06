# 🔧 Конфигурация сервера IRL Quest

## 📁 Единый файл конфигурации

**Важно**: Используется **ОДИН** `.env` файл в **корне проекта** (не в server-rust/)!

```bash
irl-quest/
├── .env.example    # 👈 Шаблон конфигурации
├── .env            # 👈 Ваша конфигурация (создайте из .env.example)
└── server-rust/    # Backend автоматически найдет .env в корне
```

## Быстрый старт

```bash
# 1. Скопируйте шаблон в корне проекта
cp .env.example .env

# 2. Отредактируйте .env
nano .env

# 3. Запустите сервер (он загрузит .env из корня)
cd server-rust
cargo run --release
```

## Минимальная конфигурация

```env
DATABASE_URL=postgres://postgres:tsukimi@localhost:5432/irl_quest
PORT=8003
JWT_SECRET=your-secret-key-here
```

## Все доступные параметры

### 📦 База данных

```env
# PostgreSQL (обязательно)
DATABASE_URL=postgres://postgres:password@localhost:5432/irlquest
```

### 🌐 Сервер

```env
PORT=8003                          # Порт для HTTP сервера
PUBLIC_IP=1.2.3.4                  # Опционально: публичный IP (автодетект если не указан)
```

### 🔐 Аутентификация

```env
JWT_SECRET=change-me-in-production  # ОБЯЗАТЕЛЬНО измените в production!
JWT_EXPIRATION_HOURS=24            # Время жизни JWT токена (часы)
REFRESH_TOKEN_EXPIRATION_DAYS=30  # Время жизни refresh token (дни)
PASSWORD_MIN_LENGTH=8              # Минимальная длина пароля
```

### 🛡️ Безопасность

```env
RATE_LIMIT_PER_MINUTE=60          # Максимум запросов в минуту
RATE_LIMIT_BURST=10               # Burst лимит для rate limiting
ENABLE_MFA=false                   # Многофакторная аутентификация (TOTP)
```

### 🤖 ML / AI

```env
ML_BASE_URL=http://localhost:11434  # URL для Ollama или другого ML сервиса
ML_MODEL_PATH=/path/to/model        # Опционально: путь к локальной модели
ML_INFER_CMD=                       # Опционально: команда для inference
ML_EMBED_CMD=                       # Опционально: команда для embeddings
```

### 🎮 Функциональность

```env
ENABLE_OAUTH=false                 # OAuth2 аутентификация (Google, Apple)
ENABLE_AR=false                    # AR функциональность
ENABLE_MULTIPLAYER=false           # Мультиплеер и WebSocket
ENABLE_IMAGE_PROCESSING=false      # Обработка изображений
IMAGE_RETENTION_MINUTES=5          # Время хранения изображений перед удалением
```

### 🌍 CORS

```env
CORS_ORIGIN=*                      # Разрешенные origins (* для разработки)
# Для production:
# CORS_ORIGIN=https://yourdomain.com,https://app.yourdomain.com
```

### 📊 Клиентская конфигурация

```env
CLIENT_CONFIG_ENDPOINT=true        # Включить /api/config endpoint
```

## 📱 Endpoint /api/config

Мобильное приложение может запросить конфигурацию сервера:

**Запрос:**
```bash
GET http://server:8003/api/config
```

**Ответ:**
```json
{
  "api_version": "2.1.0",
  "server_url": "http://192.168.1.108:8003",
  "features": {
    "oauth_enabled": false,
    "mfa_enabled": false,
    "ar_enabled": false,
    "multiplayer_enabled": false,
    "image_processing_enabled": false
  },
  "limits": {
    "max_quest_difficulty": 10,
    "max_party_size": 5,
    "max_inventory_size": 100
  }
}
```

## 🚀 IP адреса

Сервер автоматически определяет локальный IP адрес.

Для указания публичного IP используйте переменную окружения:
```env
PUBLIC_IP=your.public.ip.here
```

Логи при старте:
```
🏠 Local IP detected: 192.168.1.108
📱 Mobile clients can use: http://192.168.1.108:8003
```

## 🔒 Production рекомендации

### Обязательные изменения:

1. **Измените JWT_SECRET**:
   ```env
   JWT_SECRET=$(openssl rand -base64 64)
   ```

2. **Используйте PostgreSQL**:
   ```env
   DATABASE_URL=postgresql://user:pass@localhost:5432/irl_quest
   ```

3. **Настройте CORS**:
   ```env
   CORS_ORIGIN=https://yourdomain.com
   ```

4. **Включите безопасность**:
   ```env
   ENABLE_MFA=true
   PASSWORD_MIN_LENGTH=12
   RATE_LIMIT_PER_MINUTE=30
   ```

### Опциональные улучшения:

5. **HTTPS** - используйте reverse proxy (nginx/caddy)
6. **Firewall** - ограничьте доступ к порту сервера
7. **Мониторинг** - настройте логирование и алерты
8. **Backup** - регулярные бэкапы базы данных

## 📖 Примеры конфигураций

### Разработка (локально):

```env
DATABASE_URL=postgres://postgres:password@localhost:5432/irlquest
PORT=8003
JWT_SECRET=dev-secret-not-for-production
CORS_ORIGIN=*
RATE_LIMIT_PER_MINUTE=100
ENABLE_MFA=false
```

### Staging:

```env
DATABASE_URL=postgresql://user:pass@db:5432/irl_quest_staging
PORT=8003
JWT_SECRET=staging-secret-from-vault
CORS_ORIGIN=https://staging.irlquest.com
RATE_LIMIT_PER_MINUTE=60
ENABLE_MFA=true
PUBLIC_IP=staging.irlquest.com
```

### Production:

```env
DATABASE_URL=postgresql://user:pass@db-primary:5432/irl_quest
PORT=8003
JWT_SECRET=prod-secret-from-hsm
CORS_ORIGIN=https://irlquest.com,https://app.irlquest.com
RATE_LIMIT_PER_MINUTE=30
RATE_LIMIT_BURST=5
ENABLE_MFA=true
PASSWORD_MIN_LENGTH=12
ENABLE_OAUTH=true
ENABLE_MULTIPLAYER=true
PUBLIC_IP=irlquest.com
```

## 🔍 Отладка

Для детального логирования:

```env
RUST_LOG=debug,tower_http=debug,sqlx=info
```

Уровни логирования:
- `error` - только ошибки
- `warn` - предупреждения и ошибки
- `info` - информация, предупреждения, ошибки (по умолчанию)
- `debug` - детальная информация
- `trace` - максимальная детализация

## ❓ FAQ

**Q: Как узнать свой публичный IP?**  
A: Сервер автоматически определит его при старте. Или проверьте вручную: `curl https://api.ipify.org`

**Q: Какую версию PostgreSQL использовать?**  
A: Рекомендуется PostgreSQL 14 или выше для лучшей производительности и поддержки современных функций.

**Q: Нужно ли перезапускать сервер после изменения .env?**  
A: Да, конфигурация загружается только при старте.

**Q: Как защитить JWT_SECRET?**  
A: Используйте секрет-менеджеры (HashiCorp Vault, AWS Secrets Manager) или переменные окружения ОС.

