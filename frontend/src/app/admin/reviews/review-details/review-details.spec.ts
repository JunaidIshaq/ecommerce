import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReviewDetails } from './review-details';

describe('ReviewDetails', () => {
  let component: ReviewDetails;
  let fixture: ComponentFixture<ReviewDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewDetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReviewDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
