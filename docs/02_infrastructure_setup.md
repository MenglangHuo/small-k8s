# Mono-Ticket — AWS Infrastructure & Cluster Provisioning Guide

> **DevOps & SRE Operational Standard**: Production-grade walkthrough for provisioning an AWS EC2 host, configuring VPC networking, Security Groups, static Elastic IP, Linux kernel memory tuning (swapfile), K3s Kubernetes with native Traefik Ingress, Cert-Manager, and Custom Domain DNS resolution.

---

## 🏗️ System Architecture & Traffic Flow

The following blueprint illustrates how external internet clients resolve your custom domain and route through AWS network boundaries into the single-node K3s cluster:

```text
               Public Internet Traffic (Web Browsers & Mobile Clients)
                                          │
                                          ▼
                         [ Domain Registrar DNS Records ]
                                (Namecheap / DNS)
                         ├── yourdomain.com      ──► Elastic IP
                         ├── www.yourdomain.com  ──► Elastic IP
                         └── api.yourdomain.com  ──► Elastic IP
                                          │
                                          ▼
                       [ AWS Virtual Private Cloud (VPC) ]
                       IPv4 CIDR: 172.31.0.0/16 (or 10.0.0.0/16)
                                          │
                                          ▼
                            [ AWS Internet Gateway (IGW) ]
                             Default Route: 0.0.0.0/0
                                          │
                                          ▼
                               [ Public Subnet ]
                      (Auto-assign Public IPv4 Address: Yes)
                                          │
                                          ▼
                     [ Security Group Firewall: k3s-web-sg ]
                     ├── Inbound:  22 (SSH), 80 (HTTP), 443 (HTTPS)
                     └── Outbound: ALL Traffic 0.0.0.0/0
                                          │
                                          ▼
               [ AWS EC2 Instance: t3.micro (Free Tier Eligible) ]
               Ubuntu 24.04 LTS (x86_64) | 1 vCPU | 1 GB RAM + 2 GB Swap | 30 GB gp3
               Persistent Static Public IP: <YOUR_ELASTIC_IP>
                                          │
    ┌─────────────────────────────────────┴─────────────────────────────────────┐
    │ K3s Lightweight Kubernetes Cluster Engine                                 │
    │                                                                           │
    │  [ Host Ports 80 & 443: Traefik Ingress Controller ]                      │
    │   ├── Handles TLS Termination via Let's Encrypt Certificates             │
    │   ├── Routes HTTP to HTTPS via Traefik Middleware                         │
    │   └── Proxies traffic to internal ClusterIP Services                      │
    │                                                                           │
    │  [ Namespace: cert-manager ]                                              │
    │   └── Cert-Manager Controller, Webhook, and Cainjector (ACME HTTP-01)     │
    │                                                                           │
    │  [ Namespace: ticket-prod ]                                               │
    │   ├── ticket-admin-svc:80   ──► Vue.js SPA Frontend (Nginx Proxy)         │
    │   ├── ticket-api-svc:8000   ──► Spring Boot WebFlux Backend (Java 25)     │
    │   └── mongo-svc:27017       ──► MongoDB 7.0 (Protected by NetworkPolicy)  │
    └───────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Infrastructure Specifications & Free Tier Budget

| Resource | Configuration | AWS Free Tier Limits | Monthly Cost |
|:---|:---|:---|:---|
| **Compute (EC2)** | `t3.micro` (2 vCPU burstable, 1 GB RAM) | 750 hours/month (t2.micro / t3.micro) | **$0.00** |
| **Operating System** | Ubuntu Server 24.04 LTS (x86_64) | Included in AMI Free Tier | **$0.00** |
| **Storage (EBS)** | 30 GiB gp3 (3000 IOPS, 125 MB/s baseline) | Up to 30 GB EBS gp2/gp3 | **$0.00** |
| **Static IP (EIP)** | 1 Elastic IPv4 address | 1 free attached to running instance | **$0.00** |
| **Ingress Controller** | K3s Built-in Traefik (HostPort 80/443) | Replaces $18/mo AWS Load Balancer | **$0.00** |
| **TLS Certificate** | Let's Encrypt via Cert-Manager | Automated ACME protocol | **$0.00** |
| **Total Estimated Cloud Cost** | — | — | **$0.00 / month** |

---

## Phase 1: AWS VPC & Networking Setup

Every EC2 instance runs inside an Amazon Virtual Private Cloud (VPC). You have two standard deployment options:

### Option A: Use the AWS Default VPC (Fastest)
AWS automatically provisions a Default VPC in every region with an attached Internet Gateway and public subnets.
1. Sign in to the [AWS Management Console](https://console.aws.amazon.com/).
2. Select your desired AWS Region in the top navigation bar (e.g., `us-east-1`, `ap-southeast-1`, or `eu-central-1`).
3. Navigate to **VPC Console** → **Your VPCs**.
4. Confirm a VPC exists with **Default VPC = Yes** (typically CIDR `172.31.0.0/16`).
5. Under **Subnets**, select a subnet in your chosen Availability Zone and verify that **Auto-assign public IPv4 address = Yes**.

---

### Option B: Custom Multi-AZ VPC (2 Public Subnets + 2 Private Subnets)
If you created a production-grade VPC with **2 Availability Zones**, **2 Public Subnets**, and **2 Private Subnets**:

```text
┌─── Custom VPC (CIDR: 10.0.0.0/16) ────────────────────────────────────────────────────────┐
│  Attached Internet Gateway: ticket-igw (Route: 0.0.0.0/0 -> igw-xxxx)                      │
│                                                                                            │
│  ┌── Availability Zone A (e.g. us-east-1a) ────┐  ┌── Availability Zone B (e.g. us-east-1b) ────┐  │
│  │                                             │  │                                             │  │
│  │  [ Public Subnet 1: 10.0.1.0/24 ]           │  │  [ Public Subnet 2: 10.0.2.0/24 ]           │  │
│  │   ├── Route: 0.0.0.0/0 -> ticket-igw        │  │   ├── Route: 0.0.0.0/0 -> ticket-igw        │  │
│  │   └── ★ DEPLOY EC2 SERVER HERE ★            │  │   └── (Standby for multi-node / failover)   │  │
│  │                                             │  │                                             │  │
│  │  [ Private Subnet 1: 10.0.10.0/24 ]         │  │  [ Private Subnet 2: 10.0.20.0/24 ]         │  │
│  │   ├── Route: Local only (10.0.0.0/16)       │  │   ├── Route: Local only (10.0.0.0/16)       │  │
│  │   └── (Reserved for future RDS / DB Tier)   │  │   └── (Reserved for future RDS / DB Tier)   │  │
│  └─────────────────────────────────────────────┘  └─────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### How to Configure Your VPC:
When creating or checking this VPC in the **VPC Wizard**:
1. Go to **VPC Dashboard** → Click **Create VPC** → Select **VPC and more**.
2. Configure:
   - **Name tag auto-generation**: `mono-ticket-vpc`
   - **IPv4 CIDR block**: `10.0.0.0/16`
   - **Number of Availability Zones (AZs)**: `2` (e.g., Zone A and Zone B)
   - **Number of public subnets**: `2` (CIDRs: `10.0.1.0/24` and `10.0.2.0/24`)
   - **Number of private subnets**: `2` (CIDRs: `10.0.10.0/24` and `10.0.20.0/24`)
   - **NAT gateways**: Select **`None`** (*Critical: Avoids ~$32.40/month cost; keeps you 100% Free Tier*).
   - **VPC Endpoints**: `None`.
