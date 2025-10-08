# Keycloak Role Alignment Script
# Purpose: Define and configure standardized roles for Haven
# Date: 2025-10-07

Write-Host "🔐 Keycloak Role Alignment for Haven" -ForegroundColor Green
Write-Host "=" * 70 -ForegroundColor Cyan
Write-Host ""

# Configuration
$KEYCLOAK_URL = "http://localhost:8081"
$REALM = "haven"
$ADMIN_USER = "admin"
$ADMIN_PASS = "admin"

# Get admin token
Write-Host "1. Authenticating with Keycloak..." -ForegroundColor Yellow
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
    Write-Host "✓ Authentication successful" -ForegroundColor Green
}
catch {
    Write-Host "❌ Failed to authenticate. Ensure Keycloak is running:" -ForegroundColor Red
    Write-Host "   docker compose -f docker-compose.dev.yaml up -d" -ForegroundColor Cyan
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Verify realm exists
Write-Host ""
Write-Host "2. Verifying realm '$REALM'..." -ForegroundColor Yellow
try {
    $realm = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM" `
        -Method Get `
        -Headers $headers
    Write-Host "✓ Realm '$REALM' found" -ForegroundColor Green
}
catch {
    Write-Host "❌ Realm '$REALM' not found. Run setup-keycloak-complete.ps1 first." -ForegroundColor Red
    exit 1
}

# Define standardized roles
Write-Host ""
Write-Host "3. Creating/updating standardized realm roles..." -ForegroundColor Yellow

$roles = @(
    @{
        name = "admin"
        description = "System Administrator - Full access to all system functions and data"
    },
    @{
        name = "supervisor"
        description = "Program Supervisor - Team oversight, case review, and reporting"
    },
    @{
        name = "case-manager"
        description = "Case Manager - Primary case management and client services"
    },
    @{
        name = "intake-specialist"
        description = "Intake Specialist - Client intake and initial assessment"
    },
    @{
        name = "ce-intake"
        description = "Community Engagement & Intake - Outreach, intake coordination, and community partnerships"
    },
    @{
        name = "dv-advocate"
        description = "Domestic Violence Advocate - Specialized DV case support and crisis intervention"
    },
    @{
        name = "compliance-auditor"
        description = "Compliance & Auditor - System audit, compliance review, data quality (read-only for cases)"
    },
    @{
        name = "exec"
        description = "Executive - Strategic oversight with composite permissions (supervisor + compliance + reporting)"
    },
    @{
        name = "report-viewer"
        description = "Report Viewer - Read-only access to reports and aggregated data"
    },
    @{
        name = "external-partner"
        description = "External Partner - Limited access for external collaborators"
    }
)

foreach ($roleData in $roles) {
    try {
        $roleBody = @{
            name = $roleData.name
            description = $roleData.description
        } | ConvertTo-Json

        Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles" `
            -Method Post `
            -Headers $headers `
            -Body $roleBody
        Write-Host "  ✓ Created role: $($roleData.name)" -ForegroundColor Green
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "  ⟳ Role exists: $($roleData.name) (updating description...)" -ForegroundColor Yellow

            # Update existing role description
            try {
                $existingRole = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles/$($roleData.name)" `
                    -Method Get `
                    -Headers $headers

                $existingRole.description = $roleData.description

                Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles/$($roleData.name)" `
                    -Method Put `
                    -Headers $headers `
                    -Body ($existingRole | ConvertTo-Json -Depth 10)
                Write-Host "  ✓ Updated: $($roleData.name)" -ForegroundColor Green
            }
            catch {
                Write-Host "  ⚠️ Could not update: $($roleData.name)" -ForegroundColor Yellow
            }
        } else {
            Write-Host "  ⚠️ Error creating role $($roleData.name): $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

# Create composite role: exec
Write-Host ""
Write-Host "4. Configuring composite role: exec..." -ForegroundColor Yellow

try {
    # Get role objects for composite members
    $supervisorRole = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles/supervisor" `
        -Method Get -Headers $headers

    $complianceRole = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles/compliance-auditor" `
        -Method Get -Headers $headers

    $reportViewerRole = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles/report-viewer" `
        -Method Get -Headers $headers

    # Get exec role
    $execRole = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles/exec" `
        -Method Get -Headers $headers

    # Add composite roles to exec
    $compositeRoles = @($supervisorRole, $complianceRole, $reportViewerRole) | ConvertTo-Json

    Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles/exec/composites" `
        -Method Post `
        -Headers $headers `
        -Body $compositeRoles

    Write-Host "  ✓ Composite role 'exec' configured with: supervisor, compliance-auditor, report-viewer" -ForegroundColor Green
}
catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "  ✓ Composite roles already configured for 'exec'" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️ Could not configure composite roles: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Create client scopes
Write-Host ""
Write-Host "5. Creating client scopes..." -ForegroundColor Yellow

$clientScopes = @(
    @{
        name = "portal-api"
        description = "Main Haven Portal API Access"
        protocol = "openid-connect"
        attributes = @{
            "include.in.token.scope" = "true"
            "display.on.consent.screen" = "true"
        }
    },
    @{
        name = "export-service"
        description = "HMIS Export and Reporting Service Access"
        protocol = "openid-connect"
        attributes = @{
            "include.in.token.scope" = "true"
            "display.on.consent.screen" = "true"
        }
    },
    @{
        name = "admin-console"
        description = "Administrative Console Access"
        protocol = "openid-connect"
        attributes = @{
            "include.in.token.scope" = "true"
            "display.on.consent.screen" = "true"
        }
    }
)

foreach ($scope in $clientScopes) {
    try {
        $scopeBody = $scope | ConvertTo-Json -Depth 3

        Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/client-scopes" `
            -Method Post `
            -Headers $headers `
            -Body $scopeBody
        Write-Host "  ✓ Created client scope: $($scope.name)" -ForegroundColor Green
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "  ⟳ Client scope exists: $($scope.name)" -ForegroundColor Yellow
        } else {
            Write-Host "  ⚠️ Error creating scope $($scope.name): $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

# Configure protocol mappers for custom claims
Write-Host ""
Write-Host "6. Configuring protocol mappers for custom claims..." -ForegroundColor Yellow

# Get portal-api scope ID
try {
    $allScopes = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/client-scopes" `
        -Method Get -Headers $headers

    $portalApiScope = $allScopes | Where-Object { $_.name -eq "portal-api" } | Select-Object -First 1

    if ($portalApiScope) {
        # Create custom haven_roles claim mapper
        $rolesMapper = @{
            name = "haven_roles"
            protocol = "openid-connect"
            protocolMapper = "oidc-usermodel-realm-role-mapper"
            consentRequired = $false
            config = @{
                "claim.name" = "haven_roles"
                "jsonType.label" = "String"
                "multivalued" = "true"
                "userinfo.token.claim" = "true"
                "id.token.claim" = "true"
                "access.token.claim" = "true"
            }
        } | ConvertTo-Json -Depth 4

        try {
            Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/client-scopes/$($portalApiScope.id)/protocol-mappers/models" `
                -Method Post `
                -Headers $headers `
                -Body $rolesMapper
            Write-Host "  ✓ Created mapper: haven_roles" -ForegroundColor Green
        }
        catch {
            if ($_.Exception.Response.StatusCode -eq 409) {
                Write-Host "  ⟳ Mapper exists: haven_roles" -ForegroundColor Yellow
            }
        }

        # Create username mapper
        $usernameMapper = @{
            name = "username"
            protocol = "openid-connect"
            protocolMapper = "oidc-usermodel-property-mapper"
            consentRequired = $false
            config = @{
                "user.attribute" = "username"
                "claim.name" = "preferred_username"
                "jsonType.label" = "String"
                "userinfo.token.claim" = "true"
                "id.token.claim" = "true"
                "access.token.claim" = "true"
            }
        } | ConvertTo-Json -Depth 4

        try {
            Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/client-scopes/$($portalApiScope.id)/protocol-mappers/models" `
                -Method Post `
                -Headers $headers `
                -Body $usernameMapper
            Write-Host "  ✓ Created mapper: username" -ForegroundColor Green
        }
        catch {
            if ($_.Exception.Response.StatusCode -eq 409) {
                Write-Host "  ⟳ Mapper exists: username" -ForegroundColor Yellow
            }
        }
    }
}
catch {
    Write-Host "  ⚠️ Could not configure protocol mappers: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Assign client scopes to clients
Write-Host ""
Write-Host "7. Assigning client scopes to clients..." -ForegroundColor Yellow

try {
    # Get clients
    $allClients = Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/clients" `
        -Method Get -Headers $headers

    $frontendClient = $allClients | Where-Object { $_.clientId -eq "haven-frontend" } | Select-Object -First 1
    $backendClient = $allClients | Where-Object { $_.clientId -eq "haven-backend" } | Select-Object -First 1

    # Get scopes
    $portalApiScope = $allScopes | Where-Object { $_.name -eq "portal-api" } | Select-Object -First 1
    $exportScope = $allScopes | Where-Object { $_.name -eq "export-service" } | Select-Object -First 1
    $adminScope = $allScopes | Where-Object { $_.name -eq "admin-console" } | Select-Object -First 1

    # Assign scopes to frontend client
    if ($frontendClient -and $portalApiScope) {
        try {
            Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/clients/$($frontendClient.id)/default-client-scopes/$($portalApiScope.id)" `
                -Method Put -Headers $headers
            Write-Host "  ✓ Assigned portal-api to haven-frontend" -ForegroundColor Green
        } catch {
            if ($_.Exception.Response.StatusCode -eq 409 -or $_.Exception.Response.StatusCode -eq 204) {
                Write-Host "  ⟳ portal-api already assigned to haven-frontend" -ForegroundColor Yellow
            }
        }
    }

    # Assign scopes to backend client
    if ($backendClient -and $exportScope) {
        try {
            Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/clients/$($backendClient.id)/default-client-scopes/$($exportScope.id)" `
                -Method Put -Headers $headers
            Write-Host "  ✓ Assigned export-service to haven-backend" -ForegroundColor Green
        } catch {
            if ($_.Exception.Response.StatusCode -eq 409 -or $_.Exception.Response.StatusCode -eq 204) {
                Write-Host "  ⟳ export-service already assigned to haven-backend" -ForegroundColor Yellow
            }
        }
    }

    if ($backendClient -and $adminScope) {
        try {
            Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/$REALM/clients/$($backendClient.id)/default-client-scopes/$($adminScope.id)" `
                -Method Put -Headers $headers
            Write-Host "  ✓ Assigned admin-console to haven-backend" -ForegroundColor Green
        } catch {
            if ($_.Exception.Response.StatusCode -eq 409 -or $_.Exception.Response.StatusCode -eq 204) {
                Write-Host "  ⟳ admin-console already assigned to haven-backend" -ForegroundColor Yellow
            }
        }
    }
}
catch {
    Write-Host "  ⚠️ Could not assign client scopes: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "🎉 Keycloak Role Alignment Complete!" -ForegroundColor Green
Write-Host "=" * 70 -ForegroundColor Cyan
Write-Host ""
Write-Host "Configured Roles:" -ForegroundColor Yellow
Write-Host "  • admin, supervisor, case-manager, intake-specialist" -ForegroundColor White
Write-Host "  • ce-intake (NEW), dv-advocate (NEW)" -ForegroundColor Cyan
Write-Host "  • compliance-auditor (NEW), exec (NEW, composite)" -ForegroundColor Cyan
Write-Host "  • report-viewer, external-partner" -ForegroundColor White
Write-Host ""
Write-Host "Client Scopes:" -ForegroundColor Yellow
Write-Host "  • portal-api (mapped to haven_roles claim)" -ForegroundColor White
Write-Host "  • export-service" -ForegroundColor White
Write-Host "  • admin-console" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Export realm configuration: .\scripts\export-keycloak-realm.ps1" -ForegroundColor White
Write-Host "  2. Run database migration to add new roles to RBAC tables" -ForegroundColor White
Write-Host "  3. Create test users with new roles" -ForegroundColor White
Write-Host "  4. Validate token claims contain haven_roles" -ForegroundColor White
Write-Host ""
