output "public_ip" {
  description = "서버 주소. 고정 IP다"
  value       = aws_eip.server.public_ip
}

output "health_url" {
  description = "자물쇠 없이 열려 있다. 여기가 200이면 뜬 것이다"
  value       = "http://${aws_eip.server.public_ip}/actuator/health"
}

output "ecr_repository_url" {
  description = "이미지를 밀어 넣을 곳"
  value       = aws_ecr_repository.app.repository_url
}

output "region" {
  description = "배포 리전. 배포 스크립트가 ECR 로그인에 쓴다"
  value       = var.region
}
