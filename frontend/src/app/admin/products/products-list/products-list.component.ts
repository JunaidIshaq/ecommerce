import {Component, OnInit, ChangeDetectorRef, NgZone, Inject, PLATFORM_ID} from '@angular/core';
import {CommonModule, NgFor, NgIf, DecimalPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {isPlatformBrowser} from '@angular/common';
import {AdminApiService} from '../../services/admin-api.service';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AuthService} from '../../../services/auth.service';
import {take} from 'rxjs/operators';

const MOCK_PRODUCTS = [
  {
    id: 1,
    name: 'iPhone 15 Pro',
    categoryName: 'Mobiles',
    categoryId: 1,
    price: 1199,
    stock: 50,
    rating: 4.8,
    images: []
  },
  {
    id: 2,
    name: 'Samsung Galaxy S24',
    categoryName: 'Mobiles',
    categoryId: 1,
    price: 999,
    stock: 35,
    rating: 4.5,
    images: []
  },
  {
    id: 3,
    name: 'Sony WH-1000XM5',
    categoryName: 'Headphones',
    categoryId: 2,
    price: 349,
    stock: 0,
    rating: 4.7,
    images: []
  },
  {
    id: 4,
    name: 'Gaming Mechanical Keyboard',
    categoryName: 'Accessories',
    categoryId: 3,
    price: 129,
    stock: 100,
    rating: 4.3,
    images: []
  },
  {
    id: 5,
    name: 'Apple Watch Series 9',
    categoryName: 'Wearables',
    categoryId: 4,
    price: 499,
    stock: 25,
    rating: 4.6,
    images: []
  }
];


@Component({
  selector: 'products-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NgFor, NgIf, DecimalPipe, AdminCardComponent],
  templateUrl: './products-list.component.html',
  styleUrls: ['./products-list.component.css']
})
export class ProductsListComponent implements OnInit {
  products: any[] = [];
  searchTerm = '';
  userId: string | undefined;

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalProducts = 0;
  totalPages = 0;

  constructor(
    private adminApi: AdminApiService,
    private authService: AuthService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    console.log('ProductsListComponent: Constructor called');
  }

  ngOnInit() {
    console.log('ProductsListComponent: ngOnInit called');
    this.loadProducts();
  }

  loadProducts() {
    console.log('loadProducts called');

    // First, show mock data immediately
    this.products = MOCK_PRODUCTS;
    this.totalProducts = this.products.length;
    this.totalPages = Math.ceil(this.totalProducts / this.pageSize);
    console.log('Loaded mock products:', this.products.length);

    // Then try to fetch from API
    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.userId = user?.id;
        console.log('User from auth:', user);
        console.log('Calling products API with userId:', this.userId);

        this.adminApi.getProducts(this.currentPage, this.pageSize, this.userId).subscribe({
          next: (data: any) => {
            this.zone.run(() => {
              console.log('Products API success:', data);

              if (data && data.items && Array.isArray(data.items)) {
                this.products = data.items;
                this.totalProducts = data.totalItems;
                this.totalPages = data.totalPages;
              } else if (Array.isArray(data)) {
                this.products = data;
                this.totalProducts = data.length;
                this.totalPages = Math.ceil(this.totalProducts / this.pageSize);
              }

              this.cdr.detectChanges();
            });
          },
          error: (err) => {
            console.warn('Products API failed, using mock data', err);
            // Already showing mock data, no action needed
          }
        });
      },
      error: (err) => {
        console.warn('Auth service error:', err);
        // Already showing mock data, no action needed
      }
    });
  }

  // Pagination methods
  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadProducts();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadProducts();
    }
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadProducts();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }


  filteredProducts() {
    return this.products.filter(p =>
      (p.name && p.name.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
      (p.description && p.description.toLowerCase().includes(this.searchTerm.toLowerCase()))
    );
  }

  toggleProduct(p: any) {
    // Toggle stock: if > 0, set to 0; if 0, set to 1
    const newStock = p.stock > 0 ? 0 : 1;
    this.adminApi.updateProductStock(p.id, newStock).subscribe({
      next: () => this.loadProducts(),
      error: () => console.warn('Failed to update product stock')
    });
  }


  protected editProduct(p: any) {
    console.log('Edit product', p);
  }
}
