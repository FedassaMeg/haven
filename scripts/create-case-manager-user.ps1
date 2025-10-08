# Create Case Manager User for Testing Dashboard
# This script creates a test case manager user to compare dashboard views

Write-Host "Creating test case manager user for dashboard testing..." -ForegroundColor Green

# Get admin token
$tokenBody = @{
    username = "admin"
    password = "admin"
    grant_type = "password"
    client_id = "admin-cli"
}

try {
    Write-Host "Getting admin token..." -ForegroundColor Yellow
    $tokenResponse = Invoke-RestMethod -Uri "http://localhost:8081/realms/master/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody
    
    $token = $tokenResponse.access_token
    Write-Host "✓ Admin token obtained" -ForegroundColor Green
}
catch {
    Write-Host "❌ Failed to get admin token. Make sure Keycloak is running on http://localhost:8081" -ForegroundColor Red
    Write-Host "Run: docker compose -f docker-compose.dev.yaml up -d" -ForegroundColor Cyan
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Create the case manager user
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
    Write-Host "Creating case manager user..." -ForegroundColor Yellow
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users" `
        -Method Post `
        -Headers $headers `
        -Body $caseManagerUserBody
    Write-Host "✓ Case manager user created successfully!" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "⚠️ Case manager user already exists, continuing with role assignment..." -ForegroundColor Yellow
    } else {
        Write-Host "❌ Error creating case manager user: $_" -ForegroundColor Red
        exit 1
    }
}

# Get the user ID
try {
    Write-Host "Getting user ID..." -ForegroundColor Yellow
    $users = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=case.manager" `
        -Method Get `
        -Headers $headers

    if ($users.Count -eq 0) {
        Write-Host "❌ Could not find created user" -ForegroundColor Red
        exit 1
    }

    $userId = $users[0].id
    Write-Host "✓ User ID obtained: $userId" -ForegroundColor Green
}
catch {
    Write-Host "❌ Error getting user ID: $_" -ForegroundColor Red
    exit 1
}

# Get all roles
try {
    Write-Host "Getting available roles..." -ForegroundColor Yellow
    $roles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" `
        -Method Get `
        -Headers $headers
    
    Write-Host "✓ Found $($roles.Count) roles" -ForegroundColor Green
}
catch {
    Write-Host "❌ Error getting roles: $_" -ForegroundColor Red
    exit 1
}

# Assign case-manager role to user
$caseManagerRole = $roles | Where-Object { $_.name -eq "case-manager" }

if ($caseManagerRole) {
    $roleMapping = @(
        @{
            id = $caseManagerRole.id
            name = $caseManagerRole.name
        }
    ) | ConvertTo-Json

    try {
        Write-Host "Assigning case-manager role to user..." -ForegroundColor Yellow
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$userId/role-mappings/realm" `
            -Method Post `
            -Headers $headers `
            -Body $roleMapping
        Write-Host "✓ Assigned 'case-manager' role to test user" -ForegroundColor Green
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "✓ Case-manager role already assigned" -ForegroundColor Green
        } else {
            Write-Host "⚠️ Could not assign case-manager role: $_" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "⚠️ Case-manager role not found" -ForegroundColor Yellow
}

# Display login credentials
Write-Host ""
Write-Host "🎉 Test Case Manager User Created Successfully!" -ForegroundColor Green
Write-Host "=" * 50 -ForegroundColor Cyan
Write-Host "Username: case.manager" -ForegroundColor White
Write-Host "Password: manager123" -ForegroundColor White
Write-Host "Email: case.manager@haven.local" -ForegroundColor White
Write-Host "Role: Case Manager" -ForegroundColor White
Write-Host ""
Write-Host "Login at: http://localhost:3000/login" -ForegroundColor Cyan
Write-Host "=" * 50 -ForegroundColor Cyan
Write-Host ""
Write-Host "This user has case manager privileges and will see:" -ForegroundColor Yellow
Write-Host "• My Active Cases (assigned cases only)" -ForegroundColor White
Write-Host "• Today's Priorities with completion tracking" -ForegroundColor White
Write-Host "• Recent Updates for my cases" -ForegroundColor White
Write-Host "• Wellbeing Check with breathing exercises" -ForegroundColor White
Write-Host "• Quick Resources (crisis hotline, templates, peer support)" -ForegroundColor White
Write-Host ""