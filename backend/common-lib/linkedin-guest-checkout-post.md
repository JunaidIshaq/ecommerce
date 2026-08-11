# LinkedIn Post — Guest Checkout, Keycloak, Spring Security & Redis

---

## Version 1 — The Main Post (recommended)

**Your checkout shouldn't start with a login wall.**

Roughly 1 in 4 shoppers abandon a cart when forced to create an account. So on ShopFast I built guest checkout properly — anonymous browsing, anonymous cart, anonymous payment — on top of Keycloak, Spring Security and Redis.

Here's what I learned. 🧵

---

**❌ The first design was wrong**

I had two sets of endpoints:

```
/api/v1/cart/**         → signed-in users
/api/v1/cart/guest/**   → everyone else
```

The client picked the URL. Which meant the client had to know whether it was logged in.

It didn't. A stale JWT sitting in localStorage made the frontend confidently call the authenticated route — and every single add-to-cart came back **401**.

The bug wasn't the stale token. The bug was **asking the client a question only the server can answer.**

---

**✅ The fix: one URL, server-side identity resolution**

```java
public CartIdentity resolve(Authentication auth, HttpServletRequest request) {
    if (CartIdentity.isAuthenticated(auth)) {
        return CartIdentity.ofUser(auth.getName());   // Keycloak "sub"
    }
    String anonId = request.getHeader("X-Anon-Id");
    if (anonId == null) throw new ResponseStatusException(BAD_REQUEST, "...");
    return CartIdentity.ofGuest(requireUuid(anonId));
}
```

One endpoint. `POST /api/v1/cart/items`. Works signed in, works as a guest.

Three details that matter far more than they look:

**1️⃣ The token ALWAYS wins over the header.**
Reverse that ordering and any authenticated user can read or edit a stranger's cart just by sending their anon id. Ordering *is* the security control here.

**2️⃣ The anon id must be a UUID.**
It becomes part of a Redis key. Accept free text and an id containing `:` or `../` can be shaped to collide with someone else's basket. Validate at the boundary.

**3️⃣ No identity → 400, never a server-generated id.**
Inventing an id gives the client a cart it can't address on the next request. The basket would silently empty between page loads. Fail loudly.

---

**🔐 Keycloak + Spring Security: two layers, not one**

**Layer 1 — API Gateway (WebFlux)**

The gateway opens exactly what guests need and nothing more:

```java
.pathMatchers(AUTHENTICATED_CART_PATHS).authenticated()  // /merge, /internal
.pathMatchers(PUBLIC_CART_PATHS).permitAll()
.pathMatchers(POST, "/api/v1/order/checkout").permitAll()
.pathMatchers(GET,  "/api/v1/order/*").permitAll()
.anyExchange().authenticated()
```

Note the ordering again: `/cart/merge` and `/cart/internal` are claimed *before* the cart prefix. Merge folds a guest basket into a named account — open that and anyone can write into your cart.

Also note: `GET /api/v1/order/*` is public, but `GET /api/v1/order` (full history) is **not**. One character of difference, entirely different blast radius.

**Layer 2 — Every service validates independently**

A shared auto-configuration in `common-lib` gives all services the same resource-server chain — stateless, CSRF off, JWKS verification, issuer + audience validated, `anyRequest().authenticated()` as the default.

Duplicating this per service is how services drift apart. One forgets the role converter, another leaves `/actuator/env` open. Centralise it, and a security fix ships everywhere in one release.

**The Keycloak gotcha nobody warns you about:**
Keycloak does NOT put roles in the `scope` claim Spring reads by default. They live in `realm_access.roles` and `resource_access.<client>.roles`. Without a custom converter every request arrives authenticated with **zero authorities** — so every `@PreAuthorize` fails with a baffling 403.

---

**🛡️ Headers are just text a client can type**

The gateway *injects* `X-User-Id` from the verified token for downstream services. But first it **unconditionally strips every inbound copy**:

```java
IDENTITY_HEADERS = { "userId", "user_id", "X-User-Id",
                     "X-User-Email", "X-User-Roles", "X-User-Name" }
```

Because before this, checkout read the buyer from a client-supplied `X-User-Id`. Anyone could place — and then read — an order in someone else's name by editing one header. That's not a theoretical horizontal privilege escalation. That's a one-line curl.

---

**⚡ Redis: where guest carts actually live**

