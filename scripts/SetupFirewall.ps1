# Setup Windows Firewall for IRL-Quest Server
# REQUIRES ADMINISTRATOR PRIVILEGES

Write-Host "Setting up Windows Firewall for IRL-Quest..." -ForegroundColor Cyan

# Check if running as administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "`nERROR: This script requires administrator privileges!" -ForegroundColor Red
    Write-Host "Right-click PowerShell and select 'Run as Administrator'" -ForegroundColor Yellow
    Write-Host "`nThen run:" -ForegroundColor Yellow
    Write-Host "  .\scripts\SetupFirewall.ps1" -ForegroundColor White
    exit 1
}

# Remove old rules
Write-Host "Removing old firewall rules..." -ForegroundColor Yellow
netsh advfirewall firewall delete rule name="IRL-Quest Server" >$null 2>&1
netsh advfirewall firewall delete rule name="IRL-Quest" >$null 2>&1

# Add new rule
Write-Host "Adding firewall rule for port 8003..." -ForegroundColor Yellow
$result = netsh advfirewall firewall add rule name="IRL-Quest Server" dir=in action=allow protocol=TCP localport=8003

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nSUCCESS! Firewall configured." -ForegroundColor Green
    Write-Host "`nFirewall rule added:" -ForegroundColor Cyan
    Write-Host "  Name: IRL-Quest Server" -ForegroundColor Gray
    Write-Host "  Port: 8003" -ForegroundColor Gray
    Write-Host "  Protocol: TCP" -ForegroundColor Gray
    Write-Host "  Direction: Inbound" -ForegroundColor Gray
    Write-Host "  Action: Allow" -ForegroundColor Gray
} else {
    Write-Host "`nFAILED to add firewall rule!" -ForegroundColor Red
    Write-Host "Error code: $LASTEXITCODE" -ForegroundColor Red
}

Write-Host "`nPress any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

