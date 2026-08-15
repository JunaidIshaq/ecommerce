#!/usr/bin/env python3
"""Generate per-service Kubernetes manifests for ShopFast microservices.

Each service gets a Deployment + ClusterIP Service following the shared
contract in k8s/services/README.md: envFrom configMap+secret, readiness/
liveness/startup probes on Actuator, non-root security context, resource
limits, and any service-specific env (DB url, feign URLs, seed flags).

Run from repo root:
    python3 scripts/gen-k8s-services.py
"""
import os

OUT_DIR = "k8s/services"

# (name, port, db_name or None, extra_env dict, replicas)
SERVICES = [
    ("product-service", 8081, "product_db", {
        "CATEGORY_SERVICE_URL": "http://category-service:8082/api/v1/category",
        "APP_SEED_PRODUCTS": "true",
        "REDIS_HOST": "redis",
        "REDIS_PORT": "6379",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
    }, 2,
        '"until wget -q -O /dev/null http://category-service:8082/actuator/health; do echo waiting for category-service; sleep 3; done; echo category-service ready"'),
    ("category-service", 8082, "category_db", {
        "PRODUCT_SERVICE_URL": "http://product-service:8081/api/v1/product",
        "APP_SEED_CATEGORIES": "true",
        "REDIS_HOST": "redis",
        "REDIS_PORT": "6379",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
        "EUREKA_URL": "http://eureka-server:8761/eureka/eureka/",
    }, 2),
    ("inventory-service", 8083, "inventory_db", {
        "REDIS_HOST": "redis",
        "REDIS_PORT": "6379",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
    }, 2),
    ("order-service", 8084, "order_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
    }, 2),
    ("payment-service", 8085, "payment_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "REDIS_HOST": "redis",
        "REDIS_PORT": "6379",
        "EUREKA_URL": "http://eureka-server:8761/eureka/eureka/",
    }, 2),
    ("user-service", 8086, "user_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "REDIS_HOST": "redis",
        "REDIS_PORT": "6379",
        "EUREKA_URL": "http://eureka-server:8761/eureka/eureka/",
    }, 2),
    ("auth-service", 8087, "auth_db", {
        "KEYCLOAK_BASE_URL": "http://keycloak:8180",
        "KEYCLOAK_REALM": "shopfast",
        "KEYCLOAK_ADMIN_CLIENT_ID": "shopfast-services",
        "USER_SERVICE_URL": "http://user-service:8086",
        "SPRING_KAFKA_BOOTSTRAP_SERVERS": "kafka:9092",
        "REDIS_HOST": "redis",
        "REDIS_PORT": "6379",
        "APP_PASSWORD_ENCRYPTION_KEY": None,  # from secret
    }, 2,
        '"until wget -q -O /dev/null http://eureka-server:8761/eureka/actuator/health; do echo waiting for eureka-server; sleep 3; done; echo eureka-server ready"'),
    ("cart-service", 8088, "cart_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
    }, 2),
    ("coupon-service", 8089, "coupon_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
    }, 2),
    ("review-service", 8090, "review_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
    }, 2),
    ("notification-service", 8091, "notification_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
        "MANAGEMENT_HEALTH_MAIL_ENABLED": "false",
    }, 2),
    ("elastic-service", 8092, None, {
        "ELASTIC_URI": "http://elasticsearch:9200",
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
        "MANAGEMENT_HEALTH_ELASTICSEARCH_ENABLED": "false",
    }, 2),
    ("admin-service", 8093, "admin_db", {
        "KAFKA_BOOTSTRAP": "kafka:9092",
        "LOGSTASH_HOST": "logstash",
        "LOGSTASH_PORT": "5000",
        "MANAGEMENT_HEALTH_ELASTICSEARCH_ENABLED": "false",
        "MANAGEMENT_HEALTH_MAIL_ENABLED": "false",
        "EUREKA_URL": "http://eureka-server:8761/eureka/eureka/",
        "USER_SERVICE_URL": "http://user-service:8086",
        "ORDER_SERVICE_URL": "http://order-service:8084",
        "CART_SERVICE_URL": "http://cart-service:8088",
        "CATEGORY_SERVICE_URL": "http://category-service:8082",
        "PRODUCT_SERVICE_URL": "http://product-service:8081",
        "INVENTORY_SERVICE_URL": "http://inventory-service:8083",
        "COUPON_SERVICE_URL": "http://coupon-service:8089",
    }, 2),
]


