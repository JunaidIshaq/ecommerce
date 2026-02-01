import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ProductService} from '../../services/product.service';
import {CartService} from '../../services/cart.service';
import {NgForOf, NgIf} from '@angular/common';
import {Product} from '../../models/product.model';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  imports: [
    NgIf,
    NgForOf
  ],
  styleUrls: ['./product-detail.component.css']
})
export class ProductDetailComponent implements OnInit {
  product: Product | any;
  loading = true;
  errorMessage = '';
  activeTab = 'desc';
  stars: number[] = [];

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private cartService: CartService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productService.getProductById(id).subscribe({
        next: (p) => {
          if (p) {
            this.product = p;
            this.stars = Array(Math.round(p.rating || 4)).fill(0);
          } else {
            this.errorMessage = 'Product not found.';
          }
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorMessage = 'Failed to load product details.';
          this.loading = false;
        }
      });
    }
  }

  addToCart(product: any) {
    this.cartService.addToCart(product.id, 1).subscribe();
  }
}
