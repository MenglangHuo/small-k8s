# =============================================================================
# K3S CLUSTER MODULE - MAIN RESOURCES
# Provisions: Firewall, IAM (SSM), SSH Key Pair, EC2 Server, Elastic IP, Route53
# =============================================================================

# -----------------------------------------------------------------------------
# 1. Canonical Ubuntu 24.04 LTS AMI Lookup
# -----------------------------------------------------------------------------
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

locals {
  instance_name       = var.instance_name != "" ? var.instance_name : "${var.project_name}-${var.environment}-server"
  security_group_name = var.security_group_name != "" ? var.security_group_name : "${var.project_name}-${var.environment}-sg"
  key_name            = "${var.project_name}-${var.environment}-key"

  module_tags = merge(
    {
      Project     = var.project_name
      Environment = var.environment
      Module      = "k3s_cluster"
    },
    var.tags
  )
}

# -----------------------------------------------------------------------------
# 2. Stateful Firewall Security Group (k3s-web-sg)
# -----------------------------------------------------------------------------
resource "aws_security_group" "this" {
  name        = local.security_group_name
  description = "Firewall for ${var.project_name} (${var.environment}) K3s cluster"
  vpc_id      = var.vpc_id

  tags = merge(
    local.module_tags,
    {
      Name = local.security_group_name
    }
  )
}

# Inbound 22 (SSH)
resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each          = toset(var.allowed_ssh_cidr_blocks)
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = each.value
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
  description       = "Administrative SSH access"
}

# Inbound 80 (HTTP)
resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  description       = "HTTP traffic & Let's Encrypt ACME verification"
}

# Inbound 443 (HTTPS)
resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  description       = "Production HTTPS traffic"
}

# Outbound Egress (All Traffic)
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
  description       = "Outbound OS updates, container pulls, and ACME validations"
}

# -----------------------------------------------------------------------------
# 3. IAM Role & Instance Profile for AWS SSM Session Manager
# -----------------------------------------------------------------------------
data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  count              = var.enable_ssm ? 1 : 0
  name_prefix        = "${var.project_name}-${var.environment}-role-"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json
  description        = "IAM role for ${var.project_name}-${var.environment} EC2 instance allowing SSM Session Manager"

  tags = local.module_tags
}

resource "aws_iam_role_policy_attachment" "ssm" {
  count      = var.enable_ssm ? 1 : 0
  role       = aws_iam_role.this[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "this" {
  count       = var.enable_ssm ? 1 : 0
  name_prefix = "${var.project_name}-${var.environment}-profile-"
  role        = aws_iam_role.this[0].name

  tags = local.module_tags
}

# -----------------------------------------------------------------------------
# 4. SSH Key Pair Generation
# -----------------------------------------------------------------------------
resource "tls_private_key" "this" {
  count     = var.ssh_key_name == "" && var.ssh_public_key == "" && var.generate_ssh_key ? 1 : 0
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "local_sensitive_file" "private_key" {
  count           = length(tls_private_key.this) > 0 ? 1 : 0
  content         = tls_private_key.this[0].private_key_pem
  filename        = "${path.root}/${var.environment}-k3s-ticket-key.pem"
  file_permission = "0400"
}

resource "aws_key_pair" "this" {
  count      = var.ssh_key_name == "" ? 1 : 0
  key_name   = local.key_name
  public_key = var.ssh_public_key != "" ? var.ssh_public_key : tls_private_key.this[0].public_key_openssh

  tags = local.module_tags
}

locals {
  active_key_name = var.ssh_key_name != "" ? var.ssh_key_name : aws_key_pair.this[0].key_name
}

# -----------------------------------------------------------------------------
# 5. EC2 Instance: k3s-ticket-server
# -----------------------------------------------------------------------------
resource "aws_instance" "k3s_server" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [aws_security_group.this.id]
  key_name                    = local.active_key_name
  iam_instance_profile        = var.enable_ssm ? aws_iam_instance_profile.this[0].name : null
  associate_public_ip_address = true

  root_block_device {
    volume_size           = var.root_volume_size
    volume_type           = var.root_volume_type
    delete_on_termination = var.root_volume_delete_on_termination
    encrypted             = var.root_volume_encrypted

    tags = merge(
      local.module_tags,
      {
        Name = "${local.instance_name}-root-vol"
      }
    )
  }

  user_data = templatefile("${path.module}/templates/user_data.sh.tftpl", {
    environment          = var.environment
    swap_size_gb         = var.swap_size_gb
    swappiness           = var.swappiness
    install_k3s          = var.install_k3s
    install_cert_manager = var.install_cert_manager
    cert_manager_version = var.cert_manager_version
  })

  user_data_replace_on_change = false

  tags = merge(
    local.module_tags,
    {
      Name = local.instance_name
    }
  )

  lifecycle {
    ignore_changes = [ami]
  }
}

# -----------------------------------------------------------------------------
# 6. Static Elastic IP Allocation & Association
# -----------------------------------------------------------------------------
resource "aws_eip" "this" {
  domain   = "vpc"
  instance = aws_instance.k3s_server.id

  tags = merge(
    local.module_tags,
    {
      Name = "${var.project_name}-${var.environment}-eip"
    }
  )
}

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------
# 7. Optional Route 53 DNS Records
# -----------------------------------------------------------------------------
resource "aws_route53_record" "ticket_admin" {
  count   = var.create_route53_records && var.route53_zone_id != "" && var.domain_name != "" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = "ticket-admin.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.this.public_ip]
}

resource "aws_route53_record" "ticket_api" {
  count   = var.create_route53_records && var.route53_zone_id != "" && var.domain_name != "" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = "ticket-api.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.this.public_ip]
}

resource "aws_route53_record" "apex" {
  count   = var.create_route53_records && var.route53_zone_id != "" && var.domain_name != "" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "A"
  ttl     = 300
  records = [aws_eip.this.public_ip]
}
