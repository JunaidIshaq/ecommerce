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

## Still outstanding after this

Hardening the compose file closes the exposure, but two things remain:

1. **The old JWT secret is permanently compromised** — it is in the git history. Rotating it
   (which `generate-secrets.sh` does) invalidates all existing sessions, so users will need
   to log in again. That is the correct trade.
2. **No TLS.** Port 8080 serves plain HTTP, so credentials and tokens cross the internet in
   clear text. Put Nginx or Caddy in front with a Let's Encrypt certificate and bind 8080 to
   loopback as well. This is the next thing I would do.
3. **`ddl-auto: update` against a production database.** Hibernate is altering your live
   schema on every boot. Introducing Flyway (see `common-lib/IMPLEMENTATION_REPORT.md`
   section 7) should be scheduled before the data matters.
