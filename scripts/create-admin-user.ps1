# Create Admin User for Testing Dashboard
# This script creates a test administrator user with full admin privileges

Write-Host "Creating test administrator user for dashboard testing..." -ForegroundColor Green

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

# Create the admin user
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
    Write-Host "Creating admin user..." -ForegroundColor Yellow
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users" `
        -Method Post `
        -Headers $headers `
        -Body $adminUserBody
    Write-Host "✓ Admin user created successfully!" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "⚠️ Admin user already exists, continuing with role assignment..." -ForegroundColor Yellow
    } else {
        Write-Host "❌ Error creating admin user: $_" -ForegroundColor Red
        exit 1
    }
}

# Get the user ID
try {
    Write-Host "Getting user ID..." -ForegroundColor Yellow
    $users = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=admin.test" `
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

# Create roles if they don't exist
$rolesToCreate = @(
    @{ name = "admin"; description = "System Administrator with full access" },
    @{ name = "case-manager"; description = "Case Manager with case management access" },
    @{ name = "supervisor"; description = "Supervisor with team oversight access" },
    @{ name = "social-worker"; description = "Social Worker with client access" }
)

Write-Host "Creating/verifying roles..." -ForegroundColor Yellow
foreach ($roleData in $rolesToCreate) {
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

# Assign admin role to user
$adminRole = $roles | Where-Object { $_.name -eq "admin" }

if ($adminRole) {
    $roleMapping = @(
        @{
            id = $adminRole.id
            name = $adminRole.name
        }
    ) | ConvertTo-Json

    try {
        Write-Host "Assigning admin role to user..." -ForegroundColor Yellow
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$userId/role-mappings/realm" `
            -Method Post `
            -Headers $headers `
            -Body $roleMapping
        Write-Host "✓ Assigned 'admin' role to test user" -ForegroundColor Green
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "✓ Admin role already assigned" -ForegroundColor Green
        } else {
            Write-Host "⚠️ Could not assign admin role: $_" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "⚠️ Admin role not found" -ForegroundColor Yellow
}

# Display login credentials
Write-Host ""
Write-Host "🎉 Test Administrator User Created Successfully!" -ForegroundColor Green
Write-Host "=" * 50 -ForegroundColor Cyan
Write-Host "Username: admin.test" -ForegroundColor White
Write-Host "Password: admin123" -ForegroundColor White
Write-Host "Email: admin.test@haven.local" -ForegroundColor White
Write-Host "Role: Administrator" -ForegroundColor White
Write-Host ""
Write-Host "Login at: http://localhost:3000/login" -ForegroundColor Cyan
Write-Host "=" * 50 -ForegroundColor Cyan
Write-Host ""
Write-Host "This user has full admin privileges and will see:" -ForegroundColor Yellow
Write-Host "• System Analytics dashboard" -ForegroundColor White
Write-Host "• All cases overview" -ForegroundColor White
Write-Host "• Team management widgets" -ForegroundColor White
Write-Host "• System status monitoring" -ForegroundColor White
Write-Host "• Admin resources and settings" -ForegroundColor White
Write-Host ""