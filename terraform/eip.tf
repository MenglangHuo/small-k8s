# =============================================================================
# Elastic IP (EIP) Allocation & Association
# Follows: docs/02_infrastructure_setup.md (Phase 4)
#
# A static Elastic IP ensures DNS A records and Let's Encrypt certificate renewals
# remain permanently stable across instance restarts.
# =============================================================================

resource "aws_eip" "this" {
  domain   = "vpc"
  instance = aws_instance.k3s_server.id

  # AWS best practice: ensure IGW exists before associating EIP
  depends_on = [aws_internet_gateway.this]

  tags = {
    Name = "k3s-ticket-eip"
  }
}
