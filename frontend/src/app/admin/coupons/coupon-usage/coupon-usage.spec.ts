import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CouponUsage } from './coupon-usage';

describe('CouponUsage', () => {
  let component: CouponUsage;
  let fixture: ComponentFixture<CouponUsage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CouponUsage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CouponUsage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
