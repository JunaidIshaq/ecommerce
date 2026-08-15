# ===========================================================================
# Reusable service template / conventions for ALL ShopFast microservices.
#
# Every service Deployment below follows this contract:
#
#   - 2 replicas (overridable; gateway/order/product can scale via HPA)
#   - resources.requests/limits inherited from LimitRange unless set
#   - envFrom: configMapRef(shopfast-config) + secretRef(shopfast-secrets)
#     so config and secrets are injected uniformly (plan section 4)
#   - readiness  -> /actuator/health/readiness
#     liveness   -> /actuator/health/liveness
#     startup    -> /actuator/health (gives the JVM time to boot)
#   - securityContext: runs as non-root, read-only root FS, dropped caps
#   - image: <IMAGE_REGISTRY>/<name>:<IMAGE_TAG>  (SHA in prod, see plan 3 & 10)
#
# A service that needs extra env (DB url, feign URLs, seed flags) adds them in
# its own env: block; the shared values come from the two envFrom sources.
# ===========================================================================
