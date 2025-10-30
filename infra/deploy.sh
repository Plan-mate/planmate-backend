#!/bin/bash
set -e

APP_NAME="planmate-backend"
IMAGE_TAG=$1
ECR_REPOSITORY="357443655122.dkr.ecr.ap-northeast-2.amazonaws.com/planmate-backend"
NETWORK_NAME="planmate-network"

cd "$(dirname "$0")"
echo "🚀 배포 스크립트 시작"
echo "📦 이미지 태그: $IMAGE_TAG"
echo "📁 현재 경로: $(pwd)"

echo "🔹 AWS ECR 로그인 중..."
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin ${ECR_REPOSITORY%/*}

echo "🔹 Docker 네트워크 확인 중..."
if ! docker network ls --format '{{.Name}}' | grep -w "$NETWORK_NAME" > /dev/null 2>&1; then
  echo "🛠 네트워크가 없습니다. 새로 생성합니다: $NETWORK_NAME"
  docker network create $NETWORK_NAME
else
  echo "✅ 기존 네트워크 사용 중: $NETWORK_NAME"
fi

echo "🔹 Docker Compose Pull"
IMAGE_TAG=$IMAGE_TAG docker compose -f docker-compose.yml pull

echo "🔹 Docker Compose Up (컨테이너 재생성)"
IMAGE_TAG=$IMAGE_TAG docker compose -f docker-compose.yml up -d --force-recreate

echo "🧹 오래된 이미지 정리 중..."
docker image prune -f || true

echo "✅ 배포 완료: $IMAGE_TAG"
