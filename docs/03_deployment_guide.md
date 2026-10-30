# Mono-Ticket — Production Deployment & Service Rollout Guide

> **DevOps & SRE Operational Standard**: Production-grade walkthrough for building multi-architecture container images, securely managing secrets, customizing Kubernetes manifests, sequentially deploying cluster services, issuing Let's Encrypt SSL certificates via Traefik, and verifying public internet accessibility.

---

## 🗺️ Deployment Topology & Resource Dependencies

The following dependency chain outlines the exact order in which Kubernetes resources must be deployed:

```text
                                [ 00-namespace.yaml ]
                              Creates namespace: ticket-prod
                                         │
                                         ▼
                     [ 01-configmap.yaml ] + [ 02-secrets.yaml ]
                     Environment variables, DB credentials, S3, Telegram
                                         │
                                         ▼
                 [ 03-mongodb.yaml ] + [ 08-mongodb-network-policy.yaml ]
                 PVC (5Gi gp3) + Mongo 7.0 + Zero-Trust Network Isolation
                                         │
                                         ▼
                               [ 04-ticket-api.yaml ]
                      Spring Boot WebFlux REST API (Java 25)
                     (Waits for DB, startup probe: /actuator/health)
                                         │
                                         ▼
                              [ 05-ticket-admin.yaml ]
                    Vue.js 3 SPA Admin Dashboard (Nginx Proxy)
                     (Reverse proxies /api, /token, /login to API)
                                         │
                                         ▼
                            [ 07-cluster-issuer.yaml ]
                      Let's Encrypt ACME HTTP-01 ClusterIssuer
                                         │
                                         ▼
                                [ 06-ingress.yaml ]
                     Traefik Ingress + HTTPS Redirect Middleware
                     Binds yourdomain.com, www, and api subdomains
```

---

## Phase 1: Build & Publish Container Images (Local Workstation)

> [!WARNING]
> **DevOps SRE Golden Rule:**
> **Never build container images directly on the EC2 `t3.micro` instance!**
> Compiling Spring Boot with Gradle or bundling Vue with Vite on a 1 GB RAM instance will exhaust CPU credits and trigger kernel Out-Of-Memory (OOM) panics. Always build on your local development workstation (or CI/CD pipeline) and push images to a container registry.

### Step 1.1: Log in to Container Registry
On your **local workstation**:
```bash
docker login
# Enter your Docker Hub username and password/personal access token
```
*(If using GitHub Container Registry: `echo $GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin`)*.

---

### Step 1.2: Build & Push Backend API (Spring Boot WebFlux)
From your project root on your local workstation:
```bash
cd ticket

# Build image for x86_64 architecture (required for AWS EC2 t3.micro)
docker build --platform linux/amd64 -t <YOUR_DOCKER_USERNAME>/ticket-api:1.0.0 .

# Push image to Docker Hub
docker push <YOUR_DOCKER_USERNAME>/ticket-api:1.0.0
```

> [!NOTE]
> The `--platform linux/amd64` flag is mandatory if you are developing on Apple Silicon (M1/M2/M3) or ARM workstations to produce compatible Linux x86_64 binaries.

---

### Step 1.3: Build & Push Frontend Admin (Vue.js 3 SPA)
From your project root on your local workstation:
```bash
cd ../ticket-admin

# Build image with mock data disabled
docker build --platform linux/amd64 \
  --build-arg VITE_ENABLE_MOCK_DATA=false \
  -t <YOUR_DOCKER_USERNAME>/ticket-admin:1.0.0 .

# Push image to Docker Hub
docker push <YOUR_DOCKER_USERNAME>/ticket-admin:1.0.0
```

---

### Step 1.4: Registry Access Verification
- **Public Repositories (Default)**: Your K3s cluster can pull public images directly without authentication.
- **Private Repositories**: If your repositories are private, you must create a Kubernetes pull secret on the EC2 host (detailed in Phase 3.2).

---

## Phase 2: Transfer Kubernetes Files to EC2 Server

You need the `k8s/` manifests on your EC2 instance.

