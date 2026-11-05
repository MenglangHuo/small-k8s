# Mono-Ticket — Automated Infrastructure Deployment with Terraform

> **DevOps & SRE Operational Standard**: Comprehensive guide for provisioning and managing the **Mono-Ticket** AWS cloud infrastructure and K3s single-node Kubernetes cluster completely as code using HashiCorp Terraform.

---

## 📑 Table of Contents

1. [Overview & Automation Benefits](#1-overview--automation-benefits)
2. [Infrastructure Architecture](#2-infrastructure-architecture)
3. [Terraform Codebase Structure & Preview Order](#3-terraform-codebase-structure--preview-order)
4. [Prerequisites](#4-prerequisites)
5. [Configuration & Customization](#5-configuration--customization)
6. [Step-by-Step Deployment Procedure](#6-step-by-step-deployment-procedure)
7. [Cluster Access & Verification](#7-cluster-access--verification)
8. [DNS Configuration (Domain Registrar)](#8-dns-configuration-domain-registrar)
9. [Deploying Application Workloads](#9-deploying-application-workloads)
10. [Infrastructure Teardown & Cleanup](#10-infrastructure-teardown--cleanup)
11. [Troubleshooting & Common Questions](#11-troubleshooting--common-questions)

---

## 1. Overview & Automation Benefits

In [`docs/02_infrastructure_setup.md`](02_infrastructure_setup.md), infrastructure was provisioned manually through 9 individual phases in the AWS Management Console and SSH terminal. While instructional, manual configuration introduces human error, configuration drift, and takes 30–45 minutes to execute.

This Terraform configuration automates the entire process in **under 3 minutes**:

| Infrastructure Phase | Manual Setup ([`docs/02`](02_infrastructure_setup.md)) | Terraform Automation ([`terraform/`](../terraform)) |
| :--- | :--- | :--- |
| **VPC & Subnets** | Multi-step AWS Console wizard | Fully automated in [`04_vpc.tf`](../terraform/04_vpc.tf) |
| **Security Group** | Manual port opening in Web UI | Zero-trust firewall in [`05_security_groups.tf`](../terraform/05_security_groups.tf) |
| **IAM & SSM Role** | Manual IAM policy attachment | Automated in [`06_iam.tf`](../terraform/06_iam.tf) |
| **SSH Key Pair** | Manual `ssh-keygen` & console import | Auto-generated 4096-bit RSA key in [`07_ec2.tf`](../terraform/07_ec2.tf) |
| **EC2 Server** | Manual wizard with AMI lookup | Automated `t3.medium` launch in [`07_ec2.tf`](../terraform/07_ec2.tf) |
| **Elastic IP** | Manual allocation & association | Automated attachment in [`08_eip.tf`](../terraform/08_eip.tf) |
| **Swap & Kernel Tuning** | Manual bash commands on VM | Cloud-init automated via [`user_data.sh.tftpl`](../terraform/templates/user_data.sh.tftpl) |
| **K3s & Cert-Manager** | Manual curl scripts & manifest apply | Automatically installed on initial boot |
| **Total Setup Time** | ~35 - 45 minutes | **~2 - 3 minutes (1 command: `terraform apply`)** |

---

## 2. Infrastructure Architecture

The following diagram illustrates the provisioned AWS topology and network perimeter:

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

### Key Architectural Design Decisions
1. **1 Public Subnet & 1 Private Subnet**:
   - The compute server resides in the **Public Subnet** (`10.0.1.0/24`), which has a direct route to the Internet Gateway.
   - The **Private Subnet** (`10.0.10.0/24`) is provisioned and reserved for future isolated database tiers (e.g. AWS DocumentDB or RDS).
2. **$0 NAT Gateway Cost**:
   - Deploying directly into the Public Subnet with an attached Elastic IP gives full internet connectivity without paying for an AWS NAT Gateway (~$32.40/month savings).
3. **Hardware Sizing (`t3.medium`)**:
   - 2 vCPUs and 4 GiB physical RAM ensures smooth concurrency across Java 25 WebFlux, Vue SPA, and MongoDB without CPU throttling.
4. **Disk & Kernel Resilience**:
   - 30 GiB `gp3` root volume (3,000 IOPS baseline, encrypted with AWS KMS).
   - 2 GiB dedicated swap file + Linux kernel swappiness tuned to `10` protects against Out-Of-Memory (OOM) pod crashes.

---

## 3. Terraform Codebase Structure & Modules

The infrastructure is organized using HashiCorp's **Reusable Modules Architecture**, separating generic resource logic from environment-specific configurations:

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

## 4. Prerequisites

Before running Terraform, ensure the following tools are installed and configured on your local workstation:

### 1. Install Terraform (CLI version >= 1.5.0)
```bash
# Ubuntu / Debian
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform

# macOS (Homebrew)
brew tap hashicorp/tap
brew install hashicorp/tap/terraform

# Verify installation
terraform -version
```

### 2. Configure AWS CLI with Valid Credentials
Ensure the AWS CLI is installed and configured with programmatic access permissions for VPC, EC2, IAM, and Route 53:
```bash
aws configure
```
Verify your identity:
```bash
aws sts get-caller-identity
```

---

## 5. Environment Configuration

Each environment has its own independent `terraform.tfvars` file:

### Development (`environments/dev/terraform.tfvars`)
```hcl
aws_region    = "us-east-1"
instance_type = "t3.micro"  # Small, cost-effective instance for development
allowed_ssh_cidr_blocks = ["0.0.0.0/0"]
```

### Production (`environments/prod/terraform.tfvars`)
```hcl
aws_region       = "us-east-1"
instance_type    = "t3.large"  # High performance compute instance for production
root_volume_size = 30
allowed_ssh_cidr_blocks = ["0.0.0.0/0"] # Recommend restricting to admin IP in production

# Optional Route 53 DNS Automation
create_route53_records = false
domain_name            = "yourdomain.com"
route53_zone_id        = "Z0123456789ABCDEF"
```

---

## 6. Step-by-Step Deployment Procedure

Choose the environment you want to deploy (`dev` or `prod`):

### Option A: Deploying Development (`dev`)
```bash
cd terraform/environments/dev

# Step 1: Initialize Terraform & download modules
terraform init

# Step 2: Validate & Preview
terraform validate
terraform plan

# Step 3: Apply Infrastructure
terraform apply
```

### Option B: Deploying Production (`prod`)
```bash
cd terraform/environments/prod

# Step 1: Initialize Terraform & download modules
terraform init

# Step 2: Validate & Preview
terraform validate
terraform plan

# Step 3: Apply Infrastructure
terraform apply
```

When prompted:
```text
Do you want to perform these actions?
  Terraform will perform the actions described above.
  Only 'yes' will be accepted to approve.

  Enter a value: yes
```

### Step 4: Capture Outputs
Upon completion (~2–3 minutes), Terraform outputs all critical connection details directly to your terminal:
```text
Apply complete! Resources: 15 added, 0 changed, 0 destroyed.

Outputs:

elastic_ip = "3.218.xx.yy"
instance_id = "i-0a1b2c3d4e5f67890"
ssh_command = "ssh -i \"prod-k3s-ticket-key.pem\" ubuntu@3.218.xx.yy"
ssm_session_command = "aws ssm start-session --target i-0a1b2c3d4e5f67890"
kubeconfig_fetch_command = "ssh -i \"prod-k3s-ticket-key.pem\" ubuntu@3.218.xx.yy \"sudo cat /etc/rancher/k3s/k3s.yaml\" | sed 's/127.0.0.1/3.218.xx.yy/g' > ~/.kube/mono-ticket-prod-config"
...
---

## 7. Cluster Access & Verification

### Method 1: SSH Access (Using Auto-Generated Key)
Terraform automatically generates and saves an RSA key pair inside your environment directory with `chmod 0400`:
* For Dev: `environments/dev/dev-k3s-ticket-key.pem`
* For Prod: `environments/prod/prod-k3s-ticket-key.pem`

Connect via SSH:
```bash
# For Dev:
cd terraform/environments/dev
ssh -i "dev-k3s-ticket-key.pem" ubuntu@<DEV_ELASTIC_IP>

# For Prod:
cd terraform/environments/prod
ssh -i "prod-k3s-ticket-key.pem" ubuntu@<PROD_ELASTIC_IP>
```

### Method 2: AWS Systems Manager (SSM) Session Manager (Zero-Key Access)
You can connect without SSH keys or open port 22 directly through AWS SSM:
```bash
aws ssm start-session --target <INSTANCE_ID> --region us-east-1
```
*(Or via the AWS Console: EC2 ➔ Instances ➔ Select `k3s-ticket-server` ➔ Connect ➔ Session Manager).*

### Method 3: Remote Local `kubectl` Access
To control the Kubernetes cluster directly from your local terminal using `kubectl`:
```bash
# 1. Fetch kubeconfig and replace localhost with the Elastic IP
ssh -i "k3s-ticket-key.pem" ubuntu@<YOUR_ELASTIC_IP> "sudo cat /etc/rancher/k3s/k3s.yaml" | sed "s/127.0.0.1/<YOUR_ELASTIC_IP>/g" > ~/.kube/mono-ticket-config

# 2. Set environment variable and verify cluster communication
export KUBECONFIG=~/.kube/mono-ticket-config
kubectl get nodes -o wide
```

### Verifying Node & Cloud-Init Bootstrap Status
Log into the server and inspect the bootstrap completion:
```bash
# 1. Check cloud-init execution log
tail -n 20 /var/log/user-data.log

# 2. Verify swap space and kernel swappiness
free -h
cat /proc/sys/vm/swappiness   # Should output: 10

# 3. Verify K3s cluster node status
kubectl get nodes
# Expected output:
# NAME                STATUS   ROLES                  AGE   VERSION
# k3s-ticket-server   Ready    control-plane,master   3m    v1.30.x+k3s1

# 4. Verify Cert-Manager pods are running
kubectl get pods -n cert-manager
```

---

## 8. DNS Configuration (Domain Registrar)

Go to your domain DNS provider (Cloudflare, Namecheap, GoDaddy, Porkbun) and configure **3 `A` Records** pointing to your new Elastic IP:

| Record Type | Host / Name | Target IP | TTL | Service Handled |
| :--- | :--- | :--- | :--- | :--- |
| **A** | `@` *(or apex)* | `<YOUR_ELASTIC_IP>` | 300 | Vue.js Admin SPA Frontend |
| **A** | `www` | `<YOUR_ELASTIC_IP>` | 300 | Vue.js Admin SPA Frontend |
| **A** | `api` | `<YOUR_ELASTIC_IP>` | 300 | Spring Boot WebFlux REST API |

> [!IMPORTANT]
> If using **Cloudflare**, set the Proxy Status to **DNS Only (Grey Cloud)** initially. Cloudflare's CDN proxy blocks Let's Encrypt ACME HTTP-01 domain challenge validation until initial certificates are issued.

---

## 9. Deploying Application Workloads

Once the server is booted, K3s is ready, and DNS is resolving:

1. **Copy or clone the repository to the EC2 server:**
   ```bash
   ssh -i "k3s-ticket-key.pem" ubuntu@<YOUR_ELASTIC_IP>
   git clone https://github.com/your-username/mono-ticket.git ~/mono-ticket
   cd ~/mono-ticket/k8s
   ```

2. **Configure production secrets & domain:**
   - Update `k8s/02-secrets.yaml` with your production JWT secret, Mongo credentials, and admin passwords.
   - Update `k8s/06-ingress.yaml` and `k8s/07-cert-issuer.yaml` with your custom domain name and email.

3. **Deploy using the automated deployment script:**
   ```bash
   chmod +x deploy.sh
   ./deploy.sh deploy
   ```

4. **Verify running pods & SSL certificates:**
   ```bash
   kubectl get pods -n ticket-prod
   kubectl get certificates -n ticket-prod
   ```

For detailed instructions on workload deployment, see [`docs/03_deployment_guide.md`](03_deployment_guide.md).

---

## 10. Infrastructure Teardown & Cleanup

To avoid ongoing AWS charges when the cluster is no longer needed, Terraform can cleanly destroy all created resources:

```bash
# To destroy Dev environment:
cd terraform/environments/dev
terraform destroy

# To destroy Prod environment:
cd terraform/environments/prod
terraform destroy
```

Type `yes` when prompted. Terraform will:
1. Terminate the `t3.medium` EC2 instance and delete the 30 GB EBS volume.
2. Disassociate and release the Elastic IP.
3. Remove security groups, IAM roles, and instance profiles.
4. Delete the public and private subnets, route tables, and Internet Gateway.
5. Delete the custom VPC.

---

## 11. Troubleshooting & Common Questions

### Q: `terraform init` fails or hangs
* Ensure you have internet access so Terraform can download providers from `registry.terraform.io`.
* If you have existing state conflicts, try running:
  ```bash
  terraform init -upgrade
  ```

### Q: `ssh` connection refused or times out
1. Wait 60–90 seconds after `terraform apply` finishes for Ubuntu to initialize network services.
2. Check your public IP: if `allowed_ssh_cidr_blocks` in `terraform.tfvars` was restricted to an old IP, update it with your current IP and run `terraform apply`.
3. Alternatively, connect via **AWS SSM Session Manager** (`aws ssm start-session --target <INSTANCE_ID>`), which works even if port 22 is unreachable.

### Q: K3s node is not ready or Cert-Manager failed to deploy
Check the cloud-init bootstrap logs on the server:
```bash
sudo cat /var/log/user-data.log
```
If K3s took longer than usual to start, you can manually trigger Cert-Manager:
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.1/cert-manager.yaml
```

---

## 🔗 Related Documentation
* [`docs/00_k8s_architecture.md`](00_k8s_architecture.md) — Kubernetes Cluster Architecture & Workloads
* [`docs/02_infrastructure_setup.md`](02_infrastructure_setup.md) — Manual AWS Setup Reference Guide
* [`docs/03_deployment_guide.md`](03_deployment_guide.md) — Production Application Deployment Guide
* [`docs/04_troubleshooting.md`](04_troubleshooting.md) — Cluster & Workload Troubleshooting Runbook
