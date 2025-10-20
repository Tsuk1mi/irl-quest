<#
run-mobile-gradle.ps1
Поиск JDK, временная настройка JAVA_HOME/PATH и запуск mobile\gradlew.bat с аргументами.
Запуск (из корня репозитория):
    powershell -ExecutionPolicy Bypass -File .\scripts\run-mobile-gradle.ps1 tasks
Или внутри PowerShell:
    .\scripts\run-mobile-gradle.ps1 tasks
#>
param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [String[]]$GradleArgs
)

function Find-JavaHome {
    # 1) если уже задано в окружении и корректно
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $candidate) { return $env:JAVA_HOME }
    }

    # 2) проверить наличие java в PATH
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) {
        try {
            $javaPath = (Get-Command java).Source
            # если это java.exe из bin, вернуть родитель папки на два уровня (bin -> jdk/jre)
            $bin = Split-Path $javaPath -Parent
            $jdkRoot = Split-Path $bin -Parent

            # Если найден JRE (путь содержит 'jre'), попробуем найти соседний JDK
            if ($jdkRoot -match '[\\/]jre(\d+)?$' -or $jdkRoot.ToLower().Contains('jre')) {
                $parent = Split-Path $jdkRoot -Parent
                if (Test-Path $parent) {
                    $siblings = Get-ChildItem -Path $parent -Directory -ErrorAction SilentlyContinue
                    $prefer = $siblings | Where-Object { $_.Name -match 'jdk|openjdk|temurin|corretto' } | Select-Object -First 1
                    if ($prefer) { return $prefer.FullName }
                }
            }

            return $jdkRoot
        } catch { }
    }

    # 3) проверить типичные папки установки (не рекурсивно глубоко)
    $candidates = @(
        'C:\Program Files\Java',
        'C:\Program Files (x86)\Java',
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\AdoptOpenJDK',
        'C:\Program Files\Amazon Corretto'
    )
    foreach ($base in $candidates) {
        if (Test-Path $base) {
            # сначала ищем JDK-папки
            $jdkDirs = Get-ChildItem -Path $base -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'jdk|openjdk|temurin|corretto' }
            if ($jdkDirs -and $jdkDirs.Count -gt 0) { return $jdkDirs[0].FullName }
            # иначе вернём любую папку, содержащую bin\java.exe (включая jre)
            Get-ChildItem -Path $base -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $maybe = Join-Path $_.FullName 'bin\java.exe'
                if (Test-Path $maybe) { return $_.FullName }
            }
        }
    }

    # 4) попытаться прочитать реестр (JDK)
    try {
        $regKeys = @(
            'HKLM:\SOFTWARE\JavaSoft\Java Development Kit',
            'HKLM:\SOFTWARE\WOW6432Node\JavaSoft\Java Development Kit'
        )
        foreach ($k in $regKeys) {
            if (Test-Path $k) {
                $versions = Get-ChildItem $k -ErrorAction SilentlyContinue | Select-Object -ExpandProperty PSChildName
                foreach ($v in $versions) {
                    $home = (Get-ItemProperty -Path (Join-Path $k $v) -Name JavaHome -ErrorAction SilentlyContinue).JavaHome
                    if ($home) {
                        $bin = Join-Path $home 'bin\java.exe'
                        if (Test-Path $bin) { return $home }
                    }
                }
            }
        }
    } catch { }

    return $null
}

# основная логика
Write-Host "Запуск: mobile\gradlew.bat с аргументами: $($GradleArgs -join ' ')" -ForegroundColor Cyan

$javaHome = Find-JavaHome
# Если найден JRE (только), считаем это недостаточным — Gradle/Android требует JDK
if ($javaHome -and ($javaHome -match '(?i)[\\/]jre')) {
    Write-Host "Обнаружен JRE (не JDK): $javaHome" -ForegroundColor Yellow
    Write-Host "Gradle требует JDK (рекомендуется OpenJDK/Temurin 11 или 17). Пожалуйста, установите JDK и пропишите JAVA_HOME." -ForegroundColor Yellow
    $javaHome = $null
}
if (-not $javaHome) {
    Write-Host "JDK не найден в системе." -ForegroundColor Yellow
    Write-Host "Рекомендации:" -ForegroundColor Yellow
    Write-Host "  1) Установите JDK (Temurin/Adoptium, Corretto, OpenJDK). Рекомендуемые версии для Android/Gradle: 11 или 17." -ForegroundColor Yellow
    Write-Host "     Ссылка (Adoptium/Temurin): https://adoptium.net/" -ForegroundColor Yellow
    Write-Host "  2) После установки добавьте системную переменную JAVA_HOME = путь к JDK (без \bin) и в Path добавьте %JAVA_HOME%\bin" -ForegroundColor Yellow
    Write-Host "  3) Затем откройте новое окно PowerShell или cmd и запустите: .\mobile\gradlew.bat tasks" -ForegroundColor Yellow
    exit 2
}

Write-Host "Найден JDK: $javaHome" -ForegroundColor Green

# Защита и нормализация значений
$javaHome = $javaHome -as [string]
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    Write-Host "Ошибка: путь к Java пуст." -ForegroundColor Red
    exit 4
}

# Найдём bin и java.exe
$javaBin = Join-Path $javaHome 'bin'
$javaExe = Join-Path $javaBin 'java.exe'
if (-not (Test-Path $javaExe)) {
    # Возможно найден JRE/JDK без стандартной структуры; попробуем рекурсивно найти java.exe внутри каталога
    $found = Get-ChildItem -Path $javaHome -Filter java.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $javaExe = $found.FullName
        $javaBin = Split-Path $javaExe -Parent
    } else {
        Write-Host "В каталоге $javaHome не найден java.exe. Проверьте установку JDK/JRE." -ForegroundColor Yellow
        # Продолжим, но дальнейшие шаги, скорее всего, завершатся неудачей
    }
}

# Надёжно обновим PATH для текущей сессии
if (-not $env:Path) { $env:Path = $javaBin } else { $env:Path = $javaBin + ";" + $env:Path }
$env:JAVA_HOME = $javaHome

# покажем версию java — вызываем по полному пути, чтобы не зависеть от PATH
Write-Host "java -version:" -ForegroundColor Cyan
if (Test-Path $javaExe) {
    & "$javaExe" -version
} else {
    Write-Host "java.exe не найден, пропускаю вывод версии." -ForegroundColor Yellow
}

# путь к gradlew (надежнее через Get-Location)
$root = (Get-Location).Path
$gradlew = Join-Path $root 'mobile\gradlew.bat'
if (-not (Test-Path $gradlew)) {
    Write-Host "Файл mobile\gradlew.bat не найден по пути: $gradlew" -ForegroundColor Red
    exit 3
}

# Запустим gradlew через cmd (bat-файл корректно выполняется в cmd)
$argStr = if ($GradleArgs) { $GradleArgs -join ' ' } else { 'tasks' }
Write-Host "Выполняю: $gradlew $argStr" -ForegroundColor Cyan
$proc = Start-Process -FilePath cmd.exe -ArgumentList "/c `"$gradlew`" $argStr" -NoNewWindow -Wait -PassThru
if ($proc.ExitCode -ne 0) {
    Write-Host "gradlew завершился с кодом выхода $($proc.ExitCode)" -ForegroundColor Red
    exit $proc.ExitCode
}

Write-Host "Выполнение успешно." -ForegroundColor Green
exit 0
