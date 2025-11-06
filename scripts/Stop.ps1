# Stop IRL-Quest Server
Write-Host "Stopping IRL-Quest Server..." -ForegroundColor Cyan

# Find and kill server processes
$processes = Get-Process | Where-Object {$_.ProcessName -like "*irl-quest-server*" -or $_.Path -like "*irl-quest-server*"}

if ($processes) {
    $processes | ForEach-Object {
        Write-Host "Stopping process $($_.Id)..." -ForegroundColor Yellow
        Stop-Process -Id $_.Id -Force
    }
    Write-Host "`nServer stopped." -ForegroundColor Green
} else {
    Write-Host "`nNo server processes found." -ForegroundColor Gray
}

# Also kill by port 8003
$connections = netstat -ano | Select-String ":8003"
if ($connections) {
    $connections | ForEach-Object {
        if ($_ -match "\s+(\d+)\s*$") {
            $pid = $matches[1]
            Write-Host "Stopping process on port 8003 (PID: $pid)..." -ForegroundColor Yellow
            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        }
    }
}