3. Click **Create VPC**.

#### ⚠️ Critical DevOps Rule: Which Subnet to Attach to Your EC2 Server?
> [!IMPORTANT]
> **You MUST launch the EC2 server (`k3s-ticket-server`) into one of your PUBLIC subnets (e.g. `Public Subnet 1`).**
>
> **Why?**
> 1. **Elastic IP Attachment:** In AWS, an Elastic IP can only route inbound internet traffic to an EC2 instance if that instance is in a subnet connected directly to an Internet Gateway (`0.0.0.0/0 -> igw-xxx`).
> 2. **Zero-Cost Outbound Access ($0 NAT Gateway):** Running in a public subnet allows the server to pull Docker Hub images, run `apt update`, and talk to Let's Encrypt ACME servers directly through the Internet Gateway at **$0.00 cost**. If the server were placed in a private subnet, it would require a paid **AWS NAT Gateway** (~$32.40/month) just to download packages!
> 3. **What are the 2 Private Subnets for?** They are prepared for enterprise scaling: if you ever migrate MongoDB to an external managed database (like AWS RDS PostgreSQL or DocumentDB) or add Redis caching, the database tier resides securely in those 2 private subnets across both zones!

#### Verify Public Subnet Route Table:
1. Open **VPC Console** → **Route Tables**.
2. Select the route table associated with your **public subnets**.
3. Under the **Routes** tab, confirm this entry exists:
   - **Destination**: `0.0.0.0/0` → **Target**: `igw-xxxxxxxx` (your Internet Gateway).
