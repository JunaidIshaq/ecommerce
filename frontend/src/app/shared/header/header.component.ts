import { Component } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { map } from 'rxjs/operators';
import {Observable} from 'rxjs';
import {User} from '../../models/user.model';
import {FormsModule} from '@angular/forms';
import {AsyncPipe, NgIf} from '@angular/common';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  imports: [
    FormsModule,
    AsyncPipe,
    NgIf,
    RouterLink
  ],
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  protected cartCount$: Observable<number>;
  protected user$: Observable<User | null>;
  protected query: string;
  protected menuOpen: boolean;

  constructor(private cart: CartService, private auth: AuthService) {
    this.cartCount$ = this.cart.getCart().pipe(map(() => this.cart.count()));
    this.user$ = this.auth.currentUser();
    this.query = '';
    this.menuOpen = false;
  }

  doSearch() {
    console.log('Searching:', this.query);
  }

  logout() {
    this.auth.logout();
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu() {
    this.menuOpen = false;
  }
}
