# Swagger/OpenAPI Documentation

This document describes how to access and use the Swagger UI documentation for the microservices in this project.

## Services with Swagger Documentation

The following services have been configured with Swagger/OpenAPI documentation:

1. **Analytics Service**
2. **User Service** (Auth Service)
3. **Auction Service**

## Accessing Swagger UI

Once the services are running, you can access the Swagger UI at the following URLs:

### Analytics Service
- **Swagger UI**: `http://localhost:<ANALYTICS_PORT>/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:<ANALYTICS_PORT>/v3/api-docs`
- **Description**: API for reporting, analytics, and data insights

### User Service (Auth Service)
- **Swagger UI**: `http://localhost:<USER_SERVICE_PORT>/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:<USER_SERVICE_PORT>/v3/api-docs`
- **Description**: API for user authentication, authorization, and user management

### Auction Service
- **Swagger UI**: `http://localhost:<AUCTION_PORT>/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:<AUCTION_PORT>/v3/api-docs`
- **Description**: API for items, bidding, categories, and auction-related functionalities

> **Note**: Replace `<SERVICE_PORT>` with the actual port number configured in each service's `application.properties` file.

## Using Swagger UI

### 1. Exploring Endpoints
- Navigate to the Swagger UI URL for the service you want to explore
- You'll see a list of all available API endpoints organized by controller
- Click on any endpoint to see details including:
  - Request parameters
  - Request body schema
  - Response codes and schemas
  - Example values

### 2. Testing Endpoints

#### For Public Endpoints:
1. Click on the endpoint you want to test
2. Click "Try it out"
3. Fill in the required parameters
4. Click "Execute"
5. View the response

#### For Protected Endpoints (JWT Authentication):
1. First, obtain a JWT token by calling the authentication endpoint (usually in User Service)
2. Click the "Authorize" button at the top of the Swagger UI
3. Enter your JWT token in the format: `Bearer <your-token>`
4. Click "Authorize"
5. Now you can test protected endpoints

### 3. Generating Client Code
Swagger UI allows you to download the OpenAPI specification and generate client code in various languages:
1. Access the OpenAPI JSON endpoint (e.g., `/v3/api-docs`)
2. Use tools like [Swagger Codegen](https://swagger.io/tools/swagger-codegen/) or [OpenAPI Generator](https://openapi-generator.tech/) to generate client code

## Configuration

### SpringDoc OpenAPI Dependencies
All three services include the following Maven dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Custom Configuration
Each service has an `OpenApiConfig` class that customizes the API documentation:
- **Analytics Service**: `com.ebaazee.analytics_service.config.OpenApiConfig`
- **User Service**: `com.service.auth_svc.config.OpenApiConfig`
- **Auction Service**: `com.core.auction_system.config.OpenApiConfig`

## Customizing API Documentation

### Adding Descriptions to Endpoints
Use OpenAPI annotations in your controller classes:

```java
@Tag(name = "Users", description = "User management APIs")
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves a user by their unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@Parameter(description = "User ID") @PathVariable Long id) {
        // implementation
    }
}
```

### Adding Model Descriptions
Use OpenAPI annotations in your DTO/Entity classes:

```java
@Schema(description = "User entity representing a platform user")
public class User {
    
    @Schema(description = "Unique identifier of the user", example = "1")
    private Long id;
    
    @Schema(description = "Email address of the user", example = "user@example.com")
    private String email;
}
```

## Disabling Swagger in Production

To disable Swagger UI in production, add the following to your `application.properties`:

```properties
# Disable Swagger in production
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

Or use Spring profiles:

```properties
# application-prod.properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

## Troubleshooting

### Swagger UI Not Loading
1. Ensure the service is running
2. Check that the correct port is being used
3. Verify that SpringDoc dependency is properly added to `pom.xml`
4. Check application logs for any startup errors

### Authentication Issues
1. Make sure you're using the correct token format: `Bearer <token>`
2. Verify that the token is valid and not expired
3. Check that the security configuration allows Swagger endpoints

### Missing Endpoints
1. Ensure controllers are annotated with `@RestController`
2. Check that the controller is in a package scanned by Spring Boot
3. Verify that the endpoints are not excluded in security configuration

## Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)

## Support

For issues or questions related to API documentation, please contact the respective service team:
- Analytics Service: analytics@ebaazee.com
- User Service: auth@service.com
- Auction Service: auction@core.com
