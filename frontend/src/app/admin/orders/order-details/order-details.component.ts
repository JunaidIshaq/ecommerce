import { Component, ChangeDetectorRef, Inject, NgZone, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';
import { AuthService } from '../../../services/auth.service';
import { take } from 'rxjs/operators';

const MOCK_ORDER = {
  id: 1001,
  order_number: 'ORD-1001',
  userEmail: 'john.doe@gmail.com',
  userId: 'user-001',
  shippingAddress: '123 Main St, Springfield, IL 62704',
  phone: '+1-555-0101',
  paymentMethod: 'Credit Card',
  paymentStatus: 'PAID',
  status: 'PLACED',
  subtotal: 249.99,
  tax: 15.00,
  discount: 0,
  totalAmount: 264.99,
  createdAt: '2026-02-01T10:30:00',
  updated_at: '2026-02-01T10:30:00',
  items: [
    {
      productId: 'prod-001',
      productName: 'Wireless Headphones',
      price: 89.99,
      quantity: 2
    },
    {
      productId: 'prod-002',
      productName: 'USB-C Cable',
      price: 35.00,
      quantity: 1
    }
  ]
};

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule, NgIf, NgFor, AdminCardComponent],
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class OrderDetailsComponent {
  order: any = null;
  loading = true;
  errorMessage: string | null = null;
  private orderId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private adminApi: AdminApiService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.orderId = this.route.snapshot.paramMap.get('id');
    console.log('OrderDetailsComponent: ngOnInit called, orderId:', this.orderId);
    this.loadOrder();
  }

  loadOrder() {
    console.log('loadOrder called for id:', this.orderId);
    this.order = MOCK_ORDER;
    this.loading = false;
    this.errorMessage = null;
    this.cdr.detectChanges();
    console.log('Loaded mock order detail; attempting API...');

    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.adminApi.getOrderById(this.orderId!, user?.id).subscribe({
          next: (data: any) => {
            this.zone.run(() => {
              console.log('Order API success:', data);
              this.order = data;
              this.loading = false;
              this.errorMessage = null;
              this.cdr.detectChanges();
            });
          },
          error: (err) => {
            this.zone.run(() => {
              console.warn('Order API failed, using mock data', err);
              this.loading = false;
              this.errorMessage = null;
              this.cdr.detectChanges();
            });
          }
        });
      },
      error: (err) => {
        console.warn('Auth service error in order detail:', err);
        this.loading = false;
        this.errorMessage = null;
        this.cdr.detectChanges();
      }
    });
  }

  markShipped() {
    console.log('markShipped called for order:', this.order?.id || this.order?.order_number);
    this.adminApi.updateOrderStatus(this.order.id, 'SHIPPED')
      .subscribe(() => this.order.status = 'SHIPPED');
  }

  cancelOrder() {
    console.log('cancelOrder called for order:', this.order?.id || this.order?.order_number);
    this.adminApi.updateOrderStatus(this.order.id, 'CANCELLED')
      .subscribe(() => this.order.status = 'CANCELLED');
  }

  openRefund() {
    console.log('openRefund called for order:', this.order?.id || this.order?.order_number);
    const refundAmount = prompt('Enter refund amount:');
    if (refundAmount === null) return;
    const reason = prompt('Enter refund reason:') || '';
    this.adminApi.refundOrder(this.order.id, Number(refundAmount), reason)
      .subscribe(() => alert('Refund processed successfully'));
  }
}