### Option A: Clone the Git Repository onto EC2 (Recommended)
SSH into your EC2 server and clone the repository:
```bash
ssh -i k3s-ticket-key.pem ubuntu@<YOUR_ELASTIC_IP>

# Clone repository
git clone https://github.com/<YOUR_GITHUB_USER>/mono-ticket.git
cd mono-ticket/k8s
```

### Option B: Secure Copy (`scp`) the `k8s/` Folder Directly
From your **local workstation**:
```bash
scp -i k3s-ticket-key.pem -r k8s ubuntu@<YOUR_ELASTIC_IP>:~/
ssh -i k3s-ticket-key.pem ubuntu@<YOUR_ELASTIC_IP>
cd ~/k8s
```

---

## Phase 3: Production Secrets & Manifest Customization

Before deploying, customize your manifests with production credentials and your registered domain name.

### Step 3.1: Create Production Secrets (`k8s/02-secrets.yaml`)
To follow 12-factor application security standards, `02-secrets.yaml` is ignored in Git. A template is provided at `02-secrets.example.yaml`.

On the EC2 server:
```bash
cp 02-secrets.example.yaml 02-secrets.yaml
nano 02-secrets.yaml
```

Generate strong random credentials for MongoDB and the API secret:
```bash
# Generate 16-byte random hex passwords:
openssl rand -hex 16
```

Populate `k8s/02-secrets.yaml` with your values:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ticket-secrets
  namespace: ticket-prod
  labels:
    app.kubernetes.io/part-of: mono-ticket
    app.kubernetes.io/component: secrets
type: Opaque
stringData:
  # MongoDB bootstrap admin (used only on first database initialization)
  MONGO_INITDB_ROOT_USERNAME: "rootadmin"
  MONGO_INITDB_ROOT_PASSWORD: "GENERATE_STRONG_PASSWORD_HERE"

  # Least-privilege application account (readWrite on ticket_db only)
  MONGO_APP_USERNAME: "ticket_app"
  MONGO_APP_PASSWORD: "GENERATE_STRONG_PASSWORD_HERE"

  # API Secret Key for ticket operations
  TICKET_API_SECRET: "GENERATE_STRONG_RANDOM_SECRET_KEY_HERE"

  # ---- AWS S3 Credentials (File storage) ----
  AWS_ACCESS_KEY_ID: "YOUR_AWS_ACCESS_KEY_ID"
  AWS_SECRET_ACCESS_KEY: "YOUR_AWS_SECRET_ACCESS_KEY"
  AWS_BUCKET_PRIVATE: "your-private-bucket-name"
  AWS_BUCKET_STATIC: "your-static-bucket-name"
  AWS_STATIC_URL: "https://your-static-bucket.s3.amazonaws.com"
  AWS_PRIVATE_URL: "https://your-private-bucket.s3.amazonaws.com"
  AWS_PRIVATE_FILE_EXPIRED: "3600"

  # ---- Telegram Bot (Alerts & notifications) ----
  TELEGRAM_BOT_TOKEN: "your_telegram_bot_token"
  TELEGRAM_BOT_USERNAME: "your_bot_username"
  TELEGRAM_CHAT_ID: "your_chat_id"
  TELEGRAM_WEBHOOK_SECRET: "your_random_webhook_secret"
```

> [!TIP]
> **Testing without S3 or Telegram?**
> If you haven't set up AWS S3 or Telegram yet, set their values to placeholder strings (e.g. `none` or `dummy`). The Spring Boot backend will start up successfully as long as the environment variables are not empty.

---

### Step 3.2: Update Image Names in Deployment Manifests
Open `k8s/04-ticket-api.yaml` and `k8s/05-ticket-admin.yaml`:

```bash
# In 04-ticket-api.yaml (around line 38):
# Replace:
image: yourdockeruser/ticket-api:1.0.0
# With your actual image:
image: <YOUR_DOCKER_USERNAME>/ticket-api:1.0.0

# In 05-ticket-admin.yaml (around line 134):
# Replace:
image: yourdockeruser/ticket-admin:1.0.0
# With your actual image:
image: <YOUR_DOCKER_USERNAME>/ticket-admin:1.0.0
```

#### If using Private Repositories:
Create the `regcred` secret in the `ticket-prod` namespace:
```bash
kubectl create namespace ticket-prod --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret docker-registry regcred \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<YOUR_DOCKER_USERNAME> \
  --docker-password=<YOUR_DOCKER_TOKEN> \
  --docker-email=<YOUR_EMAIL> \
  -n ticket-prod
