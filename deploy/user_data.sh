#!/usr/bin/env bash
# EC2 첫 기동에 한 번 돈다. 실패하면 /var/log/cloud-init-output.log에 남는다.
set -euo pipefail

REGION="${region}"
NAME="${name}"
IMAGE="${image}"

dnf -y update
dnf -y install docker
systemctl enable --now docker

# compose는 dnf에 없다. 플러그인을 직접 놓는다.
install -d /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/download/v2.40.3/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

install -d /opt/daily-special
cd /opt/daily-special

# 비밀은 user_data에 없다. 인스턴스 역할로 Parameter Store에서 읽어온다.
API_KEY="$(aws ssm get-parameter --region "$REGION" --name "/$NAME/api-key" --with-decryption --query Parameter.Value --output text)"
PG_PASSWORD="$(aws ssm get-parameter --region "$REGION" --name "/$NAME/postgres-password" --with-decryption --query Parameter.Value --output text)"

# 자물쇠 없이 공개 인터넷에 뜨는 것을 여기서 막는다.
# compose의 `$${VAR:?}`로는 못 막는다 — 프로파일과 무관하게 파싱 때 평가돼서 로컬까지 막힌다.
if [ -z "$API_KEY" ]; then
  echo "DAILY_SPECIAL_API_KEY가 비어 있다. 자물쇠 없이 띄우지 않는다." >&2
  exit 1
fi

cat > .env <<ENV
APP_IMAGE=$IMAGE
DAILY_SPECIAL_API_KEY=$API_KEY
POSTGRES_PASSWORD=$PG_PASSWORD
ENV
chmod 600 .env

# compose 파일을 여기 실어 보낸다. GitHub에서 받아오면 배포가 외부 가용성에 묶이고,
# main이 앞서가면 **이 코드가 만든 것과 다른 것이 뜬다.**
# base64로 넣는 이유는 compose 안의 `$${...}`가 템플릿과 충돌하지 않게 하려는 것이다.
echo "${compose_b64}" | base64 -d > compose.yaml

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$${IMAGE%%/*}"

docker compose --profile deploy up -d
