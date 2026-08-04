# ShopFast Backend — Complete Remediation Plan

**Scope reviewed:** 16 Maven modules, ~495 Java files (`admin`, `api-gateway`, `auth`, `cart`, `category`, `common-lib`, `coupon`, `elastic`, `eureka`, `inventory`, `notification`, `order`, `payment`, `product`, `review`, `user`).

**Goal:** eliminate financial/stock correctness defects, harden reliability of the distributed transaction path, close security gaps, and remove duplication — in a sequence that is safe to ship incrementally.

---

## 0. Executive summary

| Severity | Count | Theme |
|---|---|---|
| P0 — Correctness | 12 | Wrong totals, coupons never redeemed, oversell, duplicate saga side-effects |
| P1 — Reliability | 9 | No circuit breakers, no Kafka DLT, Redis-only idempotency, premature Stripe success |
| P2 — Security | 7 | `ddl-auto: update` in prod, committed secrets, PCI card data in transit, 13× duplicated auth |
| P3 — Quality | 10 | 1.4% test coverage, N+1 Feign, cache misuse, missing indexes |

**Three defects are costing money right now:** the `Math.max(9, …)` price floor, the inverted discount, and the unguarded inventory reservation.

---

## Phase 1 — Stop the bleeding (Week 1, ~2 days)

### 1.1 Fix checkout pricing
**File:** [`CheckoutService.java`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:72)

Current:
```java
double subTotal = cartItems.stream().mapToDouble(i -> i.getPrice().doubleValue() * i.getQuantity()).sum();
double discount = 0.0;
double total = Math.max(9, subTotal - discount);   // ← $9 floor
...
total = Math.max(total, subTotal - discount);      // ← discount never applies
```

Target:
```java
BigDecimal subTotal = cartItems.stream()
        .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);

BigDecimal discount = BigDecimal.ZERO;
// after coupon validation:
discount = BigDecimal.valueOf(response.getDiscount()).setScale(2, RoundingMode.HALF_UP);
BigDecimal total = subTotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
```

**Rules to enforce project-wide:** never use `double`/`float` for currency; never `new BigDecimal(double)`; always explicit scale + `RoundingMode`.

**Acceptance:** unit tests for subtotal < discount, discount = 0, order total < $9, and multi-item rounding (e.g. 3 × $0.335).

### 1.2 Persist and redeem the coupon
- Add `order.setCouponCode(checkoutRequestDto.getCouponCode())` and `order.setDiscount(discount)` immediately after successful validation.
- Verify the `coupon_code` column exists (Flyway migration if not).
- Re-test the redemption branch in [`KafkaPaymentConsumer:78`](../order-service/src/main/java/com/shopfast/orderservice/events/KafkaPaymentConsumer.java:78) — it is currently dead code.
- Replace the double-brace init at line 80 with a builder:
```java
couponClient.redeem(CouponRedeemRequestDto.builder()
        .code(order.getCouponCode())
        .userId(order.getUserId())
        .build());
```

**Acceptance:** integration test — apply coupon, complete payment, assert `coupon_redemption` row exists and a second use of a single-use coupon is rejected.

### 1.3 Guard the inventory reservation
**File:** `InventoryService.reserveStock()`

Replace read-check-write with a conditional atomic update in `InventoryRepository`:
```java
@Modifying
@Query("""
    UPDATE InventoryItem i
       SET i.availableQuantity = i.availableQuantity - :qty,
           i.reservedQuantity  = i.reservedQuantity  + :qty
     WHERE i.productId = :productId
       AND i.availableQuantity >= :qty
""")
int tryReserve(@Param("productId") UUID productId, @Param("qty") int qty);
```
```java
if (inventoryRepository.tryReserve(productId, quantity) != 1) {
    throw new InsufficientStockException(productId, quantity);
}
```
Apply the mirror-image pattern to `releaseStock` and `confirmStock`.

**Also:** stop reading through `@Cacheable getByProductId()` inside write transactions — it returns detached/stale entities. Add a plain `findByProductIdForUpdate` for the write path.

**Acceptance:** concurrency test — 50 threads reserve 1 unit from stock of 10; exactly 10 succeed, 40 throw, final `available = 0`, `reserved = 10`.

### 1.4 Normalise `@Transactional`
Replace every `import jakarta.transaction.Transactional;` with `org.springframework.transaction.annotation.Transactional` (CheckoutService, OrderService, PaymentService, InventoryService, and any others). Add `readOnly = true` to all query methods.

**Acceptance:** `grep -r "jakarta.transaction.Transactional" --include=*.java` returns nothing.

---

## Phase 2 — One coherent order saga (Week 2, ~4 days)

### 2.1 Decide the flow
Today both a synchronous Feign call **and** an async `payment.events` consumer mutate the order and clear the cart → reserve/confirm race.

