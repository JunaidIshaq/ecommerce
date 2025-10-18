import {Component, OnInit} from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import {Observable} from 'rxjs';
import {User} from '../../models/user.model';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-profile',
  imports: [CommonModule],       // ✅ Add this
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit{
  // protected user$: Observable<User | null>;
  user: any

  ngOnInit(): void {
    // Example data (replace later with real API or auth service)
    this.user = {
      name: 'Muhammad Junaid Ishaq',
      email: 'junaidnumlcs@gmail.com',
      joinedDate: 'April 2023',
      phone: '+92 300 1234567',
      country: 'Pakistan'
    };
  }

  constructor(private auth: AuthService, private router: Router) {
  }    // this.user$ = this.auth.currentUser();


  // logout(){ this.auth.logout(); this.router.navigate(['/']); }

  logout(){
    this.router.navigate(['/']);
  }
}
