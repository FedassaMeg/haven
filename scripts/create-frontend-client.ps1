# Create proper frontend client with role mappings

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

# Delete existing client if it exists
try {
    $clients = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients?clientId=haven-frontend" -Headers $headers
    if ($clients.Count -gt 0) {
        $clientId = $clients[0].id
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients/$clientId" -Method Delete -Headers $headers
        Write-Host "Deleted existing haven-frontend client" -ForegroundColor Yellow
    }
} catch {
    Write-Host "No existing client to delete" -ForegroundColor Gray
}

# Create new frontend client with proper role mapping
$clientConfig = @{
    clientId = "haven-frontend"
    name = "Haven Frontend"
    enabled = $true
    publicClient = $true
    directAccessGrantsEnabled = $true
    standardFlowEnabled = $true
    implicitFlowEnabled = $false
    redirectUris = @(
        "http://localhost:3000/*"
    )
    webOrigins = @("http://localhost:3000")
    protocolMappers = @(
        @{
            name = "role list"
            protocol = "openid-connect"
            protocolMapper = "oidc-usermodel-realm-role-mapper"
            consentRequired = $false
            config = @{
                "multivalued" = "true"
                "userinfo.token.claim" = "true"
                "id.token.claim" = "true"
                "access.token.claim" = "true"
                "claim.name" = "roles"
                "jsonType.label" = "String"
            }
        }
    )
} | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients" -Method Post -Headers $headers -Body $clientConfig
    Write-Host "✓ Created haven-frontend client with role mapping!" -ForegroundColor Green
} catch {
    Write-Host "❌ Error creating client: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ Frontend client setup complete!" -ForegroundColor Green
Write-Host "Now try logging in:" -ForegroundColor Cyan
Write-Host "Admin: admin.test / admin123" -ForegroundColor White
Write-Host "Case Manager: case.manager / manager123" -ForegroundColor White