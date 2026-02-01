import {Component} from '@angular/core';
import {CartService} from '../../services/cart.service';
import {Address} from '../../models/user.model';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-checkout',
  standalone: true,
  templateUrl: './checkout.component.html',
  imports: [
    CommonModule,
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
    if (this.placing) return;

    this.placing = true;

    setTimeout(() => {
      this.cart.clear();
      alert("🎉 Order placed successfully!");
      this.router.navigate(['/']);
    }, 800);
  }

}
