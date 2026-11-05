# Mono-Ticket — Modular Terraform AWS Infrastructure

Production-grade, reusable **Modular Infrastructure as Code (IaC)** for provisioning **Mono-Ticket** AWS Kubernetes clusters across multiple environments (`dev` and `prod`).

---

## 📁 Modular Directory Structure

```text
terraform/
├── modules/
│   ├── vpc/                     # Reusable VPC module (1 Public Subnet, 1 Private Subnet, $0 NAT)
│   │   ├── main.tf              # VPC, Subnets, Internet Gateway, Route Tables
│   │   ├── variables.tf         # Subnet CIDR blocks, VPC name, availability zone
│   │   └── outputs.tf           # VPC ID, Subnet IDs, IGW ID
│   └── k3s_cluster/             # Reusable K3s compute & security module
│       ├── main.tf              # Security group, IAM (SSM), EC2 instance, Elastic IP, Route 53
│       ├── variables.tf         # Instance type, storage, SSH keys, cluster flags
│       ├── outputs.tf           # Elastic IP, instance ID, SSH & Kubeconfig commands
│       └── templates/
│           └── user_data.sh.tftpl # Cloud-init script (Swap, K3s, Cert-Manager)
└── environments/
    ├── dev/                     # Development environment (t3.micro, cost-optimized)
    │   ├── main.tf              # Calls modules with small instance (t3.micro)
    │   ├── variables.tf         # Dev inputs & settings
    │   ├── outputs.tf           # Dev output helpers
    │   └── terraform.tfvars     # Dev variable overrides
    └── prod/                    # Production environment (t3.large / t3.medium)
        ├── main.tf              # Calls modules with larger instance (t3.large)
        ├── variables.tf         # Prod inputs & settings
        ├── outputs.tf           # Prod output helpers
        └── terraform.tfvars     # Prod variable overrides
```

---

## 🔰 Key Concepts: Modules, Providers & Resources

### 1. What is a Module?
A **Module** is a self-contained container of Terraform resources that can be called repeatedly with different parameters.
* **Why use modules?** Don't Repeat Yourself (DRY). Instead of duplicating 150 lines of VPC and EC2 code between `dev` and `prod`, we write the logic once in `modules/`, and each environment simply calls it:

```hcl
# Inside environments/dev/main.tf
module "k3s_cluster" {
  source        = "../../modules/k3s_cluster"
  environment   = "dev"
  instance_type = "t3.micro" # Dev: small and inexpensive
  ...
}

# Inside environments/prod/main.tf
module "k3s_cluster" {
  source        = "../../modules/k3s_cluster"
  environment   = "prod"
  instance_type = "t3.large" # Prod: high performance
  ...
}
```

### 2. What is a Provider?
The translation plugin that communicates with the cloud API (e.g. `aws`, `tls`, `local`). Configured inside each environment's `main.tf`.

### 3. What is a Resource?
The individual building block created by the module (e.g. `aws_vpc`, `aws_instance`, `aws_security_group`, `aws_eip`).

---

## 🏛️ Architecture Overview

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
                               [ Public Subnet ]
                         (10.0.1.0/24 in Target AZ)
                                          │
                                          ▼
                    [ Security Group Firewall: k3s-web-sg ]
                    ├── Inbound:  22 (SSH), 80 (HTTP), 443 (HTTPS)
                    ├── Strict:   8000, 27017, 6443 NEVER opened publicly
                    └── Outbound: ALL Traffic 0.0.0.0/0
                                          │
                                          ▼
         [ AWS EC2 Instance: t3.micro (dev) / t3.large (prod) ]
         Ubuntu 24.04 LTS | Physical RAM + 2 GB Swap | 30 GB gp3
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

## 🚀 Quick Start Deployment Guide

Each environment has its own independent state file (`.tfstate`). You can deploy `dev`, `prod`, or both without interfering with each other.

### Option A: Deploying Development (`dev`)
```bash
cd environments/dev

# 1. Initialize providers and download local modules
terraform init

# 2. Preview the changes
terraform plan

# 3. Apply and build dev cluster (t3.micro)
terraform apply
```

### Option B: Deploying Production (`prod`)
```bash
cd environments/prod

# 1. Initialize providers and download local modules
terraform init

# 2. Preview the changes
terraform plan

# 3. Apply and build prod cluster (t3.large)
terraform apply
```

---

## 🔑 Accessing Your Cluster

After running `terraform apply`, Terraform outputs the connection helpers for your environment:

### 1. SSH Access
The SSH key is automatically generated and saved in the environment directory:
* For dev: `dev-k3s-ticket-key.pem`
* For prod: `prod-k3s-ticket-key.pem`

Connect via SSH:
```bash
# In environments/dev:
ssh -i "dev-k3s-ticket-key.pem" ubuntu@<DEV_ELASTIC_IP>

# In environments/prod:
ssh -i "prod-k3s-ticket-key.pem" ubuntu@<PROD_ELASTIC_IP>
```

### 2. AWS Systems Manager (SSM) Access (No Keys Needed)
```bash
aws ssm start-session --target <INSTANCE_ID> --region us-east-1
```

### 3. Remote Local `kubectl` Control
Download the Kubeconfig directly using the output command:
```bash
ssh -i "prod-k3s-ticket-key.pem" ubuntu@<ELASTIC_IP> "sudo cat /etc/rancher/k3s/k3s.yaml" | sed "s/127.0.0.1/<ELASTIC_IP>/g" > ~/.kube/mono-ticket-prod-config

export KUBECONFIG=~/.kube/mono-ticket-prod-config
kubectl get nodes -o wide
```

---

## 🧹 Teardown / Destroy

To tear down an environment and stop all cloud charges:

```bash
# To destroy dev:
cd environments/dev
terraform destroy

# To destroy prod:
cd environments/prod
terraform destroy
```
