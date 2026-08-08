variable "region" {
  description = "배포 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "name" {
  description = "리소스 이름의 앞머리"
  type        = string
  default     = "daily-special-server"
}

variable "instance_type" {
  description = <<-EOT
    EC2 인스턴스 타입.

    **이미지를 빌드하는 머신과 아키텍처가 같아야 한다.** 지금은 x86_64다 —
    amd64 이미지를 arm64 인스턴스에 올리면 컨테이너가 exec format error로 죽는다.
  EOT
  type        = string
  default     = "t3.small"
}

variable "root_volume_size" {
  description = "루트 볼륨 크기(GiB). Postgres 데이터도 여기 있다"
  type        = number
  default     = 20
}

variable "allowed_api_cidrs" {
  description = <<-EOT
    API(80)에 접근할 수 있는 CIDR.

    기본은 아무 데서도 못 들어온다. 심사용으로 열려면 ["0.0.0.0/0"]을 명시적으로 준다 —
    실수로 열리는 것보다 실수로 닫히는 편이 낫다.
  EOT
  type        = list(string)
  default     = []
}

variable "allowed_ssh_cidrs" {
  description = "SSH(22)를 열 CIDR. 비워두면 SSH를 아예 열지 않는다"
  type        = list(string)
  default     = []
}

variable "key_name" {
  description = "SSH 키 페어 이름. 비우면 키를 붙이지 않는다"
  type        = string
  default     = null
}

variable "app_image" {
  description = "앱 이미지 참조. 지금은 인스턴스에서 직접 빌드하지 않고 tar로 실어 보낸다"
  type        = string
  default     = "daily-special-server:latest"
}

variable "api_key" {
  description = "X-Api-Key 값. 비우면 서버의 자물쇠가 꺼진다"
  type        = string
  sensitive   = true

  validation {
    # 자물쇠 없이 공개 인터넷에 뜨는 것을 apply 단계에서 막는다.
    condition     = length(trimspace(var.api_key)) >= 16
    error_message = "api_key는 최소 16자여야 한다. 비우면 누구나 남의 관계를 바꿀 수 있다."
  }
}

variable "postgres_password" {
  description = "Postgres 비밀번호"
  type        = string
  sensitive   = true

  validation {
    condition     = length(trimspace(var.postgres_password)) >= 12
    error_message = "postgres_password는 최소 12자여야 한다."
  }
}
