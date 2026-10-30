#!/usr/bin/env bash
# ==============================================================================
# mono-ticket Kubernetes Deployment Script
# Target: K3s single-node cluster (AWS EC2 t3.micro/t2.micro)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="ticket-prod"

COLOR_GREEN="\033[0;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_RED="\033[0;31m"
COLOR_BLUE="\033[0;34m"
COLOR_RESET="\033[0m"

log_info() { echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"; }
log_success() { echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $1"; }
log_warn() { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $1"; }
log_err() { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"; }

check_prerequisites() {
  log_info "Checking prerequisites..."
  if ! command -v kubectl &>/dev/null; then
    log_err "kubectl is not installed or not in PATH."
    exit 1
  fi

  # Check cluster connectivity
  if ! kubectl cluster-info &>/dev/null; then
    log_err "Cannot connect to Kubernetes cluster. Is K3s running?"
    exit 1
  fi

  # Warn if placeholder domain is still in 06-ingress.yaml
  if grep -q "yourdomain.com" "${SCRIPT_DIR}/06-ingress.yaml"; then
    log_warn "Placeholder 'yourdomain.com' detected in 06-ingress.yaml!"
    log_warn "Please update 06-ingress.yaml with your registered domain name."
  fi

  # Warn if placeholder image user is still in deployment manifests
  if grep -q "yourdockeruser" "${SCRIPT_DIR}/04-ticket-api.yaml" || grep -q "yourdockeruser" "${SCRIPT_DIR}/05-ticket-admin.yaml"; then
    log_warn "Placeholder 'yourdockeruser' detected in deployment manifests!"
    log_warn "Please update image names with your actual Docker registry username."
  fi
}

deploy_all() {
  log_info "1. Applying Namespace: ${NAMESPACE}..."
  kubectl apply -f "${SCRIPT_DIR}/00-namespace.yaml"

  # Auto-apply 02-secrets.yaml if present locally
  if [ -f "${SCRIPT_DIR}/02-secrets.yaml" ]; then
    log_info "Applying secrets from ${SCRIPT_DIR}/02-secrets.yaml..."
    kubectl apply -f "${SCRIPT_DIR}/02-secrets.yaml"
  fi

  if ! kubectl get secret ticket-secrets -n "${NAMESPACE}" &>/dev/null; then
    log_err "Secret 'ticket-secrets' is missing in namespace '${NAMESPACE}'."
    log_err "Please create it from ${SCRIPT_DIR}/02-secrets.example.yaml:"
    log_err "  cp ${SCRIPT_DIR}/02-secrets.example.yaml ${SCRIPT_DIR}/02-secrets.yaml"
    log_err "  (edit credentials) && kubectl apply -f ${SCRIPT_DIR}/02-secrets.yaml"
    exit 1
  fi

  log_info "2. Applying non-sensitive ConfigMap..."
  kubectl apply -f "${SCRIPT_DIR}/01-configmap.yaml"

  log_info "3. Deploying In-cluster MongoDB..."
  kubectl apply -f "${SCRIPT_DIR}/03-mongodb.yaml"
  kubectl apply -f "${SCRIPT_DIR}/08-mongodb-network-policy.yaml"

  log_info "Waiting for MongoDB pod to become ready (up to 120s)..."
  kubectl wait --for=condition=ready pod \
    -l app.kubernetes.io/name=mongodb \
    -n "${NAMESPACE}" --timeout=120s || {
      log_warn "MongoDB took longer than 120s to report ready. Checking status..."
      kubectl get pods -n "${NAMESPACE}"
    }

  log_info "4. Deploying Spring Boot Reactive WebFlux API..."
  kubectl apply -f "${SCRIPT_DIR}/04-ticket-api.yaml"

  log_info "5. Deploying Vue.js SPA Admin Frontend..."
  kubectl apply -f "${SCRIPT_DIR}/05-ticket-admin.yaml"

  if [ -f "${SCRIPT_DIR}/07-cluster-issuer.yaml" ]; then
    log_info "6. Applying Cert-Manager ClusterIssuer..."
    kubectl apply -f "${SCRIPT_DIR}/07-cluster-issuer.yaml" || log_warn "Cert-manager might not be installed yet."
  fi

  log_info "7. Applying Ingress Routing Rules..."
  kubectl apply -f "${SCRIPT_DIR}/06-ingress.yaml"

  log_success "Deployment applied successfully!"
  echo ""
  log_info "Checking resources in namespace '${NAMESPACE}':"
  kubectl get pods,svc,ingress,pvc -n "${NAMESPACE}"
}

restart_apps() {
  log_warn "Restarting single-replica workloads; brief downtime is expected."
  kubectl rollout restart deployment/ticket-api -n "${NAMESPACE}"
  kubectl rollout restart deployment/ticket-admin -n "${NAMESPACE}"
  log_info "Watching rollout status..."
  kubectl rollout status deployment/ticket-api -n "${NAMESPACE}" --timeout=180s || true
  kubectl rollout status deployment/ticket-admin -n "${NAMESPACE}" --timeout=120s || true
  log_success "Rollout triggered."
}

teardown() {
  log_warn "Tearing down all mono-ticket application resources..."
  kubectl delete -f "${SCRIPT_DIR}/06-ingress.yaml" --ignore-not-found
  kubectl delete -f "${SCRIPT_DIR}/05-ticket-admin.yaml" --ignore-not-found
  kubectl delete -f "${SCRIPT_DIR}/04-ticket-api.yaml" --ignore-not-found
  kubectl delete -f "${SCRIPT_DIR}/03-mongodb.yaml" --ignore-not-found
  kubectl delete -f "${SCRIPT_DIR}/08-mongodb-network-policy.yaml" --ignore-not-found
  kubectl delete -f "${SCRIPT_DIR}/01-configmap.yaml" --ignore-not-found
  log_success "Resources deleted. Namespace ${NAMESPACE} retained (run 'kubectl delete ns ${NAMESPACE}' to wipe entirely)."
}

show_status() {
  log_info "Current deployment status in ${NAMESPACE}:"
  kubectl get all,pvc,ingress -n "${NAMESPACE}"
}

case "${1:-deploy}" in
  deploy)
    check_prerequisites
    deploy_all
    ;;
  restart)
    restart_apps
    ;;
  status)
    show_status
    ;;
  teardown|delete)
    teardown
    ;;
  *)
    echo "Usage: $0 {deploy|restart|status|teardown}"
    exit 1
    ;;
esac
