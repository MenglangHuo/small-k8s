# End-to-End Production Deployment Guide: Spring Boot + Next.js on AWS Free Tier Kubernetes (K3s)

This guide walks you step-by-step through deploying a **Next.js Frontend** and a **Spring Boot Backend API** on AWS using a lightweight Kubernetes cluster (**K3s**), **Nginx Ingress Controller**, and **Cert-Manager (Let's Encrypt SSL)**—all optimized to stay within the **AWS Free Tier** (1 vCPU, 1 GB RAM `t3.micro`/`t2.micro` + 30 GB EBS).

---

## Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [Phase 1: AWS Infrastructure Setup (EC2, EBS, Security Groups, RDS)](#2-phase-1-aws-infrastructure-setup)
3. [Phase 2: Custom Domain & DNS Configuration](#3-phase-2-custom-domain--dns-configuration)
4. [Phase 3: Preparing Docker Images (Spring Boot & Next.js)](#4-phase-3-preparing-docker-images)
5. [Phase 4: Linux OS Tuning & Swap Setup (Critical for 1GB RAM)](#5-phase-4-linux-os-tuning--swap-setup)
6. [Phase 5: Installing K3s (Lightweight Kubernetes) & Helm](#6-phase-5-installing-k3s--helm)
7. [Phase 6: Installing Nginx Ingress Controller & Cert-Manager](#7-phase-6-installing-nginx-ingress--cert-manager)
8. [Phase 7: Kubernetes Namespace, Secrets & ConfigMaps](#8-phase-7-kubernetes-namespace-secrets--configmaps)
9. [Phase 8: Deploying Database, Spring Boot, and Next.js](#9-phase-8-deploying-database-spring-boot-and-nextjs)
10. [Phase 9: Configuring Nginx Ingress & Automatic SSL](#10-phase-9-configuring-nginx-ingress--automatic-ssl)
11. [Phase 10: Verification, Troubleshooting & Useful Commands](#11-phase-10-verification-troubleshooting--useful-commands)

---

## 1. Architecture Overview

```text
Internet Users
   │
   ├── https://yourdomain.com ──────┐
   └── https://api.yourdomain.com ──┤
                                    ▼
                        [ AWS Security Group ]
                        (Ports 80, 443, 22 Open)
                                    │
                                    ▼
┌─── AWS EC2 Instance (Ubuntu 24.04 LTS, t3.micro, 30GB gp3 EBS) ───────────┐
│                                                                           │
│  [ Linux OS + 2GB Swap File ]                                             │
│                                                                           │
│  ┌─── K3s Single-Node Cluster ─────────────────────────────────────────┐  │
│  │                                                                     │  │
│  │  [ Namespace: ingress-nginx ]                                       │  │
│  │   └── Nginx Ingress Controller (Binds directly to Host 80 & 443)    │  │
│  │                                                                     │  │
│  │  [ Namespace: cert-manager ]                                        │  │
│  │   └── Cert-Manager (Auto-provisions & renews Let's Encrypt TLS)     │  │
│  │                                                                     │  │
│  │  [ Namespace: app-prod ]                                            │  │
│  │   ├── Service: nextjs-svc (ClusterIP :3000)                         │  │
│  │   │    └── Pod: nextjs-web (Standalone Node build, Limit: 220Mi)    │  │
│  │   │         │ (Internal SSR Calls via K8s DNS)                      │  │
│  │   │         ▼                                                       │  │
│  │   ├── Service: springboot-svc (ClusterIP :8080)                     │  │
│  │   │    └── Pod: springboot-api (JVM SerialGC, Limit: 384Mi)         │  │
│  │   │                                                                 │  │
│  │   └── Secrets & ConfigMaps (DB Credentials, Registry Pull Token)    │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────┬───────────────────────────────────────┘
                                    │ Port 5432 (Private VPC or In-Cluster)
                                    ▼
                     [ PostgreSQL Database Layer ]
           (Option A: AWS RDS db.t3.micro | Option B: K8s Pod)
```

---

## 2. Phase 1: AWS Infrastructure Setup

### Step 1.1: Launch the EC2 Instance
1. Log in to the **AWS Management Console** and navigate to **EC2** -> **Launch instance**.
2. **Name:** `k3s-prod-server`
3. **AMI (OS Image):** Select **Ubuntu Server 24.04 LTS (HVM), SSD Volume Type** (`64-bit (x86)`).
4. **Instance Type:** Select **`t3.micro`** or **`t2.micro`** (Marked *Free tier eligible*: 1 vCPU, 1 GB Memory).
5. **Key Pair (Login):** Click **Create new key pair**, name it `k3s-prod-key`, select `RSA` and `.pem`, and download it to your computer.
6. **Network Settings:** Click **Edit**:
   - **Auto-assign public IP:** `Enable`
   - **Firewall (Security Groups):** Select **Create security group** and name it `k3s-web-sg`.
   - Add the following **Inbound Security Group Rules**:
     | Type | Protocol | Port Range | Source | Description |
     | :--- | :--- | :--- | :--- | :--- |
     | **SSH** | TCP | `22` | `My IP` (or `0.0.0.0/0`) | SSH Admin Access |
     | **HTTP** | TCP | `80` | `0.0.0.0/0` | Web & Let's Encrypt Challenge |
     | **HTTPS** | TCP | `443` | `0.0.0.0/0` | Production SSL Traffic |
     *(Note: Do **NOT** open ports 3000, 8080, or 6443 to the public internet. Nginx handles routing inside the server).*
7. **Configure Storage:**
   - Change the root volume size from `8 GiB` to **`30 GiB`** and select **`gp3`** (AWS Free Tier includes up to 30 GB of EBS storage for free).
8. Click **Launch instance**.

### Step 1.2: (Optional but Recommended) Allocate an Elastic IP
If you stop and start an EC2 instance, its Public IP changes. An **Elastic IP** keeps the IP static forever and is **free** as long as it is attached to one running EC2 instance.
1. In the EC2 sidebar, go to **Network & Security** -> **Elastic IPs**.
2. Click **Allocate Elastic IP address** -> **Allocate**.
3. Select the newly created IP -> **Actions** -> **Associate Elastic IP address**.
4. Select your `k3s-prod-server` instance and click **Associate**.
5. Copy this IPv4 address (referred to below as `<YOUR_EC2_PUBLIC_IP>`).

### Step 1.3: Database Choice (Choose Option A or Option B)
* **Option A: AWS RDS Free Tier (Recommended to save EC2 RAM)**
  1. Go to **AWS RDS** -> **Create database** -> **Standard create** -> **PostgreSQL**.
  2. Templates: Select **Free tier**.
  3. DB instance identifier: `app-prod-db`, Master username: `postgres`, set a strong Master password.
  4. Instance configuration: `db.t3.micro` (or `db.t4g.micro`).
  5. Storage: `20 GiB` (`gp2`/`gp3`), **Uncheck** "Enable storage autoscaling".
  6. Connectivity: Choose **Connect to an EC2 compute resource** and select `k3s-prod-server` (AWS will automatically configure the RDS Security Group to allow port `5432` from your EC2 instance).
* **Option B: In-Cluster PostgreSQL inside Kubernetes**
  - Skip RDS creation. A ready-to-use Kubernetes StatefulSet manifest for PostgreSQL is included in **Phase 8** below.

---

## 3. Phase 2: Custom Domain & DNS Configuration

Log in to your domain registrar (Namecheap, GoDaddy, Cloudflare, Porkbun, etc.) and open the **DNS Management** page for `yourdomain.com`.

Create the following **A Records** pointing to `<YOUR_EC2_PUBLIC_IP>`:

| Type | Host / Name | Value / Target | TTL | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **A** | `@` | `<YOUR_EC2_PUBLIC_IP>` | Auto / 300s | Main Next.js app (`yourdomain.com`) |
| **A** | `www` | `<YOUR_EC2_PUBLIC_IP>` | Auto / 300s | `www.yourdomain.com` |
| **A** | `api` | `<YOUR_EC2_PUBLIC_IP>` | Auto / 300s | Spring Boot API (`api.yourdomain.com`) |

> **Important if using Cloudflare:** Set the Proxy Status to **DNS Only (Grey Cloud)** initially so Let's Encrypt can verify your server directly via HTTP-01 challenge. Once SSL works, you can switch to Orange Cloud (Full Strict SSL mode) if desired.

 Verify DNS resolution from your local terminal before continuing:
```bash
nslookup yourdomain.com
nslookup api.yourdomain.com
```

---

## 4. Phase 3: Preparing Docker Images

Because your EC2 instance only has 1 GB of RAM, **never build Docker images on the EC2 server itself** ( running `mvn package` or `next build` on the server will crash the OS). Build them on your local machine or in GitHub Actions and push them to **Docker Hub** or **GitHub Container Registry (`ghcr.io`)**.

### 4.1: Optimized `Dockerfile` for Spring Boot
Place this `Dockerfile` in your Spring Boot root directory:

```dockerfile
# Stage 1: Build JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY ../ticket/src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2: Optimized `Dockerfile` for Next.js (Standalone Mode)
1. First, ensure your `next.config.js` (or `next.config.mjs`) has `output: 'standalone'` enabled:
   ```javascript
   /** @type {import('next').NextConfig} */
   const nextConfig = {
     output: 'standalone',
   };
   export default nextConfig;
   ```
2. Place this `Dockerfile` in your Next.js root directory:
   ```dockerfile
   FROM node:20-alpine AS base

   FROM base AS deps
   RUN apk add --no-cache libc6-compat
   WORKDIR /app
   COPY package.json package-lock.json* ./
   RUN npm ci

   FROM base AS builder
   WORKDIR /app
   COPY --from=deps /app/node_modules ./node_modules
   COPY . .
   # Pass build-time public API URL
   ARG NEXT_PUBLIC_API_URL=https://api.yourdomain.com
   ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
   RUN npm run build

   FROM base AS runner
   WORKDIR /app
   ENV NODE_ENV=production
   RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs
   COPY --from=builder /app/public ./public
   COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
   COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
   USER nextjs
   EXPOSE 3000
   ENV PORT=3000
   ENV HOSTNAME="0.0.0.0"
   CMD ["node", "server.js"]
   ```

### 4.3: Build and Push to Registry (From Your Local PC)
*(Note: If you use an Apple Silicon Mac M1/M2/M3, add `--platform linux/amd64` so the image runs on your x86 AWS EC2 instance).*
```bash
# Log in to Docker Hub (or ghcr.io)
docker login

# Build & Push Spring Boot
docker build --platform linux/amd64 -t yourdockeruser/springboot-api:v1 ./backend
docker push yourdockeruser/springboot-api:v1

# Build & Push Next.js
docker build --platform linux/amd64 \
  --build-arg NEXT_PUBLIC_API_URL=https://api.yourdomain.com \
  -t yourdockeruser/nextjs-web:v1 ./frontend
docker push yourdockeruser/nextjs-web:v1
```

---

## 5. Phase 4: Linux OS Tuning & Swap Setup

Connect to your EC2 server via SSH from your local terminal:
```bash
chmod 400 k3s-prod-key.pem
ssh -i "k3s-prod-key.pem" ubuntu@<YOUR_EC2_PUBLIC_IP>
```

### Step 4.1: Update Ubuntu Packages
```bash
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y curl wget git apt-transport-https ca-certificates
```

### Step 4.2: Create a 2 GB Swap File (Mandatory on 1 GB RAM Nodes)
Without swap space, starting the JVM and K3s control plane simultaneously can spike memory above 1024 MB and freeze the instance.
```bash
# Allocate 2GB swap file
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Make swap permanent across reboots
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Tune kernel swappiness (only swap when RAM is genuinely tight)
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf

# Verify swap is active (You should see 2.0Gi under Swap)
free -h
```

---

## 6. Phase 5: Installing K3s & Helm

K3s ships with the **Traefik** ingress controller enabled by default. Because we want **Nginx Ingress Controller**, we must disable Traefik and `servicelb` during installation.

### Step 5.1: Install K3s
Run this on your EC2 server:
```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server \
  --disable traefik \
  --disable servicelb \
  --disable metrics-server \
  --write-kubeconfig-mode 644" sh -
```

### Step 5.2: Configure `kubectl` for the `ubuntu` User
So that you and Helm can run `kubectl` without typing `sudo` every time:
```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
source ~/.bashrc

# Verify node is Ready
kubectl get nodes
```

### Step 5.3: Install Helm (Kubernetes Package Manager)
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

---

## 7. Phase 6: Installing Nginx Ingress & Cert-Manager

In standard cloud setups, an Ingress controller provisions a paid AWS LoadBalancer ($18/month). To stay **100% Free Tier**, we configure Nginx Ingress to bind directly to the EC2 host's ports `80` and `443` via `hostPort`.

### Step 6.1: Install Nginx Ingress Controller via Helm
```bash
helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.hostPort.enabled=true \
  --set controller.service.type=ClusterIP \
  --set controller.resources.requests.memory=64Mi \
  --set controller.resources.limits.memory=160Mi
```

Verify Nginx Ingress pod is running:
```bash
kubectl get pods -n ingress-nginx
```
Wait until `ingress-nginx-controller-...` shows `1/1 Running`.

### Step 6.2: Install Cert-Manager (For Free Let's Encrypt HTTPS)
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.1/cert-manager.yaml
```
Wait 30–60 seconds and verify all three Cert-Manager pods are `Running`:
```bash
kubectl get pods -n cert-manager
```

### Step 6.3: Create the Let's Encrypt `ClusterIssuer`
Create a file named `cluster-issuer.yaml` on your EC2 server:
```bash
cat <<EOF > cluster-issuer.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    # Let's Encrypt Production ACME Server
    server: https://acme-v02.api.letsencrypt.org/directory
    # REPLACE WITH YOUR REAL EMAIL ADDRESS
    email: admin@yourdomain.com
    privateKeySecretRef:
      name: letsencrypt-prod-account-key
    solvers:
      - http01:
          ingress:
            class: nginx
EOF

kubectl apply -f cluster-issuer.yaml
```
Check status (`READY` should say `True`):
```bash
kubectl get clusterissuer
```

---

## 8. Phase 7: Kubernetes Namespace, Secrets & ConfigMaps

Now we create a dedicated namespace (`app-prod`) for your Spring Boot and Next.js apps and configure their environment variables and credentials.

### Step 7.1: Create the Namespace
```bash
kubectl create namespace app-prod
```

### Step 7.2: (If using Private Docker Images) Create Image Pull Secret
If your Docker Hub or GitHub Container Registry repositories are **private**, run this command so Kubernetes can pull them:
```bash
kubectl create secret docker-registry regcred \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<YOUR_DOCKER_USERNAME> \
  --docker-password=<YOUR_DOCKER_TOKEN_OR_PASSWORD> \
  --docker-email=<YOUR_EMAIL> \
  -n app-prod
```
*(If using GitHub Container Registry, set `--docker-server=ghcr.io`).*

### Step 7.3: Create Application ConfigMap and Secret
Create a file named `01-config-and-secrets.yaml`:
```bash
cat << 'EOF' > 01-config-and-secrets.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: app-prod
data:
  SPRING_PROFILES_ACTIVE: "prod"
  # If using Option B (In-cluster Postgres), use: jdbc:postgresql://postgres-svc:5432/appdb
  # If using Option A (AWS RDS), replace with your RDS Endpoint:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-svc:5432/appdb"
  SPRING_DATASOURCE_USERNAME: "postgres"
  # Allowed CORS origin for Spring Boot
  APP_CORS_ALLOWED_ORIGINS: "https://yourdomain.com,https://www.yourdomain.com"
  # Next.js internal SSR URL (Talks directly inside K8s cluster without internet hop!)
  INTERNAL_API_URL: "http://springboot-svc.app-prod.svc.cluster.local:8080"
  # Next.js public browser URL
  NEXT_PUBLIC_API_URL: "https://api.yourdomain.com"
---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: app-prod
type: Opaque
stringData:
  SPRING_DATASOURCE_PASSWORD: "SuperStrongDbPassword123!"
  JWT_SECRET: "ReplaceWithA64CharacterRandomHexSecretForSpringSecurityJwtSigning"
EOF

kubectl apply -f 01-config-and-secrets.yaml
```

---

## 9. Phase 8: Deploying Database, Spring Boot, and Next.js

### Step 8.1: (Optional) Deploy In-Cluster PostgreSQL
**Skip this step if you created an AWS RDS instance in Phase 1.** If you want PostgreSQL running inside your K3s cluster backed by your 30 GB EBS disk, apply this manifest:

```bash
cat << 'EOF' > 02-postgres.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: app-prod
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: local-path
  resources:
    requests:
      storage: 5Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-db
  namespace: app-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres-db
  template:
    metadata:
      labels:
        app: postgres-db
    spec:
      containers:
        - name: postgres
          image: postgres:16-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: "appdb"
            - name: POSTGRES_USER
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: SPRING_DATASOURCE_USERNAME
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: app-secrets
                  key: SPRING_DATASOURCE_PASSWORD
          volumeMounts:
            - mountPath: /var/lib/postgresql/data
              name: pgdata
          resources:
            requests:
              memory: "64Mi"
              cpu: "50m"
            limits:
              memory: "150Mi"
              cpu: "200m"
      volumes:
        - name: pgdata
          persistentVolumeClaim:
            claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-svc
  namespace: app-prod
spec:
  type: ClusterIP
  selector:
    app: postgres-db
  ports:
    - port: 5432
      targetPort: 5432
EOF

kubectl apply -f 02-postgres.yaml
```

### Step 8.2: Deploy Spring Boot API
Notice the `JAVA_TOOL_OPTIONS` flag `-XX:+UseSerialGC -Xmx256m -Xms128m -XX:MaxMetaspaceSize=96m`. This is **essential** on a 1 GB RAM server so the JVM stays compact and never gets OOM-killed.

```bash
cat << 'EOF' > 03-springboot.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-api
  namespace: app-prod
spec:
  replicas: 1
  strategy:
    type: Recreate # Prevents 2 JVMs from running simultaneously during updates on 1GB RAM
  selector:
    matchLabels:
      app: springboot-api
  template:
    metadata:
      labels:
        app: springboot-api
    spec:
      imagePullSecrets:
        - name: regcred # Remove if your image is public
      containers:
        - name: springboot-api
          # REPLACE WITH YOUR IMAGE
          image: yourdockeruser/springboot-api:v1
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:+UseSerialGC -Xmx256m -Xms128m -XX:MaxMetaspaceSize=96m"
          envFrom:
            - configMapRef:
                name: app-config
            - secretRef:
                name: app-secrets
          resources:
            requests:
              memory: "220Mi"
              cpu: "150m"
            limits:
              memory: "400Mi"
              cpu: "600m"
---
apiVersion: v1
kind: Service
metadata:
  name: springboot-svc
  namespace: app-prod
spec:
  type: ClusterIP
  selector:
    app: springboot-api
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
EOF

kubectl apply -f 03-springboot.yaml
```

### Step 8.3: Deploy Next.js Frontend
```bash
cat << 'EOF' > 04-nextjs.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nextjs-web
  namespace: app-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nextjs-web
  template:
    metadata:
      labels:
        app: nextjs-web
    spec:
      imagePullSecrets:
        - name: regcred # Remove if your image is public
      containers:
        - name: nextjs-web
          # REPLACE WITH YOUR IMAGE
          image: yourdockeruser/nextjs-web:v1
          imagePullPolicy: Always
          ports:
            - containerPort: 3000
          envFrom:
            - configMapRef:
                name: app-config
          resources:
            requests:
              memory: "80Mi"
              cpu: "50m"
            limits:
              memory: "220Mi"
              cpu: "300m"
---
apiVersion: v1
kind: Service
metadata:
  name: nextjs-svc
  namespace: app-prod
spec:
  type: ClusterIP
  selector:
    app: nextjs-web
  ports:
    - protocol: TCP
      port: 3000
      targetPort: 3000
EOF

kubectl apply -f 04-nextjs.yaml
```

---

## 10. Phase 9: Configuring Nginx Ingress & Automatic SSL

Now we expose both `nextjs-svc` (`yourdomain.com`) and `springboot-svc` (`api.yourdomain.com`) through the Nginx Ingress Controller and instruct Cert-Manager to automatically request and mount a Let's Encrypt TLS certificate.

Create and apply `05-ingress.yaml` (remember to replace `yourdomain.com` with your actual domain name):

```bash
cat << 'EOF' > 05-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-main-ingress
  namespace: app-prod
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "20m" # Allows up to 20MB file uploads to Spring Boot
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - yourdomain.com
        - www.yourdomain.com
        - api.yourdomain.com
      secretName: app-prod-tls-secret
  rules:
    # 1. Next.js Root Domain
    - host: yourdomain.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: nextjs-svc
                port:
                  number: 3000
    # 2. Next.js WWW Subdomain
    - host: www.yourdomain.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: nextjs-svc
                port:
                  number: 3000
    # 3. Spring Boot API Subdomain
    - host: api.yourdomain.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: springboot-svc
                port:
                  number: 8080
EOF

kubectl apply -f 05-ingress.yaml
```

---

## 11. Phase 10: Verification, Troubleshooting & Useful Commands

### 11.1: Verify Pods and Services in `app-prod`
```bash
kubectl get pods,svc,ingress -n app-prod
```
You should see `springboot-api-...` and `nextjs-web-...` with status `1/1 Running`.

### 11.2: Verify SSL Certificate Issuance
Cert-Manager takes roughly **1 to 2 minutes** to complete the HTTP-01 challenge with Let's Encrypt. Check the status with:
```bash
kubectl get certificate -n app-prod
```
Wait until the `READY` column changes from `False` to **`True`**.
If it stays `False` for more than 3 minutes, inspect the challenge status:
```bash
kubectl describe certificate app-prod-tls-secret -n app-prod
kubectl get challenges -n app-prod
```
*(99% of SSL issues are caused by either Port 80 being blocked in the AWS Security Group or DNS `A` records not yet pointing to the EC2 Public IP).*

### 11.3: View Real-Time Application Logs
```bash
# View Spring Boot logs
kubectl logs -f deployment/springboot-api -n app-prod

# View Next.js logs
kubectl logs -f deployment/nextjs-web -n app-prod

# View Nginx Ingress routing logs
kubectl logs -f -n ingress-nginx -l app.kubernetes.io/component=controller
```

### 11.4: How to Deploy a New Code Update (Zero-Cost Rolling Update)
Whenever you push a new version of your code to Docker Hub / GHCR:
```bash
# Restart Spring Boot to pull the latest image
kubectl rollout restart deployment/springboot-api -n app-prod

# Restart Next.js to pull the latest image
kubectl rollout restart deployment/nextjs-web -n app-prod
```

### 11.5: Clean Up Unused Container Images (Free Up EBS Disk Space)
Over time, old Docker images cached by K3s can fill up your 30 GB disk. Run this command occasionally to prune unused images:
```bash
sudo k3s crictl rmi --prune
```