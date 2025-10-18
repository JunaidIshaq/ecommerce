import { Component, OnInit } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { CartItem } from '../../models/cart-item.model';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  imports: [
    FormsModule,
    RouterLink,
    NgForOf,
    NgIf
  ],
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {
  cartItems: CartItem[] = [];
  total = 0;

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.cartService.getCart().subscribe(items => {
      this.cartItems = items;
      this.updateTotal();
    });
  }

  updateQuantity(id: string, qty: number): void {
    if (qty < 1) return;
    this.cartService.update(id, qty);
    this.updateTotal();
  }

  removeItem(id: string): void {
    this.cartService.remove(id);
    this.updateTotal();
  }

  updateTotal(): void {
    this.total = this.cartService.total();
  }
}
