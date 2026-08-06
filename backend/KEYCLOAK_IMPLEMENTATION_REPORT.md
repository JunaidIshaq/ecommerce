# Keycloak Implementation Report — ShopFast

**Platform:** ShopFast e-commerce (Spring Boot microservices + Angular 20 SPA)
**Identity provider:** Keycloak, realm `shopfast`, served at `https://shopfast.live/auth`
**Status:** Live in production. Legacy in-house JWT authentication fully retired.

---

## 1. Why Keycloak

The platform originally signed its own HS256 tokens inside `auth-service`. That design had
four structural problems, and each one is what Keycloak was brought in to solve.

| Problem with the in-house scheme | What Keycloak gives instead |
|---|---|
| A **shared symmetric secret** (HS256) had to be distributed to every service. Any service that could verify a token could also *mint* one, so a single compromised service compromised the whole platform. | **RS256 asymmetric signing.** Only Keycloak holds the private key; services fetch the public JWKS. A compromised service can verify but never forge. |
| **Key rotation required a coordinated redeploy** of every service. | JWKS is fetched from the realm at runtime, so **rotation is picked up automatically**. |
| Password hashing, lockout, reset flows, MFA, and session revocation were all **bespoke code we owned and had to get right**. | Brute-force protection, password policy, reset-by-email, TOTP/MFA, and session revocation are **configuration, not code**. |
| No **single sign-on** and no path to social/enterprise login. Adding an admin console or mobile app meant another auth implementation. | One realm serves web, mobile, and admin clients; **SSO, OIDC discovery, and identity brokering** are available without new code. |

Secondary but material benefits realised:

- **Standards compliance** — OIDC Authorization Code + PKCE for browsers, Client Credentials
  for service-to-service. Both are the RFC-blessed flows for their situation.
- **Centralised authorization model** — realm roles live in one place and appear in every
  token, so `@PreAuthorize("hasRole('ROLE_ADMIN')")` means the same thing in every service.
- **Auditability** — login events and admin events are recorded by Keycloak
  (`eventsEnabled` / `adminEventsEnabled`), giving a tamper-evident trail we were not
  producing before.
- **Reduced surface area in our code** — `auth-service` shrank from an identity provider to
  a thin registration orchestrator.

---

## 2. Realm design

Defined in [`keycloak/realm-export.json`](keycloak/realm-export.json).

### 2.1 Security posture

| Setting | Value | Rationale |
|---|---|---|
| `sslRequired` | `external` | TLS enforced for all non-loopback traffic. |
| `registrationAllowed` | `false` | Sign-up goes through `auth-service`, so the local profile row and the Keycloak user are created together. Self-service registration would create orphaned identities. |
| `verifyEmail` | `true` | Prevents account creation against addresses the user does not own. |
| `editUsernameAllowed` | `false` | Usernames are referenced in logs and support tickets; immutability keeps them meaningful. |
| `accessTokenLifespan` | 900 s | Short enough to bound the damage of a leaked token, long enough to avoid refresh churn. |
| `ssoSessionIdleTimeout` / `MaxLifespan` | 1800 s / 86400 s | Idle logout plus a hard daily ceiling. |
| `revokeRefreshToken` + `refreshTokenMaxReuse: 0` | on / 0 | **Refresh-token rotation with reuse detection.** A replayed refresh token invalidates the chain — this is the defence against stolen refresh tokens. |
| `bruteForceProtected` | `true`, 5 failures, 60 s → 900 s backoff | Non-permanent lockout: stops credential stuffing without handing attackers a denial-of-service against real users. |
| `passwordPolicy` | 12 chars, mixed case, digit, special, not-username, history(3) | Enforced centrally rather than re-implemented per client. |

### 2.2 Roles

Four realm roles: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SELLER`, `ROLE_SUPPORT`.

They are named with the `ROLE_` prefix deliberately so they map 1:1 onto Spring Security
authorities with no translation table — `hasRole('ADMIN')` resolves against `ROLE_ADMIN`
exactly as Spring expects. `ROLE_USER` is a default role, so every new account is usable
immediately without a provisioning step.

### 2.3 Clients

| Client | Type | Flow | Purpose |
|---|---|---|---|
| `shopfast-web` | public | Auth Code + **PKCE S256** | Angular storefront. Public because a SPA cannot hold a secret; PKCE is what makes a public client safe. |
| `shopfast-mobile` | public | Auth Code + PKCE S256 | Mobile app, `shopfast://callback`. |
| `shopfast-admin` | confidential | Auth Code + PKCE | Admin console; confidential because it runs server-side and handles privileged operations. |
| `shopfast-services` | confidential | **Client Credentials** (service account) | Internal service-to-service calls and Admin-API user provisioning. |

