# Check and fix user role assignments

Write-Host "Checking user role assignments..." -ForegroundColor Green

# Get admin token
$tokenBody = @{
    username = "admin"
    password = "admin"
    grant_type = "password"
    client_id = "admin-cli"
}

$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8081/realms/master/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body $tokenBody
$token = $tokenResponse.access_token

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Check admin user roles
Write-Host "Checking admin.test user roles..." -ForegroundColor Yellow
$adminUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=admin.test" -Method Get -Headers $headers
if ($adminUsers.Count -gt 0) {
    $adminUserId = $adminUsers[0].id
    $adminRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" -Method Get -Headers $headers
    Write-Host "Admin user roles:" -ForegroundColor White
    $adminRoles | ForEach-Object { Write-Host "  - $($_.name)" -ForegroundColor Cyan }
}

# Check case manager user roles  
Write-Host "Checking case.manager user roles..." -ForegroundColor Yellow
$caseManagerUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=case.manager" -Method Get -Headers $headers
if ($caseManagerUsers.Count -gt 0) {
    $caseManagerUserId = $caseManagerUsers[0].id
    $caseManagerRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" -Method Get -Headers $headers
    Write-Host "Case manager user roles:" -ForegroundColor White
    $caseManagerRoles | ForEach-Object { Write-Host "  - $($_.name)" -ForegroundColor Cyan }
}

# Get all available roles
Write-Host "Available roles in haven realm:" -ForegroundColor Yellow
$allRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" -Method Get -Headers $headers
$allRoles | ForEach-Object { Write-Host "  - $($_.name)" -ForegroundColor White }

Write-Host ""
Write-Host "Now fixing role assignments..." -ForegroundColor Green

# Remove all roles from case manager first
Write-Host "Removing roles from case manager..." -ForegroundColor Yellow
if ($caseManagerUsers.Count -gt 0 -and $caseManagerRoles.Count -gt 0) {
    $rolesToRemove = $caseManagerRoles | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" -Method Delete -Headers $headers -Body $rolesToRemove
        Write-Host "Removed existing roles from case manager" -ForegroundColor Green
    } catch {
        Write-Host "Could not remove existing roles" -ForegroundColor Yellow
    }
}

# Assign only case-manager role to case manager
$caseManagerRole = $allRoles | Where-Object { $_.name -eq "case-manager" }
if ($caseManagerRole -and $caseManagerUsers.Count -gt 0) {
    $roleMapping = @(@{
        id = $caseManagerRole.id
        name = $caseManagerRole.name
    }) | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" -Method Post -Headers $headers -Body $roleMapping
        Write-Host "Assigned ONLY case-manager role to case.manager user" -ForegroundColor Green
    } catch {
        Write-Host "Could not assign case-manager role" -ForegroundColor Red
    }
}

# Remove all roles from admin user first
Write-Host "Removing roles from admin user..." -ForegroundColor Yellow
if ($adminUsers.Count -gt 0 -and $adminRoles.Count -gt 0) {
    $rolesToRemove = $adminRoles | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" -Method Delete -Headers $headers -Body $rolesToRemove
        Write-Host "Removed existing roles from admin user" -ForegroundColor Green
    } catch {
        Write-Host "Could not remove existing roles" -ForegroundColor Yellow
    }
}

# Assign only admin role to admin user
$adminRole = $allRoles | Where-Object { $_.name -eq "admin" }
if ($adminRole -and $adminUsers.Count -gt 0) {
    $roleMapping = @(@{
        id = $adminRole.id
        name = $adminRole.name
    }) | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" -Method Post -Headers $headers -Body $roleMapping
        Write-Host "Assigned ONLY admin role to admin.test user" -ForegroundColor Green
    } catch {
        Write-Host "Could not assign admin role" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Verification - checking roles again:" -ForegroundColor Green

# Check admin user roles again
$adminRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" -Method Get -Headers $headers
Write-Host "Admin user (admin.test) now has roles:" -ForegroundColor Yellow
$adminRoles | ForEach-Object { Write-Host "  - $($_.name)" -ForegroundColor Cyan }

# Check case manager user roles again
$caseManagerRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" -Method Get -Headers $headers
Write-Host "Case manager user (case.manager) now has roles:" -ForegroundColor Yellow
$caseManagerRoles | ForEach-Object { Write-Host "  - $($_.name)" -ForegroundColor Cyan }

Write-Host ""
Write-Host "Role assignment fixed!" -ForegroundColor Green
Write-Host "admin.test should only see admin dashboard" -ForegroundColor White
Write-Host "case.manager should only see case manager dashboard" -ForegroundColor White