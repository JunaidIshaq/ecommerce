import {Component} from '@angular/core';
import {CartService} from '../../services/cart.service';
import {User} from '../../models/user.model';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AuthService} from '../../services/auth.service';
import {Observable} from 'rxjs';
import {take} from 'rxjs/operators';
import {Address} from '../../models/address.model';
import {ToastService} from '../../services/toast.service';

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

  // Payment method: 'CARD' | 'COD' (matches backend enum)
  paymentMethod: 'CARD' | 'COD' = 'COD';

  // Card details
  cardNumber = '';
  cardHolderName = '';
  expiryDate = '';
  cvv = '';

  constructor(private cart: CartService, private router: Router, private authService: AuthService, private toast: ToastService) {
    this.user$ = this.authService.currentUser();
  }

  total() { return this.cart.total(); }

  formatCardNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\s/g, '').replace(/\D/g, '');
    value = value.match(/.{1,4}/g)?.join(' ') || value;
    this.cardNumber = value.substring(0, 19);
  }

  formatExpiryDate(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    if (value.length >= 2) {
      value = value.substring(0, 2) + '/' + value.substring(2, 4);
    }
    this.expiryDate = value.substring(0, 5);
  }

  isCardFormValid(): boolean {
    if (this.paymentMethod !== 'CARD') return true;
    return !!(
      this.cardNumber.replace(/\s/g, '').length >= 15 &&
      this.cardHolderName.trim().length >= 2 &&
      this.expiryDate.length === 5 &&
      this.cvv.length >= 3
    );
  }

  isFormValid(): boolean {
    return !!(
      this.address.fullName.trim() &&
      this.address.street.trim() &&
      this.address.city.trim() &&
      this.address.zip.trim() &&
      this.address.country.trim() &&
      this.isCardFormValid()
    );
  }

  placeOrder() {
    if (this.placing) return;

    if (!this.isFormValid()) {
      this.toast.error('Please fill all required fields');
      return;
    }

    this.placing = true;

    this.user$.pipe(take(1)).subscribe({
      next: (user) => {
        // No sign-in gate: a guest who filled an anonymous basket can pay. The
        // buyer is derived server-side from the token or the anonymous id, so
        // there is nothing here for the client to assert.
        const body: any = {
          paymentMethod: this.paymentMethod,
          fullName: this.address.fullName,
          street: this.address.street,
          city: this.address.city,
          state: this.address.state,
          zip: this.address.zip,
          country: this.address.country,
          phone: this.address.phone
        };

        if (this.couponCode) {
          body.couponCode = this.couponCode;
        }

        if (this.paymentMethod === 'CARD') {
          body.cardNumber = this.cardNumber.replace(/\s/g, '');
          body.cardHolderName = this.cardHolderName;
          body.expiryDate = this.expiryDate;
          body.cvv = this.cvv;
        }

        this.cart.checkoutWithPayment(user?.id ?? '', body).subscribe({
          next: (res: any) => {
            this.cart.clear().subscribe();
            // Backend may wrap order in { data: {...} } or return it directly
            const order = res?.data ?? res;
            console.log('[Checkout] Order response:', order);

            if (order.payment_status === 'CONFIRMED' || order.payment_status === 'SUCCESS') {
              this.toast.success('🎉 Order placed Successfully ! Order ID: ' + (order.order_number || order.id));
              if (order.id) {
                this.router.navigate(['/order', order.id]);
              } else {
                this.router.navigate(['/']);
              }
            } else if (order.payment_status === 'PAYMENT_FAILED' || order.payment_status === 'FAILED') {
              this.toast.error('Payment failed. Please try again.');
              this.placing = false;
            } else {
              this.toast.success('🎉 Order placed Successfully ! Order ID: ' + (order.order_number || order.id));
              if (order.id) {
                this.router.navigate(['/order', order.id]);
              } else {
                this.router.navigate(['/']);
              }
            }
          },
          error: (err) => {
            console.error(err);
            this.toast.error(err.error?.message || 'Checkout failed');
            this.placing = false;
          }
        });
      },
      error: () => {
        this.toast.error('Could not get user info');
        this.placing = false;
      }
    });
  }
}
