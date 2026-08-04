# Deployment Implementation Report

**Target:** 72.62.250.5 (`srv1286917`) — `shopfast.live`
**Date:** 2026-08-04
**Baseline commit:** `9a61685`
**Final commit:** `8f734a2`
**Outcome:** 21/21 containers healthy, 0 unhealthy, login working end-to-end

This report covers the production deployment session. The earlier application-code
remediation (correctness, reliability, performance across 13 modules) is documented separately
in [`common-lib/IMPLEMENTATION_REPORT.md`](common-lib/IMPLEMENTATION_REPORT.md).

---

## 1. Executive summary

The deployment surfaced three problems that mattered more than the planned work:

| # | Problem | Severity | Origin |
|---|---|---|---|
| 1 | Hard-coded JWT secret in git; all 21 ports published to `0.0.0.0` | Critical | Pre-existing |
| 2 | 9 of 21 services dead, including `api-gateway` | Critical | **Introduced by my earlier remediation** |
| 3 | All logins broken by a rotated encryption key, masked by a misleading error | Critical | **Introduced during this session** |

All three are resolved and verified. Problems 2 and 3 were my own defects; both are analysed
honestly below, because the reasons they escaped detection are more instructive than the fixes.

---

## 2. Pre-flight survey

I did not run the deploy commands immediately. Surveying first changed the plan substantially.

```
git HEAD (server)   9a61685  — identical to local, so all prior Java fixes were already deployed
.env                absent
containers running  12 of 21
volumes             backend_postgres_data, backend_redis_data (existing data present)
memory              15 GB total, 5 GB used, 0 swap
disk                193 GB, 40% used
```

Three findings redirected the work:

1. **The server was already at the same commit as local.** A `git pull` alone would have
   changed nothing — the compose hardening was still uncommitted on my machine.
2. **Only 12 of 21 containers were running.** The stack was already broken before I touched
   it, and `api-gateway` was among the missing. This was not visible from `docker compose ps`
   without `-a`.
3. **An existing Postgres volume.** `POSTGRES_PASSWORD` only initialises a *fresh* volume, so
   introducing a new `DB_PASSWORD` would have locked every service out of a database holding
   real data.

---

## 3. Security hardening

### 3.1 What was exposed

`docker-compose.yml` hard-coded the JWT signing secret
(`supersecretkeythatshouldbereplacedandstoredsecurely`) and committed it. Anyone with repo
access could forge a token for any `userId` with any role. Separately, all 21 services
published their ports on `0.0.0.0`, so Postgres was reachable at `72.62.250.5:5433` with
`postgres`/`postgres`, and every microservice could be called directly, bypassing the gateway.

### 3.2 Changes

| Change | Rationale |
|---|---|
| All secrets via `${VAR:?message}` | Compose **refuses to start** on a missing secret. A silent weak default is impossible by construction. |
| 28 bindings moved to `127.0.0.1` | Only `api-gateway:8080` remains public. Internal services stay reachable locally and via SSH tunnel. |
| `generate-secrets.sh` | Reproducible secret generation; no hand-rolled passwords. |
| `.env` → `.gitignore`, mode 600 | Prevents the original mistake recurring. |

### 3.3 Postgres password rotation without data loss

Rotated in place on the running instance rather than recreating the volume:

```bash
docker compose exec -T postgres psql -U postgres -c "ALTER USER postgres WITH PASSWORD '$DB_PASSWORD';"
```

Verified all 12 databases present before and after (`product_db`, `category_db`,
`inventory_db`, `order_db`, `payment_db`, `user_db`, `auth_db`, `cart_db`, `coupon_db`,
`review_db`, `notification_db`, `admin_db`), and confirmed the new credential authenticates
before proceeding. Prior compose file backed up to `/root/compose-backup-*.yml`.

---

## 4. Defect 1 — nine services dead (my earlier remediation)

### Symptom
`product-service` and `payment-service` `Exited (1)`. Seven more sat in `Created` and never
started, because they `depends_on` those two with `condition: service_healthy`. `api-gateway`
was one of them.

