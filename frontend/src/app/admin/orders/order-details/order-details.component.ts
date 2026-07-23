import { Component, ChangeDetectorRef, Inject, PLATFORM_ID, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, NgIf, NgFor, isPlatformBrowser } from '@angular/common';
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
  selector: 'app-admin-order-details',
  standalone: true,
  imports: [CommonModule, NgIf, NgFor, AdminCardComponent],
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class AdminOrderDetailsComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  errorMessage: string | null = null;
  private orderId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private adminApi: AdminApiService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.orderId = this.route.snapshot.paramMap.get('id');
    console.log('[AdminOrderDetails] ngOnInit called, orderId:', this.orderId);
    this.loadOrder();
  }

  loadOrder() {
    console.log('[AdminOrderDetails] loadOrder called for id:', this.orderId);

    // Always show mock data immediately (works for both SSR and client)
    this.order = MOCK_ORDER;
    this.loading = false;
    this.errorMessage = null;
    this.cdr.detectChanges();

    // Only hit the API on the browser — localStorage/auth tokens are unavailable in SSR
    if (!isPlatformBrowser(this.platformId)) {
      console.log('[AdminOrderDetails] SSR: skipping API call, using mock data');
      return;
    }

    console.log('[AdminOrderDetails] Browser: attempting API call for order:', this.orderId);
    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.adminApi.getOrderById(this.orderId!, user?.id).subscribe({
          next: (res: any) => {
            console.log('[AdminOrderDetails] API success:', res);
            // Unwrap the API envelope: { success, status, message, data: { ... } }
            const orderData: Order | null = res?.data ?? null;
            if (orderData?.id) {
              // Only replace mock when we received a valid order
              this.order = orderData;
              this.cdr.detectChanges();
            }
          },
          error: (err) => {
            console.warn('[AdminOrderDetails] API failed, keeping mock data', err);
          }
        });
      },
      error: (err) => {
        console.warn('[AdminOrderDetails] Auth service error in order detail:', err);
      }
    });
  }

  getItemTotal(item: OrderItem): number {
    return item.quantity * item.price;
  }

  markShipped() {
    if (!this.order) return;
    console.log('[AdminOrderDetails] markShipped called for order:', this.order.order_number);
    this.adminApi.updateOrderStatus(this.order.id, 'SHIPPED')
      .subscribe(() => {
        if (this.order) this.order.status = 'SHIPPED';
        this.cdr.detectChanges();
      });
  }

  cancelOrder() {
    if (!this.order) return;
    console.log('[AdminOrderDetails] cancelOrder called for order:', this.order.order_number);
    this.adminApi.updateOrderStatus(this.order.id, 'CANCELLED')
      .subscribe(() => {
        if (this.order) this.order.status = 'CANCELLED';
        this.cdr.detectChanges();
      });
  }

  openRefund() {
    if (!this.order) return;
    console.log('[AdminOrderDetails] openRefund called for order:', this.order.order_number);
    const refundAmount = prompt('Enter refund amount:');
    if (refundAmount === null) return;
    const reason = prompt('Enter refund reason:') || '';
    this.adminApi.refundOrder(this.order.id, Number(refundAmount), reason)
      .subscribe(() => alert('Refund processed successfully'));
  }
}
