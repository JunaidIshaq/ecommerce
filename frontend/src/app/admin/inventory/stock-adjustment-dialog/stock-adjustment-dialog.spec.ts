import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockAdjustmentDialog } from './stock-adjustment-dialog';

describe('StockAdjustmentDialog', () => {
  let component: StockAdjustmentDialog;
  let fixture: ComponentFixture<StockAdjustmentDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockAdjustmentDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StockAdjustmentDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
