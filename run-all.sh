#!/usr/bin/env bash
set -euo pipefail

# Builds images and starts all services defined in the top-level docker-compose.yml
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "Starting all services from ${ROOT_DIR}..."

echo "Attempting to pull required images (best-effort)..."
if docker compose pull --ignore-pull-failures; then
  echo "Images pulled (or already present)."
else
  echo "Some image pulls failed, continuing to build/start what we can (pull failures ignored)."
fi

docker compose up --build -d --remove-orphans

echo "Waiting for databases and services to become healthy..."

check_tcp() {
  host=$1; port=$2; name=$3
  echo -n "Checking $name at $host:$port ... "
  for i in {1..30}; do
    if nc -z $host $port 2>/dev/null; then
      echo "ok"
      return 0
    fi
    sleep 1
  done
  echo "FAILED"
  return 1
}

# Wait for Postgres and RabbitMQ
check_tcp localhost 5433 "auth-db"
check_tcp localhost 5434 "auction-db"
check_tcp localhost 5435 "wallet-db"
check_tcp localhost 5672 "rabbitmq"

echo "Waiting for RabbitMQ to be fully ready..."
sleep 5

echo "All infra reachable. Give services a few more seconds to boot..."
sleep 10

echo "Checking frontend availability..."
check_tcp localhost 5173 "ebaazee-frontend"

docker compose ps

echo ""
echo "=========================================="
echo "All services are up and running!"
echo "=========================================="
echo "Use 'docker compose logs -f' to follow logs or 'docker compose down' to stop everything."