```
*(If your images are **Public**, you do not need to configure anything further; Kubernetes pulls public images automatically).*

---

### Step 3.3: Configure Email in ClusterIssuer (`k8s/07-cluster-issuer.yaml`)
Open `k8s/07-cluster-issuer.yaml` and update the email field:
```yaml
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-real-email@gmail.com   # ◄ Replace with your email!
    privateKeySecretRef:
      name: letsencrypt-prod-account-key
    solvers:
      - http01:
          ingress:
            ingressClassName: traefik
```
*(Let's Encrypt requires a valid email for account registration and expiration alerts).*

---

### Step 3.4: Configure Custom Domain in Ingress (`k8s/06-ingress.yaml`)
Open `k8s/06-ingress.yaml` and replace `yourdomain.com` with your real registered domain name (e.g. `example.com`):
- Root domain: `yourdomain.com` → `ticket-admin-svc:80`
- WWW subdomain: `www.yourdomain.com` → `ticket-admin-svc:80`
- API subdomain: `api.yourdomain.com` → `ticket-api-svc:8000`

#### Quick Sed Command (Automated Replacement):
If your domain is `example.com`, run:
```bash
sed -i 's/yourdomain.com/example.com/g' 06-ingress.yaml
```

Verify the replacement:
```bash
grep -E 'host|hosts' 06-ingress.yaml
```

---

## Phase 4: Step-by-Step Cluster Services Deployment

Deploy each Kubernetes resource sequentially, verifying that each service is healthy before proceeding to the next.

### Step 4.1: Create Namespace
```bash
kubectl apply -f 00-namespace.yaml
```
Verify:
```bash
kubectl get ns ticket-prod
```

---

### Step 4.2: Apply ConfigMap & Production Secrets
```bash
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secrets.yaml
```
Verify:
```bash
kubectl get configmap,secret -n ticket-prod
```

---

### Step 4.3: Deploy MongoDB & Zero-Trust NetworkPolicy
Deploy MongoDB with its 5 GiB Persistent Volume Claim (backed by K3s local-path provisioner on your 30GB EBS disk):
```bash
kubectl apply -f 03-mongodb.yaml
kubectl apply -f 08-mongodb-network-policy.yaml
```

Wait for MongoDB to report `Ready`:
```bash
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=mongodb \
  -n ticket-prod --timeout=120s
```

Check Persistent Volume Claim status (must show `Bound`):
```bash
kubectl get pvc -n ticket-prod
```

Inspect database initialization logs:
```bash
kubectl logs -l app.kubernetes.io/name=mongodb -n ticket-prod --tail=20
```
You should see: `Created app user: ticket_app` (or `Waiting for connections`).

---

### Step 4.4: Deploy Backend API (Spring Boot WebFlux)
Deploy the reactive API:
```bash
kubectl apply -f 04-ticket-api.yaml
```

> [!NOTE]
> **Spring Boot Initialization Time on `t3.micro`:**
> With 1 vCPU and Java memory capped at `-XX:+UseSerialGC -Xmx256m`, cold startup takes approximately **60 to 90 seconds**.
> The deployment defines a resilient `startupProbe` that tests `/actuator/health` every 10 seconds (allowing up to 300 seconds total).

Monitor the API pod:
```bash
kubectl get pods -n ticket-prod -w
```

Watch the live application logs:
```bash
kubectl logs -f deployment/ticket-api -n ticket-prod
```

Wait until you see the confirmation log line:
```text
Started TicketApplication in XX.XXX seconds (process running for XX.XXX)
```

#### Test In-Cluster Health Check:
Open a temporary port-forward on the EC2 server to test the backend internally:
```bash
kubectl port-forward svc/ticket-api-svc 8000:8000 -n ticket-prod &
PF_PID=$!

curl http://localhost:8000/actuator/health
# Expected output: {"status":"UP"}

kill $PF_PID
```

---

### Step 4.5: Deploy Frontend Admin (Vue.js SPA + Nginx)
Deploy the frontend and its Nginx reverse proxy configuration:
```bash
kubectl apply -f 05-ticket-admin.yaml
```

Wait for the frontend pod to report ready:
```bash
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=ticket-admin \
  -n ticket-prod --timeout=60s
