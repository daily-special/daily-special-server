# 배포 — EC2 한 대

앱과 Postgres를 EC2 한 대에서 `docker compose`로 돌린다. 로컬에서 쓰는 `compose.yaml`을 **그대로** 쓴다 — `deploy` 프로파일만 켠다.

## 왜 EC2인가 (지금은)

Fargate는 컨테이너가 언제든 사라지는 전제라 **Postgres를 거기 둘 수 없다.** 그러면 RDS가 강제되고, 안정적인 주소를 위해 ALB까지 붙는다. 컨테이너 하나 앞의 ALB가 컴퓨트보다 비싸다.

| | EC2 + compose | Fargate + RDS + ALB |
|---|---|---|
| 대략 월 비용 | ~$12 | ~$40 |
| Terraform 분량 | 이 폴더 | 3~4배 |

**본선에는 Fargate로 옮긴다.** 그때는 백엔드 쇼케이스라 값을 하고, 여기서 만든 이미지·ECR·환경변수·헬스체크가 그대로 쓰인다.

## ⚠️ 아키텍처를 맞춘다

**이미지를 빌드하는 머신과 인스턴스의 아키텍처가 같아야 한다.** 지금은 둘 다 x86_64다.

```bash
docker image inspect <이미지> --format '{{.Architecture}}'   # amd64 여야 한다
```

어긋나면 인스턴스에서 컨테이너가 `exec format error`로 죽는다. Terraform은 이걸 못 잡아준다 — apply는 성공하고 서버만 안 뜬다.

## 준비물

- AWS 자격증명 (`aws configure` 또는 환경변수)
- 계정에 **기본 VPC**가 있어야 한다. 없으면 `data.tf`가 실패한다
- Terraform 1.15+, Docker

## 순서

**이미지가 ECR에 없으면 인스턴스가 뜨다가 실패한다.** 그래서 두 번 나눠 apply 한다.

```bash
cp terraform.tfvars.example terraform.tfvars   # 채운다
terraform init

# 1. 이미지 넣을 곳부터
terraform apply -target=aws_ecr_repository.app

ECR=$(terraform output -raw ecr_repository_url)
REGION=$(terraform output -raw region 2>/dev/null || echo ap-northeast-2)

# 2. 이미지 빌드 후 밀어 넣기
cd .. && ./gradlew bootBuildImage --imageName="$ECR:latest" && cd deploy
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${ECR%%/*}"
docker push "$ECR:latest"

# 3. 나머지
terraform apply
```

확인.

```bash
curl "$(terraform output -raw health_url)"
# {"groups":["liveness","readiness"],"status":"UP"}
```

## 다시 배포할 때

```bash
./gradlew bootBuildImage --imageName="$ECR:latest" && docker push "$ECR:latest"
```

그다음 인스턴스를 새로 만든다.

```bash
terraform apply -replace=aws_instance.server
```

> ⚠️ **인스턴스를 새로 만들면 Postgres 데이터가 사라진다.** DB가 인스턴스의 루트 볼륨에 있기 때문이다. 지금은 프로토타입이라 받아들인 것이고, 데이터를 지켜야 하면 DB를 RDS로 빼거나 EBS를 따로 붙인다.

## 열려 있는 것

| 포트 | 누구에게 | 기본값 |
|---|---|---|
| 80 | `allowed_api_cidrs` | **비어 있음 — 아무도 못 들어온다** |
| 22 | `allowed_ssh_cidrs` | 비어 있음 |

실수로 열리는 것보다 실수로 닫히는 편이 낫다고 보고 기본을 닫아뒀다.

`/actuator/health`는 **API 키 없이** 열린다. 배포 확인에 키가 필요하면 곤란해서다.

## 비밀

`api_key`와 `postgres_password`는 Parameter Store에 `SecureString`으로 들어가고, 인스턴스가 **자기 역할로 읽어간다.** user_data에는 값이 없다 — user_data는 메타데이터로 노출되고 콘솔에도 평문으로 보인다.

**다만 값은 Terraform 상태 파일에 평문으로 남는다.** `*.tfstate`를 커밋하지 않는 것이 그래서 중요하다(`.gitignore`에 있다). 상태 파일은 비밀처럼 다룬다.

## 아직 없는 것

- **HTTPS** — 지금 80만 연다. 도메인과 인증서가 붙을 때 같이 한다
- **무중단 배포** — 인스턴스를 새로 만든다. 몇 분 끊긴다
- **백업** — 없다. 위의 데이터 경고와 같은 이유다
- **진짜 인증** — `X-Api-Key`는 자물쇠지 신원 확인이 아니다. 누가 부르는지 구별하지 않는다
