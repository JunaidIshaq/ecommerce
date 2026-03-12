import {Component, OnInit} from '@angular/core';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AdminApiService} from '../../services/admin-api.service';
import {FormsModule} from '@angular/forms';
import {NgForOf, NgIf, DecimalPipe} from '@angular/common';

interface ProductResponse {
  items?: any[];
  totalItems?: number;
  totalPages?: number;
}

const MOCK_PRODUCTS = [
  {
    id: 1,
    name: 'iPhone 15 Pro',
    categoryName: 'Mobiles',
    price: 1199,
    active: true
  },
  {
    id: 2,
    name: 'Samsung Galaxy S24',
    categoryName: 'Mobiles',
    price: 999,
    active: true
  },
  {
    id: 3,
    name: 'Sony WH-1000XM5',
    categoryName: 'Headphones',
    price: 349,
    active: false
  },
  {
    id: 4,
    name: 'Gaming Mechanical Keyboard',
    categoryName: 'Accessories',
    price: 129,
    active: true
  },
  {
    id: 5,
    name: 'Apple Watch Series 9',
    categoryName: 'Wearables',
    price: 499,
    active: true
  }
];


@Component({
  selector: 'products-list',
  imports: [
    AdminCardComponent,
    FormsModule,
    NgForOf,
    NgIf,
    DecimalPipe
  ],
  templateUrl: './products-list.component.html',
  styleUrl: './products-list.component.css'
})
export class ProductsListComponent implements OnInit {

  constructor(private adminApiService: AdminApiService) { }

  products: any[] = [];
  searchTerm = '';

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalProducts = 0;
  totalPages = 0;

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.adminApiService.getProducts(this.currentPage, this.pageSize).subscribe({
      next: (data: any) => {
        // Check if data is wrapped in a response object
        if (data && data.items && Array.isArray(data.items)) {
          this.products = data.items;
          this.totalProducts = data.totalItems;
          this.totalPages = data.totalPages;
        } else if (Array.isArray(data)) {
          this.products = data;
          this.totalProducts = data.length;
          this.totalPages = Math.ceil(this.totalProducts / this.pageSize);
        } else {
          console.warn('Unexpected data format:', data);
          this.products = MOCK_PRODUCTS;
          this.totalProducts = this.products.length;
          this.totalPages = Math.ceil(this.totalProducts / this.pageSize);
        }
      },
      error: () => {
        console.warn('Using mock products data');
        this.products = MOCK_PRODUCTS;
        this.totalProducts = this.products.length;
        this.totalPages = Math.ceil(this.totalProducts / this.pageSize);
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
    this.adminApiService.updateProductStock(p.id, newStock).subscribe({
      next: () => this.loadProducts(),
      error: () => console.warn('Failed to update product stock')
    });
  }


  protected editProduct(p: any) {
    console.log('Edit product', p);
  }
}
