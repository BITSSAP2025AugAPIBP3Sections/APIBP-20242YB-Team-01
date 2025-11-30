# Infrastructure Services Deployment Guide

## Overview

Your Ebaazee platform uses two types of Docker images:

### 1. **Custom Application Images** (You build & push these)
- User Service
- Auction Service
- Payment Service
- Notification Service
- Analytics Service
- Frontend

### 2. **Public Infrastructure Images** (Already available on Docker Hub)
- PostgreSQL databases
- RabbitMQ
- Envoy Gateway
- OpenSearch (optional)

---

## Infrastructure Components

### Databases (PostgreSQL)

**Three separate PostgreSQL instances** are used for data isolation:

| Database | Purpose | Image | K8s Manifest | Service Name |
|----------|---------|-------|--------------|--------------|
| auth-db | User authentication & profiles | `postgres:15` | `k8s/auth-db.yaml` | `auth-db:5432` |
| auction-db | Auctions, bids, products | `postgres:15` | `k8s/auction-db.yaml` | `auction-db:5432` |
| wallet-db | Payment wallets & transactions | `postgres:15` | `k8s/wallet-db.yaml` | `wallet-db:5432` |

**Features:**
- ✅ Persistent storage via PersistentVolumeClaims (1Gi each)
- ✅ Health checks configured
- ✅ Separate credentials for security
- ✅ Uses official PostgreSQL 15 image from Docker Hub

**No action needed:** Kubernetes pulls `postgres:15` automatically from Docker Hub.

---

### Message Broker (RabbitMQ)

**Purpose:** Event-driven communication between services

| Component | Image | Port | Purpose |
|-----------|-------|------|---------|
| RabbitMQ | `rabbitmq:3-management` | 5672 | AMQP messaging |
| Management UI | (same) | 15672 | Admin interface |

**Used by:**
- Auction Service → Publishes payment events
- Payment Service → Consumes payment requests
- Notification Service → Consumes notification events

**Features:**
- ✅ Built-in management UI
- ✅ Uses official RabbitMQ image from Docker Hub
- ✅ Configured in `k8s/rabbitmq.yaml`

**No action needed:** Kubernetes pulls `rabbitmq:3-management` automatically.

---

### API Gateway (Envoy)

**Purpose:** Single entry point for all client requests

| Component | Image | Port | Purpose |
|-----------|-------|------|---------|
| Envoy | `envoyproxy/envoy:v1.27-latest` | 8080 | HTTP routing |
| Admin | (same) | 9901 | Envoy admin interface |

**Features:**
- ✅ Intelligent routing to microservices
- ✅ Load balancing
- ✅ Circuit breaking
- ✅ Rate limiting
- ✅ Configuration via ConfigMap

**Configuration:**
- Routes defined in `gateway/envoy/envoy.yaml`
- Loaded via ConfigMap in Kubernetes

**No action needed:** Kubernetes pulls `envoyproxy/envoy:v1.27-latest` automatically.

---

### Logging (OpenSearch) - Optional

**Note:** OpenSearch is defined in `docker-compose.yml` but **not yet in k8s/ manifests**.

If you want to deploy logging to Kubernetes:

| Component | Image | Purpose |
|-----------|-------|---------|
| OpenSearch Node 1 | `opensearchproject/opensearch:latest` | Log storage |
| OpenSearch Node 2 | `opensearchproject/opensearch:latest` | Cluster node |
| OpenSearch Dashboards | `opensearchproject/opensearch-dashboards:latest` | Log visualization |
| Fluent Bit | `fluent/fluent-bit:latest` | Log collection |

**Status:** Currently only in Docker Compose, not in Kubernetes manifests.

---

## Complete Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  CLIENT (Browser/Mobile)                                    │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  ENVOY API GATEWAY                                          │
│  Image: envoyproxy/envoy:v1.27-latest                       │
│  Port: 8080                                                 │
└─────┬────────────────────────────────────────────────────┬──┘
      │                                                     │
      ├─────────────────────┬───────────────────────────────┤
      ▼                     ▼                               ▼
┌─────────────┐   ┌─────────────────┐         ┌──────────────────┐
│ USER        │   │ AUCTION         │         │ PAYMENT          │
│ SERVICE     │   │ SERVICE         │         │ SERVICE          │
│ (Custom)    │   │ (Custom)        │         │ (Custom)         │
└──────┬──────┘   └────────┬────────┘         └────────┬─────────┘
       │                   │                           │
       ▼                   ▼                           ▼
