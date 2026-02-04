import {Component, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {DatePipe, NgForOf} from '@angular/common';
import {AdminApiService} from '../../services/admin-api.service';

const MOCK_PAYMENTS = [
  {
    id: 1,
    orderId: 1001,
    userEmail: 'john.doe@gmail.com',
    amount: 249.99,
    status: 'SUCCESS',
    createdAt: new Date('2026-02-01T10:30:00')
  },
  {
    id: 2,
    orderId: 1002,
    userEmail: 'sara.khan@yahoo.com',
    amount: 89.50,
    status: 'FAILED',
    createdAt: new Date('2026-02-02T14:10:00')
  },
  {
    id: 3,
    orderId: 1003,
    userEmail: 'alex123@gmail.com',
    amount: 560.00,
    status: 'SUCCESS',
    createdAt: new Date('2026-01-29T09:15:00')
  },
  {
    id: 4,
    orderId: 1004,
    userEmail: 'admin@shop.com',
    amount: 120.75,
    status: 'REFUNDED',
    createdAt: new Date('2026-02-03T16:45:00')
  },
  {
    id: 5,
    orderId: 1005,
    userEmail: 'mike.ross@mail.com',
    amount: 42.00,
    status: 'SUCCESS',
    createdAt: new Date('2026-02-04T11:20:00')
  }
];


@Component({
  selector: 'payments-list',
  standalone: true,
  imports: [
    FormsModule,
    AdminCardComponent,
    DatePipe,
    NgForOf
  ],
  templateUrl: './payments-list.component.html',
  styleUrl: './payments-list.component.css'
})
export class PaymentsListComponent implements OnInit {


  ngOnInit() {
    this.payments = MOCK_PAYMENTS; // show mock instantly
    this.loadPayments();           // try real API
  }

  constructor(private adminApiService: AdminApiService) { }

  payments: any[] = [];
  statusFilter = '';

  loadPayments() {
    this.adminApiService.getPayments().subscribe({
      next: data => this.payments = data,
      error: () => {
        console.warn('Using mock payments data');
        this.payments = MOCK_PAYMENTS;
      }
    });
  }


  filteredPayments() {
    return this.payments.filter(p =>
      this.statusFilter ? p.status === this.statusFilter : true
    );
  }

}
