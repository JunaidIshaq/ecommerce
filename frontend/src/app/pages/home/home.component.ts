import {ChangeDetectorRef, Component, Inject, NgZone, OnInit, PLATFORM_ID} from '@angular/core';
import { isPlatformBrowser, NgForOf, NgIf } from '@angular/common';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [NgIf, NgForOf, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  products: any[] = [];
  currentPage = 1;
  pageSize = 12;
  totalPages = 0;
  totalItems = 0;
  visiblePages: number[] = [];
  maxVisible = 3;
  loading = true;
  errorMessage = '';

  constructor(
    private productService: ProductService,
    private cart: CartService,
    private ngZone: NgZone,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.loadProducts(this.currentPage);
  }

  /**
   * 🔹 Load paginated products from backend
   */
  loadProducts(page: number): void {
    this.loading = true;

    this.productService.getAllProducts(page, this.pageSize).subscribe({
      next: (response: any) => {
        // Expecting backend JSON structure: { items, totalItems, totalPages, page, size }
        this.ngZone.run(() => {
          this.products = response.items || [];
          this.totalItems = response.totalItems || 0;
          this.totalPages = response.totalPages || 0;
          this.currentPage = response.page || 1;
          this.updateVisiblePages();
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.loading = false;
          this.errorMessage = 'Failed to load products.';
        });
        console.error('❌ Product load error:', err);
      }
    });
  }

  /**
   * 🔹 Handle pagination click
   */
  setPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.loadProducts(page);

    if (isPlatformBrowser(this.platformId)) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  /**
   * 🔹 Update visible pagination numbers
   */
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

  /**
   * 🛒 Add product to cart
   */
  addToCart(product: any): void {
    this.cart.add(product, 1);
  }

  /**
   * 🔹 Navigate to product details page
   */
  goToProductDetail(id: string): void {
    this.router.navigate(['/product', id]);
  }
}
