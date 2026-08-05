#!/usr/bin/env bash
#
# auth-service manages users through the Keycloak Admin REST API using the
# `shopfast-services` client's service account. Without these role mappings the
# API returns 403 and POST /api/v1/auth/register fails with a 500.
#
#   manage-users / view-users / query-users  -> create and look up users
#   view-realm / manage-realm                -> map the default realm role onto
#                                               a newly created user; Keycloak's
#                                               canMapRole() check requires realm
#                                               management rights unless
#                                               fine-grained admin permissions
#                                               are enabled.
#
# Usage: KEYCLOAK_ADMIN_PASSWORD=... ./grant-service-account-roles.sh [container]
set -euo pipefail

CONTAINER="${1:-keycloak}"
REALM="${KEYCLOAK_REALM:-shopfast}"
CLIENT_ID="${KEYCLOAK_ADMIN_CLIENT_ID:-shopfast-services}"
SERVER="${KEYCLOAK_INTERNAL_URL:-http://localhost:8180/auth}"
ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
: "${KEYCLOAK_ADMIN_PASSWORD:?KEYCLOAK_ADMIN_PASSWORD must be set}"

kcadm() { docker exec "$CONTAINER" /opt/keycloak/bin/kcadm.sh "$@"; }

kcadm config credentials \
  --server "$SERVER" --realm master \
  --user "$ADMIN_USER" --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null

kcadm add-roles -r "$REALM" \
  --uusername "service-account-${CLIENT_ID}" \
  --cclientid realm-management \
  --rolename manage-users \
  --rolename view-users \
  --rolename query-users \
  --rolename view-realm \
  --rolename manage-realm

echo "Granted realm-management roles to service-account-${CLIENT_ID} in realm ${REALM}"
