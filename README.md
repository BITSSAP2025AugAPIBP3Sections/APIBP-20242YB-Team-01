<div align="center">

# Ebaazee - Microservices-Based Online Auction System with REST-Based APIs


</div>

---

## Overview

### Problem
Traditional auction platforms struggle with **scalability**, **real-time bidding reliability**, and **payment processing complexity**. Monolithic architectures make it difficult to handle concurrent auctions, manage notifications efficiently, and process payments securely while maintaining system resilience.

### The Solution
**Ebaazee** is a **production-ready microservices-based auction platform** that provides:
-  **Secure Authentication** with JWT tokens and role-based access control
- **Real-time Auction Management** with concurrent bid handling and WebSocket updates
- **Integrated Payment Processing** with wallet management and fund locking
- **Automated Email Notifications** via event-driven architecture
-  **Advanced Analytics & Reporting** with GraphQL and Excel exports
-  **Centralized Logging** using OpenSearch cluster and Fluent Bit
- **API Gateway** with Envoy for intelligent routing and circuit breaking

---

## System Architecture

### Microservices Overview

| Service | Description | Technology Stack | Port |
|---------|-------------|------------------|------|
| **User Service (API Gateway -Envoy)** | Authentication, authorization, JWT management, user profiles | Java 21, Spring Boot 3.5, PostgreSQL, Redis | 8081 |
| **Auction Service** | Auction & bid management, product catalog, scheduled tasks | Java 21, Spring Boot 3.5, PostgreSQL, RabbitMQ | 8082 |
| **Payment Service** | Wallet operations, payment gateway, fund locking/unlocking | Node.js 20, TypeScript, Express, PostgreSQL | 8086 |
| **Notification Service** | Email notifications via SMTP, event consumption | Go 1.22, RabbitMQ, Gmail SMTP | 8083 |
| **Analytics Service** | Business intelligence, GraphQL reporting, Excel exports | Java 21, Spring Boot 3.5, GraphQL, Apache POI | 8085 |


###  Infrastructure Components

| Component | Description | Port(s) |
|-----------|-------------|---------|
| **PostgreSQL (auth-db)** | User service database | 5433 |
| **PostgreSQL (auction-db)** | Auction & analytics database | 5434 |
| **PostgreSQL (wallet-db)** | Payment service database | 5435 |
| **Redis** | Session management, token caching | 6379 |
| **RabbitMQ** | Event-driven messaging broker | 5672, 15672 (Management UI) |
| **OpenSearch Node 1** | Centralized logging node | 9200, 9600 |
| **OpenSearch Node 2** | Clustered logging node | - |
| **OpenSearch Dashboards** | Log visualization UI | 5601 |
| **Fluent Bit** | Log aggregation and forwarding | - |

---

## System Flow

### User Authentication Flow
```
User → API Gateway (Envoy:8080) → User Service (8081)
  ↓
Validates credentials → Generates JWT (access + refresh tokens)
  ↓
Stores session in Redis → Returns tokens to client
  ↓
All subsequent requests include JWT → Gateway validates via User Service
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

###  Payment Processing Flow
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

## System Diagram

![alt text](system-diagram.png)

## Container Diagram

![alt text](container-diagram.png)

## Quick Start

### Prerequisites

| Requirement | Version | Download |
|-------------|---------|----------|
| **Docker Desktop** | Latest | [Download](https://www.docker.com/products/docker-desktop) |
| **Docker Compose** | v2.0+ | Included with Docker Desktop |
| **Git** | Latest | [Download](https://git-scm.com/) |
| **Minimum RAM** | 8GB | Recommended: 16GB |

---

### Method 1: Docker Compose (Recommended - Complete Stack)

**Perfect for:** Development, testing, demonstrations, production simulation  
**Time to run:** 3-5 minutes (first build may take 10-15 minutes)

```bash
# 1. Clone the repository
git clone https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YB-Team-01.git
cd APIBP-20242YB-Team-01

# 2. Start all services
./run-all.sh
# Or manually: docker compose up --build

# 3. Wait for services to initialize (2-3 minutes)
# Watch logs: docker compose logs -f

