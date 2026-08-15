# ShopFast → Kubernetes: Complete Migration Plan

## 1. Context & Goal

ShopFast is a production-grade e-commerce platform built as **16+ Spring Boot microservices** with:
- **Service discovery**: Netflix Eureka
- **Identity**: Keycloak 26
- **Messaging**: Kafka + Zookeeper
- **Datastores**: PostgreSQL 15, Redis 7, Elasticsearch 8
- **Logging**: ELK (Filebeat / Logstash / Elasticsearch / Kibana)
- **Edge**: Nginx (shopfast.live, shopfast_test.live)
- **Orchestration today**: Docker Compose

**Goal:** Migrate to Kubernetes for elastic scaling, self-healing, standardized config/secrets, and GitOps-driven delivery — with zero-downtime rollouts.

---

## 2. Prerequisites & Environment

1. **Local cluster** (pick one):
   - Docker Desktop with Kubernetes enabled, **or**
   - `minikube start --cpus=4 --memory=8192 --driver=docker` (recommended for this size).
2. **Tooling**: `kubectl`, `helm`, `kustomize` (bundled), `skaffold` (dev loop), `k9s` (optional UI).
3. **Registry**: local minikube registry for dev; Docker Hub / GHCR for real deploys. Namespace convention: `shopfast/<service>:<tag>`.

---

## 3. Container Image Preparation

- Build **common-lib** once and publish it so each service image stops rebuilding it.
- Tag images by **git SHA** (never `latest`) to enable rollbacks.
- Shrink images: switch the run stage from JDK to `eclipse-temurin:21-jre`.
- Script builds: `scripts/build-images.sh` or a `skaffold.yaml`.

---

## 4. Namespace & Configuration Strategy

- Create `k8s/overlays/` (base + dev/prod) using **Kustomize**.
- Namespaces: `shopfast` (dev), `shopfast-prod`.
- Externalize config:
  - Non-secret values → **ConfigMaps**
  - Passwords, Keycloak secrets, JWT keys → **Secrets**
- Keep **Eureka** for discovery initially; point services to `eureka-server.shopfast.svc.cluster.local`.
  - Optional later: replace Eureka with Kubernetes Service DNS.
- Inject via `envFrom: configMapRef / secretRef` in pod specs.

---

## 5. Stateful Backing Services (Deploy First)

Use **StatefulSets + PVCs** or Helm charts:

| Service | Deployment |
|---|---|
| PostgreSQL 15 | Helm `bitnami/postgresql`, PVC 10Gi, Secret for creds |
| Redis 7 | Helm `bitnami/redis` (or `redis-ha`) |
| Kafka + Zookeeper | Helm `bitnami/kafka` or Strimzi Operator; topics via init job or `KafkaTopic` CRDs |
| Elasticsearch 8 | Helm `elastic/elasticsearch` + Kibana; reuse existing Filebeat (DaemonSet) & Logstash (Deployment) reading pod stdout |
| Keycloak 26 | Helm `bitnami/keycloak` / `codecentric/keycloak`, import realm |

---

## 6. Stateless Microservices

For each of the 16 services (eureka-server, api-gateway, auth, user, product, category, inventory, cart, coupon, order, payment, review, notification, admin, elastic-service, frontend):

- **Deployment**: 2 replicas, `resources.requests/limits` (e.g. 512Mi/1Gi).
- **Probes**: `readinessProbe` (`/actuator/health/readiness`), `livenessProbe` (`/actuator/health/liveness`), `startupProbe` for slow JVM boot.
- **Service**: ClusterIP exposing the port.
- **HPA**: CPU > 70% for busy services (gateway, order, product).
- Generate uniformly via a **Kustomize base + per-service patches** or a templated Helm chart.

---

## 7. Ingress & Routing

- Deploy **ingress-nginx**; map existing `Nginx/shopfast.live` path rules to an `Ingress` resource.
- Build & deploy the **frontend** image; expose it and api-gateway via the same Ingress hosts.
- **TLS**: cert-manager + Let's Encrypt (self-signed for dev) for shopfast.live / shopfast_test.live.
- Route `/api/*` to api-gateway; let the gateway fan out internally.

---

## 8. Observability (ELK already present)

- **Logging**: Filebeat DaemonSet → Logstash → Elasticsearch → Kibana (reuse configs).
- **Metrics**: Prometheus + Grafana via `kube-prometheus-stack`; scrape Actuator `/actuator/prometheus`.
- **Tracing** (optional): Spring Cloud Sleuth → Zipkin/Jaeger.
- Build per-service Grafana dashboards + node-health views.

---

## 9. Resilience & Security

- **Resilience4j** circuit breakers on api-gateway.
- **NetworkPolicies**: deny-by-default; only api-gateway reachable from ingress.
- **Secrets**: SealedSecrets or External Secrets Operator (never commit raw Secrets).
- **Pod security**: non-root `securityContext`; namespace **resource quotas**.

---

## 10. CI/CD

Extend `.github/workflows/docker-image.yml`:
1. On push/PR: `mvn test` → build images → push to registry → `kubectl apply -k k8s/overlays/dev`.
2. **GitOps**: Argo CD syncs `k8s/overlays/prod` from git → cluster (easy rollbacks).
3. Add **Trivy** image scan in CI before push.

---

## 11. Local Dev Workflow

- `skaffold dev` for hot-reload on a single service.
- `kubectl port-forward` to debug DBs/Kafka/Keycloak.
- One-command bring-up: `kubectl apply -k k8s/overlays/dev`.

---

## 12. Suggested Repository Layout

```
k8s/
  base/            <- Deployments, Services, ConfigMaps (shared)
  overlays/
    dev/
    prod/
  infra/           <- Helm releases / operators (pg, redis, kafka, es, keycloak)
  ingress/         <- Ingress + TLS + cert-manager
scripts/
  build-images.sh
  deploy.sh
```

---

## 13. Migration Order (Low → High Risk)

1. Namespace + ConfigMaps/Secrets scaffolding
2. Infra: PostgreSQL, Redis, Kafka, Elasticsearch, Keycloak
3. Eureka + api-gateway
4. Leaf services: product, category, user, auth
5. Order / cart / payment / notification flow
6. Frontend + Ingress + TLS
7. Observability + HPA + NetworkPolicies
8. CI/CD + Argo CD + prod overlay

---

## 14. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Eureka vs K8s DNS overlap | Keep Eureka initially; migrate to K8s Service discovery per service once stable |
| Stateful data loss | Use PVCs with backups; test restore before cutover |
| Image bloat / slow boot | JRE base, readiness/startup probes, SHA tags, resource limits |
| Secret leakage | SealedSecrets / External Secrets; Trivy scanning |
| Config drift between envs | Kustomize overlays + Argo CD GitOps single source of truth |

---

## 15. Next Step

Implement **Phases 3–6** first: a Kustomize base + reusable service template (start with `product-service`), plus the PostgreSQL/Redis/Kafka infra Helm installs — to stand up a working vertical slice, then expand service-by-service.
