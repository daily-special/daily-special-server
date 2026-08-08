# AMI를 하드코딩하지 않는다. ID는 리전마다 다르고 몇 주면 낡는다.
#
# **x86_64를 쓴다.** 이미지를 빌드하는 개발 머신이 x86이기 때문이다. arm64(Graviton)가
# 조금 싸지만, amd64 이미지를 arm64 인스턴스에 올리면 컨테이너가 `exec format error`로
# 죽는다 — 실제로 한 번 그렇게 잡아뒀다가 배포 직전에 발견했다.
#
# arm64로 가려면 이미지도 arm64로 빌드해야 한다(`bootBuildImage`의 imagePlatform + qemu).
# 그건 느리고 잘 깨져서, **빌드 머신과 인스턴스를 맞추는 쪽**을 골랐다.
#
# `kernel-default`는 커널이 올라가면 따라 올라간다 — 2026-08-17에 6.1에서 6.18로 바뀐다는
# 공지가 있다. 특정 커널에 묶고 싶으면 `al2023-ami-kernel-6.12-x86_64`처럼 고정한다.
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
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
