# Kubernetes Blue/Green Deployment Guide — Ticket API

This directory provides the manifests and automation script for zero-downtime **Blue/Green Deployments** for `ticket-api`.

---

## 🎯 How Blue/Green Deployment Works

```text
       Live Production Traffic (Ingress / Admin UI)
                            │
                            ▼
               [ Service: ticket-api-svc ]
                 selector: version=blue
                  │                   │
                  ▼ (100% Traffic)    ▼ (0% Traffic)
       ┌─────────────────────┐     ┌─────────────────────┐
       │   ticket-api-blue   │     │   ticket-api-green  │
       │     (v1.0.0 Live)   │     │ (v1.0.1 Candidate)  │
       └─────────────────────┘     └─────────────────────┘
```

1. **Blue** is running version `1.0.0` and receiving 100% of production traffic.
2. You deploy **Green** with version `1.0.1` (`04-ticket-api-green.yaml`).
3. You smoke-test Green in isolation via `ticket-api-preview-svc` or port-forwarding without affecting live users.
4. When ready, switch live traffic instantly (0ms) to Green via `./switch.sh green`.
5. If any critical bug occurs, rollback to Blue in 1 second via `./switch.sh blue`.
6. Once stable, scale down Blue to 0 replicas to conserve RAM on resource-constrained servers.

---

## 🚀 Step-by-Step Execution

### 1. Initial State: Deploy Blue & Service
```bash
kubectl apply -f 04-ticket-api-service.yaml
kubectl apply -f 04-ticket-api-blue.yaml
```

### 2. Deploy Candidate Release: Green
Update the image tag in `04-ticket-api-green.yaml` to your new version (e.g., `1.0.1`), then deploy:
```bash
kubectl apply -f 04-ticket-api-green.yaml
```

Wait until Green is ready:
```bash
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=ticket-api,version=green -n ticket-prod --timeout=120s
```

### 3. Test Green Privately Before Cutover
Port-forward directly to the preview service or Green pod:
```bash
kubectl port-forward svc/ticket-api-preview-svc 8001:8000 -n ticket-prod &
curl http://localhost:8001/actuator/health
# Expected: {"status":"UP"}
kill %1
```

### 4. Atomic Cutover to Green
Run the switcher script:
```bash
./switch.sh green
```
*(Or via raw kubectl: `kubectl patch service ticket-api-svc -n ticket-prod -p '{"spec":{"selector":{"version":"green"}}}'`)*.

### 5. Instant Rollback (If needed)
If you see errors in production logs:
```bash
./switch.sh blue
```

### 6. Resource Cleanup (Recommended for t3.micro)
On 1 GB RAM instances, running both Blue and Green simultaneously uses ~400–500 MB RAM. After Green is stable, scale down Blue to 0:
```bash
kubectl scale deployment ticket-api-blue -n ticket-prod --replicas=0
```
When preparing the next release, scale it back up with the new tag!
