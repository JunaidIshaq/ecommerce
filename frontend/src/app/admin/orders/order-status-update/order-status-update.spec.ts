import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderStatusUpdate } from './order-status-update';

describe('OrderStatusUpdate', () => {
  let component: OrderStatusUpdate;
  let fixture: ComponentFixture<OrderStatusUpdate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderStatusUpdate]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderStatusUpdate);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
