import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CartItem } from '../models/cart-item.model';
import { Product } from '../models/product.model';
import { safeLocalStorageGet, safeLocalStorageSet } from '../utils/browser-storage';

const STORAGE_KEY = 'ecom_cart_v1';

@Injectable({ providedIn: 'root' })
export class CartService {
  private items$ = new BehaviorSubject<CartItem[]>(this.load());

  constructor() {
    // Optional: sync localStorage to BehaviorSubject changes
    this.items$.subscribe(() => this.persist());
  }

  /** Load saved cart from localStorage */
  private load(): CartItem[] {
    try {
      const raw = safeLocalStorageGet(STORAGE_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  }

  /** Save cart to localStorage */
  private persist(): void {
    safeLocalStorageSet(STORAGE_KEY, JSON.stringify(this.items$.value));
  }

  /** Observable stream for components to subscribe */
  getCart() {
    return this.items$.asObservable();
  }

  /** Current cart value (synchronous access) */
  get value(): CartItem[] {
    return this.items$.value;
  }

  /** ✅ Add or increase quantity */
  add(product: Product, qty = 1): void {
    const current = [...this.items$.value];
    const existing = current.find((item) => item.product.id === product.id);

    if (existing) {
      // Update quantity if already exists
      existing.qty += qty;
    } else {
      // Add new product
      current.push({ product, qty });
    }

    this.items$.next(current);
    this.persist();
  }

  /** ✅ Update quantity for a product */
  update(id: string, qty: number): void {
    const current = [...this.items$.value];
    const item = current.find((i) => i.product.id === id);

    if (item) {
      item.qty = qty;
      if (item.qty <= 0) {
        this.remove(id);
        return;
      }
      this.items$.next(current);
      this.persist();
    }
  }

  /** ✅ Remove item from cart */
  remove(id: string): void {
    const filtered = this.items$.value.filter((i) => i.product.id !== id);
    this.items$.next(filtered);
    this.persist();
  }

  /** ✅ Clear all items */
  clear(): void {
    this.items$.next([]);
    this.persist();
  }

  /** ✅ Cart total amount */
  total(): number {
    return this.items$.value.reduce(
      (sum, i) => sum + i.product.price * i.qty,
      0
    );
  }

  /** ✅ Total item count */
  count(): number {
    return this.items$.value.reduce((sum, i) => sum + i.qty, 0);
  }
}
