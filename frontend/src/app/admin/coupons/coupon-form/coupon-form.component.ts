import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';

@Component({
  selector: 'app-coupon-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './coupon-form.component.html',
  styleUrls: ['./coupon-form.component.css']
})
export class CouponFormComponent {

  @Input() coupon: any | null = null;
  @Output() closed = new EventEmitter<void>();   // ✅ renamed
  @Output() saved = new EventEmitter<void>();

  form: any = {
    code: '',
    type: 'PERCENT',
    value: 0,
    maxUsage: 0,
    expiry: ''
  };

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    if (this.coupon) {
      this.form = { ...this.coupon };
    }
  }

  submit() {
    const request = this.coupon
      ? this.adminApi.updateCoupon(this.coupon.id, this.form)
      : this.adminApi.createCoupon(this.form);

    request.subscribe(() => this.saved.emit());
  }

  close() {
    this.closed.emit();
  }
}
