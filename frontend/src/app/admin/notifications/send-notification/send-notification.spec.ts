import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SendNotification } from './send-notification';

describe('SendNotification', () => {
  let component: SendNotification;
  let fixture: ComponentFixture<SendNotification>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SendNotification]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SendNotification);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