┌─────────────┐   ┌─────────────────┐         ┌──────────────────┐
│ auth-db     │   │ auction-db      │         │ wallet-db        │
│ postgres:15 │   │ postgres:15     │         │ postgres:15      │
└─────────────┘   └─────────────────┘         └──────────────────┘
                          │                           │
                          └───────────┬───────────────┘
                                      ▼
                          ┌─────────────────────┐
                          │ RabbitMQ            │
                          │ rabbitmq:3-mgmt     │
                          └──────────┬──────────┘
                                     │
                   ┌─────────────────┴─────────────────┐
                   ▼                                   ▼
         ┌──────────────────┐              ┌──────────────────┐
         │ NOTIFICATION     │              │ ANALYTICS        │
         │ SERVICE          │              │ SERVICE          │
         │ (Custom)         │              │ (Custom)         │
         └──────────────────┘              └──────────────────┘
```

**Legend:**
- **Custom Images** = You build and push to Docker Hub
- **Public Images** = Already on Docker Hub (postgres, rabbitmq, envoy)

---

## Deployment Order (Recommended)

### Step 1: Deploy Infrastructure Services First

These have no dependencies and should start first:

```bash
# Deploy databases
kubectl apply -f k8s/auth-db.yaml
kubectl apply -f k8s/auction-db.yaml
kubectl apply -f k8s/wallet-db.yaml

# Wait for databases to be ready
kubectl get pods -l app=auth-db -w
# Wait until "Running"

# Deploy RabbitMQ
kubectl apply -f k8s/rabbitmq.yaml

# Wait for RabbitMQ to be ready
kubectl get pods -l app=rabbitmq -w
```

### Step 2: Deploy Application Services

After infrastructure is ready:

```bash
# Deploy application services
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/auction-service.yaml
kubectl apply -f k8s/payment-service.yaml
kubectl apply -f k8s/notification-service.yaml
kubectl apply -f k8s/analytics-service.yaml

# Watch them start
kubectl get pods -w
```

### Step 3: Deploy API Gateway

After services are up:

```bash
# Deploy Envoy gateway
kubectl apply -f k8s/envoy.yaml

# Verify
kubectl get pods -l app=envoy
```

### Step 4: Deploy Frontend (Optional)

```bash
# If you have frontend K8s manifest
kubectl apply -f k8s/frontend.yaml
```

---

## All-in-One Deployment (Easier)

Kubernetes is smart enough to handle dependencies, so you can deploy everything at once:

```bash
# Deploy everything
kubectl apply -f k8s/

# Watch all pods start
kubectl get pods -w
```

**Kubernetes will:**
1. Start databases first (no dependencies)
2. Start RabbitMQ
3. Start application services (may restart a few times waiting for DBs)
4. Start Envoy gateway
5. Everything eventually reaches "Running" state

---

## Verifying Infrastructure Services

### Check Databases

```bash
# Check all database pods
kubectl get pods | grep db

# Should see:
# auth-db-xxx       Running
# auction-db-xxx    Running
# wallet-db-xxx     Running

# Test database connection
kubectl exec -it deployment/auth-db -- psql -U auth -d authsvc -c '\l'
```

### Check RabbitMQ

```bash
# Check RabbitMQ pod
kubectl get pods -l app=rabbitmq

# Access RabbitMQ management UI
kubectl port-forward service/rabbitmq 15672:15672

# Open browser: http://localhost:15672
# Login: guest / guest
```

### Check Envoy Gateway

```bash
# Check Envoy pod
kubectl get pods -l app=envoy

# Test gateway endpoint
kubectl port-forward service/envoy 8080:8080

# In another terminal:
curl http://localhost:8080/health

# Access Envoy admin
kubectl port-forward service/envoy 9901:9901
# Open: http://localhost:9901
```

---

## Environment Variables

Your application services connect to infrastructure via these environment variables:

### User Service → auth-db
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://auth-db:5432/authsvc
SPRING_DATASOURCE_USERNAME: auth
SPRING_DATASOURCE_PASSWORD: authpw
```

### Auction Service → auction-db + RabbitMQ
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://auction-db:5432/auction_db
RABBITMQ_URL: amqp://guest:guest@rabbitmq:5672
```

### Payment Service → wallet-db + RabbitMQ
```yaml
PGHOST: wallet-db
PGPORT: "5432"
RABBITMQ_URL: amqp://guest:guest@rabbitmq:5672
```

### Notification Service → RabbitMQ
```yaml
RABBITMQ_URL: amqp://guest:guest@rabbitmq:5672
```

**These are already configured in your k8s/*.yaml files!**

---

## Persistent Storage

### Database Volumes

Each database has a PersistentVolumeClaim:

```yaml
# Example from auth-db.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: auth-db-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

