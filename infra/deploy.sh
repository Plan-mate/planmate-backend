#!/bin/bash
set -e

APP_NAME="planmate-backend"
IMAGE_TAG=$1
ECR_REPOSITORY="357443655122.dkr.ecr.ap-northeast-2.amazonaws.com/planmate-backend"
NETWORK_NAME="planmate-network"
SECRET_ID="/secret/planmate"

echo "🔑 Retrieving DB_PASSWORD from AWS Secrets Manager..."
RAW_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id $SECRET_ID \
  --query SecretString \
  --output text)

if [ -z "$RAW_SECRET" ]; then
  echo "❌ Failed to fetch secret from Secrets Manager!"
  exit 1
fi

DB_PASSWORD=$(echo "$RAW_SECRET" | jq -r '.DB_PASSWORD')

if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" == "null" ]; then
  echo "❌ DB_PASSWORD not found in Secrets JSON!"
  exit 1
fi

aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin ${ECR_REPOSITORY%/*}

if ! docker network ls --format '{{.Name}}' | grep -w "$NETWORK_NAME" > /dev/null 2>&1; then
  docker network create $NETWORK_NAME
fi

cd /home/ubuntu/planmate-backend/infra

IMAGE_TAG=$IMAGE_TAG DB_PASSWORD=$DB_PASSWORD docker compose -f docker-compose.yml pull
IMAGE_TAG=$IMAGE_TAG DB_PASSWORD=$DB_PASSWORD docker compose -f docker-compose.yml up -d --force-recreate --remove-orphans

docker image prune -f

echo "✅ Deployment completed successfully with tag: $IMAGE_TAG"
