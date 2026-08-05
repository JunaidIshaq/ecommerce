import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

/**
 * Registration still goes through the backend: a ShopFast account is a Keycloak
 * identity *plus* a profile row, and something has to create both.
 *
 * The password travels in plain text over TLS. The old AES pre-encryption step
 * is gone — the key shipped in the bundle, so it was obfuscation, not encryption.
 */
@Component({
  selector: 'app-signup',
  standalone: true,
  templateUrl: './signup.component.html',
  imports: [FormsModule, CommonModule, RouterLink],
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {
  loading = false;
  error = '';

  credentials = {
    firstName: '',
    lastName: '',
    email: '',
    password: ''
  };
  confirmPassword = '';

  constructor(private auth: AuthService, private router: Router) {}

  signUp() {
    if (this.credentials.password !== this.confirmPassword) {
      this.error = 'Passwords do not match';
      return;
    }

    this.loading = true;
    this.error = '';

    this.auth.signup(this.credentials).subscribe({
      // The response is deliberately identical whether or not the email was
      // already taken, so there is nothing to branch on here. Hand the user to
      // Keycloak and let it decide.
      next: () => this.auth.login('/'),
      error: () => {
        this.error = 'Signup failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
