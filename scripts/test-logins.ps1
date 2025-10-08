# Test the actual login tokens to see what roles are returned

Write-Host "Testing user logins to see actual roles..." -ForegroundColor Green

# Test admin.test login
Write-Host "Testing admin.test login..." -ForegroundColor Yellow
try {
    $adminLogin = Invoke-RestMethod -Uri "http://localhost:8081/realms/haven/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body @{
        username = "admin.test"
        password = "admin123"
        grant_type = "password"
        client_id = "haven-frontend"
    }
    
    # Decode the JWT token to see roles (simple base64 decode)
    $tokenParts = $adminLogin.access_token.Split('.')
    $payload = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($tokenParts[1] + "=="))
    
    Write-Host "Admin login successful!" -ForegroundColor Green
    Write-Host "Token payload contains:" -ForegroundColor White
    $payload | Write-Host
} catch {
    Write-Host "Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test case.manager login
Write-Host "Testing case.manager login..." -ForegroundColor Yellow
try {
    $caseLogin = Invoke-RestMethod -Uri "http://localhost:8081/realms/haven/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body @{
        username = "case.manager"
        password = "manager123"
        grant_type = "password"
        client_id = "haven-frontend"
    }
    
    # Decode the JWT token to see roles
    $tokenParts = $caseLogin.access_token.Split('.')
    $payload = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($tokenParts[1] + "=="))
    
    Write-Host "Case manager login successful!" -ForegroundColor Green
    Write-Host "Token payload contains:" -ForegroundColor White
    $payload | Write-Host
} catch {
    Write-Host "Case manager login failed: $($_.Exception.Message)" -ForegroundColor Red
}