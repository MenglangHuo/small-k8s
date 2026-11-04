#!/usr/bin/env bash
# ==============================================================================
# Mono-Ticket Blue/Green Traffic Switcher
# Usage:
#   ./switch.sh green    # Cut over live production traffic to Green
#   ./switch.sh blue     # Instant rollback live production traffic to Blue
#   ./switch.sh status   # Show active color and endpoint mappings
# ==============================================================================

set -euo pipefail

NAMESPACE="ticket-prod"
TARGET="${1:-status}"

COLOR_GREEN="\033[0;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_RED="\033[0;31m"
COLOR_BLUE="\033[0;34m"
COLOR_RESET="\033[0m"

log_info() { echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"; }
log_success() { echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $1"; }
log_warn() { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $1"; }
log_err() { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"; }

get_active_color() {
  kubectl get service ticket-api-svc -n "${NAMESPACE}" -o jsonpath='{.spec.selector.version}' 2>/dev/null || echo "unknown"
}

show_status() {
  local active
  active=$(get_active_color)
  log_info "Active production deployment: ${COLOR_YELLOW}${active}${COLOR_RESET}"
  echo ""
  log_info "Current endpoints behind ticket-api-svc:"
  kubectl get endpoints ticket-api-svc -n "${NAMESPACE}"
  echo ""
  log_info "All ticket-api pods:"
  kubectl get pods -l app.kubernetes.io/name=ticket-api -n "${NAMESPACE}" -L version
}

switch_to() {
  local target="$1"
  local current
  current=$(get_active_color)

  if [ "${current}" == "${target}" ]; then
    log_warn "Production service is already pointing to '${target}'."
    return 0
  fi

  log_info "Verifying that 'ticket-api-${target}' deployment exists and has ready replicas..."
  if ! kubectl get deployment "ticket-api-${target}" -n "${NAMESPACE}" &>/dev/null; then
    log_err "Deployment 'ticket-api-${target}' does not exist in namespace '${NAMESPACE}'!"
    exit 1
  fi

  log_info "Waiting for at least 1 pod of 'ticket-api-${target}' to report Ready..."
  kubectl wait --for=condition=ready pod \
    -l app.kubernetes.io/name=ticket-api,version="${target}" \
    -n "${NAMESPACE}" --timeout=120s

  log_info "Executing atomic 0ms cutover: routing ticket-api-svc to version='${target}'..."
  kubectl patch service ticket-api-svc -n "${NAMESPACE}" \
    -p "{\"spec\":{\"selector\":{\"version\":\"${target}\"}}}"

  log_success "Cutover complete! Live traffic is now served by '${target}'."
  echo ""
  show_status
}

case "${TARGET}" in
  green|blue)
    switch_to "${TARGET}"
    ;;
  status)
    show_status
    ;;
  *)
    echo "Usage: $0 {green|blue|status}"
    exit 1
    ;;
esac