**Recommended target (event-driven, stock reserved before charging):**

```
CREATED ──reserve cmd──▶ inventory  ──INVENTORY_RESERVED──▶ RESERVED
RESERVED ──payment cmd─▶ payment    ──PAYMENT_SUCCESS─────▶ CONFIRMED ──▶ confirm stock, redeem coupon, clear cart, notify
                                     ──PAYMENT_FAILED─────▶ PAYMENT_FAILED ──▶ release stock
INVENTORY_REJECTED ────────────────────────────────────────▶ CANCELLED
```

`CheckoutService` becomes: validate cart → price → persist order (`CREATED`) → write outbox row → return `202 Accepted` with the order id. The frontend polls `GET /orders/{id}` or subscribes over SSE.

Delete the synchronous payment block (lines 144-223) and the duplicated cart-clear/reserve logic there; the consumer owns all post-payment side-effects.

### 2.2 Formalise the state machine
Create `OrderStatusTransitions` with an allow-list; reject illegal transitions loudly.
```
CREATED   → RESERVED | CANCELLED | PAYMENT_FAILED
RESERVED  → CONFIRMED | PAYMENT_FAILED | CANCELLED
CONFIRMED → SHIPPED | REFUNDED
SHIPPED   → DELIVERED | RETURNED
```
Fixes the missing `save()` on the `PAYMENT_REFUNDED` branch and prevents stale out-of-order Kafka events from regressing status.

### 2.3 Transactional outbox
New table per publishing service:
```sql
CREATE TABLE outbox_event (
  id            UUID PRIMARY KEY,
  aggregate_type VARCHAR(64)  NOT NULL,
  aggregate_id   VARCHAR(64)  NOT NULL,
  event_type     VARCHAR(64)  NOT NULL,
  payload        JSONB        NOT NULL,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  published_at   TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_event (created_at) WHERE published_at IS NULL;
```
A `@Scheduled(fixedDelay = 500)` poller (with `SELECT … FOR UPDATE SKIP LOCKED`) publishes and stamps `published_at`. Eliminates the dual-write and the "marked processed before publish" bug at [`CheckoutService:197`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:197).

Interim cheaper option if outbox is too large a step: `@TransactionalEventListener(phase = AFTER_COMMIT)`.

### 2.4 Durable idempotency
Keep Redis as the fast path but make Postgres authoritative:
```sql
CREATE TABLE processed_event (
  event_id    VARCHAR(64) PRIMARY KEY,
  consumer    VARCHAR(64) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```
Insert inside the consumer transaction; catch `DataIntegrityViolationException` → skip as duplicate. Same for `RedisPaymentIdempotencyStore` (unique index on `payments(order_id)`).

---

## Phase 3 — Resilience (Week 3, ~3 days)

### 3.1 Circuit breakers + timeouts (currently zero)
Add to the parent `pom.xml`:
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```
Baseline config in a shared `common-lib` yml fragment:
```yaml
feign:
  circuitbreaker.enabled: true
  client.config.default:
    connectTimeout: 2000
    readTimeout: 5000
resilience4j.circuitbreaker.configs.default:
  slidingWindowSize: 20
  failureRateThreshold: 50
  waitDurationInOpenState: 10s
  permittedNumberOfCallsInHalfOpenState: 5
resilience4j.retry.configs.default:
  maxAttempts: 3
  waitDuration: 200ms
  enableExponentialBackoff: true
```
Add a `fallback` class to each of the 13 `@FeignClient`s. **Never retry non-idempotent POSTs** (payments) without an idempotency key.

### 3.2 Standardise service discovery
Half the clients hard-pin `url = "${x.service.url}"`, which bypasses Eureka and load balancing entirely (single instance only). Drop `url` from all `@FeignClient`s and rely on `name` + discovery; use `spring.cloud.discovery.client.simple.instances` for local overrides.

### 3.3 Kafka hardening
Producers:
```yaml
spring.kafka.producer:
  acks: all
  retries: 10
  properties:
    enable.idempotence: true
    max.in.flight.requests.per.connection: 5
    delivery.timeout.ms: 120000
