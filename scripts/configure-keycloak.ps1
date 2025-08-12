# Wait for Keycloak to be ready
Write-Host "Waiting for Keycloak to be ready..."
while ($true) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/realms/master" -UseBasicParsing -ErrorAction Stop
        break
    }
    catch {
        Start-Sleep -Seconds 2
    }
}

Write-Host "Keycloak is ready. Configuring..."

# Get admin token
$tokenBody = @{
    username = "admin"
    password = "admin"
    grant_type = "password"
    client_id = "admin-cli"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri "http://localhost:8081/realms/master/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody
    
    $token = $tokenResponse.access_token
}
catch {
    Write-Host "Failed to get admin token. Make sure Keycloak is running with admin/admin credentials."
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Create realm if it doesn't exist
$realmBody = @{
    realm = "haven"
    enabled = $true
    sslRequired = "external"
    registrationAllowed = $true
    loginWithEmailAllowed = $true
    duplicateEmailsAllowed = $false
    resetPasswordAllowed = $true
    editUsernameAllowed = $false
    bruteForceProtected = $true
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms" `
        -Method Post `
        -Headers $headers `
        -Body $realmBody `
        -ErrorAction SilentlyContinue
}
catch {
    # Realm might already exist
}

# Create or update the frontend client
$clientBody = @{
    clientId = "haven-frontend"
    enabled = $true
    publicClient = $true
    redirectUris = @(
        "http://localhost:3000/*",
        "http://localhost:3001/*",
        "http://localhost:3002/*"
    )
    webOrigins = @(
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:3002",
        "+"
    )
    protocol = "openid-connect"
    attributes = @{
        "pkce.code.challenge.method" = "S256"
        "post.logout.redirect.uris" = "http://localhost:3000/*"
    }
    standardFlowEnabled = $true
    implicitFlowEnabled = $false
    directAccessGrantsEnabled = $true
    serviceAccountsEnabled = $false
} | ConvertTo-Json -Depth 3

# Check if client exists
try {
    $existingClients = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients?clientId=haven-frontend" `
        -Method Get `
        -Headers $headers
    
    if ($existingClients.Count -gt 0) {
        $clientId = $existingClients[0].id
        # Update existing client
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients/$clientId" `
            -Method Put `
            -Headers $headers `
            -Body $clientBody
        Write-Host "Client 'haven-frontend' updated successfully"
    }
    else {
        # Create new client
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients" `
            -Method Post `
            -Headers $headers `
            -Body $clientBody
        Write-Host "Client 'haven-frontend' created successfully"
    }
}
catch {
    # Try to create new client
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients" `
            -Method Post `
            -Headers $headers `
            -Body $clientBody
        Write-Host "Client 'haven-frontend' created successfully"
    }
    catch {
        Write-Host "Error configuring client: $_"
    }
}

# Create roles
$roles = @("admin", "case_manager", "social_worker", "supervisor", "viewer")
foreach ($role in $roles) {
    $roleBody = @{ name = $role } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" `
            -Method Post `
            -Headers $headers `
            -Body $roleBody `
            -ErrorAction SilentlyContinue
    }
    catch {
        # Role might already exist
    }
}

Write-Host "Keycloak configuration completed successfully!"
Write-Host ""
Write-Host "Access Keycloak Admin Console at: http://localhost:8081"
Write-Host "Username: admin"
Write-Host "Password: admin"