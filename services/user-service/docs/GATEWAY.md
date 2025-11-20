Envoy-based API Gateway

We replaced the in-service proxy with an Envoy-based API Gateway. Envoy runs as a separate container and routes incoming HTTP requests to the appropriate backend services.

Where the config lives

The Envoy config is in `gateway/envoy/envoy.yaml` (static config). It listens on port 8080 and exposes an admin API on 9901.

Routes

- /api/auth*, /api/users* -> `user-service` (port 8081)
- /api/products* -> `auction-service` (port 8082)
- /api/auctions* -> `auction-service` (port 8082)
- /api/payment* -> `payment-service` (port 8081)
- /api/notification* or /api/notifications* -> `notification-service` (port 8080)
- /api/analytics* -> `analytics-service` (port 8085)
- Default fallback -> `user-service`

How to run (docker-compose)

From repository root run:

```bash
docker compose up --build envoy
```

or start the whole stack (envoy will be available at http://localhost:8080):

```bash
docker compose up --build
```

Notes and next steps

- The Envoy config includes basic retry and circuit-breaker thresholds. Tune these values for your environment.
- For production-grade features (rate limiting, auth, metrics), integrate Envoy filters, a rate-limit service, and Prometheus/Grafana.
- If you prefer a developer-friendly Java-based gateway, consider adding Spring Cloud Gateway as a separate service instead of an in-app controller.
