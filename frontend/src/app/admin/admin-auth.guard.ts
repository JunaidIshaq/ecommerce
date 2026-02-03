import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const AdminAuthGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const roles = auth.getUserRoles();

  console.log('ADMIN GUARD ROLES:', roles); // 👈 check browser console

  if (roles?.includes('ADMIN') || roles?.includes('SUPER_ADMIN') ||
    roles?.includes('ROLE_ADMIN') || roles?.includes('ROLE_SUPER_ADMIN')) {
    return true;
  }

  router.navigate(['/']);
  return false;
};
