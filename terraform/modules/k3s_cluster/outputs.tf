# =============================================================================
# K3S CLUSTER MODULE - OUTPUTS
# =============================================================================

output "instance_id" {
  description = "The EC2 instance ID."
  value       = aws_instance.k3s_server.id
}

output "instance_type" {
  description = "The EC2 instance type."
  value       = aws_instance.k3s_server.instance_type
}

output "instance_private_ip" {
  description = "The internal VPC IPv4 address."
  value       = aws_instance.k3s_server.private_ip
}

output "elastic_ip" {
  description = "The permanent static Elastic IP."
  value       = aws_eip.this.public_ip
}

output "security_group_id" {
  description = "The security group ID."
  value       = aws_security_group.this.id
}

output "ssh_command" {
  description = "SSH connection string."
  value       = "ssh -i \"${var.environment}-k3s-ticket-key.pem\" ubuntu@${aws_eip.this.public_ip}"
}

output "ssm_session_command" {
  description = "AWS SSM Session command."
  value       = "aws ssm start-session --target ${aws_instance.k3s_server.id}"
}

output "kubeconfig_fetch_command" {
  description = "Kubeconfig download command."
  value       = "ssh -i \"${var.environment}-k3s-ticket-key.pem\" ubuntu@${aws_eip.this.public_ip} \"sudo cat /etc/rancher/k3s/k3s.yaml\" | sed 's/127.0.0.1/${aws_eip.this.public_ip}/g' > ~/.kube/mono-ticket-${var.environment}-config"
}

output "dns_records_to_configure" {
  description = "DNS records to configure at registrar (Point to the Elastic IP)."
  value = {
    ticket_admin = {
      record_type = "A"
      host        = "ticket-admin"
      full_domain = "ticket-admin.${var.domain_name != "" ? var.domain_name : "menglanghuo.online"}"
      target_ip   = aws_eip.this.public_ip
      description = "Vue.js Admin SPA Frontend (ticket-admin.menglanghuo.online)"
    }
    ticket_api = {
      record_type = "A"
      host        = "ticket-api"
      full_domain = "ticket-api.${var.domain_name != "" ? var.domain_name : "menglanghuo.online"}"
      target_ip   = aws_eip.this.public_ip
      description = "Spring Boot WebFlux REST API (ticket-api.menglanghuo.online)"
    }
  }
}

output "generated_private_key_path" {
  description = "Local filesystem path where private key was saved."
  value       = length(local_sensitive_file.private_key) > 0 ? local_sensitive_file.private_key[0].filename : "Existing key: ${local.active_key_name}"
}
