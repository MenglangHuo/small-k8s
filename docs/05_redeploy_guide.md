# Mono-Ticket — Application Redeployment & Zero-Downtime Rollout Runbook

> **DevOps & SRE Operational Standard**: Comprehensive walkthrough and practical runbook for redeploying applications (`ticket-admin`, `ticket-api`), propagating environment and secret changes, orchestrating zero-downtime rolling updates, performing Blue/Green cutovers, and managing emergency rollbacks on AWS EC2 single-node Kubernetes (K3s).

---

## 🧭 Matrix of Changes: What Changed & What to Execute

Whenever you make changes to the repository, use this matrix to identify the exact workflow required:

| Type of Change | What Was Modified | Local Action | Server Action | Downtime |
| :--- | :--- | :--- | :--- | :--- |
| **Backend Code** | Java classes, controllers, services in `ticket/` | Build & push `ticket-api:tag` | `kubectl set image` or edit `04-ticket-api.yaml` | **Zero (0s)** |
| **Frontend Code** | Vue 3 components, views, styles in `ticket-admin/` | Build & push `ticket-admin:tag` | `kubectl set image` or edit `05-ticket-admin.yaml` | **Zero (0s)** |
| **Config / Env** | Non-sensitive properties in `01-configmap.yaml` | Git commit / scp file | `kubectl apply -f 01-configmap.yaml && kubectl rollout restart` | **Zero (0s)** |
| **Secrets / Keys** | Passwords, tokens, API keys in `02-secrets.yaml` | Update file on EC2 | `kubectl apply -f 02-secrets.yaml && kubectl rollout restart` | **Zero (0s)** |
| **Ingress / Domain** | Routes, subdomains in `06-ingress.yaml` | Git commit / scp file | `kubectl apply -f 06-ingress.yaml` | **Zero (0s)** |
| **Autoscaling** | Replicas, thresholds in `09-ticket-api-hpa.yaml` | Git commit / scp file | `kubectl apply -f 09-ticket-api-hpa.yaml` | **Zero (0s)** |

---

## 1. Zero-Downtime Architecture: How It Works

Both `04-ticket-api.yaml` and `05-ticket-admin.yaml` use the Kubernetes `RollingUpdate` strategy:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

```text
Time ──►

T0: [ Pod v1 (Serving Traffic: 100%) ]
                 │
T1:              ├─────────────────────────────► [ Pod v2 (Starting up / Probing) ]
                 │                                      │
                 │                                      ▼
T2: [ Pod v1 (Still Serving: 100%) ]             [ Pod v2 Passes /actuator/health ]
                                                        │
T3: [ Pod v1 (Draining / SIGTERM) ]  ◄──────────────────┴── (Traffic shifts to v2)
                 │
T4: [ Pod v1 Terminated ]                        [ Pod v2 (Serving Traffic: 100%) ]
```

- **`maxUnavailable: 0`**: Ensures Kubernetes will **never** terminate an existing active pod until a new pod is fully initialized, passing health probes, and ready to take traffic.
- **`maxSurge: 1`**: Instructs Kubernetes to create 1 temporary replacement pod first.
- **`startupProbe` & `readinessProbe`**: Spring Boot takes 60–90 seconds to cold-start JVM bytecode on a 1 vCPU instance. The `readinessProbe` checks `/actuator/health` and shields the new pod from receiving any user traffic until it returns `HTTP 200 OK`.
- **Memory Cushion**: The 2 GB swap on your EC2 host safely absorbs the temporary 400–500 MB footprint while both pods overlap for ~60 seconds.

---

## 2. Redeploying the Backend API (`ticket-api`)

Follow these steps when modifying Java Spring Boot WebFlux code in `ticket/`:

### Step 2.1: Increment Version & Build Image (Local Machine)
From your local project root:
```bash
cd ticket

# Define your target release tag (e.g. 1.0.1, 1.0.2)
TAG="1.0.1"
DOCKER_USER="<YOUR_DOCKER_USERNAME>"

# Build for x86_64 architecture (EC2 t3.micro compatible)
docker build --platform linux/amd64 -t ${DOCKER_USER}/ticket-api:${TAG} .

# Push image to registry
docker push ${DOCKER_USER}/ticket-api:${TAG}
```

