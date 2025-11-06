# Test IRL-Quest API
Write-Host "Testing IRL-Quest API..." -ForegroundColor Cyan

$baseUrl = "http://192.168.1.67:8003/api/v1"

# Test login
Write-Host "`n1. Testing /auth/login..." -ForegroundColor Yellow
try {
    $loginBody = @{
        username = "testuser"
        password = "password"
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    Write-Host "   OK - Token received" -ForegroundColor Green
    Write-Host "   User ID: $($loginResponse.user_id)" -ForegroundColor Gray
    Write-Host "   Username: $($loginResponse.username)" -ForegroundColor Gray
    Write-Host "   Client IP: $($loginResponse.client_ip)" -ForegroundColor Gray
    
    Write-Host "`n✅ All tests passed!" -ForegroundColor Green
    Write-Host "`nServer is working correctly!" -ForegroundColor Cyan
    
} catch {
    Write-Host "   FAIL: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Response: $responseBody" -ForegroundColor Red
    }
}
