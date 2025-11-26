# Quick Start: Using Swagger UI

## 🎯 Service Access Points

Access each service's Swagger UI directly:

| Service | Swagger UI URL |
|---------|---------------|
| � **User Service** | http://localhost:8081/swagger-ui.html |
| 🎯 **Auction** | http://localhost:8082/swagger-ui.html |
| 📊 **Analytics** | http://localhost:8085/swagger-ui.html |

## Step-by-Step Guide

### 1️⃣ Test Public Endpoints (No Auth Required)

1. Go to **User Service** Swagger UI: http://localhost:8081/swagger-ui.html
2. Find the **Auth Controller** section
3. Look for `POST /api/auth/v1/register` endpoint
4. Click on it to expand
5. Click **"Try it out"** button
6. Modify the example request body with your details:
   ```json
   {
     "email": "test@example.com",
     "password": "SecurePass123!",
     "name": "Test User"
   }
   ```
7. Click **"Execute"**
8. Check the response - you should get a 200/201 status

### 2️⃣ Get Your JWT Token

1. Still in **User Service** Swagger UI
2. Find `POST /api/auth/v1/login` endpoint
3. Click **"Try it out"**
4. Enter your credentials:
   ```json
   {
     "email": "test@example.com",
     "password": "SecurePass123!"
   }
   ```
5. Click **"Execute"**
6. Copy the `accessToken` from the response (without quotes)

### 3️⃣ Authorize in Swagger UI

1. At the top of the Swagger UI page, click the **🔓 Authorize** button
2. In the popup, enter: `Bearer <paste-your-token-here>`
   - Example: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. Click **"Authorize"**
4. Click **"Close"**
5. Now the lock icon should show as 🔒 (locked/authorized)

### 4️⃣ Test Protected Endpoints

**Example: Create an Auction (Auction Service)**

1. Go to **Auction Service** Swagger UI: http://localhost:8082/swagger-ui.html
2. Click the **🔓 Authorize** button and add your token (as in step 3)
3. Find the `POST /api/products/v1` endpoint
4. Click **"Try it out"**
5. Enter product details:
   ```json
   {
     "name": "Vintage Camera",
     "description": "Classic 35mm film camera",
     "startingPrice": 100.00,
     "startTime": "2025-11-27T10:00:00",
     "endTime": "2025-11-30T18:00:00"
   }
   ```
6. Click **"Execute"**
7. You should get a successful response with the created product

**Example: View Analytics (Analytics Service)**

1. Go to **Analytics Service** Swagger UI: http://localhost:8085/swagger-ui.html
2. Click the **🔓 Authorize** button and add your token
3. Explore available analytics endpoints
4. Try the GraphQL endpoint or any reporting endpoints

## Common Operations

### Testing Different User Roles

If the system has role-based access:
1. Create users with different roles
2. Login with each user to get their token
3. Test endpoints with different tokens to see permissions in action

### Viewing Request/Response Schemas

- Click on any endpoint to expand it
- Scroll down to see the **"Schemas"** section
- This shows the structure of request bodies and responses
- Click on schema names to see nested objects

### Downloading the OpenAPI Specification

1. In Swagger UI, look for the `/v3/api-docs` link (usually at the top)
2. Or directly visit:
   - Analytics: http://localhost:8085/v3/api-docs
   - User Service: http://localhost:8081/v3/api-docs
   - Auction: http://localhost:8082/v3/api-docs
3. Save the JSON to use with code generators or other tools

## Troubleshooting

### 🔴 "401 Unauthorized" Error
- **Problem**: Your token is missing or expired
- **Solution**: 
  1. Get a fresh token by logging in again
  2. Click Authorize and update your token
  3. Make sure you included "Bearer " prefix

### 🔴 Swagger UI Not Loading
- **Problem**: Service might not be running
- **Solution**:
  1. Check if the service is running: `docker compose ps`
  2. Check service logs: `docker compose logs <service-name>`
  3. Wait a minute - services might still be starting up

### 🔴 "403 Forbidden" Error
- **Problem**: You don't have permission for this endpoint
- **Solution**:
  1. Check if you're logged in with the right user role
  2. Verify the endpoint requires the role you have
  3. Contact an admin if you need different permissions

### 🔴 Endpoints Not Showing Up
- **Problem**: Controllers might not be properly scanned
- **Solution**:
  1. Rebuild the service: `docker compose up --build <service-name>`
  2. Check application logs for startup errors
  3. Verify the controller has `@RestController` annotation

## Tips & Tricks

### 💡 Save Time with Swagger
- Use the "Models" section at the bottom to understand data structures
- Copy example values and modify them for quick testing
- Use the "Curl" tab to see how to call the API from command line

### 💡 Testing Workflows
1. **Register** a user (User Service)
2. **Login** to get token (User Service)
3. **Create** an auction (Auction Service)
4. **Place** a bid (Auction Service)
5. **View** analytics (Analytics Service)

### 💡 Working with Multiple Services
- Keep multiple Swagger UI tabs open (one per service)
- Use the same JWT token across all services
- Remember that each service has different endpoints

## Example: Complete Auction Workflow

```
1. User Service → Register: POST /api/auth/v1/register
2. User Service → Login: POST /api/auth/v1/login (get token)
3. Authorize with token in all Swagger UIs
4. Auction Service → Create product: POST /api/products/v1
5. Auction Service → Place bid: POST /api/bids/v1
6. Analytics Service → View stats (GraphQL or REST endpoints)
```

## Need More Help?

- 📖 Detailed documentation: [SWAGGER_DOCUMENTATION.md](./SWAGGER_DOCUMENTATION.md)
- 📋 Technical details: [SWAGGER_INTEGRATION_SUMMARY.md](./SWAGGER_INTEGRATION_SUMMARY.md)
- 🏠 Project overview: [README.md](./README.md)

---

**Happy Testing! 🚀**
