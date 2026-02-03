import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';

@Component({
  selector: 'app-refund-dialog',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './refund-dialog.component.html',
  styleUrls: ['./refund-dialog.component.css']
})
export class RefundDialogComponent {
  @Input() orderId!: number;
  amount!: number;
  reason = '';

  constructor(private adminApi: AdminApiService) {}

  submit() {
    this.adminApi.refundOrder(this.orderId, this.amount, this.reason)
      .subscribe(() => {
        alert('Refund successful');
        this.close();
      });
  }


  close() {
    document.body.classList.remove('modal-open');
  }
}
