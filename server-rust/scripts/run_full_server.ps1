param(
    [string]$DatabaseUrl = $env:DATABASE_URL,
    [string]$Host = $(if ($env:SERVER_HOST) { $env:SERVER_HOST } else { "0.0.0.0" }),
    [int]$Port = $(if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8003 })
)

Write-Host "IRL Quest - Building full Rust server (release)..."
# Ensure Rust toolchain is installed on the Windows Server
# choco install rust -y  # Uncomment if Chocolatey is available

$env:DATABASE_URL = if ($DatabaseUrl) { $DatabaseUrl } else { "postgresql://postgres:password@localhost:5432/irl_quest" }
$env:SERVER_HOST = $Host
$env:SERVER_PORT = $Port
$env:SECRET_KEY = if ($env:SECRET_KEY) { $env:SECRET_KEY } else { "rust-secret-key-for-irl-quest-dev" }
$env:JWT_ALGORITHM = if ($env:JWT_ALGORITHM) { $env:JWT_ALGORITHM } else { "HS256" }
$env:ACCESS_TOKEN_EXPIRE_MINUTES = if ($env:ACCESS_TOKEN_EXPIRE_MINUTES) { $env:ACCESS_TOKEN_EXPIRE_MINUTES } else { 60 }

Push-Location "$PSScriptRoot\..\server-rust"

# Build
cargo build --release
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed"; exit 1 }

# Run
$bin = Join-Path (Join-Path (Get-Location) "target\release") "irl-quest-server.exe"
if (-not (Test-Path $bin)) { $bin = Join-Path (Join-Path (Get-Location) "target\release") "irl-quest-server" }

Write-Host "Starting server on http://$Host:$Port ..."
& $bin
