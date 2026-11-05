# =============================================================================
# ENVIRONMENT: DEV - VARIABLES
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
  description = "EC2 instance type for dev environment (cost-optimized t3.micro)."
  type        = string
  default     = "t3.micro"
}

variable "availability_zone" {
  description = "Target availability zone (optional)."
  type        = string
  default     = ""
}

variable "allowed_ssh_cidr_blocks" {
  description = "Allowed CIDR blocks for SSH."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "domain_name" {
  description = "Optional domain name for dev."
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
