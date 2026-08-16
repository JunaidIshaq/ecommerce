import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { AuthService } from '../../services/auth.service';

/**
 * OIDC redirect landing page.
 *
 * Keycloak sends the browser back here with `?code=...&state=...`. This component's
 * only job is to let the library exchange that code for tokens and then get out of
 * the way. It renders a spinner because the exchange is a network round trip and an
 * empty page here looks like a crash.
 */
@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback">
      <div class="spinner" aria-hidden="true"></div>
      <p>{{ error || 'Signing you in…' }}</p>
    </div>
  `,
  styles: [`
    .callback {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; min-height: 60vh; gap: 1rem; color: #555;
    }
    .spinner {
      width: 2.5rem; height: 2.5rem; border-radius: 50%;
      border: 3px solid #e0e0e0; border-top-color: #666;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class CallbackComponent implements OnInit {
  error = '';

  constructor(
    private oidc: OidcSecurityService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // There is no redirect to process during server-side rendering, and the query
    // string is not available there either.
    if (!isPlatformBrowser(this.platformId)) return;

    this.oidc.checkAuth().subscribe({
      next: ({ isAuthenticated }) => {
        if (isAuthenticated) {
          const returnUrl = sessionStorage.getItem(AuthService.RETURN_URL_KEY) || '/';
          sessionStorage.removeItem(AuthService.RETURN_URL_KEY);
          this.router.navigateByUrl(returnUrl, { replaceUrl: true });
        } else {
          this.error = 'Sign-in did not complete. Redirecting…';
          this.router.navigateByUrl('/', { replaceUrl: true });
        }
      },
      error: () => {
        this.error = 'Sign-in failed. Redirecting…';
        this.router.navigateByUrl('/', { replaceUrl: true });
      }
    });
  }
}