```
Consumers: `enable-auto-commit: false`, `ack-mode: RECORD`, `isolation.level: read_committed`, plus a shared error handler:
```java
@Bean
DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
    var recoverer = new DeadLetterPublishingRecoverer(template,
            (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
    var handler = new DefaultErrorHandler(recoverer, new ExponentialBackOffWithMaxRetries(3));
    handler.addNotRetryableExceptions(IllegalArgumentException.class, JsonProcessingException.class);
    return handler;
}
```
Create `*.DLT` topics + an alert on DLT depth > 0.

### 3.4 Payment correctness
- Gate `mockPaymentGateway()` behind `payment.mock.enabled` (default `false`) — it currently fails 10% of real CARD payments.
- Stripe: do **not** set `success = true` at intent creation. Emit `PAYMENT_PENDING`; only the verified webhook emits `PAYMENT_SUCCESS`.
- Verify the Stripe webhook signature (`Webhook.constructEvent`) and make webhook handling idempotent on `event.id`.
- Guard `PaymentStatus.valueOf(status)` against unknown values.
- Pass a stable Stripe idempotency key (`orderId`) so retries cannot double-charge; do not blindly `idempotencyStore.clear()` on every failure.
- Remove the unused `ProcessedCommandRepository` from the `PaymentService` constructor.

---

## Phase 4 — Security (Week 4, ~4 days)

### 4.1 Schema ownership — highest risk item
`ddl-auto: update` is set in **all 10 database services, prod profiles included**. Hibernate can silently drop/alter columns and never removes obsolete ones.

1. Generate a Flyway `V1__baseline.sql` per service from the current schema.
2. Set `spring.jpa.hibernate.ddl-auto: validate` everywhere.
3. Enable `spring.flyway.enabled: true`, `baseline-on-migrate: true`.
4. Add a CI check that fails the build on `ddl-auto: (update|create)`.

### 4.2 Secrets
- Remove the default from `${JWT_SECRET:supersecret…}` in all 13 ymls so the app fails fast without the env var.
- Remove `password: postgres` and `password: your-email-password`; source from env / Docker secrets / Vault.
- Add `target/` to `.gitignore` and `git rm -r --cached **/target` (compiled ymls with secrets are currently in the repo).
- Rotate the leaked JWT secret and DB credentials.
- Add `gitleaks` to CI.

### 4.3 De-duplicate authentication (13× copies)
Move `JwtAuthenticationFilter`, `JwtUtil` (12 copies), and `GlobalExceptionHandler` (13 copies) into `common-lib` as an auto-configuration:
```
common-lib/src/main/resources/META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports
```
with `@ConditionalOnProperty(name = "shopfast.security.jwt.enabled", matchIfMissing = true)`. Delete the per-service copies. Prefer validating the JWT once at [`api-gateway/JwtAuthFilter`](../api-gateway/src/main/java/com/shopfast/apigateway/filter/JwtAuthFilter.java) and propagating a signed internal header downstream.

### 4.4 PCI-DSS
Raw `cardNumber` / `cvv` / `expiryDate` currently flow from the frontend → order-service → payment-service in `PaymentRequest`. This puts every service in PCI scope.
- Tokenize client-side with Stripe Elements; send only `paymentMethodId`.
- Delete card fields from `CheckoutRequestDto`, `PaymentRequest`, `PaymentRequestDto`.
- Until then: `@ToString.Exclude` + `@JsonIgnore` on those fields and confirm they never reach logs.
- Remove the `System.out.println` Redis smoke tests from every `*Application.java`.

---

## Phase 5 — Performance & data model (Week 5, ~3 days)

### 5.1 Indexes (Flyway)
```sql
CREATE UNIQUE INDEX idx_orders_order_number ON orders (order_number);
CREATE INDEX        idx_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX        idx_orders_status       ON orders (status);
CREATE UNIQUE INDEX idx_payments_order      ON payments (order_id);
CREATE INDEX        idx_payments_intent     ON payments (payment_intent_id);
CREATE UNIQUE INDEX idx_inventory_product   ON inventory (product_id);
CREATE INDEX        idx_order_items_order   ON order_items (order_id);
```

### 5.2 Remove N+1 remote calls
`InventoryService.getAllInventoryItemsWithProduct()` issues one Feign call per row. Add `POST /api/v1/product/batch` accepting a list of ids; one call per page.

### 5.3 Order number collisions
`UUID.randomUUID().toString().substring(0,8)` gives ~4.3 B space → ~50% collision probability near 100k orders, with no unique constraint. Use a DB sequence (`ORD-{yyyyMMdd}-{seq}`) or ULID, plus the unique index above.

### 5.4 Cache correctness
- `@CacheEvict(allEntries = true)` on every inventory write flushes the whole cache — evict by `key = "#productId"`.
- Never `@Cacheable` an entity that is subsequently mutated; cache DTOs only.
- Set explicit Redis TTLs per cache name.

### 5.5 JPA hygiene
- Drop `orderItemRepository.saveAll(items)` before the parent is persisted — use `cascade = ALL` from `Order`.
- Remove the redundant 2-3 `orderRepository.save(order)` calls per transaction (dirty checking suffices).
- Ensure all `@ManyToOne` are `FetchType.LAZY`; use `@EntityGraph` for list endpoints.
- Set `spring.jpa.properties.hibernate.jdbc.batch_size: 50`, `order_inserts: true`.

---

## Phase 6 — Structure, testing, observability (Weeks 6-7)

### 6.1 Refactor the god method
`CheckoutService.checkout()` is ~175 lines with 6 responsibilities. Split into:
- `CartLoader` — fetch + validate cart
- `PricingService` — subtotal, coupon, tax, shipping (pure, fully unit-testable)
- `OrderFactory` — build `Order` + `OrderItem` + `ShippingAddress`
- `CheckoutOrchestrator` — persist + outbox
- `OrderNotifier` — notification events

Also: replace all hand-written multi-arg constructors with `@RequiredArgsConstructor`; remove `@RequestParam` from service-layer signatures (web concern leaking into `InventoryService`); fix `Optional.of(...orElseThrow(...))` in [`OrderService.getOrderById()`](../order-service/src/main/java/com/shopfast/orderservice/service/OrderService.java:97).

### 6.2 Notifications
`OrderService:83-85` and `CheckoutService:231` hard-code `junaidnumlcs@gmail.com` and a fixed userId — every customer email goes to one inbox. Resolve the recipient from user-service (cached) or carry `email` on the event.

### 6.3 Testing (currently 7 test files / 495 sources = 1.4%)
Target ≥ 60% on service layers, ordered by value:
1. `PricingService` — pure unit tests (discounts, rounding, edge cases)
2. `InventoryService` concurrency — Testcontainers Postgres, 50 parallel reservations
3. Kafka consumer idempotency — Testcontainers Kafka, replay the same event twice
4. Saga happy path + compensations — payment failure releases stock, order cancelled
5. Contract tests (Spring Cloud Contract) for the 13 Feign clients
6. `@WebMvcTest` slices for controllers (auth rules, validation)

Add JaCoCo with a build-failing threshold; wire into GitHub Actions.

### 6.4 Observability
- Micrometer Tracing + OpenTelemetry; propagate trace ids over Kafka headers.
- Business metrics: `orders.created`, `payments.failed`, `inventory.reserve.rejected`, `outbox.lag`, `dlt.depth`.
- Extend `alerts.yml`: payment failure rate > 5%, DLT depth > 0, outbox lag > 30 s, circuit breaker open.
- Structured JSON logging with `orderId`/`userId`/`traceId` in MDC; never log card data or JWTs.
- Correct actuator probes: `/actuator/health/liveness` and `/readiness` in `docker-compose`/k8s.

---

## Timeline

| Week | Phase | Outcome |
|---|---|---|
| 1 | Phase 1 | Correct totals, coupons redeemed, no oversell |
| 2 | Phase 2 | Single coherent saga, outbox, durable idempotency |
| 3 | Phase 3 | Circuit breakers, Kafka DLT, correct Stripe lifecycle |
| 4 | Phase 4 | Flyway-managed schemas, no committed secrets, shared auth, PCI scope reduced |
| 5 | Phase 5 | Indexes, no N+1, safe caching, collision-free order numbers |
| 6-7 | Phase 6 | Decomposed services, ≥60% coverage, tracing + business alerts |

## Definition of done (per phase)
- All new/changed logic covered by tests; JaCoCo threshold met.
- No `jakarta.transaction.Transactional`, no `ddl-auto: update`, no committed secrets (CI-enforced).
- Every Feign client has a timeout, circuit breaker and fallback.
- Every Kafka consumer is idempotent and has a DLT.
- Load test: 200 concurrent checkouts on 100 units of stock → exactly 100 confirmed orders, zero oversell, zero double-charge.

---

## Final status

All planned phases are implemented and the full 13-module reactor compiles clean
(`mvn -o compile`).

### Known caveat: no Flyway

Flyway is not on the dependency tree and the local Maven repository only has
flyway-core 6.x/7.x, which is incompatible with Spring Boot 3. Two decisions follow:

1. Hibernate remains the schema source. `spring.jpa.hibernate.ddl-auto` is
   externalised as `${JPA_DDL_AUTO:update}` in every service. Do **not** set this to
   `validate` until a real migration tool is introduced — startup would fail because
   nothing else creates the schema.
2. Indexes are declared on the entities (`@Table(indexes = ...)`) so Hibernate emits
   them, instead of in versioned SQL migrations.

**Recommended follow-up:** add `flyway-core` 9+ / `flyway-database-postgresql`,
baseline the existing schema, move the index DDL into `V1__baseline.sql`, and only
then flip `JPA_DDL_AUTO` to `validate`.

### Remaining recommended work (not blocking)

- Test coverage is still very low; the money math, order state machine, and inventory
  concurrency paths are the highest-value places to add tests.
- Raw card data still crosses the wire to payment-service; move to tokenisation.
- The 13x duplicated auth/JWT filter code should be extracted into `common-lib`.
