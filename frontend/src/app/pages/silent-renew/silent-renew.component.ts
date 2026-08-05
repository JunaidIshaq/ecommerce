import { Component, OnInit } from '@angular/core';
import { OidcService } from '../../services/oidc.service';

/**
 * /silent-renew — loaded inside a hidden iframe by the UserManager. It posts the
 * result back to the parent window and does nothing else.
 */
@Component({
  selector: 'app-silent-renew',
  standalone: true,
  template: ''
})
export class SilentRenewComponent implements OnInit {
  constructor(private oidc: OidcService) {}

  ngOnInit(): void {
    if (!this.oidc.isBrowser()) return;
    this.oidc.signinSilentCallback().catch(() => {
      // Swallowed deliberately: the parent window is notified via the
      // silentRenewError event, and there is no UI in this iframe to show.
    });
  }
}
