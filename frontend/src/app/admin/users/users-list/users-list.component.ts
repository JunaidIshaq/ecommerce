import {Component} from '@angular/core';
import {CommonModule, NgFor, NgIf} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AdminApiService} from '../../services/admin-api.service';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';

const MOCK_USERS = [
  { id: 1, email: 'admin@shop.com', role: 'ADMIN', active: true },
  { id: 2, email: 'john.doe@gmail.com', role: 'USER', active: true },
  { id: 3, email: 'sara.khan@yahoo.com', role: 'USER', active: false },
  { id: 4, email: 'manager@shop.com', role: 'ADMIN', active: true },
  { id: 5, email: 'alex123@gmail.com', role: 'USER', active: true },
];


@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NgFor, NgIf, AdminCardComponent],
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent {
  users: any;
  searchTerm = '';
  roleFilter = '';

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    // show dummy data instantly
    console.log('UsersListComponent ngOnInit called');
    this.users = MOCK_USERS;
  }

  loadUsers() {
    this.adminApi.getUsers().subscribe({
      next: data => this.users = data,
      error: () => {
        console.warn('Using mock users data');
        this.users = MOCK_USERS;
      }
    });
  }

  filteredUsers() {
    return this.users.filter((user: { email: string; role: string; }) =>
      user.email.toLowerCase().includes(this.searchTerm.toLowerCase()) &&
      (this.roleFilter ? user.role === this.roleFilter : true)
    );
  }

  blockUser(id: number) {
    this.adminApi.blockUser(id).subscribe(() => this.loadUsers());
  }

  unblockUser(id: number) {
    this.adminApi.unblockUser(id).subscribe(() => this.loadUsers());
  }

  viewUser(user: any) {
    console.log('View user', user);
  }
}
