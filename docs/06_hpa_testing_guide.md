# Mono-Ticket — Horizontal Pod Autoscaler (HPA) Deep Dive, Real-World Scenarios & CLI Testing Guide

> **DevOps & SRE Operational Standard**: Complete architectural guide explaining how `k8s/09-ticket-api-hpa.yaml` works, the mathematical autoscaling algorithm, real-world traffic scenarios, prerequisites (Metrics-Server), and step-by-step CLI stress testing procedures to verify scaling from 1 to 2 instances and automatic cooldown on AWS EC2 single-node Kubernetes.

---

## 🧠 1. How the Horizontal Pod Autoscaler (HPA) Works

### 1.1 Architectural Control Loop

The Horizontal Pod Autoscaler is a built-in Kubernetes controller that continuously monitors resource usage and dynamically adjusts replica counts:

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ Kube-Controller-Manager (HPA Control Loop: Runs every 15 seconds)           │
│                                                                             │
│   1. Queries Metrics-Server API:                                            │
│      "What is the average CPU & Memory of ticket-api pods?"                 │
│                                │                                            │
│                                ▼                                            │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │ Metrics-Server (Scrapes cgroup metrics from Kubelet every 15s)       │  │
│   │ - ticket-api-pod-1: CPU = 120m (80%), Memory = 152Mi (76%)           │  │
│   └──────────────────────────────────────────────────────────────────────┘  │
│                                │                                            │
│   2. Evaluates against Target (CPU Target = 50%, Memory Target = 80%):      │
│      Current CPU (80%) > Target CPU (50%) ──► Scale-Up Triggered!           │
│                                │                                            │
│   3. Calculates Desired Replicas via Algorithm:                             │
│      desiredReplicas = ceil[ 1 * (80 / 50) ] = ceil[1.6] = 2                │
│                                │                                            │
│   4. Patches Deployment Spec:                                               │
│      Deployment/ticket-api replicas: 1 ──► 2                                │
│                                │                                            │
│   5. Kube-Scheduler schedules Pod-2 on Node; Service balances traffic!      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 1.2 The Core Mathematical Scaling Formula

Kubernetes uses the following deterministic equation:

$$\text{desiredReplicas} = \left\lceil \text{currentReplicas} \times \left( \frac{\text{currentMetricValue}}{\text{targetMetricValue}} \right) \right\rceil$$

Where:
- $\lceil \dots \rceil$ denotes the ceiling function (rounds up to nearest integer).
- $\text{targetMetricValue}$ is defined in `09-ticket-api-hpa.yaml`:
  - **CPU**: `50%` of `requests.cpu` ($150\text{m} \times 0.50 = 75\text{m}$).
  - **Memory**: `80%` of `requests.memory` ($200\text{Mi} \times 0.80 = 160\text{Mi}$).

#### Example Calculation:
1. `ticket-api` is running **1 pod**. Baseline CPU is `15m` ($10\%$).
2. Traffic hits. CPU usage climbs to `110m` ($73.3\%$).
3. The HPA controller evaluates:
   $$\text{desiredReplicas} = \left\lceil 1 \times \left( \frac{73.3\%}{50\%} \right) \right\rceil = \lceil 1.466 \rceil = 2$$
4. Since `maxReplicas: 2` allows it, the controller immediately creates the 2nd pod!

---

### 1.3 Deep Dive into `k8s/09-ticket-api-hpa.yaml`

