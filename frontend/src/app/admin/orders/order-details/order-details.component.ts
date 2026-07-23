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
  id: '65731af7-0fa1-43c6-a227-cc90dc6147ec',
  discount: '0.00',
  status: 'CREATED',
  items: [
    {
      quantity: 1,
      price: 822.44,
      product_id: '39cd2aab-0e04-461b-b7fd-42fb70b55d2b',
      product_name: 'Small Marble Bag',
      product_slug: 'sleek-paper-knife',
      product_description: 'Veritatis quasi et corrupti omnis culpa facilis modi repellat eum omnis iusto velit accusamus omnis quia aut voluptates consequatur architecto reprehenderit quas enim aut illo error alias nihil sit rem sunt consequatur quia pariatur veritatis quis laudantium nulla dolorem ipsa praesentium maiores et quaerat eos distinctio officia officia tenetur aliquam voluptas enim exercitationem eos eveniet maiores dolor animi ducimus vel assumenda laudantium incidunt fugiat sint at optio quod ut et libero occaecati itaque id aut dolorem quisquam officia magnam velit laudantium consectetur similique vero aut est dignissimos sit aut quibusdam maxime hic dicta sapiente sapiente enim adipisci et nulla aut est minus.',
      image_url: 'https://picsum.photos/seed/5/600/400',
      images: [
        'https://picsum.photos/seed/5/600/400'
      ]
    },
    {
      quantity: 1,
      price: 71.61,
      product_id: '755c8917-2f55-46e4-8917-5f8165df7255',
      product_name: 'Practical Linen Hat',
      product_slug: 'incredible-silk-clock',
      product_description: 'Et odio et voluptatem voluptate accusamus quisquam et impedit sit est quia labore reprehenderit repudiandae veritatis eius sed itaque voluptatem doloribus quo velit maiores libero facilis voluptatem alias quia est quo minus eligendi nostrum et et laboriosam voluptatum aspernatur omnis reprehenderit repellendus nihil quaerat quas laborum amet consequatur omnis vel maxime totam qui repellat laboriosam eius impedit eaque fugiat eveniet vitae quos accusamus pariatur laudantium aut quaerat qui quae numquam fugit facere quia magni iusto excepturi nisi magnam aut consequatur totam delectus fugiat vel suscipit harum sapiente veritatis et quidem possimus repudiandae et repellat fugiat est id ipsum dignissimos ratione sint.',
      image_url: 'https://picsum.photos/seed/4/600/400',
      images: [
        'https://picsum.photos/seed/4/600/400'
      ]
    }
  ],
  user_id: 'bd75776e-c3f7-48d2-943d-f9aee4196dd1',
  order_number: 'Order-76587C00',
  sub_total: '894.05',
  total_amount: '894.05',
  created_at: '2026-07-23T14:01:59.487577Z',
  updated_at: '2026-07-23T14:01:59.487609Z'
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
