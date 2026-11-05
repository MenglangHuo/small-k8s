# =============================================================================
# K3S CLUSTER MODULE - VARIABLES
# =============================================================================

variable "project_name" {
  description = "Project name prefix."
  type        = string
  default     = "mono-ticket"
}

variable "environment" {
  description = "Deployment environment name (dev, prod, etc.)."
  type        = string
}

variable "vpc_id" {
  description = "The ID of the VPC where the cluster will be deployed."
  type        = string
}

variable "subnet_id" {
  description = "The public subnet ID where the EC2 instance will reside."
  type        = string
}

variable "internet_gateway_id" {
  description = "The Internet Gateway ID (used to establish explicit dependency for Elastic IP)."
  type        = string
  default     = ""
}

variable "instance_name" {
  description = "Name tag for the EC2 server instance."
  type        = string
  default     = ""
}

variable "instance_type" {
  description = "EC2 instance sizing (e.g. t3.micro for dev, t3.medium or t3.large for prod)."
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size" {
  description = "Size of the root EBS disk in GiB (minimum 30 GiB recommended)."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size >= 30
    error_message = "root_volume_size must be at least 30 GiB."
  }
}

variable "root_volume_type" {
  description = "EBS storage performance class (gp3 default)."
  type        = string
  default     = "gp3"
}

variable "root_volume_delete_on_termination" {
  description = "Delete EBS root volume on EC2 termination."
  type        = bool
  default     = true
}

variable "root_volume_encrypted" {
  description = "Enable KMS encryption at rest for the root EBS disk."
  type        = bool
  default     = true
}

variable "security_group_name" {
  description = "Custom security group name. If empty, defaults to '<project_name>-<environment>-k3s-sg'."
  type        = string
  default     = ""
}

variable "allowed_ssh_cidr_blocks" {
  description = "IPv4 CIDR blocks permitted for SSH access on port 22."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "ssh_key_name" {
  description = "Existing AWS Key Pair name. Leave empty to auto-generate."
  type        = string
  default     = ""
}

variable "ssh_public_key" {
  description = "Public key material to register. Leave empty to auto-generate if generate_ssh_key is true."
  type        = string
  default     = ""
}

variable "generate_ssh_key" {
  description = "Auto-generate a 4096-bit RSA key pair and save the private key locally."
  type        = bool
  default     = true
}

variable "enable_ssm" {
  description = "Attach AWS Systems Manager (SSM) instance profile for terminal access."
  type        = bool
  default     = true
}

variable "swap_size_gb" {
  description = "Dedicated swap file size in GiB."
  type        = number
  default     = 2
}

variable "swappiness" {
  description = "Linux kernel swappiness (10 recommended)."
  type        = number
  default     = 10
}

variable "install_k3s" {
  description = "Whether to automatically install and initialize K3s."
  type        = bool
  default     = true
}

variable "install_cert_manager" {
  description = "Whether to deploy Cert-Manager."
  type        = bool
  default     = true
}

variable "cert_manager_version" {
  description = "Cert-Manager release version."
  type        = string
  default     = "v1.16.1"
}

variable "domain_name" {
  description = "Root domain name (e.g., 'yourdomain.com')."
  type        = string
  default     = ""
}

variable "create_route53_records" {
  description = "Whether to create Route 53 A records (@, www, api) pointing to the Elastic IP."
  type        = bool
  default     = false
}

variable "route53_zone_id" {
  description = "Route 53 Hosted Zone ID for domain_name."
  type        = string
  default     = ""
}

variable "tags" {
  description = "Additional tags to merge onto resources."
  type        = map(string)
  default     = {}
}
