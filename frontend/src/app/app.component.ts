import {Component, signal} from '@angular/core';
import {Router, RouterOutlet} from '@angular/router';
import {HeaderComponent} from './shared/header/header.component';
import {FooterComponent} from './shared/footer/footer.component';
import {CartService} from './services/cart.service';
import {AuthService} from './services/auth.service';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, NgIf],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  protected readonly title = signal('frontend');

  constructor(private cart: CartService, private auth: AuthService, private router: Router) {}

  ngOnInit() {
    this.auth.currentUser().subscribe(user => {
      this.cart.loadCart().subscribe();
    });
  }

  isAdminRoute(): boolean {
    return this.router.url.startsWith('/admin');
  }

}
