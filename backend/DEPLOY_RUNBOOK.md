# Deployment Runbook — 72.62.250.5

Run these yourself over SSH. Every command is copy-pasteable.

---

## What changed and why your old command will now fail

`docker-compose.yml` previously hard-coded three secrets and published all 21 ports to
`0.0.0.0`. On a public IP that meant:

- The JWT signing key `supersecretkeythatshouldbereplacedandstoredsecurely` is in your git
  history. Anyone with the repo can mint a token with any `userId` and any role and call
  any endpoint as any user, including admin.
- Postgres was reachable on `72.62.250.5:5433` with user `postgres` / password `postgres`.
- All 14 services were reachable directly, bypassing the API gateway and therefore
  bypassing whatever the gateway enforces.

Both are now fixed:

| Change | Effect |
|---|---|
| Secrets read from `.env` via `${VAR:?...}` | Compose **refuses to start** if a secret is missing. No silent bad default is possible. |
| All ports bound to `127.0.0.1` except `8080` | Only the API gateway is internet-facing. Everything else stays reachable locally and over an SSH tunnel. |

`docker compose up` will now fail fast with `required variable DB_PASSWORD is missing` until
you create `.env`. That is deliberate — the step below fixes it.

---

## One-time setup on the server

```bash
ssh root@72.62.250.5
cd /path/to/ecommerce/backend      # wherever docker-compose.yml lives

git pull                           # get the hardened compose + code fixes

./generate-secrets.sh              # creates .env with fresh random secrets, mode 600
cat .env                           # copy these into your password manager NOW
```

Back the file up before continuing. If you lose `APP_PASSWORD_ENCRYPTION_KEY`, every stored
password becomes undecryptable and users must reset.

Verify compose resolves cleanly before touching the running system:

```bash
docker compose config -q && echo "config OK"
```

---

## Deploy

```bash
sudo docker compose down
sudo docker compose build
sudo docker compose up -d
```

Same three commands you already use.

---

## The one thing that will probably bite you

Your Postgres volume already exists and still has the **old** password (`postgres`). The
`POSTGRES_PASSWORD` variable only initialises a *fresh* volume — it does not change an
existing one. So after generating a new `DB_PASSWORD`, the services will fail to
authenticate.

Pick one:

**Option A — change the password inside the running database (keeps all data):**

```bash
sudo docker compose up -d postgres
source .env
sudo docker compose exec -T postgres \
  psql -U postgres -c "ALTER USER postgres WITH PASSWORD '$DB_PASSWORD';"
sudo docker compose up -d
```

**Option B — keep the existing password.** Edit `.env` and set `DB_PASSWORD=postgres`. Less
secure, but Postgres is now bound to loopback so it is no longer reachable from the
internet. Acceptable as an interim step; do Option A when you can take the downtime.

---

## Verify

```bash
sudo docker compose ps                       # every service should be "healthy"
sudo docker compose logs --tail=50 auth-service
curl -f http://localhost:8080/actuator/health
```

Confirm the internal ports are no longer public — run this **from your laptop**, not the
server:

```bash
curl --max-time 5 http://72.62.250.5:8081/actuator/health   # expect: connection refused
curl --max-time 5 http://72.62.250.5:5433                   # expect: connection refused
curl --max-time 5 http://72.62.250.5:8080/actuator/health   # expect: 200
```

The first two failing is the success condition.

To reach an internal service for debugging, tunnel it:

```bash
ssh -L 8081:127.0.0.1:8081 root@72.62.250.5
# then browse http://localhost:8081 on your laptop
```

---

## Rollback

The original compose file is saved as `docker-compose.yml.bak`:

```bash
cp docker-compose.yml.bak docker-compose.yml
sudo docker compose up -d
```

---

# Keycloak cutover

Do this as a separate deployment from the hardening above. It changes how every
request in the system is authenticated, so it deserves its own window and its own
rollback decision.

The design point that makes this survivable: **the gateway accepts both old HS256
tokens and new Keycloak RS256 tokens simultaneously.** Logged-in users are not
kicked out at cutover, and the frontend can migrate on its own schedule.

## Before you start

```bash
# 1. Back up. keycloak_db does not exist yet, but user_db is the source for
#    migration and you want a restore point if the migration script misbehaves.
sudo docker compose exec -T postgres pg_dump -U postgres -d user_db -Fc > user_db-pre-keycloak.dump

# 2. Add the new secrets to .env.
{
  echo "SHOPFAST_SERVICES_CLIENT_SECRET=$(openssl rand -hex 32)"
  echo "SHOPFAST_ADMIN_CLIENT_SECRET=$(openssl rand -hex 32)"
  echo "KEYCLOAK_ADMIN=admin"
  echo "KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -base64 24)"
  echo "KEYCLOAK_ISSUER_URI=http://keycloak:8180/realms/shopfast"
} >> .env
chmod 600 .env

# 3. Keep JWT_SECRET. Removing it now invalidates every active session.
grep -q '^JWT_SECRET=' .env || echo "JWT_SECRET is missing - old tokens will be rejected"

sudo docker compose config -q && echo "config OK"
```

