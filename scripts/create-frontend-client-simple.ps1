$response = Invoke-RestMethod -Uri "http://localhost:8081/realms/master/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body @{
    username = "admin"
    password = "admin"
    grant_type = "password"
    client_id = "admin-cli"
}

$headers = @{
    "Authorization" = "Bearer $($response.access_token)"
    "Content-Type" = "application/json"
}

$clientConfig = @{
    clientId = "haven-frontend"
    name = "Haven Frontend"
    enabled = $true
    publicClient = $true
    directAccessGrantsEnabled = $true
    standardFlowEnabled = $true
    redirectUris = @("http://localhost:3000/*")
    webOrigins = @("http://localhost:3000")
} | ConvertTo-Json -Depth 5

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients" -Method Post -Headers $headers -Body $clientConfig
    Write-Host "Frontend client created successfully!" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "Client already exists - thats OK!" -ForegroundColor Green
    } else {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "Setup complete - test the login now!"