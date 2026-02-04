import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';

@Component({
  selector: 'app-coupons-list',
  standalone: true,
  imports: [CommonModule, DatePipe, AdminCardComponent],
  templateUrl: './coupons-list.component.html',
  styleUrls: ['./coupons-list.component.css']
})
export class CouponsListComponent {

  coupons: any[] = [];

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.loadCoupons();
  }

  loadCoupons() {
    this.adminApi.getCoupons().subscribe(data => this.coupons = data);
  }

  openCreate() {
    console.log('Open create coupon dialog');
    // Next step: open coupon-form component as modal
  }

  editCoupon(coupon: any) {
    console.log('Edit coupon', coupon);
    // Later: open dialog with existing values
  }

  toggleCoupon(coupon: any) {
    this.adminApi.toggleCoupon(coupon.id).subscribe(() => {
      this.loadCoupons();
    });
  }
}
