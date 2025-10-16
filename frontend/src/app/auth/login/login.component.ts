import { Component } from '@angular/core';
import { AuthService } from '../auth.service';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  standalone: true, // 👈 this line is critical
  imports: [NgIf, FormsModule,  RouterLink],
})
export class LoginComponent {
  credentials = { email: '', password: '' };

  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  login() {
    this.authService.login(this.credentials).subscribe({
      next: res => {
        localStorage.setItem('auth_token', res.token);
        this.router.navigate(['/']);
        alert('Login successful');
      },
      error: err => alert(err.error.message)
    });
  }

}
