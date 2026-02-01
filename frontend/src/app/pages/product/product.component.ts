import {ChangeDetectorRef, Component, Inject, NgZone, OnInit, PLATFORM_ID} from '@angular/core';
import {isPlatformBrowser, NgForOf, NgIf} from '@angular/common';
import {ProductService} from '../../services/product.service';
import {CartService} from '../../services/cart.service';
import {Router} from '@angular/router';
import {CategoryService} from '../../services/category.service';
import {SearchService} from '../../services/search.service';
import {debounceTime, distinctUntilChanged} from 'rxjs';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [NgIf, NgForOf],
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css']
})
export class ProductComponent implements OnInit {

  products: any[] = [];
  categories: any[] = [];
  selectedCategoryId: string | null = null;
  sortBy: string | null = null;
  sortOrder: string | null = null;
  currentPage = 1;
  pageSize = 12;
  totalPages = 0;
  totalItems = 0;
  visiblePages: number[] = [];
  maxVisible = 3;
  loading = true;
  errorMessage = '';
  private searchKeyword: string | undefined;

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private searchService: SearchService,
    private cart: CartService,
    private ngZone: NgZone,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.loadProducts(this.currentPage);
    this.loadCategories();

    this.searchService.search$
      .pipe(
        debounceTime(100),
        distinctUntilChanged()
      )
      .subscribe(keyword => {
        this.searchKeyword = keyword;
        this.currentPage = 1;
        this.loadProducts(1);
      });
  }


  /**
   * 🔹 Fetch paginated products from backend
   */
  loadProducts(page: number): void {
    this.loading = true;

    this.productService.getAllProducts(page, this.pageSize, this.searchKeyword, this.selectedCategoryId, this.sortBy, this.sortOrder).subscribe({
      next: (response: any) => {
        this.ngZone.run(() => {
          // Expecting backend response shape: { items, totalItems, totalPages, page, size }
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

  protected onCategoryChange(categoryId: string): void {
    this.selectedCategoryId = categoryId || null;
    this.currentPage = 1; // reset pagination
    this.loadProducts(1);
  }


  protected onSortChange(value: string) {
    if (!value) return;
    const [sortBy, sortOrder] = value.split(',');
    this.sortBy = sortBy; // reset pagination
    this.sortOrder = sortOrder; // reset pagination
    this.loadProducts(1);
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
   * 🔹 Update visible pagination numbers dynamically
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
    this.cart.addToCart(product.id, 1).subscribe();
  }

  /**
   * 🔹 Navigate to product details page
   */
  goToProductDetail(id: string): void {
    this.router.navigate(['/product', id]);
  }

  private loadCategories() {
    this.categoryService.getAllCategories(1, 10).subscribe({
      next: (response: any) => {
        this.ngZone.run(() => {
          // Expecting backend response shape: { items, totalItems, totalPages, page, size }
          this.categories = response.items || [];
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.loading = false;
          this.errorMessage = 'Failed to load categories.';
        });
        console.error('❌ Category load error:', err);
      }
    });
  }

}
