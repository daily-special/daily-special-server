# 버전을 고정한다. 안 하면 몇 달 뒤에 같은 코드가 다른 인프라를 만든다.
terraform {
  required_version = "~> 1.15"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.58"
    }
  }

  # 상태는 지금 로컬 파일이다. 혼자 쓰고 배포 대상이 하나라 그걸로 충분하다.
  #
  # 여럿이 만지게 되면 S3 백엔드로 옮긴다. 잠금에 DynamoDB는 더 이상 필요 없다 —
  # 폐기됐고 S3 자체 잠금(`use_lockfile`)으로 대체됐다.
  #
  #   backend "s3" {
  #     bucket       = "..."
  #     key          = "daily-special-server/terraform.tfstate"
  #     region       = "ap-northeast-2"
  #     encrypt      = true
  #     use_lockfile = true
  #   }
}

provider "aws" {
  region = var.region

  default_tags {
    # 태그를 리소스마다 손으로 붙이지 않는다. 하나라도 빠지면 나중에 비용을 못 가른다.
    tags = {
      Project   = "daily-special"
      Component = "server"
      ManagedBy = "terraform"
    }
  }
}
