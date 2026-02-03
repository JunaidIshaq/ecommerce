import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConversionRate } from './conversion-rate';

describe('ConversionRate', () => {
  let component: ConversionRate;
  let fixture: ComponentFixture<ConversionRate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConversionRate]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConversionRate);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
