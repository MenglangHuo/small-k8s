#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script: rollback.sh
# Purpose: Emergency Rollback to the Previous Known-Good Container Release
# Usage: ./scripts/rollback.sh
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

PREV_VERSION_FILE="${ROOT_DIR}/.previous_deployed_version"

echo "=========================================="
echo " Initiating Service Rollback"
echo "=========================================="

if [ ! -f "${PREV_VERSION_FILE}" ]; then
    echo "==> Warning: No previous version record found (.previous_deployed_version)."
    echo "==> Restarting container using previous Docker cache..."
    docker compose down ticket-app || true
    docker compose up -d ticket-app
else
    PREV_IMAGE=$(cat "${PREV_VERSION_FILE}")
    echo "==> Rolling back to previous stable image: ${PREV_IMAGE}"
    
    # Retag previous image to latest so compose can launch it
    docker tag "${PREV_IMAGE}" ticket-service:latest 2>/dev/null || true
    
    docker compose stop ticket-app
    docker compose rm -f ticket-app
    docker compose up -d --no-build ticket-app
fi

# Verify rollback health
HEALTH_URL="http://localhost:8000/actuator/health"
MAX_ATTEMPTS=15
WAIT_SECONDS=3

echo "==> Verifying rollback state at ${HEALTH_URL}..."
HEALTHY=false
for i in $(seq 1 ${MAX_ATTEMPTS}); do
    if curl -s -f "${HEALTH_URL}" | grep -q "UP"; then
        HEALTHY=true
        echo "==> Rolled-back application is UP and HEALTHY!"
        break
    else
        echo "    Waiting for service to recover (${i}/${MAX_ATTEMPTS})..."
        sleep ${WAIT_SECONDS}
    fi
done

if [ "${HEALTHY}" = true ]; then
    echo "=========================================="
    echo " Rollback completed SUCCESSFULLY!"
    echo "=========================================="
    exit 0
else
    echo "=========================================="
    echo " Rollback ERROR: Service still failing!"
    docker compose logs --tail=50 ticket-app || true
    echo "=========================================="
    exit 1
fi
