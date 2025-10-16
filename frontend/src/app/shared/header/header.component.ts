import { Component, EventEmitter, Output } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { map } from 'rxjs/operators';
import {FormsModule} from '@angular/forms';
import {AsyncPipe} from '@angular/common';
import {Observable} from 'rxjs';
import {User} from '../../models/user.model';
import {RouterLink} from '@angular/router';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  imports: [
    FormsModule,
    AsyncPipe,
    RouterLink,
    CommonModule
  ],
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  protected cartCount$: Observable<number>;
  protected user$: Observable<User | null>;
  protected query: string;


  constructor(private cart: CartService, private auth: AuthService) {
    this.cartCount$ = this.cart.getCart().pipe(map(() => this.cart.count()));
    this.user$ = this.auth.currentUser();
    this.query = '';

  }
  @Output() search = new EventEmitter<string>();
  doSearch() { this.search.emit(this.query.trim()); }
  logout() { this.auth.logout(); }
}
