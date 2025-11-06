# Build IRL-Quest Server
Write-Host "Building IRL-Quest Server..." -ForegroundColor Cyan

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location "$projectRoot\server-rust"

try {
    Write-Host "Compiling Rust server (release mode)..." -ForegroundColor Yellow
    cargo build --release --bin irl-quest-server
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`nBuild successful!" -ForegroundColor Green
        Write-Host "Executable: server-rust\target\release\irl-quest-server.exe" -ForegroundColor Gray
    } else {
        Write-Host "`nBuild failed!" -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

