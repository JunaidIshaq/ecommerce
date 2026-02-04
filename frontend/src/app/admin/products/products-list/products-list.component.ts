import {Component, OnInit} from '@angular/core';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AdminApiService} from '../../services/admin-api.service';
import {FormsModule} from '@angular/forms';
import {NgForOf} from '@angular/common';

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
    NgForOf
  ],
  templateUrl: './products-list.component.html',
  styleUrl: './products-list.component.css'
})
export class ProductsListComponent implements OnInit {

  constructor(private adminApiService: AdminApiService) { }

  products: any[] = [];
  searchTerm = '';

  ngOnInit() {
    this.products = MOCK_PRODUCTS; // show dummy instantly
    this.loadProducts();           // try real API
  }

  loadProducts() {
    this.adminApiService.getProducts().subscribe({
      next: data => this.products = data,
      error: () => {
        console.warn('Using mock products data');
        this.products = MOCK_PRODUCTS;
      }
    });
  }


  filteredProducts() {
    return this.products.filter(p =>
      p.name.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  toggleProduct(p: any) {
    this.adminApiService.toggleProduct(p.id).subscribe(() => this.loadProducts());
  }


  protected editProduct(p: any) {

  }
}