### Root cause
```
Parameter 0 of method kafkaErrorHandler required a bean of type
'org.springframework.kafka.core.KafkaOperations' that could not be found.
```

The `KafkaErrorHandlingConfig` I added during the reliability phase declared:

```java
public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> template)
```

`KafkaTemplate<String, Object>` — what these services actually declare — implements
`KafkaOperations<String, Object>`. Spring includes generic parameters in the type match, so it
does **not** qualify. Context initialisation failed and the JVM exited.

### Why it wasn't caught
The two services that *did* start (`notification-service`, `elastic-service`) declare no
template bean of their own and were falling back to Boot's auto-configured
`KafkaTemplate<Object, Object>`, which happens to match exactly. A compile check cannot catch
this — it is a runtime bean-resolution failure — and the services that would have exposed it
were the ones I had not started.

### Fix

```java
public DefaultErrorHandler kafkaErrorHandler(ObjectProvider<KafkaOperations<?, ?>> templates) {
    KafkaOperations<?, ?> template =
            templates.getIfUnique(() -> templates.orderedStream().findFirst().orElse(null));

    return (template != null)
            ? new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff)
            : new DefaultErrorHandler(backOff);
}
```

The wildcard matches any declared template; `getIfUnique` disambiguates when several exist;
and a service with no producer still receives a working retry handler rather than refusing to
boot. Applied to all six services that carry this config, so the failure mode cannot return
for a service that later adds a differently-typed template.

Verified by compiling all six against JDK 17.

---

## 5. Defect 2 — all logins broken (this session)

### Symptom
```
POST https://shopfast.live/api/v1/auth/login
500 {"message":"User not exists with this email : alice@example.com"}
```

### Investigation
Evidence contradicted the error message:
- `user_db` contained `alice@example.com` and nine other seeded users
- `auth-service` → `user-service` internal lookup returned **200**
- `auth_db` contained no `users` table at all — auth-service holds no user data; it fetches
  over Feign

So the user existed and was retrievable. The message was wrong.

### Root cause
`generate-secrets.sh` randomised `APP_PASSWORD_ENCRYPTION_KEY`. That key is **not** a
server-side secret — the Angular client encrypts the password with the same key before posting
it:

```
frontend/src/environments/environment.ts:11
  passwordEncryptionKey: 'MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI='
```

It is a *shared symmetric constant*. Once the backend held a different value, `decrypt()` threw
for every login.

### The compounding defect
`AuthService.login` wrapped its entire body in a catch-all:

```java
} catch (Exception e) {
    throw new RuntimeException("User not exists with this email : " + request.getEmail());
}
```

The cause was discarded and nothing was logged. A key mismatch, a `user-service` outage, and a
genuinely unknown email produced byte-identical responses — and the message actively pointed
away from the real cause, toward the database. Grepping the logs for the exception returned
nothing at all.

### Fixes

**Immediate:** restored the key in `.env`, restarted `auth-service`, confirmed login returns 200.

**`generate-secrets.sh`** — preserves any existing key, defaults to the shared constant, and
documents why:

```bash
if [[ -f "$TARGET" ]] && grep -q '^APP_PASSWORD_ENCRYPTION_KEY=' "$TARGET"; then
    EXISTING_KEY="$(grep '^APP_PASSWORD_ENCRYPTION_KEY=' "$TARGET" | head -1 | cut -d= -f2-)"
else
    EXISTING_KEY="$DEFAULT_ENCRYPTION_KEY"
fi
```

**`AuthService`** — logs the real cause server-side, returns a generic 401:

```java
} catch (Exception e) {
    log.error("Login failed for email={} - {}", request.getEmail(), e.toString(), e);
    throw new InvalidCredentialsException("Invalid email or password");
}
```

This also removes a user-enumeration signal: unknown emails and wrong passwords are now
indistinguishable to a caller.

---

## 6. Verification

Every claim below was executed, not assumed.

### Container health
```
21/21 healthy — unhealthy/exited count: 0
```

