# Quick Docker Hub Deployment Reference

## 🚀 Quick Start (3 Commands)

```bash
# 1. Push YOUR application images to Docker Hub
./docker-push.sh

# 2. Update Kubernetes manifests with your images
./update-k8s-images.sh

# 3. Deploy EVERYTHING (infra + apps) to Kubernetes
kubectl apply -f k8s/
```

**Note:** Infrastructure services (PostgreSQL, RabbitMQ, Envoy) use public images and are automatically pulled by Kubernetes. You only need to push your custom application services!

---

## 📦 Images That Will Be Created

### Your Custom Images (Pushed to Docker Hub)
- `your-username/ebaazee-user-service:latest`
- `your-username/ebaazee-auction-service:latest`
- `your-username/ebaazee-payment-service:latest`
- `your-username/ebaazee-notification-service:latest`
- `your-username/ebaazee-analytics-service:latest`
- `your-username/ebaazee-frontend:latest`

### Infrastructure Images (Already Public on Docker Hub)
- `postgres:15` (used for 3 databases: auth-db, auction-db, wallet-db)
- `rabbitmq:3-management` (message broker)
- `envoyproxy/envoy:v1.27-latest` (API gateway)

✅ **Infrastructure images are automatically pulled by Kubernetes - no action needed!**

---

## 🔑 Before You Start

1. Create a Docker Hub account at [hub.docker.com](https://hub.docker.com)
2. Make sure Docker Desktop is running
3. Have `kubectl` configured for your cluster

---

## 📝 Script Usage

### docker-push.sh
```bash
# Interactive mode (will prompt for username)
./docker-push.sh

# With environment variables
export DOCKER_USERNAME=your-username
export IMAGE_TAG=v1.0.0
./docker-push.sh
```

### update-k8s-images.sh
```bash
# Interactive mode
./update-k8s-images.sh

# With environment variable
export DOCKER_USERNAME=your-username
./update-k8s-images.sh
```

---

## 🔍 Verify Deployment

```bash
# Check pods
kubectl get pods

# Check services
kubectl get svc

# Watch pods starting
kubectl get pods -w

# View logs
kubectl logs -f deployment/user-service
```

---

## 🛠️ Quick Fixes

| Problem | Solution |
|---------|----------|
| Docker push fails | `docker logout && docker login` |
| Pod not starting | `kubectl describe pod <pod-name>` |
| Image pull errors | Make repos public on Docker Hub |

---

## 📚 Full Documentation

- **[DOCKER_HUB_SETUP.md](DOCKER_HUB_SETUP.md)** - Complete guide with detailed troubleshooting
- **[docs/INFRASTRUCTURE_SERVICES.md](docs/INFRASTRUCTURE_SERVICES.md)** - Infrastructure setup (databases, RabbitMQ, Envoy)
- **[docs/DOCKER_HUB_DEPLOYMENT.md](docs/DOCKER_HUB_DEPLOYMENT.md)** - Technical deep dive with CI/CD examples
