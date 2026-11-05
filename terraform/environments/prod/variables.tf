# =============================================================================
# ENVIRONMENT: PROD - VARIABLES
# =============================================================================

variable "aws_region" {
  description = "AWS Region."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name prefix."
  type        = string
  default     = "mono-ticket"
}

variable "instance_type" {
  description = "EC2 instance type for prod environment (t3.large per architecture or t3.medium)."
  type        = string
  default     = "t3.large"
}

variable "root_volume_size" {
  description = "Root disk size in GiB for production."
  type        = number
  default     = 30
}

variable "availability_zone" {
  description = "Target availability zone (optional)."
  type        = string
  default     = ""
}

variable "allowed_ssh_cidr_blocks" {
  description = "Allowed CIDR blocks for SSH. Strongly recommend restricting to admin office/VPN IP."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "domain_name" {
  description = "Production root domain name (e.g. 'yourdomain.com')."
  type        = string
  default     = ""
}

variable "create_route53_records" {
  description = "Whether to create Route 53 DNS records."
  type        = bool
  default     = false
}

variable "route53_zone_id" {
  description = "Route 53 hosted zone ID."
  type        = string
  default     = ""
}

variable "tags" {
  description = "Additional tags."
  type        = map(string)
  default     = {}
}
