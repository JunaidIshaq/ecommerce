import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { OidcSecurityService } from 'angular-auth-oidc-client';

/**
 * Admin route guard.
 *
 * The previous version called `auth.getUserRoles()`, which returned a hard-coded
 * ['SUPER_ADMIN'] - so it granted access to everyone, including anonymous visitors.
 * Roles now come from the ID token Keycloak signed.
 *
 * This still only controls what the UI renders. The admin APIs enforce ROLE_ADMIN
 * server-side with @PreAuthorize; anyone can bypass this guard by calling the API
 * directly, and that is fine, because the API is where the real check lives.
 */
export const AdminAuthGuard: CanActivateFn = () => {
  const oidc = inject(OidcSecurityService);
  const router = inject(Router);

  return oidc.getPayloadFromIdToken().pipe(
    take(1),
    map((claims: any) => {
      const roles: string[] = claims?.realm_access?.roles ?? [];

      if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) {
        return true;
      }

      // Send them home rather than to a login page: an authenticated non-admin
      // logging in again will not gain the role, so a login prompt is a dead end.
      router.navigate(['/']);
      return false;
    })
  );
};
