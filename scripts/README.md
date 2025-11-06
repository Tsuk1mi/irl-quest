# IRL-Quest Scripts

Набор PowerShell скриптов для управления сервером.

## Требования

- PostgreSQL установлен и запущен
- База данных: `irl_quest`
- Пользователь: `postgres`
- Пароль: `tsukimi`

## Скрипты

### Build.ps1
Собирает Rust сервер в release режиме.

```powershell
.\scripts\Build.ps1
```

### Start.ps1
Запускает сервер (необходимо собрать перед первым запуском).

```powershell
.\scripts\Start.ps1
# Или на другом порту:
.\scripts\Start.ps1 -Port 8004
```

### Stop.ps1
Останавливает все запущенные экземпляры сервера.

```powershell
.\scripts\Stop.ps1
```

### Run.ps1
Собирает и запускает сервер (все в одном).

```powershell
.\scripts\Run.ps1
# Пропустить сборку:
.\scripts\Run.ps1 -SkipBuild
# Другой порт:
.\scripts\Run.ps1 -Port 8004
```

### Test.ps1
Тестирует API сервера (должен быть запущен).

```powershell
.\scripts\Test.ps1
```

## Быстрый старт

1. **Первый запуск:**
```powershell
# Собрать и запустить
.\scripts\Run.ps1
```

2. **В другом окне PowerShell - протестировать:**
```powershell
.\scripts\Test.ps1
```

3. **Остановить:**
```powershell
.\scripts\Stop.ps1
```

## Настройка мобильного приложения

IP сервера настраивается в `mobile/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.67:8003/api/v1/\"")
```

Замените `192.168.1.67` на IP вашего компьютера.

## Устранение неполадок

### База данных не подключается

Проверьте параметры подключения в скриптах:
- Host: `localhost`
- Port: `5432`
- Database: `irl_quest`
- User: `postgres`
- Password: `tsukimi`

### Порт 8003 занят

Остановите старые процессы:
```powershell
.\scripts\Stop.ps1
```

Или используйте другой порт:
```powershell
.\scripts\Start.ps1 -Port 8004
```

### Сервер не собирается

Убедитесь, что установлен Rust:
```powershell
cargo --version
```

Если нет, установите с https://rustup.rs

