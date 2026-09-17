#!/usr/bin/env bash
# Oreon (rag-platform) - one-shot local setup.
# Run this from the project root: ./setup.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

echo "==> Checking prerequisites..."

command -v docker >/dev/null 2>&1 || {
  echo "ERROR: Docker is not installed. Get it from https://docs.docker.com/get-docker/ and re-run this script."
  exit 1
}

if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  echo "ERROR: Docker Compose isn't available. Install Docker Desktop (bundles Compose) or the compose plugin."
  exit 1
fi
echo "    Docker + Compose found."

echo "==> Setting up environment file..."
if [ ! -f .env ]; then
  cp .env.example .env
  echo "    Created .env from .env.example."
else
  echo "    .env already exists - leaving it untouched."
fi

# Auto-generate a real JWT secret if the placeholder is still sitting there.
if grep -q "JWT_SECRET=replace-with-a-long-random-string" .env 2>/dev/null; then
  NEW_SECRET=$(openssl rand -base64 48 | tr -d '\n')
  sed -i.bak "s|JWT_SECRET=replace-with-a-long-random-string|JWT_SECRET=${NEW_SECRET}|" .env
  rm -f .env.bak
  echo "    Generated a random JWT_SECRET."
fi

echo ""
echo "==> API keys required (can't be automated - these need your own accounts)"
echo "    NVIDIA_API_KEY     -> get one free at https://build.nvidia.com"
echo "    OPENROUTER_API_KEY -> get one free at https://openrouter.ai"
echo "    Open .env and paste both in before continuing."
echo ""

if grep -Eq "NVIDIA_API_KEY=nvapi-x+$|OPENROUTER_API_KEY=sk-or-v1-x+$" .env 2>/dev/null; then
  read -rp "Press Enter once both keys are set in .env (Ctrl+C to stop and do it now)... " _
fi

echo "==> Building and starting Postgres/pgvector, Redis, Kafka, and the app..."
$COMPOSE up --build -d

echo "==> Waiting for the app to report healthy..."
for i in $(seq 1 30); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo ""
    echo "Oreon is up: http://localhost:8080"
    exit 0
  fi
  sleep 3
done

echo ""
echo "App didn't report healthy within 90s - check logs with: $COMPOSE logs -f app"
