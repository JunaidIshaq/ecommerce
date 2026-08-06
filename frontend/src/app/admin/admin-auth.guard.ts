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
 * This still only controls what the UI renders - a guard is a rendering decision,
 * not an access control, and anyone can bypass it with curl. It is safe to rely on
 * only because the same role is enforced twice server-side: at the gateway on
 * /api/v1/admin/**, and again inside admin-service's own filter chain.
 *
 * That server-side half was missing until recently: the rule in admin-service sat
 * commented out above a permitAll(), so this guard was briefly the only thing
 * standing in front of the admin API. Do not weaken it on the assumption that the
 * backend is covered - check that it still is.
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
