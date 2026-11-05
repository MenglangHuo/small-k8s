# =============================================================================
# ENVIRONMENT: DEV
# Calls reusable modules with a small instance (t3.micro)
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
      Environment = "dev"
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
  environment         = "dev"
  vpc_cidr            = "10.10.0.0/16"
  public_subnet_cidr  = "10.10.1.0/24"
  private_subnet_cidr = "10.10.10.0/24"
  availability_zone   = var.availability_zone
  tags                = var.tags
}

# -----------------------------------------------------------------------------
# 2. Reusable K3s Compute Module (small instance: t3.micro)
# -----------------------------------------------------------------------------
module "k3s_cluster" {
  source = "../../modules/k3s_cluster"

  project_name            = var.project_name
  environment             = "dev"
  vpc_id                  = module.vpc.vpc_id
  subnet_id               = module.vpc.public_subnet_id
  internet_gateway_id     = module.vpc.internet_gateway_id
  instance_type           = var.instance_type # Default: t3.micro
  root_volume_size        = 30
  allowed_ssh_cidr_blocks = var.allowed_ssh_cidr_blocks
  domain_name             = var.domain_name
  create_route53_records  = var.create_route53_records
  route53_zone_id         = var.route53_zone_id
  tags                    = var.tags
}
