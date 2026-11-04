# =============================================================================
# Custom Multi-AZ VPC Architecture
# Follows: docs/02_infrastructure_setup.md (Phase 1, Option B)
#
# Architecture Topology:
# - VPC: 10.0.0.0/16 (mono-ticket-vpc)
# - Internet Gateway: ticket-igw
# - 2 Public Subnets: 10.0.1.0/24 (AZ A), 10.0.2.0/24 (AZ B)
# - 2 Private Subnets: 10.0.10.0/24 (AZ A), 10.0.20.0/24 (AZ B) [Reserved for DB tier]
# - Zero NAT Gateways ($0 cost per DevOps guide rule)
# =============================================================================

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = var.vpc_name
  }
}

# -----------------------------------------------------------------------------
# Internet Gateway (ticket-igw)
# -----------------------------------------------------------------------------
resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "ticket-igw"
  }
}

# -----------------------------------------------------------------------------
# Public Subnets (2 Availability Zones)
# -----------------------------------------------------------------------------
resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = local.selected_azs[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "mono-ticket-subnet-public${count.index + 1}-${local.selected_azs[count.index]}"
    Tier = "Public"
  }
}

# -----------------------------------------------------------------------------
# Private Subnets (2 Availability Zones - Reserved for future DB / RDS tier)
# -----------------------------------------------------------------------------
resource "aws_subnet" "private" {
  count                   = 2
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.private_subnet_cidrs[count.index]
  availability_zone       = local.selected_azs[count.index]
  map_public_ip_on_launch = false

  tags = {
    Name = "mono-ticket-subnet-private${count.index + 1}-${local.selected_azs[count.index]}"
    Tier = "Private"
  }
}

# -----------------------------------------------------------------------------
# Public Route Table & Subnet Associations
# Default route: 0.0.0.0/0 -> ticket-igw
# -----------------------------------------------------------------------------
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "mono-ticket-public-rt"
    Tier = "Public"
  }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# -----------------------------------------------------------------------------
# Private Route Table & Subnet Associations
# Local-only routing (10.0.0.0/16) - No NAT Gateway ($0/month)
# -----------------------------------------------------------------------------
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "mono-ticket-private-rt"
    Tier = "Private"
  }
}

resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
