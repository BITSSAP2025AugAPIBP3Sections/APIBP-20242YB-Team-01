# Project Overview

This document provides a concise reference for contributors, maintainers, and users of the Ebaazee microservices-based auction platform. For full details, see the main `README.md`.

---

## System Overview
- **Microservices:** User, Auction, Payment, Notification, Analytics
- **API Gateway:** Envoy Proxy (intelligent routing, circuit breaking)
- **Databases:** 3x PostgreSQL (per service)
- **Messaging:** RabbitMQ (event-driven)
- **Logging:** OpenSearch cluster, Fluent Bit, OpenSearch Dashboards
- **Languages:** Java (Spring Boot), Node.js (TypeScript), Go

---

## Quick Start Reference

### Docker Compose
- `git clone https://github.com/BITSSAP2025AugAPIBP3Sections/APIBP-20242YB-Team-01.git` — Clone the repository
- `cd APIBP-20242YB-Team-01` — Enter the project directory
- `./run-all.sh` — Start all services
- `docker compose logs -f` — View logs
- `docker compose down` — Stop all services


### Kubernetes (kind)

#### Prerequisites
- Docker Desktop running
- [kind](https://kind.sigs.k8s.io/) and [kubectl](https://kubernetes.io/docs/tasks/tools/) installed (`kind --version`, `kubectl version --client`)
- At least 8GB RAM (16GB recommended)

#### Step-by-Step Workflow

```bash
# 1. Start a kind cluster
kind create cluster

# 2. Build and load all service images into kind
./k8s-build-and-load.sh

# 3. Create the Envoy config ConfigMap (from your project root)
kubectl create configmap envoy-config --from-file=envoy.yaml=$(pwd)/gateway/envoy/envoy.yaml

# 4. Apply all Kubernetes manifests
kubectl apply -f k8s/

# 5. Check if all pods are running
kubectl get pods

# 6. If any pods are not running, check logs:
kubectl logs <pod-name>  # See why a pod is failing
kubectl describe pod <pod-name>  # Get detailed status and events

# 7. Port-forward Envoy service to localhost (API Gateway)
kubectl port-forward service/envoy 8080:8080
# Now access your app at http://localhost:8080

# 8. (Optional) Clean up all resources and the cluster
kubectl delete -f k8s/
kubectl delete configmap envoy-config
kind delete cluster
```

#### Troubleshooting
- **ImagePullBackOff:** Make sure you ran `./k8s-build-and-load.sh` after creating the kind cluster.
- **CrashLoopBackOff:** Check pod logs for stack traces or configuration errors.
- **Port already in use:** Change the port in the port-forward command or stop the process using it (`lsof -i :8080`).
- **ConfigMap not found:** Ensure you created the Envoy config ConfigMap before applying manifests.
- **Pods stuck in Pending:** Check `kubectl describe pod <pod-name>` for resource or scheduling issues.

For more advanced usage, see the [ARCHITECTURE_AND_SYSTEM.md](ARCHITECTURE_AND_SYSTEM.md) and service-specific docs.

---

## Service Ports
| Service              | Port  |
|----------------------|-------|
| User Service         | 8081  |
| Auction Service      | 8082  |
| Payment Service      | 8086  |
| Notification Service | 8083  |
| Analytics Service    | 8085  |
| Envoy Gateway        | 8080  |
| RabbitMQ Mgmt        | 15672 |
| OpenSearch Dashboards| 5601  |

---

## API Docs
- Swagger UI: `/swagger-ui.html` on each service port
- OpenAPI specs: see `api-specs/` directory

---

## Troubleshooting
- **Check logs:** `docker compose logs <service>`
- **Restart service:** `docker compose restart <service>`
- **Check DB:** `docker compose ps auth-db auction-db wallet-db`
- **RabbitMQ UI:** `http://localhost:15672` (guest/guest)
- **OpenSearch Dashboards:** `http://localhost:5601`

---

## Security Checklist (Production)
- Change all default secrets/passwords
- Enable HTTPS/TLS for all endpoints
- Use environment variables for secrets
- Enable rate limiting, CORS, and request validation
- Run containers as non-root
- Regularly update dependencies

---

## Contribution Guide
- Fork, branch, code, test, PR
- Follow code style guides (see main README)
- Use Conventional Commits for messages
- Minimum 70% test coverage

---

## More Docs
- See service-specific `INFO.MD` in each service folder
- See `gateway/api-gateway/API-GATEWAY.MD` for gateway config
- See `api-specs/` for OpenAPI specs

---

For full details, see the main [README.md](../README.md).
