# 🚀 Docker Hub Deployment Guide

Complete guide for building, pushing, and deploying Ebaazee microservices to Docker Hub and Kubernetes.

---

## 📋 Quick Overview

**What you'll do:**
1. Build 6 custom application Docker images
2. Push them to your Docker Hub account
3. Deploy everything (apps + infrastructure) to Kubernetes

**What's automated:**
- Infrastructure images (PostgreSQL, RabbitMQ, Envoy) use public images - Kubernetes pulls them automatically
- Scripts handle building and pushing your custom services
- All configuration is pre-set in Kubernetes manifests

---

## 🎯 3-Step Deployment

```bash
# Step 1: Build and push your application images to Docker Hub
./docker-push.sh

# Step 2: Update Kubernetes manifests with your Docker Hub username
./update-k8s-images.sh

# Step 3: Deploy everything to Kubernetes
kubectl apply -f k8s/
```

**That's it!** See sections below for details.

---

## � What Gets Deployed

### Your Custom Services (You build & push these)
| Service | Image | Size | Tech |
|---------|-------|------|------|
| User Service | `{user}/ebaazee-user-service` | ~350MB | Java 21, Spring Boot |
| Auction Service | `{user}/ebaazee-auction-service` | ~350MB | Java 21, Spring Boot |
| Payment Service | `{user}/ebaazee-payment-service` | ~200MB | Node.js, TypeScript |
| Notification Service | `{user}/ebaazee-notification-service` | ~20MB | Go |
| Analytics Service | `{user}/ebaazee-analytics-service` | ~350MB | Java 21, Spring Boot |
| Frontend | `{user}/ebaazee-frontend` | ~150MB | React, Vite |

**Total:** ~1.4 GB

### Infrastructure Services (Auto-pulled from Docker Hub)
| Component | Public Image | Purpose |
|-----------|-------------|---------|
| PostgreSQL (x3) | `postgres:15` | auth-db, auction-db, wallet-db |
| RabbitMQ | `rabbitmq:3-management` | Message broker |
| Envoy | `envoyproxy/envoy:v1.27-latest` | API Gateway |

✅ **No action needed for infrastructure - Kubernetes handles it!**

---

## 🚀 Detailed Steps

### Prerequisites

```bash
# 1. Docker Desktop running
docker info

# 2. Docker Hub account created at hub.docker.com

# 3. Login to Docker Hub
docker login
```

### Step 1: Build & Push Images

```bash
./docker-push.sh
```

The script will prompt for your Docker Hub username and then:
- Build all 6 application images
- Push them to Docker Hub
- Show progress and results (takes 10-20 minutes)

**Optional:** Set variables to skip prompts
```bash
export DOCKER_USERNAME=your-username
export IMAGE_TAG=v1.0.0  # defaults to 'latest'
./docker-push.sh
```

### Step 2: Update Kubernetes Manifests

```bash
./update-k8s-images.sh
```

Updates all `k8s/*.yaml` files with your Docker Hub images and creates backup files.

### Step 3: Deploy to Kubernetes

**Prerequisites before deploying:**
- ✅ Images pushed to Docker Hub (Step 1 completed)
- ✅ Kubernetes cluster accessible (`kubectl cluster-info` works)
- ✅ Using correct context (e.g., `docker-desktop`)

```bash
# Verify cluster is accessible
kubectl cluster-info

# Deploy everything
kubectl apply -f k8s/

# Watch pods start (wait 2-5 minutes)
kubectl get pods -w  # Press Ctrl+C when all are "Running"
```

This deploys both your custom services AND infrastructure (databases, RabbitMQ, Envoy).

**Expected outcome:**
- Infrastructure pods (databases, RabbitMQ): Running immediately
- App pods: Will pull images from Docker Hub, then start running
| RabbitMQ | `rabbitmq:3-management` | Message broker | `k8s/rabbitmq.yaml` |
| Envoy | `envoyproxy/envoy:v1.27-latest` | API Gateway | `k8s/envoy.yaml` |
| OpenSearch (planned) | `opensearchproject/opensearch:latest` | Logging (if deploying) | Not in k8s/ yet |

**✅ These are automatically pulled by Kubernetes from Docker Hub - no action needed!**

---

## 🎯 Environment Variables for Scripts

### Option 1: Interactive (Recommended for first-time)
Just run the scripts and they will prompt you:
```bash
./docker-push.sh
# Will ask: "Please enter your Docker Hub username:"
```

### Option 2: Export Variables
Set once and run multiple times:
```bash
export DOCKER_USERNAME=your-dockerhub-username
export IMAGE_TAG=latest

./docker-push.sh
./update-k8s-images.sh
```

### Option 3: Inline
Provide variables directly:
```bash
DOCKER_USERNAME=myusername IMAGE_TAG=v1.0.0 ./docker-push.sh
```

---

## ✅ Verification

### Check Docker Hub
Visit https://hub.docker.com → Login → Verify 6 repositories created

### Check Kubernetes
```bash
kubectl get pods              # All should be "Running"
kubectl get svc               # Verify services
kubectl logs -f deployment/user-service  # Check logs

# Test API Gateway
kubectl port-forward service/envoy 8080:8080
curl http://localhost:8080
```

---

## 🐛 Troubleshooting

### Cluster Not Accessible
```bash
# If you see "no such host" errors with kubectl:
# 1. Check current context
kubectl config current-context

# 2. Switch to Docker Desktop Kubernetes (recommended)
kubectl config use-context docker-desktop

# 3. Or enable Kubernetes in Docker Desktop:
# Docker Desktop → Settings → Kubernetes → Enable Kubernetes
```

### Docker login fails
```bash
docker logout && docker login
```

### Build fails
```bash
# Build service individually to see full error
cd services/user-service && docker build -t test .
```

### Pods stuck in "ImagePullBackOff"
This means Kubernetes can't pull your images. **You must run ./docker-push.sh first!**

```bash
# Check if images were pushed
kubectl describe pod <pod-name> | grep -A 5 "Events:"

# Solution: Push images to Docker Hub
./docker-push.sh

# After images are pushed, pods will automatically recover
# Or force restart:
kubectl rollout restart deployment/user-service
```

Make repositories public OR create image pull secret:
```bash
kubectl create secret docker-registry dockerhub-secret \
  --docker-username=USER --docker-password=PASS --docker-email=EMAIL
```

### Pods in "CrashLoopBackOff"
```bash
kubectl logs <pod-name>           # Check error logs
kubectl describe pod <pod-name>   # Check events
# Verify: databases running, environment variables set correctly
```

---

## 🔄 Updating After Code Changes

```bash
export IMAGE_TAG=v1.1.0        # Use new version
./docker-push.sh               # Rebuild and push
./update-k8s-images.sh         # Update manifests
kubectl apply -f k8s/          # Deploy updates
```

---

## 📚 Related Documentation

- **[Quick Reference](DOCKER_DEPLOYMENT_QUICKSTART.md)** - Cheat sheet
- **[Infrastructure Services](docs/INFRASTRUCTURE_SERVICES.md)** - Database, RabbitMQ, Envoy details
- **[Full Deployment Guide](docs/DOCKER_HUB_DEPLOYMENT.md)** - Comprehensive technical guide
- **[Architecture](docs/ARCHITECTURE_AND_SYSTEM.md)** - System design

---

## 📞 Support

**Need help?**
1. Check troubleshooting section above
2. Review `kubectl logs <pod-name>`
3. Check `kubectl get events`
4. See detailed docs in `docs/` folder

---

**🚀 That's it! You're ready to deploy to Docker Hub and Kubernetes.**
