import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIf, NgForOf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';
import { Order } from '../../models/order.model';
import { PaginatedOrdersResponse } from '../../models/paginated-orders.model';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, NgIf, NgForOf, RouterLink],
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.css']
})
export class OrderHistoryComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  errorMessage = '';
  userId: string | undefined;

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;
  totalItems = 0;

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.userId = user?.id;
        this.orderService.getOrders(this.currentPage, this.pageSize, this.userId).subscribe({
          next: (data: PaginatedOrdersResponse) => {
            this.orders = data.items || [];
            this.totalPages = data.totalPages || 0;
            this.totalItems = data.totalItems || 0;
            this.currentPage = data.page || 1;
            this.loading = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Failed to load orders:', err);
            this.errorMessage = err.error?.message || 'Failed to load order history. Please try again later.';
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      }
    });
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.loadOrders();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  get pages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = this.clamp(this.currentPage - Math.floor(maxVisible / 2), 1, this.totalPages);
    let end = this.clamp(start + maxVisible - 1, 1, this.totalPages);

    if (end - start + 1 < maxVisible) {
      start = this.clamp(end - maxVisible + 1, 1, this.totalPages);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, value));
  }

  get paginationEnd(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return '';
    return status.toLowerCase();
  }

  getPaymentStatusClass(paymentStatus: string | undefined): string {
    if (!paymentStatus) return '';
    const status = paymentStatus.toLowerCase();
    if (status === 'paid' || status === 'confirmed' || status === 'success') {
      return 'payment-success';
    } else if (status === 'failed' || status === 'payment_failed') {
      return 'payment-failed';
    } else if (status === 'pending') {
      return 'payment-pending';
    }
    return '';
  }

  formatPaymentMethod(method: string | undefined): string {
    if (!method) return 'N/A';
    const methodMap: { [key: string]: string } = {
      'COD': 'Cash on Delivery',
      'CARD': 'Credit / Debit Card',
      'CREDIT_CARD': 'Credit Card',
      'DEBIT_CARD': 'Debit Card',
      'CASH': 'Cash on Delivery'
    };
    return methodMap[method.toUpperCase()] || method;
  }

  formatCurrency(value: number | undefined): string {
    const v = value ?? 0;
    return '$' + v.toFixed(2);
  }

  toNumber(value: string | undefined | null): number {
    return parseFloat(value || '0');
  }

  getPrimaryImage(item: any): string | null {
    if (item.images && item.images.length > 0) {
      return item.images[0];
    }
    if (item.image_url) {
      return item.image_url;
    }
    return null;
  }

  formatDate(dateString: string | null | undefined): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return 'N/A';
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
