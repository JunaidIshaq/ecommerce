import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NotificationLogs } from './notification-logs';

describe('NotificationLogs', () => {
  let component: NotificationLogs;
  let fixture: ComponentFixture<NotificationLogs>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationLogs]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NotificationLogs);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
