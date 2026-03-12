import {ChangeDetectorRef, Component, NgZone, OnInit} from '@angular/core';
import { CommonModule, NgFor, NgIf, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';
import {RouterLink} from '@angular/router';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AuthService} from '../../../services/auth.service';
import {take} from 'rxjs/operators';

const MOCK_ORDERS = [
  {
    id: 1001,
    userEmail: 'john.doe@gmail.com',
    total: 249.99,
    status: 'PLACED',
    createdAt: new Date('2026-02-01T10:30:00')
  },
  {
    id: 1002,
    userEmail: 'sara.khan@yahoo.com',
    total: 89.50,
    status: 'SHIPPED',
    createdAt: new Date('2026-02-02T14:10:00')
  },
  {
    id: 1003,
    userEmail: 'alex123@gmail.com',
    total: 560.00,
    status: 'DELIVERED',
    createdAt: new Date('2026-01-29T09:15:00')
  },
  {
    id: 1004,
    userEmail: 'admin@shop.com',
    total: 120.75,
    status: 'CANCELLED',
    createdAt: new Date('2026-02-03T16:45:00')
  },
  {
    id: 1005,
    userEmail: 'mike.ross@mail.com',
    total: 42.00,
    status: 'PLACED',
    createdAt: new Date('2026-02-04T11:20:00')
  }
];


@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NgFor, NgIf, DatePipe, AdminCardComponent, RouterLink],
  templateUrl: './orders-list.component.html',
  styleUrls: ['./orders-list.component.css']
})
export class OrdersListComponent implements OnInit{
  orders: any[] = [];  // Initialize as empty array
  searchTerm = '';
  statusFilter = '';
  userId: string | undefined;

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalOrders = 0;
  totalPages = 0;

  constructor(private adminApi: AdminApiService, private authService: AuthService, private zone: NgZone, private cdr: ChangeDetectorRef) {
    console.error('OrdersListComponent: Constructor called');
  }

  ngOnInit() {
    console.log('OrdersListComponent: ngOnInit called');
    this.loadOrders();
  }

  loadOrders() {
    console.log('loadOrders called');

    // First, show mock data immediately
    this.orders = MOCK_ORDERS;
    this.totalOrders = this.orders.length;
    this.totalPages = Math.ceil(this.totalOrders / this.pageSize);
    console.log('Loaded mock orders:', this.orders.length);

    // Then try to fetch from API
    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.userId = user?.id;
        console.log('Calling orders API with userId:', this.userId);

        this.adminApi.getOrders(this.currentPage, this.pageSize, this.userId).subscribe({
          next: (data: any) => {
            this.zone.run(() => {
              console.log('Orders API success:', data);

              if (data && data.items && Array.isArray(data.items)) {
                this.orders = data.items;
                this.totalOrders = data.totalItems;
                this.totalPages = data.totalPages;
              }

              this.cdr.detectChanges();
            });
          },
          error: (err) => {
            console.warn('Orders API failed, using mock data', err);
            // Already showing mock data
          }
        });
      },
      error: (err) => {
        console.warn('Auth service error:', err);
        // Already showing mock data
      }
    });
  }

  // Pagination methods
  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadOrders();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadOrders();
    }
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadOrders();
    }
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


  // filteredOrders() {
  //   return this.orders.filter((order: { id: { toString: () => string | string[]; }; status: string; }) =>
  //     order.id.toString().includes(this.searchTerm) &&
  //     (this.statusFilter ? order.status === this.statusFilter : true)
  //   );
  // }

  cancelOrder(id: number) {
    if (confirm('Are you sure you want to cancel this order?')) {
      this.adminApi.updateOrderStatus(id, 'CANCELLED')
        .subscribe(() => this.loadOrders());
    }
  }

  viewOrder(order: any) {
    console.log('View order', order);
  }
}
