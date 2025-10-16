import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.css',
  imports: [NgIf, FormsModule],
})
export class SignupComponent {
  error = '';
  user = {
    firstName: '',
    lastName: '',
    country: '',
    city: '',
    email: '',
    password: ''
  };

  constructor(private authService: AuthService, private router: Router) {}


  signup() {

    console.log(this.user)
    this.authService.signup(this.user).subscribe({
      next: res => {
        alert('Signup successful')
        this.router.navigate(['/login']);
      },
      error: err => alert(err.error.message)
    });
  }
}