Here is the exact production configuration:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ticket-api-hpa
  namespace: ticket-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ticket-api
  minReplicas: 1
  maxReplicas: 2
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 50
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 30
```

#### Why These Parameters Were Chosen:
1. **`minReplicas: 1`, `maxReplicas: 2`**:
   On a single-node AWS Free Tier host (`t3.micro` with 1 vCPU and 1 GB RAM), running 2 Java Spring Boot pods is the safe upper ceiling. Scaling beyond 2 pods would exhaust CPU credits and cause swap thrashing.
2. **`averageUtilization: 50` (CPU)**:
   Because the pod requests `150m` CPU, 50% is only **75m CPU**. Spring Boot at idle uses ~10m. Under light load, it quickly uses 90m–150m. This makes triggering and testing the autoscale straightforward without needing an enterprise-scale load generator.
3. **`scaleUp.stabilizationWindowSeconds: 0`**:
   Zero delay for scale-up. As soon as a traffic surge hits, the second pod starts launching immediately (within the 15-second controller poll cycle).
4. **`scaleDown.stabilizationWindowSeconds: 60`**:
   The default Kubernetes scale-down delay is 300 seconds (5 minutes). For fast development and testing turnaround, 60 seconds ensures rapid, observable recovery back to 1 pod without prolonged waiting.

---

### 1.4 Why Can a 1 GB RAM Node Safely Run 2 Pods?

| Component | Request | Limit | Under Peak Load (2 Pods) |
| :--- | :--- | :--- | :--- |
| **ticket-api Pod 1** | 200 MiB | 400 MiB | ~220 MiB |
| **ticket-api Pod 2** | 200 MiB | 400 MiB | ~220 MiB |
| **MongoDB 7.0** | 64 MiB | 200 MiB | ~120 MiB |
| **Vue SPA (Nginx)** | 32 MiB | 96 MiB | ~32 MiB |
| **K3s + OS Baseline** | ~220 MiB | ~350 MiB | ~260 MiB |
| **Total Memory Required** | **~716 MiB** | **~1446 MiB** | **~852 MiB** |

**The Safety Buffer:**
- Physical RAM: **1024 MiB**
- Linux Swap File: **2048 MiB** (`/swapfile` configured in [02_infrastructure_setup.md](./02_infrastructure_setup.md))
- Total Addressable Memory: **3072 MiB**
- Even with 2 JVMs running concurrently, total memory stays around ~850–950 MiB. Physical RAM + swap handles this with zero risk of Linux kernel OOM panics.

---

## 🔌 2. Prerequisites: Checking & Enabling Metrics-Server

HPA cannot function without **Metrics-Server**. Before testing, verify it is operational.

### Step 2.1: Check Current Metrics Availability
Run on your EC2 host:
```bash
kubectl top nodes
kubectl top pods -n ticket-prod
```

If it returns real numbers (e.g. `ticket-api-... 12m 168Mi`), **Metrics-Server is ready!** Skip to Section 4.

---

### Step 2.2: Re-Enabling Metrics-Server on K3s (If Disabled)
If K3s was originally installed with `--disable metrics-server`, `kubectl top` will return:
`error: Metrics API not available`.

To re-enable it in 30 seconds:
```bash
# 1. Remove the disable flag from K3s systemd unit:
sudo sed -i 's/--disable metrics-server//g' /etc/systemd/system/k3s.service

# 2. Reload and restart K3s:
sudo systemctl daemon-reload
sudo systemctl restart k3s

# 3. Wait 30 seconds, then verify the metrics-server pod in kube-system:
kubectl get pods -n kube-system -l k8s-app=metrics-server
```
Expected output:
```text
NAME                              READY   STATUS    RESTARTS   AGE
metrics-server-587b667b55-v4b9d   1/1     Running   0          30s
```

---

## 🎯 3. Four Real-World Autoscaling Scenarios

### Scenario 1: Baseline Idle Traffic (Day-to-Day Operations)
- **Traffic**: Regular administrative usage (a few requests per minute).
- **Cluster State**: 1 replica of `ticket-api`.
- **Metrics**: CPU ~8m (5%), Memory ~160Mi (80%).
- **HPA Decision**: Target is not exceeded. Replicas remain at `1`.

```text
[ Users / Admin ] ──► [ Traefik ] ──► [ ticket-api Pod 1 ] ──► [ MongoDB ]
```

---

### Scenario 2: Traffic Spike / User Surge (Scale-Up Event)
- **Traffic**: Bulk ticket imports, marketing email blast, or high concurrent logins.
- **Cluster State**: CPU rises to 90m–150m (>50% target).
- **HPA Decision**: Controller detects threshold breach and scales to `2` replicas.
- **Traffic Routing**: Traefik and `ticket-api-svc` automatically round-robin requests between Pod 1 and Pod 2.

```text
                                        ┌──► [ ticket-api Pod 1 ] (50% traffic) ──┐
[ Traffic Burst ] ──► [ ticket-api-svc ]┤                                         ├──► [ MongoDB ]
                                        └──► [ ticket-api Pod 2 ] (50% traffic) ──┘
