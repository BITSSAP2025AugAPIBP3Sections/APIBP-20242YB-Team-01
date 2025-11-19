Local setup (Postgres + Redis) for auth-svc
=========================================

Quick start using Docker Compose (recommended for development):

1. Start services

```bash
docker compose up -d
```

This brings up:
- Postgres on localhost:5433 (container port 5432) (user: auth, password: authpw, db: authsvc)
- Redis on localhost:6379

Note: the compose file maps Postgres host port 5433 -> container 5432 to avoid conflicts with any existing local Postgres instances.
If you prefer to use the standard host port 5432, edit `docker-compose.yml` and change the Postgres ports mapping from `"5433:5432"` to `"5432:5432"`, but ensure no other process is listening on 5432.

2. Configure application properties (example `src/main/resources/application.properties` overrides):

```
# If you use the included docker-compose, Postgres will be available on host port 5433
spring.datasource.url=jdbc:postgresql://localhost:5433/authsvc
spring.datasource.username=auth
spring.datasource.password=authpw

# Redis defaults: localhost:6379
spring.redis.host=localhost
spring.redis.port=6379
```

Note: the project may include `application.properties`/`application.yml` in `src/main/resources` or you may have local overrides (e.g. `src/main/resources/application-local.properties` or IDE run config). Make sure your active configuration points to `jdbc:postgresql://localhost:5433/authsvc` if you started Postgres with the provided `docker-compose.yml` mappings. If you change docker-compose to map host port 5432, update this URL accordingly.

Note: some provided example config files in the repository use the database name `auth_db` (and may point to host port 5432). To avoid confusion pick one canonical DB name for local dev:
- Option A (recommended): update your `src/main/resources/application.properties`/`application.yml` to use `jdbc:postgresql://localhost:5433/authsvc` (matches `docker-compose.yml`).
- Option B: change `docker-compose.yml` to create `auth_db` and map host port 5432 -> container 5432.

Keeping names/ports consistent between compose, Flyway migrations and application properties avoids runtime connection issues.

3. Run the app

```bash
./mvnw spring-boot:run
```

Alternative: disable Redis autoconfiguration for quick boot
---------------------------------------------------------
If you don't need Redis locally (for caching or token blacklists), you can disable Redis auto-configuration so the application won't try to connect at startup. Add the following property to disable Redis auto-config:

```
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
```

Note: disabling Redis auto-configuration means any beans/components that rely on Redis will not function. Use only for quick local builds/tests.

Optional: run Redis via homebrew (macOS)
-------------------------------------
If you prefer not to use Docker:

```bash
brew install redis
brew services start redis
# or run redis-server in foreground:
redis-server /usr/local/etc/redis.conf
```
