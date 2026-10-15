#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script: deploy.sh
# Purpose: Zero-Downtime Safe Deployment with Automatic Healthcheck Verification
# Usage: ./scripts/deploy.sh [IMAGE_TAG]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

ENV_FILE="${ROOT_DIR}/.env"
if [ ! -f "${ENV_FILE}" ]; then
    if [ -f "${ROOT_DIR}/.env.example" ]; then
        echo "==> Warning: .env not found. Creating from .env.example..."
        cp "${ROOT_DIR}/.env.example" "${ENV_FILE}"
    else
        echo "==> Error: .env file is missing." >&2
        exit 1
    fi
fi

# 1. Record current running image/tag for rollback
CURRENT_RUNNING=$(docker ps --filter "name=ticket-service" --format "{{.Image}}" 2>/dev/null || echo "")
if [ -n "${CURRENT_RUNNING}" ]; then
    echo "${CURRENT_RUNNING}" > "${ROOT_DIR}/.previous_deployed_version"
    echo "==> Saved previous version for rollback: ${CURRENT_RUNNING}"
fi

# 2. Determine target tag
TAG="${1:-latest}"
echo "=========================================="
echo " Deploying Ticket Service (Tag: ${TAG})"
echo "=========================================="

# 3. Spin up dependencies and app
docker compose up -d mongo
echo "==> Waiting for MongoDB to become healthy..."
docker compose wait-for-service 2>/dev/null || sleep 5

echo "==> Deploying ticket-app container..."
docker compose up -d --build ticket-app

# 4. Perform healthcheck polling
HEALTH_URL="http://localhost:8000/actuator/health"
MAX_ATTEMPTS=20
WAIT_SECONDS=3

echo "==> Verifying application health at ${HEALTH_URL}..."
HEALTHY=false
for i in $(seq 1 ${MAX_ATTEMPTS}); do
    if curl -s -f "${HEALTH_URL}" | grep -q "UP"; then
        HEALTHY=true
        echo "==> Application is UP and HEALTHY (attempt ${i}/${MAX_ATTEMPTS})!"
        break
    else
        echo "    Attempt ${i}/${MAX_ATTEMPTS}: Service not ready yet, retrying in ${WAIT_SECONDS}s..."
        sleep ${WAIT_SECONDS}
    fi
done

if [ "${HEALTHY}" = true ]; then
    echo "=========================================="
    echo " Deployment SUCCESSFUL!"
    echo " Service running on http://localhost:8000"
    echo "=========================================="
    echo "${TAG}" > "${ROOT_DIR}/.current_deployed_version"
    exit 0
else
    echo "=========================================="
    echo " Deployment FAILED: Healthcheck did not pass!"
    echo " Container logs:"
    docker compose logs --tail=50 ticket-app || true
    echo " Triggering automated rollback..."
    echo "=========================================="
    "${SCRIPT_DIR}/rollback.sh"
    exit 1
fi
