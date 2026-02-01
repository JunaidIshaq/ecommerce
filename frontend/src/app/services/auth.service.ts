// import { Injectable } from '@angular/core';
// import { BehaviorSubject, of } from 'rxjs';
// import { delay } from 'rxjs/operators';
// import { User } from '../models/user.model';
// import { safeLocalStorageGet, safeLocalStorageSet, safeLocalStorageRemove } from '../utils/browser-storage';
//
// const AUTH_KEY = 'ecom_auth_user';
//
// @Injectable({ providedIn: 'root' })
// export class AuthService {
//   private user$ = new BehaviorSubject<User | null>(this.load());
//
//   private load(): User | null {
//     const raw = safeLocalStorageGet(AUTH_KEY);
//     return raw ? JSON.parse(raw) : null;
//   }
//
//   private persist(u: User | null) {
//     if (u) safeLocalStorageSet(AUTH_KEY, JSON.stringify(u));
//     else safeLocalStorageRemove(AUTH_KEY);
//   }
//
//   currentUser() { return this.user$.asObservable(); }
//
//   login(email: string, password: string) {
//     // ✅ Return the observable, don't subscribe here
//     const mock: User = { id: 'u1', name: 'Demo User', email, token: 'mock-jwt' };
//     return of(mock).pipe(
//       delay(300)
//     );
//   }
//
//   signup(name: string, email: string, password: string) {
//     const mock: User = { id: 'u2', name, email, token: 'mock-jwt' };
//     return of(mock).pipe(delay(400));
//   }
//
//   logout() {
//     this.user$.next(null);
//     this.persist(null);
//   }
// }
import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {isPlatformBrowser} from '@angular/common';
import {AuthResponse} from '../models/auth-response.model';
import {BehaviorSubject, Observable, tap} from 'rxjs';
import {User} from '../models/user.model';
import {safeLocalStorageGet} from '../utils/browser-storage';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'https://shopfast.live'; // ✅ plural endpoint
  // private baseUrl = 'http://localhost:8087'; // ✅ plural endpoint


  // 🔥 Holds current logged in user
  private userSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  user$ = this.userSubject.asObservable();

  // private baseUrl = `${environment_dev.apiUrl}/api/v1/auth`;
  private readonly AUTH_KEY = 'auth_token';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    // Initialize user safely
    const user = this.isBrowser() ? this.getUserFromStorage() : null;
    this.userSubject = new BehaviorSubject<User | null>(user);
    this.user$ = this.userSubject.asObservable();
  }

  currentUser(): Observable<User | null> {
    return this.user$;
  }


  private load(): User | null {
    const raw = safeLocalStorageGet(this.AUTH_KEY);
    return raw ? JSON.parse(raw) : null;
  }


  login(credentials: any) {
    return this.http.post<AuthResponse>(`${this.baseUrl}/api/v1/auth/login`, credentials, {
      headers: { 'Content-Type': 'application/json' }
    }).pipe(
      tap(res => {
        this.persistTokens(res);

        // decode token to get user info
        const payload = JSON.parse(atob(res.accessToken.split('.')[1]));

        const user: User = {
          id: payload.sub,
          email: payload.email,
          role: payload.role
        };

        localStorage.setItem('user', JSON.stringify(user));
        this.userSubject.next(user);

      })
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refresh_token');

    return this.http.post<AuthResponse>(
      `${this.baseUrl}/api/v1/auth/refresh`,
      { refreshToken }
    ).pipe(
      tap(res => this.persistTokens(res))
    );
  }


  signup(userData: any) {
    return this.http.post(`${this.baseUrl}/signup`, userData, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  private getUserFromStorage(): User | null {
    if (!this.isBrowser()) return null;

    const data = localStorage.getItem('user');
    return data ? JSON.parse(data) : null;
  }

  private persistTokens(res: AuthResponse) {
    if (!this.isBrowser()) return;

    localStorage.setItem('access_token', res.accessToken);
    localStorage.setItem('refresh_token', res.refreshToken);
  }

  logout() {
    if (this.isBrowser()) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('user');
    }
    this.userSubject.next(null);
  }

  isLoggedIn(): boolean {
    return this.isBrowser() && !!localStorage.getItem(this.AUTH_KEY);
  }

  getAccessToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem('access_token');
  }

}
