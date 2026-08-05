import { Component, Inject, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ProfileService } from '../../services/profile.service';
import { Profile } from '../../models/profile.model';
import { environment } from '../../../environments/environment';

/**
 * The signed-in user's own profile.
 *
 * <p>Previously this rendered a hard-coded object with one developer's name and
 * email in it, so every visitor saw the same fictional account. It now loads the
 * real record from user-service, which derives identity from the access token
 * rather than from anything this page sends.
 *
 * <p>Password changes are not handled here. Keycloak owns credentials; the button
 * hands off to its account console, which already enforces the realm's password
 * policy and re-authentication rules. Re-implementing that would mean this app
 * touching passwords again, which the Keycloak migration deliberately stopped.
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {

  private readonly profileService = inject(ProfileService);

  profile: Profile | null = null;

  loading = true;
  /** Non-null when the load failed; shown instead of a silently empty page. */
  loadError: string | null = null;

  editing = false;
  saving = false;
  saveError: string | null = null;
  saved = false;

  /** Working copy, so cancelling an edit does not leave the view mutated. */
  form = { firstName: '', lastName: '', phone: '', country: '' };

  /** Keycloak's own account console - the only place passwords are handled. */
  readonly accountConsoleUrl = `${environment.keycloak.authority}/account/#/security/signingin`;

  constructor(
    private auth: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Under SSR there is no token, so the request would come back 401 and render
    // an error into the pre-rendered HTML. Let the browser do this one.
    if (!isPlatformBrowser(this.platformId)) {
      this.loading = false;
      return;
    }
    this.load();
  }

  load(): void {
    this.loading = true;
    this.loadError = null;

    this.profileService.getProfile().subscribe({
      next: profile => {
        this.profile = profile;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.loadError = err.status === 401
          ? 'Your session has expired. Please sign in again.'
          : 'We could not load your profile. Please try again.';
      }
    });
  }

  startEdit(): void {
    if (!this.profile) {
      return;
    }
    this.form = {
      firstName: this.profile.firstName ?? '',
      lastName: this.profile.lastName ?? '',
      phone: this.profile.phone ?? '',
      country: this.profile.country ?? ''
    };
    this.saveError = null;
    this.saved = false;
    this.editing = true;
  }

  cancelEdit(): void {
    this.editing = false;
    this.saveError = null;
  }

  save(): void {
    this.saving = true;
    this.saveError = null;

    this.profileService.updateProfile({
      firstName: this.form.firstName.trim(),
      lastName: this.form.lastName.trim(),
      phone: this.form.phone.trim(),
      country: this.form.country.trim()
    }).subscribe({
      // The server's response replaces local state rather than the submitted
      // form, so anything it normalised or rejected is visible immediately.
      next: profile => {
        this.profile = profile;
        this.saving = false;
        this.editing = false;
        this.saved = true;
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.saveError = err.status === 400
          ? 'Please check the values you entered.'
          : 'Saving failed. Please try again.';
      }
    });
  }

  /** Initials for the avatar placeholder when there is no uploaded photo. */
  get initials(): string {
    const first = this.profile?.firstName?.trim()?.charAt(0) ?? '';
    const last = this.profile?.lastName?.trim()?.charAt(0) ?? '';
    const fallback = this.profile?.email?.trim()?.charAt(0) ?? '?';
    return ((first + last) || fallback).toUpperCase();
  }

  logout(): void {
    this.profileService.clear();
    this.auth.logout();
  }
}
