import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgIf, NgForOf } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

const MOCK_ORDER: Order = {
  id: 'a8cf6a43-a750-4c58-b86b-97265eee50df',
  user_id: 'bd75776e-c3f7-48d2-943d-f9aee4196dd1',
  order_number: 'Order-F6B2C505',
  status: 'CONFIRMED',
  sub_total: '822.44',
  discount: '0.00',
  total_amount: '822.44',
  order_status: null,
  payment_method: 'COD',
  payment_status: 'CONFIRMED',
  items: [
    {
      quantity: 1,
      price: 822.44,
      images: [
        'https://picsum.photos/seed/5/600/400'
      ],
      product_id: '39cd2aab-0e04-461b-b7fd-42fb70b55d2b',
      product_name: 'Small Marble Bag',
      product_slug: 'sleek-paper-knife',
      product_description: 'Veritatis quasi et corrupti omnis culpa facilis modi repellat eum omnis iusto velit accusamus omnis quia aut voluptates consequatur architecto reprehenderit quas enim aut illo error alias nihil sit rem sunt consequatur quia pariatur veritatis quis laudantium nulla dolorem ipsa praesentium maiores et quaerat eos distinctio officia officia tenetur aliquam voluptas enim exercitationem eos eveniet maiores dolor animi ducimus vel assumenda laudantium incidunt fugiat sint at optio quod ut et libero occaecati itaque id aut dolorem quisquam officia magnam velit laudantium consectetur similique vero aut est dignissimos sit aut quibusdam maxime hic dicta sapiente sapiente enim adipisci et nulla aut est minus.',
      image_url: null
    }
  ],
  created_at: null,
  updated_at: '2026-08-01T12:11:38.871629Z'
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
          if (data?.id) {
            this.order = data;
          }
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.warn('[CustomerOrderDetail] API failed, keeping mock data', err);
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
    const total = this.toNumber(this.order?.total_amount);
    const discount = this.toNumber(this.order?.discount);
    return this.itemsTotal - discount;
  }

  toNumber(value: string | undefined | null): number {
    return parseFloat(value || '0');
  }

  formatCurrency(value: number | undefined): string {
    const v = value ?? 0;
    return '$' + v.toFixed(2);
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
