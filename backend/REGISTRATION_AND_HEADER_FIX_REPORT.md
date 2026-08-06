# Implementation Report — Header Auth Gating & Keycloak Registration Repair

**Date:** 2026-08-05
**Environment:** production — https://shopfast.live (VPS `72.62.250.5`, compose project `/root/ecommerce/backend`)
**Commits:** `d3cc150`, `313f116`
**Author:** engineering

---

## 1. Executive Summary

Two independent problems were reported and resolved in this session.

| # | Problem | Severity | Status |
|---|---------|----------|--------|
| 1 | Header rendered the Profile link and notification bell to anonymous visitors | Low (UX / minor information leak) | Fixed & deployed |
| 2 | `POST /api/v1/auth/register` returned HTTP 500 — **user registration was completely broken in production** | Critical | Fixed & verified |

Problem 2 turned out to be three stacked configuration defects, each masked by the one before it. Every fix revealed a new failure mode: missing credential → wrong URL → missing authorisation. All three are now corrected, verified end to end, and captured in version control so a rebuild cannot silently reintroduce them.

---

## 2. Problem 1 — Header exposed authenticated-only UI

### 2.1 Symptom

A logged-out visitor saw a **Profile** link and a **notification bell** in the header. Clicking Profile produced a request that could only 401, and the bell could never load notifications.

### 2.2 Root cause

`shared/header/header.component.html` already had an authentication-aware section (`Hi, {{ u.email }}` / Logout vs. Login / Sign Up) driven by `user$ | async`, but three elements had been added later **outside** that guard:

- line 39 — desktop `Profile` link
- line 118 — mobile-menu `Profile` link
- line 42 — `.notif-wrapper` (bell icon, unread badge, dropdown)

There was no logic bug; the guard simply had not been applied to those nodes.

### 2.3 Changes

**`frontend/src/app/shared/header/header.component.html`**

```html
<!-- desktop -->
<a routerLink="/profile" class="nav-link" *ngIf="(user$ | async)">Profile</a>
<div class="notif-wrapper" *ngIf="(user$ | async)"> ... </div>

<!-- mobile menu -->
<a routerLink="/profile" (click)="closeMenu()" *ngIf="(user$ | async)">Profile</a>
```

**`frontend/src/app/shared/header/header.component.ts`**

```ts
logout() {
  this.profileService.clear();   // added
  this.auth.logout();
}
```

`ProfileService` was injected into the constructor for this.

### 2.4 Why `user$ | async` and not a stored boolean

`AuthService.user$` is fed from the OIDC library's `userData$`. Using the async pipe means the header reacts to login **and** logout without any manual refresh, and it stays correct under the app's `provideZonelessChangeDetection()` configuration — the async pipe marks the view dirty itself, whereas a plain field assignment in a `subscribe()` would not (this is the same class of bug that previously froze the profile page on "Loading…").

### 2.5 Security note

Clearing `ProfileService` on logout matters beyond tidiness: the service holds a `BehaviorSubject` cache of the signed-in user's profile. On a shared browser, logging out and logging in as a different user would otherwise briefly render the previous user's name, email and phone number from that cache before the new fetch resolved. The profile page already did this; the header logout button did not.

### 2.6 Deployment

```
npx ng build --configuration production      # main-STICLPMZ.js
rsync -az --delete dist/frontend/browser/ root@72.62.250.5:/var/www/shopfast.live/
```

Verified the new bundle hash is present on the server.

---

## 3. Problem 2 — Registration returned HTTP 500

### 3.1 Reported symptom

```
POST https://shopfast.live/api/v1/auth/register
{"error":"Internal Server Error",
 "message":"keycloak.admin.client-secret is not configured; auth-service cannot manage users",
 "status":500}
```

### 3.2 Architecture context

Since the Keycloak migration, `auth-service` no longer stores credentials. `POST /api/v1/auth/register` performs a three-step orchestration against the **Keycloak Admin REST API** using the `shopfast-services` client's *service account* (OAuth2 client-credentials grant):

