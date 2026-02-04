import { Component } from '@angular/core';
import { CommonModule, NgFor, NgIf, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';
import {RouterLink} from '@angular/router';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';

const MOCK_ORDERS = [
  {
    id: 1001,
    userEmail: 'john.doe@gmail.com',
    total: 249.99,
    status: 'PLACED',
    createdAt: new Date('2026-02-01T10:30:00')
  },
  {
    id: 1002,
    userEmail: 'sara.khan@yahoo.com',
    total: 89.50,
    status: 'SHIPPED',
    createdAt: new Date('2026-02-02T14:10:00')
  },
  {
    id: 1003,
    userEmail: 'alex123@gmail.com',
    total: 560.00,
    status: 'DELIVERED',
    createdAt: new Date('2026-01-29T09:15:00')
  },
  {
    id: 1004,
    userEmail: 'admin@shop.com',
    total: 120.75,
    status: 'CANCELLED',
    createdAt: new Date('2026-02-03T16:45:00')
  },
  {
    id: 1005,
    userEmail: 'mike.ross@mail.com',
    total: 42.00,
    status: 'PLACED',
    createdAt: new Date('2026-02-04T11:20:00')
  }
];


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

    this.orders = MOCK_ORDERS;
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