```

Verify that all internal services are active:
```bash
kubectl get svc -n ticket-prod
```

---

### Step 4.6: Deploy Let's Encrypt ClusterIssuer
Apply the ClusterIssuer so Cert-Manager knows how to request certificates from Let's Encrypt via Traefik:
```bash
kubectl apply -f 07-cluster-issuer.yaml
```

Verify ClusterIssuer status:
```bash
kubectl get clusterissuer letsencrypt-prod
```
Status should report **`READY = True`**.

---

### Step 4.7: Apply Ingress Routing & HTTPS Redirect
Apply the Ingress rules and Traefik HTTPS-redirect middleware:
```bash
kubectl apply -f 06-ingress.yaml
```

Verify Ingress creation:
```bash
kubectl get ingress -n ticket-prod
```

Expected output:
```text
NAME             CLASS     HOSTS                                        ADDRESS          PORTS     AGE
ticket-ingress   traefik   yourdomain.com,www.yourdomain.com,api....    <YOUR_ELASTIC_IP> 80, 443   15s
```

---

## Phase 5: Automated SSL Certificate Issuance & Validation

When `06-ingress.yaml` is applied, the annotation `cert-manager.io/cluster-issuer: "letsencrypt-prod"` immediately triggers Cert-Manager to negotiate an SSL certificate with Let's Encrypt.

### Step 5.1: How the HTTP-01 Challenge Works Under the Hood
1. Cert-Manager requests a certificate from Let's Encrypt for your domains (`yourdomain.com`, `www`, `api`).
2. Let's Encrypt provides an ACME cryptographic challenge token.
3. Cert-Manager registers a temporary route in Traefik at:
   `http://yourdomain.com/.well-known/acme-challenge/<TOKEN>`
4. Let's Encrypt servers query your domain over the public internet on **Port 80** to verify the token.
5. Once verified, Let's Encrypt signs the TLS certificate.
6. Cert-Manager stores the certificate and private key in the secret `ticket-prod-tls-secret`.

---

### Step 5.2: Track Certificate Progress
Run:
```bash
kubectl get certificate -n ticket-prod -w
```

Watch the `READY` column transition from `False` to `True` (typically 30–90 seconds):
```text
NAME                     READY   SECRET                   AGE
ticket-prod-tls-secret   True    ticket-prod-tls-secret   75s
```

---

### Step 5.3: Deep-Dive SSL Troubleshooting
If `READY` remains `False` after 3 minutes:

1. **Describe the Certificate**:
   ```bash
   kubectl describe certificate ticket-prod-tls-secret -n ticket-prod
   ```
2. **Inspect Certificate Requests & ACME Challenges**:
   ```bash
   kubectl get certificaterequests -n ticket-prod
   kubectl get orders -n ticket-prod
   kubectl get challenges -n ticket-prod
   ```
   If a challenge exists, run `kubectl describe challenge <CHALLENGE_NAME> -n ticket-prod` to see the exact error from Let's Encrypt.
3. **Common Root Causes**:
   - **Port 80 Blocked**: Verify that your AWS Security Group `k3s-web-sg` has port 80 open to `0.0.0.0/0`.
   - **DNS Unpropagated**: Verify `dig +short yourdomain.com` returns `<YOUR_ELASTIC_IP>`. If DNS points to the wrong IP, Let's Encrypt cannot reach the challenge file.
   - **Cloudflare Orange Cloud Active**: If using Cloudflare DNS, set to **DNS Only (Grey Cloud)** until the initial certificate is validated.

---

## Phase 6: Verify Public Internet Access

Now verify the entire stack from an external machine (your laptop browser or terminal).

### 1. Test Vue.js Admin Frontend (Root & WWW)
Open your web browser and navigate to:
- **`https://yourdomain.com`**
- **`https://www.yourdomain.com`**

