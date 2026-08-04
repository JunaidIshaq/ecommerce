# Environment Variables & Deployment Configuration

Every value below was extracted directly from the `application.yml` / `application-dev.yml`
files in this repository — this is the actual, complete set the code reads.

---

## 1. The two rules that matter most

**Rule 1 — `POSTGRES_URI`, `SERVER_PORT` and `KAFKA_CONSUMER_GROUP` are per-service, not global.**

Each service has its own database (`order_db`, `product_db`, …), its own port, and its own
consumer group. If you export one global `POSTGRES_URI`, **all 12 services will connect to
the same database** and Hibernate `ddl-auto: update` will merge every schema into one.
Likewise a single global `KAFKA_CONSUMER_GROUP` makes unrelated services compete for the
same partitions and each event reaches only one of them.

Set these three **inside each service's own environment block**, never in a shared `.env`
that is applied globally.

**Rule 2 — Two variables have no default and the app will not start correctly without them.**

| Variable | Consequence if unset |
|---|---|
| `DB_PASSWORD` | Datasource authentication fails at startup |
| `APP_PASSWORD_ENCRYPTION_KEY` (auth-service) | Startup fails — the default was deliberately removed |

Everything else has a working default aimed at Docker Compose networking.

---

## 2. Global variables (safe to share across all services)

