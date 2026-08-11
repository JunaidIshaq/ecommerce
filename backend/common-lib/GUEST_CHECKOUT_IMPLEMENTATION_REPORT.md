# Guest User & Checkout Flow — Implementation Report

## 1. Overview

ShopFast lets a shopper browse the catalogue, fill a basket, and pay **without
being forced to create an account first**. The same set of URLs serves both
signed-in users and anonymous guests; who a request belongs to is decided
**server-side** from what the request actually proves (a verified Keycloak JWT
when there is one, otherwise a browser-generated anonymous id), never from a
client-supplied identity header.

This document describes:

- How a **guest** is identified and how that differs from a signed-in user.
- How **authentication** works (Keycloak + Spring Security resource servers).
- The **cart** flow for guests and users, and the guest→user **merge** on login.
- The **checkout** flow, including guest **capability tokens** for order access.
- The **security boundaries** that keep one shopper from acting as another.

---

## 2. Identity Model

There are exactly two kinds of shopper, captured as small value objects:

- Signed-in user → keyed on the **Keycloak subject (`sub`)**, an immutable user id.
- Guest → keyed on a **client-generated UUID** (`anon_cart_id`) stored in the
  browser's `localStorage` and sent as the `X-Anon-Id` header.

The distinction is modelled explicitly so a guest id can never be mistaken for a
real account:

- Cart side: [`CartIdentity`](../cart-service/src/main/java/com/shopfast/cartservice/web/CartIdentity.java:14)
  — `record CartIdentity(String id, boolean guest)`.
- Order side: [`OrderIdentity`](../order-service/src/main/java/com/shopfast/orderservice/web/OrderIdentity.java:9)
  — `record OrderIdentity(String id, boolean guest)`.

**The security-relevant rule:** a verified token always wins over a
client-supplied anonymous id. See
[`CartIdentity.isAuthenticated()`](../cart-service/src/main/java/com/shopfast/cartservice/web/CartIdentity.java:32).
If a header could override a verified `sub`, an authenticated user could read or
edit another shopper's guest cart just by sending an id, and a signed-in user
could silently write into an anonymous cart they don't own.

---

## 3. Authentication: Keycloak + Spring Security

### 3.1 Token issuance

Access tokens are issued by **Keycloak** (RS256, verified against the realm's
public JWKS). The principal is the Keycloak `sub`; roles are read from Keycloak's
non-standard claim structure.

### 3.2 Role mapping

Keycloak does not place roles in the standard `scope` claim. Realm roles live
under `realm_access.roles` and per-client roles under
`resource_access.<clientId>.roles`.
[`KeycloakRealmRoleConverter`](src/main/java/com/shopfast/common/security/KeycloakRealmRoleConverter.java:28)
maps both into Spring `GrantedAuthority` values. Without it every request would be
authenticated but carry zero authorities, so every `@PreAuthorize` would fail
closed with 403.

### 3.3 Two layers of enforcement

**Layer 1 — API Gateway (reactive, WebFlux):**
[`GatewaySecurityConfig`](../api-gateway/src/main/java/com/shopfast/apigateway/security/GatewaySecurityConfig.java:43)
is the outer boundary. It:

- Permits anonymous **storefront reads** (`/api/v1/product/**`,
  `/api/v1/category/**` on GET).
- Permits the **cart** paths (`/api/v1/cart`, `/api/v1/cart/items/**`) so guests
  can add to cart without a token — while keeping `/api/v1/cart/merge` and
  `/api/v1/cart/internal` **authenticated**.
- Permits **guest checkout** (`POST /api/v1/order/checkout`) and the
  single-order read (`GET /api/v1/order/*`) — but never the order-history list.
- Gates admin surfaces on **roles** (`hasAnyRole("ADMIN", "SUPER_ADMIN")`), not
  merely on being logged in.
- Verifies the JWT via a `ReactiveJwtDecoder` built from `keycloak.issuer-uri`
  (with a `DualReactiveJwtDecoder` fallback during the legacy HS256 migration
  window).

