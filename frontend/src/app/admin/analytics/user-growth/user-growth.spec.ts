import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGrowth } from './user-growth';

describe('UserGrowth', () => {
  let component: UserGrowth;
  let fixture: ComponentFixture<UserGrowth>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGrowth]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGrowth);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
