# =============================================================================
# ENVIRONMENT: DEV - OUTPUTS
# Forwards module outputs for easy access
# =============================================================================

output "elastic_ip" {
  description = "The static Elastic IP address of the Dev K3s node."
  value       = module.k3s_cluster.elastic_ip
}

output "instance_id" {
  description = "The EC2 instance ID."
  value       = module.k3s_cluster.instance_id
}

output "ssh_command" {
  description = "SSH connection command."
  value       = module.k3s_cluster.ssh_command
}

output "ssm_session_command" {
  description = "AWS SSM terminal access command."
  value       = module.k3s_cluster.ssm_session_command
}

output "kubeconfig_fetch_command" {
  description = "Download Dev Kubeconfig command."
  value       = module.k3s_cluster.kubeconfig_fetch_command
}

output "dns_records_to_configure" {
  description = "DNS records to configure at registrar."
  value       = module.k3s_cluster.dns_records_to_configure
}

output "vpc_id" {
  description = "The VPC ID."
  value       = module.vpc.vpc_id
}

output "public_subnet_id" {
  description = "The Public Subnet ID."
  value       = module.vpc.public_subnet_id
}
