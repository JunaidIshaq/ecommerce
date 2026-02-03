import {Component} from '@angular/core';
import {CommonModule, NgFor, NgIf} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AdminApiService} from '../../services/admin-api.service';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';

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
    this.loadUsers();
  }

  loadUsers() {
    this.adminApi.getUsers().subscribe(data => this.users = data);
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
