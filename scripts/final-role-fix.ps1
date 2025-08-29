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

$caseUser = (Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=case.manager" -Headers $headers)[0]
$caseRole = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/roles/case-manager" -Headers $headers

$roleData = @(@{
    id = $caseRole.id
    name = $caseRole.name
})

$body = $roleData | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$($caseUser.id)/role-mappings/realm" -Method Post -Headers $headers -Body $body

Write-Host "Case-manager role assigned successfully" -ForegroundColor Green

# Verify final state
Write-Host "Final role verification:" -ForegroundColor Yellow

$adminUser = (Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users?username=admin.test" -Headers $headers)[0]
$adminRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$($adminUser.id)/role-mappings/realm" -Headers $headers
Write-Host "admin.test has roles:" -ForegroundColor Cyan
$adminRoles | ForEach-Object { Write-Host "  - $($_.name)" }

$caseRoles = Invoke-RestMethod -Uri "http://localhost:8081/admin/realms/haven/users/$($caseUser.id)/role-mappings/realm" -Headers $headers
Write-Host "case.manager has roles:" -ForegroundColor Cyan
$caseRoles | ForEach-Object { Write-Host "  - $($_.name)" }