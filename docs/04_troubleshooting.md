# Mono-Ticket — Troubleshooting Guide

> When things go wrong. Symptoms → Diagnosis → Fix.

---

## 1. Pod Issues

### CrashLoopBackOff
**Symptom:** Pod keeps restarting every few seconds.

```bash
# Diagnosis: Check the logs
kubectl logs <pod-name> -n ticket-prod
kubectl logs <pod-name> -n ticket-prod --previous   # Logs from the crashed container

# Common causes:
# - Spring Boot can't connect to MongoDB → check mongo-svc is running
# - Missing environment variables → check configmap/secrets
# - JVM OOM → check JAVA_TOOL_OPTIONS in 04-ticket-api.yaml
```

**Fix:** Read the error in the logs and address the root cause. Most common: MongoDB not ready yet (deploy order).

---

### ImagePullBackOff
**Symptom:** Pod stuck, can't download the Docker image.

```bash
# Diagnosis
kubectl describe pod <pod-name> -n ticket-prod | grep -A 5 "Events"

# Common causes:
# - Wrong image name/tag
# - Private repo without regcred secret
# - Docker Hub rate limit (anonymous: 100 pulls/6h)
```

**Fix:**
```bash
# Check image name is correct in the manifest
# If private repo, create pull secret:
kubectl create secret docker-registry regcred \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<USER> \
  --docker-password=<TOKEN> \
  -n ticket-prod
```

---

### Pending
**Symptom:** Pod stuck in `Pending` state, never starts.

```bash
# Diagnosis
kubectl describe pod <pod-name> -n ticket-prod

# Look for:
# - "Insufficient memory" → reduce resource requests
# - "Insufficient cpu" → reduce CPU requests
# - "no persistent volumes available" → check PVC status
```

**Fix:** Reduce `resources.requests` in the deployment manifest, or check PVC:
```bash
kubectl get pvc -n ticket-prod
```

---

### OOMKilled
**Symptom:** Pod keeps getting killed with reason `OOMKilled`.

```bash
# Diagnosis
kubectl describe pod <pod-name> -n ticket-prod | grep -A 3 "Last State"

# The container exceeded its memory limit
```

**Fix:**
1. Check swap is active on EC2: `free -h`
2. For the API, verify `JAVA_TOOL_OPTIONS` is set with heap caps
3. Increase `resources.limits.memory` if you have headroom
4. Check if swap is configured: `swapon --show`

---

## 2. SSL / TLS Issues

### Certificate Not Ready

```bash
# Check certificate status
kubectl get certificate -n ticket-prod
kubectl describe certificate ticket-prod-tls-secret -n ticket-prod

# Check if challenges are pending
kubectl get challenges -n ticket-prod
kubectl describe challenge <challenge-name> -n ticket-prod
```

**Common causes & fixes:**

| Cause | Fix |
|:---|:---|
| Port 80 blocked | Open port 80 in AWS Security Group |
| DNS not pointing to EC2 | Check `nslookup yourdomain.com` resolves to EC2 IP |
| Cloudflare Orange Cloud | Switch to Grey Cloud (DNS Only) for HTTP-01 challenge |
| Wrong email in ClusterIssuer | Edit `07-cluster-issuer.yaml` with valid email |
| Rate limit exceeded | Wait 1 hour, Let's Encrypt has rate limits |

---

## 3. Networking Issues

### Service Not Resolving (DNS)

```bash
# Test DNS resolution from inside a pod
kubectl exec -it deployment/ticket-api -n ticket-prod -- sh
# Inside the pod:
nslookup mongo-svc
nslookup ticket-api-svc.ticket-prod.svc.cluster.local
```

**Fix:** Check service selector labels match pod labels:
```bash
kubectl get svc mongo-svc -n ticket-prod -o yaml | grep -A 5 selector
kubectl get pods -n ticket-prod --show-labels
```

---

### 502 Bad Gateway

**Symptom:** Nginx Ingress returns 502 when accessing the site.

```bash
# Check ingress controller logs
kubectl logs -f -n ingress-nginx -l app.kubernetes.io/component=controller

# Check if backend pods are ready
kubectl get pods -n ticket-prod
```

**Common causes:**
- Backend pod not ready yet (startup probe still running)
- Service port mismatch (check `targetPort` matches `containerPort`)
- Wrong service name in Ingress manifest

---

### 504 Gateway Timeout

```bash
# API is too slow to respond (common during JVM cold start)
# Increase timeouts in ingress annotation:
nginx.ingress.kubernetes.io/proxy-read-timeout: "120"
nginx.ingress.kubernetes.io/proxy-connect-timeout: "30"
```

---

## 4. Database Issues

### MongoDB Connection Refused

```bash
# Check if MongoDB pod is running
kubectl get pods -l app.kubernetes.io/name=mongodb -n ticket-prod

# Check MongoDB logs
kubectl logs deployment/mongo-db -n ticket-prod

# Test connection from API pod
kubectl exec -it deployment/ticket-api -n ticket-prod -- sh
curl mongo-svc:27017   # Should get a response (not a clean one, but not "refused")
```

