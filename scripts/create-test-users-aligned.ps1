# Create Test Users with New Roles
# Purpose: Create test users for each new role to validate end-to-end role propagation
# Date: 2025-10-07

Write-Host "👥 Creating Test Users for Role Validation" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan

# Configuration
$KEYCLOAK_URL = "http://localhost:8081"
$REALM = "haven"
$ADMIN_USER = "admin"
$ADMIN_PASS = "admin"

# Get admin token
Write-Host "1. Authenticating..." -ForegroundColor Yellow
$tokenBody = @{
    username = $ADMIN_USER
    password = $ADMIN_PASS
    grant_type = "password"
    client_id = "admin-cli"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody

    $token = $tokenResponse.access_token
    Write-Host "✓ Authenticated" -ForegroundColor Green
}
catch {
    Write-Host "❌ Authentication failed" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Define test users
$testUsers = @(
    @{
        username = "ce.intake.test"
        email = "ce.intake@haven.test"
        firstName = "Community"
        lastName = "Intake"
        password = "intake123"
        role = "ce-intake"
    },
    @{
        username = "dv.advocate.test"
        email = "dv.advocate@haven.test"
        firstName = "DV"
        lastName = "Advocate"
        password = "advocate123"
        role = "dv-advocate"
    },
    @{
        username = "compliance.test"
        email = "compliance@haven.test"
        firstName = "Compliance"
        lastName = "Auditor"
        password = "audit123"
        role = "compliance-auditor"
    },
    @{
        username = "exec.test"
        email = "exec@haven.test"
        firstName = "Executive"
        lastName = "Director"
        password = "exec123"
        role = "exec"
    }
)

# Get all roles for assignment
Write-Host ""
Write-Host "2. Fetching roles..." -ForegroundColor Yellow
try {
    $allRoles = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles" `
        -Method Get `
        -Headers $headers
    Write-Host "✓ Fetched $($allRoles.Count) roles" -ForegroundColor Green
}
catch {
    Write-Host "❌ Failed to fetch roles" -ForegroundColor Red
    exit 1
}

# Create users
Write-Host ""
Write-Host "3. Creating test users..." -ForegroundColor Yellow

foreach ($userData in $testUsers) {
    $userBody = @{
        username = $userData.username
        email = $userData.email
        firstName = $userData.firstName
        lastName = $userData.lastName
        enabled = $true
        emailVerified = $true
        credentials = @(
            @{
                type = "password"
                value = $userData.password
                temporary = $false
            }
        )
    } | ConvertTo-Json -Depth 3

    try {
        Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/users" `
            -Method Post `
            -Headers $headers `
            -Body $userBody
        Write-Host "  ✓ Created user: $($userData.username)" -ForegroundColor Green

        # Get user ID for role assignment
        Start-Sleep -Milliseconds 500
        $users = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/users?username=$($userData.username)" `
            -Method Get `
            -Headers $headers

        if ($users.Count -gt 0) {
            $userId = $users[0].id

            # Find and assign role
            $targetRole = $allRoles | Where-Object { $_.name -eq $userData.role } | Select-Object -First 1

            if ($targetRole) {
                $roleMapping = @(
                    @{
                        id = $targetRole.id
                        name = $targetRole.name
                    }
                ) | ConvertTo-Json

                Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/users/$userId/role-mappings/realm" `
                    -Method Post `
                    -Headers $headers `
                    -Body $roleMapping
                Write-Host "    ✓ Assigned role: $($userData.role)" -ForegroundColor Green
            } else {
                Write-Host "    ⚠️ Role not found: $($userData.role)" -ForegroundColor Yellow
            }
        }
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "  ⟳ User exists: $($userData.username)" -ForegroundColor Yellow
        } else {
            Write-Host "  ⚠️ Error creating user: $($userData.username) - $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

# Summary
Write-Host ""
Write-Host "🎉 Test Users Created!" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Credentials:" -ForegroundColor Yellow
Write-Host ""

foreach ($user in $testUsers) {
    Write-Host "  $($user.role.ToUpper())" -ForegroundColor Cyan
    Write-Host "    Username: $($user.username)" -ForegroundColor White
    Write-Host "    Password: $($user.password)" -ForegroundColor White
    Write-Host "    Email: $($user.email)" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Test login at http://localhost:3000/login" -ForegroundColor White
Write-Host "  2. Run validation script: .\scripts\validate-role-propagation.ps1" -ForegroundColor White
Write-Host "  3. Check token claims contain haven_roles" -ForegroundColor White
Write-Host ""
