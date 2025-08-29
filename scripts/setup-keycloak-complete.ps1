# Complete Keycloak Setup for Haven
# This script sets up everything needed for testing the role-based dashboard

Write-Host "🚀 Setting up Keycloak for Haven Dashboard Testing" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan

# Get admin token from master realm
Write-Host "1. Getting admin token..." -ForegroundColor Yellow
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
    Write-Host "✓ Admin token obtained" -ForegroundColor Green
}
catch {
    Write-Host "❌ Failed to get admin token. Make sure Keycloak is running on port 8081" -ForegroundColor Red
    Write-Host "Run: docker compose -f docker-compose.dev.yaml up -d" -ForegroundColor Cyan
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Create Haven Realm
Write-Host ""
Write-Host "2. Creating 'haven' realm..." -ForegroundColor Yellow
$realmBody = @{
    realm = "haven"
    displayName = "Haven Case Management"
    enabled = $true
    loginWithEmailAllowed = $true
    duplicateEmailsAllowed = $false
    registrationAllowed = $false
    resetPasswordAllowed = $true
    rememberMe = $true
    accessTokenLifespan = 3600
    ssoSessionMaxLifespan = 36000
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms" `
        -Method Post `
        -Headers $headers `
        -Body $realmBody
    Write-Host "✓ Haven realm created successfully" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✓ Haven realm already exists" -ForegroundColor Green
    } else {
        Write-Host "❌ Error creating realm: $_" -ForegroundColor Red
        exit 1
    }
}

# Create Frontend Client
Write-Host ""
Write-Host "3. Creating frontend client..." -ForegroundColor Yellow
$frontendClientBody = @{
    clientId = "haven-frontend"
    name = "Haven Frontend"
    protocol = "openid-connect"
    enabled = $true
    publicClient = $true
    directAccessGrantsEnabled = $true
    standardFlowEnabled = $true
    implicitFlowEnabled = $false
    serviceAccountsEnabled = $false
    redirectUris = @(
        "http://localhost:3000/*",
        "http://localhost:3000/login/*",
        "http://localhost:3000/dashboard/*"
    )
    webOrigins = @("http://localhost:3000")
    attributes = @{
        "pkce.code.challenge.method" = "S256"
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients" `
        -Method Post `
        -Headers $headers `
        -Body $frontendClientBody
    Write-Host "✓ Frontend client created successfully" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✓ Frontend client already exists" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not create frontend client: $_" -ForegroundColor Yellow
    }
}

# Create Backend Client
Write-Host ""
Write-Host "4. Creating backend client..." -ForegroundColor Yellow
$backendClientBody = @{
    clientId = "haven-backend"
    name = "Haven Backend"
    protocol = "openid-connect"
    enabled = $true
    publicClient = $false
    directAccessGrantsEnabled = $true
    standardFlowEnabled = $true
    serviceAccountsEnabled = $true
    redirectUris = @("http://localhost:8080/*")
    webOrigins = @("http://localhost:8080")
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/clients" `
        -Method Post `
        -Headers $headers `
        -Body $backendClientBody
    Write-Host "✓ Backend client created successfully" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✓ Backend client already exists" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not create backend client: $_" -ForegroundColor Yellow
    }
}

# Create Roles
Write-Host ""
Write-Host "5. Creating realm roles..." -ForegroundColor Yellow
$roles = @(
    @{ name = "admin"; description = "System Administrator with full access" },
    @{ name = "case-manager"; description = "Case Manager with case management access" },
    @{ name = "supervisor"; description = "Supervisor with team oversight access" },
    @{ name = "social-worker"; description = "Social Worker with client access" },
    @{ name = "viewer"; description = "Read-only access" }
)

foreach ($roleData in $roles) {
    try {
        $roleBody = @{
            name = $roleData.name
            description = $roleData.description
        } | ConvertTo-Json

        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" `
            -Method Post `
            -Headers $headers `
            -Body $roleBody
        Write-Host "✓ Created role: $($roleData.name)" -ForegroundColor Green
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "✓ Role already exists: $($roleData.name)" -ForegroundColor Green
        } else {
            Write-Host "⚠️ Could not create role $($roleData.name): $_" -ForegroundColor Yellow
        }
    }
}

# Create Test Users
Write-Host ""
Write-Host "6. Creating test users..." -ForegroundColor Yellow

