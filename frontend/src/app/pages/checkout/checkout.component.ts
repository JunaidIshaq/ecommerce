import {Component} from '@angular/core';
import {CartService} from '../../services/cart.service';
import {Address, User} from '../../models/user.model';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AuthService} from '../../services/auth.service';
import {Observable} from 'rxjs';
import {take} from 'rxjs/operators';

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
  couponCode = '';
  user$: Observable<User | null>;


  constructor(private cart: CartService, private router: Router, private authService: AuthService) {
    this.user$ = this.authService.currentUser();
  }

  total() { return this.cart.total(); }

  placeOrder() {
    if (this.placing) return;

    this.placing = true;

    this.user$.pipe(take(1)).subscribe({
      next: (user) => {
        if (!user || !user.id) {
          alert("User not logged in !");
          this.placing = false;
          return;
        }

        this.cart.checkout(user.id, this.couponCode).subscribe({
          next: (order) => {
            this.cart.clear().subscribe();
            alert("🎉 Order placed Successfully ! Order ID: " + order.order_number);
            this.router.navigate(['/']);
          },
          error: (err) => {
            console.error(err);
            alert(err.error?.message || "Checkout failed");
            this.placing = false;
          }
        });
      },
      error: () => {
        alert("Could not get user info");
        this.placing = false;
      }
    });
  }


}
