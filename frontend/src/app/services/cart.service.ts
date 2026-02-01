import {Injectable} from '@angular/core';
import {BehaviorSubject, switchMap, tap} from 'rxjs';
import {CartItem} from '../models/cart-item.model';
import {safeLocalStorageSet} from '../utils/browser-storage';
import {HttpClient} from '@angular/common/http';
import {AuthService} from './auth.service';

const STORAGE_KEY = 'ecom_cart_v1';

function getOrCreateAnonId(): string {
  let id = localStorage.getItem('anon_cart_id');
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem('anon_cart_id', id);
  }
  return id;
}


@Injectable({ providedIn: 'root' })
export class CartService {

  private baseUrl = 'https://shopfast.live'; // ✅ plural endpoint
  // private baseUrl = 'http://localhost:8088'; // ✅ plural endpoint

  private cartItems$ = new BehaviorSubject<CartItem[]>([]);

  // constructor() {
    // Optional: sync localStorage to BehaviorSubject changes
    // this.items$.subscribe(() => this.persist());
  // }
  constructor(private http: HttpClient,
              private auth: AuthService,
              @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  private isLoggedIn(): boolean {
    return !!this.auth.getAccessToken(); // or auth.isLoggedIn()
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

  // 🔹 Load cart from backend
  loadCart() {
    if (this.isLoggedIn()) {
      return this.http.get<CartItem[]>(`${this.baseUrl}/api/v1/cart`)
        .pipe(tap(items => this.cartItems$.next(items)));
    } else {
      const anonId = this.getOrCreateAnonId();
      return this.http.get<CartItem[]>(`${this.baseUrl}/api/v1/cart/guest`, { params: { anonId } })
        .pipe(tap(items => this.cartItems$.next(items)));
    }
  }

  // 🔹 Add item
  addToCart(productId: string, quantity: number = 1) {
    if (this.isLoggedIn()) {
      return this.http.post(`${this.baseUrl}/api/v1/cart/items`, { productId, quantity })
        .pipe(tap(() => this.loadCart().subscribe()));
    } else {
      const anonId = this.getOrCreateAnonId();
      return this.http.post(`${this.baseUrl}/api/v1/cart/guest/items?anonId=${anonId}`, { productId, quantity })
        .pipe(tap(() => this.loadCart().subscribe()));
    }
  }


  // 🔹 Remove item
  removeFromCart(productId: string) {
    if (this.isLoggedIn()) {
      return this.http.delete(`${this.baseUrl}/api/v1/cart/items/${productId}`)
        .pipe(tap(() => this.loadCart().subscribe()));
    } else {
      const anonId = this.getOrCreateAnonId();
      return this.http.delete(`${this.baseUrl}/api/v1/cart/guest/items/${productId}?anonId=${anonId}`)
        .pipe(tap(() => this.loadCart().subscribe()));
    }
  }


  // 🔹 Clear cart
  clear() {
    if (this.isLoggedIn()) {
      return this.http.delete(`${this.baseUrl}/api/v1/cart`)
        .pipe(tap(() => this.cartItems$.next([])));
    } else {
      const anonId = this.getOrCreateAnonId();
      return this.http.delete(`${this.baseUrl}/api/v1/cart/guest?anonId=${anonId}`)
        .pipe(tap(() => this.cartItems$.next([])));
    }
  }

  onLoginSuccess() {
    const anonId = localStorage.getItem('anon_cart_id');
    if (!anonId) return;

    this.http.post(`${this.baseUrl}/api/v1/cart/merge`, { anonId }).subscribe(() => {
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
    if (this.isLoggedIn()) {
      return this.http.put(
        `${this.baseUrl}/api/v1/cart/items/${productId}`,
        {},
        { params: { quantity } }
      ).pipe(switchMap(() => this.loadCart()));
    } else {
      const anonId = this.getOrCreateAnonId();
      return this.http.put(
        `${this.baseUrl}/api/v1/cart/guest/items/${productId}`,
        {},
        { params: { anonId, quantity } }
      ).pipe(switchMap(() => this.loadCart()));
    }
  }


  /** ✅ Remove item from cart */
  remove(id: string): void {
    const filtered = this.cartItems$.value.filter((i) => i.productId !== id);
    this.cartItems$.next(filtered);
    this.persist();
  }

}