4. If missing, click **Edit routes** → **Add route** → Destination: `0.0.0.0/0` → Target: **Internet Gateway** → Save.

---

## Phase 2: Security Group Configuration (`k3s-web-sg`)

The AWS Security Group functions as a stateful perimeter firewall for your EC2 instance.

1. In the EC2 Console left sidebar, go to **Network & Security** → **Security Groups**.
2. Click **Create security group**.
3. Fill in basic details:
   - **Security group name**: `k3s-web-sg`
   - **Description**: `Allow SSH, HTTP, and HTTPS for K3s mono-ticket`
   - **VPC**: Select your Default VPC (or `mono-ticket-vpc`).

### Inbound Rules (Ingress Traffic)
Click **Add rule** to configure these three rules:

| Type | Protocol | Port Range | Source | Purpose & Description |
|:---|:---|:---|:---|:---|
| **SSH** | TCP | `22` | `My IP` (or `0.0.0.0/0`) | Secure administrative terminal access |
| **HTTP** | TCP | `80` | `0.0.0.0/0` | Let's Encrypt ACME HTTP-01 challenges & HTTP redirection |
| **HTTPS** | TCP | `443` | `0.0.0.0/0` | Production encrypted HTTPS web traffic |

> [!WARNING]
> **Strict Isolation Rule:** Never open ports `8000` (Spring Boot API), `27017` (MongoDB), or `6443` (Kubernetes API) to `0.0.0.0/0`. All application traffic is routed securely through Traefik Ingress on ports 80 and 443.

### Outbound Rules (Egress Traffic)
Verify the outbound rule is set to allow all traffic:

| Type | Protocol | Port Range | Destination | Purpose |
|:---|:---|:---|:---|:---|
| **All traffic** | All | All | `0.0.0.0/0` | Operating system updates, Docker Hub image pulls, and Let's Encrypt ACME verification calls |

> [!IMPORTANT]
> If outbound traffic is blocked, `apt-get`, container image downloads, and Cert-Manager's verification requests to Let's Encrypt servers will fail.

4. Click **Create security group**.

---

## Phase 3: Launch the EC2 Instance

1. Navigate to **EC2 Console** → **Instances** → Click **Launch instances**.
2. Configure the instance parameters:

### 3.1: Name and Operating System Image
- **Name**: `k3s-ticket-server`
- **Application and OS Images (AMI)**:
  - Select **Ubuntu**.
  - Choose **Ubuntu Server 24.04 LTS (HVM), SSD Volume Type**.
  - Architecture: **64-bit (x86)**.

