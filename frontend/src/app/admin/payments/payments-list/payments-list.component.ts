import { Component } from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {DatePipe, NgForOf} from '@angular/common';
import {AdminApiService} from '../../services/admin-api.service';

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
export class PaymentsListComponent {

  constructor(private adminApiService: AdminApiService) { }

  payments: any[] = [];
  statusFilter = '';

  loadPayments() {
    this.adminApiService.getPayments().subscribe(data => this.payments = data);
  }

  filteredPayments() {
    return this.payments.filter(p =>
      this.statusFilter ? p.status === this.statusFilter : true
    );
  }

}
