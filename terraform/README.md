# Mono-Ticket — Terraform AWS Infrastructure Automation

Production-grade Infrastructure as Code (IaC) for provisioning the **Mono-Ticket** AWS single-node Kubernetes cluster. This configuration strictly implements the architecture and security specifications documented across [`docs/02_infrastructure_setup.md`](../docs/02_infrastructure_setup.md), configured with a **`t3.medium`** compute instance (2 vCPU, 4 GiB RAM) for enhanced stability and workload headroom.

---

## 🏛️ Provisioned Infrastructure Architecture

```text
               Public Internet Traffic (Web Browsers & Mobile Clients)
                                          │
                                          ▼
                         [ Domain Registrar DNS Records ]
                         ├── yourdomain.com      ──► Elastic IP
                         ├── www.yourdomain.com  ──► Elastic IP
                         └── api.yourdomain.com  ──► Elastic IP
                                          │
                                          ▼
                      [ AWS Custom VPC: mono-ticket-vpc ]
                           IPv4 CIDR: 10.0.0.0/16
                                          │
                                          ▼
                         [ AWS Internet Gateway (IGW) ]
                            Default Route: 0.0.0.0/0
                                          │
                                          ▼
                              [ Public Subnet 1 ]
                     (10.0.1.0/24 in Availability Zone A)
                                          │
                                          ▼
                    [ Security Group Firewall: k3s-web-sg ]
                    ├── Inbound:  22 (SSH), 80 (HTTP), 443 (HTTPS)
                    ├── Strict:   8000, 27017, 6443 NEVER opened publicly
                    └── Outbound: ALL Traffic 0.0.0.0/0
                                          │
                                          ▼
               [ AWS EC2 Instance: t3.medium (k3s-ticket-server) ]
               Ubuntu 24.04 LTS (x86_64) | 2 vCPU | 4 GB RAM + 2 GB Swap | 30 GB gp3
               Persistent Static Public IP: <YOUR_ELASTIC_IP>
                                          │
    ┌─────────────────────────────────────┴─────────────────────────────────────┐
    │ K3s Lightweight Kubernetes Cluster (Automated via cloud-init user_data)   │
    │                                                                           │
    │  [ Host Ports 80 & 443: Traefik Ingress Controller ]                      │
    │   ├── Handles TLS Termination via Let's Encrypt Certificates             │
    │   ├── Routes HTTP to HTTPS via Traefik Middleware                         │
    │   └── Proxies traffic to internal ClusterIP Services                      │
    │                                                                           │
    │  [ Namespace: cert-manager ]                                              │
    │   └── Cert-Manager Controller v1.16.1 (ACME HTTP-01 Let's Encrypt)        │
    │                                                                           │
    │  [ Namespace: ticket-prod (Ready for k8s/ manifests) ]                   │
    │   ├── ticket-admin-svc:80   ──► Vue.js SPA Frontend (Nginx Proxy)         │
    │   ├── ticket-api-svc:8000   ──► Spring Boot WebFlux Backend (Java 25)     │
    │   └── mongo-svc:27017       ──► MongoDB 7.0 (Protected by NetworkPolicy)  │
    └───────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Features & Best Practices Implemented

1. **Custom Multi-AZ VPC (`10.0.0.0/16`)**:
   - 2 Public Subnets (`10.0.1.0/24`, `10.0.2.0/24`) across 2 AZs.
   - 2 Private Subnets (`10.0.10.0/24`, `10.0.20.0/24`) reserved for future RDS/DocumentDB database tier.
   - **$0 NAT Gateway Cost**: Deploys the compute node directly into Public Subnet 1 with an attached Elastic IP, avoiding ~$32.40/month AWS NAT Gateway charges.
2. **`t3.medium` EC2 Compute**:
   - 2 vCPUs and 4 GiB physical RAM with 30 GiB `gp3` root EBS storage (3,000 IOPS, 125 MB/s baseline).
   - Encrypted at rest via AWS KMS.
3. **Hardened Security Group (`k3s-web-sg`)**:
   - Inbound ports 22 (SSH), 80 (HTTP), and 443 (HTTPS) only.
   - Zero-trust isolation: internal service ports (8000 API, 27017 MongoDB, 6443 Kubernetes API) are strictly blocked from external public access.
4. **Elastic IP (EIP)**:
   - Allocates and associates a static public IPv4 address to preserve DNS and SSL ACME challenge mappings across reboots.
5. **Automated Zero-Touch Bootstrap (`user_data.sh.tftpl`)**:
   - Packages upgrade and dependencies installation.
   - 2 GB swap space creation on root disk and kernel swappiness tuned to `10`.
   - Host `ufw` firewall disabled to ensure Flannel CNI packet forwarding.
   - Automated K3s installation (`--disable metrics-server --write-kubeconfig-mode 644`).
   - Automated `kubectl` config setup for the `ubuntu` user.
   - Automated Cert-Manager `v1.16.1` installation.
6. **Key Pair & SSM Support**:
   - Auto-generates an RSA 4096-bit private key file (`k3s-ticket-key.pem` with `chmod 0400`) if no existing key is specified.
   - Attaches `AmazonSSMManagedInstanceCore` IAM instance profile allowing browser/CLI terminal access via AWS Systems Manager.
7. **Optional Route 53 DNS Automation**:
   - Configurable creation of `@`, `www`, and `api` A records if your domain is managed in Route 53.

---

## 🚀 Quick Start Guide

### Step 1: Prerequisites
Ensure you have the following installed locally:
- [Terraform](https://developer.hashicorp.com/terraform/install) (>= 1.5.0)
- [AWS CLI](https://aws.amazon.com/cli/) configured with valid AWS credentials (`aws configure`)

### Step 2: Initialize Terraform
Navigate to the `terraform/` directory:
```bash
cd terraform
terraform init
```

### Step 3: Customize Variables (Optional)
Create your local variable definition file:
```bash
cp terraform.tfvars.example terraform.tfvars
```
Edit `terraform.tfvars` if you wish to adjust the region, restrict SSH CIDR blocks, or configure Route 53.

### Step 4: Preview and Apply
```bash
terraform plan
terraform apply
```
Type `yes` when prompted.

Upon successful completion, Terraform outputs:
- The static **Elastic IP**
- The **SSH command** to log into the instance
- The **AWS SSM Session command**
- The **Kubeconfig fetch command**
- The **DNS A records** required for your domain

---

## 🔑 Accessing Your Cluster

### 1. SSH Terminal Access
If you allowed Terraform to generate the key pair, `k3s-ticket-key.pem` was automatically created in the repository root:
```bash
# From repository root
ssh -i "k3s-ticket-key.pem" ubuntu@<YOUR_ELASTIC_IP>
```

### 2. AWS Systems Manager (SSM) Session Manager (No SSH key required)
```bash
aws ssm start-session --target <INSTANCE_ID> --region us-east-1
```

### 3. Fetching Kubeconfig to Local Machine
To manage the cluster using your local `kubectl`:
```bash
ssh -i "k3s-ticket-key.pem" ubuntu@<YOUR_ELASTIC_IP> "sudo cat /etc/rancher/k3s/k3s.yaml" | sed "s/127.0.0.1/<YOUR_ELASTIC_IP>/g" > ~/.kube/mono-ticket-config
export KUBECONFIG=~/.kube/mono-ticket-config
kubectl get nodes
```

---

## 🌐 Configuring Domain DNS

Go to your domain registrar (Namecheap, Cloudflare, Porkbun, GoDaddy) and add the following 3 `A` records pointing to your Elastic IP:

| Record Type | Host / Name | Value | TTL | Target Service |
|:---|:---|:---|:---|:---|
| **A** | `@` | `<YOUR_ELASTIC_IP>` | 300 | Vue.js Admin Frontend |
| **A** | `www` | `<YOUR_ELASTIC_IP>` | 300 | Vue.js Admin Frontend |
| **A** | `api` | `<YOUR_ELASTIC_IP>` | 300 | Spring Boot WebFlux API |

*(Note: If using Cloudflare, set proxy status to **DNS Only** initially so Let's Encrypt ACME HTTP-01 challenges succeed).*

---

## 📦 Deploying Mono-Ticket Application Manifests

Once the server is booted and DNS is resolving:
1. SSH into the server:
   ```bash
   ssh -i "k3s-ticket-key.pem" ubuntu@<YOUR_ELASTIC_IP>
   ```
2. Clone or copy your repository manifests into `~/mono-ticket/k8s/`.
3. Configure `k8s/02-secrets.yaml` and `k8s/06-ingress.yaml` with your credentials and domain name (see [`docs/03_deployment_guide.md`](../docs/03_deployment_guide.md)).
4. Deploy the stack:
   ```bash
   cd ~/mono-ticket/k8s
   ./deploy.sh deploy
   ```

---

## 🧹 Teardown / Destroy

To completely destroy all created AWS resources:
```bash
cd terraform
terraform destroy
```
