# Keycloak Realm Export Script
# Purpose: Export Haven realm configuration for version control and reproducibility
# Date: 2025-10-07

Write-Host "📦 Exporting Keycloak Realm Configuration" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan

# Configuration
$KEYCLOAK_URL = "http://localhost:8081"
$REALM = "haven"
$OUTPUT_DIR = "infrastructure/keycloak"
$OUTPUT_FILE = "$OUTPUT_DIR/haven-realm-v2.json"
$ADMIN_USER = "admin"
$ADMIN_PASS = "admin"

# Create output directory
New-Item -ItemType Directory -Force -Path $OUTPUT_DIR | Out-Null

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
    Write-Host "✓ Authenticated successfully" -ForegroundColor Green
}
catch {
    Write-Host "❌ Authentication failed. Is Keycloak running?" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Export full realm
Write-Host ""
Write-Host "2. Exporting realm configuration..." -ForegroundColor Yellow

try {
    $realmExport = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM" `
        -Method Get `
        -Headers $headers

    # Export with pretty formatting
    $realmJson = $realmExport | ConvertTo-Json -Depth 20

    # Save to file
    Set-Content -Path $OUTPUT_FILE -Value $realmJson -Encoding UTF8
    Write-Host "✓ Realm exported to: $OUTPUT_FILE" -ForegroundColor Green

    # Get file size
    $fileSize = (Get-Item $OUTPUT_FILE).Length
    Write-Host "  File size: $([math]::Round($fileSize/1KB, 2)) KB" -ForegroundColor Gray
}
catch {
    Write-Host "❌ Export failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Export roles separately for clarity
Write-Host ""
Write-Host "3. Exporting roles..." -ForegroundColor Yellow

try {
    $roles = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM/roles" `
        -Method Get `
        -Headers $headers

    $rolesJson = $roles | ConvertTo-Json -Depth 10
    $rolesFile = "$OUTPUT_DIR/haven-roles-v2.json"

    Set-Content -Path $rolesFile -Value $rolesJson -Encoding UTF8
    Write-Host "✓ Roles exported to: $rolesFile" -ForegroundColor Green
    Write-Host "  Total roles: $($roles.Count)" -ForegroundColor Gray
}
catch {
    Write-Host "⚠️ Could not export roles separately" -ForegroundColor Yellow
}

# Export client scopes
Write-Host ""
Write-Host "4. Exporting client scopes..." -ForegroundColor Yellow

try {
    $scopes = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM/client-scopes" `
        -Method Get `
        -Headers $headers

    $scopesJson = $scopes | ConvertTo-Json -Depth 10
    $scopesFile = "$OUTPUT_DIR/haven-client-scopes-v2.json"

    Set-Content -Path $scopesFile -Value $scopesJson -Encoding UTF8
    Write-Host "✓ Client scopes exported to: $scopesFile" -ForegroundColor Green
    Write-Host "  Total scopes: $($scopes.Count)" -ForegroundColor Gray
}
catch {
    Write-Host "⚠️ Could not export client scopes" -ForegroundColor Yellow
}

# Create metadata file
Write-Host ""
Write-Host "5. Creating metadata..." -ForegroundColor Yellow

$metadata = @"
# Haven Keycloak Realm Export
**Version:** 2.0.0
**Export Date:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
**Realm:** $REALM
**Keycloak Version:** 25.0.0

## Changes in v2.0.0
- Added new roles: ce-intake, dv-advocate, compliance-auditor, exec
- Created composite role: exec (includes supervisor + compliance-auditor + report-viewer)
- Added client scopes: portal-api, export-service, admin-console
- Configured protocol mappers for custom haven_roles claim
- Standardized role naming to kebab-case

## Roles
$(
    $roles | ForEach-Object {
        "- **$($_.name)**: $($_.description)"
    } | Out-String
)

## Client Scopes
$(
    $scopes | Where-Object { $_.name -in @('portal-api', 'export-service', 'admin-console') } | ForEach-Object {
        "- **$($_.name)**: $($_.description)"
    } | Out-String
)

## Import Instructions
``````powershell
# Option 1: Via Keycloak Admin Console
# 1. Login to http://localhost:8081/admin
# 2. Select "Add realm" → "Import" → Choose haven-realm-v2.json

# Option 2: Via Docker (for clean setup)
docker exec -it keycloak /opt/keycloak/bin/kc.sh import --file /tmp/haven-realm-v2.json
``````

## Rollback
To rollback to previous version:
``````powershell
# Re-import haven-realm-v1.json (if available)
``````
"@

$metadataFile = "$OUTPUT_DIR/RELEASE_NOTES_v2.md"
Set-Content -Path $metadataFile -Value $metadata -Encoding UTF8
Write-Host "✓ Metadata created: $metadataFile" -ForegroundColor Green

# Summary
Write-Host ""
Write-Host "🎉 Export Complete!" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""
Write-Host "Exported Files:" -ForegroundColor Yellow
Write-Host "  📄 $OUTPUT_FILE" -ForegroundColor White
Write-Host "  📄 $OUTPUT_DIR/haven-roles-v2.json" -ForegroundColor White
Write-Host "  📄 $OUTPUT_DIR/haven-client-scopes-v2.json" -ForegroundColor White
Write-Host "  📄 $OUTPUT_DIR/RELEASE_NOTES_v2.md" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Review exported JSON files" -ForegroundColor White
Write-Host "  2. Commit to version control" -ForegroundColor White
Write-Host "  3. Tag release: git tag keycloak-v2.0.0" -ForegroundColor White
Write-Host ""
