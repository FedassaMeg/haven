# Simple Keycloak Setup for Haven Dashboard Testing

Write-Host "Setting up Keycloak for Haven..." -ForegroundColor Green

# Get admin token
$tokenBody = @{
    username = "admin"
    password = "admin" 
    grant_type = "password"
    client_id = "admin-cli"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri "http://localhost:8081/realms/master/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body $tokenBody
    $token = $tokenResponse.access_token
    Write-Host "Admin token obtained" -ForegroundColor Green
} catch {
    Write-Host "Failed to get admin token. Make sure Keycloak is running." -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Create haven realm
Write-Host "Creating haven realm..." -ForegroundColor Yellow
$realmBody = @{
    realm = "haven"
    enabled = $true
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms" -Method Post -Headers $headers -Body $realmBody
    Write-Host "Haven realm created" -ForegroundColor Green
} catch {
    Write-Host "Realm may already exist" -ForegroundColor Yellow
}

# Create roles
Write-Host "Creating roles..." -ForegroundColor Yellow
$roles = @("admin", "case-manager", "supervisor", "social-worker")

foreach ($roleName in $roles) {
    $roleBody = @{
        name = $roleName
        description = "Role: $roleName"
    } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" -Method Post -Headers $headers -Body $roleBody
        Write-Host "Created role: $roleName" -ForegroundColor Green
    } catch {
        Write-Host "Role $roleName may already exist" -ForegroundColor Yellow
    }
}

# Create admin user
Write-Host "Creating admin user..." -ForegroundColor Yellow
$adminUserBody = @{
    username = "admin.test"
    email = "admin.test@haven.local"
    firstName = "Admin"
    lastName = "Test"
    enabled = $true
    emailVerified = $true
    credentials = @(
        @{
            type = "password"
            value = "admin123"
            temporary = $false
        }
    )
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users" -Method Post -Headers $headers -Body $adminUserBody
    Write-Host "Admin user created" -ForegroundColor Green
} catch {
    Write-Host "Admin user may already exist" -ForegroundColor Yellow
}

# Create case manager user
Write-Host "Creating case manager user..." -ForegroundColor Yellow
$caseManagerUserBody = @{
    username = "case.manager"
    email = "case.manager@haven.local"
    firstName = "Case"
    lastName = "Manager"
    enabled = $true
    emailVerified = $true
    credentials = @(
        @{
            type = "password"
            value = "manager123"
            temporary = $false
        }
    )
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users" -Method Post -Headers $headers -Body $caseManagerUserBody
    Write-Host "Case manager user created" -ForegroundColor Green
} catch {
    Write-Host "Case manager user may already exist" -ForegroundColor Yellow
}

# Assign roles
Write-Host "Assigning roles..." -ForegroundColor Yellow

# Get users and roles
$allRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" -Method Get -Headers $headers
$adminUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=admin.test" -Method Get -Headers $headers
$caseManagerUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=case.manager" -Method Get -Headers $headers

# Assign admin role
if ($adminUsers.Count -gt 0) {
    $adminUserId = $adminUsers[0].id
    $adminRole = $allRoles | Where-Object { $_.name -eq "admin" }
    
    if ($adminRole) {
        $roleMapping = @(@{
            id = $adminRole.id
            name = $adminRole.name
        }) | ConvertTo-Json
        
        try {
            Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" -Method Post -Headers $headers -Body $roleMapping
            Write-Host "Assigned admin role" -ForegroundColor Green
        } catch {
            Write-Host "Admin role may already be assigned" -ForegroundColor Yellow
        }
    }
}

# Assign case manager role
if ($caseManagerUsers.Count -gt 0) {
    $caseManagerUserId = $caseManagerUsers[0].id
    $caseManagerRole = $allRoles | Where-Object { $_.name -eq "case-manager" }
    
    if ($caseManagerRole) {
        $roleMapping = @(@{
            id = $caseManagerRole.id
            name = $caseManagerRole.name
        }) | ConvertTo-Json
        
        try {
            Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" -Method Post -Headers $headers -Body $roleMapping
            Write-Host "Assigned case-manager role" -ForegroundColor Green
        } catch {
            Write-Host "Case-manager role may already be assigned" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "ADMIN USER: admin.test / admin123" -ForegroundColor White
Write-Host "CASE MANAGER: case.manager / manager123" -ForegroundColor White
Write-Host ""
Write-Host "Login at: http://localhost:3000/login" -ForegroundColor Cyan