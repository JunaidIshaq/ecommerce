import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import {FormsModule} from '@angular/forms';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  imports: [
    FormsModule,
    CommonModule
  ],
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {
  name=''; email=''; password=''; loading=false; error='';

  constructor(private auth: AuthService, private router: Router) {}

  submit() {
    this.loading = true; this.error = '';
    this.auth.signup(this.name, this.email, this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => { this.error = 'Signup failed'; this.loading = false; }
    });
  }
}
