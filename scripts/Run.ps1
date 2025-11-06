# Build and Run IRL-Quest Server
param(
    [switch]$SkipBuild = $false,
    [int]$Port = 8003
)

Write-Host "================================" -ForegroundColor Magenta
Write-Host " IRL-Quest Server" -ForegroundColor Magenta
Write-Host "================================" -ForegroundColor Magenta

$scriptDir = $PSScriptRoot

# Build if not skipped
if (-not $SkipBuild) {
    Write-Host "`nStep 1: Building..." -ForegroundColor Cyan
    & "$scriptDir\Build.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`nBuild failed. Exiting." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "`nSkipping build..." -ForegroundColor Gray
}

# Stop old server
Write-Host "`nStep 2: Stopping old server..." -ForegroundColor Cyan
& "$scriptDir\Stop.ps1"

Start-Sleep -Seconds 2

# Start new server
Write-Host "`nStep 3: Starting server..." -ForegroundColor Cyan
& "$scriptDir\Start.ps1" -Port $Port

