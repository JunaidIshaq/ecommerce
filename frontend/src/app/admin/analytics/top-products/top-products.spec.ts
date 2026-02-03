import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TopProducts } from './top-products';

describe('TopProducts', () => {
  let component: TopProducts;
  let fixture: ComponentFixture<TopProducts>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TopProducts]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TopProducts);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
