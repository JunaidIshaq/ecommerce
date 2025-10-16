import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CartItem } from '../models/cart-item.model';
import { Product } from '../models/product.model';
import { safeLocalStorageGet, safeLocalStorageSet } from '../utils/browser-storage';

const STORAGE_KEY = 'ecom_cart_v1';

@Injectable({ providedIn: 'root' })
export class CartService {
  private items$ = new BehaviorSubject<CartItem[]>(this.load());

  private load(): CartItem[] {
    try {
      const raw = safeLocalStorageGet(STORAGE_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  }

  private persist() {
    safeLocalStorageSet(STORAGE_KEY, JSON.stringify(this.items$.value));
  }

  getCart() { return this.items$.asObservable(); }

  add(product: Product, qty = 1) {
  }

  update(id: string, qty: number) { /* same as before */ }

  remove(id: string) { /* same as before */ }

  clear() { this.items$.next([]); this.persist(); }

  total(): number {
    return this.items$.value.reduce((sum, i) => sum + i.product.price * i.qty, 0);
  }

  count(): number {
    return this.items$.value.reduce((sum, i) => sum + i.qty, 0);
  }
}
