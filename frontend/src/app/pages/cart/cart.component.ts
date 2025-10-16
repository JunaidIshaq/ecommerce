import {Component} from '@angular/core';
import {CartService} from '../../services/cart.service';
import {CartItem} from '../../models/cart-item.model';
import {Router} from '@angular/router';
import {DecimalPipe} from '@angular/common';
import {Observable} from 'rxjs';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  imports: [
    DecimalPipe,
    CommonModule
  ],
  styleUrls: ['./cart.component.css']
})
export class CartComponent {
  protected items$: Observable<CartItem[]>;

  constructor(private cart: CartService, private router: Router) {

    this.items$ = this.cart.getCart();
  }


  update(id: string, v: string) { this.cart.update(id, Math.max(1, +v)); }
  remove(id: string) { this.cart.remove(id); }
  total() { return this.cart.total(); }
  checkout() { this.router.navigate(['/checkout']); }
}