```

---

### Scenario 3: Traffic Subsides (Cooldown & Scale-Down)
- **Traffic**: Surge passes; traffic returns to normal.
- **Cluster State**: CPU drops back to 10m (6%).
- **HPA Decision**: Controller starts the 60-second cooldown timer. After 60 seconds of consistently low metrics, Pod 2 is gracefully terminated. Replicas return to `1`.

---

### Scenario 4: Pod Crash Under Load (Self-Healing + HPA)
- **Scenario**: If Pod 1 runs out of resources or encounters an unhandled runtime error while handling traffic.
- **Cluster State**: `readinessProbe` fails (`/actuator/health`). Service instantly removes Pod 1 from endpoints.
- **HPA & Deployment Reaction**: All traffic redirects to Pod 2. Deployment controller restarts Pod 1. Zero downtime for end users.

---

## 🧪 4. Hands-on CLI Testing Guide

Here are four practical ways to stress-test the HPA from the command line.

### Step 4.1: Open Monitoring Terminals
Before running the test, open two terminal sessions on your EC2 host:

**Terminal 1 (Watch HPA metrics in real-time):**
```bash
kubectl get hpa ticket-api-hpa -n ticket-prod -w
```

**Terminal 2 (Watch Pod replicas scaling):**
```bash
kubectl get pods -l app.kubernetes.io/name=ticket-api -n ticket-prod -w
```

---

### Step 4.2: Execute Load Test (Choose 1 of 4 Options)

#### Option A: Ephemeral In-Cluster Load Generator (Recommended — No tools required)
This launches a temporary BusyBox container inside your Kubernetes cluster that queries the internal API service in a fast loop:

```bash
kubectl run load-generator --rm -i --tty \
  --image=busybox:1.36 \
  --restart=Never \
  -n ticket-prod \
  -- /bin/sh -c "while true; do wget -q -O- http://ticket-api-svc:8000/actuator/health; done"
```

> **Why this is best**: Runs directly inside K3s, avoids public internet latency, and automatically deletes itself when you press `Ctrl + C`.

---

#### Option B: Apache Bench (`ab`) from Local Workstation or Server
Send 10,000 HTTP requests with a concurrency of 20 against your public API domain:

```bash
ab -n 10000 -c 20 https://ticket-api.menglanghuo.online/actuator/health
```

---

#### Option C: `hey` Modern HTTP Benchmark
If you have `hey` installed:

```bash
hey -n 8000 -c 25 -q 50 https://ticket-api.menglanghuo.online/actuator/health
```

---

#### Option D: Pure Bash Parallel `curl` Worker Loop
No third-party binaries required. Run this in bash:

```bash
for i in {1..3000}; do
  curl -s -o /dev/null -w "%{http_code}\n" https://ticket-api.menglanghuo.online/actuator/health &
  if (( i % 30 == 0 )); then sleep 0.05; fi