**Layer 2 — Each service (servlet stack):**
[`ResourceServerAutoConfiguration`](src/main/java/com/shopfast/common/security/ResourceServerAutoConfiguration.java:47)
in `common-lib` gives every service the **same** resource-server chain:
stateless sessions, CSRF disabled (bearer-token APIs), `default-deny`
(`anyRequest().authenticated()`), JWKS-based decoding with issuer + optional
audience validation. Public/guest paths are opened per-service via the
`shopfast.security.public-paths` property rather than blanket `permitAll`.

This is **defence in depth**: the gateway is the first check, but a request that
reaches a pod directly is still validated independently.

### 3.4 Identity header injection & scrubbing

[`IdentityHeaderFilter`](../api-gateway/src/main/java/com/shopfast/apigateway/filter/IdentityHeaderFilter.java:54)
runs at the gateway and, from the **verified token**, injects identity headers
(`X-User-Id`, `userId`, `X-User-Email`, `X-User-Roles`, …) for downstream
services. Critically, it **unconditionally strips any inbound copies of those
headers first**, so a client can never smuggle `X-User-Id: <somebody-else>` past
the gateway. Anonymous requests simply arrive with those headers absent.

> Note: header injection is a convenience/defence-in-depth mechanism. New code
> prefers reading identity from the token via the resolvers below.

---

## 4. Cart Flow

### 4.1 Storage

Carts live in **Redis**, keyed by prefix in
[`CartService`](../cart-service/src/main/java/com/shopfast/cartservice/service/CartService.java:22):

- User cart: `cart:<userId>`
- Guest cart: `cart:guest:<anonId>` — with a **TTL** (default 14 days) refreshed
  on access, so abandoned guest baskets expire.

### 4.2 One controller, both shopper types

[`CartController`](../cart-service/src/main/java/com/shopfast/cartservice/controller/CartController.java:42)
exposes a single `/api/v1/cart` surface. For every operation it calls
[`CartIdentityResolver.resolve()`](../cart-service/src/main/java/com/shopfast/cartservice/web/CartIdentityResolver.java:34)
to decide user vs. guest, then branches to the `...User` or `...Guest` service
method.

The resolver:

1. If the request carries a **verified token**, returns `CartIdentity.ofUser(sub)`.
2. Otherwise reads `X-Anon-Id` (or the legacy `anonId` query param) and returns
   `CartIdentity.ofGuest(uuid)`.
3. If neither is present → **400 Bad Request** (it will not invent a server-side
   id, which the client could never address again — the basket would silently
   empty between page loads).

The anonymous id is validated to be a **UUID** because it becomes part of a Redis
key; free text like `../` or a delimiter could be shaped to collide with another
cart's key.

**Why this design:** the cart used to have two parallel controllers
(`/cart/**` and `/cart/guest/**`) and the client picked the URL. A stale token
in browser storage made the client believe it was logged in, so it hit the
authenticated URL and every add-to-cart returned **401**. Deciding ownership
server-side removes that choice from the client.

### 4.3 Frontend behaviour

[`CartService`](../../frontend/src/app/services/cart.service.ts:17) (Angular):

- [`getOrCreateAnonId()`](../../frontend/src/app/services/cart.service.ts:83)
  lazily creates and persists the `anon_cart_id` UUID.
- [`cartHeaders()`](../../frontend/src/app/services/cart.service.ts:152) sends
  `X-Anon-Id` **unconditionally**. The server prefers the verified token, so the
  header is ignored for a real session — but sending it always means that a token
  the server rejects still falls back to the guest cart instead of leaving the
  request with no identity.
- [`isLoggedIn()`](../../frontend/src/app/services/cart.service.ts:50) treats
  anything it cannot positively verify as a live session (readable, unexpired
  `exp`) as a **guest**, because the guest path always works.

### 4.4 Guest → User cart merge on login

When a guest signs in,
[`onLoginSuccess()`](../../frontend/src/app/services/cart.service.ts:192) posts
the `anonId` to `/api/v1/cart/merge` (an **authenticated** endpoint), then clears
the local `anon_cart_id`. Server-side,
[`CartService.mergeGuestIntoUser()`](../cart-service/src/main/java/com/shopfast/cartservice/service/CartService.java:254)
folds the guest hash into the user's cart:

