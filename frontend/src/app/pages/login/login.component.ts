import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {CommonModule} from '@angular/common';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  standalone: true, // 👈 this line is critical
  imports: [CommonModule, FormsModule,  RouterLink],
})
export class LoginComponent {
  credentials = { email: '', password: '' };

  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  login() {
    if (!this.credentials.email || !this.credentials.password) return;

    this.loading = true;
    this.error = '';


    this.authService.login(this.credentials).subscribe({
      next: res => {
        this.router.navigate(['/']);
        alert('Login successful');
      },
      error: (err) => {
        this.error = err?.error?.message || 'Invalid email or password';
        this.loading = false;
      }
    });
  }

}
