import { Component, OnInit } from '@angular/core';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import {RouterLink} from '@angular/router';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  imports: [
    RouterLink,
    CommonModule
  ],
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  featured: Product[] = [];
  loading = true;

  constructor(private products: ProductService, private cart: CartService) {}

  ngOnInit(): void {
    this.products.featured().subscribe(list => {
      this.featured = list;
      this.loading = false;
    });
  }

  addToCart(p: Product) { this.cart.add(p, 1); }
}
