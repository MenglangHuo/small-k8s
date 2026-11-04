# =============================================================================
# Optional Route 53 DNS Records
# Follows: docs/02_infrastructure_setup.md (Phase 7)
#
# Automatically provisions the 3 required A records pointing to the Elastic IP:
# 1. @ (root domain)  -> Elastic IP (Vue.js SPA)
# 2. www               -> Elastic IP (Vue.js SPA)
# 3. api               -> Elastic IP (Spring Boot WebFlux API)
#
# Only provisioned if create_route53_records = true and route53_zone_id is provided.
# If managing DNS outside AWS (e.g. Namecheap, Cloudflare), set create_route53_records = false.
# =============================================================================

resource "aws_route53_record" "apex" {
  count   = var.create_route53_records && var.route53_zone_id != "" && var.domain_name != "" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "A"
  ttl     = 300
  records = [aws_eip.this.public_ip]
}

resource "aws_route53_record" "www" {
  count   = var.create_route53_records && var.route53_zone_id != "" && var.domain_name != "" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = "www.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.this.public_ip]
}

resource "aws_route53_record" "api" {
  count   = var.create_route53_records && var.route53_zone_id != "" && var.domain_name != "" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = "api.${var.domain_name}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.this.public_ip]
}
