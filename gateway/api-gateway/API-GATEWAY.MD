# API Gateway

The API Gateway serves as the single entry point for all Ebaazee microservices. 
It handles request routing, authentication, and load balancing between services. 
The gateway ensures secure and scalable communication for both REST APIs and WebSocket connections.

## Analytics GraphQL via Gateway

The gateway exposes the analytics GraphQL endpoint at:

- URL (gateway): `http://localhost:8080/api/analytics/graphql`
- Forwards to analytics-service `/graphql` (path rewritten by Envoy)
- Use POST with JSON GraphQL body and include `Authorization: Bearer <token>` header

Example curl (replace `$TOKEN` and `productId`):

```bash
curl -X POST http://localhost:8080/api/analytics/graphql \
	-H "Authorization: Bearer $TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"query":"query { downloadProductReport(productId: 1) { filename base64Content } }"}'
```

Notes:
- The analytics GraphQL query `downloadProductReport` requires an authenticated user with the `ADMIN` role.
- The response includes a `filename` and `base64Content` (xlsx encoded in base64). Decode client-side to save as an `.xlsx` file.