### Authentication — from the public internet
| Case | Result |
|---|---|
| Valid credentials | **200** + access & refresh tokens |
| Unknown email | **401** "Invalid password" |
| Malformed password | **401** "Invalid password" |

Identical responses for the two failure cases — no enumeration.

### Port exposure — tested **from outside the server**
```
8081 closed   8082 closed   8083 closed   5433 closed   6379 closed
9092 closed   8761 closed   9200 closed   8080 OPEN (gateway, intended)
```

### Routing
`product-service` direct → **200**. Through the gateway → **401**, i.e. the gateway enforcing
auth. Confirmed via `git log -- api-gateway/` that this is pre-existing behaviour and not a
regression from my changes.

---

## 7. Corrections to my own earlier reporting

Two claims I made were wrong and are retracted:

1. **"No TLS."** nginx terminates HTTPS for `shopfast.live` and sends
   `Strict-Transport-Security`. I had only probed port 8080 directly and generalised from it.
2. **"All modules compile clean."** My first verification loop used `../mvnw`, which did not
   exist; the `&&` chain made every service print `OK`. Re-run with the correct JDK 17
   `JAVA_HOME`, five of six services initially failed with `release version 17 not supported`.
   The real compile check passed only after fixing the toolchain.

Both matter: the first would have sent you to solve a problem you don't have, the second was a
verification step that verified nothing.

---

## 8. Commits

| Commit | Description |
|---|---|
| `f97d249` | Harden compose: secrets from `.env`, 28 ports to loopback, add runbook + secret script |
| `cbf89f4` | Fix Kafka DLT error handler failing startup in 6 services |
| `d816f12` | Stop rotating the frontend-shared key; log real login errors |
| `8f734a2` | Correct runbook: TLS already terminated by nginx |

12 files changed, 412 insertions, 94 deletions.

---

## 9. Outstanding risks

Ordered by what I would address first.

### 9.1 Port 8080 bypasses nginx — *low effort, real exposure*
The gateway is bound to `0.0.0.0`, so the API is reachable over **plain HTTP on the raw IP**,
sidestepping nginx and HSTS entirely. Credentials sent that way are in clear text. Bind it to
`127.0.0.1`; nginx already proxies from localhost, so nothing else changes.

### 9.2 No swap, no JVM heap caps — *availability*
10 GB of 15 GB used with swap disabled and no `mem_limit` or `-XX:MaxRAMPercentage` on any
service. Each JVM sizes its heap against the full 15 GB. Under load the kernel OOM-killer
selects a victim by its own heuristics — very likely not the service that misbehaved, so the
failure appears somewhere unrelated.

### 9.3 `ddl-auto: update` on production data — *data integrity*
Hibernate alters the live schema on every boot. It never drops columns, so drift accumulates
silently and there is no migration history or rollback path. Flyway groundwork is described in
`common-lib/IMPLEMENTATION_REPORT.md` §7.

### 9.4 Client-side password encryption is not a security control
The key ships in the JavaScript bundle. Anyone can read it and decrypt the payload; TLS is what
actually protects the credential. Treat `APP_PASSWORD_ENCRYPTION_KEY` as a compatibility
constant shared with the frontend, never rotated alone. Changing it requires deploying both
sides together and re-encrypting or resetting stored passwords.

### 9.5 Unmapped URLs return 500 instead of 404
`GlobalExceptionHandler` treats `NoResourceFoundException` as an internal error. I hit this
while probing. It inflates error-rate metrics and buries genuine 500s.

### 9.6 The old JWT secret is permanently compromised
It is in git history and cannot be un-leaked. It has been rotated, which invalidated all
existing sessions — users must log in again. That is the correct trade, but it is a visible
impact worth knowing about.

---

## 10. Rollback

```bash
cd ecommerce/backend
cp docker-compose.yml.bak docker-compose.yml     # or /root/compose-backup-*.yml
sudo docker compose up -d
```

`.env` is mode 600 and gitignored. **Back up `APP_PASSWORD_ENCRYPTION_KEY`** — losing it makes
every stored password undecryptable and forces a reset for all users.
