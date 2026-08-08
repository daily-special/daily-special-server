# AMI를 하드코딩하지 않는다. ID는 리전마다 다르고 몇 주면 낡는다.
#
# `kernel-default`는 커널이 올라가면 따라 올라간다 — 2026-08-17에 6.1에서 6.18로 바뀐다는
# 공지가 있다. 특정 커널에 묶고 싶으면 `al2023-ami-kernel-6.12-arm64`처럼 고정한다.
data "aws_ssm_parameter" "al2023_arm64" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
}

# 기본 VPC를 쓴다. 인스턴스 하나에 VPC를 새로 짜면 코드가 두 배가 되는데 얻는 게 없다.
# 계정에 기본 VPC가 없으면 여기서 실패한다 — 그때 VPC를 만든다.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}