`directAccessGrantsEnabled` is **`false` on every browser-facing client.** The Resource
Owner Password Credentials grant is deprecated and would let a client collect raw passwords,
defeating the point of federating authentication.

`shopfast-services` carries an **audience protocol mapper** injecting `aud: shopfast-services`
— this is what makes the audience check in §3.2 enforceable.

---

## 3. Backend: resource-server model

### 3.1 One shared configuration, not eleven copies

All services inherit their security from a single Spring Boot auto-configuration in
`common-lib`: [`ResourceServerAutoConfiguration`](common-lib/src/main/java/com/shopfast/common/security/ResourceServerAutoConfiguration.java:47).

This was a deliberate architectural decision. Per-service security configuration is how
microservice fleets drift apart — one service ends up with CSRF enabled, another forgets the
role converter, a third leaves `/actuator/env` open. Centralising it means **a security fix
ships everywhere in one release**.

The shared chain enforces:

- `sessionCreationPolicy(STATELESS)` — no server-side sessions; the token is the entire state.
- CSRF disabled — correct *because* there are no cookies; CSRF protection guards cookie auth
  and here would only break legitimate bearer-token calls.
- `httpBasic` and `formLogin` disabled — bearer tokens are the only accepted credential, so
  there is no second, unmonitored way in.
- `OPTIONS /**` permitted — browsers send preflight unauthenticated; blocking it breaks CORS.
- **`anyRequest().authenticated()` as the default** — a newly added endpoint is protected
  until someone deliberately opens it. The failure direction is safe.

Per-service tuning is confined to
[`ShopfastSecurityProperties`](common-lib/src/main/java/com/shopfast/common/security/ShopfastSecurityProperties.java:16).
Note that `/actuator/**` is *not* blanket-public: only `health`, `info`, and `prometheus` are,
because `env` and `heapdump` leak secrets.

A service can override with its own `SecurityFilterChain` bean, or opt out via
`shopfast.security.enabled=false` (used in tests).

**This override is also the model's weak point.** Default-deny holds only for services
that actually inherit the shared chain. `admin-service` defines its own, and had drifted
badly — see §7.1.

### 3.2 Token validation

Three independent checks, each closing a distinct hole:

1. **Signature** against the realm JWKS — proves Keycloak issued it.
2. **Issuer (`iss`)** — proves *which realm* issued it. Without this, a token from any other
   realm on the same server would be accepted.
3. **Audience (`aud`)** via [`AudienceValidator`](common-lib/src/main/java/com/shopfast/common/security/AudienceValidator.java:18)
   — proves the token was minted *for us*. In a multi-client realm, signature + issuer alone
   would let a token handed to a low-trust frontend be replayed against these APIs.

**The split-URI detail.** `issuer-uri` must be the URL the *browser* uses, because that value
is baked into every token and a mismatch fails validation. But that public URL is often not
reachable from inside the container network — using it would mean hairpinning out to the
public IP and back through the TLS terminator just to read a public key, making service
startup depend on the reverse proxy. So `jwk-set-uri` is pointed at the internal address
while `issuer-uri` stays public. Nothing is weakened: the issuer is still validated, and
JWKS is public data by design.

### 3.3 Principal identity

`setPrincipalClaimName("sub")` — the principal is Keycloak's **stable user id**, not the
username or email. Both of those are user-mutable; using either as a database key would
corrupt ownership the first time a user changed their email.

### 3.4 Role mapping

`KeycloakRealmRoleConverter` flattens `realm_access.roles` and the configured clients'
`resource_access.*.roles` into Spring authorities. Without it, every `@PreAuthorize` in the
platform would fail closed — this converter is the load-bearing link between the realm's
authorization model and the code's.

### 3.5 Gateway: identity headers, and why they are stripped

[`IdentityHeaderFilter`](api-gateway/src/main/java/com/shopfast/apigateway/filter/IdentityHeaderFilter.java:54)
derives `userId` / `X-User-Id` / `X-User-Email` / `X-User-Roles` from the **verified** token.

