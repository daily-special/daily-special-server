# 이미지를 ECR에 둔다.
#
# 인스턴스에서 직접 빌드하지 않는 이유는 t4g.small의 2GiB로 Gradle과 빌드팩을 돌리는 것이
# 아슬아슬하고, 빌드가 배포마다 몇 분씩 들기 때문이다.
#
# scp로 실어 보내는 방법도 있지만 그러면 SSH를 열어야 한다. ECR이면 인스턴스 역할로 당겨오므로
# SSH가 필요 없고, **본선에 Fargate로 옮길 때 그대로 쓴다** — 그쪽은 ECR이 필수다.
resource "aws_ecr_repository" "app" {
  name                 = var.name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# 태그 없는 옛 이미지가 쌓이면 요금이 는다. 배포할 때마다 하나씩 남는다.
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "최근 5개만 남긴다"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}
