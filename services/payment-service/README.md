# Payment Gateway Service

## Overview
A Node.js/TypeScript service for wallet management and payment processing, supporting deposit, freeze, deduction, and integration with RabbitMQ and a payment gateway. Wallets are persisted in PostgreSQL.

---

## Prerequisites
- Node.js (v20+ recommended)
- Docker (for PostgreSQL)

---

## Running PostgreSQL with Docker
Run the following command to start a PostgreSQL container named `pg-wallet`:

```sh
docker run --name pg-wallet -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=wallets -p 5435:5432 -d postgres:latest
```

- Host: `localhost`
- Port: `5435`
- User: `postgres`
- Password: `postgres`
- Database: `wallets`

Create the wallets table:

```sql
CREATE TABLE public.wallets (
  user_id VARCHAR PRIMARY KEY,
  balance NUMERIC NOT NULL DEFAULT 0,
  locked NUMERIC NOT NULL DEFAULT 0
);
```

---

## Running the Fake Payment Gateway

To simulate payment gateway responses, start the fake gateway:

```sh
node fake-payment-gateway/index.ts
```

This will run a mock payment gateway on port 9000 for local development.

---

## Environment Variables
Set these in a `.env` file or your shell:

```
PGHOST=localhost
PGPORT=5435
PGUSER=postgres
PGPASSWORD=postgres
PGDATABASE=wallets
PORT=8081
RABBITMQ_URL=amqp://guest:guest@localhost:5672
GATEWAY_BASE_URL=http://localhost:9000
GATEWAY_API_KEY=
```

---

## Install & Run

```sh
npm install
npm run dev
```

---

## API Endpoints & Use Cases

### 1. Deposit Funds
**POST /wallet/deposit**
- **Body:** `{ "userId": "user1", "amount": 100, "source": "card_123" }`
- **Use case:** User adds funds to their wallet via an external payment source.

### 2. Get Wallet Balance
**GET /wallet/:userId**
- **Use case:** Retrieve current balance and locked funds for a user.

### 3. Freeze Funds
**POST /wallet/freeze**
- **Body:** `{ "userId": "user1", "amount": 30 }`
- **Use case:** Lock funds for a bid or reservation (e.g., auction scenario).

### 4. Unfreeze Funds
**POST /wallet/unfreeze**
- **Body:** `{ "userId": "user1", "amount": 30 }`
- **Use case:** Release locked funds (e.g., user outbid in auction).

### 5. Deduct Locked Funds
**POST /wallet/deduct**
- **Body:** `{ "userId": "user1", "amount": 30, "auctionId": "a1" }`
- **Use case:** Finalize payment, deducting locked funds (e.g., auction winner).

### 6. Create Payment Intent
**POST /payment/create**
- **Body:** `{ "userId": "user1", "amount": 100, "meta": { ... } }`
- **Use case:** Create a payment intent with the external gateway.

---

## Example Workflow
1. Deposit funds to wallet.
2. Freeze funds for a bid.
3. Unfreeze if outbid, or deduct if won.
4. Check wallet balance at any time.

---

## Notes
- RabbitMQ is used for event publishing (e.g., deposit added, payment locked/unlocked).
- The payment gateway can be simulated with the included fake gateway on port 9000.
- All wallet operations are persisted in PostgreSQL.