## Start Keycloak

```bash
sudo docker compose up -d postgres
sudo docker compose up -d keycloak

# First boot creates the schema and imports the realm. Expect 60-90 seconds.
sudo docker compose logs -f keycloak | grep -m1 'Running the server'
```

Confirm the realm imported and is serving keys. If this returns anything other than
a JSON document with an `issuer` field, stop — every service will fail to start.

```bash
curl -sf http://localhost:8180/realms/shopfast/.well-known/openid-configuration | head -c 200
curl -sf http://localhost:8180/realms/shopfast/protocol/openid-connect/certs | head -c 200
```

## Deploy the services

```bash
sudo docker compose build
sudo docker compose up -d
sudo docker compose ps          # all healthy
```

A service that cannot reach the issuer URL **fails to start**, rather than starting
and silently accepting unverified tokens. If something is stuck restarting, check
its logs for `Unable to resolve the Configuration with the provided Issuer`.

## Verify both token types work

```bash
source .env

# New path: get a real Keycloak token via the service client.
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/shopfast/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=shopfast-services \
  -d client_secret="$SHOPFAST_SERVICES_CLIENT_SECRET" | jq -r .access_token)

[ "$TOKEN" != "null" ] && echo "token OK"
curl -s -o /dev/null -w '%{http_code}\n' \
  -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products   # expect 200

# No token must still be rejected.
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/v1/orders  # expect 401

# Old path: an existing frontend session should still work. Test from a browser
# that was logged in before the deploy - it should not be redirected to login.
```

## Migrate users

Run against a copy first if you can. BCrypt hashes cannot be imported into
Keycloak, so migrated users get an `UPDATE_PASSWORD` required action and must set a
new password at first login. There is no way around this short of a custom SPI.

```bash
cd keycloak

python3 migrate-users.py --dry-run          # reports counts, writes nothing
python3 migrate-users.py                    # writes migration-journal-<ts>.json
```

Keep the journal file. It is the only rollback path:

```bash
python3 migrate-users.py --rollback migration-journal-<ts>.json
```

Rollback deletes **only** the users that run created — accounts registered directly
in Keycloak afterwards are left alone.

## Set up backups before you walk away

`keycloak_db` now holds every credential in the system. Nothing else does.

```bash
cd backend
./keycloak/backup-keycloak-db.sh
(crontab -l 2>/dev/null; echo "17 3 * * * cd $(pwd) && ./keycloak/backup-keycloak-db.sh >> /var/log/keycloak-backup.log 2>&1") | crontab -
```

## Rolling back the cutover

Within the first hours, before users have reset passwords:

```bash
git checkout HEAD~1 -- docker-compose.yml
sudo docker compose up -d --build
```

Old tokens still validate because `JWT_SECRET` is unchanged, so sessions survive the
rollback too. Users who already reset their password through Keycloak will not be
able to log in against the old auth-service — this is why the migration step is the
point of no return, not the deployment step.

## Retiring the legacy path

Do not do this at cutover. Watch the `LegacyTokensStillInUse` alert; when it has
been silent for longer than the longest refresh-token lifetime (30 days):

```bash
sed -i '/^JWT_SECRET=/d' .env
sudo docker compose up -d api-gateway
```

The gateway then rejects every HS256 token. Verify a legacy token now returns 401
before considering it done, then proceed to the code cleanup in
`common-lib/plans/keycloak-migration-plan.md`.

---

## Still outstanding after this

Hardening the compose file closes the exposure, but two things remain:

1. **The old JWT secret is permanently compromised** — it is in the git history. Rotating it
   (which `generate-secrets.sh` does) invalidates all existing sessions, so users will need
   to log in again. That is the correct trade.
2. **TLS is already handled** — nginx terminates HTTPS for `shopfast.live` and sends
   `Strict-Transport-Security`, so traffic is encrypted. Port 8080 is still bound to
   `0.0.0.0` though, which means the API can also be reached over plain HTTP on the raw IP,
   bypassing nginx and HSTS. Binding it to `127.0.0.1` (nginx proxies from localhost) closes
   that and costs nothing.

   Related: the client-side password encryption is not a security control. The key sits in
   the JavaScript bundle, so anyone can read it — the real protection is TLS. Treat
   `APP_PASSWORD_ENCRYPTION_KEY` as a compatibility constant shared with the frontend, not a
   secret, and never rotate it on its own.
3. **`ddl-auto: update` against a production database.** Hibernate is altering your live
   schema on every boot. Introducing Flyway (see `common-lib/IMPLEMENTATION_REPORT.md`
   section 7) should be scheduled before the data matters.
