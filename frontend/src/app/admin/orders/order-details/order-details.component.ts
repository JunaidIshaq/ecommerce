import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule, NgIf, NgFor, AdminCardComponent],
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class OrderDetailsComponent {
  order: any;

  constructor(private route: ActivatedRoute, private adminApi: AdminApiService) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    this.adminApi.getOrderById(id!).subscribe((data: any) => this.order = data);
  }

  markShipped() {
    this.adminApi.updateOrderStatus(this.order.id, 'SHIPPED')
      .subscribe(() => this.order.status = 'SHIPPED');
  }

  cancelOrder() {
    this.adminApi.updateOrderStatus(this.order.id, 'CANCELLED')
      .subscribe(() => this.order.status = 'CANCELLED');
  }

  protected openRefund() {

  }
}