**What you get automatically:**
-  **Complete microservices stack** (5 production-ready services)
- **3 PostgreSQL databases** (isolated per service domain)
-  **Redis cache** for session management
- **RabbitMQ message broker** with management UI
- **OpenSearch cluster** (2-node) for centralized logging
- **OpenSearch Dashboards** for log visualization
-  **Fluent Bit** for automatic log aggregation
-  **Envoy API Gateway** with circuit breaking and retries
-  **JWT authentication** ready to use
-  **Email notification system** (configure SMTP credentials)

---

### Method 2: Individual Service Setup (Development Mode)

**Perfect for:** Service-specific development, debugging, isolated testing

```bash
# 1. Start infrastructure only
docker compose up auth-db auction-db wallet-db redis rabbitmq -d

# 2. Run individual services locally
# See service-specific README files:
cd services/user-service && ./mvnw spring-boot:run
cd services/auction-service && ./mvnw spring-boot:run
cd services/payment-service && npm install && npm start
cd services/notification-service && go run cmd/notifier/main.go
cd services/analytics-service && ./mvnw spring-boot:run
```

---

### Complete Workflow Test

####  Register a New User
```bash
curl -X POST http://localhost:8080/api/auth/v1/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "role": "BUYER"
  }'

# Expected Response:
# {
#   "id": 1,
#   "name": "John Doe",
#   "email": "john.doe@example.com",
#   "role": "BUYER",
#   "createdAt": "2025-11-21T10:30:00Z"
# }
```

#### Login and Obtain JWT Token
```bash
curl -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123!"
  }'

# Expected Response:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "expiresIn": 3600
# }

# Save the accessToken for subsequent requests
export TOKEN="<your-access-token>"
```

####  View Available Auctions
```bash
curl http://localhost:8080/api/auctions/v1 \
  -H "Authorization: Bearer $TOKEN"

# Expected Response:
# [
#   {
#     "id": 1,
#     "title": "Vintage Watch",
#     "description": "Rare 1960s Omega Seamaster",
#     "startingPrice": 500.00,
#     "currentBid": 750.00,
#     "status": "ACTIVE",
#     "endTime": "2025-11-25T18:00:00Z",
#     "sellerId": 5,
#     "categoryId": 2
#   }
# ]
```

####  Get Product Details
```bash
curl http://localhost:8080/api/products/v1 \
  -H "Authorization: Bearer $TOKEN"

# Expected Response:
# {
#   "id": 1,
#   "name": "Vintage Omega Watch",
#   "description": "Authentic 1960s timepiece",
#   "category": "Watches & Jewelry",
#   "images": ["https://cdn.example.com/watch1.jpg"],
#   "condition": "EXCELLENT"
# }
```

####  Check Wallet Balance
```bash
curl http://localhost:8080/api/payment/wallet/<userId> \
  -H "Authorization: Bearer $TOKEN"

# Expected Response:
# {
#   "userId": 1,
#   "availableBalance": 1000.00,
#   "lockedBalance": 0.00,
#   "totalBalance": 1000.00,
#   "currency": "USD"
# }
```

#### Place a Bid
```bash
curl -X POST http://localhost:8080/api/bids/v1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": 1,
    "amount": 800.00
  }'

# Expected Response:
# {
#   "id": 42,
#   "auctionId": 1,
#   "bidderId": 1,
#   "amount": 800.00,
#   "timestamp": "2025-11-21T11:15:30Z",
#   "status": "ACCEPTED"
# }
# 
# Side Effects:
# - $800 locked in your wallet
# - RabbitMQ event published
# - Email notification sent
# - Analytics updated
```

#### View Bids for an Auction
```bash
curl http://localhost:8080/api/bids/auction/v1 \
  -H "Authorization: Bearer $TOKEN"

# Expected Response:
# [
#   {
#     "id": 42,
#     "bidderId": 1,
#     "bidderName": "John Doe",
#     "amount": 800.00,
#     "timestamp": "2025-11-21T11:15:30Z"
#   },
#   {
#     "id": 41,
#     "bidderId": 3,
#     "bidderName": "Jane Smith",
#     "amount": 750.00,
#     "timestamp": "2025-11-21T10:45:20Z"
#   }
# ]
```

