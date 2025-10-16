import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';
import { Product } from '../models/product.model';
import {faker} from '@faker-js/faker/locale/en';

@Injectable({ providedIn: 'root' })
export class ProductService {
  // Mock dataset
  // private readonly products: Product[] = [
  //   { id: '1', name: 'Wireless Headphones', description: 'Noise cancelling over-ear', price: 129.99, category: 'Electronics', stock: 24, image: 'https://picsum.photos/seed/p1/640/480' },
  //   { id: '2', name: 'Running Shoes', description: 'Lightweight, breathable', price: 79.99, category: 'Fashion', stock: 50, image: 'https://picsum.photos/seed/p2/640/480' },
  //   { id: '3', name: 'Smart Watch', description: 'Heart-rate, GPS, 7-day battery', price: 199.00, category: 'Electronics', stock: 13, image: 'https://picsum.photos/seed/p3/640/480' },
  //   { id: '4', name: 'Backpack 28L', description: 'Water-resistant, laptop sleeve', price: 49.00, category: 'Accessories', stock: 70, image: 'https://picsum.photos/seed/p4/640/480' },
  //   { id: '5', name: 'Mechanical Keyboard', description: 'RGB, hot-swappable keys', price: 109.00, category: 'Electronics', stock: 8, image: 'https://picsum.photos/seed/p5/640/480' },
  //   { id: '6', name: 'Casual T-Shirt', description: '100% cotton, slim fit', price: 25.00, category: 'Fashion', stock: 100, image: 'https://picsum.photos/seed/p6/640/480' },
  //   { id: '7', name: 'Coffee Maker', description: '12-cup automatic drip', price: 89.99, category: 'Home Appliances', stock: 30, image: 'https://picsum.photos/seed/p7/640/480' },
  //   { id: '8', name: 'Yoga Mat', description: 'Non-slip, eco-friendly material', price: 35.00, category: 'Fitness', stock: 40, image: 'https://picsum.photos/seed/p8/640/480' },
  //   { id: '9', name: 'Gaming Mouse', description: '16000 DPI, programmable buttons', price: 59.99, category: 'Electronics', stock: 22, image: 'https://picsum.photos/seed/p9/640/480' },
  //   { id: '10', name: 'Bluetooth Speaker', description: 'Portable, waterproof, 10h battery', price: 75.00, category: 'Electronics', stock: 18, image: 'https://picsum.photos/seed/p10/640/480' }
  // ];
  private readonly products: Product[] = this.generateProducts(1000);


  private featuredIds = new Set(['1', '3', '5']);
  private products$ = new BehaviorSubject<Product[]>(this.products);

  list(): Observable<Product[]> {
    return this.products$.asObservable().pipe(delay(200));
  }

  listByQuery(q?: string, category?: string, sort?: 'priceAsc'|'priceDesc'|'rating'): Observable<Product[]> {
    return this.list().pipe(
      map(list => {
        let filtered = [...list];
        if (q && q.trim()) {
          const s = q.toLowerCase();
          filtered = filtered.filter(p =>
            p.name.toLowerCase().includes(s) ||
            p.description.toLowerCase().includes(s) ||
            (p.tags || []).some(t => t.toLowerCase().includes(s))
          );
        }
        if (category) {
          filtered = filtered.filter(p => p.category === category);
        }
        if (sort === 'priceAsc') filtered.sort((a,b)=>a.price-b.price);
        if (sort === 'priceDesc') filtered.sort((a,b)=>b.price-a.price);
        if (sort === 'rating') filtered.sort((a,b)=>(b.rating||0)-(a.rating||0));
        return filtered;
      })
    );
  }

  get(id: string): Observable<Product | undefined> {
    return of(this.products.find(p => p.id === id)).pipe(delay(150));
  }

  featured(): Observable<Product[]> {
    return of(this.products.filter(p => this.featuredIds.has(p.id))).pipe(delay(150));
  }

  categories(): Observable<string[]> {
    return of([...new Set(this.products.map(p => p.category))]);
  }

  private generateProducts(count: number): Product[] {
    const categories = ['Electronics', 'Fashion', 'Accessories', 'Fitness', 'Home Appliances', 'Toys'];
    const list: Product[] = [];

    for (let i = 1; i <= count; i++) {
      const category = faker.helpers.arrayElement(categories);
      list.push({
        id: i.toString(),
        name: faker.commerce.productName(),
        description: faker.commerce.productDescription(),
        price: parseFloat(faker.commerce.price({ min: 10, max: 500 })),
        category,
        stock: faker.number.int({ min: 5, max: 100 }),
        image: `https://picsum.photos/seed/p${i}/400/300`,
      });
    }

    return list;
  }
}
