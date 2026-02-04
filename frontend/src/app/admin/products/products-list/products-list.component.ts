import { Component } from '@angular/core';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {HttpClient} from '@angular/common/http';
import {AdminApiService} from '../../services/admin-api.service';
import {FormsModule} from '@angular/forms';
import {NgForOf} from '@angular/common';

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
export class ProductsListComponent {

  constructor(private adminApiService: AdminApiService) { }

  products: any[] = [];
  searchTerm = '';

  loadProducts() {
    this.adminApiService.getProducts().subscribe(data => this.products = data);
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