The important half is not the injection — it is the **unconditional stripping of inbound
copies before the authenticated values are set.** A header is just text a client can type.
Nothing otherwise stops a caller sending `userId: <somebody-else>` and being believed, and
because services are mutually reachable inside the network that is a real
horizontal-privilege-escalation path, not a theoretical one. The strip list is deliberately
generous across every spelling in use; scrubbing only some would achieve nothing.

This is defence in depth, not the boundary. Services validate the bearer token independently,
and **new code reads identity from the token via `@AuthenticationPrincipal Jwt`**, not from
headers. The headers exist to avoid rewriting twenty pre-existing controllers at once.

---

## 4. Frontend: OIDC integration

Detailed in [`keycloak/FRONTEND_MIGRATION.md`](keycloak/FRONTEND_MIGRATION.md).

- `angular-auth-oidc-client` with **Authorization Code + PKCE**. No tokens are ever handled
  by application code; the library owns acquisition, storage, and silent renewal.
- An HTTP interceptor attaches the bearer token only to configured API origins, so tokens
  cannot leak to third-party hosts.
- `userData$` drives UI state. Authenticated-only affordances (profile links, notification
  bell) are gated on it, so signing out removes them without a reload.
- Logout calls the OIDC end-session endpoint — terminating the **Keycloak SSO session**, not
  just the local token. Clearing local state alone would leave the user silently re-logged-in
  on the next authorize request.
- The app runs zoneless (`provideZonelessChangeDetection()`); auth-dependent views use
  **signals** so async identity updates actually re-render.

---

## 5. Registration and user provisioning

Self-registration is disabled in the realm, so `auth-service` orchestrates it:

1. Validate the request.
2. Create the user in Keycloak via the **Admin REST API**, authenticating as the
   `shopfast-services` service account (client credentials — no admin password in any service).
3. Set the credential and assign the default realm role.
4. Create the corresponding local profile row.

This keeps Keycloak authoritative for credentials while the platform keeps its own profile
data, and guarantees the two are created together rather than drifting.

### 5.1 Required service-account roles

The service account needs explicit `realm-management` grants, applied by
[`keycloak/grant-service-account-roles.sh`](keycloak/grant-service-account-roles.sh):

| Role | Why |
|---|---|
| `manage-users`, `view-users`, `query-users` | Create and look up users. |
| `view-realm`, `manage-realm` | Required to **map a realm role onto a new user** — Keycloak's `canMapRole()` check demands realm-management rights unless fine-grained admin permissions are enabled. This is non-obvious and was the cause of a production 403. |

### 5.2 Production defects found and fixed

Registration failed in production with a 500 due to three stacked issues, all now resolved
and captured in the tooling:

1. **Secret not delivered** — `SHOPFAST_SERVICES_CLIENT_SECRET` was set on the `keycloak`
   compose service instead of `auth-service`. Now declared with the mandatory `${VAR:?}`
   pattern so a missing secret fails at startup rather than at first registration.
2. **Wrong Admin API base URL** — production Keycloak runs with `KC_HTTP_RELATIVE_PATH=/auth`,
   so `KEYCLOAK_BASE_URL` must include `/auth`; without it every Admin call 404'd.
3. **Missing service-account roles** — the 403 described in §5.1.

`auth-service` deliberately declares `keycloak.admin.client-secret` with **no fallback value**,
so a misconfiguration surfaces as a loud, unambiguous error instead of an anonymous 401 later.

---

## 6. Migration from the legacy system

| Script | Role |
|---|---|
| [`migrate-users.py`](keycloak/migrate-users.py) | Ports legacy JPA users into the realm, preserving ids/emails and mapping legacy roles onto realm roles. |
| [`seed-dev-users.py`](keycloak/seed-dev-users.py) | Creates a deterministic set of test accounts across all roles for dev/QA. |
| [`repair-client-scopes.py`](keycloak/repair-client-scopes.py) | Ensures each client exposes the scopes and protocol mappers the services expect — a missing scope silently strips claims and looks like a permissions bug. |
| [`apply-resource-server.py`](keycloak/apply-resource-server.py), [`apply-security-config.py`](keycloak/apply-security-config.py), [`apply-security-config-rest.py`](keycloak/apply-security-config-rest.py), [`fix-oauth-placement.py`](keycloak/fix-oauth-placement.py) | Applied the resource-server configuration consistently across every service module during the cutover. |
| [`backup-keycloak-db.sh`](keycloak/backup-keycloak-db.sh) | Scheduled dump of the Keycloak database. |

**Why the backup script matters:** once Keycloak owns identity, its database is the single
point of failure for the whole platform. Losing it means losing every account. Backups were
added as part of the migration, not deferred.

