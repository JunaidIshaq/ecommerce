#!/usr/bin/env bash
# Build all ShopFast container images and tag them by git SHA (plan section 3).
#
# Usage:
#   ./scripts/build-images.sh [registry] [push]
#   registry defaults to ghcr.io/shopfast
#   pass "push" as the 2nd arg to docker push after building
#
# Builds common-lib once, then each service image, mirroring the per-service
# Dockerfile context (repo root) so common-lib is available at build time.
set -euo pipefail

REGISTRY="${1:-ghcr.io/shopfast}"
PUSH="${2:-no}"

GIT_SHA="$(git rev-parse --short HEAD)"
echo "Building ShopFast images @ git SHA ${GIT_SHA} into ${REGISTRY}"

# common-lib must be installed to the local Maven repo first so each service
# image build can resolve it (matches the Dockerfile build stage).
echo "==> installing common-lib"
(cd backend && mvn -q -f common-lib/pom.xml install -DskipTests)

SERVICES=(
  eureka-server api-gateway product-service category-service user-service
  auth-service inventory-service order-service payment-service cart-service
  coupon-service review-service notification-service elastic-service
  admin-service
)

for svc in "${SERVICES[@]}"; do
  echo "==> building ${svc}"
  docker build -t "${REGISTRY}/${svc}:${GIT_SHA}" -t "${REGISTRY}/${svc}:latest" \
    -f "backend/${svc}/Dockerfile" backend
  if [ "${PUSH}" = "push" ]; then
    docker push "${REGISTRY}/${svc}:${GIT_SHA}"
    docker push "${REGISTRY}/${svc}:latest"
  fi
done

# Frontend image
echo "==> building frontend"
docker build -t "${REGISTRY}/frontend:${GIT_SHA}" -t "${REGISTRY}/frontend:latest" frontend
if [ "${PUSH}" = "push" ]; then
  docker push "${REGISTRY}/frontend:${GIT_SHA}"
  docker push "${REGISTRY}/frontend:latest"
fi

echo "Done. Apply with:"
echo "  kubectl apply -k k8s/overlays/dev"
echo "  # then pin images to ${GIT_SHA}:"
echo "  kustomize edit set image ghcr.io/shopfast/=ghcr.io/shopfast/:${GIT_SHA} -- k8s/overlays/dev"
