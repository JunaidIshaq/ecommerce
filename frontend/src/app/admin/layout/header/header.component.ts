import { Component, HostListener } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  pageTitle = 'Dashboard';
  isProfileMenuOpen = false;

  constructor(private router: Router, private auth: AuthService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        const url = event.urlAfterRedirects.split('/').pop();
        this.pageTitle = this.formatTitle(url || 'dashboard');
      }
    });
  }

  toggleProfileMenu() {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  closeProfileMenu() {
    this.isProfileMenuOpen = false;
  }

  onProfileMenuClick(event: Event) {
    event.stopPropagation();
  }

  logout() {
    // No local navigation: signoutRedirect sends the browser to Keycloak's
    // end-session endpoint, which returns it to post_logout_redirect_uri.
    this.auth.logout();
  }

  @HostListener('document:click', ['$event.target'])
  onDocumentClick(target: EventTarget | null) {
    const profileWrapper = document.querySelector('.profile-wrapper');
    if (profileWrapper && target && !profileWrapper.contains(target as Node)) {
      this.closeProfileMenu();
    }
  }

  formatTitle(text: string) {
    return text.charAt(0).toUpperCase() + text.slice(1);
  }
}