- Duplicate products have quantities **summed** (capped at 999).
- The **user's** price/title snapshot is kept for duplicates.
- The guest cart is **deleted** after merge.

---

## 5. Checkout Flow

### 5.1 Entry point

[`CheckoutController.checkout()`](../order-service/src/main/java/com/shopfast/orderservice/controller/CheckoutController.java:40)
handles `POST /api/v1/order/checkout`. It:

1. Resolves the buyer via
   [`OrderIdentityResolver.resolve()`](../order-service/src/main/java/com/shopfast/orderservice/web/OrderIdentityResolver.java:32)
   — token first, `X-Anon-Id` (validated UUID) otherwise, else **400**.
2. Delegates to
   [`CheckoutService.checkout()`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:71).
3. For a **guest**, adds the one-time `accessToken` to the response.

The buyer previously came from a client-supplied `X-User-Id` header, which meant
anyone could place — and later read — an order in another shopper's name just by
editing a header. That is now impossible.

### 5.2 Order orchestration

[`CheckoutService`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:55)
runs a single `@Transactional` flow:

1. **Load cart** via
   [`RemoteGateway.getCart()`](../order-service/src/main/java/com/shopfast/orderservice/client/RemoteGateway.java:51),
   which forwards the correct header — `X-User-Id` for a user, `X-Anon-Id` for a
   guest (see [`CartClient`](../order-service/src/main/java/com/shopfast/orderservice/client/CartClient.java:17)).
   Getting this wrong reads an empty cart and fails with "Cart is empty". Empty
   cart → `IllegalArgumentException`.
2. **Price** the order in `BigDecimal` (2 dp, HALF_UP) — never `double`.
3. **Coupons apply to signed-in users only.** A guest gets no discount, because
   coupons are validated per account (usage limits, eligibility); a shared guest
   id would otherwise give unlimited redemptions across browsers.
4. **Build & persist** the order via
   [`buildOrder()`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:158):
   sets `userId`, the `guest` flag, order number, addresses, items. For a guest
   it also mints a **capability token** (`newAccessToken()`, 256 CSPRNG bits,
   URL-safe).
5. **Take payment** via
   [`processPayment()`](../order-service/src/main/java/com/shopfast/orderservice/service/CheckoutService.java:224):
   - **COD** → order stays `CREATED`/`PENDING`; stock reserved.
   - **CARD SUCCESS** → `CONFIRMED`; stock reserved.
   - **CARD FAILED** → `PAYMENT_FAILED`.
   - Otherwise (e.g. Stripe intent) → `PENDING`, settled later by webhook.
6. **Reserve stock** by publishing an idempotent `RESERVE` Kafka command (the
   idempotency marker is written in the **same transaction**, so a rollback can't
   leave a "processed" record behind).
7. **Publish** an order notification event.

Post-payment side effects (cart clearing, coupon redemption, stock confirmation)
are owned exclusively by the Kafka payment consumer, so they happen exactly once.

### 5.3 Frontend checkout

[`CheckoutComponent.placeOrder()`](../../frontend/src/app/pages/checkout/checkout.component.ts:81)
has **no sign-in gate** — a guest who filled a basket can pay. It builds the
address/payment body and calls
[`CartService.checkoutWithPayment()`](../../frontend/src/app/services/cart.service.ts:127),
which posts with the guest `X-Anon-Id` header and captures any returned token via
[`rememberOrderToken()`](../../frontend/src/app/services/cart.service.ts:136).

---

## 6. Guest Order Access (Capability Tokens)

A guest has no JWT, so nothing about a later request proves an order is theirs.
The order id alone is **not** an access control (UUIDs are "hard to guess", not
secret). So checkout hands back a one-time **capability token**.

- Stored on the order:
  [`Order.accessToken`](../order-service/src/main/java/com/shopfast/orderservice/model/Order.java:81)
  — 256 random bits, never returned when listing orders.
