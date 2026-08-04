#!/usr/bin/env bash
# Generates a .env with fresh random secrets.
#
#   ./generate-secrets.sh          # writes .env, refuses to overwrite
#   ./generate-secrets.sh --force  # overwrites (rotates every secret)
#
# Rotating APP_PASSWORD_ENCRYPTION_KEY makes existing stored passwords
# undecryptable, and rotating JWT_SECRET invalidates all live sessions.
set -euo pipefail

cd "$(dirname "$0")"
TARGET=".env"

if [[ -f "$TARGET" && "${1:-}" != "--force" ]]; then
    echo "$TARGET already exists. Use --force to overwrite (this rotates all secrets)." >&2
    exit 1
fi

cat > "$TARGET" <<EOF
# Generated $(date -u +%Y-%m-%dT%H:%M:%SZ). Do not commit.
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
JWT_SECRET=$(openssl rand -base64 48)
APP_PASSWORD_ENCRYPTION_KEY=$(openssl rand -base64 32)
EOF

chmod 600 "$TARGET"
echo "Wrote $TARGET (mode 600)."
echo "Back it up somewhere safe - losing APP_PASSWORD_ENCRYPTION_KEY means losing all stored passwords."
