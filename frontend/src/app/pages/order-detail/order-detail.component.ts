import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgIf, NgForOf } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

const MOCK_ORDER: Order = {
  id: 1001,
  order_number: 'ORD-1001',
  userEmail: 'john.doe@gmail.com',
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
  selector: 'app-customer-order-detail',
  standalone: true,
  imports: [CommonModule, NgIf, NgForOf],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.css']
})
export class CustomerOrderDetailComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      console.log('[CustomerOrderDetail] ngOnInit called, orderId:', id);
      this.order = MOCK_ORDER;
      this.loading = false;
      this.cdr.detectChanges();
      console.log('[CustomerOrderDetail] Loaded mock order for id:', id, '; attempting API...');

      this.orderService.getOrderById(id).subscribe({
        next: (data) => {
          console.log('[CustomerOrderDetail] API success:', data);
          this.order = data;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.warn('[CustomerOrderDetail] API failed, using mock data', err);
          this.errorMessage = err.error?.message || 'Failed to load order details. Using cached data.';
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.errorMessage = 'Invalid order ID.';
      this.loading = false;
    }
  }

  get itemsTotal(): number {
    if (!this.order?.items?.length) return 0;
    return this.order.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  get orderTotal(): number {
    return (
      (this.order?.totalAmount ??
        (this.itemsTotal +
          (this.order?.tax || 0) -
          (this.order?.discount || 0))) || 0
    );
  }

  formatCurrency(value: number | undefined): string {
    const v = value ?? 0;
    return '$' + v.toFixed(2);
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

  goBack(): void {
    this.router.navigate(['/']);
  }
}
