# Validate Role Propagation End-to-End
# Purpose: Test that roles propagate correctly from Keycloak to tokens to application
# Date: 2025-10-07

Write-Host "🔍 Validating Role Propagation" -ForegroundColor Green
Write-Host "=" * 70 -ForegroundColor Cyan
Write-Host ""

# Configuration
$KEYCLOAK_URL = "http://localhost:8081"
$REALM = "haven"
$CLIENT_ID = "haven-frontend"

# Test users to validate
$testUsers = @(
    @{ username = "ce.intake.test"; password = "intake123"; expectedRole = "ce-intake" },
    @{ username = "dv.advocate.test"; password = "advocate123"; expectedRole = "dv-advocate" },
    @{ username = "compliance.test"; password = "audit123"; expectedRole = "compliance-auditor" },
    @{ username = "exec.test"; password = "exec123"; expectedRole = "exec" }
)

$allPassed = $true
$testResults = @()

foreach ($user in $testUsers) {
    Write-Host "Testing user: $($user.username)" -ForegroundColor Yellow
    Write-Host "  Expected role: $($user.expectedRole)" -ForegroundColor Gray

    $testResult = @{
        username = $user.username
        expectedRole = $user.expectedRole
        tokenObtained = $false
        roleInToken = $false
        customClaimPresent = $false
        passed = $false
        details = @()
    }

    try {
        # Get access token
        $tokenBody = @{
            username = $user.username
            password = $user.password
            grant_type = "password"
            client_id = $CLIENT_ID
        }

        $tokenResponse = Invoke-RestMethod `
            -Uri "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" `
            -Method Post `
            -ContentType "application/x-www-form-urlencoded" `
            -Body $tokenBody

        $accessToken = $tokenResponse.access_token
        $testResult.tokenObtained = $true
        Write-Host "    ✓ Token obtained" -ForegroundColor Green

        # Decode JWT (simple base64 decode of payload)
        $tokenParts = $accessToken.Split('.')
        if ($tokenParts.Length -eq 3) {
            $payloadBase64 = $tokenParts[1]
            # Add padding if needed
            $padding = "=" * ((4 - ($payloadBase64.Length % 4)) % 4)
            $payloadBase64 = $payloadBase64 + $padding

            $payloadBytes = [Convert]::FromBase64String($payloadBase64)
            $payloadJson = [System.Text.Encoding]::UTF8.GetString($payloadBytes)
            $payload = $payloadJson | ConvertFrom-Json

            Write-Host "    Token payload decoded" -ForegroundColor Gray

            # Check realm_access.roles
            if ($payload.realm_access -and $payload.realm_access.roles) {
                $roles = $payload.realm_access.roles
                Write-Host "    Realm roles: $($roles -join ', ')" -ForegroundColor Gray

                if ($roles -contains $user.expectedRole) {
                    $testResult.roleInToken = $true
                    Write-Host "    ✓ Expected role found in realm_access.roles" -ForegroundColor Green
                } else {
                    Write-Host "    ✗ Expected role NOT found in realm_access.roles" -ForegroundColor Red
                    $testResult.details += "Missing role in realm_access.roles"
                }
            } else {
                Write-Host "    ✗ No realm_access.roles in token" -ForegroundColor Red
                $testResult.details += "No realm_access.roles claim"
            }

            # Check for custom haven_roles claim
            if ($payload.PSObject.Properties.Name -contains "haven_roles") {
                $havenRoles = $payload.haven_roles
                Write-Host "    Custom claim 'haven_roles': $($havenRoles -join ', ')" -ForegroundColor Gray
                $testResult.customClaimPresent = $true
                Write-Host "    ✓ Custom haven_roles claim present" -ForegroundColor Green
            } else {
                Write-Host "    ⚠️ Custom haven_roles claim not configured (optional)" -ForegroundColor Yellow
                $testResult.details += "haven_roles claim not configured"
            }

            # Check token expiration
            if ($payload.exp) {
                $expTime = [DateTimeOffset]::FromUnixTimeSeconds($payload.exp).LocalDateTime
                Write-Host "    Token expires: $expTime" -ForegroundColor Gray
            }

            # Check issuer
            if ($payload.iss) {
                Write-Host "    Issuer: $($payload.iss)" -ForegroundColor Gray
            }

            # Overall pass/fail
            if ($testResult.tokenObtained -and $testResult.roleInToken) {
                $testResult.passed = $true
                Write-Host "  ✓ PASSED" -ForegroundColor Green
            } else {
                $testResult.passed = $false
                Write-Host "  ✗ FAILED" -ForegroundColor Red
                $allPassed = $false
            }

        } else {
            Write-Host "    ✗ Invalid token format" -ForegroundColor Red
            $testResult.details += "Invalid JWT format"
            $allPassed = $false
        }

    }
    catch {
        Write-Host "    ✗ Error: $($_.Exception.Message)" -ForegroundColor Red
        $testResult.details += "Exception: $($_.Exception.Message)"
        $allPassed = $false
    }

    $testResults += $testResult
    Write-Host ""
}

# Summary
Write-Host "=" * 70 -ForegroundColor Cyan
Write-Host "Validation Summary" -ForegroundColor Green
Write-Host "=" * 70 -ForegroundColor Cyan
Write-Host ""

$passedCount = ($testResults | Where-Object { $_.passed }).Count
$totalCount = $testResults.Count

Write-Host "Total Tests: $totalCount" -ForegroundColor White
Write-Host "Passed: $passedCount" -ForegroundColor Green
Write-Host "Failed: $($totalCount - $passedCount)" -ForegroundColor Red
Write-Host ""

if ($allPassed) {
    Write-Host "🎉 All validations PASSED!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Role propagation is working correctly:" -ForegroundColor White
    Write-Host "  ✓ Users can authenticate with Keycloak" -ForegroundColor Green
    Write-Host "  ✓ Roles are present in JWT tokens" -ForegroundColor Green
    Write-Host "  ✓ Tokens contain expected role claims" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "  1. Test UI role-based access control" -ForegroundColor White
    Write-Host "  2. Test API endpoint authorization" -ForegroundColor White
    Write-Host "  3. Verify composite role (exec) includes child roles" -ForegroundColor White
    exit 0
} else {
    Write-Host "❌ Some validations FAILED" -ForegroundColor Red
    Write-Host ""
    Write-Host "Failed Tests:" -ForegroundColor Yellow
    foreach ($result in $testResults | Where-Object { -not $_.passed }) {
        Write-Host "  • $($result.username)" -ForegroundColor Red
        foreach ($detail in $result.details) {
            Write-Host "    - $detail" -ForegroundColor Gray
        }
    }
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Cyan
    Write-Host "  1. Verify Keycloak is running: docker ps" -ForegroundColor White
    Write-Host "  2. Check roles exist: .\scripts\setup-keycloak-roles-aligned.ps1" -ForegroundColor White
    Write-Host "  3. Check test users exist: .\scripts\create-test-users-aligned.ps1" -ForegroundColor White
    Write-Host "  4. Review Keycloak Admin Console: http://localhost:8081/admin" -ForegroundColor White
    exit 1
}
