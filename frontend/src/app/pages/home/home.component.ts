import {Component, Inject, NgZone, OnInit, PLATFORM_ID} from '@angular/core';
import {isPlatformBrowser, NgForOf, NgIf} from '@angular/common';
import {ProductService} from '../../services/product.service';
import {CartService} from '../../services/cart.service';
import {Router, RouterLink} from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  imports: [
    NgIf,
    NgForOf,
    RouterLink
  ],
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  products: any[] = [];
  paginatedProducts: any[] = [];
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;
  visiblePages: number[] = [];
  maxVisible = 3;
  loading = true;
  errorMessage = '';

  constructor(
    private productService: ProductService,
    private cart: CartService,
    private ngZone: NgZone,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getAllProducts(this.currentPage, this.pageSize).subscribe({
      next: (data: any[]) => {
        this.ngZone.run(() => {
          this.products = data;
          this.totalPages = Math.ceil(this.products.length / this.pageSize);
          this.setPage(1);
          this.loading = false;
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.loading = false;
          this.errorMessage = 'Failed to load product.';
        });
        console.error('Product load error:', err);
      }
    });
  }

  setPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;

    this.currentPage = page;
    const start = (page - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedProducts = this.products.slice(start, end);

    // Update visible pagination range
    this.updateVisiblePages();

    if (isPlatformBrowser(this.platformId)) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  updateVisiblePages(): void {
    const half = Math.floor(this.maxVisible / 2);
    let startPage = Math.max(this.currentPage - half, 1);
    let endPage = startPage + this.maxVisible - 1;

    if (endPage > this.totalPages) {
      endPage = this.totalPages;
      startPage = Math.max(endPage - this.maxVisible + 1, 1);
    }

    this.visiblePages = [];
    for (let i = startPage; i <= endPage; i++) {
      this.visiblePages.push(i);
    }
  }

  addToCart(product: any): void {
    this.cart.add(product, 1);
  }

  goToProductDetail(id: string): void {
    this.router.navigate(['/product', id]);
  }
}