```
cart:<keycloak-sub>     → signed-in user
cart:guest:<anon-uuid>  → guest, with a 14-day TTL refreshed on access
```

Redis Hashes, product id as the field. Three reasons it fits:

• **TTL is free garbage collection.** Guest carts are inherently abandoned traffic. A relational table would need a cleanup job; Redis expires them for me.
• **O(1) field updates.** Changing one item's quantity doesn't rewrite the basket.
• **The prefix IS the isolation boundary.** Guest and user carts can never collide — which is exactly why that UUID validation isn't optional.

**Merge on login** sums quantities for duplicate products (capped at 999), keeps the user's price snapshot, then deletes the guest key. Sign in, and your basket follows you.

---

**🎟️ The hardest part: letting a guest view their own order**

A guest has no JWT. So after checkout, what proves the order is theirs?

Not the order id. UUIDs are *hard to guess* — that is not access control.

So checkout mints a **capability token**: 256 bits from a CSPRNG, stored on the order, returned exactly once, never included when listing orders.

```java
private boolean canRead(Order order, String userId, String orderToken) {
    if (userId != null && userId.equals(order.getUserId())) return true;
    if (order.isGuest() && matchesAccessToken(order, orderToken)) return true;
    return isAdmin();
}
```

Two subtleties:

**Constant-time comparison** (`MessageDigest.isEqual`) — a naive `.equals()` can be timed character by character to recover a valid token.

**404 on mismatch, not 403** — "Forbidden" confirms the id belongs to a real order. That's an enumeration oracle. "Not found" tells an attacker nothing.

---

**💡 The three lessons I'm taking forward**

**Never let the client assert identity.** Derive it from what the request cryptographically proves. Everything else is a suggestion.

**Ordering is a security control.** Token before header. `/merge` before `/cart/**`. These aren't style choices — they're the whole boundary.

**"Hard to guess" ≠ "protected".** If there's no credential, mint one. A capability token is a perfectly good answer when a JWT isn't available.

---

Guests can now browse, add to cart, and pay — without an account. And no shopper can act as another one.

Stack: Spring Boot • Spring Security • Keycloak (OAuth2/OIDC) • Spring Cloud Gateway • Redis • Kafka • PostgreSQL • Angular

---

How does your team handle anonymous-to-authenticated identity? I'd genuinely like to hear other approaches. 👇

#Java #SpringBoot #SpringSecurity #Keycloak #Redis #Microservices #OAuth2 #SoftwareArchitecture #BackendDevelopment #Ecommerce #WebSecurity

---
---

## Version 2 — Short / High-Engagement Variant

**"Just make them log in first."**

That's the easiest way to lose 25% of your revenue at the checkout page.

So I built proper guest checkout on ShopFast. Four things I got wrong first:

**1. I let the client choose the endpoint.**
`/cart/**` for users, `/cart/guest/**` for guests. A stale JWT in localStorage made the frontend pick wrong → every add-to-cart returned 401.

Fix: one URL. The server resolves identity from the verified Keycloak token, falling back to an `X-Anon-Id` header. The token **always** wins — reverse that and any user can edit a stranger's cart.

**2. I trusted a header.**
Checkout read the buyer from client-supplied `X-User-Id`. One curl away from placing an order in your name.

Fix: the gateway now strips every identity header unconditionally, *then* re-injects them from the verified token. A client can't contribute to them at all.

**3. I forgot Keycloak hides its roles.**
Roles aren't in `scope` — they're in `realm_access.roles`. Without a custom converter, every request is authenticated with zero authorities and every `@PreAuthorize` throws a mystifying 403.

**4. I thought a UUID order id was "secure enough".**
It isn't. Hard to guess is not access control.

Fix: guests get a 256-bit capability token at checkout. Constant-time comparison (timing attacks are real). And 404 — not 403 — on mismatch, so nobody can enumerate valid order ids.

Guest carts live in Redis under `cart:guest:<uuid>` with a 14-day TTL — abandoned baskets garbage-collect themselves. On login they merge into `cart:<keycloak-sub>` and the guest key is deleted.

**The takeaway:** never let the client tell you who it is. Derive identity from what the request can actually prove.

Spring Boot • Spring Security • Keycloak • Redis • Spring Cloud Gateway

#Java #SpringBoot #SpringSecurity #Keycloak #Redis #Microservices #WebSecurity #BackendDevelopment
