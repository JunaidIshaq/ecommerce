import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportedReviews } from './reported-reviews';

describe('ReportedReviews', () => {
  let component: ReportedReviews;
  let fixture: ComponentFixture<ReportedReviews>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportedReviews]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReportedReviews);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
