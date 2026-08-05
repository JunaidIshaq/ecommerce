export const environment = {
  production: false,
  authPort: 8087,
  adminPort: 8093,
  categoryPort: 8082,
  productPort: 8081,
  notificationPort: 8091,
  cartPort: 8088,
  checkoutPort: 8084,
  baseDomain: null,

  // Keycloak. `clientId` is public and carries no secret - a browser app cannot
  // keep one. PKCE is what replaces the client secret; see keycloak/FRONTEND_MIGRATION.md.
  keycloak: {
    authority: 'http://localhost:8180/realms/shopfast',
    clientId: 'shopfast-web',
    // Must be listed verbatim in the client's redirectUris, or Keycloak refuses
    // the authorization request before the user ever sees a login page.
    redirectUrl: 'http://localhost:4200/callback',
    postLogoutRedirectUri: 'http://localhost:4200',
    // openid is mandatory; profile/email populate the ID token claims the header
    // renders. offline_access is deliberately absent: it is not assigned to the
    // shopfast-web client (Keycloak rejects the whole request with invalid_scope),
    // and a browser client should not hold an offline refresh token anyway.
    scope: 'openid profile email',
  },
};
