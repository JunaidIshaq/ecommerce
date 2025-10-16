import {Component, OnInit} from '@angular/core';
import {Product} from '../../models/product.model';
import {ProductService} from '../../services/product.service';
import {CartService} from '../../services/cart.service';
import {RouterLink} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';


@Component({
  selector: 'app-products',
  templateUrl: './products.component.html',
  imports: [
    RouterLink,
    FormsModule,
    CommonModule
  ],
  styleUrls: ['./products.component.css']
})
export class ProductsComponent implements OnInit {
  products: Product[] = [];
  categories: string[] = [];
  q = ''; category = ''; sort: 'priceAsc'|'priceDesc'|'rating'|'' = '';

  loading = false;

  constructor(private svc: ProductService, private cart: CartService) {}

  ngOnInit(): void {
    this.svc.categories().subscribe(c => this.categories = c);
    this.search();
    this.loadProducts();
    this.loading = false;
  }


  loadProducts(): void {
    this.svc.list().subscribe({
      next: data => {
        this.products = data;
        this.loading = false;
      },
      error: err => {
        console.error('Failed to load products:', err);
        this.loading = false;
      }
    });
  }

  search() {
    this.loading = true;
    this.svc.listByQuery(this.q, this.category, this.sort || undefined)
      .subscribe(list => { this.products = list; this.loading = false; });
  }

  addToCart(p: Product) { this.cart.add(p, 1); }
}