# Admin User
$adminUserBody = @{
    username = "admin.test"
    email = "admin.test@haven.local"
    firstName = "Admin"
    lastName = "Tester"
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
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users" `
        -Method Post `
        -Headers $headers `
        -Body $adminUserBody
    Write-Host "✓ Admin user created" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✓ Admin user already exists" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not create admin user: $_" -ForegroundColor Yellow
    }
}

# Case Manager User
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
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users" `
        -Method Post `
        -Headers $headers `
        -Body $caseManagerUserBody
    Write-Host "✓ Case manager user created" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✓ Case manager user already exists" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not create case manager user: $_" -ForegroundColor Yellow
    }
}

# Assign Roles to Users
Write-Host ""
Write-Host "7. Assigning roles to users..." -ForegroundColor Yellow

# Get all roles
try {
    $allRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" `
        -Method Get `
        -Headers $headers
}
catch {
    Write-Host "❌ Error getting roles: $_" -ForegroundColor Red
    exit 1
}

# Get admin user and assign admin role
try {
    $adminUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=admin.test" `
        -Method Get `
        -Headers $headers
    
    if ($adminUsers.Count -gt 0) {
        $adminUserId = $adminUsers[0].id
        $adminRole = $allRoles | Where-Object { $_.name -eq "admin" }
        
        if ($adminRole) {
            $adminRoleMapping = @(
                @{
                    id = $adminRole.id
                    name = $adminRole.name
                }
            ) | ConvertTo-Json

            Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" `
                -Method Post `
                -Headers $headers `
                -Body $adminRoleMapping
            Write-Host "✓ Assigned admin role to admin.test" -ForegroundColor Green
        }
    }
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✓ Admin role already assigned" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not assign admin role" -ForegroundColor Yellow
    }
}

# Get case manager user and assign case-manager role
try {
    $caseManagerUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=case.manager" `
        -Method Get `
        -Headers $headers
    
    if ($caseManagerUsers.Count -gt 0) {
        $caseManagerUserId = $caseManagerUsers[0].id
        $caseManagerRole = $allRoles | Where-Object { $_.name -eq "case-manager" }
        
        if ($caseManagerRole) {
            $caseManagerRoleMapping = @(
                @{
                    id = $caseManagerRole.id
                    name = $caseManagerRole.name
                }
            ) | ConvertTo-Json

            Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" `
                -Method Post `
                -Headers $headers `
                -Body $caseManagerRoleMapping
            Write-Host "✓ Assigned case-manager role to case.manager" -ForegroundColor Green
        }
    }
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "✓ Case-manager role already assigned" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not assign case-manager role" -ForegroundColor Yellow
    }
}

# Test the setup
Write-Host ""
Write-Host "8. Testing setup..." -ForegroundColor Yellow
try {
    $testResponse = Invoke-RestMethod -Uri "http://localhost:8081/realms/haven/.well-known/openid-configuration" `
        -Method Get
    Write-Host "✓ Haven realm is accessible" -ForegroundColor Green
    Write-Host "✓ OIDC endpoint: $($testResponse.issuer)" -ForegroundColor Green
}
catch {
    Write-Host "⚠️ Could not access haven realm configuration" -ForegroundColor Yellow
}

# Display final results
Write-Host ""
Write-Host "🎉 Keycloak Setup Complete!" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""
Write-Host "ADMIN USER:" -ForegroundColor Yellow
Write-Host "Username: admin.test" -ForegroundColor White
Write-Host "Password: admin123" -ForegroundColor White
Write-Host "Role: Administrator" -ForegroundColor White
Write-Host ""
Write-Host "CASE MANAGER USER:" -ForegroundColor Yellow
Write-Host "Username: case.manager" -ForegroundColor White
Write-Host "Password: manager123" -ForegroundColor White
Write-Host "Role: Case Manager" -ForegroundColor White
Write-Host ""
Write-Host "KEYCLOAK ADMIN CONSOLE:" -ForegroundColor Yellow
Write-Host "URL: http://localhost:8081/admin" -ForegroundColor White
Write-Host "Username: admin" -ForegroundColor White
Write-Host "Password: admin" -ForegroundColor White
Write-Host "Realm: haven" -ForegroundColor White
Write-Host ""
Write-Host "HAVEN APPLICATION:" -ForegroundColor Yellow
Write-Host "URL: http://localhost:3000/login" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Start your Haven frontend: npm run dev" -ForegroundColor White
Write-Host "2. Navigate to http://localhost:3000/login" -ForegroundColor White
Write-Host "3. Login with either test user above" -ForegroundColor White
Write-Host "4. See different dashboard views based on roles!" -ForegroundColor White
Write-Host ""