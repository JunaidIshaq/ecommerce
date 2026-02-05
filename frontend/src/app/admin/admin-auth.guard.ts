import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const AdminAuthGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  console.log('ADMIN GUARD: Checking access');

  const roles = auth.getUserRoles();
  console.log('ADMIN GUARD ROLES:', roles);

  if (roles?.includes('ADMIN') || roles?.includes('SUPER_ADMIN') ||
    roles?.includes('ROLE_ADMIN') || roles?.includes('ROLE_SUPER_ADMIN')) {
    console.log('ADMIN GUARD: Access granted');
    return true;
  }

  console.log('ADMIN GUARD: Access denied, redirecting to /');
  router.navigate(['/']);
  return false;
};
