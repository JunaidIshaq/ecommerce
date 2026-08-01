import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {isPlatformBrowser} from '@angular/common';
import {AuthResponse} from '../models/auth-response.model';
import {BehaviorSubject, Observable, tap, from, switchMap } from 'rxjs';
import {User} from '../models/user.model';
import {safeLocalStorageGet} from '../utils/browser-storage';
import {environment} from '../../environments/environment';
import {encryptPassword} from '../utils/password-encryption';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.authPort}`; // Uses environment-based URL


  // 🔥 Holds current logged in user
  private userSubject: BehaviorSubject<User | null>;
  user$: Observable<User | null>;

  // private baseUrl = `${environment_dev.apiUrl}/api/v1/auth`;
  private readonly AUTH_KEY = 'auth_token';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    // Initialize with null first, then restore from storage after platformId is available
    const initialUser = this.isBrowser() ? this.getUserFromStorage() : null;
    this.userSubject = new BehaviorSubject<User | null>(initialUser);
    this.user$ = this.userSubject.asObservable();
    console.log('AuthService: Constructor called, initial user:', initialUser);
  }

  currentUser(): Observable<User | null> {
    return this.user$;
  }


  private load(): User | null {
    const raw = safeLocalStorageGet(this.AUTH_KEY);
    return raw ? JSON.parse(raw) : null;
  }


  login(credentials: any) {
    return from(encryptPassword(credentials.password, environment.passwordEncryptionKey)).pipe(
      switchMap(encryptedPassword => {
        const encryptedCredentials = {
          ...credentials,
          password: encryptedPassword
        };

        return this.http.post<AuthResponse>(`${this.baseUrl}/api/v1/auth/login`, encryptedCredentials, {
          headers: { 'Content-Type': 'application/json' }
        }).pipe(
          tap(res => {
            this.persistTokens(res);

            // decode token to get user info
            const payload = JSON.parse(atob(res.accessToken.split('.')[1]));

            const user: User = {
              name: '',
              id: payload.sub,
              email: payload.email,
              role: payload.role
            };

            localStorage.setItem('user', JSON.stringify(user));
            this.userSubject.next(user);

          })
        );
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
    const refreshToken = this.isBrowser() ? localStorage.getItem('refresh_token') : null;

    this.http.post(`${this.baseUrl}/api/v1/auth/logout`, { refreshToken }, {
      headers: { 'Content-Type': 'application/json' }
    }).subscribe({
      next: () => this.clearAuth(),
      error: () => this.clearAuth()
    });
  }

  private clearAuth() {
    if (this.isBrowser()) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('user');
    }
    this.userSubject.next(null);
  }

  // Call this after app stabilizes to restore user from localStorage (works with SSR)
  initializeAuth(): void {
    console.log('initializeAuth called');

    // Use isPlatformBrowser for more reliable platform detection
    if (!this.isBrowser()) {
      console.log('initializeAuth: not browser, skipping');
      return;
    }

    const user = this.getUserFromStorage();
    console.log('initializeAuth: user from storage:', user);
    if (user) {
      this.userSubject.next(user);
    }
  }

  isLoggedIn(): boolean {
    return this.isBrowser() && !!localStorage.getItem(this.AUTH_KEY);
  }

  getAccessToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem('access_token');
  }

  getUserRoles() {
    return ['SUPER_ADMIN'];
  }
}
