# 인스턴스가 ECR에서 이미지를 당기고 Parameter Store에서 비밀을 읽는다.
# 그 둘 말고는 아무 권한도 주지 않는다.

data "aws_iam_policy_document" "assume_ec2" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "instance" {
  name               = "${var.name}-instance"
  assume_role_policy = data.aws_iam_policy_document.assume_ec2.json
}

# ECR 읽기는 AWS 관리형 정책을 쓴다. 직접 쓰면 액션 목록이 낡는다.
resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# 비밀은 **이 두 개만** 읽을 수 있다. 계정의 다른 파라미터는 못 본다.
data "aws_iam_policy_document" "read_secrets" {
  statement {
    actions = ["ssm:GetParameter", "ssm:GetParameters"]
    resources = [
      aws_ssm_parameter.api_key.arn,
      aws_ssm_parameter.postgres_password.arn,
    ]
  }

  # SecureString을 풀려면 KMS가 필요하다. 기본 키로 암호화했으므로 ssm 경유만 허용한다.
  statement {
    actions   = ["kms:Decrypt"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${var.region}.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "read_secrets" {
  name   = "${var.name}-read-secrets"
  role   = aws_iam_role.instance.id
  policy = data.aws_iam_policy_document.read_secrets.json
}

resource "aws_iam_instance_profile" "instance" {
  name = "${var.name}-instance"
  role = aws_iam_role.instance.name
}
