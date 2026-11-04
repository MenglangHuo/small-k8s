# =============================================================================
# Outputs for Mono-Ticket Infrastructure
# =============================================================================

# -----------------------------------------------------------------------------
# Networking Outputs
# -----------------------------------------------------------------------------
output "vpc_id" {
  description = "The ID of the custom VPC (mono-ticket-vpc)."
  value       = aws_vpc.this.id
}

output "vpc_cidr" {
  description = "The CIDR block of the VPC."
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the 2 public subnets across Availability Zones."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the 2 private subnets across Availability Zones (reserved for DB tier)."
  value       = aws_subnet.private[*].id
}

output "security_group_id" {
  description = "The ID of the k3s-web-sg security group."
  value       = aws_security_group.k3s_web.id
}

# -----------------------------------------------------------------------------
# Compute & Static IP Outputs
# -----------------------------------------------------------------------------
output "instance_id" {
  description = "The EC2 instance ID of k3s-ticket-server."
  value       = aws_instance.k3s_server.id
}

output "instance_type" {
  description = "The instance type of the provisioned server (t3.medium)."
  value       = aws_instance.k3s_server.instance_type
}

output "instance_private_ip" {
  description = "The internal VPC IPv4 address of the EC2 server."
  value       = aws_instance.k3s_server.private_ip
}

output "elastic_ip" {
  description = "The permanent static Elastic IPv4 address attached to k3s-ticket-server."
  value       = aws_eip.this.public_ip
}

# -----------------------------------------------------------------------------
# Operational & Access Helpers
# -----------------------------------------------------------------------------
output "ssh_command" {
  description = "Direct SSH command to connect to your K3s server."
  value       = "ssh -i \"k3s-ticket-key.pem\" ubuntu@${aws_eip.this.public_ip}"
}

output "ssm_session_command" {
  description = "AWS SSM Session Manager command to access server terminal without SSH keys."
  value       = "aws ssm start-session --target ${aws_instance.k3s_server.id} --region ${var.aws_region}"
}

output "kubeconfig_fetch_command" {
  description = "Command to download cluster kubeconfig to your local machine and replace localhost with the Elastic IP."
  value       = "ssh -i \"k3s-ticket-key.pem\" ubuntu@${aws_eip.this.public_ip} \"sudo cat /etc/rancher/k3s/k3s.yaml\" | sed 's/127.0.0.1/${aws_eip.this.public_ip}/g' > ~/.kube/mono-ticket-config"
}

output "dns_records_to_configure" {
  description = "The 3 DNS A records required at your domain registrar pointing to the Elastic IP (docs Phase 7)."
  value = {
    root_domain = {
      record_type = "A"
      host        = "@"
      target_ip   = aws_eip.this.public_ip
      description = "Vue.js Admin SPA Frontend"
    }
    www_subdomain = {
      record_type = "A"
      host        = "www"
      target_ip   = aws_eip.this.public_ip
      description = "Vue.js Admin SPA Frontend"
    }
    api_subdomain = {
      record_type = "A"
      host        = "api"
      target_ip   = aws_eip.this.public_ip
      description = "Spring Boot WebFlux REST API Backend"
    }
  }
}

output "generated_private_key_path" {
  description = "Local filesystem path where the generated SSH private key was saved (if auto-generated)."
  value       = length(local_sensitive_file.private_key_pem) > 0 ? local_sensitive_file.private_key_pem[0].filename : "Existing key used: ${local.active_key_name}"
}
