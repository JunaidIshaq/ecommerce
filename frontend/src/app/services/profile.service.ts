import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';
import { Profile, UpdateProfileRequest } from '../models/profile.model';

/**
 * Talks to the user-service profile endpoints through the gateway.
 *
 * <p>There is no user id in any of these URLs, by design. The server derives the
 * identity from the `sub` claim of the bearer token, so a caller cannot ask for
 * somebody else's profile by changing a path segment. The bearer token itself is
 * attached by the global HTTP interceptor, which is why nothing here touches
 * headers.
 */
@Injectable({ providedIn: 'root' })
export class ProfileService {

  private readonly http = inject(HttpClient);

  // In dev there is no baseDomain, and every service is reached through the
  // gateway on 8080 rather than its own port - the profile API only exists
  // behind the gateway because that is where the token is validated first.
  private readonly baseUrl = environment.baseDomain
    ? `${environment.baseDomain}/api/v1/user/profile`
    : `http://localhost:8080/api/v1/user/profile`;

  /**
   * Cached last-known profile so that components rendered after the first load
   * (the header, for instance) do not each trigger their own request.
   */
  private readonly profileSubject = new BehaviorSubject<Profile | null>(null);
  readonly profile$ = this.profileSubject.asObservable();

  /**
   * Loads the signed-in user's profile, creating it on the server if this is
   * their first visit since the Keycloak migration.
   */
  getProfile(): Observable<Profile> {
    return this.http.get<Profile>(this.baseUrl)
      .pipe(tap(profile => this.profileSubject.next(profile)));
  }

  /**
   * Saves the editable subset of the profile and returns the stored result.
   * The response - not the submitted form - is what updates the cache, so the
   * UI always shows what the server actually persisted.
   */
  updateProfile(request: UpdateProfileRequest): Observable<Profile> {
    return this.http.put<Profile>(this.baseUrl, request)
      .pipe(tap(profile => this.profileSubject.next(profile)));
  }

  /** Drops the cached profile; call on logout so the next user starts clean. */
  clear(): void {
    this.profileSubject.next(null);
  }
}