> [!WARNING]
> **Always compile and build images locally!** Never run `docker build` or `./gradlew build` directly on your EC2 `t3.micro` instance, as the CPU and memory spike will trigger kernel freezes.

---

### Step 2.2: Trigger Rollout on EC2 Server

SSH into your EC2 server:
```bash
ssh -i k3s-ticket-key.pem ubuntu@<YOUR_ELASTIC_IP>
cd ~/mono-ticket/k8s
```

Choose **Option A** (Standard Rolling Update) or **Option B** (Isolated Blue/Green):

#### Option A: Native Rolling Update (Standard & Recommended)
Directly update the running container image:
```bash
kubectl set image deployment/ticket-api ticket-api=${DOCKER_USER}/ticket-api:1.0.1 -n ticket-prod
```
*(Or edit line 38 of `04-ticket-api.yaml` with the new tag and run `kubectl apply -f 04-ticket-api.yaml`)*.

Watch the transition in real-time:
```bash
kubectl rollout status deployment/ticket-api -n ticket-prod
```

Expected output:
```text
Waiting for deployment "ticket-api" rollout to finish: 1 out of 2 new replicas have been updated...
Waiting for deployment "ticket-api" rollout to finish: 1 old replicas are pending termination...
Waiting for deployment "ticket-api" rollout to finish: 1 old replicas are pending termination...
deployment "ticket-api" successfully rolled out
```

---

#### Option B: Blue/Green Deployment (Isolated Pre-Testing)
If you want to privately verify the new version in production before routing any user traffic to it:

1. Open `k8s/blue-green/04-ticket-api-green.yaml` and set image to `${DOCKER_USER}/ticket-api:1.0.1`.
2. Deploy the Green candidate:
   ```bash
   kubectl apply -f blue-green/04-ticket-api-green.yaml
   ```
3. Wait for Green pod to be ready:
   ```bash
   kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=ticket-api,version=green -n ticket-prod --timeout=120s
   ```
4. Test Green privately via preview port-forward:
   ```bash
   kubectl port-forward svc/ticket-api-preview-svc 8001:8000 -n ticket-prod &
   curl -s http://localhost:8001/actuator/health
   # Must return: {"status":"UP"}
   kill %1
   ```
5. Cut over live production traffic with **0ms latency**:
   ```bash
   ./blue-green/switch.sh green
   ```
6. Free server RAM by scaling Blue down to 0:
   ```bash
   kubectl scale deployment ticket-api-blue -n ticket-prod --replicas=0
   ```

---

## 3. Redeploying the Frontend Admin (`ticket-admin`)

Follow these steps when modifying Vue.js 3 components, routes, or assets in `ticket-admin/`:

### Step 3.1: Build & Push Image (Local Machine)
From your local project root:
```bash
cd ticket-admin

TAG="1.0.1"
DOCKER_USER="<YOUR_DOCKER_USERNAME>"

# Build Vue SPA container image
docker build --platform linux/amd64 \
  --build-arg VITE_ENABLE_MOCK_DATA=false \
  -t ${DOCKER_USER}/ticket-admin:${TAG} .

# Push to registry
docker push ${DOCKER_USER}/ticket-admin:${TAG}
```

---

### Step 3.2: Trigger Rollout on EC2 Server
On your EC2 server:
```bash
kubectl set image deployment/ticket-admin ticket-admin=${DOCKER_USER}/ticket-admin:1.0.1 -n ticket-prod
```
*(Because Nginx has an ultra-lightweight footprint (~32 MiB RAM), this rollout completes in less than 5 seconds).*

Track rollout status:
```bash
kubectl rollout status deployment/ticket-admin -n ticket-prod
```

Verify in browser:
Open `https://ticket-admin.menglanghuo.online` (or your configured domain) and perform a hard refresh (`Ctrl + Shift + R` or `Cmd + Shift + R`) to bypass browser asset cache.

---

## 4. Redeploying When Configuration or Secrets Change

When non-code configurations change, containers must be refreshed to reload the new environment variables.

### Scenario A: Updating Non-Sensitive Config (`01-configmap.yaml`)
1. Edit `k8s/01-configmap.yaml` on the server or workstation.
2. Apply the ConfigMap:
   ```bash
   kubectl apply -f 01-configmap.yaml
   ```
