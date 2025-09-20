# Requires: Rust toolchain and PostgreSQL connection string in DATABASE_URL
# Usage: powershell -ExecutionPolicy Bypass -File .\scripts\run_full_server.ps1

param(
    [string]$ServerHost = $env:SERVER_HOST
        ? $env:SERVER_HOST : "0.0.0.0",
    [int]$ServerPort = $env:SERVER_PORT
        ? [int]$env:SERVER_PORT : 8003,
    [string]$DatabaseUrl = $env:DATABASE_URL
)

$ErrorActionPreference = "Stop"

Write-Host "=== IRL Quest Rust Server (Full) ==="

if (-not $DatabaseUrl) {
    Write-Host "DATABASE_URL not set. Example:"
    Write-Host "  postgresql://postgres:password@localhost:5432/irl_quest"
    throw "Please set DATABASE_URL environment variable."
}

# Ensure Rust is installed
if (-not (Get-Command cargo -ErrorAction SilentlyContinue)) {
    Write-Host "Rust not found. Please install Rust (https://rustup.rs) and re-run."
    exit 1
}

Push-Location "$PSScriptRoot\..\server-rust"

# Set env
$env:DATABASE_URL = $DatabaseUrl
$env:SERVER_HOST = $ServerHost
$env:SERVER_PORT = "$ServerPort"
if (-not $env:SECRET_KEY) { $env:SECRET_KEY = "rust-secret-key-for-irl-quest-dev" }
if (-not $env:JWT_ALGORITHM) { $env:JWT_ALGORITHM = "HS256" }
if (-not $env:ACCESS_TOKEN_EXPIRE_MINUTES) { $env:ACCESS_TOKEN_EXPIRE_MINUTES = "60" }

Write-Host "Building release binary..."
cargo build --release

$bin = "$(Get-Location)\target\release\irl-quest-server.exe"
if (-not (Test-Path $bin)) {
    # On Windows, the artifact might be without .exe depending on target; check both
    $bin = "$(Get-Location)\target\release\irl-quest-server"
}

Write-Host "Starting server on $ServerHost:$ServerPort"
& $bin
Pop-Location
