# Start IRL-Quest Server
param(
    [int]$Port = 8003
)

Write-Host "Starting IRL-Quest Server..." -ForegroundColor Cyan

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

# Stop old server first
Write-Host "Stopping old server instances..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*irl-quest-server*" -or $_.Path -like "*irl-quest-server*"} | Stop-Process -Force -ErrorAction SilentlyContinue

Start-Sleep -Seconds 1

# Set environment variables
$env:DATABASE_URL = "postgresql://postgres:tsukimi@localhost:5432/irl_quest"
$env:JWT_SECRET = "your-secret-key-change-in-production"
$env:PORT = $Port
$env:RUST_LOG = "info"

Write-Host "`nEnvironment:" -ForegroundColor Gray
Write-Host "  Port: $Port" -ForegroundColor Gray
Write-Host "  Database: $env:DATABASE_URL" -ForegroundColor Gray
Write-Host "`nStarting server..." -ForegroundColor Green

Push-Location "$projectRoot\server-rust"

try {
    & ".\target\release\irl-quest-server.exe"
} finally {
    Pop-Location
}