1. create the user in realm `shopfast`
2. assign the default realm role
3. sync a profile row into `user_db` via `user-service`

Any failure in steps 1–2 aborts registration. All three defects below sat in that path.

---

### 3.3 Defect A — admin client secret never reached auth-service

**Evidence**

```
$ docker exec auth-service printenv | grep -i keycloak
KEYCLOAK_ISSUER_URI=...
KEYCLOAK_JWK_SET_URI=...
# no SHOPFAST_SERVICES_CLIENT_SECRET
```

The variable *was* defined in `.env` and *was* referenced in `docker-compose.yml` — but on **line 83, inside the `keycloak` service block**, not the `auth-service` block. It was used to bootstrap the client inside Keycloak and never handed to the consumer of that client.

`auth-service/src/main/resources/application.yml` deliberately has no default:

```yaml
keycloak:
  admin:
    client-secret: ${SHOPFAST_SERVICES_CLIENT_SECRET:}
```

so the service failed loudly rather than authenticating as nobody — correct behaviour, but the failure surfaced only when a real user tried to sign up.

**Fix** — `backend/docker-compose.yml`, `auth-service.environment`:

```yaml
- KEYCLOAK_BASE_URL=${KEYCLOAK_BASE_URL:-http://keycloak:8180}
- KEYCLOAK_REALM=${KEYCLOAK_REALM:-shopfast}
- KEYCLOAK_ADMIN_CLIENT_ID=${KEYCLOAK_ADMIN_CLIENT_ID:-shopfast-services}
- SHOPFAST_SERVICES_CLIENT_SECRET=${SHOPFAST_SERVICES_CLIENT_SECRET:?SHOPFAST_SERVICES_CLIENT_SECRET must be set in .env}
```

The `:?` form is deliberate: a deployment missing this secret now **fails at `docker compose up`**, not silently at the first customer signup. This matches the existing treatment of `DB_PASSWORD`, `JWT_SECRET` and `APP_PASSWORD_ENCRYPTION_KEY`.

---

### 3.4 Defect B — Admin API base URL missing the `/auth` context path

**Next error**

```
{"message":"404 Not Found: \"<html><body><h1>Resource not found</h1></body></html>\""}
```

**Evidence**

```
$ docker exec keycloak printenv | grep KC_HTTP
KC_HTTP_RELATIVE_PATH=/auth
```

Production serves Keycloak under `/auth` (nginx routes `https://shopfast.live/auth` to it, and `KC_HTTP_RELATIVE_PATH` is set to match so issuer URLs are consistent). The compose default for the internal admin URL was `http://keycloak:8180` with no suffix, so every Admin REST call hit a path that does not exist.

This is easy to miss because the *issuer* URI (`KEYCLOAK_ISSUER_URI`) and the *JWK set* URI already included `/auth` — they had been fixed during the migration. The admin base URL was a fourth, newly introduced URL that nobody had exercised yet.

**Fix** — production `.env` on the VPS:

```
KEYCLOAK_BASE_URL=http://keycloak:8180/auth
```

The compose default stays `http://keycloak:8180` so local dev (which runs with `KEYCLOAK_RELATIVE_PATH=/`) keeps working, with an explanatory comment added above the line.

---

### 3.5 Defect C — service account lacked realm-management authorisation

**Next error**

```
{"message":"403 Forbidden: \"{\"error\":\"HTTP 403 Forbidden\"}\""}
```

Reaching a 403 was itself progress: the request was now correctly routed and authenticated.

**First grant** — the service account had *no* `realm-management` roles at all:

```
kcadm add-roles -r shopfast \
  --uusername service-account-shopfast-services \
  --cclientid realm-management \
  --rolename manage-users --rolename view-users --rolename query-users
```

Token inspection confirmed the roles landed:

```json
{"realm-management": {"roles": ["manage-users","view-users","query-groups","query-users"]}}
```

**Still 403.** The service logs pinpointed the exact call:

```
c.s.a.service.KeycloakAuthFacade : Profile sync failed after creating Keycloak user
  aa80c3a8-... ; disabling it
org.springframework.web.client.HttpClientErrorException$Forbidden: 403 Forbidden
  at KeycloakAdminClient.assignRealmRole(KeycloakAdminClient.java:165)
  at KeycloakAuthFacade.register(KeycloakAuthFacade.java:61)
```

So **step 1 (create user) now succeeded** and step 2 (assign realm role) was rejected. Keycloak's `RoleContainerPermissions.canMapRole()` requires realm-level management rights to map a *realm* role onto a user; `manage-users` alone is insufficient unless fine-grained admin permissions are enabled (a non-default feature).

**Second grant:**

```
--rolename view-realm --rolename manage-realm
```

After restarting `auth-service` (it caches the client-credentials access token, so previously issued tokens carry the old role set until refresh):

```json
{"message":"If the address is not already registered, a verification email has been sent."}
```

Registration restored.

---

### 3.6 Verification

| Check | Result |
|-------|--------|
| `POST /api/v1/auth/register` (new email) | 200 — enumeration-safe success message |
| Duplicate email | Logged `Registration attempted for an already-registered email`, same generic response (no user enumeration) |
| Client-credentials token contains `manage-users`, `view-realm`, `manage-realm` | Confirmed by decoding `resource_access` |
| Test accounts removed | Deleted from Keycloak realm `shopfast` and from `user_db.users` |

---

## 4. Files Changed

| File | Change |
|------|--------|
| `frontend/src/app/shared/header/header.component.html` | Gate Profile links (desktop + mobile) and notification bell on `user$ \| async` |
| `frontend/src/app/shared/header/header.component.ts` | Inject `ProfileService`; clear cached profile on logout |
| `backend/docker-compose.yml` | Pass Keycloak admin base URL, realm, client id and secret to `auth-service`; secret is mandatory (`:?`) |
| `backend/keycloak/grant-service-account-roles.sh` | **New** — idempotent script granting the required `realm-management` roles, with rationale for each |
| VPS `/root/ecommerce/backend/.env` | Added `KEYCLOAK_BASE_URL=http://keycloak:8180/auth` (not in VCS — secrets file) |
| Keycloak realm `shopfast` | Role mappings on `service-account-shopfast-services` (runtime state, reproducible via the script above) |

---

## 5. Why This Broke, and What Prevents Recurrence

The Keycloak migration moved user management from local JPA + BCrypt to the Keycloak Admin API. The **read** path (token validation) was exercised constantly and therefore correct. The **write** path (user provisioning) is only exercised by a real signup, and it depended on three pieces of configuration that no test or health check touched:

1. a secret handed to the right container
2. a URL that matches the reverse-proxy context path
3. authorisation grants that live in Keycloak's database, not in the repo

Mitigations now in place:

- The secret is **required at container start** (`:?`), converting a silent runtime 500 into a loud deploy-time failure.
- The role grants are **codified** in `keycloak/grant-service-account-roles.sh` with comments explaining why each role is needed, so a realm rebuild or a fresh environment can restore them deterministically instead of by trial and error.
- The `/auth` path requirement is **commented inline** in `docker-compose.yml` next to `KEYCLOAK_BASE_URL`.

---

## 6. Recommended Follow-Ups (not done in this session)

1. **Roll back failed registrations properly.** `KeycloakAuthFacade.register` currently *disables* the Keycloak user when a later step fails ("disabling it"). Those orphan disabled accounts permanently occupy the email address, so the customer can never retry — the retry hits "already registered". The failure path should **delete** the just-created user instead. Several such orphans were produced during this debugging session and should be audited.

2. **Smoke-test the write path.** A post-deploy check that registers and deletes a throwaway account would have caught all three defects before a user did.

3. **Tighten `manage-realm`.** It is broader than registration requires. Enabling Keycloak's fine-grained admin permissions would allow granting only `map-role` on the specific default role, dropping realm-wide management rights from the service account.

4. **Consider a realm bootstrap/import.** Role mappings currently live only in the Keycloak database plus a helper script. A partial realm import applied on startup would make the authorisation model fully declarative.
