# Frontend Migration: from `POST /api/v1/auth/login` to Authorization Code + PKCE

This is a breaking change for every client. The backend no longer accepts a
username and password over its own API; Keycloak does, on its own login page.

## What actually changed, and why

Previously the frontend collected the password, AES-encrypted it with a key shipped
to the client, POSTed it to `auth-service`, and got back an HS256 JWT signed with a
secret that every microservice held a copy of.

Three things were wrong with that:

1. **The frontend handled the password.** Any XSS on the storefront could read it.
   With Authorization Code the password is typed into a page served by Keycloak on a
   different origin; the storefront's JavaScript can never see it.
2. **Encrypting the password in the browser bought nothing.** The key was in the
   bundle, so it was obfuscation, not encryption. TLS already provided the transport
   security it was pretending to add.
3. **Every service could mint tokens.** A symmetric secret means a verifier is also
   a signer. Compromise any one of thirteen services and you can forge an admin
   token. With RS256 the services hold only the public key.

## The new flow

```
Browser                        Keycloak                     API Gateway
   |                              |                              |
   |-- 1. redirect /auth -------->|                              |
   |    ?client_id=shopfast-web   |                              |
   |    &response_type=code       |  user types password here,   |
   |    &code_challenge=S256(v)   |  never in your app           |
   |                              |                              |
   |<-- 2. redirect back ---------|                              |
   |    ?code=abc                 |                              |
   |                              |                              |
   |-- 3. POST /token ----------->|                              |
   |    code=abc                  |                              |
   |    code_verifier=v           |                              |
   |<-- access + refresh token ---|                              |
   |                              |                              |
   |-- 4. GET /api/v1/products, Authorization: Bearer <access> -->|
```

Step 3 is where PKCE earns its place. A public client has no secret, so without
PKCE anyone who intercepts the redirect (a malicious app registered for the same
custom URL scheme, browser history, a proxy log) could exchange the code for
tokens. The `code_verifier` is a random value only the originating client knows,
and the authorization request commits to it in advance via its SHA-256 hash.
Keycloak refuses the exchange if they do not match.

## Client registration

| Client | Type | Use |
|---|---|---|
| `shopfast-web` | public, PKCE required | browser storefront |
| `shopfast-mobile` | public, PKCE required | native app, redirect `shopfast://callback` |
| `shopfast-admin` | confidential | admin console (has a server-side backend) |
| `shopfast-services` | confidential, client-credentials | backend-to-backend only, never a browser |

`shopfast-web` and `shopfast-mobile` are **public clients with no secret**. This is
correct and deliberate: a secret shipped in a JS bundle or an APK is not a secret.
PKCE is what replaces it. Do not "fix" this by adding a secret to the frontend.

## Implementation

Use a certified library. Do not hand-roll the redirect and token exchange — the
subtle parts (`state` validation, nonce checking, clock skew, refresh rotation) are
exactly where hand-rolled implementations get it wrong.

- Web (React/Vue/Angular): [`oidc-client-ts`](https://github.com/authts/oidc-client-ts)
  or `keycloak-js`
- React specifically: `react-oidc-context` on top of `oidc-client-ts`
- Mobile: `AppAuth-iOS` / `AppAuth-Android` (system browser, never a WebView)

### Minimal web configuration

```js
import { UserManager, WebStorageStateStore } from "oidc-client-ts";

export const userManager = new UserManager({
  authority: "https://auth.yourdomain.com/realms/shopfast",
  client_id: "shopfast-web",
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: window.location.origin,
  response_type: "code",

  // openid is mandatory; profile/email populate the ID token claims the UI shows.
  scope: "openid profile email",

  // Refresh the access token in the background shortly before it expires so a
  // user mid-checkout is not bounced to the login page. Access tokens live 15
  // minutes; this fires at ~13.
  automaticSilentRenew: true,
  accessTokenExpiringNotificationTimeInSeconds: 120,

  // sessionStorage, not localStorage: tokens die with the tab, and they are not
  // shared across tabs of other apps on the same origin. It does not defeat XSS
  // (nothing in a browser does), it just shortens the exposure window.
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
});
```

### Callback route

```js
// /callback - this component's only job is to finish the exchange and get out.
useEffect(() => {
  userManager
    .signinRedirectCallback()
    .then(() => navigate("/", { replace: true }))
    // A failed callback usually means a stale/replayed code or a clock skew
    // problem. Sending the user back to login is the only safe recovery.
    .catch(() => userManager.signinRedirect());
}, []);
```

### Attaching the token

```js
const user = await userManager.getUser();
fetch("/api/v1/orders", {
  headers: { Authorization: `Bearer ${user.access_token}` },
});
```

Send the **access token**, not the ID token. The ID token is proof of
authentication addressed to your frontend; the access token is the one whose `aud`
claim names the API. The gateway rejects anything else — that check is what stops a
token issued for a different client being replayed against ShopFast.

### Reading roles

```js
const claims = user.profile; // parsed ID token
const roles = user.profile.realm_access?.roles ?? [];
const isAdmin = roles.includes("ROLE_ADMIN");
```

Use this for showing and hiding UI only. Authorization is enforced server-side by
`@PreAuthorize` on every endpoint; the frontend check is a convenience, not a
control, and an attacker will simply call the API directly.

### Logout

```js
await userManager.signoutRedirect();
```

Clearing local storage is not logout — the Keycloak SSO session survives and the
next login silently succeeds without a password prompt. `signoutRedirect` hits the
OIDC end-session endpoint and terminates the session at the source.

## Registration and password reset

Registration still goes through the backend, because a ShopFast account is a
Keycloak identity *plus* a profile row, and something has to create both:

```
POST /api/v1/auth/register
{ "email": "...", "password": "...", "firstName": "...", "lastName": "..." }
```

Send the password **in plain text over TLS**. The old AES pre-encryption step is
gone. If your client still imports the encryption helper, delete it.

The response is deliberately identical whether or not the email was already taken.
Do not try to detect "email exists" from it and do not add a client-side
availability check — both reintroduce the account-enumeration leak the generic
response exists to close.

Password reset is Keycloak's, via `POST /api/v1/auth/password-reset` or the
"Forgot password?" link on the Keycloak login page. Do not build your own form.

## Migrated users

Existing users were imported without their passwords: BCrypt hashes are not
portable into Keycloak's credential store. Every migrated user carries an
`UPDATE_PASSWORD` required action, so their first login redirects to a
set-a-new-password screen before returning to your `redirect_uri`.

Your callback handler must not assume the redirect comes straight back. It already
will not if you use the libraries above — but any hand-rolled flow that assumes a
single round trip will break here.

## Cutover checklist

- [ ] Add your dev origin to `redirectUris` and `webOrigins` on `shopfast-web`.
      A missing `webOrigins` entry surfaces as an opaque CORS failure on the token
      request, not as a helpful OAuth error.
- [ ] Delete the login form, the password-encryption helper, and any code reading
      `JWT_SECRET`-signed tokens.
- [ ] Replace token storage with the library's `userStore`.
- [ ] Verify refresh works by idling past 15 minutes with the tab open.
- [ ] Verify logout by logging out, then hitting the login route again — you must
      be prompted for credentials.
- [ ] Test a migrated account end to end, including the forced password change.

Old HS256 tokens keep working at the gateway until they expire, so web and mobile
can cut over independently rather than in one simultaneous release.