| Variable | Default | Notes |
|---|---|---|
| `DB_PASSWORD` | *(none — required)* | DB username is hard-coded to `postgres` |
| `JWT_SECRET` | *(none in prod; dev has a placeholder)* | Must be **identical** across all services and the gateway, or tokens issued by auth-service will be rejected everywhere else |
| `REDIS_HOST` | `redis` (prod) / `localhost` (dev) | |
| `REDIS_PORT` | `6379` | |
| `KAFKA_BOOTSTRAP` | `kafka:9092` | |
| `ELASTIC_URI` | `http://elasticsearch:9200` | |
| `EUREKA_URL` | `http://eureka-server:8761/eureka/eureka/` | See [Section 6](#6-issues-found-while-compiling-this-list) |
| `JPA_DDL_AUTO` | `update` | **Do not set to `validate`** — see [Section 5](#5-jpa_ddl_auto-warning) |

## 3. Per-service variables

### Ports and databases

| Service | `SERVER_PORT` | `POSTGRES_URI` database | `KAFKA_CONSUMER_GROUP` |
|---|---|---|---|
| api-gateway | 8080 | — | — |
| product-service | 8081 | `product_db` | `product-service-group` |
| category-service | 8082 | `category_db` | — |
| inventory-service | 8083 | `inventory_db` | `inventory-service-group` |
| order-service | 8084 | `order_db` | `order-service-group` |
| payment-service | 8085 | `payment_db` | — |
| user-service | 8086 | `user_db` | `user-service-group` |
| auth-service | 8087 | `auth_db` | — |
| cart-service | 8088 | `cart_db` | — |
| coupon-service | 8089 | `coupon_db` | — |
| review-service | 8090 | `review_db` | — |
| notification-service | 8091 | `notification_db` | `notification-service-group` |
| admin-service | 8092 | `admin_db` | `admin-service-group` |
| elastic-service | 8093 | — | `elastic-service-group` |
| eureka-server | 8761 | — | — |

URI format: `jdbc:postgresql://postgres:5432/<database>`

### Service discovery URLs

Only needed if you are **not** relying on Eureka/Compose DNS. Defaults already point at
Docker Compose service names.

| Variable | Default | Consumed by |
|---|---|---|
| `PRODUCT_SERVICE_URL` | `http://product-service:8081` | order, cart, review, admin, inventory, user, coupon, category, auth, payment |
| `CATEGORY_SERVICE_URL` | `http://category-service:8082` | most services |
| `INVENTORY_SERVICE_URL` | `http://inventory-service:8083` | order, cart, review, admin, user, coupon, auth, payment |
| `ORDER_SERVICE_URL` | `http://order-service:8084` | admin |
| `PAYMENT_SERVICE_URL` | `http://payment-service:8085` | order |
| `USER_SERVICE_URL` | `http://user-service:8086` | admin, auth |
| `CART_SERVICE_URL` | `http://cart-service:8088` | order, admin |
| `COUPON_SERVICE_URL` | `http://coupon-service:8089` | admin, order |

Note that some of these expect a **bare host:port** and others expect the **full API path**
(`http://product-service:8081/api/v1/product`) depending on the consumer. Copy the shape of
the existing default rather than assuming.

### Secrets

| Variable | Service | Notes |
|---|---|---|
| `APP_PASSWORD_ENCRYPTION_KEY` | auth-service | Base64, 32 bytes. **Required.** Rotating it invalidates all stored passwords |
| `STRIPE_SECRET_KEY` | payment-service | Placeholder default — must be replaced |
| `STRIPE_PUBLISHABLE_KEY` | payment-service | Placeholder default |
| `STRIPE_WEBHOOK_SECRET` | payment-service | Placeholder default; webhook signature verification fails without the real value |

---

## 3b. Your actual deployment (`docker compose` from `backend/`)

You deploy with:

```bash
sudo docker compose down
sudo docker compose build
sudo docker compose up -d
```

That reads `backend/docker-compose.yml`, which **already sets every value inline** — it does
not use `POSTGRES_URI` or `DB_PASSWORD` at all. Instead it sets Spring's own property names
directly:

```yaml
- SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auth_db
- SPRING_DATASOURCE_USERNAME=postgres
- SPRING_DATASOURCE_PASSWORD=postgres
```

Spring's relaxed binding maps `SPRING_DATASOURCE_PASSWORD` to `spring.datasource.password`,
which **overrides** the `${DB_PASSWORD}` placeholder in `application.yml`. So `DB_PASSWORD`
being unset is harmless in your setup — the compose value wins. The same applies to
`POSTGRES_URI`. All 14 services already have `JWT_SECRET` set inline, and `JPA_DDL_AUTO` is
absent, so it correctly falls back to `update`.

**One change was required.** Removing the committed `APP_PASSWORD_ENCRYPTION_KEY` default
from `auth-service/application.yml` would have made auth-service fail to start, because
compose never supplied that variable. I added it to the auth-service block with an
overridable default so your existing command keeps working:

```yaml
- APP_PASSWORD_ENCRYPTION_KEY=${APP_PASSWORD_ENCRYPTION_KEY:-MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=}
```

`docker compose config -q` passes. To use a real key, drop a `.env` next to
`docker-compose.yml`:

```dotenv
APP_PASSWORD_ENCRYPTION_KEY=<output of: openssl rand -base64 32>
```

Compose picks `.env` up automatically and the `:-` fallback is then ignored. **Important:**
changing this key makes existing encrypted passwords undecryptable, so set it before you
have real users, or plan a re-encryption migration.

### Security caveat for this compose file

`docker-compose.yml` is a development configuration: the Postgres password is literally
`postgres`, the JWT secret is the placeholder string, and every service port is published
to the host. It is fine for local work but should not be what runs in production — the
repo already has `docker-compose.prod.yml` and `.env.production.example` for that.

### Verifying the deploy

```bash
sudo docker compose ps                        # all should be "healthy"
sudo docker compose logs -f auth-service      # confirm auth-service starts
```

If auth-service is the only container failing, it is almost certainly the encryption key.

---

## 4. How to pass them

### Docker Compose (this repo has `docker-compose.prod.yml`)

Shared values go in a `.env` beside the compose file — Compose interpolates it
automatically. Per-service values go in each service's `environment:` block.

`.env` (never commit this):

```dotenv
DB_PASSWORD=<strong-password>
JWT_SECRET=<64+ random chars, same everywhere>
APP_PASSWORD_ENCRYPTION_KEY=<base64 32 bytes>
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

Generate the secrets:

```bash
openssl rand -base64 48                 # JWT_SECRET
openssl rand -base64 32                 # APP_PASSWORD_ENCRYPTION_KEY
```

Compose service block — note the per-service values are literal, not from `.env`:

```yaml
services:
  order-service:
    image: shopfast/order-service:latest
    environment:
      SERVER_PORT: 8084
      POSTGRES_URI: jdbc:postgresql://postgres:5432/order_db
      KAFKA_CONSUMER_GROUP: order-service-group
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      KAFKA_BOOTSTRAP: kafka:9092
      REDIS_HOST: redis
      JPA_DDL_AUTO: update

  payment-service:
    image: shopfast/payment-service:latest
    environment:
      SERVER_PORT: 8085
      POSTGRES_URI: jdbc:postgresql://postgres:5432/payment_db
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY}
      STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
```

Then:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

### Plain Docker

```bash
docker run -d --name order-service \
  -e SERVER_PORT=8084 \
  -e POSTGRES_URI=jdbc:postgresql://postgres:5432/order_db \
  -e KAFKA_CONSUMER_GROUP=order-service-group \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  shopfast/order-service:latest
```

### Kubernetes

Shared secrets once, per-service config in each Deployment:

```bash
kubectl create secret generic shopfast-secrets \
  --from-literal=DB_PASSWORD='...' \
  --from-literal=JWT_SECRET='...' \
  --from-literal=APP_PASSWORD_ENCRYPTION_KEY='...'
```

```yaml
containers:
  - name: order-service
    envFrom:
      - secretRef:
          name: shopfast-secrets
    env:
      - name: POSTGRES_URI
        value: jdbc:postgresql://postgres:5432/order_db
      - name: KAFKA_CONSUMER_GROUP
        value: order-service-group
```

### Local development

The `dev` profile has working defaults for everything (localhost hosts, placeholder
secrets), so a bare run works:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

### Precedence

Spring resolves in this order, highest first:
command-line `--args` → `SPRING_APPLICATION_JSON` → OS environment → `application-<profile>.yml`
→ `application.yml`. So an env var always beats the YAML default.

---

## 5. `JPA_DDL_AUTO` warning

Leave this at `update`. Flyway is not present in this project (see the implementation
report), which means **Hibernate is the only thing that creates the schema**. Setting
`validate` on a fresh database will fail startup with "table not found", and on an existing
database it will fail on any entity change.

Once Flyway is introduced and the schema is baselined, switch to `validate`.

---

## 6. Issues found while compiling this list

Three things surfaced while auditing the configuration. Two were fixed; one needs your decision.

**Fixed — committed encryption key.** `auth-service/application.yml` shipped a working
default for `APP_PASSWORD_ENCRYPTION_KEY`
(`MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=`, which decodes to
`12345678901234567890123456789012`). Any deployment that forgot to set the variable would
have silently encrypted every password with a key that is now in the git history. The
default has been removed so the service fails loudly instead.

**Fixed — wrong URL path.** `user-service/application-dev.yml` pointed
`PRODUCT_SERVICE_URL` at `/api/v1/project` instead of `/api/v1/product`.

**Needs your decision — the `EUREKA_URL` default looks wrong.** It is
`http://eureka-server:8761/eureka/eureka/` — `eureka` appears twice. The conventional
value is `http://eureka-server:8761/eureka/`. I have not changed it because if registration
is currently working in your environment, something else may be compensating for the extra
segment, and silently changing it could break discovery. Worth verifying against a running
Eureka dashboard before correcting.

**Also note:** all 14 `application-dev.yml` files still carry the literal JWT secret
`supersecretkeythatshouldbereplacedandstoredsecurely`. That is acceptable for local
development, but it means anyone who starts a service with `SPRING_PROFILES_ACTIVE=dev`
against a production database is running with a publicly-known signing key. Make sure the
production deployment never activates the `dev` profile.
