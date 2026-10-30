# End-to-End Production Deployment Guide: Spring Boot WebFlux + Vue.js SPA + MongoDB on AWS Free Tier Kubernetes (K3s)

> **Master Architecture & Deployment Guide**
> 
> This document has been modernized and split into modular, focused guides for ease of maintenance:
> - 🏛️ [00_k8s_architecture.md](./00_k8s_architecture.md) — Comprehensive deep dive into cluster, node, pod, service, namespace, and container interactions.
> - 📋 [01_requirements.md](./01_requirements.md) — Pre-flight prerequisites, AWS specifications, and memory budgets.
> - 🏗️ [02_infrastructure_setup.md](./02_infrastructure_setup.md) — Step-by-step EC2 launch, Swap allocation, K3s installation, Nginx Ingress, and DNS setup.
> - 🚀 [03_deployment_guide.md](./03_deployment_guide.md) — Docker build workflows, sequential K8s manifest application, and zero-downtime updates.
> - 🛠️ [04_troubleshooting.md](./04_troubleshooting.md) — Incident triage, JVM tuning on 1 GB nodes, SSL debugging, and reset options.
> - ⎈ [k8s_guideline.md](../k8s/k8s_guideline.md) — Docker Compose to Kubernetes translation matrix and architecture guide.

---

## 1. System Architecture Overview

The system runs on a single AWS EC2 instance (`t3.micro` or `t2.micro`, 1 vCPU, 1 GB RAM + 2 GB Swap file, 30 GB gp3 EBS) using **K3s** (lightweight Kubernetes). Ingress traffic is managed by K3s native **Traefik Ingress Controller** directly bound to host ports 80/443 (avoiding expensive AWS Classic/Network Load Balancers).

```text
Internet Users / External Clients
   │
   ├── https://yourdomain.com ──────┐
   ├── https://www.yourdomain.com ──┤
   └── https://api.yourdomain.com ──┤
                                    ▼
                         [ AWS Security Group ]
                         (Inbound: Ports 80, 443, 22)
                                    │
                                    ▼
┌─── AWS EC2 Instance (Ubuntu 24.04 LTS, t3.micro, 30GB gp3 EBS) ─────────────────────┐
│                                                                                     │
│  [ Linux OS Kernel + 2GB Swap File ]                                                │
│                                                                                     │
│  ┌─── K3s Single-Node Kubernetes Cluster ────────────────────────────────────────┐  │
│  │                                                                               │  │
│  │  [ K3s Native Traefik Ingress Controller (HostPort 80/443, SSL Termination) ] │  │
│  │                                                                               │  │
│  │  [ Namespace: cert-manager ]                                                  │  │
│  │   └── Cert-Manager (Let's Encrypt automated ACME HTTP-01 challenge)           │  │
│  │                                                                               │  │
│  │  [ Namespace: ticket-prod ]                                                   │  │
│  │   │                                                                           │  │
│  │   ├── Service: ticket-admin-svc (ClusterIP :80)                                │  │
│  │   │    └── Pod: ticket-admin (Vue.js 3 SPA on Nginx, Reverse Proxy)           │  │
│  │   │         ├── Serves HTML/JS/CSS assets with caching headers                │  │
│  │   │         └── Internal Proxy (/api/*, /token, /login) ─────────────────┐    │  │
│  │   │                                                                      │    │  │
│  │   ├── Service: ticket-api-svc (ClusterIP :8000) ◄────────────────────────┘    │  │
│  │   │    └── Pod: ticket-api (Spring Boot 4.1 WebFlux, Java 25, SerialGC)       │  │
│  │   │         ├── Capped Heap: -Xmx256m -Xms128m -XX:MaxMetaspaceSize=96m       │  │
│  │   │         └── Healthcheck Probes: /actuator/health                          │  │
│  │   │                                                                           │  │
│  │   ├── Service: mongo-svc (ClusterIP :27017)                                   │  │
│  │   │    └── Pod: mongo-db (MongoDB 7.0 Reactive Engine)                        │  │
│  │   │         └── PersistentVolumeClaim: mongo-pvc (5Gi local-path on EBS)      │  │
│  │   │                                                                           │  │
│  │   └── ConfigMaps & Secrets (AWS S3 credentials, Telegram tokens, DB URI)     │  │
│  └───────────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technical Stack Specifications

| Component | Technology | Role | Port | Container Image Base |
| :--- | :--- | :--- | :--- | :--- |
| **API Backend** | Spring Boot 4.1, Spring WebFlux | Reactive REST API, S3 file manager, Telegram alerts | `8000` | `eclipse-temurin:21-jre-alpine` (Layered JAR) |
| **Admin Frontend** | Vue.js 3, Vite, Tailwind CSS 4, Pinia | Ticket dashboard & client ingestion portal | `80` | `nginx:1.27-alpine` |
| **Database** | MongoDB 7.0 Community | Non-relational reactive document store | `27017` | `mongo:7.0` |
| **Storage** | K3s Local-Path Provisioner | 5 GiB persistent storage bound to EC2 gp3 EBS | N/A | Host filesystem mount |
| **Ingress & SSL** | K3s Traefik + Cert-Manager | HostPort proxy & automated Let's Encrypt TLS renewal | `80`, `443` | Native K3s Traefik + Cert-Manager manifest |

---

## 3. Step-by-Step Deployment Roadmap

### Phase 1: Local Docker Image Preparation
> **Crucial Rule:** Never run `gradle bootJar` or `pnpm build` on the 1 GB EC2 server directly. It will freeze the node and trigger kernel OOM panics. Always build locally or via CI/CD.

1. **Spring Boot Reactive API (`ticket/Dockerfile`)**:
   ```bash
   cd ticket
   docker build --platform linux/amd64 -t yourdockeruser/ticket-api:latest .
   docker push yourdockeruser/ticket-api:latest
   ```
2. **Vue.js 3 Admin Frontend (`ticket-admin/Dockerfile`)**:
   ```bash
   cd ../ticket-admin
   docker build --platform linux/amd64 \
     --build-arg VITE_ENABLE_MOCK_DATA=false \
     -t yourdockeruser/ticket-admin:latest .
   docker push yourdockeruser/ticket-admin:latest
   ```

---

### Phase 2: EC2 Host Setup & 2GB Swap Allocation
1. Launch Ubuntu 24.04 LTS on `t3.micro` with a **30 GiB gp3 root disk**.
2. Open ports `22` (SSH), `80` (HTTP), and `443` (HTTPS) in Security Group `k3s-web-sg`.
3. Associate an **Elastic IP** to maintain a persistent address.
4. SSH into the instance:
   ```bash
   chmod 400 k3s-ticket-key.pem
   ssh -i "k3s-ticket-key.pem" ubuntu@<YOUR_EC2_PUBLIC_IP>
   ```
5. Configure 2 GB Swap space and reduce swappiness:
   ```bash
   sudo fallocate -l 2G /swapfile
   sudo chmod 600 /swapfile
   sudo mkswap /swapfile
   sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   sudo sysctl vm.swappiness=10
   echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
   ```

---

### Phase 3: K3s Cluster Initialization
Install K3s keeping native Traefik Ingress enabled (disabling metrics-server to conserve ~60 MB RAM):
```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server \
  --disable metrics-server \
  --write-kubeconfig-mode 644" sh -

mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
source ~/.bashrc
```

Install Cert-Manager for Automated SSL:
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.1/cert-manager.yaml
```

---

### Phase 4: Deploying Application Manifests
1. Clone or sync this repository onto the EC2 server (or copy the `k8s/` folder).
2. Edit `k8s/02-secrets.yaml` and inject your real AWS S3 and Telegram bot tokens.
3. Edit `k8s/06-ingress.yaml` and set your registered custom domain.
4. Execute the deployment script:
   ```bash
   chmod +x k8s/deploy.sh
   ./k8s/deploy.sh deploy
   ```

---

### Phase 5: Verification & Zero-Downtime Operations
Check running state:
```bash
kubectl get pods,svc,ingress,pvc -n ticket-prod
```
Check Let's Encrypt certificate acquisition:
```bash
kubectl get certificate -n ticket-prod
```
Perform a zero-downtime rolling update after pushing new Docker images:
```bash
./k8s/deploy.sh restart
```

---

## 4. Deep-Dive Documentation Index

To explore detailed architectural topics, refer to each specialized document:
- **[00_k8s_architecture.md](./00_k8s_architecture.md)**: Deep dive into cluster, node, pod, service, namespace, and container interactions.
- **[01_requirements.md](./01_requirements.md)**: Hardware constraints, RAM budget formulas, and security boundaries.
- **[02_infrastructure_setup.md](./02_infrastructure_setup.md)**: Step-by-step AWS provisioning and kernel tuning commands.
- **[03_deployment_guide.md](./03_deployment_guide.md)**: Build arguments, image publishing, and sequential rollout walkthroughs.
- **[04_troubleshooting.md](./04_troubleshooting.md)**: Handling JVM cold starts, CrashLoopBackOff triage, and certificate resolution.
- **[k8s/k8s_guideline.md](../k8s/k8s_guideline.md)**: Guide for developers migrating from `docker-compose.yml` to Kubernetes objects.