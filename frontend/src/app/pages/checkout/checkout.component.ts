import { Component } from '@angular/core';
import { CartService } from '../../services/cart.service';
import { Address } from '../../models/user.model';
import { Router } from '@angular/router';
import {DecimalPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  imports: [
    DecimalPipe,
    FormsModule
  ],
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent {
  address: Address = { fullName:'', street:'', city:'', state:'', zip:'', country:'', phone:'' };
  placing = false;

  constructor(private cart: CartService, private router: Router) {}

  total() { return this.cart.total(); }

  placeOrder() {
    this.placing = true;
    // Mock place order
    setTimeout(() => {
      this.cart.clear();
      this.router.navigate(['/']);
    }, 800);
  }
}
