# Architecture and System Design

This document contains the detailed architecture, system flows, and infrastructure overview for the Ebaazee auction platform. This content was previously in the main README.

---

## System Architecture


### Microservices Overview

| Service | Description | Technology Stack | Port |
|---------|-------------|------------------|------|
| **User Service** | Authentication, authorization, JWT management, user profiles | Java 21, Spring Boot 3.5, PostgreSQL | 8081 |
| **API Gateway (Envoy)** | Central entrypoint, intelligent routing, circuit breaking | Envoy Proxy | 8080 |
| **Auction Service** | Auction & bid management, product catalog, scheduled tasks | Java 21, Spring Boot 3.5, PostgreSQL, RabbitMQ | 8082 |
| **Payment Service** | Wallet operations, payment gateway, fund locking/unlocking | Node.js 20, TypeScript, Express, PostgreSQL | 8086 |
| **Notification Service** | Email notifications via SMTP, event consumption | Go 1.22, RabbitMQ, Gmail SMTP | 8083 |
| **Analytics Service** | Business intelligence, GraphQL reporting, Excel exports | Java 21, Spring Boot 3.5, GraphQL, Apache POI | 8085 |

### Infrastructure Components

| Component | Description | Port(s) |
|-----------|-------------|---------|
| **PostgreSQL (auth-db)** | User service database | 5433 |
| **PostgreSQL (auction-db)** | Auction & analytics database | 5434 |
| **PostgreSQL (wallet-db)** | Payment service database | 5435 |
| **RabbitMQ** | Event-driven messaging broker | 5672, 15672 (Management UI) |
| **OpenSearch Node 1** | Centralized logging node | 9200, 9600 |
| **OpenSearch Node 2** | Clustered logging node | - |
| **OpenSearch Dashboards** | Log visualization UI | 5601 |
| **Fluent Bit** | Log aggregation and forwarding | - |

---

## System Flows

### User Authentication Flow

```
User → API Gateway (Envoy:8080) → User Service (8081)
  ↓
Validates credentials → Generates JWT (access + refresh tokens)
  ↓
Returns tokens to client
  ↓
All subsequent requests include JWT → Gateway forwards to User Service for validation
```

### Auction Creation & Bidding Flow
```
Seller creates auction → Auction Service validates → Stores in PostgreSQL
  ↓
Scheduled task monitors status (PENDING → ACTIVE → CLOSED)
  ↓
Buyer places bid → Auction Service validates (amount, timing, eligibility)
  ↓
Payment Service checks wallet → Locks required funds
  ↓
RabbitMQ publishes bid event → Notification Service sends email
  ↓
Analytics Service tracks metrics → Updates bid statistics
  ↓
Auction closes → Winner determined → Funds deducted from winner's wallet
```

### Payment Processing Flow
```
User deposits funds → Payment Gateway processes → Wallet balance updated
  ↓
Bid placed → Payment Service freezes bid amount (locked balance)
  ↓
Auction ends:
  - Winner → Locked funds deducted
  - Outbid users → Frozen funds released back to available balance
  ↓
RabbitMQ publishes payment events → Notification Service sends confirmations
```

### Real-time Notification Flow
```
Event occurs (bid placed, auction ended, payment processed)
  ↓
Service publishes event to RabbitMQ exchange
  ↓
Notification Service consumes from queue → Formats email
  ↓
Sends via SMTP (Gmail) → User receives notification
  ↓
All events logged to OpenSearch via Fluent Bit
```

### Analytics & Reporting Flow
```
Admin requests analytics → API Gateway → Analytics Service
  ↓
REST API: GET top bidders, popular auctions, statistics
  ↓
GraphQL API: Complex queries, Excel report generation
  ↓
Analytics Service queries auction-db → Processes data
  ↓
Returns JSON response or base64-encoded Excel file
```

---

## Diagrams

- System Diagram: See `system-diagram.png`
- Container Diagram: See `container-diagram.png`

---

## API Endpoints

> **Note:** This is the single source of truth for all API endpoints. For request/response schemas and more examples, see the Swagger UI for each service or the OpenAPI YAML files in `api-specs/`.

### User Service (`/api/auth/v1`)

> **Note:** For full API details, request/response schemas, and more examples, see the [Swagger UI for User Service](http://localhost:8081/swagger-ui.html).
> 
| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/auth/v1/register` | POST | Register new user account | No |
| `/api/auth/v1/login` | POST | Login and obtain JWT tokens | No |
| `/api/auth/v1/refresh-token` | POST | Refresh access token using refresh token | No |
| `/api/auth/v1/revoke` | POST | Revoke a specific refresh token (logout from device) | No |
| `/api/auth/v1/revoke-all` | POST | Revoke all refresh tokens for current user | Yes |
| `/api/auth/v1/me` | GET | Get current authenticated user's profile | Yes |
| `/api/auth/v1/users/{id}` | GET | Get user profile by user ID | Yes |

#### Example: Register a New User
Request:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "role": "BUYER"
}
```
Response:
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "BUYER",
  "createdAt": "2025-11-21T10:30:00Z"
}
```

#### Example: Login
Request:
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```
Response:
```json
{
  "accessToken": "<jwt-token>",
  "refreshToken": "<jwt-refresh-token>",
  "expiresIn": 3600
}
```




### Auction Service

