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
    Write-Host "Failed to get admin token. Make sure Keycloak is running."
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Create a test user
$userBody = @{
    username = "testuser"
    email = "testuser@example.com"
    firstName = "Test"
    lastName = "User"
    enabled = $true
    emailVerified = $true
    credentials = @(
        @{
            type = "password"
            value = "password123"
            temporary = $false
        }
    )
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users" `
        -Method Post `
        -Headers $headers `
        -Body $userBody
    Write-Host "Test user created successfully!"
    Write-Host "Username: testuser"
    Write-Host "Password: password123"
}
catch {
    Write-Host "Error creating user (may already exist): $_"
}

# Get the user ID
$users = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=testuser" `
    -Method Get `
    -Headers $headers

if ($users.Count -gt 0) {
    $userId = $users[0].id
    
    # Get case_manager role ID
    $roles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" `
        -Method Get `
        -Headers $headers
    
    $caseManagerRole = $roles | Where-Object { $_.name -eq "case_manager" }
    
    if ($caseManagerRole) {
        # Assign role to user
        $roleMapping = @(
            @{
                id = $caseManagerRole.id
                name = $caseManagerRole.name
            }
        ) | ConvertTo-Json
        
        try {
            Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$userId/role-mappings/realm" `
                -Method Post `
                -Headers $headers `
                -Body $roleMapping
            Write-Host "Assigned 'case_manager' role to test user"
        }
        catch {
            # Role might already be assigned
        }
    }
}