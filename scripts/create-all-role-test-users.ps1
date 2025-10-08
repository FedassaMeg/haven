# Create Test Users for All Roles
# Purpose: Create comprehensive test accounts for every role in the system
# Date: 2025-10-07

Write-Host "👥 Creating Test Users for All Roles" -ForegroundColor Green
Write-Host "=" * 80 -ForegroundColor Cyan

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
    Write-Host "❌ Authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure Keycloak is running on port 8081" -ForegroundColor Yellow
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Define ALL test users for each role
$testUsers = @(
    @{
        username = "admin.test"
        email = "admin@haven.test"
        firstName = "Admin"
        lastName = "User"
        password = "Admin123!"
        role = "admin"
        description = "System Administrator - Full access"
    },
    @{
        username = "supervisor.test"
        email = "supervisor@haven.test"
        firstName = "Sarah"
        lastName = "Supervisor"
        password = "Super123!"
        role = "supervisor"
        description = "Supervisor - Team oversight and case management"
    },
    @{
        username = "case.manager.test"
        email = "case.manager@haven.test"
        firstName = "Carlos"
        lastName = "Manager"
        password = "Case123!"
        role = "case-manager"
        description = "Case Manager - Client case management"
    },
    @{
        username = "intake.specialist.test"
        email = "intake@haven.test"
        firstName = "Isabel"
        lastName = "Intake"
        password = "Intake123!"
        role = "intake-specialist"
        description = "Intake Specialist - New client intake and assessment"
    },
    @{
        username = "ce.intake.test"
        email = "ce.intake@haven.test"
        firstName = "Connor"
        lastName = "Entry"
        password = "CE123!"
        role = "ce-intake"
        description = "Coordinated Entry Intake - CE assessments and referrals"
    },
    @{
        username = "dv.advocate.test"
        email = "dv.advocate@haven.test"
        firstName = "Diana"
        lastName = "Advocate"
        password = "DV123!"
        role = "dv-advocate"
        description = "DV Advocate - Domestic violence support services"
    },
    @{
        username = "compliance.test"
        email = "compliance@haven.test"
        firstName = "Chloe"
        lastName = "Auditor"
        password = "Comply123!"
        role = "compliance-auditor"
        description = "Compliance Auditor - Regulatory compliance and auditing"
    },
    @{
        username = "exec.test"
        email = "exec@haven.test"
        firstName = "Eva"
        lastName = "Director"
        password = "Exec123!"
        role = "exec"
        description = "Executive Director - High-level reporting and oversight"
    },
    @{
        username = "report.viewer.test"
        email = "reports@haven.test"
        firstName = "Ryan"
        lastName = "Viewer"
        password = "Report123!"
        role = "report-viewer"
        description = "Report Viewer - Read-only access to reports"
    },
    @{
        username = "external.partner.test"
        email = "external@haven.test"
        firstName = "Emma"
        lastName = "Partner"
        password = "Partner123!"
        role = "external-partner"
        description = "External Partner - Limited external access"
    },
    @{
        username = "counselor.test"
        email = "counselor@haven.test"
        firstName = "Claire"
        lastName = "Counselor"
        password = "Counsel123!"
        role = "counselor"
        description = "Counselor - Therapeutic services and counseling"
    },
    @{
        username = "advocate.test"
        email = "advocate@haven.test"
        firstName = "Alex"
        lastName = "Advocate"
        password = "Advocate123!"
        role = "advocate"
        description = "Advocate - Client advocacy and support"
    },
    @{
        username = "data.analyst.test"
        email = "data.analyst@haven.test"
        firstName = "David"
        lastName = "Analyst"
        password = "Data123!"
        role = "data-analyst"
        description = "Data Analyst - HMIS exports and data analysis"
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
    Write-Host "❌ Failed to fetch roles: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Create users
Write-Host ""
Write-Host "3. Creating test users..." -ForegroundColor Yellow
Write-Host ""

$createdCount = 0
$existingCount = 0
$errorCount = 0

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
        $createdCount++

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
                Write-Host "    ✓ Assigned role: $($userData.role)" -ForegroundColor Cyan
            } else {
                Write-Host "    ⚠️ Role not found: $($userData.role)" -ForegroundColor Yellow
            }
        }
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "  ⟳ User already exists: $($userData.username)" -ForegroundColor Yellow
            $existingCount++
        } else {
            Write-Host "  ❌ Error creating user: $($userData.username)" -ForegroundColor Red
            Write-Host "     $($_.Exception.Message)" -ForegroundColor Gray
            $errorCount++
        }
    }
}

# Summary
Write-Host ""
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "📊 Summary" -ForegroundColor Green
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "  Created: $createdCount users" -ForegroundColor Green
Write-Host "  Already existed: $existingCount users" -ForegroundColor Yellow
Write-Host "  Errors: $errorCount users" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host ""

# Output credentials
Write-Host "🔐 TEST USER CREDENTIALS" -ForegroundColor Green
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host ""

foreach ($user in $testUsers) {
    Write-Host "  Role: $($user.role.ToUpper())" -ForegroundColor Cyan
    Write-Host "  ├─ Username: $($user.username)" -ForegroundColor White
    Write-Host "  ├─ Password: $($user.password)" -ForegroundColor White
    Write-Host "  ├─ Email: $($user.email)" -ForegroundColor Gray
    Write-Host "  └─ Description: $($user.description)" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "✅ Setup Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Start the frontend: cd frontend/apps/cm-portal && npm run dev" -ForegroundColor White
Write-Host "  2. Navigate to: http://localhost:3000/login" -ForegroundColor White
Write-Host "  3. Login with any test user credentials above" -ForegroundColor White
Write-Host "  4. Verify role-based permissions work correctly" -ForegroundColor White
Write-Host ""