#### View Analytics (Admin Only)
```bash
# Top Bidders
curl http://localhost:8080/api/analytics/v1/bidders/top?limit=5 \
  -H "Authorization: Bearer $TOKEN"

# Expected Response:
# [
#   {
#     "userId": 3,
#     "userName": "Jane Smith",
#     "totalBids": 45,
#     "totalAmount": 12500.00,
#     "rank": 1
#   },
#   {
#     "userId": 1,
#     "userName": "John Doe",
#     "totalBids": 12,
#     "totalAmount": 3200.00,
#     "rank": 2
#   }
# ]

# Popular Auctions
curl http://localhost:8080/api/analytics/v1/auctions/popular?limit=5 \
  -H "Authorization: Bearer $TOKEN"

# Expected Response:
# [
#   {
#     "auctionId": 1,
#     "title": "Vintage Watch",
#     "totalBids": 23,
#     "uniqueBidders": 8,
#     "currentPrice": 800.00,
#     "views": 342
#   }
# ]
```

#### Download Excel Report (GraphQL - Admin Only)
```bash
curl -X POST http://localhost:8080/api/analytics/v1/graphql \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { downloadProductReport(productId: 1) { filename base64Content } }"
  }'

# Expected Response:
# {
#   "data": {
#     "downloadProductReport": {
#       "filename": "product_1_report.xlsx",
#       "base64Content": "UEsDBBQACAgIAAAAAAAAAAAAAAAAAAAAAAAL..."
#     }
#   }
# }
# 
# 💡 Decode base64Content to save as Excel file:
# echo "<base64Content>" | base64 -d > report.xlsx
```

---

## API Endpoints

### User Service (Port 8081 → Gateway: /api/auth, /api/users)

| Endpoint | Method | Description | Auth Required | Role |
|----------|--------|-------------|---------------|------|
| `/api/auth/v1/register` | POST | Register new user account | No | - |
| `/api/auth/v1/login` | POST | Login and obtain JWT tokens | No | - |

**Sample Request Bodies:**

```json
// Register
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "role": "BUYER"
}

// Login
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

---

### Auction Service (Port 8082 → Gateway: /api/auctions, /api/bids, /api/products, /api/categories)

| Endpoint | Method | Description | Auth Required | Role |
|----------|--------|-------------|---------------|------|
| `/api/auctions` | GET | List all active auctions | No | - |
| `/api/auctions/{id}` | GET | Get auction details | No | - |
| `/api/auctions` | POST | Create new auction | Yes | SELLER |
| `/api/auctions/{id}` | PUT | Update auction details | Yes | SELLER (Owner) |
| `/api/auctions/{id}` | DELETE | Delete auction | Yes | SELLER/ADMIN |
| `/api/bids` | POST | Place a bid on auction | Yes | BUYER |
| `/api/bids/auction/{auctionId}` | GET | Get all bids for auction | Yes | - |
| `/api/bids/user/{userId}` | GET | Get user's bid history | Yes | Owner/ADMIN |
| `/api/products` | GET | List all products | No | - |
| `/api/products/{id}` | GET | Get product details | No | - |
| `/api/products` | POST | Create new product | Yes | SELLER |
| `/api/categories` | GET | List all categories | No | - |
| `/api/categories/{id}` | GET | Get category details | No | - |

**Sample Request Bodies:**

```json
// Create Auction
{
  "productId": 1,
  "startingPrice": 500.00,
  "reservePrice": 1000.00,
  "startTime": "2025-11-22T10:00:00Z",
  "endTime": "2025-11-25T18:00:00Z",
  "description": "Rare vintage watch in excellent condition"
}

// Place Bid
{
  "auctionId": 1,
  "amount": 800.00
}
```

---

### Payment Service (Port 8086 → Gateway: /api/payment)

| Endpoint | Method | Description | Auth Required | Role |
|----------|--------|-------------|---------------|------|
| `/api/payment/wallet/deposit` | POST | Add funds to wallet | Yes | - |
| `/api/payment/wallet/{userId}` | GET | Get wallet balance and details | Yes | Owner/ADMIN |
| `/api/payment/wallet/freeze` | POST | Lock funds for bid (internal) | Yes | System |
| `/api/payment/wallet/unfreeze` | POST | Release locked funds | Yes | System |
| `/api/payment/wallet/deduct` | POST | Deduct funds from wallet | Yes | System |
| `/api/payment/transactions/{userId}` | GET | Get transaction 

**Sample Request Bodies:**

```json
// Deposit Funds
{
  "userId": 1,
  "amount": 500.00,
  "paymentMethod": "CREDIT_CARD",
  "paymentGatewayToken": "tok_1234567890"
}

