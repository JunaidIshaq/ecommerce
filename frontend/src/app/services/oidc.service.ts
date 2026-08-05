import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { User as OidcUser, UserManager, WebStorageStateStore } from 'oidc-client-ts';
import { environment } from '../../environments/environment';

/**
 * Thin, SSR-safe wrapper around oidc-client-ts `UserManager`.
 *
 * Authorization Code + PKCE against Keycloak. `shopfast-web` is a *public*
 * client: there is no client secret here and there must never be one — a secret
 * shipped in a JS bundle is not a secret. PKCE is what replaces it.
 */
@Injectable({ providedIn: 'root' })
export class OidcService {
  private manager: UserManager | null = null;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    if (this.isBrowser()) {
      this.manager = new UserManager({
        authority: environment.keycloak.authority,
        client_id: environment.keycloak.clientId,
        // Taken from config rather than derived from window.location.origin:
        // Keycloak matches redirect_uri against the client's registered list
        // verbatim, so it must be the value someone deliberately registered, not
        // whatever origin the page happens to be served from.
        redirect_uri: environment.keycloak.redirectUrl,
        post_logout_redirect_uri: environment.keycloak.postLogoutRedirectUri,
        silent_redirect_uri: environment.keycloak.redirectUrl.replace(/\/callback$/, '/silent-renew'),
        response_type: 'code',

        // openid is mandatory; profile/email populate the ID token claims the UI shows.
        scope: environment.keycloak.scope,

        // Refresh the access token in the background shortly before it expires so a
        // user mid-checkout is not bounced to the login page. Access tokens live 15
        // minutes; this fires at ~13.
        automaticSilentRenew: true,
        accessTokenExpiringNotificationTimeInSeconds: 120,

        // sessionStorage, not localStorage: tokens die with the tab and are not
        // shared with other apps on the same origin. It does not defeat XSS
        // (nothing in a browser does), it just shortens the exposure window.
        userStore: new WebStorageStateStore({ store: window.sessionStorage }),
        stateStore: new WebStorageStateStore({ store: window.sessionStorage })
      });
    }
  }

  isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  /** Null during SSR — every caller must tolerate that. */
  get userManager(): UserManager | null {
    return this.manager;
  }

  getUser(): Promise<OidcUser | null> {
    return this.manager ? this.manager.getUser() : Promise.resolve(null);
  }

  signinRedirect(returnUrl?: string): Promise<void> {
    if (!this.manager) return Promise.resolve();
    return this.manager.signinRedirect({ state: returnUrl ?? window.location.pathname });
  }

  signinRedirectCallback(): Promise<OidcUser> {
    if (!this.manager) return Promise.reject(new Error('OIDC unavailable on the server'));
    return this.manager.signinRedirectCallback();
  }

  signinSilent(): Promise<OidcUser | null> {
    return this.manager ? this.manager.signinSilent() : Promise.resolve(null);
  }

  signinSilentCallback(): Promise<void> {
    return this.manager ? this.manager.signinSilentCallback().then(() => void 0) : Promise.resolve();
  }

  /**
   * Hits the OIDC end-session endpoint. Clearing storage is not logout: the
   * Keycloak SSO session would survive and the next login would silently succeed.
   */
  signoutRedirect(): Promise<void> {
    return this.manager ? this.manager.signoutRedirect() : Promise.resolve();
  }
}
