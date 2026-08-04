# ShopFast Backend — Implementation Report

**Scope:** end-to-end review and remediation of all microservices under `ecommerce/backend`
**Modules touched:** 13 (order, payment, inventory, product, category, cart, coupon, review, admin, auth, user, notification, common-lib)
**Build status:** `mvn -o compile` — all modules compile clean
**Schema tooling:** Hibernate auto-DDL (Flyway unavailable — see [Known Constraints](#known-constraints))

---

## 1. Executive summary

The review found **12 correctness defects that affect money or stock**, **9 reliability
gaps**, **7 security issues**, and a set of performance problems dominated by N+1 remote
calls and incorrect cache invalidation.

The most serious findings were not crashes — they were silent. An inverted discount and a
hard-coded price floor produced wrong order totals with no error. Coupons were validated
and displayed but never redeemed. Two concurrent checkouts could both pass the stock
check and oversell. A malformed URL in the product client meant every order response
silently lost its product names and images. A Kafka consumer marked events as processed
before handling them, so any transient failure permanently dropped the event.

All of the above are fixed. The remaining open items are listed in
[Section 9](#9-remaining-work-not-implemented).

---

## 2. Correctness fixes (money and stock)

### 2.1 Checkout total calculation

**File:** [`CheckoutService.java`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:1)

Three defects in one method:

| Defect | Effect |
|---|---|
| `Math.max(9, subtotal)` price floor | Any order under $9 was silently charged $9 |
| Discount added instead of subtracted | Coupons *increased* the total |
| `double` arithmetic | Cent-level drift on multi-item orders |

**Fix:** the whole calculation moved to `BigDecimal` with an explicit scale and
`RoundingMode.HALF_UP` applied once at the end. The floor was deleted outright — it had no
business justification and no corresponding requirement. Discount is now subtracted and
clamped so a discount larger than the subtotal yields zero rather than a negative charge.

### 2.2 Coupon persistence and redemption

**Files:** [`CheckoutService.java`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:1), [`Order.java`](../order-service/src/main/java/com/shopfast/orderservice/model/Order.java:1), [`KafkaPaymentConsumer.java`](../order-service/src/main/java/com/shopfast/orderservice/events/KafkaPaymentConsumer.java:1)

A coupon was validated against coupon-service, its discount applied to the displayed
total, and then discarded. The order row stored neither the code nor the discount, and
the redemption call was never made. Consequences: a single-use coupon could be reused
indefinitely, refunds could not reconstruct the original discount, and finance reports
could not reconcile.

**Fix:** `couponCode` and `discount` are persisted on the order at creation time.
Redemption is invoked on payment success (the correct point — redeeming at validation
time would burn the coupon on abandoned checkouts). A double-brace initialisation idiom
in the same file was removed; it creates an anonymous subclass per call and leaks the
enclosing instance.

### 2.3 Inventory oversell

**Files:** [`InventoryService.java`](../inventory-service/src/main/java/com/shopfast/inventoryservice/service/InventoryService.java:1), [`InventoryRepository.java`](../inventory-service/src/main/java/com/shopfast/inventoryservice/repository/InventoryRepository.java:1)

The original reserve path was read → check → write. Under concurrency, two requests both
read `available = 1`, both pass the check, and both write. Classic lost update.

**Fix:** `reserve`, `release`, and `confirm` are each a single conditional `UPDATE` where
the availability predicate lives in the `WHERE` clause. The affected-row count is the
success signal:

```
UPDATE ... SET available = available - :qty, reserved = reserved + :qty
WHERE product_id = :id AND available >= :qty
```

A return of `0` means either the row is missing or there was insufficient stock; the code
distinguishes the two by re-loading, and throws `InsufficientStockException` with the
reason. A related hazard was also closed: the cached read method was being used inside
write transactions, which could feed a stale detached entity into a save and revert a
concurrent update. Write paths now use an uncached `loadForWrite` bound to the current
persistence context, and the cached method carries a Javadoc warning.

### 2.4 Order state machine

**Files:** `OrderStatusTransitions.java`, [`OrderService.java`](../order-service/src/main/java/com/shopfast/orderservice/service/OrderService.java:1)

Status changes were unguarded, so out-of-order Kafka delivery could move a `DELIVERED`
order back to `PENDING`. Separately, the `PAYMENT_REFUNDED` branch mutated the entity but
never called `save()`, so refunds were discarded on transaction commit.

**Fix:** an explicit transition allow-list rejects illegal moves, making the consumer
naturally tolerant of replays and reordering. The missing `save()` was added.

### 2.5 Transaction annotation

**Scope:** all services

Several classes imported `jakarta.transaction.Transactional` rather than Spring's. The
two have different rollback semantics, and mixing them means rollback rules do not behave
as the surrounding code assumes.

**Fix:** standardised on `org.springframework.transaction.annotation.Transactional`
throughout.

### 2.6 Broken product client URL

**File:** [`ProductClient.java`](../order-service/src/main/java/com/shopfast/orderservice/client/ProductClient.java:1)

The client appended the API prefix twice, producing
`/api/v1/product/api/v1/product/{id}`. Every call 404'd, the exception was swallowed, and
product enrichment returned null — so order history rendered without product names or
images, indefinitely, with nothing in the logs to indicate a fault.

**Fix:** corrected to a relative path, and a `fetchProductsByIds(List<UUID>)` batch method
was added (see [Section 5.1](#51-n1-remote-calls)).

---

## 3. Event-driven reliability

### 3.1 Idempotency ordering bug

**File:** [`KafkaInventoryConsumer.java`](../product-service/src/main/java/com/shopfast/productservice/events/KafkaInventoryConsumer.java:1)

The consumer marked an event as processed *before* invoking the handler. If the handler
threw, the listener retried, found the event already flagged, and returned early —
permanently dropping it. Product stock then diverged from inventory with no recovery path
and no DLT entry.

**Fix:** the mark now happens only after the handler succeeds. The dedupe check on entry
still short-circuits genuine duplicates. An unrelated `(int)` cast on a JSON-deserialised
numeric was also changed to `((Number) x).intValue()`, which was a latent
`ClassCastException` whenever Jackson produced a `Long`.

### 3.2 Producer and consumer hardening

**Scope:** 6 services

- Idempotent producers enabled (prevents duplicates on internal broker retries).
- `DefaultErrorHandler` with bounded backoff replaces the default infinite retry, which
  previously let one poison message block a partition forever.
- Dead-letter topics configured so unprocessable messages are quarantined and visible
  rather than discarded.

### 3.3 Circuit breakers and timeouts

**Files:** [`RemoteGateway.java`](../order-service/src/main/java/com/shopfast/orderservice/client/RemoteGateway.java:1), [`ProductGateway.java`](../cart-service/src/main/java/com/shopfast/cartservice/client/ProductGateway.java:1), plus config in 6 services

Feign timeouts were declared under a misspelled property prefix, so **no timeout was ever
in effect** — a hung downstream would hold a request thread until the socket died.
Corrected to `spring.cloud.openfeign`.

Resilience4j (`resilience4j-spring-boot3` + `spring-boot-starter-aop`) was then added to
the six Feign-consuming services, and remote calls were routed through gateway wrappers
carrying `@CircuitBreaker` and `@Retry`.

The important design decision here is the **failure policy split**, documented in the
gateway:

| Call | Policy | Rationale |
|---|---|---|
| Payment authorisation | Fail loudly (`RemoteServiceUnavailableException`) | Never assume success on a money operation |
| Coupon validation | Fail loudly | A silent fallback would grant an unvalidated discount |
| Cart clearing | Degrade quietly | Best-effort cleanup; a stale cart is a cosmetic issue |
| Product lookup (cart) | Degrade to `null` | Display enrichment only |

A generic "return empty on failure" fallback would have been actively dangerous on the
first two rows.

---

## 4. Cache correctness

Three independent staleness bugs, all of which served wrong data for the full TTL window.

### 4.1 Inventory paged views never invalidated

**File:** [`InventoryService.java`](../inventory-service/src/main/java/com/shopfast/inventoryservice/service/InventoryService.java:1)

Every mutation evicted `inventoryByProduct` but left the `inventory` and
`inventoryWithProduct` paged caches untouched — even though those payloads embed the stock
numbers. The list endpoints reported pre-mutation stock for 15 minutes.

**Fix:** all four mutating methods now evict the paged regions as well.

### 4.2 Async stock events never invalidated product caches

**File:** [`ProductService.java`](../product-service/src/main/java/com/shopfast/productservice/service/ProductService.java:1)

`updateStockAndAvailability` — the handler for inventory events — had no cache
annotations. The storefront's cached product and page payloads kept the old stock value
and the old in/out-of-stock flag.

**Fix:** added eviction of `product`, `productInternalSearch`, and `productsPage`.

### 4.3 Subcategory list stale on create

**File:** [`CategoryService.java`](../category-service/src/main/java/com/shopfast/categoryservice/service/CategoryService.java:1)

Creating a child category changes the parent's children list, which lives in a separate
`subcategories` region that create never evicted. Also removed a
`key = "#result.id"` eviction that was redundant alongside the `allEntries` eviction on
the same region in the same annotation.

### 4.4 TTLs

**File:** [`RedisConfig.java`](../inventory-service/src/main/java/com/shopfast/inventoryservice/config/RedisConfig.java:1)

The flat 15-minute default is inappropriate for stock. Per-region overrides added:
`inventoryByProduct` 30s, `inventory` and `inventoryWithProduct` 60s. Eviction-on-write
remains the primary mechanism; the TTL is the backstop for out-of-band changes.

---

## 5. Performance

### 5.1 N+1 remote calls

Two loops issuing one HTTP call per row were replaced with single batch calls:

- [`OrderController`](../order-service/src/main/java/com/shopfast/orderservice/controller/OrderController.java:1) and [`AdminOrderController`](../order-service/src/main/java/com/shopfast/orderservice/controller/AdminOrderController.java:1) — one call per line item became one `fetchProductsByIds` call per order.
- Inventory-with-product listing — one call per row became one batch call per page.

For a 20-item order this is 20 round trips reduced to 1.

### 5.2 JPA fetch behaviour

**File:** [`Order.java`](../order-service/src/main/java/com/shopfast/orderservice/model/Order.java:1)

`Order.items` is `EAGER`, which on a paged query means one extra `SELECT` per order.
`EAGER` was retained deliberately — the DTO mapping happens outside the transaction, so
switching to `LAZY` would introduce `LazyInitializationException` risk across several call
sites for no gain. Instead `@BatchSize(50)` collapses those N selects into one `IN()`
query per 50 orders, removing the N+1 without changing fetch semantics.

### 5.3 Indexes

Added declaratively via `@Table(indexes = ...)`:

| Table | Index |
|---|---|
| `orders` | `(user_id, created_at)`, `(status)` |
| `order_items` | `(order_id)` |
| `payments` | `(order_id)`, `(payment_intent_id)`, `(status)` |
| `notifications` | `(user_id, created_at)`, `(status, channel)` |

These match the actual query predicates (user order history sorted by date, admin status
filters, webhook lookup by payment intent).

### 5.4 Order number generation

Replaced a timestamp-based generator that collided under concurrent checkouts with a
collision-safe scheme.

---

## 6. Security and configuration

| Issue | Fix |
|---|---|
| JWT and gateway secrets committed as YAML defaults | Defaults stripped from all base `application.yml`; values must come from the environment |
| `target/` tracked | Added to `.gitignore` |
| Mock payment gateway selectable at runtime | Gated behind a property so it cannot activate in production |
| Stripe path reported success before confirmation | Success is now reported only after confirmation |
| Webhook `Status.valueOf` on unvalidated input | Guarded against unknown values (previously an unrecognised status threw and the webhook was retried indefinitely) |
| `System.out` Redis smoke-test runners on startup | Removed from all 11 applications |
| `ddl-auto` hard-coded | Externalised as `${JPA_DDL_AUTO:update}` in all 12 production profiles |

---

## 7. Known constraints

**Flyway could not be introduced.** It is not on the dependency tree, and the local Maven
repository contains only flyway-core 6.x/7.x, which is incompatible with Spring Boot 3
(requires 9+). Two consequences:

1. **Hibernate remains the schema source.** `JPA_DDL_AUTO` defaults to `update`.
   Setting it to `validate` would fail startup, because nothing else creates the schema.
   An earlier iteration of this work set `validate` and it was caught and reverted.
2. **Indexes are declared on entities** rather than in versioned SQL.

**Recommended path forward:** add `flyway-core` 9+ and `flyway-database-postgresql`,
baseline the existing schema, move the index DDL into `V1__baseline.sql`, and only then
flip `JPA_DDL_AUTO` to `validate`.

---

## 8. Verification

- Each module was compiled after every change batch.
- Final full reactor build: `mvn -o compile` across all 13 modules — clean.
- Build requires `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (release 17).

**Verification gap worth stating plainly:** this is compile-level verification only.
Test coverage was ~1.4% before this work and no tests were added, so the behavioural
changes — particularly the money math and the inventory concurrency path — are argued
correct by inspection rather than demonstrated by test. That is the single largest
outstanding risk.

---

## 9. Remaining work (not implemented)

Ordered by value:

1. **Tests.** Highest priority. The three areas that most need them: checkout money math
   (discount clamping, rounding, zero-total edge cases), the order state machine
   (illegal transitions, replayed events), and inventory concurrency (parallel reserves
   against the last unit).
2. **Card data handling.** Raw card details still cross the wire to payment-service. This
   should move to gateway-side tokenisation; as it stands the PCI scope covers far more
   of the system than it needs to.
3. **Duplicated auth code.** The JWT filter is copy-pasted across 13 services. It belongs
   in `common-lib` as an auto-configuration — right now a security fix requires 13
   identical edits, and they will drift.
4. **Flyway migration** as described in Section 7.
5. **Saga compensation review.** The compensating actions were made idempotent, but the
   overall saga has not been tested against partial-failure scenarios end to end.