---

## 7. Administrative access

`ROLE_ADMIN` is a realm role, not a database flag. There is no "admin users" table: an
operator is made an administrator by assigning the realm role in Keycloak, and it then
appears in `realm_access.roles` on every token they hold.

**Admin console:** `https://shopfast.live/admin` (lazy-loaded Angular module, client-rendered),
with pages under `/admin/dashboard`, `/users`, `/orders`, `/products`, `/inventory`,
`/coupons`, `/reviews`, `/payments`, `/notifications`.

**Admin APIs:** `admin-service` on `/api/v1/admin/**`, routed via the gateway.

Access is checked in three places, and only the last two are security controls:

| Layer | Check | Status |
|---|---|---|
| Angular `AdminAuthGuard` | Reads `realm_access.roles` from the ID token; redirects non-admins | UI only — trivially bypassed with `curl` |
| API gateway | `hasAnyRole("ADMIN", "SUPER_ADMIN")` on `/api/v1/admin/**` | Enforced |
| `admin-service` filter chain | Same role rule, plus default-deny | Enforced |

### 7.1 Defect found during this review: the admin API was open

While documenting the above, the server-side half was found to be **absent**:

```java
// .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")  ← commented out
.requestMatchers("/api/v1/admin/**").permitAll()
```

The single `@PreAuthorize("hasRole('ADMIN')")` in `AdminUserController` was commented out
too, and the gateway required only `authenticated()` — which proves *who* a caller is, not
*what they may do*. **Any signed-in customer could list every user, read every order, and
adjust stock.** The browser guard was the only obstacle, and a route guard is a rendering
decision, not an access control.

Three related problems in the same file:

1. `permitAll()` on the entire admin tree (now `hasAnyRole("ADMIN", "SUPER_ADMIN")`).
2. `/actuator/**` blanket-permitted, unlike every other service — `env` and `heapdump` would
   have disclosed the datasource password and in-memory secrets. Now narrowed to
   `health`, `info`, `prometheus`.
3. `admin-service` had **never been migrated off HS256**: it still ran the legacy
   `JwtAuthenticationFilter` beside the Keycloak resource server and still consumed
   `JWT_SECRET`. It was the last holder of the shared symmetric key. The filter, `JwtUtils`,
   and the `JWT_SECRET` wiring have been deleted; it now accepts RS256 only.

The role check is now applied at both the gateway and the service, so bypassing the gateway
and reaching the pod directly is still rejected.

**Lesson:** the value of a shared security configuration is lost precisely where a service
opts out of it. Overriding `SecurityFilterChain` should be treated as a reviewable exception,
and a commented-out authorization rule should never survive review.

---

## 8. Deployment topology

- Keycloak runs behind nginx at `https://shopfast.live/auth`, configured with
  `KC_HTTP_RELATIVE_PATH=/auth`, `KC_PROXY_HEADERS=xforwarded`, `KC_HOSTNAME_STRICT=false`.
  Proxy-header awareness is required or Keycloak generates redirect URIs against its internal
  address and the login flow breaks.
- Keycloak's port is **not exposed publicly**; all traffic arrives through the TLS terminator.
- Every secret is injected via environment with the mandatory `${VAR:?}` form, so the stack
  refuses to start rather than booting with a blank credential.

---

## 9. Outcome

- Symmetric shared secrets eliminated; no service can forge a token.
- Key rotation is automatic.
- Authentication policy — lockout, password rules, session lifetimes, MFA readiness — is
  configuration, reviewable in one place.
- One security configuration governs all services; fixes ship fleet-wide in one release.
- Default-deny authorization, audience-pinned tokens, and gateway header scrubbing close the
  replay and privilege-escalation paths the previous design left open.
- `auth-service` is no longer an identity provider, only a registration orchestrator.

### Recommended next steps

1. **Enable MFA (TOTP)** for `ROLE_ADMIN` — the realm already supports it; it is a required-action
   policy change.
2. **Ship Keycloak events to the log pipeline** so authentication anomalies are alertable rather
   than only browsable in the admin console.
3. **Automate role granting in provisioning** — fold `grant-service-account-roles.sh` into the
   bootstrap path so a realm rebuild cannot reproduce the §5.2 403.
4. **Restore-test the backups.** An untested backup is an assumption, not a control.
5. **Adopt fine-grained admin permissions** to drop `manage-realm` from the service account,
   narrowing it to exactly the role-mapping permission it needs.
