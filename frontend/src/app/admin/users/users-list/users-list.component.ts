import {ChangeDetectorRef, Component, NgZone, OnInit} from '@angular/core';
import {CommonModule, NgFor, NgIf} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AdminApiService} from '../../services/admin-api.service';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AuthService} from '../../../services/auth.service';
import {take} from 'rxjs/operators';

const MOCK_USERS = [
  { id: 1, email: 'admin@shop.com', role: 'ADMIN', status: 'ACTIVE' },
  { id: 2, email: 'john.doe@gmail.com', role: 'USER', status: 'ACTIVE' },
  { id: 3, email: 'sara.khan@yahoo.com', role: 'USER', status: 'INACTIVE' },
  { id: 4, email: 'manager@shop.com', role: 'ADMIN', status: 'ACTIVE' },
  { id: 5, email: 'alex123@gmail.com', role: 'USER', status: 'ACTIVE' },
];


@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NgFor, NgIf, AdminCardComponent],
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit {
  users: any[] = [];
  searchTerm = '';
  roleFilter = '';
  userId: string | undefined;

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalUsers = 0;
  totalPages = 0;

  constructor(private adminApi: AdminApiService, private authService: AuthService, private cdr: ChangeDetectorRef, private zone: NgZone) {
    console.log('UsersListComponent: Constructor called');
  }

  ngOnInit() {
    // show dummy data instantly
    console.log('UsersListComponent ngOnInit called');
    this.loadUsers();
  }

  loadUsers() {
    console.log('loadUsers called');

    // First, show mock data immediately
    this.users = MOCK_USERS;
    this.totalUsers = this.users.length;
    this.totalPages = Math.ceil(this.totalUsers / this.pageSize);
    console.log('Loaded mock users:', this.users.length);

    // Then try to fetch from API
    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.userId = user?.id;
        console.log('User from auth:', user);
        console.log('Calling users API with userId:', this.userId);

        this.adminApi.getUsers(this.currentPage, this.pageSize, this.userId).subscribe({
          next: (data: any) => {
            this.zone.run(() => {
              console.log('Users API success:', data);

              if (data && data.items && Array.isArray(data.items)) {
                this.users = data.items;
                this.totalUsers = data.totalItems;
                this.totalPages = data.totalPages;
              }

              this.cdr.detectChanges();
            });
          },
          error: (err) => {
            console.warn('Users API failed, using mock data', err);
            // Already showing mock data, no action needed
          }
        });
      },
      error: (err) => {
        console.warn('Auth service error:', err);
        // Already showing mock data, no action needed
      }
    });
  }

  // Pagination methods
  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadUsers();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadUsers();
    }
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadUsers();
    }
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

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }
}
