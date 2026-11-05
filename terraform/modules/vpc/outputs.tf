# =============================================================================
# VPC MODULE - OUTPUTS
# =============================================================================

output "vpc_id" {
  description = "The ID of the VPC."
  value       = aws_vpc.this.id
}

output "vpc_cidr" {
  description = "The CIDR block of the VPC."
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_id" {
  description = "The ID of the public subnet."
  value       = aws_subnet.public.id
}

output "private_subnet_id" {
  description = "The ID of the private subnet."
  value       = aws_subnet.private.id
}

output "internet_gateway_id" {
  description = "The ID of the Internet Gateway."
  value       = aws_internet_gateway.this.id
}

output "availability_zone" {
  description = "The availability zone used for subnets."
  value       = local.selected_az
}
