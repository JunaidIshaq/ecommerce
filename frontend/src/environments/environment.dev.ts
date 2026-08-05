export const environment = {
  production: true,
  authPort: null, // Use domain directly
  adminPort: null,
  categoryPort: null,
  productPort: null,
  notificationPort: null,
  cartPort: null,
  checkoutPort: null,
  baseDomain: 'https://shopfast.live',

  // Keycloak / OIDC — public client, PKCE. No client secret here, ever.
  keycloak: {
    // Keycloak is mounted at /auth on the main domain rather than its own
    // subdomain, which avoids a second DNS record and a second certificate.
    authority: 'https://shopfast.live/auth/realms/shopfast',
    clientId: 'shopfast-web',
    // Must match the client's redirectUris in the realm exactly, including scheme.
    redirectUrl: 'https://shopfast.live/callback',
    postLogoutRedirectUri: 'https://shopfast.live',
    // openid is mandatory; profile/email populate the ID token claims the header
    // renders. offline_access is deliberately absent: it is not assigned to the
    // shopfast-web client (Keycloak rejects the whole request with invalid_scope),
    // and a browser client should not hold an offline refresh token anyway.
    scope: 'openid profile email',
  }
};