> **Note:** For full API details, request/response schemas, and more examples, see the [Swagger UI for Auction Service](http://localhost:8082/swagger-ui.html).
> 
| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/bids/v1` | GET | List all bids | Yes |
| `/api/bids/v1/{id}` | GET | Get bid by ID | Yes |
| `/api/bids/v1/users/{userId}` | GET | Get all bids by user | Yes |
| `/api/bids/v1/products/{productId}` | GET | Get all bids for product | Yes |
| `/api/bids/v1` | POST | Place a bid on auction | Yes |
| `/api/products/v1` | GET | List all products | No |
| `/api/products/v1/{id}` | GET | Get product details | No |
| `/api/products/v1/category/{category}` | GET | Get products by category | No |
| `/api/products/v1` | POST | Create new product | Yes |
| `/api/products/v1/{id}` | PUT | Update product | Yes |
| `/api/products/v1/{id}` | DELETE | Delete product | Yes |
| `/api/categories/v1` | GET | List all categories | No |
| `/api/categories/v1/{id}` | GET | Get category details | No |
| `/api/categories/v1` | POST | Create new category | Yes |
| `/api/categories/v1/{id}` | PUT | Update category | Yes |
| `/api/categories/v1/{id}` | DELETE | Delete category | Yes |

#### Example: Place Bid
Request:
```json
{
  "auctionId": 1,
  "amount": 800.00
}
```
Response:
```json
{
  "id": 42,
  "auctionId": 1,
  "bidderId": 1,
  "amount": 800.00,
  "timestamp": "2025-11-21T11:15:30Z",
  "status": "ACCEPTED"
}
```

### Payment Service Endpoints

> **Note:** For full API details, request/response schemas, and more examples, see the [Swagger UI for Payment Service](http://localhost:8086/swagger-ui.html).

| Endpoint | Method | Description | Auth Required | Role |
|----------|--------|-------------|---------------|------|
| `/api/payment/wallet/v1/deposit` | POST | Add funds to wallet | Yes | - |
| `/api/payment/wallet/v1/{userId}` | GET | Get wallet balance and details | Yes | Owner/ADMIN |
| `/api/payment/wallet/v1/freeze` | POST | Lock funds for bid (internal) | Yes | System |
| `/api/payment/wallet/v1/unfreeze` | POST | Release locked funds | Yes | System |
| `/api/payment/wallet/v1/deduct` | POST | Deduct funds from wallet | Yes | System |
| `/api/payment/transactions/v1/{userId}` | GET | Get transaction history | Yes | Owner/ADMIN |
| `/api/payment/v1/swagger-ui.html` | GET | Swagger UI for Payment Service | No | - |

### Notification Service Endpoints

| Endpoint | Method | Description | Auth Required | Role |
|----------|--------|-------------|---------------|------|
| `/api/notifications/v1/send` | POST | Send notification email | Yes | System |
| `/api/notifications/v1/health` | GET | Health check | No | - |


### Analytics Service

> **Note:** For full API details, request/response schemas, and more examples, see the [Swagger UI for Analytics Service](http://localhost:8085/swagger-ui.html).


| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/analytics/v1/analytics/top-bidders` | GET | Get top bidders by total amount | Yes |
| `/api/analytics/v1/auctions/popular` | GET | Get popular auctions by bid count | Yes |
| `/api/analytics/v1/auctions/{auctionId}/stats` | GET | Get detailed auction statistics | Yes |
| `/api/analytics/v1/events/new-bid` | POST | Event: new bid placed | Yes (internal) |
| `/api/analytics/v1/events/auction-status` | POST | Event: auction status update | Yes (internal) |

#### Example: Get Top Bidders
Request:
`GET /api/analytics/v1/analytics/top-bidders?limit=5`
Response:
```json
[
  {
    "userId": 3,
    "userName": "Jane Smith",
    "totalBids": 45,
    "totalAmount": 12500.00,
    "rank": 1
  },
  {
    "userId": 1,
    "userName": "John Doe",
    "totalBids": 12,
    "totalAmount": 3200.00,
    "rank": 2
  }
]
```

**Sample Request Bodies:**


#### Example: Create Auction
Request:
```json
{
  "productId": 1,
  "startingPrice": 500.00,
  "reservePrice": 1000.00,
  "startTime": "2025-11-22T10:00:00Z",
  "endTime": "2025-11-25T18:00:00Z",
  "description": "Rare vintage watch in excellent condition"
}
```
Response:
```json
{
  "id": 1,
  "productId": 1,
  "startingPrice": 500.00,
  "reservePrice": 1000.00,
  "status": "PENDING",
  "description": "Rare vintage watch in excellent condition"
}
```

#### Example: Place Bid
Request:
```json
{
  "auctionId": 1,
  "amount": 800.00
}
```
Response:
```json
{
  "id": 42,
  "auctionId": 1,
  "bidderId": 1,
  "amount": 800.00,
  "timestamp": "2025-11-21T11:15:30Z",
  "status": "ACCEPTED"
}
```

#### Example: Deposit Funds
Request:
```json
{
  "userId": 1,
  "amount": 500.00,
  "paymentMethod": "CREDIT_CARD",
  "paymentGatewayToken": "tok_1234567890"
}
```
Response:
```json
{
  "transactionId": 1001,
  "userId": 1,
  "amount": 500.00,
  "status": "SUCCESS",
  "balance": 1500.00
}
```

#### Example: Send Notification
Request:
```json
{
  "to": "user@example.com",
  "subject": "Auction Won!",
  "body": "Congratulations, you have won the auction."
}
```
Response:
```json
{
  "status": "SENT",
  "to": "user@example.com"
}
```

#### Example: Get Top Bidders
Request:
`GET /api/analytics/v1/bidders/top?limit=5`

Response:
```json
[
  {
    "userId": 3,
    "userName": "Jane Smith",
    "totalBids": 45,
    "totalAmount": 12500.00,
    "rank": 1
  },
  {
    "userId": 1,
    "userName": "John Doe",
    "totalBids": 12,
    "totalAmount": 3200.00,
    "rank": 2
  }
]
```


---
See also the `api-specs/` directory for OpenAPI YAML files.

---

For more, see [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md).
