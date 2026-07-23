import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgIf, NgForOf } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, NgIf, NgForOf],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.css']
})
export class OrderDetailComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.orderService.getOrderById(id).subscribe({
        next: (data) => {
          this.order = data;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to load order details.';
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
}
