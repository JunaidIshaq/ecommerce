import { ApplicationConfig, isDevMode } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import {
  provideHttpClient,
  withFetch,
  withInterceptors
} from '@angular/common/http';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import {
  provideAuth,
  authInterceptor,
  LogLevel
} from 'angular-auth-oidc-client';
import { routes } from './app.routes';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideAuth({
      config: {
        authority: environment.keycloak.authority,
        redirectUrl: environment.keycloak.redirectUrl,
        postLogoutRedirectUri: environment.keycloak.postLogoutRedirectUri,
        clientId: environment.keycloak.clientId,

        // `code` + a public client means PKCE, which the library enables by default.
        // A browser app cannot hold a client secret, so PKCE is the only thing
        // preventing a stolen authorization code from being redeemed by someone else.
        responseType: 'code',

        // openid is required; profile/email populate the claims the header displays.
        scope: 'openid profile email',

        // Refresh silently in the background rather than bouncing a user mid-checkout
        // to the login page when the 15-minute access token expires.
        silentRenew: true,
        useRefreshToken: true,
        renewTimeBeforeTokenExpiresInSeconds: 120,

        // Only send the access token to our own API. Without this the library would
        // attach it to every outgoing request, including third-party ones - which is
        // how bearer tokens end up in someone else's logs.
        secureRoutes: [
          environment.baseDomain ?? 'http://localhost:',
        ],

        logLevel: isDevMode() ? LogLevel.Warn : LogLevel.Error,

        // Reject an ID token whose `nonce`/`at_hash` do not line up. On by default;
        // stated explicitly because turning it off is a common "fix" for clock skew.
        ignoreNonceAfterRefresh: false,
      }
    }),

    // Replaces the hand-written AuthInterceptor. The old one read a token from
    // localStorage and, on a 401, tried to refresh it against auth-service. Both
    // halves of that are now the library's job, and it does the refresh correctly
    // under concurrent requests instead of firing one refresh per in-flight call.
    provideHttpClient(withFetch(), withInterceptors([authInterceptor()])),

    provideRouter(routes, withViewTransitions()),
    provideClientHydration(withEventReplay()),
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
  ]
};
