import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RefundManagement } from './refund-management';

describe('RefundManagement', () => {
  let component: RefundManagement;
  let fixture: ComponentFixture<RefundManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RefundManagement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RefundManagement);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