done
wait
```

---

## 📊 5. What You Will See in the Terminal (Expected Output)

### Phase 1: Baseline Idle State
In Terminal 1 (HPA Watch):
```text
NAME             REFERENCE               TARGETS                      MINPODS   MAXPODS   REPLICAS   AGE
ticket-api-hpa   Deployment/ticket-api   cpu: 6%/50%, mem: 72%/80%    1         2         1          10m
```

### Phase 2: Traffic Spike Detected (Scale-Up Event)
After running the load generator for ~15 seconds:
```text
ticket-api-hpa   Deployment/ticket-api   cpu: 86%/50%, mem: 75%/80%   1         2         1          10m15s
ticket-api-hpa   Deployment/ticket-api   cpu: 104%/50%, mem: 77%/80%  1         2         2          10m30s
```

In Terminal 2 (Pod Watch):
```text
NAME                          READY   STATUS              RESTARTS   AGE
ticket-api-5c74f76f7b-9x2mk   1/1     Running             0          25m
ticket-api-5c74f76f7b-h4b8q   0/1     ContainerCreating   0          0s
ticket-api-5c74f76f7b-h4b8q   0/1     Running             0          12s
ticket-api-5c74f76f7b-h4b8q   1/1     Running             0          42s
```
> Notice that Pod 2 passes its `startupProbe` and `readinessProbe` and transitions to `1/1 Running`. The workload is now load-balanced across 2 instances!

### Phase 3: Cooldown & Automatic Scale-Down
Stop your load generator (press `Ctrl + C` in Terminal 3).

1. In Terminal 1, CPU drops back to baseline:
   ```text
   ticket-api-hpa   Deployment/ticket-api   cpu: 5%/50%, mem: 73%/80%    1         2         2          12m
   ```
2. The 60-second stabilization window elapses.
3. HPA issues scale-down:
   ```text
   ticket-api-hpa   Deployment/ticket-api   cpu: 5%/50%, mem: 72%/80%    1         2         1          13m
   ```
4. In Terminal 2, Pod 2 is cleanly terminated:
   ```text
   ticket-api-5c74f76f7b-h4b8q   1/1     Terminating   0          3m
   ticket-api-5c74f76f7b-h4b8q   0/1     Terminating   0          3m15s
   ```

---

## 🔍 6. Inspecting HPA Events & Detailed Metrics

To inspect internal controller decisions, run:

```bash
kubectl describe hpa ticket-api-hpa -n ticket-prod
```

### Key Output Sections to Understand:
```text
Name:                                                  ticket-api-hpa
Namespace:                                             ticket-prod
Reference:                                             Deployment/ticket-api
Metrics:
  ( current / target )
  resource cpu on pods  (as a percentage of request):  6% (9m) / 50%
  resource memory on pods  (as a percentage of request): 74% (148Mi) / 80%
Min replicas:                                          1
Max replicas:                                          2
Deployment pods:                                       1 current / 1 desired
Conditions:
  Type            Status  Reason               Message
  ----            ------  ------               -------
  AbleToScale     True    ReadyForNewScale     recommended size matches current size
  ScalingActive   True    ValidMetricFound     the HPA was able to successfully calculate a replica count
  ScalingLimited  False   DesiredWithinRange   the desired count is within the acceptable range
Events:
  Type    Reason             Age   From                       Message
  ----    ------             ----  ----                       -------
  Normal  SuccessfulRescale  2m    horizontal-pod-autoscaler  New size: 2; reason: cpu resource utilization (percentage of request) above target
  Normal  SuccessfulRescale  1m    horizontal-pod-autoscaler  New size: 1; reason: All metrics below target
```

---

## 🛠️ 7. Troubleshooting Common HPA Issues

| Symptom | Probable Cause | Solution |
| :--- | :--- | :--- |
| `TARGETS: <unknown>/50%` | `metrics-server` is not installed or pod just started | Verify `kubectl top pods -n ticket-prod`. If failing, re-enable Metrics-Server via Section 2.2 and wait 30 seconds. |
| HPA never triggers scale-up | Load test traffic is too low or hitting wrong endpoint | Use Option A (In-Cluster BusyBox generator) which generates ~80 requests/sec and easily pushes CPU > 50%. |
| Pod 2 stays in `Pending` | Node CPU/Memory requests exceeded | Verify `kubectl describe node \| grep -A 10 "Allocated resources"`. Free Tier limits are respected if following mono-ticket manifest limits. |
| Memory metric stays at ~75% | JVM heap allocation stays allocated | This is normal Java JVM behavior. Netty/JVM retains allocated heap until GC. Scale-up/down is predominantly driven by dynamic CPU spikes. |
| Scale-down takes too long | Cooldown stabilization window is running | The manifest defines `stabilizationWindowSeconds: 60`. Wait at least 60 seconds after stopping traffic for scale-down to initiate. |

---

## 📋 Quick Command Cheat Sheet

```bash
# View HPA summary
kubectl get hpa -n ticket-prod

# Watch HPA live metrics
kubectl get hpa ticket-api-hpa -n ticket-prod -w

# View detailed HPA controller events
kubectl describe hpa ticket-api-hpa -n ticket-prod

# Inspect live pod resource consumption
kubectl top pods -n ticket-prod

# Launch temporary load generator pod
kubectl run load-generator --rm -i --tty --image=busybox:1.36 --restart=Never -n ticket-prod -- /bin/sh -c "while true; do wget -q -O- http://ticket-api-svc:8000/actuator/health; done"

# Check metrics-server pod status
kubectl get pods -n kube-system -l k8s-app=metrics-server
```