### 3.2: Instance Type
- Select **`t3.micro`** (2 vCPUs, 1 GiB RAM — Free Tier eligible).
- *(If `t3.micro` is not available in your region's Free Tier, choose `t2.micro`)*.

### 3.3: Key Pair (Login Credentials)
- Click **Create new key pair**:
  - **Key pair name**: `k3s-ticket-key`
  - **Key pair type**: `RSA`
  - **Private key file format**: `.pem`
- Click **Create key pair**.
- Save the downloaded `k3s-ticket-key.pem` file to a secure directory (e.g. `~/.ssh/` or your project root).

### 3.4: Network Settings
Click **Edit** in the top-right corner of the Network Settings box:
- **VPC**: Select your custom VPC: **`mono-ticket-vpc`** (or Default VPC).
- **Subnet**: Select one of your **PUBLIC subnets** (e.g., `mono-ticket-subnet-public1-us-east-1a`).
  > ⚠️ **DO NOT select a private subnet!** The server must reside in a public subnet to attach the Elastic IP and access the internet without a paid NAT Gateway.
- **Auto-assign public IP**: **Enable** (provides immediate connectivity before attaching the Elastic IP).
- **Firewall (security groups)**: Choose **Select existing security group**.
- **Common security groups**: Check **`k3s-web-sg`**.
  *(Note: Security Groups in AWS are scoped to a specific VPC. Make sure `k3s-web-sg` was created under `mono-ticket-vpc`).*

### 3.5: Configure Storage (30 GiB gp3)
- **Size**: Change `8 GiB` to **`30 GiB`** (AWS Free Tier includes up to 30 GB of EBS storage).
- **Volume type**: **`gp3`** (provides 3,000 IOPS and 125 MB/s baseline throughput).
- **Delete on termination**: `Yes`.

### 3.6: Launch
- Review the summary panel on the right.
- Click **Launch instance**.
- Wait until the instance state is **Running** and **Status check** reports **2/2 checks passed**.

---

## Phase 4: Allocate and Associate an Elastic IP (Mandatory)

By default, an EC2 instance receives a dynamic public IPv4 address that changes whenever the instance is stopped or restarted. A static **Elastic IP (EIP)** ensures your DNS records and Let's Encrypt renewals remain stable permanently.

1. In the EC2 Console left sidebar, go to **Network & Security** → **Elastic IPs**.
2. Click **Allocate Elastic IP address** → Leave defaults → Click **Allocate**.
3. Select the allocated Elastic IP from the list.
4. Click **Actions** → **Associate Elastic IP address**.
5. Configure:
   - **Resource type**: `Instance`
   - **Instance**: Select `k3s-ticket-server`.
   - **Private IP address**: Select the internal IPv4 of the instance.
   - **Reassociation**: Check *Allow this Elastic IP address to be reassociated*.
6. Click **Associate**.
7. **Copy this IP address** — this is your permanent public IP: `<YOUR_ELASTIC_IP>`.

> [!NOTE]
> Elastic IPs are **100% free** as long as they are associated with an active, running EC2 instance.

---

## Phase 5: SSH Connection & OS Kernel Memory Tuning

### Step 5.1: Connect via SSH

#### On Linux / macOS / WSL:
Open your terminal in the directory where `k3s-ticket-key.pem` is stored:
```bash
# Set strict read-only permissions on key (required by OpenSSH)
chmod 400 k3s-ticket-key.pem

# SSH into the server as user 'ubuntu'
ssh -i "k3s-ticket-key.pem" ubuntu@<YOUR_ELASTIC_IP>
```

#### On Windows (PowerShell):
```powershell
# Reset permissions and grant read access only to current user
icacls.exe k3s-ticket-key.pem /reset
icacls.exe k3s-ticket-key.pem /grant:r "$($env:username):(R)"
icacls.exe k3s-ticket-key.pem /inheritance:r

# Connect
ssh -i k3s-ticket-key.pem ubuntu@<YOUR_ELASTIC_IP>
```

*(Alternative: You can also use **EC2 Instance Connect** in the AWS Console by clicking the instance → Connect → EC2 Instance Connect).*

---

### Step 5.2: Update System Packages
Run on the server:
```bash
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y curl wget git net-tools iptables ca-certificates apt-transport-https
```

---

### Step 5.3: Swap Space Allocation (Critical for 1 GB RAM Nodes)

> [!IMPORTANT]
> **DevOps Memory Math:**
> A `t3.micro` instance has 1 GiB (957 MiB usable) physical RAM.
> Under full load, the Ubuntu OS (~150MB), K3s Server (~280MB), MongoDB (~160MB), Spring Boot WebFlux JVM (~350MB), and Traefik (~80MB) require ~1.02 GB.
> Without swap space, the Linux kernel Out-Of-Memory (OOM) killer will trigger, killing containers or freezing your server. Creating a 2 GB swap file increases effective system memory to 3 GB.

Execute the following commands to configure 2 GB swap:
```bash
# 1. Allocate a 2GB file on root disk
sudo fallocate -l 2G /swapfile

# 2. Restrict permissions to root only
sudo chmod 600 /swapfile

# 3. Format as swap space
sudo mkswap /swapfile

# 4. Activate the swap file
sudo swapon /swapfile

# 5. Make swap persistent across server reboots
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 6. Tune kernel swappiness (10 means only swap when physical RAM is genuinely tight)
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
```

#### Verify Swap is Active:
```bash
free -h
```

Expected output:
```text
               total        used        free      shared  buff/cache   available
Mem:           957Mi       180Mi       480Mi       1.0Mi       297Mi       690Mi
Swap:          2.0Gi          0B       2.0Gi
```

---

### Step 5.4: Verify Host Firewall (UFW)
K3s and its Flannel CNI rely on `iptables` rules for packet routing. If Ubuntu's `ufw` firewall is enabled, it may block internal Pod-to-Pod and HostPort traffic. Ensure UFW is disabled:
```bash
sudo ufw status
# If active, disable it (AWS Security Group handles perimeter security):
sudo ufw disable
```

---

## Phase 6: Install K3s Lightweight Kubernetes

K3s ships with Traefik as its native Ingress controller. We keep Traefik enabled and disable `metrics-server` to save ~60 MB RAM on our micro instance.

### Step 6.1: Run K3s Installer
```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server \
  --disable metrics-server \
  --write-kubeconfig-mode 644" sh -
```

**Flag Explanations:**
- `--disable metrics-server`: Conserves ~60 MB RAM (can be re-enabled later if Horizontal Pod Autoscaling is needed).
- `--write-kubeconfig-mode 644`: Sets `/etc/rancher/k3s/k3s.yaml` to readable mode so non-root users can execute `kubectl`.
- *(Traefik & ServiceLB remain enabled to provide native Ingress on host ports 80 and 443).*

---

### Step 6.2: Configure `kubectl` for the `ubuntu` User
```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
source ~/.bashrc
```

---

### Step 6.3: Verify Cluster Status
Check that your single node reports `Ready`:
```bash
kubectl get nodes -o wide
```

Expected output:
```text
NAME              STATUS   ROLES                  AGE   VERSION        INTERNAL-IP
ip-172-31-x-x    Ready    control-plane,master   45s   v1.31.x+k3s1   172.31.x.x
```

Check system pods in `kube-system`:
```bash
kubectl get pods -n kube-system
```
Wait until `coredns`, `local-path-provisioner`, and `traefik` show status **`Running`** or **`Completed`**.

---

## Phase 7: Domain Registrar & DNS Configuration

To access your Mono-Ticket application with valid Let's Encrypt SSL, configure DNS records at your domain registrar (e.g., **Namecheap**, **Spaceship**, **Cloudflare**, **Porkbun**, **GoDaddy**, etc.).

### Step 7.1: Add DNS A Records
1. Log in to your domain registrar's management console.
2. Navigate to your domain's **DNS Management** / **Advanced DNS** settings.
3. Remove any default parking or placeholder records.
4. Add the following **3 `A` Records** pointing to your static **`<YOUR_ELASTIC_IP>`**:

| Record Type | Host / Name | IP Address / Value | TTL | Routing Target |
|:---|:---|:---|:---|:---|
| **A** | `@` *(root domain)* | `<YOUR_ELASTIC_IP>` | `300` (or Automatic) | Vue.js Admin SPA Frontend |
| **A** | `www` | `<YOUR_ELASTIC_IP>` | `300` (or Automatic) | Vue.js Admin SPA Frontend |
| **A** | `api` | `<YOUR_ELASTIC_IP>` | `300` (or Automatic) | Spring Boot WebFlux API Backend |

> [!IMPORTANT]
> **Cloudflare Proxy Warning:** If managing your DNS through Cloudflare, make sure the proxy status for all three records is set to **DNS Only (Grey Cloud)** during initial deployment. Let's Encrypt requires direct HTTP contact with your server to complete ACME HTTP-01 challenges.

---

### Step 7.2: Verify DNS Propagation
From your **local workstation terminal**, verify that the domain resolves to your Elastic IP:

```bash
# Check root domain
nslookup yourdomain.com
# or
dig +short yourdomain.com

# Check api subdomain
nslookup api.yourdomain.com
# or
dig +short api.yourdomain.com
```

Both queries must return `<YOUR_ELASTIC_IP>`. DNS propagation typically takes between 2 to 15 minutes.

---

## Phase 8: Verify Traefik Ingress Connectivity

K3s installs Traefik and binds host ports `80` and `443` automatically.

### Step 8.1: Inspect Traefik Deployment
Run on the EC2 server:
```bash
kubectl get pods -n kube-system -l app.kubernetes.io/name=traefik
```
The Traefik pod should report `1/1 Running`.

---

### Step 8.2: Verify Ports 80 and 443 are Bound
Check that the server is actively listening on HTTP and HTTPS:
```bash
sudo ss -tulpn | grep -E ':(80|443)'
```

---

### Step 8.3: Test External HTTP Ingress from the Internet
From your **local workstation** or browser, test reaching the server:
```bash
curl -I http://<YOUR_ELASTIC_IP>
```

Expected HTTP response:
```http
HTTP/1.1 404 Not Found
Date: ...
```

> [!NOTE]
> A `404 Not Found` response from Traefik is the **expected success signal** at this stage. It confirms that external traffic reached AWS, passed the Security Group, reached the EC2 host, and was processed by Traefik.

---

## Phase 9: Install Cert-Manager (Let's Encrypt SSL Automation)

Cert-Manager automatically provisions and renews TLS certificates from Let's Encrypt using ACME HTTP-01 challenge validation.

### Step 9.1: Apply Cert-Manager v1.16.1 Manifest
Run on your EC2 server:
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.1/cert-manager.yaml
```

---

### Step 9.2: Verify Cert-Manager Pods
Wait ~30 seconds, then check the pods:
```bash
kubectl get pods -n cert-manager
```

Expected output:
```text
NAME                                       READY   STATUS    RESTARTS   AGE
cert-manager-xxxxxxxxxx-xxxxx              1/1     Running   0          45s
cert-manager-cainjector-xxxxxxxxxx-xxxxx   1/1     Running   0          45s
cert-manager-webhook-xxxxxxxxxx-xxxxx      1/1     Running   0          45s
```
All 3 pods must report **`1/1 Running`**.

---

## 🎯 Pre-Flight Infrastructure Verification Checklist

Before deploying the application, ensure each item below is verified:

- [ ] EC2 `t3.micro` instance is in `Running` state with Ubuntu 24.04 LTS.
- [ ] Security Group `k3s-web-sg` has Inbound ports `22`, `80`, `443` open and Outbound `All traffic` open.
- [ ] Elastic IP is associated and permanent.
- [ ] 2 GB Swap file is active (`free -h` shows ~2.0Gi under Swap).
- [ ] K3s is running and `kubectl get nodes` reports node in `Ready` state.
- [ ] DNS A records (`@`, `www`, `api`) resolve to `<YOUR_ELASTIC_IP>`.
- [ ] Traefik Ingress is running and listening on ports 80/443 (`curl -I http://<YOUR_ELASTIC_IP>` returns 404).
- [ ] Cert-Manager has all 3 pods running in namespace `cert-manager`.

---

## 🚀 Next Step

Infrastructure provisioning is complete! Proceed to:
👉 **[03_deployment_guide.md](./03_deployment_guide.md)** to configure application secrets, customize domain manifests, deploy cluster services sequentially, obtain SSL certificates, and make Ticket-Mono accessible to the public internet!