// Freeze Funds (Internal)
{
  "userId": 1,
  "amount": 800.00,
  "bidId": 42,
  "reason": "BID_PLACEMENT"
}
```

---

### Analytics Service (Port 8085 → Gateway: /api/analytics)

#### REST Endpoints

| Endpoint | Method | Description | Auth Required | Role |
|----------|--------|-------------|---------------|------|
| `/api/analytics/v1/bidders/top` | GET | Get top bidders by total amount | Yes | ADMIN |
| `/api/analytics/v1/auctions/popular` | GET | Get popular auctions by bid count | Yes | ADMIN |
| `/api/analytics/v1/auctions/{id}/stats` | GET | Get detailed auction statistics | Yes | ADMIN |

**Query Parameters:**
- `limit` (number): Limit results (default: 10, max: 100)
- `startDate` (ISO date): Filter from date
- `endDate` (ISO date): Filter to date

#### GraphQL Endpoint: `/api/analytics/graphql`

**Available Queries:**

```graphql
query {
  # Download Excel report (returns base64)
  downloadProductReport(productId: 1) {
    filename
    base64Content
    generatedAt
  }
}
```

**Available Mutations:**

```graphql
mutation {
  # Process auction completion
  processAuctionCompletion(
    auctionId: 1
    winnerId: 456
    finalAmount: 800.00
  ) {
    success
    message
  }
}
```
---
### API Gateway Routes (Envoy - Port 8080)

All client requests go through the API Gateway with intelligent routing:

| Route Pattern | Target Service | Features |
|---------------|----------------|----------|
| `/api/auth/v1*` | User Service (8081) | Retry on 5xx (2 attempts), 2s timeout |
| `/api/users/v1*` | User Service (8081) | Retry on 5xx (2 attempts), 2s timeout |
| `/api/products/v1*` | Auction Service (8082) | Retry on 5xx (1 attempt), 2s timeout |
| `/api/auctions/v1*` | Auction Service (8082) | Circuit breaker (max 100 connections) |
| `/api/bids/v1*` | Auction Service (8082) | Circuit breaker (max 100 connections) |
| `/api/categories/v1*` | Auction Service (8082) | Load balancing (round-robin) |
| `/api/payment/*` | Payment Service (8086) | Circuit breaker (max 50 connections) |
| `/api/analytics/*` | Analytics Service (8085) | Standard routing |

**Gateway Features:**
- Circuit breaking (prevents cascading failures)
- Retry policies (automatic retry on transient errors)
- Load balancing (round-robin across instances)
- Request timeout management (configurable per route)
- Health checks (automatic endpoint monitoring)
- Connection pooling (optimized resource usage)

---

## Tech Stack

### Backend Services

| Service | Language/Framework | Database | Key Dependencies |
|---------|-------------------|----------|------------------|
| **User Service** | Java 21, Spring Boot 3.5 | PostgreSQL 15, Redis 7 | Spring Security, JWT (io.jsonwebtoken), Spring Data JPA, Lombok |
| **Auction Service** | Java 21, Spring Boot 3.5 | PostgreSQL 15 | Spring Data JPA, Spring Scheduling, RabbitMQ, Lombok, Validation API |
| **Payment Service** | Node.js 20, TypeScript 5 | PostgreSQL 15 | Express.js, pg (PostgreSQL), Opossum (circuit breaker), amqplib |
| **Notification Service** | Go 1.22 | - (Stateless) | net/smtp, RabbitMQ client (amqp091-go), dotenv |
| **Analytics Service** | Java 21, Spring Boot 3.5 | PostgreSQL 15 (shared with Auction) | Spring GraphQL, Apache POI (Excel), Spring WebFlux, Lombok |

### Infrastructure & DevOps

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **API Gateway** | Envoy Proxy | v1.27 | HTTP routing, load balancing, circuit breaking, retries |
| **Databases** | PostgreSQL | 15 | Persistent data storage (3 isolated instances) |
| **Caching** | Redis | 7 | Session management, JWT token storage, rate limiting |
| **Messaging** | RabbitMQ | 3 (Management) | Event-driven communication, async processing |
| **Logging** | OpenSearch | 2.x | Centralized log storage, search, and analysis |
| **Log Aggregation** | Fluent Bit | Latest | Collect logs from Docker containers → OpenSearch |
| **Visualization** | OpenSearch Dashboards | 2.x | Log visualization, monitoring, alerting |
| **Containerization** | Docker, Docker Compose | Latest | Service orchestration, deployment |

### Security & Monitoring

- **Authentication:** JWT (RS256/HS256), Spring Security 6, bcrypt password hashing
- **Authorization:** Role-based access control (BUYER, SELLER, ADMIN)
- **API Security:** Envoy rate limiting, request validation, CORS configuration
- **Data Security:** PostgreSQL role-based access, encrypted connections
- **Logging:** Structured JSON logs via Fluent Bit → OpenSearch cluster
- **Monitoring:** OpenSearch Dashboards for real-time log visualization
- **Health Checks:** Built-in health endpoints for all services

---

## Key Features

### Authentication & Authorization
- JWT-based authentication with access + refresh tokens
- Role-based access control (BUYER, SELLER, ADMIN)
-  Password hashing with BCrypt (strength: 12 rounds)
-  Redis-backed session management
-  Secure HTTP-only cookies for token storage
-  Token refresh mechanism (automatic renewal)
-  OAuth 2.0 ready architecture

###  Auction Management
- Product catalog with hierarchical categories
- Auction lifecycle management (PENDING → ACTIVE → CLOSED → COMPLETED)
- Concurrent bid handling with optimistic locking
- Scheduled tasks for automatic status updates (Spring @Scheduled)
-  Real-time bid validation (minimum increment, timing, user eligibility)
-  Reserve price protection
-  Data loader for sample data (dev-db profile)
- WebSocket support for real-time updates

###  Payment Processing
- Wallet-based payment system (balance + locked funds)
-  Fund locking mechanism for active bids
-  External payment gateway integration (simulated)
-  Circuit breaker pattern for reliability (Opossum)
-  Transaction history and audit trail
- RabbitMQ event publishing (payment.locked, payment.success, payment.failed)
-  Automatic fund release for outbid users
-  Payment reconciliation

### Notifications
- Email notifications via SMTP (Gmail integration)
-  RabbitMQ event consumption (fan-out exchange)
-  Support for multiple event types:
  - Bid placed
  - Auction won
  - Payment confirmation
  - Payment failed
-  Configurable SMTP settings via environment variables
-  Email templating system
- Retry logic for failed deliveries

###  Analytics & Reporting
-  REST API for basic analytics (top bidders, popular auctions)
-  GraphQL API for complex queries and aggregations
- Excel report generation (Apache POI) with base64 encoding
-  Real-time statistics computation
-  Top bidders and popular auctions tracking
-  Revenue analytics and trend analysis
-  Auction performance metrics
- Data visualization-ready APIs

###  Logging & Monitoring
- Centralized logging with OpenSearch cluster (2-node)
-  Automatic log aggregation with Fluent Bit
-  OpenSearch Dashboards for visualization
-  Structured JSON log format
-  Service-specific log levels (DEBUG, INFO, WARN, ERROR)
-  Request/response logging
-  Performance metrics tracking
-  Error tracking and alerting-ready

---

##  Deployment

### Docker Compose Architecture

```yaml
Infrastructure Layer:
  ├── auth-db (PostgreSQL:5433)
  ├── auction-db (PostgreSQL:5434)
  ├── wallet-db (PostgreSQL:5435)
  ├── redis (Redis:6379)
  ├── rabbitmq (RabbitMQ:5672, Management:15672)
  ├── opensearch-node1 (OpenSearch:9200, 9600)
  ├── opensearch-node2 (OpenSearch - clustered)
  ├── opensearch-dashboards (Dashboards:5601)
  └── fluent-bit (Log aggregation)

Application Layer (via Gateway:8080):
  ├── envoy (API Gateway:8080, Admin:9901)
  ├── user-service (Internal:8081)
  ├── auction-service (Internal:8082)
  ├── payment-service (Internal:8081, External:8086)
  ├── notification-service (Internal:8080, External:8083)
  └── analytics-service (Internal:8085)

Data Flow:
  Client → Envoy Gateway → Microservices → Databases
           ↓
  RabbitMQ (Events) → Notification Service
           ↓
  Fluent Bit → OpenSearch Cluster → Dashboards
```

---

### Deployment Commands

```bash
#  Start all services
./run-all.sh
# Or: docker compose up --build

#  Start specific services
docker compose up user-service auction-service payment-service

#  View logs (all services)
docker compose logs -f

#  View logs (specific service)
docker compose logs -f user-service

#  Check running containers
docker compose ps

#  Stop all services
docker compose down

#  Stop and remove volumes (deletes all data)
docker compose down -v

#  Restart a service
docker compose restart user-service

# Rebuild specific service
docker compose up --build user-service

#  Clean up unused resources
docker system prune -a --volumes
```

---

### Service Health Monitoring

```bash
# Check all services health
docker compose ps

#  View resource usage
docker stats

# Access OpenSearch Dashboards
open http://localhost:5601

# Access RabbitMQ Management UI
open http://localhost:15672
# Default credentials: guest/guest

#  Access Envoy Admin Interface
open http://localhost:9901

#  Check logs in OpenSearch
# 1. Go to http://localhost:5601
# 2. Navigate to "Discover"
# 3. Create index pattern: fluent-bit-*
# 4. Filter by kubernetes.container_name or log_level

#  Monitor RabbitMQ Queues
# 1. Go to http://localhost:15672
# 2. Login with guest/guest
# 3. Navigate to "Queues" tab
# 4. Check: payment-service-consumer, notification-queue
```

---

##  Troubleshooting

### Issue: Services won't start

```bash
# Check logs for errors
docker compose logs <service-name>

# Common fixes:
# 1. Restart the service
docker compose restart <service-name>

# 2. Rebuild the image
docker compose up --build <service-name>

# 3. Check for port conflicts
lsof -i :<port-number>
kill -9 <PID>

# 4. Clean Docker cache
docker system prune -a
```

---

### Issue: Database connection failures

```bash
# Check if databases are running
docker compose ps auth-db auction-db wallet-db

# View database logs
docker compose logs auth-db

# Restart databases
docker compose restart auth-db auction-db wallet-db

# Verify database is accepting connections
docker exec -it <db-container> psql -U <username> -d <database>
```

---

### Issue: RabbitMQ connection errors

```bash
# Restart RabbitMQ
docker compose restart rabbitmq

# Check RabbitMQ logs
docker compose logs rabbitmq

# Wait 30-60 seconds after startup for RabbitMQ to fully initialize

# Verify RabbitMQ is ready
curl http://localhost:15672/api/overview
# Login: guest/guest
```

---

### Issue: Port already in use

```bash
# Find process using port
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change port in docker-compose.yml
# ports:
#   - "8090:8080"  # Use 8090 instead
```

---

### Issue: Out of memory errors

```bash
# Check Docker memory allocation
docker stats

# Increase Docker memory:
# Docker Desktop → Settings → Resources → Memory: 8GB+

# Or reduce running services:
docker compose up envoy user-service auction-service payment-service

# Stop unused services:
docker compose stop notification-service analytics-service
```

---

### Issue: Builds failing (Java services)

```bash
# Common Maven issues:

# 1. Clear Maven cache in container
docker compose down
docker volume prune

# 2. Check JDK version in pom.xml
# Should be: <java.version>21</java.version>

# 3. Build locally to verify
cd services/user-service
./mvnw clean package

# 4. Ensure Docker can pull Maven image
docker pull maven:3.9.4-eclipse-temurin-21
```

---

### Issue: Payment service fails to build

```bash
# TypeScript compilation issues:

# 1. Install dependencies locally
cd services/payment-service
npm install
# Or: pnpm install

# 2. Verify tsconfig.json exists
cat tsconfig.json

# 3. Build locally to test
npm run build

# 4. Ensure Dockerfile installs dev dependencies
# Should have: RUN npm install (not npm install --production)
```

---

### Issue: Email notifications not sending

```bash
# Configure SMTP credentials:

# 1. Create .env file in project root
cat > .env << EOF
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
EOF

# 2. For Gmail, create an App Password:
# https://myaccount.google.com/apppasswords

# 3. Restart notification service
docker compose restart notification-service

# 4. Check logs
docker compose logs -f notification-service
```

---

### Issue: OpenSearch not starting

```bash
# Common OpenSearch issues:

# 1. Increase vm.max_map_count (Linux/macOS)
sudo sysctl -w vm.max_map_count=262144

# 2. Make it permanent
echo 'vm.max_map_count=262144' | sudo tee -a /etc/sysctl.conf

# 3. Restart OpenSearch
docker compose restart opensearch-node1 opensearch-node2

# 4. Check OpenSearch logs
docker compose logs opensearch-node1

# 5. Verify OpenSearch is up
curl http://localhost:9200
```

---

##  Additional Resources

### Service-Specific Documentation
- [User Service README](services/user-service/INFO.MD)
- [Auction Service README](services/auction-service/INFO.MD)
- [Payment Service README](services/payment-service/INFO.MD)
- [Notification Service README](services/notification-service/INFO.MD)
- [Analytics Service README](services/analytics-service/INFO.MD)
- [API Gateway Configuration](gateway/api-gateway/API-GATEWAY.MD)

### OpenAPI Specifications
Located in `api-specs/` directory:
- [User Service API](api-specs/user-service.yaml)
- [Auction Service API](api-specs/auction-service.yaml)
- [Payment Service API](api-specs/payment-service.yaml)
- [Notification Service API](api-specs/notification-service.yaml)
- [Analytics Service API](api-specs/analytics-service.yaml)

### Configuration Files
- [Envoy Gateway Config](gateway/envoy/envoy.yaml)
- [Docker Compose](docker-compose.yml)
- [Fluent Bit Config](fluent-bit.conf)
- [Log Parsers](parsers.conf)

---

## Security Best Practices

### Production Deployment Checklist

#### Authentication & Authorization
- [ ] Change default JWT secret (use 256-bit key)
- [ ] Implement token rotation policy
- [ ] Set appropriate token expiration (access: 15min, refresh: 7 days)
- [ ] Enable MFA for admin accounts
- [ ] Implement account lockout after failed attempts

#### Database Security
- [ ] Change all default database passwords
- [ ] Use strong passwords (16+ chars, mixed case, symbols)
- [ ] Enable SSL/TLS for database connections
- [ ] Implement database user role separation
- [ ] Enable audit logging
- [ ] Regular backup schedule (automated)
- [ ] Encrypt backups at rest

#### API Security
- [ ] Enable HTTPS/TLS for all services (Let's Encrypt)
- [ ] Configure CORS policies (whitelist domains)
- [ ] Implement rate limiting (e.g., 100 req/min per user)
- [ ] Add request size limits (prevent DoS)
- [ ] Enable API versioning (/api/v1/, /api/v2/)
- [ ] Implement request signing for service-to-service calls
- [ ] Add IP whitelisting for admin endpoints

#### Messaging & Events
- [ ] Enable SASL/SSL for RabbitMQ
- [ ] Create RabbitMQ user with limited permissions
- [ ] Use separate queues per service (isolation)
- [ ] Implement message encryption for sensitive data
- [ ] Set message TTL (time-to-live)
- [ ] Enable dead-letter queues for failed messages

#### Logging & Monitoring
- [ ] Enable OpenSearch authentication
- [ ] Change default OpenSearch admin password
- [ ] Implement log retention policy (e.g., 90 days)
- [ ] Set up alerting for critical errors
- [ ] Implement audit logging for sensitive operations
- [ ] Use structured logging (JSON format)
- [ ] Sanitize logs (remove sensitive data like passwords, tokens)

#### Infrastructure
- [ ] Use environment variables for all secrets
- [ ] Implement secrets management (HashiCorp Vault, AWS Secrets Manager)
- [ ] Enable Docker Content Trust (image signing)
- [ ] Run containers as non-root users
- [ ] Use read-only file systems where possible
- [ ] Implement network segmentation (separate backend/frontend networks)
- [ ] Enable container resource limits (CPU, memory)

#### Application Security
- [ ] Implement input validation (all endpoints)
- [ ] Use parameterized queries (prevent SQL injection)
- [ ] Enable CSRF protection
- [ ] Implement file upload restrictions (type, size)
- [ ] Sanitize user-generated content (prevent XSS)
- [ ] Add security headers (HSTS, X-Frame-Options, CSP)
- [ ] Regular dependency updates (npm audit, OWASP Dependency-Check)

#### Compliance & Auditing
- [ ] Implement GDPR compliance (data deletion, export)
- [ ] Log all access to sensitive data
- [ ] Implement user consent management
- [ ] Regular security audits (quarterly)
- [ ] Penetration testing (before production launch)
- [ ] Maintain security incident response plan
- [ ] Regular backup testing (recovery drills)

---

##  Contributing

We welcome contributions from the community! Please follow these guidelines:

### Getting Started
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Write/update tests
5. Ensure all tests pass
6. Commit with meaningful messages
7. Push to your fork
8. Open a Pull Request

### Code Style

#### Java Services
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Lombok annotations (@Data, @Builder, etc.)
- Document public APIs with Javadoc
- Keep methods under 50 lines
- Use meaningful variable names

```java
// Good
@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String name;
}

// Bad
public class UserDto {
    public Long i;
    public String e, n;
}
```

#### Node.js/TypeScript
- Use TypeScript strict mode
- Follow [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- Use async/await (avoid callbacks)
- Use ES modules (import/export)
- Document functions with JSDoc

```typescript
// Good
export async function getUserById(id: string): Promise<User> {
    const user = await db.findUser(id);
    if (!user) throw new NotFoundException();
    return user;
}

// Bad
exports.getUser = function(id, callback) {
    db.findUser(id, function(err, user) {
        callback(err, user);
    });
};
```

#### Go Service
- Follow [Effective Go](https://go.dev/doc/effective_go)
- Use `gofmt` for formatting
- Use meaningful package names
- Handle errors explicitly

```go
// Good
func sendEmail(to string, subject string, body string) error {
    if err := smtp.SendMail(config); err != nil {
        return fmt.Errorf("failed to send email: %w", err)
    }
    return nil
}
```

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(user-service): add password reset endpoint
fix(payment): resolve wallet balance lock issue
docs(readme): update API endpoint documentation
chore(deps): upgrade Spring Boot to 3.5.1
test(auction): add integration tests for bidding
refactor(analytics): improve query performance
```

### Pull Request Guidelines
- **Title:** Use format `[ServiceName] Brief description`
- **Description:** Clearly explain what and why
- **Link Issues:** Reference related issues (`Closes #123`)
- **Tests:** Include unit/integration tests
- **Documentation:** Update README and API specs if needed
- **Screenshots:** For UI changes (if applicable)
- **Breaking Changes:** Clearly mark and explain

### Testing Requirements
- Unit tests for new features
- Integration tests for API endpoints
- Minimum 70% code coverage
- All tests must pass before merge

---

##  License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 APIBP-20242YB-Team-01

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

## 👥 Team

**APIBP-20242YB-Team-01**  
BITS Pilani - API-Based Product Development Course

### Contributors
This project is built by students as part of the **API-Based Product Development** course at BITS Pilani.

<a href="https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YB-Team-01/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=BITSSAP2025AugAPIBP3Sections/APIBP-20242YB-Team-01" />
</a>

---

### Resources
- **GitHub Issues:** [Report bugs or request features](https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YB-Team-01/issues)
- **API Gateway:** `http://localhost:8080`
- **OpenSearch Dashboards:** `http://localhost:5601`
- **RabbitMQ Management:** `http://localhost:15672` (guest/guest)
- **Envoy Admin:** `http://localhost:9901`

### Common Commands
```bash
# Start everything
./run-all.sh

# Check health
curl http://localhost:8080/api/users/health

# View logs
docker compose logs -f

# Stop everything
docker compose down
```

---

<div align="center">

### Building Scalable Auction Platforms with Microservices

**Version:** 1.0.0  
**Status:** Production Ready  

Built with ❤️ by Team 01

[⬆ Back to Top](#-ebaazee---microservices-based-auction-platform)

</div>