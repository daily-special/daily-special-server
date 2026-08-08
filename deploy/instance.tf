# 80과 22 말고는 들어올 길이 없다. 기본값은 **아무 데서도 못 들어온다** —
# 실수로 열리는 것보다 실수로 닫히는 편이 낫다.
resource "aws_security_group" "instance" {
  name        = "${var.name}-instance"
  description = "daily-special 서버"
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "api" {
  for_each = toset(var.allowed_api_cidrs)

  security_group_id = aws_security_group.instance.id
  description       = "API"
  cidr_ipv4         = each.value
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each = toset(var.allowed_ssh_cidrs)

  security_group_id = aws_security_group.instance.id
  description       = "SSH"
  cidr_ipv4         = each.value
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
}

# 나가는 길은 열어야 한다 — ECR·SSM·패키지 저장소를 부른다.
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.instance.id
  description       = "전부 허용"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_instance" "server" {
  ami                  = data.aws_ssm_parameter.al2023_arm64.value
  instance_type        = var.instance_type
  subnet_id            = data.aws_subnets.default.ids[0]
  iam_instance_profile = aws_iam_instance_profile.instance.name
  key_name             = var.key_name

  vpc_security_group_ids = [aws_security_group.instance.id]

  metadata_options {
    # IMDSv2 강제. v1을 열어두면 SSRF 하나로 인스턴스 자격증명이 샌다.
    http_tokens   = "required"
    http_endpoint = "enabled"
  }

  root_block_device {
    volume_size = var.root_volume_size
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = templatefile("${path.module}/user_data.sh", {
    region      = var.region
    name        = var.name
    image       = "${aws_ecr_repository.app.repository_url}:latest"
    compose_b64 = base64encode(file("${path.module}/../compose.yaml"))
  })

  # user_data가 바뀌면 인스턴스를 다시 만든다. 안 그러면 스크립트만 바뀌고
  # 이미 뜬 박스는 옛 설정 그대로 남아, 코드와 실물이 조용히 갈라진다.
  #
  # 대가: **Postgres 데이터가 사라진다.** 지금은 프로토타입이라 받아들이지만,
  # 데이터를 지켜야 하면 DB를 RDS로 빼거나 EBS를 따로 붙여야 한다.
  user_data_replace_on_change = true

  tags = { Name = var.name }
}

# 주소가 배포마다 바뀌면 클라이언트 설정을 매번 고쳐야 한다.
resource "aws_eip" "server" {
  instance = aws_instance.server.id
  domain   = "vpc"
}
