import { Component } from '@angular/core';
import { CommonModule, NgFor, NgIf, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';
import {RouterLink} from '@angular/router';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NgFor, NgIf, DatePipe, AdminCardComponent, RouterLink],
  templateUrl: './orders-list.component.html',
  styleUrls: ['./orders-list.component.css']
})
export class OrdersListComponent {
  orders: any;
  searchTerm = '';
  statusFilter = '';

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.loadOrders();
  }

  loadOrders() {
    this.adminApi.getOrders().subscribe(data =>  this.orders = data);
  }

  filteredOrders() {
    return this.orders.filter((order: { id: { toString: () => string | string[]; }; status: string; }) =>
      order.id.toString().includes(this.searchTerm) &&
      (this.statusFilter ? order.status === this.statusFilter : true)
    );
  }

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
