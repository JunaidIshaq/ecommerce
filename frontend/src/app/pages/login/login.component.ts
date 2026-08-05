import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { OidcService } from '../../services/oidc.service';

/**
 * /login no longer collects credentials. The password is typed into a page
 * served by Keycloak on a different origin, so this application's JavaScript can
 * never see it. This route exists only to kick off the redirect for links that
 * still point at it.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `<p style="padding:2rem;text-align:center">Redirecting to sign in…</p>`
})
export class LoginComponent implements OnInit {
  constructor(
    private auth: AuthService,
    private oidc: OidcService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    if (!this.oidc.isBrowser()) return;
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
    this.auth.login(returnUrl);
  }
}
