import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ProductService} from '../../services/product.service';
import {Product} from '../../models/product.model';
import {CartService} from '../../services/cart.service';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';


@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  imports: [
    FormsModule,
    CommonModule
  ],
  styleUrls: ['./product-detail.component.css']
})
export class ProductDetailComponent implements OnInit {
  product!: Product;
  qty = 1;

  constructor(private route: ActivatedRoute, private products: ProductService, private cart: CartService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.products.get(id).subscribe(p => this.product = <Product>p);
  }

  add() { if (this.product) this.cart.add(this.product, this.qty); }
}
