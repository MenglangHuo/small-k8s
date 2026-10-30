# 🎫 Mono-Ticket — Kubernetes Deployment

> Spring Boot WebFlux API + Vue.js SPA + MongoDB → K3s on AWS EC2

## Architecture

```
Internet → Nginx Ingress (80/443) → ┬─ yourdomain.com     → ticket-admin (Vue SPA + Nginx proxy)
                                     └─ api.yourdomain.com → ticket-api (Spring Boot WebFlux)
                                                                    ↓
                                                              mongo-svc (MongoDB 7.0)
```

All running on a single **AWS EC2 t3.micro** (1 vCPU, 1GB RAM + 2GB Swap) using **K3s**.

---

## 📚 Documentation

| Document | Description |
|:---|:---|
| [00_k8s_architecture.md](docs/00_k8s_architecture.md) | Deep dive: Cluster, node, pod, services, and networking architecture |
| [01_requirements.md](docs/01_requirements.md) | Prerequisites, specs, and resource budget |
| [02_infrastructure_setup.md](docs/02_infrastructure_setup.md) | EC2 launch, swap, K3s, Nginx Ingress, DNS |
| [03_deployment_guide.md](docs/03_deployment_guide.md) | Build images, deploy manifests, verify |
| [04_troubleshooting.md](docs/04_troubleshooting.md) | When things go wrong |
| [k8s_guideline.md](k8s/k8s_guideline.md) | K8s concepts, docker-compose comparison |

---

## 🚀 Quick Start

```bash
# 1. Build and push images (on your local machine)
docker build --platform linux/amd64 -t yourdockeruser/ticket-api:latest ./ticket
docker build --platform linux/amd64 -t yourdockeruser/ticket-admin:latest ./ticket-admin
docker push yourdockeruser/ticket-api:latest
docker push yourdockeruser/ticket-admin:latest

# 2. Edit secrets (replace CHANGE_ME values)
vim k8s/02-secrets.yaml

# 3. Deploy to K3s (on EC2 server)
kubectl apply -f k8s/

# 4. Check status
kubectl get pods -n ticket-prod
```

---

## 📁 K8s Manifests

```
k8s/
├── 00-namespace.yaml        # ticket-prod namespace
├── 01-configmap.yaml        # Non-sensitive env vars
├── 02-secrets.example.yaml  # Credentials template (copy to 02-secrets.yaml)
├── 03-mongodb.yaml          # MongoDB: PVC + Deployment + Service + Init
├── 04-ticket-api.yaml       # Spring Boot API: Deployment + Service
├── 05-ticket-admin.yaml     # Vue.js SPA: Nginx ConfigMap + Deployment + Service
├── 06-ingress.yaml          # Domain routing + Traefik HTTPS redirect
├── 07-cluster-issuer.yaml   # Let's Encrypt auto-SSL (Traefik ACME)
├── 08-mongodb-network-policy.yaml # Zero-trust DB NetworkPolicy
├── deploy.sh                # Production deployment automation CLI script
└── k8s_guideline.md         # Understanding the K8s setup
```

---

## 🔄 Deploying Updates

```bash
# Rebuild, push, then:
kubectl rollout restart deployment/ticket-api -n ticket-prod
kubectl rollout restart deployment/ticket-admin -n ticket-prod
```

---

## 🗂️ Project Structure

```
mono-ticket/
├── ticket/                  # Spring Boot WebFlux API (Java 25, Gradle)
│   ├── Dockerfile           # Multi-stage build → eclipse-temurin:21-jre-alpine
│   ├── docker-compose.yml   # Local dev compose (MongoDB + API)
│   └── src/                 # Java source code
├── ticket-admin/            # Vue.js 3 SPA (Vite + Tailwind + Pinia)
│   ├── Dockerfile           # Multi-stage build → nginx:1.27-alpine
│   └── src/                 # Vue source code
├── k8s/                     # Kubernetes manifests
├── docs/                    # Deployment documentation
└── README.md                # ← You are here
```
