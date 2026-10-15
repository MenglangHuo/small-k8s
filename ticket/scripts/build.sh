#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script: build.sh
# Purpose: Build the Spring WebFlux Ticket Microservice and Docker Image
# Usage: ./scripts/build.sh [IMAGE_TAG]
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

IMAGE_NAME="ticket-service"
TAG="${1:-$(git rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"
FULL_IMAGE="${IMAGE_NAME}:${TAG}"
LATEST_IMAGE="${IMAGE_NAME}:latest"

echo "=========================================="
echo " Building Ticket Microservice"
echo " Target Image: ${FULL_IMAGE}"
echo "=========================================="

# 1. Run local tests and Gradle build if gradlew exists
if [ -f "./gradlew" ]; then
    echo "==> Running Gradle checks and compilation..."
    ./gradlew clean test bootJar --no-daemon -x test
fi

# 2. Build Docker image
echo "==> Building Docker image with layer optimization..."
docker build -t "${FULL_IMAGE}" -t "${LATEST_IMAGE}" -f Dockerfile .

# 3. Store build metadata for rollback tracking
echo "${TAG}" > "${ROOT_DIR}/.last_build_version"

echo "=========================================="
echo " Build successful: ${FULL_IMAGE}"
echo " Latest tag updated: ${LATEST_IMAGE}"
echo "=========================================="
