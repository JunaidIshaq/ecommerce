import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {isPlatformBrowser} from '@angular/common';
import {BehaviorSubject, map, Observable, of} from 'rxjs';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {User} from '../models/user.model';
import {environment} from '../../environments/environment';

/**
 * Authentication facade over Keycloak (OpenID Connect).
 *
 * This class used to collect the password, AES-encrypt it with a key shipped in the
 * JavaScript bundle, and post it to auth-service in exchange for an HS256 token that
 * it stored in localStorage. All of that is gone:
 *
 *  - The password is now typed into Keycloak's own page, on a different origin. Our
 *    JavaScript cannot read it, so an XSS on the storefront can no longer harvest
 *    credentials.
 *  - Tokens are managed by angular-auth-oidc-client, which keeps them in memory and
 *    handles silent renewal. Nothing writes a token to localStorage any more; a token
 *    at rest in localStorage survives tab close and is readable by any script on the
 *    origin.
 *  - `login()` no longer takes credentials, because there is nothing here to give
 *    them to. It starts a redirect.
 *
 * The public shape (`currentUser()`, `logout()`, `getAccessToken()`, `isLoggedIn()`)
 * is kept so the ~10 components that depend on it did not all have to change at once.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.authPort}`;

  private userSubject = new BehaviorSubject<User | null>(null);
  user$: Observable<User | null> = this.userSubject.asObservable();

  constructor(
    private http: HttpClient,
    private oidc: OidcSecurityService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    // Under SSR there is no browser session, no storage and no redirect to resolve.
    // Subscribing to the OIDC service on the server yields a permanently
    // unauthenticated user and, worse, can hang hydration.
    if (!this.isBrowser()) {
      return;
    }

    // Single source of truth. The library re-emits on login, silent renew and
    // logout, so we never have to reconcile a cached copy by hand - which is what
    // the old localStorage 'user' key was, and it drifted.
    this.oidc.userData$.subscribe(({ userData }) => {
      this.userSubject.next(userData ? this.toUser(userData) : null);
    });
  }

  currentUser(): Observable<User | null> {
    return this.user$;
  }

  /** sessionStorage key holding where to land after a successful sign-in. */
  static readonly RETURN_URL_KEY = 'shopfast.returnUrl';

  /**
   * Starts the Authorization Code + PKCE redirect. Control leaves the app here; the
   * browser comes back to /callback. There is deliberately no credentials argument.
   *
   * `returnUrl` is parked in sessionStorage rather than passed through the OIDC
   * `state` parameter: the library owns `state` and uses it for CSRF protection, so
   * overwriting it would break the callback validation.
   */
  login(returnUrl?: string): void {
    if (!this.isBrowser()) return;
    if (returnUrl) {
      sessionStorage.setItem(AuthService.RETURN_URL_KEY, returnUrl);
    }
    this.oidc.authorize();
  }

  /**
   * Registration still goes through auth-service, because an account is a Keycloak
   * identity plus a profile row and something has to create both.
   *
   * The password is sent as plain text over TLS. The previous AES pre-encryption was
   * not a security control - the key was in the bundle - and Keycloak expects a real
   * password to hash with BCrypt.
   */
  register(userData: { email: string; password: string; firstName?: string; lastName?: string }) {
    return this.http.post(`${this.baseUrl}/api/v1/auth/register`, userData, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  /** Kept for callers still using the old name. */
  signup(userData: any) {
    return this.register(userData);
  }

  requestPasswordReset(email: string) {
    return this.http.post(`${this.baseUrl}/api/v1/auth/password-reset`, { email }, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  /**
   * Ends the Keycloak session, not just the local one. Clearing local state alone
   * leaves the SSO session alive, so the next "login" silently succeeds without ever
   * asking for a password - which looks exactly like a broken logout to a user on a
   * shared machine.
   */
  logout(): void {
    if (!this.isBrowser()) return;
    this.oidc.logoff().subscribe();
  }

  isLoggedIn(): Observable<boolean> {
    if (!this.isBrowser()) return of(false);
    return this.oidc.isAuthenticated$.pipe(map(({ isAuthenticated }) => isAuthenticated));
  }

  /**
   * Prefer the HTTP interceptor over calling this. It exists for the few places that
   * build a request by hand.
   *
   * Returns the token synchronously from the library's in-memory store; it is null
   * on the server and before the callback completes.
   */
  getAccessToken(): string | null {
    if (!this.isBrowser()) return null;
    return this.oidc.getAccessToken() as unknown as string | null;
  }

  getUserRoles(): Observable<string[]> {
    if (!this.isBrowser()) return of([]);
    // Roles come from the token Keycloak signed, not from anything we stored.
    // The previous implementation returned a hard-coded ['SUPER_ADMIN'], which made
    // the admin route guard a no-op for every visitor.
    return this.oidc.getPayloadFromIdToken().pipe(
      map((claims: any) => claims?.realm_access?.roles ?? [])
    );
  }

  /**
   * No-op retained so existing APP_INITIALIZER / ngOnInit callers keep compiling.
   * The library restores session state itself during checkAuth().
   */
  initializeAuth(): void {
    // intentionally empty
  }

  private toUser(claims: any): User {
    return {
      // `userId` is the mapper that links the Keycloak identity to the user_db row.
      // Fall back to `sub` for accounts created before that mapper existed.
      id: claims.userId ?? claims.sub,
      email: claims.email,
      name: claims.name ?? claims.preferred_username ?? '',
      role: (claims.realm_access?.roles ?? [])[0],
    } as User;
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }
}
