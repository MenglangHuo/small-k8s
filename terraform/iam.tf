# =============================================================================
# IAM Configuration for EC2 Instance
# Provides AWS Systems Manager (SSM) Session Manager support (AWS Best Practice)
# =============================================================================

data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2_role" {
  count              = var.enable_ssm ? 1 : 0
  name_prefix        = "${var.project_name}-ec2-role-"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json
  description        = "IAM role for ${var.project_name} EC2 instance allowing SSM Session Manager"

  tags = {
    Name = "${var.project_name}-ec2-role"
  }
}

resource "aws_iam_role_policy_attachment" "ssm_managed" {
  count      = var.enable_ssm ? 1 : 0
  role       = aws_iam_role.ec2_role[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  count       = var.enable_ssm ? 1 : 0
  name_prefix = "${var.project_name}-instance-profile-"
  role        = aws_iam_role.ec2_role[0].name

  tags = {
    Name = "${var.project_name}-instance-profile"
  }
}