**Expected Results:**
- 🔒 The browser shows a secure padlock icon (SSL valid, issued by Let's Encrypt Authority).
- The Mono-Ticket Admin interface loads and renders cleanly.

---

### 2. Test HTTP to HTTPS Automatic Redirection
In your local terminal, send an unencrypted HTTP request:
```bash
curl -I http://yourdomain.com
```

**Expected Response:**
```http
HTTP/1.1 308 Permanent Redirect
Location: https://yourdomain.com/
```

---

### 3. Test Backend API Subdomain Directly
In your local terminal:
```bash
curl -s https://api.yourdomain.com/actuator/health
```

**Expected Response:**
```json
{"status":"UP"}
```

---

### 4. Test In-Browser API Proxying
1. Open `https://yourdomain.com` in your browser.
2. Press `F12` to open Developer Tools → switch to the **Network** tab.
3. Log in or interact with the ticket dashboard.
4. Verify that requests to `/token`, `/login`, or `/api/*` succeed with `200 OK`.
   *(Nginx smoothly proxies these internal calls to `ticket-api-svc:8000` inside Kubernetes without triggering browser CORS issues).*

---

## Phase 7: Automated Deployment with `deploy.sh`

The repository includes a helper CLI script `k8s/deploy.sh` for lifecycle management:

```bash
# Make script executable
chmod +x deploy.sh

# Deploy all services sequentially with health checks
./deploy.sh deploy

# View current pod and service status
./deploy.sh status

# Trigger a rolling restart
./deploy.sh restart

# Teardown all application pods and services
./deploy.sh teardown
```

---

## Phase 8: Deploying Code Updates (Zero Downtime)

When you make changes to backend or frontend code:

### On Your Local Machine:
1. Rebuild and push the updated images with a new tag:
   ```bash
   # Backend
   cd ticket
   docker build --platform linux/amd64 -t <YOUR_DOCKER_USERNAME>/ticket-api:1.0.1 .
   docker push <YOUR_DOCKER_USERNAME>/ticket-api:1.0.1

   # Frontend
   cd ../ticket-admin
   docker build --platform linux/amd64 --build-arg VITE_ENABLE_MOCK_DATA=false -t <YOUR_DOCKER_USERNAME>/ticket-admin:1.0.1 .
   docker push <YOUR_DOCKER_USERNAME>/ticket-admin:1.0.1
   ```

### On Your EC2 Server:
2. Update the image tag in `04-ticket-api.yaml` and `05-ticket-admin.yaml` (or trigger rolling restart if using `:latest`):
   ```bash
   kubectl rollout restart deployment/ticket-api -n ticket-prod
   kubectl rollout restart deployment/ticket-admin -n ticket-prod
   ```

3. Watch rollout progression:
   ```bash
   kubectl rollout status deployment/ticket-api -n ticket-prod
   kubectl rollout status deployment/ticket-admin -n ticket-prod
   ```

---

## Phase 9: Server Maintenance & Resource Management

### Clean Up Stale Images (Free Disk Space)
Over time, pulling new images leaves dangling container layers on your EBS disk. Prune them with:
```bash
sudo k3s crictl rmi --prune
```

### Inspect Disk & Memory
```bash
# Check disk usage
df -h /

# Check memory and swap usage
free -h
```

---

## 📋 Quick Command Cheat Sheet

| Task | Command |
|:---|:---|
| Check all pods | `kubectl get pods -n ticket-prod` |
| View API live logs | `kubectl logs -f deployment/ticket-api -n ticket-prod` |
| View Frontend live logs | `kubectl logs -f deployment/ticket-admin -n ticket-prod` |
| View MongoDB live logs | `kubectl logs -f deployment/mongo-db -n ticket-prod` |
| View Traefik ingress logs | `kubectl logs -f -n kube-system -l app.kubernetes.io/name=traefik` |
| Check SSL certificate | `kubectl get certificate -n ticket-prod` |
| Shell into MongoDB | `kubectl exec -it deployment/mongo-db -n ticket-prod -- mongosh ticket_db` |
| Restart application | `./k8s/deploy.sh restart` |

---

## 🏁 Conclusion

Your Mono-Ticket stack is now live on the public internet, secured with auto-renewing Let's Encrypt SSL, running on a resilient single-node K3s cluster, completely within AWS Free Tier limits!

If you encounter runtime issues, refer to:
👉 **[04_troubleshooting.md](./04_troubleshooting.md)**
