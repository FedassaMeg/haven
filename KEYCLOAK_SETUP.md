# Keycloak Setup for Haven

This guide walks you through setting up Keycloak for the Haven application.

## Quick Start

1. **Start Services**:
   ```bash
   make docker-up
   # OR
   docker compose -f docker-compose.dev.yaml up -d
   ```

2. **Access Keycloak Admin Console**:
   - URL: http://localhost:8081/admin
   - Username: `admin`
   - Password: `admin`

## Realm Configuration

### 1. Create Haven Realm

1. In Keycloak Admin Console, hover over "master" in the top-left
2. Click "Create Realm"
3. Set Realm name: `haven`
4. Click "Create"

### 2. Create Client for Backend API

1. Go to **Clients** → **Create client**
2. Set:
   - **Client ID**: `haven-backend`
   - **Client type**: OpenID Connect
   - **Client authentication**: ON (for confidential client)
3. Click **Next**
4. Configure:
   - **Valid redirect URIs**: `http://localhost:8080/*`
   - **Web origins**: `http://localhost:8080`
5. Click **Save**
6. Go to **Credentials** tab and copy the **Client Secret** for later use

### 3. Create Client for Frontend

1. Go to **Clients** → **Create client**
2. Set:
   - **Client ID**: `haven-frontend` 
   - **Client type**: OpenID Connect
   - **Client authentication**: OFF (for public client)
3. Click **Next**
4. Configure:
   - **Valid redirect URIs**: `http://localhost:3000/*`
   - **Web origins**: `http://localhost:3000`
5. Click **Save**

### 4. Create Test Users

1. Go to **Users** → **Add user**
2. Create users with different roles:
   - **admin-user**: Full system access
   - **case-worker**: Case management access  
   - **counselor**: Client counseling access
   - **volunteer**: Limited volunteer access

### 5. Create Roles

1. Go to **Realm roles** → **Create role**
2. Create the following roles:
   - `ADMIN`: System administrator
   - `CASE_WORKER`: Case management
   - `COUNSELOR`: Client counseling
   - `VOLUNTEER`: Volunteer access
   - `VIEWER`: Read-only access

### 6. Assign Roles to Users

1. Go to **Users** → Select a user → **Role mapping**
2. Click **Assign role** and select appropriate roles

## Environment Variables

Update your `.env` file or application configuration:

```properties
KEYCLOAK_URL=http://localhost:8081
KEYCLOAK_REALM=haven
KEYCLOAK_CLIENT_ID=haven-backend
KEYCLOAK_CLIENT_SECRET=<copy-from-keycloak-credentials>
```

## Testing OAuth2 Flow

### 1. Get Access Token

```bash
curl -X POST http://localhost:8081/realms/haven/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=haven-backend" \
  -d "client_secret=<your-client-secret>" \
  -d "username=<test-username>" \
  -d "password=<test-password>"
```

### 2. Use Token with API

```bash
curl -H "Authorization: Bearer <access-token>" \
  http://localhost:8080/api/actuator/health
```

## Development URLs

- **Keycloak Admin**: http://localhost:8081/admin (admin/admin)
- **Haven Realm**: http://localhost:8081/realms/haven
- **OIDC Configuration**: http://localhost:8081/realms/haven/.well-known/openid-configuration
- **Backend API**: http://localhost:8080/api
- **API Docs**: http://localhost:8080/api/swagger-ui.html

## Troubleshooting

### Keycloak Not Starting
```bash
# Check container logs
docker logs keycloak

# Restart Keycloak
docker restart keycloak
```

### JWT Token Issues
- Verify issuer URI matches exactly
- Check client credentials
- Ensure realm name is correct
- Validate token expiration

### Connection Refused
- Ensure Keycloak is running on port 8081
- Check Docker container status
- Verify no firewall blocking connections