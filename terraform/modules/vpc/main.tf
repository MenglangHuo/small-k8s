# =============================================================================
# VPC MODULE - MAIN RESOURCES
# Provisions: VPC, Internet Gateway, 1 Public Subnet, 1 Private Subnet, Routing
# =============================================================================

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  selected_az = var.availability_zone != "" ? var.availability_zone : data.aws_availability_zones.available.names[0]
  vpc_name    = var.vpc_name != "" ? var.vpc_name : "${var.project_name}-${var.environment}-vpc"

  module_tags = merge(
    {
      Project     = var.project_name
      Environment = var.environment
      Module      = "vpc"
    },
    var.tags
  )
}

# -----------------------------------------------------------------------------
# 1. Virtual Private Cloud
# -----------------------------------------------------------------------------
resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(
    local.module_tags,
    {
      Name = local.vpc_name
    }
  )
}

# -----------------------------------------------------------------------------
# 2. Internet Gateway (ticket-igw)
# -----------------------------------------------------------------------------
resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id #"Create an Internet Gateway and attach it to the VPC that I created above."

  tags = merge(
    local.module_tags,
    {
      Name = "${var.project_name}-${var.environment}-igw"
    }
  )
}

# -----------------------------------------------------------------------------
# 3. Public Subnet
# -----------------------------------------------------------------------------
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = local.selected_az
  map_public_ip_on_launch = true

  tags = merge(
    local.module_tags,
    {
      Name = "${var.project_name}-${var.environment}-public-subnet"
      Tier = "Public"
    }
  )
}

# -----------------------------------------------------------------------------
# 4. Private Subnet (Reserved for future DB tier)
# -----------------------------------------------------------------------------
resource "aws_subnet" "private" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.private_subnet_cidr
  availability_zone       = local.selected_az
  map_public_ip_on_launch = false

  tags = merge(
    local.module_tags,
    {
      Name = "${var.project_name}-${var.environment}-private-subnet"
      Tier = "Private"
    }
  )
}

# -----------------------------------------------------------------------------
# 5. Public Route Table & Subnet Association
# -----------------------------------------------------------------------------
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = merge(
    local.module_tags,
    {
      Name = "${var.project_name}-${var.environment}-public-rt"
      Tier = "Public"
    }
  )
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# -----------------------------------------------------------------------------
# 6. Private Route Table & Subnet Association
# -----------------------------------------------------------------------------
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  tags = merge(
    local.module_tags,
    {
      Name = "${var.project_name}-${var.environment}-private-rt"
      Tier = "Private"
    }
  )
}

resource "aws_route_table_association" "private" {
  subnet_id      = aws_subnet.private.id
  route_table_id = aws_route_table.private.id
}
