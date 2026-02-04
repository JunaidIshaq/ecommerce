import {Component, OnInit} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';
import {CouponFormComponent} from '../coupon-form/coupon-form.component';


const MOCK_COUPONS = [
  {
    id: 1,
    code: 'WELCOME10',
    type: 'PERCENT',
    value: 10,
    expiry: new Date('2026-03-01'),
    usedCount: 45,
    maxUsage: 100,
    active: true
  },
  {
    id: 2,
    code: 'FLAT50',
    type: 'AMOUNT',
    value: 50,
    expiry: new Date('2026-02-20'),
    usedCount: 12,
    maxUsage: 50,
    active: true
  },
  {
    id: 3,
    code: 'SUMMER20',
    type: 'PERCENT',
    value: 20,
    expiry: new Date('2025-12-31'),
    usedCount: 98,
    maxUsage: 100,
    active: false
  },
  {
    id: 4,
    code: 'FREESHIP',
    type: 'AMOUNT',
    value: 15,
    expiry: new Date('2026-04-15'),
    usedCount: 5,
    maxUsage: 200,
    active: true
  }
];



@Component({
  selector: 'app-coupons-list',
  standalone: true,
  imports: [CommonModule, DatePipe, AdminCardComponent, CouponFormComponent],
  templateUrl: './coupons-list.component.html',
  styleUrls: ['./coupons-list.component.css']
})
export class CouponsListComponent implements OnInit {

  coupons: any[] = [];
  showForm = false;
  selectedCoupon: any = null;

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.coupons = MOCK_COUPONS; // show dummy instantly
    this.loadCoupons();          // try real API
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
