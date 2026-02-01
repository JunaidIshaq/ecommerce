import {Component, signal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {HeaderComponent} from './shared/header/header.component';
import {FooterComponent} from './shared/footer/footer.component';
import {CartService} from './services/cart.service';
import {AuthService} from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  protected readonly title = signal('frontend');

  constructor(private cart: CartService, private auth: AuthService) {}

  ngOnInit() {
    this.auth.currentUser().subscribe(user => {
      this.cart.loadCart().subscribe();
    });
  }

}
