import { Component, ChangeDetectorRef, Inject, NgZone, PLATFORM_ID, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';
import { AuthService } from '../../../services/auth.service';
import { take } from 'rxjs/operators';

export interface OrderItem {
  quantity: number;
  price: number;
  product_id: string;
  product_name: string | null;
  product_slug: string | null;
  product_description: string | null;
  image_url: string | null;
  images: any[] | null;
}

export interface Order {
  id: string;
  discount: string;
  status: string;
  items: OrderItem[];
  user_id: string;
  order_number: string;
  sub_total: string;
  total_amount: string;
  created_at: string;
  updated_at: string;
}

const MOCK_ORDER: Order = {
  id: 'mock-order-id',
  discount: '0.00',
  status: 'CREATED',
  items: [
    {
      quantity: 1,
      price: 89.99,
      product_id: 'mock-product-id',
      product_name: 'Sample Product',
      product_slug: null,
      product_description: null,
      image_url: null,
      images: null
    }
  ],
  user_id: 'mock-user-id',
  order_number: 'Order-MOCK0001',
  sub_total: '89.99',
  total_amount: '89.99',
  created_at: new Date().toISOString(),
  updated_at: new Date().toISOString()
};

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule, NgIf, NgFor, AdminCardComponent],
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class OrderDetailsComponent implements OnInit {
  order: Order | null = null;
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
    console.log('Loaded mock order; attempting API...');

    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.adminApi.getOrderById(this.orderId!, user?.id).subscribe({
          next: (res: any) => {
            this.zone.run(() => {
              console.log('Order API success:', res);
              // Unwrap the API envelope: { success, status, message, data: { ... } }
              this.order = res?.data ?? res;
              this.loading = false;
              this.errorMessage = null;
              this.cdr.detectChanges();
            });
          },
          error: (err) => {
            this.zone.run(() => {
              console.warn('Order API failed, keeping mock data', err);
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

  getItemTotal(item: OrderItem): number {
    return item.quantity * item.price;
  }

  markShipped() {
    if (!this.order) return;
    console.log('markShipped called for order:', this.order.order_number);
    this.adminApi.updateOrderStatus(this.order.id, 'SHIPPED')
      .subscribe(() => {
        if (this.order) this.order.status = 'SHIPPED';
        this.cdr.detectChanges();
      });
  }

  cancelOrder() {
    if (!this.order) return;
    console.log('cancelOrder called for order:', this.order.order_number);
    this.adminApi.updateOrderStatus(this.order.id, 'CANCELLED')
      .subscribe(() => {
        if (this.order) this.order.status = 'CANCELLED';
        this.cdr.detectChanges();
      });
  }

  openRefund() {
    if (!this.order) return;
    console.log('openRefund called for order:', this.order.order_number);
    const refundAmount = prompt('Enter refund amount:');
    if (refundAmount === null) return;
    const reason = prompt('Enter refund reason:') || '';
    this.adminApi.refundOrder(this.order.id, Number(refundAmount), reason)
      .subscribe(() => alert('Refund processed successfully'));
  }
}
