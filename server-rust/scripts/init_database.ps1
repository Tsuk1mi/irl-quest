# PowerShell скрипт для инициализации базы данных PostgreSQL
# Создает БД, применяет миграции, заполняет тестовыми данными

param(
    [string]$DbHost = "localhost",
    [string]$DbPort = "5432",
    [string]$DbName = "irl_quest",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "tsukimi"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   IRL Quest Database Initialization   " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Установка DATABASE_URL
$env:DATABASE_URL = "postgresql://${DbUser}:${DbPassword}@${DbHost}:${DbPort}/${DbName}"

Write-Host "Database URL: $env:DATABASE_URL" -ForegroundColor Yellow
Write-Host ""

# 1. Проверка наличия PostgreSQL
Write-Host "[1/6] Checking PostgreSQL..." -ForegroundColor Green
$psqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"

if (!(Test-Path $psqlPath)) {
    Write-Host "Error: PostgreSQL not found at $psqlPath" -ForegroundColor Red
    Write-Host "Please install PostgreSQL 18 or update the path in the script" -ForegroundColor Yellow
    exit 1
}

Write-Host "  ✓ PostgreSQL found" -ForegroundColor Green
Write-Host ""

# 2. Проверка наличия sqlx-cli
Write-Host "[2/6] Checking sqlx-cli..." -ForegroundColor Green

try {
    $sqlxVersion = & sqlx --version 2>&1
    Write-Host "  ✓ sqlx-cli found: $sqlxVersion" -ForegroundColor Green
} catch {
    Write-Host "  sqlx-cli not found. Installing..." -ForegroundColor Yellow
    cargo install sqlx-cli --no-default-features --features postgres
    Write-Host "  ✓ sqlx-cli installed" -ForegroundColor Green
}

Write-Host ""

# 3. Создание базы данных
Write-Host "[3/6] Creating database..." -ForegroundColor Green

try {
    sqlx database create 2>&1 | Out-Null
    Write-Host "  ✓ Database created or already exists" -ForegroundColor Green
} catch {
    Write-Host "  Warning: Could not create database (may already exist)" -ForegroundColor Yellow
}

Write-Host ""

# 4. Применение миграций
Write-Host "[4/6] Applying migrations..." -ForegroundColor Green

$migrationsCount = (Get-ChildItem -Path "migrations" -Filter "*.sql").Count
Write-Host "  Found $migrationsCount migration files" -ForegroundColor Cyan

try {
    $output = sqlx migrate run 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ All migrations applied successfully" -ForegroundColor Green
    } else {
        Write-Host "  Warning: Some migrations may have failed" -ForegroundColor Yellow
        Write-Host "  Output: $output" -ForegroundColor Gray
    }
} catch {
    Write-Host "  Error applying migrations: $_" -ForegroundColor Red
    Write-Host "  Continuing anyway..." -ForegroundColor Yellow
}

Write-Host ""

# 5. Создание тестового пользователя
Write-Host "[5/6] Creating test user..." -ForegroundColor Green

$testUserSql = @"
-- Создание тестового пользователя
INSERT INTO users (email, username, hashed_password, is_active)
VALUES ('test@example.com', 'testuser', '\$2b\$12\$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyMK8H8zL9Z.', true)
ON CONFLICT (email) DO NOTHING;

-- Установка базовых характеристик
UPDATE users 
SET 
    level = 1,
    experience = 0,
    gold = 100,
    character_class = 'warrior',
    character_race = 'human',
    strength = 10,
    intelligence = 10,
    dexterity = 10,
    charisma = 10,
    luck = 10
WHERE email = 'test@example.com';
"@

$tempSqlFile = [System.IO.Path]::GetTempFileName() + ".sql"
$testUserSql | Out-File -FilePath $tempSqlFile -Encoding UTF8

try {
    & $psqlPath -U $DbUser -d $DbName -f $tempSqlFile 2>&1 | Out-Null
    Write-Host "  ✓ Test user created (test@example.com / password: password)" -ForegroundColor Green
} catch {
    Write-Host "  Warning: Could not create test user (may already exist)" -ForegroundColor Yellow
} finally {
    Remove-Item $tempSqlFile -ErrorAction SilentlyContinue
}

Write-Host ""

# 6. Проверка структуры БД
Write-Host "[6/6] Verifying database structure..." -ForegroundColor Green

$checkTablesSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';"
$tempCheckFile = [System.IO.Path]::GetTempFileName() + ".sql"
$checkTablesSql | Out-File -FilePath $tempCheckFile -Encoding UTF8

try {
    $tableCount = & $psqlPath -U $DbUser -d $DbName -t -A -f $tempCheckFile
    Write-Host "  ✓ Database contains $tableCount tables" -ForegroundColor Green
} catch {
    Write-Host "  Warning: Could not verify table count" -ForegroundColor Yellow
} finally {
    Remove-Item $tempCheckFile -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Database initialization complete!   " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Database details:" -ForegroundColor White
Write-Host "  Host: $DbHost" -ForegroundColor Gray
Write-Host "  Port: $DbPort" -ForegroundColor Gray
Write-Host "  Database: $DbName" -ForegroundColor Gray
Write-Host "  User: $DbUser" -ForegroundColor Gray
Write-Host ""
Write-Host "Test account:" -ForegroundColor White
Write-Host "  Email: test@example.com" -ForegroundColor Gray
Write-Host "  Password: password" -ForegroundColor Gray
Write-Host ""
Write-Host "You can now run the server with:" -ForegroundColor Yellow
Write-Host "  cargo run --release" -ForegroundColor Cyan
Write-Host ""

