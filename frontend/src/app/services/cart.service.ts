import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import {BehaviorSubject, switchMap, tap} from 'rxjs';
import {CartItem} from '../models/cart-item.model';
import {safeLocalStorageSet} from '../utils/browser-storage';
import {rememberGuestOrderToken} from '../utils/guest-order-tokens';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {AuthService} from './auth.service';
import {isPlatformBrowser} from '@angular/common';
import {Address} from '../models/address.model';
import {environment} from '../../environments/environment';

const STORAGE_KEY = 'ecom_cart_v1';



@Injectable({ providedIn: 'root' })
export class CartService {

  private baseUrlCart = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.cartPort}`;

  private baseUrlCheckout = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.checkoutPort}`;

  private cartItems$ = new BehaviorSubject<CartItem[]>([]);

  // constructor() {
    // Optional: sync localStorage to BehaviorSubject changes
    // this.items$.subscribe(() => this.persist());
  // }
  constructor(private http: HttpClient,
              private auth: AuthService,
              @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  /**
   * Decides between the authenticated cart endpoints and the anonymous
   * /api/v1/cart/guest/** ones.
   *
   * Presence of a token is not enough. A stale token left in storage after a
   * session expires made this return true, so an anonymous shopper was sent to
   * POST /api/v1/cart/items - a path the gateway only opens to a valid bearer -
   * and every add-to-cart came back 401. Anything we cannot positively verify as
   * a live session is therefore treated as a guest: the guest endpoints always
   * work, so failing this way costs nothing, while failing the other way breaks
   * the storefront for logged-out users.
   */
  private isLoggedIn(): boolean {
    if (!isPlatformBrowser(this.platformId)) return false;

    const token = this.auth.getAccessToken();
    if (!token) return false;

    const expiry = this.tokenExpiry(token);
    // A token whose exp we cannot read is not trusted to pick the user branch.
    if (expiry === null) return false;

    // Small skew so a token expiring mid-flight does not race the request.
    return expiry - 5_000 > Date.now();
  }

  /** Milliseconds-since-epoch of the JWT `exp` claim, or null if unreadable. */
  private tokenExpiry(token: string): number | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      const exp = JSON.parse(json)?.exp;
      return typeof exp === 'number' ? exp * 1000 : null;
    } catch {
      return null;
    }
  }

  /** Save cart to localStorage */
  private persist(): void {
    safeLocalStorageSet(STORAGE_KEY, JSON.stringify(this.cartItems$.value));
  }


   getOrCreateAnonId(): string | '' {
    if (!isPlatformBrowser(this.platformId)) return '';
    let id = localStorage.getItem('anon_cart_id');
    if (!id) {
      id = crypto.randomUUID();
      localStorage.setItem('anon_cart_id', id);
    }
    return id;
  }

  /** Observable stream for components to subscribe */
  getCart() {
    return this.cartItems$.asObservable();
  }


  /**
   * Identity is no longer asserted by the caller. order-service takes the buyer
   * from the bearer token when there is one and from X-Anon-Id otherwise, so the
   * same call works signed-in or as a guest - and a client can no longer check
   * out in someone else's name by setting a header.
   */
  checkout(userId: string, couponCode?: string, address?: Address) {
    const headers = this.cartHeaders();

    const body: any = {};

    if (couponCode) {
      body.couponCode = couponCode;
    }

    if (address) {
      body.fullName = address.fullName;
      body.street = address.street;
      body.city = address.city;
      body.state = address.state;
      body.zip = address.zip;
      body.country = address.country;
      body.phone = address.phone;
    }
    return this.http.post<any>(`${this.baseUrlCheckout}/api/v1/order/checkout`, body, { headers })
      .pipe(tap(res => this.rememberOrderToken(res)));
  }

  checkoutWithPayment(userId: string, body: any) {
    return this.http.post<any>(`${this.baseUrlCheckout}/api/v1/order/checkout`, body, { headers: this.cartHeaders() })
      .pipe(tap(res => this.rememberOrderToken(res)));
  }

  /**
   * A guest checkout returns a one-time token; it is the only thing that will
   * open the order afterwards, so it has to be kept the moment it arrives.
   */
  private rememberOrderToken(response: any): void {
    const order = response?.data ?? response;
    const token = order?.access_token ?? order?.accessToken;
    if (order?.id && token) {
      rememberGuestOrderToken(String(order.id), token);
    }
  }

  /**
   * Headers for a cart call.
   *
   * X-Anon-Id is sent whenever there is no live session. cart-service uses the
   * verified token in preference to this header, so sending it costs nothing when
   * signed in - but it is what lets a guest use the same URLs, instead of the
   * client having to pick a separate /guest path and getting it wrong.
   */
  private cartHeaders(): HttpHeaders {
    let headers = new HttpHeaders();
    // Sent unconditionally. cart-service prefers the verified token, so this is
    // ignored for a real session; withholding it whenever the client *believed*
    // it was signed in meant a token the server rejected left the request with no
    // identity at all, and the add-to-cart failed instead of falling back to the
    // guest cart.
    const anonId = this.getOrCreateAnonId();
    if (anonId) headers = headers.set('X-Anon-Id', anonId);
    return headers;
  }

  // 🔹 Load cart from backend
  loadCart() {
    return this.http.get<CartItem[]>(`${this.baseUrlCart}/api/v1/cart`, { headers: this.cartHeaders() })
      .pipe(tap(items => this.cartItems$.next(items)));
  }

  // 🔹 Add item
  addToCart(productId: string, quantity: number = 1) {
    return this.http.post(`${this.baseUrlCart}/api/v1/cart/items`, { productId, quantity },
      { headers: this.cartHeaders() })
      .pipe(tap(() => this.loadCart().subscribe()));
  }


  // 🔹 Remove item
  removeFromCart(productId: string) {
    return this.http.delete(`${this.baseUrlCart}/api/v1/cart/items/${productId}`,
      { headers: this.cartHeaders() })
      .pipe(tap(() => this.loadCart().subscribe()));
  }


  // 🔹 Clear cart
  clear() {
    return this.http.delete(`${this.baseUrlCart}/api/v1/cart`, { headers: this.cartHeaders() })
      .pipe(tap(() => this.cartItems$.next([])));
  }

  onLoginSuccess() {
    // Reached from auth callbacks that can also run during server-side
    // rendering, where localStorage does not exist and would throw.
    if (!isPlatformBrowser(this.platformId)) return;

    const anonId = localStorage.getItem('anon_cart_id');
    if (!anonId) return;

    this.http.post(`${this.baseUrlCart}/api/v1/cart/merge`, { anonId }).subscribe(() => {
      localStorage.removeItem('anon_cart_id');
      this.loadCart().subscribe();
    });
  }



  // 🔹 Total price
  total(): number {
    return this.cartItems$.value.reduce((sum, i) => sum + i.price * i.quantity, 0);
  }

  // 🔹 Total item count
  count(): number {
    return this.cartItems$.value.reduce((sum, i) => sum + i.quantity, 0);
  }

  /** ✅ Update quantity for a product */
  updateQuantity(productId: string, quantity: number) {
    return this.http.put(
      `${this.baseUrlCart}/api/v1/cart/items/${productId}`,
      {},
      { params: { quantity }, headers: this.cartHeaders() }
    ).pipe(switchMap(() => this.loadCart()));
  }


  /** ✅ Remove item from cart */
  remove(id: string): void {
    const filtered = this.cartItems$.value.filter((i) => i.productId !== id);
    this.cartItems$.next(filtered);
    this.persist();
  }

}
