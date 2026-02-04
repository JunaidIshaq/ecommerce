import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';
import {CouponFormComponent} from '../coupon-form/coupon-form.component';

@Component({
  selector: 'app-coupons-list',
  standalone: true,
  imports: [CommonModule, DatePipe, AdminCardComponent, CouponFormComponent],
  templateUrl: './coupons-list.component.html',
  styleUrls: ['./coupons-list.component.css']
})
export class CouponsListComponent {

  coupons: any[] = [];
  showForm = false;
  selectedCoupon: any = null;

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.loadCoupons();
  }

  loadCoupons() {
    this.adminApi.getCoupons().subscribe(data => this.coupons = data);
  }

  openCreate() {
    this.selectedCoupon = null;
    this.showForm = true;
  }

  editCoupon(coupon: any) {
    this.selectedCoupon = coupon;
    this.showForm = true;
  }

  toggleCoupon(coupon: any) {
    this.adminApi.toggleCoupon(coupon.id).subscribe(() => {
      this.loadCoupons();
    });
  }

  closeForm() {
    this.showForm = false;
  }

  onSaved() {
    this.loadCoupons();
    this.closeForm();
  }
}
