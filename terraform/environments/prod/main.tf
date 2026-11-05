# =============================================================================
# ENVIRONMENT: PROD
# Calls reusable modules with a larger instance (t3.large / t3.medium)
# =============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = "prod"
      ManagedBy   = "Terraform"
    }
  }
}

# -----------------------------------------------------------------------------
# 1. Reusable VPC Module
# -----------------------------------------------------------------------------
module "vpc" {
  source = "../../modules/vpc"

  project_name        = var.project_name
  environment         = "prod"
  vpc_cidr            = "10.0.0.0/16"
  public_subnet_cidr  = "10.0.1.0/24"
  private_subnet_cidr = "10.0.10.0/24"
  availability_zone   = var.availability_zone
  tags                = var.tags
}

# -----------------------------------------------------------------------------
# 2. Reusable K3s Compute Module (larger instance: t3.large)
# -----------------------------------------------------------------------------
module "k3s_cluster" {
  source = "../../modules/k3s_cluster"

  project_name            = var.project_name
  environment             = "prod"
  vpc_id                  = module.vpc.vpc_id
  subnet_id               = module.vpc.public_subnet_id
  internet_gateway_id     = module.vpc.internet_gateway_id
  instance_type           = var.instance_type # Default: t3.large (or t3.medium)
  root_volume_size        = var.root_volume_size
  allowed_ssh_cidr_blocks = var.allowed_ssh_cidr_blocks
  domain_name             = var.domain_name
  create_route53_records  = var.create_route53_records
  route53_zone_id         = var.route53_zone_id
  tags                    = var.tags
}
