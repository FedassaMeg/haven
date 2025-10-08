# Simple role assignment fix

# Get token
$response = Invoke-RestMethod -Uri "http://localhost:8081/realms/master/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body @{
    username = "admin"
    password = "admin" 
    grant_type = "password"
    client_id = "admin-cli"
}

$headers = @{
    "Authorization" = "Bearer $($response.access_token)"
    "Content-Type" = "application/json"
}

# Get admin user
$adminUser = (Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=admin.test" -Headers $headers)[0]
Write-Host "Admin user ID: $($adminUser.id)"

# Get case manager user
$caseUser = (Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=case.manager" -Headers $headers)[0]
Write-Host "Case manager user ID: $($caseUser.id)"

# Get admin role
$adminRole = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles/admin" -Headers $headers
Write-Host "Admin role: $($adminRole.name) (ID: $($adminRole.id))"

# Get case-manager role
$caseRole = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles/case-manager" -Headers $headers
Write-Host "Case-manager role: $($caseRole.name) (ID: $($caseRole.id))"

# Assign admin role
Write-Host "Assigning admin role..."
$adminRoleBody = "[$(@{id=$adminRole.id; name=$adminRole.name} | ConvertTo-Json)]"
try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$($adminUser.id)/role-mappings/realm" -Method Post -Headers $headers -Body $adminRoleBody
    Write-Host "✓ Admin role assigned"
} catch {
    Write-Host "Admin role assignment failed: $($_.Exception.Message)"
}

# Assign case-manager role  
Write-Host "Assigning case-manager role..."
$caseRoleBody = "[$(@{id=$caseRole.id; name=$caseRole.name} | ConvertTo-Json)]"
try {
    Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$($caseUser.id)/role-mappings/realm" -Method Post -Headers $headers -Body $caseRoleBody
    Write-Host "✓ Case-manager role assigned"
} catch {
    Write-Host "Case-manager role assignment failed: $($_.Exception.Message)"
}

# Verify assignments
Write-Host "`nVerifying roles:"
$adminRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$($adminUser.id)/role-mappings/realm" -Headers $headers
Write-Host "admin.test roles:"
$adminRoles | ForEach-Object { Write-Host "  - $($_.name)" }

$caseRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$($caseUser.id)/role-mappings/realm" -Headers $headers
Write-Host "case.manager roles:"
$caseRoles | ForEach-Object { Write-Host "  - $($_.name)" }