import { Injectable } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { User } from '../models/user.model';
import { safeLocalStorageGet, safeLocalStorageSet, safeLocalStorageRemove } from '../utils/browser-storage';

const AUTH_KEY = 'ecom_auth_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private user$ = new BehaviorSubject<User | null>(this.load());

  private load(): User | null {
    const raw = safeLocalStorageGet(AUTH_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  private persist(u: User | null) {
    if (u) safeLocalStorageSet(AUTH_KEY, JSON.stringify(u));
    else safeLocalStorageRemove(AUTH_KEY);
  }

  currentUser() { return this.user$.asObservable(); }

  login(email: string, password: string) {
    // ✅ Return the observable, don't subscribe here
    const mock: User = { id: 'u1', name: 'Demo User', email, token: 'mock-jwt' };
    return of(mock).pipe(
      delay(300)
    );
  }

  signup(name: string, email: string, password: string) {
    const mock: User = { id: 'u2', name, email, token: 'mock-jwt' };
    return of(mock).pipe(delay(400));
  }

  logout() {
    this.user$.next(null);
    this.persist(null);
  }
}
