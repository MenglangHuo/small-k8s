# -----------------------------------------------------------------------------
# General Configuration
# -----------------------------------------------------------------------------

variable "aws_region" {
  description = "The AWS region where resources will be provisioned (e.g. us-east-1, ap-southeast-1)."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment name (e.g., production, staging, dev)."
  type        = string
  default     = "production"
}

variable "project_name" {
  description = "Project name prefix used for resource naming and tagging."
  type        = string
  default     = "mono-ticket"
}

variable "tags" {
  description = "A map of additional tags to assign to all resources."
  type        = map(string)
  default     = {}
}

# -----------------------------------------------------------------------------
# VPC & Networking Configuration (based on docs/02_infrastructure_setup.md Phase 1)
# -----------------------------------------------------------------------------

variable "vpc_name" {
  description = "Name tag for the Virtual Private Cloud (matches docs: mono-ticket-vpc)."
  type        = string
  default     = "mono-ticket-vpc"
}

variable "vpc_cidr" {
  description = "IPv4 CIDR block for the custom VPC (matches docs: 10.0.0.0/16)."
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr must be a valid IPv4 CIDR block."
  }
}

variable "availability_zones" {
  description = "List of Availability Zones to deploy subnets into. If empty, the first 2 available AZs in aws_region will be used."
  type        = list(string)
  default     = []
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for the 2 public subnets (matches docs: 10.0.1.0/24 and 10.0.2.0/24)."
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) == 2
    error_message = "Exactly 2 public subnet CIDR blocks must be defined for multi-AZ alignment."
  }
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for the 2 private subnets reserved for future DB tier (matches docs: 10.0.10.0/24 and 10.0.20.0/24)."
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.20.0/24"]

  validation {
    condition     = length(var.private_subnet_cidrs) == 2
    error_message = "Exactly 2 private subnet CIDR blocks must be defined for multi-AZ alignment."
  }
}

# -----------------------------------------------------------------------------
# Security Group Configuration (based on docs/02_infrastructure_setup.md Phase 2)
# -----------------------------------------------------------------------------

variable "security_group_name" {
  description = "Name of the security group for the K3s host (matches docs: k3s-web-sg)."
  type        = string
  default     = "k3s-web-sg"
}

variable "allowed_ssh_cidr_blocks" {
  description = "IPv4 CIDR blocks permitted for SSH access on port 22. Defaults to 0.0.0.0/0 per docs, but recommend locking down to your IP in production."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# -----------------------------------------------------------------------------
# EC2 Compute & Storage Configuration (docs/02_infrastructure_setup.md Phase 3)
# -----------------------------------------------------------------------------

variable "instance_name" {
  description = "Name tag for the EC2 server instance (matches docs: k3s-ticket-server)."
  type        = string
  default     = "k3s-ticket-server"
}

variable "instance_type" {
  description = "EC2 instance type for the K3s single-node cluster (chosen as t3.medium per user requirement: 2 vCPU, 4 GiB RAM)."
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size" {
  description = "Size of the root EBS volume in GiB (matches docs: 30 GiB)."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size >= 30
    error_message = "root_volume_size must be at least 30 GiB as specified in the deployment guidelines."
  }
}

variable "root_volume_type" {
  description = "Root EBS storage volume type (matches docs: gp3)."
  type        = string
  default     = "gp3"
}

variable "root_volume_delete_on_termination" {
  description = "Whether the root EBS volume is deleted when the EC2 instance is terminated."
  type        = bool
  default     = true
}

variable "root_volume_encrypted" {
  description = "Whether the root EBS volume is encrypted with default AWS KMS key."
  type        = bool
  default     = true
}

# -----------------------------------------------------------------------------
# SSH Key Pair & Access Configuration
# -----------------------------------------------------------------------------

variable "ssh_key_name" {
  description = "Name of an existing AWS Key Pair to attach. Leave empty to either supply ssh_public_key or auto-generate one."
  type        = string
  default     = ""
}

variable "ssh_public_key" {
  description = "Public key material (e.g. 'ssh-rsa AAAA...') to register as AWS Key Pair. Leave empty to auto-generate if generate_ssh_key is true."
  type        = string
  default     = ""
}

variable "generate_ssh_key" {
  description = "Whether to automatically generate an RSA key pair and save the private key file locally (k3s-ticket-key.pem) if no key is provided."
  type        = bool
  default     = true
}

variable "enable_ssm" {
  description = "Attach AmazonSSMManagedInstanceCore IAM role to allow AWS Systems Manager Session Manager terminal access."
  type        = bool
  default     = true
}

# -----------------------------------------------------------------------------
# OS Kernel Tuning & Cluster Bootstrapping (docs/02_infrastructure_setup.md Phase 5, 6, 9)
# -----------------------------------------------------------------------------

variable "swap_size_gb" {
  description = "Swap file size in GiB to allocate on root disk (matches docs: 2 GB swap)."
  type        = number
  default     = 2
}

variable "swappiness" {
  description = "Linux kernel swappiness value (matches docs: 10, to only swap when physical RAM is tight)."
  type        = number
  default     = 10
}

variable "install_k3s" {
  description = "Whether to automatically install and initialize K3s Kubernetes in user_data."
  type        = bool
  default     = true
}

variable "install_cert_manager" {
  description = "Whether to automatically apply the Cert-Manager manifest in user_data (matches docs Phase 9)."
  type        = bool
  default     = true
}

variable "cert_manager_version" {
  description = "Cert-Manager release version to apply (matches docs Phase 9: v1.16.1)."
  type        = string
  default     = "v1.16.1"
}

# -----------------------------------------------------------------------------
# Optional Route 53 DNS Configuration (docs/02_infrastructure_setup.md Phase 7)
# -----------------------------------------------------------------------------

variable "domain_name" {
  description = "Root domain name for Mono-Ticket (e.g., 'yourdomain.com'). Required if create_route53_records is enabled."
  type        = string
  default     = ""
}

variable "create_route53_records" {
  description = "Whether to create Route 53 A records (@, www, api) pointing to the Elastic IP. Set to true if managing DNS in AWS Route53."
  type        = bool
  default     = false
}

variable "route53_zone_id" {
  description = "Route 53 hosted zone ID for domain_name. Required if create_route53_records is true."
  type        = string
  default     = ""
}
