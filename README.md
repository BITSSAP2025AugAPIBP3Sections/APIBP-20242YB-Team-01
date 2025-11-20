# Run all services (local development)

This repository contains 5 microservices and some supporting infrastructure. The repo now includes a top-level `docker-compose.yml` and a convenience script `run-all.sh` to build and start everything locally.

Quick start
1. From project root:

```bash
./run-all.sh
```

This will build images and start the following infra + services:

- Infra
  - Postgres (auth-db) -> host:5433
  - Postgres (auction-db) -> host:5434
  - Postgres (wallet-db) -> host:5435
  - Redis -> host:6379
  - RabbitMQ -> host:5672 (management UI: 15672)

- Services (host ports)
  - user-service -> 8081
  - auction-service -> 8082
  - analytics-service -> 8085
  - payment-service -> 8086 (container listens on 8081)
  - notification-service -> 8083 (container listens on 8080)

Useful commands

```bash
# show running containers
docker compose ps

# follow logs for all services
docker compose logs -f

# stop and remove
docker compose down
```

Notes & troubleshooting
- If you see a warning: "the attribute `version` is obsolete" it's harmless — this compose file now omits that line.
- Builds may take some time the first run as Maven/Go/Node layers are downloaded.
- If a Java build fails in Docker due to JDK issues, ensure Docker can pull `maven:3.9.4-eclipse-temurin-21` or adjust Java version in the service's `pom.xml`.
- If payment service fails to build due to `tsc` missing, run locally `pnpm install` or ensure the Dockerfile is allowed to install dev dependencies.
- For notification service to send emails you must provide SMTP credentials via env in `services/notification-service/.env` or set variables in compose.