**What this means:**
- Data persists even if pods restart
- 1GB storage per database
- Can increase storage by editing the yaml

### Checking Volumes

```bash
# List persistent volumes
kubectl get pv

# List persistent volume claims
kubectl get pvc

# Check storage usage
kubectl exec -it deployment/auth-db -- df -h /var/lib/postgresql/data
```

---

## Scaling Infrastructure

### Scale Databases

**⚠️ Warning:** PostgreSQL typically runs as single instance with persistent storage.

For read replicas (advanced):
```bash
# Not recommended without proper replication setup
# kubectl scale deployment auth-db --replicas=2
```

### Scale RabbitMQ

**For production**, consider RabbitMQ cluster:
```bash
# Basic scaling (single node)
kubectl scale deployment rabbitmq --replicas=1

# For true clustering, use RabbitMQ Operator
# https://www.rabbitmq.com/kubernetes/operator/operator-overview.html
```

### Scale Envoy

Envoy is stateless and can scale easily:
```bash
# Scale to 3 replicas for high availability
kubectl scale deployment envoy --replicas=3

# Verify
kubectl get pods -l app=envoy
```

---

## Common Issues

### Issue: Database pods stuck in Pending

```bash
# Check PVC status
kubectl get pvc

# If no storage class available:
kubectl get storageclass

# Create default storage class or specify one in PVC
```

### Issue: RabbitMQ connection refused

```bash
# Check RabbitMQ logs
kubectl logs deployment/rabbitmq

# Wait for RabbitMQ to fully start (~30 seconds)
# Check if service is running
kubectl get svc rabbitmq
```

### Issue: Envoy can't reach services

```bash
# Verify all services exist
kubectl get svc

# Check Envoy logs
kubectl logs deployment/envoy

# Verify ConfigMap is loaded
kubectl get configmap envoy-config
kubectl describe configmap envoy-config
```

### Issue: Services can't connect to database

```bash
# Check database is running
kubectl get pods | grep db

# Test database connection from service pod
kubectl exec -it deployment/user-service -- bash
# Inside pod:
apt-get update && apt-get install -y postgresql-client
psql -h auth-db -U auth -d authsvc
# If connection works, issue is with app configuration
```

---

## Updating Infrastructure

### Updating PostgreSQL Version

```bash
# Edit k8s/auth-db.yaml
# Change: image: postgres:15
# To:     image: postgres:16

# Apply changes
kubectl apply -f k8s/auth-db.yaml

# Kubernetes will rolling update
kubectl rollout status deployment/auth-db
```

### Updating RabbitMQ Version

```bash
# Edit k8s/rabbitmq.yaml
# Change: image: rabbitmq:3-management
# To:     image: rabbitmq:3.12-management

kubectl apply -f k8s/rabbitmq.yaml
```

### Updating Envoy Version

```bash
# Edit k8s/envoy.yaml
# Change: image: envoyproxy/envoy:v1.27-latest
# To:     image: envoyproxy/envoy:v1.28-latest

kubectl apply -f k8s/envoy.yaml
```

---

## Summary

### ✅ What You DON'T Need to Push to Docker Hub

- ✅ PostgreSQL (`postgres:15`) - Already on Docker Hub
- ✅ RabbitMQ (`rabbitmq:3-management`) - Already on Docker Hub
- ✅ Envoy (`envoyproxy/envoy:v1.27-latest`) - Already on Docker Hub
- ✅ OpenSearch images - Already on Docker Hub (if you add them)

### 🔧 What You DO Need to Push to Docker Hub

- 🔧 User Service
- 🔧 Auction Service
- 🔧 Payment Service
- 🔧 Notification Service
- 🔧 Analytics Service
- 🔧 Frontend

### 📝 Complete Deployment Command

```bash
# 1. Build and push YOUR services
./docker-push.sh

# 2. Update K8s manifests
./update-k8s-images.sh

# 3. Deploy EVERYTHING (infra + apps)
kubectl apply -f k8s/

# That's it! Kubernetes handles the rest.
```

---

## Next Steps

1. ✅ Run `./docker-push.sh` to push your application services
2. ✅ Run `./update-k8s-images.sh` to update manifests
3. ✅ Run `kubectl apply -f k8s/` to deploy everything
4. ✅ Watch pods start: `kubectl get pods -w`
5. ✅ Verify services: `kubectl get svc`
6. ✅ Test gateway: `kubectl port-forward service/envoy 8080:8080`

All infrastructure images are automatically pulled by Kubernetes! 🎉