- Kept on the client per order id (so placing a second order doesn't lock the
  shopper out of the first):
  [`guest-order-tokens.ts`](../../frontend/src/app/utils/guest-order-tokens.ts:25).
- Presented as the `X-Order-Token` header when reopening the order.

Access is enforced in
[`OrderController.getOrder()`](../order-service/src/main/java/com/shopfast/orderservice/controller/OrderController.java:102)
via [`canRead()`](../order-service/src/main/java/com/shopfast/orderservice/controller/OrderController.java:118):
readable by the **owner** (token `sub` matches `userId`), **staff/admin**, or a
guest presenting the **matching capability token**. The token comparison is
**constant-time** ([`matchesAccessToken()`](../order-service/src/main/java/com/shopfast/orderservice/controller/OrderController.java:132))
to prevent timing attacks. A mismatch returns **404, not 403**, so an attacker
cannot use the response to confirm a valid order id and enumerate ids.

---

## 7. End-to-End Sequences

### 7.1 Guest journey

```
Browser (no login)
  └─ generate anon_cart_id (UUID) → localStorage
  └─ POST /api/v1/cart/items          [X-Anon-Id]
       Gateway: permit (public cart path), strip identity headers
       cart-service: resolve → guest → Redis key cart:guest:<anonId> (TTL)
  └─ POST /api/v1/order/checkout      [X-Anon-Id]
       Gateway: permit (public order POST), strip identity headers
       order-service: resolve → guest
         load guest cart, price (no coupon), persist order (guest=true),
         mint accessToken, take payment, reserve stock
       ← returns order + accessToken
  └─ store accessToken per orderId → localStorage
  └─ GET /api/v1/order/<id>           [X-Order-Token]
       order-service: canRead → guest token matches (constant-time) → 200
```

### 7.2 Signed-in journey

```
Browser (Keycloak JWT)
  └─ POST /api/v1/cart/items          [Authorization: Bearer <jwt>] (+X-Anon-Id, ignored)
       Gateway: verify JWT, inject X-User-Id from sub, strip inbound copies
       cart-service: resolve → user → Redis key cart:<sub>
  └─ (on login) POST /api/v1/cart/merge  → guest cart folded into user cart
  └─ POST /api/v1/order/checkout      [Bearer]
       order-service: resolve → user; coupons allowed; order guest=false (no token)
  └─ GET /api/v1/order                (history) — authenticated, owner-scoped
```

---

## 8. Security Properties Summary

| Concern | Mechanism |
|---|---|
| Token verification | Keycloak RS256 via JWKS, issuer (+ optional audience) validated at gateway **and** each service |
| Role mapping | [`KeycloakRealmRoleConverter`](src/main/java/com/shopfast/common/security/KeycloakRealmRoleConverter.java:28) reads `realm_access` / `resource_access` |
| Identity spoofing | Inbound identity headers stripped unconditionally at the gateway; identity derived from token, not headers |
| Guest vs. user | Token always wins over `X-Anon-Id`; `guest` flag persisted on the order |
| Guest id safety | `X-Anon-Id` must be a UUID (used in Redis keys) |
| Guest order access | Per-order 256-bit capability token, constant-time compare, 404 on mismatch |
| Coupon abuse | Discounts only for authenticated buyers |
| Default posture | `default-deny`; public/guest paths are explicit, enumerated exceptions |
| Idempotency | Stock `RESERVE` command + processed marker in the same DB transaction |

---

## 9. Notable Design Trade-offs

- **Guest baskets expire** (Redis TTL) — intentional cleanup of abandoned carts;
  a guest returning after the TTL starts fresh.
- **Guests forfeit coupons** — a deliberate anti-abuse choice.
- **Capability token lives in `localStorage`** — a guest who clears storage or
  switches browsers loses the ability to reopen the order; acceptable because the
  alternative (order id as the only credential) is not access control at all.
- **Legacy HS256 decoder** retained behind a config flag during the Keycloak
  migration; removed once all clients issue Keycloak tokens.
