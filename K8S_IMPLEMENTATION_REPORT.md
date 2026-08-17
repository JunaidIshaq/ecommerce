# Shopfast E-Commerce Platform — Kubernetes Implementation Report

**Date:** 2026-08-17  
**Cluster:** Single-node k3s on `72.62.250.5` (srv1286917)  
**GitOps:** ArgoCD — `https://github.com/JunaidIshaq/ecommerce.git` → `k8s/overlays/dev`  
**Ingress:** nginx + self-signed cert-manager TLS  
**Platform:** Spring Cloud microservices + Angular frontend + Keycloak OIDC  

---

## Table of Contents

1. [Cluster Architecture](#1-cluster-architecture)
2. [Namespace & Workloads](#2-namespace--workloads)
3. [Services & Networking](#3-services--networking)
4. [Ingress & TLS](#4-ingress--tls)
5. [Configuration & Secrets](#5-configuration--secrets)
6. [Security](#6-security)
7. [Storage](#7-storage)
8. [Autoscaling](#8-autoscaling)
9. [GitOps (ArgoCD)](#9-gitops-argocd)
10. [Issues Fixed](#10-issues-fixed)
11. [Remaining Gaps](#11-remaining-gaps)
12. [Current State Summary](#12-current-state-summary)

---

## 1. Cluster Architecture

### 1.1 Infrastructure

| Layer | Component | Details |
|---|---|---|
| **Kubernetes distro** | k3s (v1.x) | Single-node, containerd runtime |
| **Node** | srv1286917 (`72.62.250.5`) | 4 CPU, single node |
| **Container registry** | Docker Registry | `localhost:5000` (insecure, node-local) |
| **Ingress controller** | ingress-nginx | Exposed via host network on 80/443 |
| **TLS** | cert-manager + self-signed | Issuer: `shopfast-selfsigned`, Cert: `shopfast-tls` |
| **GitOps** | ArgoCD | 3 applications managing the cluster |

### 1.2 ArgoCD Applications

| Application | Source Path | Sync Status | Health |
|---|---|---|---|
| `shopfast-services` | `k8s/overlays/dev` | OutOfSync | Degraded (shared-resource warnings) |
| `shopfast-infra` | `k8s/overlays/dev` | OutOfSync | Degraded (shared-resource warnings) |
| `shopfast-ingress` | `k8s/ingress` | Synced | Healthy |

> **Note:** "Degraded" is caused by shared resources (Ingress, Certificate, NetworkPolicies, Namespace) declared across multiple ArgoCD apps. Pods themselves are healthy.

### 1.3 Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | Angular 19 + `angular-auth-oidc-client` v19 |
| **API Gateway** | Spring Cloud Gateway |
| **Service Discovery** | Netflix Eureka Server |
| **Business Services** | Spring Boot 3.x (14 services) |
| **Identity** | Keycloak 26 (OIDC / OAuth2) |
| **Message Queue** | Kafka 7.6.1 + ZooKeeper |
| **Search** | Elasticsearch 8.19.5 |
| **Cache** | Redis 7 |
| **Databases** | PostgreSQL 15 |
| **Monitoring** | (planned) Prometheus + Grafana via HPA |

---

## 2. Namespace & Workloads

All application workloads run in the `shopfast` namespace.

### 2.1 Deployments (Complete List)

| Deployment | Image | Replicas | Status | Restarts |
|---|---|---|---|---|
| **frontend** | `localhost:5000/shopfast/frontend:dev` | 1 | Running | 0 |
| **api-gateway** | `localhost:5000/shopfast/api-gateway:2d9e2e2` | 1 | Running | 0 |
| **keycloak** | `quay.io/keycloak/keycloak:26.0` | 1 | Running | 0 |
| **eureka-server** | `localhost:5000/shopfast/eureka-server:dev` | 1 | Running | 0 |
| **product-service** | `localhost:5000/shopfast/product-service:dev` | 1 | Running | 3 |
| **cart-service** | `localhost:5000/shopfast/cart-service:dev` | 1 | Running | 0 |
| **order-service** | `localhost:5000/shopfast/order-service:dev` | 1 | Running | 2 |
| **user-service** | `localhost:5000/shopfast/user-service:dev` | 1 | Running | 0 |
| **auth-service** | `localhost:5000/shopfast/auth-service:dev` | 1 | **CrashLoopBackOff** | 14 |
| **admin-service** | `localhost:5000/shopfast/admin-service:dev` | 1 | Running | 0 |
| **category-service** | `localhost:5000/shopfast/category-service:dev` | 1 | Running | 0 |
| **inventory-service** | `localhost:5000/shopfast/inventory-service:dev` | 1 | Running | 0 |
| **payment-service** | `localhost:5000/shopfast/payment-service:dev` | 1 | Running | 0 |
| **coupon-service** | `localhost:5000/shopfast/coupon-service:dev` | 1 | Running | 0 |
| **review-service** | `localhost:5000/shopfast/review-service:dev` | 1 | Running | 0 |
| **notification-service** | `localhost:5000/shopfast/notification-service:dev` | 1 | Running | 0 |
| **elastic-service** | `localhost:5000/shopfast/elastic-service:dev` | 1 | Running | 0 |
| **postgres** | `postgres:15` | 1 | Running | 0 |
| **redis** | `redis:7` | 1 | Running | 0 |
| **kafka** | `confluentinc/cp-kafka:7.6.1` | 1 | Running | 68 |
| **zookeeper** | `confluentinc/cp-zookeeper:7.6.1` | 1 | Running | 0 |
| **elasticsearch** | `docker.elastic.co/elasticsearch:8.19.5` | 1 | Running | 0 |
| **pgadmin** | `dpage/pgadmin4:latest` | 1 | Running | 0 |
| **kafka-ui** | `provectuslabs/kafka-ui:latest` | 1 | Running | 0 |

> **Note:** Kafka's 68 restarts indicate instability — likely resource pressure or ZooKeeper connectivity issues.

### 2.2 Deployment Strategy

- **Replicas:** All services run with 1 replica (single-node cluster constraint).
- **Image Pull Policy:** `IfNotPresent` (uses local registry).
- **Build Context:** Docker multi-stage builds on the server (`/root/ecommerce/`), pushed to `localhost:5000`.
- **Frontend:** Angular production build → nginx:alpine.

---

## 3. Services & Networking

### 3.1 Service Registry

All internal services use ClusterIP services with Eureka for service discovery:

| Service | ClusterIP | Port(s) | Protocol |
|---|---|---|---|
| api-gateway | 10.43.33.121 | 8080 | TCP |
| frontend | 10.43.254.130 | 80 | TCP |
| keycloak | 10.43.223.111 | 8180, 9000 | TCP |
| eureka-server | 10.43.160.59 | 8761 | TCP |
| product-service | 10.43.79.72 | 8081 | TCP |
| cart-service | 10.43.95.242 | 8088 | TCP |
| order-service | 10.43.237.47 | 8084 | TCP |
| auth-service | 10.43.161.230 | 8087 | TCP |
| user-service | 10.43.146.197 | 8086 | TCP |
| category-service | 10.43.235.67 | 8082 | TCP |
| inventory-service | 10.43.102.46 | 8083 | TCP |
| payment-service | 10.43.243.44 | 8085 | TCP |
| coupon-service | 10.43.57.31 | 8089 | TCP |
| review-service | 10.43.49.115 | 8090 | TCP |
| notification-service | 10.43.78.144 | 8091 | TCP |
| elastic-service | 10.43.17.93 | 8092 | TCP |
| admin-service | 10.43.125.64 | 8093 | TCP |
| postgres | 10.43.56.199 | 5432 | TCP |
| redis | 10.43.217.166 | 6379 | TCP |
| kafka | 10.43.123.69 | 9092 | TCP |
| elasticsearch | 10.43.108.234 | 9200, 9300 | TCP |
| pgadmin | 10.43.233.81 | 80 | TCP |
| kafka-ui | 10.43.81.158 | 8080 | TCP |

### 3.2 Service Communication

- **Frontend → API:** HTTPS via ingress (`/api` → api-gateway:8080)
- **Gateway → Services:** HTTP via Eureka discovery or direct service DNS
- **Service-to-Service:** Feign clients with `ServiceTokenRelayInterceptor` (OAuth2 client_credentials)
- **Config:** `shopfast-config` ConfigMap injected as env vars; secrets via `shopfast-secrets`

---

## 4. Ingress & TLS

### 4.1 Ingress Controller

- **Class:** `nginx`
- **Address:** `72.62.250.5`
- **Ports:** 80 (HTTP), 443 (HTTPS)
- **TLS:** Self-signed certificate (`shopfast-tls` secret)

### 4.2 Routing Rules

| Host | Path | Backend Service | Port |
|---|---|---|---|
| shopfast.live | `/api` | api-gateway | 8080 |
| shopfast.live | `/realms` | api-gateway → Keycloak proxy | 8080 |
| shopfast.live | `/` | frontend | 80 |
| shopfast.live | `/eureka` | eureka-server | 8761 |
| shopfast.live | `/kafka` | kafka-ui | 8080 |
| shopfast.live | `/pgadmin` | pgadmin | 80 |
| www.shopfast.live | `/` | frontend | 80 |
| shopfast-test.live | `/api`, `/realms`, `/` | api-gateway / frontend | 8080/80 |
| www.shopfast-test.live | `/` | frontend | 80 |

### 4.3 TLS Configuration

```yaml
tls:
  - hosts:
      - shopfast.live
      - www.shopfast.live
    secretName: shopfast-tls
```

- **Issuer:** `shopfast-selfsigned` (self-signed ClusterIssuer)
- **Certificate:** `shopfast-tls` (managed by cert-manager)
- **Production TODO:** Replace with Let's Encrypt ClusterIssuer

### 4.4 Frontend Nginx Config

The `frontend-nginx-config` ConfigMap provides:
- SPA fallback routing (all non-API requests → `index.html`)
- Legacy proxy paths (`/api`, `/users`, `/orders` → admin-service:8093)

> **Issue:** The `/api` path in frontend-nginx bypasses the api-gateway for admin paths. This is a legacy configuration that should be reviewed.

---

## 5. Configuration & Secrets

### 5.1 ConfigMaps

| ConfigMap | Keys | Purpose |
|---|---|---|
| `shopfast-config` | 26 keys | DB, Redis, Kafka, Keycloak (issuer/base/jwk), Eureka URLs |
| `keycloak-realm` | 1 key (`realm-export.json`) | **Empty (0 bytes)** — realm import may be broken |
| `elasticsearch-config` | 1 key | Elasticsearch URI |
| `frontend-nginx-config` | 1 key (`default.conf`) | Nginx SPA routing |
| `postgres-init` | SQL scripts | Database initialization |

### 5.2 ConfigMap Key Values (shopfast-config)

```
KEYCLOAK_ISSUER_URI   = https://shopfast.live/realms/shopfast
KEYCLOAK_BASE_URL     = http://keycloak:8180
KEYCLOAK_JWK_SET_URI  = http://keycloak:8180/realms/shopfast/protocol/openid-connect/certs
KEYCLOAK_REALM        = shopfast
KEYCLOAK_ADMIN_CLIENT_ID = shopfast-services
POSTGRES_HOST         = postgres
POSTGRES_PORT         = 5432
REDIS_HOST            = redis
REDIS_PORT            = 6379
KAFKA_BOOTSTRAP       = kafka:9092
EUREKA_URL            = http://eureka-server:8761/eureka/eureka/
```

### 5.3 Secrets (`shopfast-secrets`)

| Key | Purpose |
|---|---|
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Symmetric JWT fallback/legacy |
| `KEYCLOAK_ADMIN` | Keycloak admin username |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password |
| `SHOPFAST_SERVICES_CLIENT_SECRET` | Service-to-service client_credentials |
| `SHOPFAST_ADMIN_CLIENT_SECRET` | Admin client secret |
| `APP_PASSWORD_ENCRYPTION_KEY` | Application-level encryption key |

---

## 6. Security

### 6.1 Identity & Access Management

| Component | Details |
|---|---|
| **IdP** | Keycloak 26 (RS256) |
| **Web client** | `shopfast-web` (public, PKCE) |
| **Service client** | `shopfast-services` (confidential, client_credentials, serviceAccountsEnabled=true) |
| **Admin client** | `shopfast-admin` (confidential) |
| **Token issuer** | `https://shopfast.live/realms/shopfast` |
| **Audience mapper** | `shopfast-web` has audience mapper → `aud: ["shopfast-web", "account"]` |

### 6.2 Frontend Authentication

- **Library:** `angular-auth-oidc-client` v19
- **Flow:** Authorization Code + PKCE
- **Token storage:** SessionStorage (not localStorage)
- **Silent renew:** Enabled (120s before expiry)
- **Custom interceptor:** `tokenInterceptor` attaches bearer token to `/api/**` requests synchronously

### 6.3 Backend Resource Servers

- All services validate JWT against `https://shopfast.live/realms/shopfast`
- `/api/v1/product/internal/**` endpoints require `.authenticated()` (service-to-service)
- Public endpoints (GET /api/v1/product/**) are `permitAll`
- Admin endpoints require `ROLE_ADMIN`

### 6.4 Service-to-Service Authentication

- **Mechanism:** `ServiceTokenRelayInterceptor` (common-lib)
- **Client:** `shopfast-services` (client_credentials grant)
- **Fallback:** Relays caller's user token when present
- **Activation:** Conditional on `shopfast.security.service-client.client-secret` being set

### 6.5 Network Policies

| Policy | Purpose |
|---|---|
| `default-deny-ingress` | Deny all ingress by default |
| `allow-ingress-to-edge` | Allow ingress-nginx to reach api-gateway, frontend, eureka, kafka-ui, pgadmin |
| `allow-intra-namespace` | Allow all pods to communicate within namespace (Eureka/Feign/Kafka) |
| `allow-egress` | Allow DNS + external egress |

### 6.6 Security Gaps

| Gap | Severity |
|---|---|
| No RBAC (ServiceAccounts, Roles, RoleBindings) | High |
| Self-signed TLS only (no Let's Encrypt) | Medium |
| Secrets in plain Kubernetes Secrets (not Sealed Secrets) | Medium |
| No PodSecurity Standards enforcement | Medium |
| `frontend-nginx-config` has stale `/api` paths bypassing gateway | Low |
| No image scanning in CI/CD | Low |

---

## 7. Storage

### 7.1 Persistent Volume Claims

| PVC | Size | Usage |
|---|---|---|
| `keycloak-data` | 4Gi | Keycloak realm data + user sessions |
| *(postgres PVC)* | *(not verified)* | Cart, Order, User databases |

### 7.2 Storage Classes

- Not explicitly defined — uses k3s default storage class.
- No backup/restore strategy configured.

---

## 8. Autoscaling

### 8.1 Horizontal Pod Autoscalers

| HPA | Target | Min | Max | Current |
|---|---|---|---|---|
| `api-gateway-hpa` | CPU 70% | 1 | 6 | 1 |
| `order-service-hpa` | CPU 70% | 1 | 6 | 1 |
| `product-service-hpa` | CPU 70% | 1 | 6 | 1 |

> **Note:** HPA requires metrics-server. On single-node k3s, this is typically available by default.

---

## 9. GitOps (ArgoCD)

### 9.1 Applications

| App | Repository | Path | Sync Policy |
|---|---|---|---|
| `shopfast-services` | `https://github.com/JunaidIshaq/ecommerce.git` | `k8s/overlays/dev` | Automated (Prune) |
| `shopfast-infra` | `https://github.com/JunaidIshaq/ecommerce.git` | `k8s/overlays/dev` | Automated (Prune) |
| `shopfast-ingress` | `https://github.com/JunaidIshaq/ecommerce.git` | `k8s/ingress` | Automated (Prune) |

### 9.2 Kustomize Structure

```
k8s/
├── base/
│   ├── namespace.yaml
│   ├── configmap.yaml          # shopfast-config (shared env)
│   ├── network-policies.yaml
│   ├── quota.yaml
│   └── secrets/
│       └── secrets.yaml        # shopfast-secrets
├── services/                   # 18 service manifests
│   ├── frontend.yaml
│   ├── api-gateway.yaml
│   ├── keycloak (in infra)
│   └── ...
├── infra/
│   ├── postgres.yaml
│   ├── redis.yaml
│   ├── kafka.yaml
│   ├── keycloak.yaml
│   └── ...
├── ingress/
│   └── ingress.yaml
└── overlays/dev/
    └── kustomization.yaml      # Retags ghcr.io → localhost:5000, scales to 1 replica
```

### 9.3 Image Tag Strategy (Dev)

| Original Image | Dev Image |
|---|---|
| `ghcr.io/shopfast/frontend` | `localhost:5000/shopfast/frontend:dev` |
| `ghcr.io/shopfast/api-gateway` | `localhost:5000/shopfast/api-gateway:2d9e2e2` |
| `ghcr.io/shopfast/eureka-server` | `localhost:5000/shopfast/eureka-server:dev` |
| All other services | `localhost:5000/shopfast/<service>:dev` |

> **Note:** The api-gateway uses a pinned SHA (`2d9e2e2`) instead of `dev` tag.

### 9.4 Current Git State

- **HEAD:** `f48826b` — fix: AuthService.getAccessToken returns string
- **Unsynced commits:** `773026f` (issuer alignment + frontend interceptor), `f48826b` (auth fix)
- **Sync Status:** OutOfSync from HEAD

---

## 10. Issues Fixed

| Date | Issue | Root Cause | Fix Applied |
|---|---|---|---|
| 2026-08-16 | Keycloak login → callback loop | `CallbackComponent` called `authorize()` on `checkAuth()` error | Navigate to `/` on error; consume return URL |
| 2026-08-16 | userinfo 401 "iss claim is not valid" | `KC_HOSTNAME` unset → inconsistent issuer derivation | Set `KC_HOSTNAME=https://shopfast.live` |
| 2026-08-16 | userinfo 401 "audience" | No audience mapper on `shopfast-web` client | Added `oidc-audience-mapper` protocol mapper |
| 2026-08-16 | API Gateway crash (JWT validation) | Gateway `KEYCLOAK_ISSUER_URI=http://keycloak:8180` vs Keycloak public issuer `https://shopfast.live` | Updated gateway env to public issuer |
| 2026-08-16 | Cart 500 / Service 401 | Backend services validated tokens against internal issuer while Keycloak issued public-iss tokens | Updated `KEYCLOAK_ISSUER_URI` to `https://shopfast.live` in ConfigMap + all service manifests |
| 2026-08-17 | Frontend `Bearer [object Object]` | `AuthService.getAccessToken()` returned Observable cast to string | Read token synchronously from `sessionStorage` |
| 2026-08-17 | Frontend no token in API headers | Library interceptor not reliably attaching token | Added explicit `tokenInterceptor` |

---

## 11. Remaining Gaps

### 11.1 Critical / Immediate

| # | Gap | Impact | Recommendation |
|---|---|---|---|
| 1 | **ArgoCD OutOfSync** | Latest config/issuer changes not fully propagated | Trigger manual sync for `shopfast-services` and `shopfast-infra` |
| 2 | **auth-service CrashLoopBackOff** | Auth endpoints unstable | Investigate pod logs; old pod still serving |
| 3 | **keycloak-realm ConfigMap empty** | Realm may not auto-import from `realm-export.json` | Verify realm import config; fix ConfigMap |
| 4 | **frontend-nginx-config stale paths** | `/api`, `/users`, `/orders` bypass api-gateway auth | Review and update proxy targets |
| 5 | **Kafka 68 restarts** | Message queue instability | Check ZooKeeper connectivity, resource limits, logs |

### 11.2 Security Hardening

| # | Gap | Recommendation |
|---|---|---|
| 1 | No RBAC (ServiceAccounts, Roles, RoleBindings) | Implement least-privilege RBAC for all service accounts |
| 2 | Self-signed TLS only | Replace cert-manager Issuer with Let's Encrypt ClusterIssuer |
| 3 | Secrets in plain Kubernetes Secrets | Consider Sealed Secrets or external vault (HashiCorp Vault) |
| 4 | No PodSecurity Standards | Add `runAsNonRoot`, `readOnlyRootFilesystem`, `allowPrivilegeEscalation: false` |
| 5 | No image scanning | Add Trivy/Clair to CI pipeline |
| 6 | No network segmentation beyond 3 policies | Fine-trace NetworkPolicies per service |

### 11.3 Observability & Operations

| # | Gap | Recommendation |
|---|---|---|
| 1 | No Prometheus/Grafana | Add monitoring stack; HPA needs metrics-server |
| 2 | No centralized logging | Add ELK/Loki/PLG stack |
| 3 | No distributed tracing | Add OpenTelemetry instrumentation |
| 4 | No backup strategy | PVC snapshots for Postgres, Keycloak, Redis |
| 5 | No resource limits | Add `resources.requests/limits` to all deployments |
| 6 | No PodDisruptionBudgets | Ensure availability during node maintenance |
| 7 | No health check refinement | Add custom liveness/readiness probes beyond `/actuator/health` |

### 11.4 CI/CD

| # | Gap | Recommendation |
|---|---|---|
| 1 | No automated image build on push | Add GitHub Actions workflow |
| 2 | No automated ArgoCD sync | Add webhook or sync automation on merge |
| 3 | Image tags are `dev`/manual SHA | Use Git SHA or semantic version tags |
| 4 | No test stage | Add unit/integration tests before image build |
| 5 | No staging environment | Add `overlays/staging` for pre-prod validation |

---

## 12. Current State Summary

```
Cluster:        UP (23/24 pods running, 1 CrashLoopBackOff)
Ingress:        UP (nginx + self-signed TLS)
Keycloak:       UP (public issuer https://shopfast.live/realms/shopfast)
API Gateway:    UP (public issuer validated)
Frontend:       UP (token interceptor deployed, auth fix deployed)
Auth Service:   DEGRADED (CrashLoopBackOff, 14 restarts)
ArgoCD:         OutOfSync (manual sync needed)
Kafka:          UNSTABLE (68 restarts)
Eureka:         UP
Databases:      UP (Postgres, Redis)
```

### Recommended Next Actions

1. **Trigger ArgoCD sync** for `shopfast-services` and `shopfast-infra` to apply latest issuer/config changes
2. **Investigate auth-service CrashLoopBackOff** — check logs for root cause
3. **Fix empty `keycloak-realm` ConfigMap** — ensure realm-export.json is loaded
4. **Clean up `frontend-nginx-config`** — remove stale `/api`, `/users`, `/orders` admin paths
5. **Investigate Kafka restarts** — check ZooKeeper connectivity and resource pressure
6. **Implement RBAC and Pod Security Standards**
7. **Replace self-signed TLS with Let's Encrypt**

---

## Appendix A: Manifest Structure

```
k8s/
├── argocd/
│   ├── application-dev.yaml
│   ├── application-infra.yaml
│   ├── application-ingress.yaml
│   ├── application-services.yaml
│   └── applicationset.yaml
├── base/
│   ├── configmap.yaml              # shopfast-config (26 keys)
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   ├── network-policies.yaml       # 4 policies
│   ├── quota.yaml
│   ├── sealed-secret-example.yaml
│   ├── servicemonitor.yaml
│   └── secrets/
│       ├── kustomization.yaml
│       └── secrets.yaml            # shopfast-secrets
├── infra/
│   ├── elasticsearch.yaml
│   ├── kafka.yaml
│   ├── keycloak-realm-configmap.yaml
│   ├── keycloak.yaml               # Keycloak 26 deployment
│   ├── kustomization.yaml
│   ├── pgadmin.yaml
│   ├── postgres.yaml
│   ├── realm-export.json
│   └── redis.yaml
├── ingress/
│   ├── ingress.yaml                # nginx ingress + cert-manager
│   └── kustomization.yaml
├── overlays/
│   ├── dev/
│   │   └── kustomization.yaml      # Dev overlay (retag images, scale 1)
│   └── prod/
│       ├── kustomization.yaml
│       └── prod-issuer.yaml
└── services/
    ├── admin-service.yaml
    ├── api-gateway.yaml
    ├── auth-service.yaml
    ├── cart-service.yaml
    ├── category-service.yaml
    ├── coupon-service.yaml
    ├── elastic-service.yaml
    ├── eureka-server.yaml
    ├── frontend-nginx-config.yaml
    ├── frontend.yaml
    ├── hpa.yaml                     # HPA for gateway, order, product
    ├── inventory-service.yaml
    ├── kustomization.yaml
    ├── notification-service.yaml
    ├── order-service.yaml
    ├── payment-service.yaml
    ├── product-service.yaml
    ├── review-service.yaml
    └── user-service.yaml
```

## Appendix B: Environment Variables (shopfast-config)

| Variable | Value | Used By |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | `https://shopfast.live/realms/shopfast` | All services (JWT validation) |
| `KEYCLOAK_BASE_URL` | `http://keycloak:8180` | Admin/management calls |
| `KEYCLOAK_JWK_SET_URI` | `http://keycloak:8180/realms/shopfast/protocol/openid-connect/certs` | JWKS fetching |
| `KEYCLOAK_REALM` | `shopfast` | Keycloak realm name |
| `POSTGRES_HOST` | `postgres` | All DB services |
| `POSTGRES_PORT` | `5432` | All DB services |
| `REDIS_HOST` | `redis` | Cart, cache services |
| `REDIS_PORT` | `6379` | Cart, cache services |
| `KAFKA_BOOTSTRAP` | `kafka:9092` | Kafka producers/consumers |
| `EUREKA_URL` | `http://eureka-server:8761/eureka/eureka/` | All services |

---

*Report generated: 2026-08-17*