3. Trigger a zero-downtime rolling restart of affected services:
   ```bash
   kubectl rollout restart deployment/ticket-api -n ticket-prod
   ```
4. Confirm pods reloaded the updated values:
   ```bash
   kubectl exec -it deployment/ticket-api -n ticket-prod -- env | grep -E 'SERVER_PORT|SPRING_'
   ```

---

### Scenario B: Rotating Production Secrets (`02-secrets.yaml`)
1. Edit `k8s/02-secrets.yaml` with your new credentials (e.g. rotated DB password or API secret).
2. Apply the secret:
   ```bash
   kubectl apply -f 02-secrets.yaml
   ```
3. Perform rolling restarts:
   ```bash
   kubectl rollout restart deployment/ticket-api -n ticket-prod
   ```
4. Verify application connectivity to MongoDB:
   ```bash
   kubectl logs deployment/ticket-api -n ticket-prod --tail=50 | grep -i "mongodb"
   ```

---

### Scenario C: Updating Ingress Routing or Domains (`06-ingress.yaml`)
If you change domains or path routes in `06-ingress.yaml`:
```bash
kubectl apply -f 06-ingress.yaml
```
Ingress rules update dynamically in Traefik within seconds. **No pod restart is required.**

---

## 5. One-Command Automation via `deploy.sh`

The repository provides the `k8s/deploy.sh` CLI for everyday management:

```bash
cd ~/mono-ticket/k8s

# 1. Re-apply all manifests (Config, Secrets, DB, API, Admin, Ingress, HPA)
./deploy.sh deploy

# 2. Trigger zero-downtime rolling restart of all applications
./deploy.sh restart

# 3. Check live status of all cluster pods, services, ingress, and HPA
./deploy.sh status
```

---

## 6. Live Rollout Verification & Health Checks

Always verify health after every deployment:

### 1. Check Pod Status and Restart Counts
```bash
kubectl get pods -n ticket-prod -o wide
```
Look for:
- `STATUS = Running`
- `READY = 1/1`
- `RESTARTS = 0`

### 2. Tail Live Application Logs
```bash
# Backend Spring Boot logs
kubectl logs -f deployment/ticket-api -n ticket-prod --tail=100

# Frontend Nginx access/error logs
kubectl logs -f deployment/ticket-admin -n ticket-prod --tail=50
```

### 3. Query Internal Actuator Health
```bash
kubectl port-forward svc/ticket-api-svc 8000:8000 -n ticket-prod &
PF_PID=$!
curl -s http://localhost:8000/actuator/health
kill $PF_PID
```
Output must be: `{"status":"UP"}`.

---

## 7. Emergency Rollback Procedures

If a newly deployed release contains a fatal bug, crash loop, or regression, execute an immediate rollback:

### Rollback Strategy 1: Kubernetes Native Rollout Undo (Fastest)
Kubernetes records previous ReplicaSet revisions. You can roll back to the previous stable release with one command:

```bash
# Roll back backend API
kubectl rollout undo deployment/ticket-api -n ticket-prod

# Roll back frontend Admin
kubectl rollout undo deployment/ticket-admin -n ticket-prod
```

View previous revision history:
```bash
kubectl rollout history deployment/ticket-api -n ticket-prod
```

Roll back to a specific revision number (e.g. revision 2):
```bash
kubectl rollout undo deployment/ticket-api --to-revision=2 -n ticket-prod
```

---

### Rollback Strategy 2: Blue/Green Instant Revert
If running Blue/Green mode and Green fails smoke tests:
```bash
cd ~/mono-ticket/k8s/blue-green
./switch.sh blue
```
Live traffic switches back to the Blue container within **1 millisecond**.

---

## 8. Server Health & Disk Housekeeping

Each new image build leaves behind old Docker image layers on your EBS disk. Over time, this consumes disk space on a 30 GB volume.

### Clean Up Stale Images (Post-Deploy Routine)
Run this command after successful deployments:
```bash
sudo k3s crictl rmi --prune
```

### Verify Disk and Memory Availability
```bash
# Check free disk space
df -h /

# Check memory and swap usage
free -h
```
Ensure root filesystem (`/`) has at least **5 GB** free and Swap usage is under **50%**.
