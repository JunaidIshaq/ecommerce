#!/usr/bin/env bash
# Generates a .env with fresh random secrets.
#
#   ./generate-secrets.sh          # writes .env, refuses to overwrite
#   ./generate-secrets.sh --force  # overwrites (rotates DB_PASSWORD and JWT_SECRET)
#
# Rotating JWT_SECRET invalidates all live sessions - users must log in again.
#
# APP_PASSWORD_ENCRYPTION_KEY is deliberately NOT randomised. The Angular client
# encrypts the password with this same key before posting it (see
# frontend/src/environments/environment.ts: passwordEncryptionKey), so it is a *shared*
# symmetric key, not a server-side secret. Generating a new one here silently breaks
# every login: auth-service fails to decrypt, and because AuthService.login used to
# swallow the cause, the API replied "User not exists with this email" - which sends
# you looking in the database instead of at the key. Learned that the hard way.
#
# To change it you must deploy the frontend and backend together, and re-encrypt or
# reset any password already stored under the old key.
set -euo pipefail

cd "$(dirname "$0")"
TARGET=".env"

# Keep whatever key is already in use; only fall back to the current shared default.
DEFAULT_ENCRYPTION_KEY="MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI="
if [[ -f "$TARGET" ]] && grep -q '^APP_PASSWORD_ENCRYPTION_KEY=' "$TARGET"; then
    EXISTING_KEY="$(grep '^APP_PASSWORD_ENCRYPTION_KEY=' "$TARGET" | head -1 | cut -d= -f2-)"
else
    EXISTING_KEY="$DEFAULT_ENCRYPTION_KEY"
fi

if [[ -f "$TARGET" && "${1:-}" != "--force" ]]; then
    echo "$TARGET already exists. Use --force to overwrite (this rotates DB_PASSWORD and JWT_SECRET)." >&2
    exit 1
fi

cat > "$TARGET" <<EOF
# Generated $(date -u +%Y-%m-%dT%H:%M:%SZ). Do not commit.
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
JWT_SECRET=$(openssl rand -base64 48)
# Shared with the Angular client - must match frontend environment.ts. Do not rotate alone.
APP_PASSWORD_ENCRYPTION_KEY=$EXISTING_KEY
EOF

chmod 600 "$TARGET"
echo "Wrote $TARGET (mode 600)."
echo
echo "DB_PASSWORD and JWT_SECRET are new. APP_PASSWORD_ENCRYPTION_KEY was preserved,"
echo "because it is shared with the frontend and rotating it alone breaks all logins."
echo
echo "If Postgres already has a volume, its password is unchanged - update it with:"
echo "  docker compose exec -T postgres psql -U postgres -c \"ALTER USER postgres WITH PASSWORD '<new>';\""
