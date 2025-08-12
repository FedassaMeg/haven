#!/bin/bash

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
until curl -s http://localhost:8081/realms/master > /dev/null 2>&1; do
  sleep 2
done

echo "Keycloak is ready. Configuring..."

# Get admin token
TOKEN=$(curl -s -X POST "http://localhost:8081/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "Failed to get admin token. Make sure Keycloak is running with admin/admin credentials."
  exit 1
fi

# Create realm if it doesn't exist
curl -s -X POST "http://localhost:8081/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "haven",
    "enabled": true,
    "sslRequired": "external",
    "registrationAllowed": true,
    "loginWithEmailAllowed": true,
    "duplicateEmailsAllowed": false,
    "resetPasswordAllowed": true,
    "editUsernameAllowed": false,
    "bruteForceProtected": true
  }' 2>/dev/null

# Create or update the frontend client
curl -s -X POST "http://localhost:8081/admin/realms/haven/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "haven-frontend",
    "enabled": true,
    "publicClient": true,
    "redirectUris": [
      "http://localhost:3000/*",
      "http://localhost:3001/*",
      "http://localhost:3002/*"
    ],
    "webOrigins": [
      "http://localhost:3000",
      "http://localhost:3001",
      "http://localhost:3002"
    ],
    "protocol": "openid-connect",
    "attributes": {
      "pkce.code.challenge.method": "S256"
    },
    "standardFlowEnabled": true,
    "implicitFlowEnabled": false,
    "directAccessGrantsEnabled": true,
    "serviceAccountsEnabled": false
  }' 2>/dev/null

# Check if client was created or already exists
CLIENT_ID=$(curl -s -X GET "http://localhost:8081/admin/realms/haven/clients?clientId=haven-frontend" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

if [ "$CLIENT_ID" != "null" ] && [ -n "$CLIENT_ID" ]; then
  # Update existing client
  curl -s -X PUT "http://localhost:8081/admin/realms/haven/clients/$CLIENT_ID" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "clientId": "haven-frontend",
      "enabled": true,
      "publicClient": true,
      "redirectUris": [
        "http://localhost:3000/*",
        "http://localhost:3001/*",
        "http://localhost:3002/*"
      ],
      "webOrigins": [
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:3002"
      ],
      "protocol": "openid-connect",
      "attributes": {
        "pkce.code.challenge.method": "S256"
      },
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "serviceAccountsEnabled": false
    }'
  echo "Client 'haven-frontend' updated successfully"
else
  echo "Client 'haven-frontend' created successfully"
fi

# Create roles
ROLES=("admin" "case_manager" "social_worker" "supervisor" "viewer")
for role in "${ROLES[@]}"; do
  curl -s -X POST "http://localhost:8081/admin/realms/haven/roles" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$role\"}" 2>/dev/null
done

echo "Keycloak configuration completed successfully!"
echo ""
echo "Access Keycloak Admin Console at: http://localhost:8081"
echo "Username: admin"
echo "Password: admin"