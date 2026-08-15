#!/usr/bin/env bash
# Deploy ShopFast to Kubernetes via Kustomize (plan section 11 / 13).
#
# Usage:
#   ./scripts/deploy.sh [overlay] [context]
#   overlay  = dev | prod   (default: dev)
#   context  = kubectl context to use (default: current-context)
#
# Pre-reqs: a cluster, kubectl, the ingress-nginx + cert-manager addons, and
# real secrets (see k8s/base/sealed-secret-example.yaml). For dev, generate a
# local .env and seal/replace the placeholder Secret first.
set -euo pipefail

OVERLAY="${1:-dev}"
CTX="${2:-}"

KUSTOMIZE_DIR="k8s/overlays/${OVERLAY}"

if [ ! -d "${KUSTOMIZE_DIR}" ]; then
  echo "Unknown overlay: ${OVERLAY}" >&2
  exit 1
fi

if [ -n "${CTX}" ]; then
  kubectl="kubectl --context=${CTX}"
else
  kubectl="kubectl"
fi

echo "==> deploying ShopFast (${OVERLAY})"
${kubectl} apply -k "${KUSTOMIZE_DIR}"

echo "==> waiting for namespace shopfast to exist"
${kubectl} wait --for=jsonpath='{.status.phase}'=Active namespace/shopfast --timeout=60s || true

echo "==> rollout status"
for dep in $(${kubectl} -n shopfast get deployments -o name); do
  ${kubectl} -n shopfast rollout status "${dep}" --timeout=300s || true
done

echo "==> pods"
${kubectl} -n shopfast get pods
