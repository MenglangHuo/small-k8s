# Mono-Ticket — Requirements & Prerequisites

> Everything you need **before** you touch AWS or Kubernetes.

---

## 1. Application Stack Overview

| Component | Technology | Port | Image Base |
|:---|:---|:---|:---|
| **Backend API** | Spring Boot 4.1 WebFlux (Java 25, Gradle) | `8000` | `eclipse-temurin:21-jre-alpine` |
| **Frontend SPA** | Vue.js 3 + Vite + Tailwind CSS 4 | `80` (nginx) | `nginx:1.27-alpine` |
| **Database** | MongoDB 7.0 (Reactive) | `27017` | `mongo:7.0` |

---

## 2. AWS Account Requirements

| Requirement | Details |
|:---|:---|
| AWS Account | Free Tier eligible (12-month free tier for new accounts) |
| Billing Alert | **Set up a billing alarm** at \$5 threshold — safety net |
| Region | `us-east-1` (N. Virginia) has the widest Free Tier coverage |

---

## 3. EC2 Instance Specification

| Spec | Value | Notes |
|:---|:---|:---|
| **Instance Type** | `t3.micro` or `t2.micro` | Free Tier: 750 hrs/month for 12 months |
| **vCPU** | 1 | Shared, burstable |
| **RAM** | 1 GB | We add 2 GB swap to survive |
| **AMI** | Ubuntu Server 24.04 LTS (x86_64) | HVM, SSD Volume Type |
| **Storage** | 30 GiB gp3 EBS | Free Tier includes up to 30 GB |
| **Elastic IP** | 1 (optional, recommended) | Free when attached to a running instance |

---

## 4. Software Prerequisites

### On EC2 (installed during setup)
| Software | Purpose |
|:---|:---|
| **K3s** | Lightweight Kubernetes distribution (includes `kubectl`) |
| **curl, wget, git** | Standard Linux system utilities |

> **💡 Note on Helm:** For this initial basic setup, we use **100% pure `kubectl` and standard YAML manifests**—no Helm required! Helm packaging and charts will be introduced in the advanced branch (`k8s/helm`).

### On Your Local Machine
| Software | Purpose |
|:---|:---|
| **Docker Desktop** | Build container images locally |
| **SSH client** | Connect to EC2 (built-in on Mac/Linux, use PuTTY on Windows) |
| **Git** | Clone your repository |
| **kubectl** (optional) | Remote cluster management from your laptop |

---

## 5. Docker Registry Account

You need a container registry to push images from your local machine and pull them on EC2.

| Registry | URL | Notes |
|:---|:---|:---|
| Docker Hub | `docker.io` | Free: unlimited public repos, 1 private repo |
| GitHub Container Registry | `ghcr.io` | Free: unlimited for public repos |

---

## 6. Domain & DNS (Optional but Recommended)

| Requirement | Details |
|:---|:---|
| **Domain name** | Any registrar: Namecheap, Cloudflare, Porkbun, GoDaddy |
| **DNS A records** | Point `@`, `www`, `api` to your EC2 Elastic IP |
| **SSL** | Free via Let's Encrypt + cert-manager (requires domain) |

> **Without a domain**: You can still access your app via EC2 public IP — just skip the Ingress TLS and cert-manager setup.

---

## 7. Network / Security Group Rules

| Rule | Protocol | Port | Source | Why |
|:---|:---|:---|:---|:---|
| SSH | TCP | `22` | Your IP | Admin access |
| HTTP | TCP | `80` | `0.0.0.0/0` | Web traffic + Let's Encrypt challenge |
| HTTPS | TCP | `443` | `0.0.0.0/0` | Production SSL traffic |

> **⚠️ Do NOT open these ports to the public:**
> - `8000` (API) — Nginx Ingress routes internally
> - `27017` (MongoDB) — Only accessed inside the cluster
> - `6443` (K8s API) — Only needed from the server itself

---

## 8. Application Credentials (Have These Ready)

| Credential | Where Used | Required? |
|:---|:---|:---|
| AWS S3 Access Key & Secret | Backend — file storage | Yes, if using S3 features |
| S3 Bucket Names (private + static) | Backend — file upload/download | Yes, if using S3 |
| Telegram Bot Token | Backend — notification integration | Optional |
| Telegram Chat ID | Backend — notification target | Optional |
| Docker Hub / GHCR credentials | K8s image pull secret | Only if private repos |

---

## 9. Resource Budget (1 GB RAM + 2 GB Swap)

This is a **tight** fit. Here's the expected memory breakdown:

| Component | Requests | Limits | Notes |
|:---|:---|:---|:---|
| K3s control plane | ~100 Mi | ~200 Mi | Lightweight, but always running |
| Nginx Ingress Controller | 64 Mi | 160 Mi | Handles all HTTP/HTTPS routing |
| MongoDB 7.0 | 64 Mi | 200 Mi | Lean config for dev/prod |
| Spring Boot API (JVM) | 200 Mi | 400 Mi | SerialGC + capped heap, largest consumer |
| Vue.js SPA (Nginx) | 32 Mi | 96 Mi | Just serves static files |
| cert-manager | ~40 Mi | ~80 Mi | Background SSL manager |
| **OS + buffers** | ~100 Mi | — | Linux kernel, systemd |
| **Total** | **~600 Mi** | **~1136 Mi** | Fits in 1 GB RAM + 2 GB swap |

> **⚠️ Critical**: The JVM is the biggest memory consumer. The `JAVA_TOOL_OPTIONS` flags in the K8s manifest cap it to prevent OOM kills. Never remove them on a micro instance.

---

## 10. Skills & Knowledge Prerequisites

| Skill | Level | Why |
|:---|:---|:---|
| **Linux terminal** | Basic | SSH, file editing, running commands |
| **Docker** | Basic | Building images, docker login, docker push |
| **Git** | Basic | Clone, pull, push |
| **YAML** | Basic | K8s manifests are all YAML |
| **Kubernetes** | Beginner | This guide teaches you step-by-step |
| **Networking** | Basic | DNS, ports, HTTP/HTTPS concepts |

---

## Next Steps

Once you have all prerequisites ready, proceed to:
→ [02_infrastructure_setup.md](./02_infrastructure_setup.md) — Set up your EC2 instance and K3s cluster
