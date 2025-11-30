# Docker Hub Deployment Guide

This guide explains how to build, push, and deploy Ebaazee microservices to Docker Hub and Kubernetes.

## Prerequisites

1. **Docker Desktop** installed and running
2. **Docker Hub account** - Create one at [hub.docker.com](https://hub.docker.com)
3. **kubectl** configured for your Kubernetes cluster
4. **Git** (for version control)

---

## Step 1: Push Images to Docker Hub

### Option A: Automated Script (Recommended)

We've provided a script that builds and pushes all services automatically.

```bash
# Make the script executable
chmod +x docker-push.sh

# Run the script (it will prompt for your Docker Hub username)
./docker-push.sh

# Or set username as environment variable
export DOCKER_USERNAME=your-dockerhub-username
./docker-push.sh

# To use a specific tag (default is 'latest')
export IMAGE_TAG=v1.0.0
./docker-push.sh
```

### Option B: Manual Build and Push

If you prefer to build and push services individually:

```bash
# Login to Docker Hub
docker login

# Set your Docker Hub username
DOCKER_USERNAME=your-dockerhub-username
IMAGE_TAG=latest

# Build and push User Service
docker build -t $DOCKER_USERNAME/ebaazee-user-service:$IMAGE_TAG ./services/user-service
docker push $DOCKER_USERNAME/ebaazee-user-service:$IMAGE_TAG

# Build and push Auction Service
docker build -t $DOCKER_USERNAME/ebaazee-auction-service:$IMAGE_TAG ./services/auction-service
docker push $DOCKER_USERNAME/ebaazee-auction-service:$IMAGE_TAG

# Build and push Payment Service
docker build -t $DOCKER_USERNAME/ebaazee-payment-service:$IMAGE_TAG ./services/payment-service
docker push $DOCKER_USERNAME/ebaazee-payment-service:$IMAGE_TAG

# Build and push Notification Service
docker build -t $DOCKER_USERNAME/ebaazee-notification-service:$IMAGE_TAG ./services/notification-service
docker push $DOCKER_USERNAME/ebaazee-notification-service:$IMAGE_TAG

# Build and push Analytics Service
docker build -t $DOCKER_USERNAME/ebaazee-analytics-service:$IMAGE_TAG ./services/analytics-service
docker push $DOCKER_USERNAME/ebaazee-analytics-service:$IMAGE_TAG

# Build and push Frontend
docker build -t $DOCKER_USERNAME/ebaazee-frontend:$IMAGE_TAG ./ebaazee-frontend
docker push $DOCKER_USERNAME/ebaazee-frontend:$IMAGE_TAG
```

---

## Step 2: Update Kubernetes Manifests

### Option A: Automated Update Script

```bash
# Make the script executable
chmod +x update-k8s-images.sh

# Run the script (it will prompt for your Docker Hub username)
./update-k8s-images.sh

# Or set username as environment variable
export DOCKER_USERNAME=your-dockerhub-username
./update-k8s-images.sh
```

### Option B: Manual Update

Edit each Kubernetes manifest file in the `k8s/` directory and update the `image:` field:

```yaml
# Before
image: apibp-20242yb-team-01-user-service:dev

# After
image: your-dockerhub-username/ebaazee-user-service:latest
```

Update these files:
- `k8s/user-service.yaml`
- `k8s/auction-service.yaml`
- `k8s/payment-service.yaml`
- `k8s/notification-service.yaml`
- `k8s/analytics-service.yaml`

---

## Step 3: Deploy to Kubernetes

### Deploy All Services

This command deploys **both infrastructure and application services**:

```bash
# Apply all Kubernetes manifests (databases + apps + gateway)
kubectl apply -f k8s/

# This deploys:
# - Infrastructure: PostgreSQL (x3), RabbitMQ, Envoy
# - Your apps: User, Auction, Payment, Notification, Analytics services

# Verify deployments
kubectl get deployments
kubectl get pods
kubectl get services
```

**Note:** Infrastructure services (PostgreSQL, RabbitMQ, Envoy) use **public Docker images** from Docker Hub. Kubernetes automatically pulls these. You only needed to push your **custom application services**.

### Deploy Individual Services

```bash
# Deploy databases
kubectl apply -f k8s/auth-db.yaml
kubectl apply -f k8s/auction-db.yaml
kubectl apply -f k8s/wallet-db.yaml

# Deploy RabbitMQ
kubectl apply -f k8s/rabbitmq.yaml

# Deploy microservices
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/auction-service.yaml
kubectl apply -f k8s/payment-service.yaml
kubectl apply -f k8s/notification-service.yaml
kubectl apply -f k8s/analytics-service.yaml

# Deploy API Gateway
kubectl apply -f k8s/envoy.yaml
```

### Monitor Deployment

```bash
# Watch pods starting up
kubectl get pods -w

# Check logs for a specific service
kubectl logs -f deployment/user-service

# Check service endpoints
kubectl get svc

# Describe a pod for troubleshooting
kubectl describe pod <pod-name>
```

---

## Step 4: Access the Application

### Port Forwarding (for local testing)

```bash
# Forward API Gateway port
kubectl port-forward service/envoy 8080:8080

# Access the application
open http://localhost:8080
```

### LoadBalancer/NodePort (for production)

Update service types in Kubernetes manifests:

```yaml
spec:
  type: LoadBalancer  # or NodePort
  ports:
    - port: 8080
      targetPort: 8080
```

---

## Image List

After running the push script, your images will be available at:

- `your-dockerhub-username/ebaazee-user-service:latest`
- `your-dockerhub-username/ebaazee-auction-service:latest`
- `your-dockerhub-username/ebaazee-payment-service:latest`
- `your-dockerhub-username/ebaazee-notification-service:latest`
- `your-dockerhub-username/ebaazee-analytics-service:latest`
- `your-dockerhub-username/ebaazee-frontend:latest`

---

## Troubleshooting

### Docker Build Fails

```bash
# Check Docker daemon is running
docker info

# Check available disk space
docker system df

# Clean up unused images
docker system prune -a
```

### Docker Push Fails

```bash
# Verify you're logged in
docker login

# Check credentials
cat ~/.docker/config.json

# Try logging out and back in
docker logout
docker login
```

### Kubernetes Pods Not Starting

```bash
# Check pod status
kubectl get pods

# View pod logs
kubectl logs <pod-name>

# Describe pod for events
kubectl describe pod <pod-name>

# Check image pull status
kubectl get events --sort-by=.metadata.creationTimestamp
```

### Image Pull Error in Kubernetes

```bash
# Make sure images are public in Docker Hub
# Or create an image pull secret for private repos

kubectl create secret docker-registry dockerhub-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=your-username \
  --docker-password=your-password \
  --docker-email=your-email

# Reference the secret in your deployment
spec:
  imagePullSecrets:
    - name: dockerhub-secret
```

---

## Environment Variables

Make sure to configure these environment variables in your Kubernetes manifests:

### User Service
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `OPENSEARCH_HOST`

### Auction Service
- `SPRING_DATASOURCE_URL`
- `RABBITMQ_URL`
- `OPENSEARCH_HOST`

### Payment Service
- `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`
- `RABBITMQ_URL`
- `GATEWAY_BASE_URL`

### Notification Service
- `RABBITMQ_URL`
- `SMTP_HOST`, `SMTP_PORT`
- `SMTP_USERNAME`, `SMTP_PASSWORD`

---

## CI/CD Integration

You can integrate the docker-push.sh script into your CI/CD pipeline:

### GitHub Actions Example

```yaml
name: Build and Push to Docker Hub

on:
  push:
    branches: [ main ]

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Login to Docker Hub
        uses: docker/login-action@v1
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Build and Push Images
        env:
          DOCKER_USERNAME: ${{ secrets.DOCKER_USERNAME }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          chmod +x docker-push.sh
          ./docker-push.sh
```

---

## Updating Images

When you make changes and want to update the deployed images:

```bash
# 1. Build and push new images with a new tag
export IMAGE_TAG=v1.1.0
./docker-push.sh

# 2. Update Kubernetes manifests
./update-k8s-images.sh

# 3. Apply changes to Kubernetes
kubectl apply -f k8s/

# 4. Or do a rolling restart
kubectl rollout restart deployment/user-service
kubectl rollout restart deployment/auction-service
# ... etc for other services
```

---

## Best Practices

1. **Use Semantic Versioning**: Tag images with version numbers (v1.0.0, v1.1.0, etc.)
2. **Never use :latest in production**: Always use specific version tags
3. **Keep Images Small**: Use multi-stage builds (already implemented in Dockerfiles)
4. **Security Scan**: Run `docker scan` on images before pushing
5. **Resource Limits**: Set CPU and memory limits in Kubernetes manifests
6. **Health Checks**: Configure liveness and readiness probes
7. **Secrets Management**: Use Kubernetes secrets for sensitive data

---

## Support

For issues or questions:
- Check service-specific docs in `services/*/INFO.MD`
- Review [docs/ARCHITECTURE_AND_SYSTEM.md](docs/ARCHITECTURE_AND_SYSTEM.md)
- Check Docker Hub repository for image availability
- Review Kubernetes events: `kubectl get events`

---

## License

MIT License - See [LICENSE](LICENSE) file for details.
