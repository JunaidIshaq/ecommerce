import { Component, Inject, OnInit, PLATFORM_ID, computed, inject, signal } from '@angular/core';
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
 * <p>All state is held in signals, not plain fields. The application runs with
 * {@code provideZonelessChangeDetection()}, so there is no zone patching HTTP
 * callbacks to schedule a re-render: assigning to a plain property from inside a
 * subscribe updates the object but leaves the DOM untouched until something else
 * (a click, say) happens to trigger change detection. That is precisely how the
 * first version of this page got stuck on "Loading…" until you clicked it.
 * Signals notify the framework themselves, so they are the only safe way to
 * carry async results into the template here.
 *
 * <p>Password changes are not handled here. Keycloak owns credentials; the button
 * hands off to its account console, which already enforces the realm's password
 * policy and re-authentication rules.
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

  readonly profile = signal<Profile | null>(null);

  readonly loading = signal(true);
  /** Non-null when the load failed; shown instead of a silently empty page. */
  readonly loadError = signal<string | null>(null);

  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly saved = signal(false);

  /** Working copy, so cancelling an edit does not leave the view mutated. */
  form = { firstName: '', lastName: '', phone: '', country: '' };

  /** Keycloak's own account console - the only place passwords are handled. */
  readonly accountConsoleUrl = `${environment.keycloak.authority}/account/#/security/signingin`;

  /** Initials for the avatar placeholder when there is no uploaded photo. */
  readonly initials = computed(() => {
    const p = this.profile();
    const first = p?.firstName?.trim()?.charAt(0) ?? '';
    const last = p?.lastName?.trim()?.charAt(0) ?? '';
    const fallback = p?.email?.trim()?.charAt(0) ?? '?';
    return ((first + last) || fallback).toUpperCase();
  });

  constructor(
    private auth: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Under SSR there is no token, so the request would come back 401 and bake an
    // error into the pre-rendered HTML. Let the browser do this one.
    if (!isPlatformBrowser(this.platformId)) {
      this.loading.set(false);
      return;
    }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.profileService.getProfile().subscribe({
      next: profile => {
        this.profile.set(profile);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.loadError.set(err.status === 401
          ? 'Your session has expired. Please sign in again.'
          : 'We could not load your profile. Please try again.');
      }
    });
  }

  startEdit(): void {
    const p = this.profile();
    if (!p) {
      return;
    }
    this.form = {
      firstName: p.firstName ?? '',
      lastName: p.lastName ?? '',
      phone: p.phone ?? '',
      country: p.country ?? ''
    };
    this.saveError.set(null);
    this.saved.set(false);
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.saveError.set(null);
  }

  save(): void {
    this.saving.set(true);
    this.saveError.set(null);

    this.profileService.updateProfile({
      firstName: this.form.firstName.trim(),
      lastName: this.form.lastName.trim(),
      phone: this.form.phone.trim(),
      country: this.form.country.trim()
    }).subscribe({
      // The server's response replaces local state rather than the submitted
      // form, so anything it normalised or rejected is visible immediately.
      next: profile => {
        this.profile.set(profile);
        this.saving.set(false);
        this.editing.set(false);
        this.saved.set(true);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.saveError.set(err.status === 400
          ? 'Please check the values you entered.'
          : 'Saving failed. Please try again.');
      }
    });
  }

  logout(): void {
    this.profileService.clear();
    this.auth.logout();
  }
}