def env_block(name, db_name, extra, port):
    lines = []
    if db_name:
        lines.append(f'            - name: SPRING_DATASOURCE_URL\n'
                     f'              value: "jdbc:postgresql://postgres:5432/{db_name}"')
        lines.append(f'            - name: SPRING_DATASOURCE_USERNAME\n'
                     f'              valueFrom:\n'
                     f'                configMapKeyRef:\n'
                     f'                  name: shopfast-config\n'
                     f'                  key: POSTGRES_USER')
        lines.append(f'            - name: SPRING_DATASOURCE_PASSWORD\n'
                     f'              valueFrom:\n'
                     f'                secretKeyRef:\n'
                     f'                  name: shopfast-secrets\n'
                     f'                  key: DB_PASSWORD')
    if name == "auth-service":
        lines.append(f'            - name: APP_PASSWORD_ENCRYPTION_KEY\n'
                     f'              valueFrom:\n'
                     f'                secretKeyRef:\n'
                     f'                  name: shopfast-secrets\n'
                     f'                  key: APP_PASSWORD_ENCRYPTION_KEY')
    for k, v in extra.items():
        if v is None:
            continue
        lines.append(f'            - name: {k}\n              value: "{v}"')
    # JWT_SECRET for services that accept legacy HS256 (all except admin-service)
    if name != "admin-service":
        lines.append(f'            - name: JWT_SECRET\n'
                     f'              valueFrom:\n'
                     f'                secretKeyRef:\n'
                     f'                  name: shopfast-secrets\n'
                     f'                  key: JWT_SECRET')
    # Keycloak issuer / JWKS for token validation
    lines.append(f'            - name: KEYCLOAK_ISSUER_URI\n'
                 f'              value: "http://keycloak:8180/realms/shopfast"')
    lines.append(f'            - name: KEYCLOAK_JWK_SET_URI\n'
                 f'              value: "http://keycloak:8180/realms/shopfast/protocol/openid-connect/certs"')
    lines.append(f'            - name: SERVER_PORT\n              value: "{port}"')
    return "\n".join(lines)


def manifest(name, port, db_name, extra, replicas, init_wait_cmd='"true"'):
    return f"""# {name} — auto-generated by scripts/gen-k8s-services.py
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {name}
  namespace: shopfast
  labels:
    app: {name}
spec:
  replicas: {replicas}
  selector:
    matchLabels:
      app: {name}
  template:
    metadata:
      labels:
        app: {name}
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      initContainers:
        - name: wait-for-deps
          image: busybox:1.36
          command: ["sh", "-c", {init_wait_cmd}]
      containers:
        - name: {name}
          image: ghcr.io/shopfast/{name}:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: {port}
          envFrom:
            - configMapRef:
                name: shopfast-config
            - secretRef:
                name: shopfast-secrets
          env:
{env_block(name, db_name, extra, port)}
          volumeMounts:
            - name: tmp
              mountPath: /tmp
          resources:
            requests:
              cpu: 100m
              memory: 256Mi
            limits:
              cpu: "1"
              memory: 512Mi
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: {port}
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: {port}
            initialDelaySeconds: 60
            periodSeconds: 20
            timeoutSeconds: 5
            failureThreshold: 5
          startupProbe:
            httpGet:
              path: /actuator/health
              port: {port}
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 5
            failureThreshold: 30
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop: ["ALL"]
      volumes:
        - name: tmp
          emptyDir: {{}}
---
apiVersion: v1
kind: Service
metadata:
  name: {name}
  namespace: shopfast
spec:
  selector:
    app: {name}
  ports:
    - port: {port}
      targetPort: {port}
  type: ClusterIP
"""


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for svc in SERVICES:
        name = svc[0]
        init_wait = svc[5] if len(svc) > 5 else '"true"'
        content = manifest(svc[0], svc[1], svc[2], svc[3], svc[4], init_wait)
        path = os.path.join(OUT_DIR, f"{name}.yaml")
        with open(path, "w") as f:
            f.write(content)
        print("wrote", path)


if __name__ == "__main__":
    main()
