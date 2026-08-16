import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Attaches the Keycloak bearer token to every call our own API gateway serves.
 *
 * Why this exists instead of relying solely on the library's `authInterceptor()`:
 * the library reads the token from its own internal auth state, which is only
 * populated after `checkAuth()` resolves. Building the request header from
 * `AuthService.getAccessToken()` (the same source the rest of the app uses) keeps
 * the header in lock-step with what the UI believes the session is, and works for
 * both absolute (`https://shopfast.live/api/...`) and relative (`/api/...`) URLs.
 *
 * Third-party requests are left untouched on purpose: a bearer token in someone
 * else's logs is a credential leak, not a convenience.
 */
export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();

  // Only our own API. Match both the absolute production URL and a relative one.
  const isOurApi =
    req.url.startsWith('/api/') ||
    req.url.includes('/api/');

  if (!token || !isOurApi) {
    return next(req);
  }

  const authReq = req.clone({
    headers: req.headers.set('Authorization', `Bearer ${token}`),
  });

  return next(authReq);
};
