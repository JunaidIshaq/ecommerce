import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminManagement } from './admin-management';

describe('AdminManagement', () => {
  let component: AdminManagement;
  let fixture: ComponentFixture<AdminManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminManagement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminManagement);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
