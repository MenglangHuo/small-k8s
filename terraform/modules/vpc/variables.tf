# =============================================================================
# VPC MODULE - VARIABLES
# =============================================================================

variable "project_name" {
  description = "Project name used for resource naming and tagging."
  type        = string
  default     = "mono-ticket"
}

variable "environment" {
  description = "Environment name (e.g. dev, prod)."
  type        = string
}

variable "vpc_name" {
  description = "Custom name for the VPC. If empty, defaults to '<project_name>-<environment>-vpc'."
  type        = string
  default     = ""
}

variable "vpc_cidr" {
  description = "The IPv4 CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr must be a valid IPv4 CIDR block."
  }
}

variable "public_subnet_cidr" {
  description = "Subnet CIDR block for public internet-facing compute nodes."
  type        = string
  default     = "10.0.1.0/24"

  validation {
    condition     = can(cidrhost(var.public_subnet_cidr, 0))
    error_message = "public_subnet_cidr must be a valid IPv4 CIDR block."
  }
}

variable "private_subnet_cidr" {
  description = "Subnet CIDR block for private tier (reserved for future database tier)."
  type        = string
  default     = "10.0.10.0/24"

  validation {
    condition     = can(cidrhost(var.private_subnet_cidr, 0))
    error_message = "private_subnet_cidr must be a valid IPv4 CIDR block."
  }
}

variable "availability_zone" {
  description = "Specific AWS Availability Zone to use. If empty, the first available AZ in the region is used."
  type        = string
  default     = ""
}

variable "tags" {
  description = "Additional tags to merge onto VPC resources."
  type        = map(string)
  default     = {}
}
