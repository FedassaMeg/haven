# Clean fix for user roles

Write-Host "Fixing user roles properly..." -ForegroundColor Green

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

# Get users
$adminUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=admin.test" -Method Get -Headers $headers
$caseManagerUsers = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=case.manager" -Method Get -Headers $headers

if ($adminUsers.Count -eq 0 -or $caseManagerUsers.Count -eq 0) {
    Write-Host "Users not found!" -ForegroundColor Red
    exit 1
}

$adminUserId = $adminUsers[0].id
$caseManagerUserId = $caseManagerUsers[0].id

# Get available roles
$allRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles" -Method Get -Headers $headers

# Find the admin role (try different variations)
$adminRole = $allRoles | Where-Object { $_.name -eq "admin" }
if (!$adminRole) {
    $adminRole = $allRoles | Where-Object { $_.name -eq "ADMIN" }
}

# Find the case-manager role (try different variations)
$caseManagerRole = $allRoles | Where-Object { $_.name -eq "case-manager" }
if (!$caseManagerRole) {
    $caseManagerRole = $allRoles | Where-Object { $_.name -eq "case_manager" }
    if (!$caseManagerRole) {
        $caseManagerRole = $allRoles | Where-Object { $_.name -eq "CASE_MANAGER" }
    }
}

Write-Host "Found admin role: $($adminRole.name)" -ForegroundColor Yellow
Write-Host "Found case manager role: $($caseManagerRole.name)" -ForegroundColor Yellow

# Assign admin role to admin user
if ($adminRole) {
    $adminRoleMapping = @(@{
        id = $adminRole.id
        name = $adminRole.name
    }) | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" -Method Post -Headers $headers -Body $adminRoleMapping
        Write-Host "✓ Successfully assigned admin role to admin.test" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ Admin role may already be assigned or error occurred" -ForegroundColor Yellow
    }
}

# Assign case-manager role to case manager user  
if ($caseManagerRole) {
    $caseManagerRoleMapping = @(@{
        id = $caseManagerRole.id
        name = $caseManagerRole.name
    }) | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" -Method Post -Headers $headers -Body $caseManagerRoleMapping
        Write-Host "✓ Successfully assigned case-manager role to case.manager" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ Case-manager role may already be assigned or error occurred" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Final verification:" -ForegroundColor Green

# Check final role assignments
$adminFinalRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$adminUserId/role-mappings/realm" -Method Get -Headers $headers
Write-Host "admin.test final roles:" -ForegroundColor Yellow
$adminFinalRoles | ForEach-Object { 
    Write-Host "  - $($_.name)" -ForegroundColor $(if ($_.name -eq "admin" -or $_.name -eq "ADMIN") { "Green" } else { "Gray" })
}

$caseManagerFinalRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$caseManagerUserId/role-mappings/realm" -Method Get -Headers $headers  
Write-Host "case.manager final roles:" -ForegroundColor Yellow
$caseManagerFinalRoles | ForEach-Object { 
    Write-Host "  - $($_.name)" -ForegroundColor $(if ($_.name -eq "case-manager" -or $_.name -eq "case_manager" -or $_.name -eq "CASE_MANAGER") { "Green" } else { "Gray" })
}

Write-Host ""
Write-Host "Test the login now:" -ForegroundColor Cyan
Write-Host "Admin: admin.test / admin123" -ForegroundColor White
Write-Host "Case Manager: case.manager / manager123" -ForegroundColor White