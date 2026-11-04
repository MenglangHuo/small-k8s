# =============================================================================
# EC2 Compute Instance & SSH Key Management
# Follows: docs/02_infrastructure_setup.md (Phase 3 & 5)
# User Choice: EC2 instance type is set to t3.medium
# =============================================================================

# -----------------------------------------------------------------------------
# SSH Key Pair Generation / Association
# -----------------------------------------------------------------------------

# Auto-generate RSA private key if no existing key or public key is supplied
resource "tls_private_key" "k3s_key" {
  count     = var.ssh_key_name == "" && var.ssh_public_key == "" && var.generate_ssh_key ? 1 : 0
  algorithm = "RSA"
  rsa_bits  = 4096
}

# Save generated private key to project root with chmod 0400 permissions
resource "local_sensitive_file" "private_key_pem" {
  count           = length(tls_private_key.k3s_key) > 0 ? 1 : 0
  content         = tls_private_key.k3s_key[0].private_key_pem
  filename        = "${path.module}/../k3s-ticket-key.pem"
  file_permission = "0400"
}

# Register AWS Key Pair if not referencing an existing key_name
resource "aws_key_pair" "this" {
  count      = var.ssh_key_name == "" ? 1 : 0
  key_name   = "k3s-ticket-key"
  public_key = var.ssh_public_key != "" ? var.ssh_public_key : tls_private_key.k3s_key[0].public_key_openssh

  tags = {
    Name = "k3s-ticket-key"
  }
}

locals {
  active_key_name = var.ssh_key_name != "" ? var.ssh_key_name : aws_key_pair.this[0].key_name
}

# -----------------------------------------------------------------------------
# EC2 Instance: k3s-ticket-server
# -----------------------------------------------------------------------------
resource "aws_instance" "k3s_server" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.instance_type       # User specified: t3.medium (2 vCPU, 4 GiB RAM)
  subnet_id                   = aws_subnet.public[0].id # Public Subnet 1 (Required for EIP & $0 NAT cost)
  vpc_security_group_ids      = [aws_security_group.k3s_web.id]
  key_name                    = local.active_key_name
  iam_instance_profile        = var.enable_ssm ? aws_iam_instance_profile.ec2_profile[0].name : null
  associate_public_ip_address = true

  root_block_device {
    volume_size           = var.root_volume_size # 30 GiB gp3
    volume_type           = var.root_volume_type # gp3
    delete_on_termination = var.root_volume_delete_on_termination
    encrypted             = var.root_volume_encrypted

    tags = {
      Name = "${var.instance_name}-root-vol"
    }
  }

  user_data = templatefile("${path.module}/templates/user_data.sh.tftpl", {
    swap_size_gb         = var.swap_size_gb
    swappiness           = var.swappiness
    install_k3s          = var.install_k3s
    install_cert_manager = var.install_cert_manager
    cert_manager_version = var.cert_manager_version
  })

  user_data_replace_on_change = false

  tags = {
    Name = var.instance_name
  }

  lifecycle {
    ignore_changes = [
      ami # Prevent accidental replacement if new Canonical Ubuntu patch release publishes
    ]
  }
}