**Fix:** Ensure MongoDB pod is `Running` and `Ready` before the API starts.

### MongoDB Data Lost After Pod Restart

```bash
# Check PVC is bound
kubectl get pvc -n ticket-prod

# If PVC status is "Pending", local-path provisioner may not be working
kubectl get storageclass
```

**Fix:** Ensure `storageClassName: local-path` is correct (K3s default). Data persists as long as the PVC exists, even if the pod restarts.

---

## 5. JVM Issues (Spring Boot on 1GB RAM)

### JVM Takes 60-90s to Start

This is **normal** on a `t3.micro`. The startup probe gives it up to 5 minutes (`failureThreshold: 30 × periodSeconds: 10`).

### GC Overhead / Slow Performance

```bash
# Check JVM memory from inside the pod
kubectl exec -it deployment/ticket-api -n ticket-prod -- sh
java -XshowSettings:vm -version 2>&1 | head -20
```

**Current JVM flags (tuned for 1GB RAM):**
```
-XX:+UseSerialGC        # Lowest memory overhead GC
-Xmx256m                # Max heap 256 MB
-Xms128m                # Initial heap 128 MB  
-XX:MaxMetaspaceSize=96m # Cap metadata
-XX:+UseContainerSupport # Respect K8s limits
```

### If You Upgrade to t3.small (2GB RAM)

Update `JAVA_TOOL_OPTIONS` in `04-ticket-api.yaml`:
```yaml
value: "-XX:+UseG1GC -Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseContainerSupport"
```
And increase limits:
```yaml
resources:
  requests:
    memory: "400Mi"
  limits:
    memory: "700Mi"
```

---

## 6. Memory Management

### Check System Memory

```bash
# On the EC2 server
free -h
# Shows RAM and Swap usage

# Check which processes use the most memory
ps aux --sort=-%mem | head -10
```

### Server Frozen / Unresponsive

If the instance becomes unresponsive:

1. **Wait 5 minutes** — the OOM killer may free memory and things recover
2. **If still frozen** — Go to AWS Console → EC2 → Select instance → **Instance State** → **Reboot instance**
3. **After reboot**, K3s and all pods auto-restart (may take 3-5 minutes)

### Reduce Memory Usage

```bash
# Delete non-essential pods temporarily
kubectl scale deployment ticket-admin --replicas=0 -n ticket-prod  # Stop frontend
kubectl scale deployment ticket-api --replicas=0 -n ticket-prod    # Stop API

# Bring them back
kubectl scale deployment ticket-admin --replicas=1 -n ticket-prod
kubectl scale deployment ticket-api --replicas=1 -n ticket-prod
```

---

## 7. Useful Debug Commands

| Task | Command |
|:---|:---|
| Pod status + events | `kubectl describe pod <name> -n ticket-prod` |
| Live logs (follow) | `kubectl logs -f <pod> -n ticket-prod` |
| Previous crash logs | `kubectl logs <pod> -n ticket-prod --previous` |
| Shell into pod | `kubectl exec -it <pod> -n ticket-prod -- sh` |
| All resources | `kubectl get all -n ticket-prod` |
| All events (sorted) | `kubectl get events -n ticket-prod --sort-by=.lastTimestamp` |
| Node resource usage | `kubectl describe node` |
| Ingress details | `kubectl describe ingress -n ticket-prod` |
| Certificate details | `kubectl describe certificate -n ticket-prod` |
| Check DNS from pod | `kubectl exec -it <pod> -n ticket-prod -- nslookup mongo-svc` |
| Port-forward to local | `kubectl port-forward svc/ticket-api-svc 8000:8000 -n ticket-prod` |
| Watch pods in real-time | `watch kubectl get pods -n ticket-prod` |

---

## 8. Nuclear Option — Full Reset

When everything is broken and you want to start fresh:

```bash
# Delete all application resources
kubectl delete -f k8s/

# Wait for everything to terminate
kubectl get pods -n ticket-prod --watch

# ⚠️  This also deletes the PVC (MongoDB data will be lost!)
kubectl delete namespace ticket-prod

# Redeploy from scratch
kubectl apply -f k8s/
```

### Full K3s Reset (Last Resort)

```bash
# Uninstall K3s completely
/usr/local/bin/k3s-uninstall.sh

# Reinstall K3s
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server \
  --disable traefik \
  --disable servicelb \
  --disable metrics-server \
  --write-kubeconfig-mode 644" sh -

# Reconfigure kubectl
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config

# Reinstall Nginx Ingress (Pure kubectl)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/baremetal/deploy.yaml
kubectl patch deployment ingress-nginx-controller -n ingress-nginx --patch '{"spec":{"template":{"spec":{"containers":[{"name":"controller","ports":[{"containerPort":80,"hostPort":80,"name":"http"},{"containerPort":443,"hostPort":443,"name":"https"}],"resources":{"requests":{"cpu":"50m","memory":"64Mi"},"limits":{"cpu":"250m","memory":"160Mi"}}}]}}}}'

# Reinstall cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.1/cert-manager.yaml

# Redeploy app
kubectl apply -f k8s/
```
