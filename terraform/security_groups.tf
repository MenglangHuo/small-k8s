# =============================================================================
# Security Group Configuration: k3s-web-sg
# Follows: docs/02_infrastructure_setup.md (Phase 2)
#
# Rules:
# - Inbound 22 (SSH): Administrative terminal access
# - Inbound 80 (HTTP): Let's Encrypt ACME HTTP-01 challenges & HTTPS redirect
# - Inbound 443 (HTTPS): Production encrypted web traffic
# - Strict Isolation: Ports 8000, 27017, 6443 are NEVER opened to 0.0.0.0/0
# - Outbound ALL: Package updates, image pulls, ACME validation calls
# =============================================================================

resource "aws_security_group" "k3s_web" {
  name        = var.security_group_name
  description = "Allow SSH, HTTP, and HTTPS for K3s mono-ticket"
  vpc_id      = aws_vpc.this.id

  tags = {
    Name = var.security_group_name
  }
}

# -----------------------------------------------------------------------------
# Inbound Security Rules
# -----------------------------------------------------------------------------

# SSH Access (Port 22)
resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each          = toset(var.allowed_ssh_cidr_blocks)
  security_group_id = aws_security_group.k3s_web.id
  cidr_ipv4         = each.value
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
  description       = "Secure administrative terminal access"
}

# HTTP Access (Port 80)
resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.k3s_web.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  description       = "Let's Encrypt ACME HTTP-01 challenges & HTTP redirection"
}

# HTTPS Access (Port 443)
resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.k3s_web.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  description       = "Production encrypted HTTPS web traffic"
}

# -----------------------------------------------------------------------------
# Outbound Security Rules (Egress)
# -----------------------------------------------------------------------------
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.k3s_web.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
  description       = "Operating system updates, Docker Hub image pulls, and Let's Encrypt ACME verification calls"
}
