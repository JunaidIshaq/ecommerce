#!/usr/bin/env bash
# Generate strong values for the ShopFast Kubernetes Secrets (mirrors
# backend/generate-secrets.sh). Output is appended to a local .env that you
# then seal (kubeseal) or load into your external secret store. NEVER commit it.
set -euo pipefail

OUT="${1:-k8s/.env.generated}"

{
  echo "# Generated $(date -u) — do NOT commit."
  echo "DB_PASSWORD=$(openssl rand -base64 24)"
  echo "JWT_SECRET=$(openssl rand -base64 48)"
  echo "APP_PASSWORD_ENCRYPTION_KEY=$(openssl rand -base64 32)"
  echo "KEYCLOAK_ADMIN=admin"
  echo "KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -base64 24)"
  echo "SHOPFAST_SERVICES_CLIENT_SECRET=$(openssl rand -base64 32)"
  echo "SHOPFAST_ADMIN_CLIENT_SECRET=$(openssl rand -base64 32)"
} >> "${OUT}"

echo "Wrote secrets to ${OUT}. Edit k8s/base/secrets.yaml or seal them before applying."
