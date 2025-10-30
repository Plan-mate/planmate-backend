#!/bin/bash
set -e

APP_NAME="planmate-backend"
IMAGE_TAG=$1
ECR_REPOSITORY="357443655122.dkr.ecr.ap-northeast-2.amazonaws.com/planmate-backend"
NETWORK_NAME="planmate-network"

echo "🔑 Retrieving DB_PASSWORD from AWS Secrets Manager..."
DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id planmate-db-password \
  --query SecretString \
  --output text)

if [ -z "$DB_PASSWORD" ]; then
  echo "❌ Failed to fetch DB_PASSWORD from Secrets Manager!"
  exit 1
fi

echo "✅ DB_PASSWORD loaded successfully."

# Docker 로그인 및 네트워크 설정 등 기존 로직 유지
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin ${ECR_REPOSITORY%/*}

if ! docker network ls --format '{{.Name}}' | grep -w "$NETWORK_NAME" > /dev/null 2>&1; then
  docker network create $NETWORK_NAME
fi

cd /home/ubuntu/planmate-backend/infra

# ⚙️ Docker Compose 실행 시 환경변수로 주입
IMAGE_TAG=$IMAGE_TAG DB_PASSWORD=$DB_PASSWORD docker compose -f docker-compose.yml pull
IMAGE_TAG=$IMAGE_TAG DB_PASSWORD=$DB_PASSWORD docker compose -f docker-compose.yml up -d --force-recreate

docker image prune -f
echo "✅ Deployment completed successfully."
