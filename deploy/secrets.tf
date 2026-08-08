# 비밀을 user_data에 넣지 않는다.
#
# user_data는 인스턴스 메타데이터로 노출되고, EC2를 describe할 수 있는 누구나 읽을 수 있으며,
# 콘솔에도 평문으로 보인다. Parameter Store에 넣고 인스턴스가 역할로 읽어간다.
#
# 값 자체는 Terraform 상태에 남는다 — 상태 파일을 비밀처럼 다뤄야 한다는 뜻이다.
resource "aws_ssm_parameter" "api_key" {
  name  = "/${var.name}/api-key"
  type  = "SecureString"
  value = var.api_key
}

resource "aws_ssm_parameter" "postgres_password" {
  name  = "/${var.name}/postgres-password"
  type  = "SecureString"
  value = var.postgres_password
}
