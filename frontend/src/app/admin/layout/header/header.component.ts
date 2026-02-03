import { Component } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';

@Component({
  selector: 'app-admin-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  pageTitle = 'Dashboard';

  constructor(private router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        const url = event.urlAfterRedirects.split('/').pop();
        this.pageTitle = this.formatTitle(url || 'dashboard');
      }
    });
  }

  formatTitle(text: string) {
    return text.charAt(0).toUpperCase() + text.slice(1);
  }
}
