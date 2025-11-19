# Auction System

This is a Spring Boot-based auction system for managing items, bidding, and auction-related functionalities.

## Features
- User, Product, Category, and Bid management
- JWT-based authentication and authorization
- Scheduled auction tasks
- RESTful API endpoints
- PostgreSQL database integration

## Technologies Used
- Java 21
- Spring Boot 3.5.x
- Spring Data JPA
- Spring Security
- Lombok
- PostgreSQL
- Maven

## Getting Started
1. Clone the repository:
   ```sh
   git clone <repo-url>
   ```
2. Run a local Postgres for this microservice (we use a non-conflicting host port 5433 so it won't interfere with other running services):

   ```sh
   # from project root
   docker compose up -d
   ```

3. Start the auction microservice using the `dev-db` profile so it uses the Postgres above and seeds sample data:

   ```sh
   ./mvnw -Dspring-boot.run.profiles=dev-db spring-boot:run
   # or with the JVM property
   ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev-db"
   ```

4. The service will run on port 8082 by default (configured in `application-dev-db.properties`).

Notes:
- The Docker Compose file starts Postgres on host port 5434 and creates database `auction_db` with user `auction_user` / password `auction_pass`.
- The `DataLoader` component (active in profile `dev-db`) seeds example categories, users, products and a bid so you can exercise the APIs immediately.
- This keeps your existing auth service and its Postgres (usually on 5432) untouched.

## Development
- Use Lombok for model and DTO classes to reduce boilerplate.
- Annotation processing must be enabled in your IDE.
- All main code is under `src/main/java/com/core/auction_system`.

## License
MIT
