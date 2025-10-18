import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';
import { Product } from '../models/product.model';
import { faker } from '@faker-js/faker/locale/en';

@Injectable({ providedIn: 'root' })
export class ProductService {
  // ✅ Automatically generate a list of 1000 fake product on startup
  private readonly products: Product[] = this.generateProducts(1000);

  // ✅ Reactive observable for all product
  private products$ = new BehaviorSubject<Product[]>(this.products);

  // ✅ Featured product (example subset)
  private featuredIds = new Set(['1', '3', '5']);

  constructor() {
    console.log(`[ProductService] Generated ${this.products.length} products`);
  }

  /**
   * ✅ Get all product as an observable (auto-load)
   * Automatically emits when service is initialized
   */
  list(): Observable<Product[]> {
    return this.products$.asObservable();
  }

  /**
   * ✅ Search, filter, and sort product dynamically
   */
  listByQuery(
    q?: string,
    category?: string,
    sort?: 'priceAsc' | 'priceDesc' | 'rating'
  ): Observable<Product[]> {
    return this.list().pipe(
      map((list) => {
        let filtered = [...list];

        if (q?.trim()) {
          const s = q.toLowerCase();
          filtered = filtered.filter(
            (p) =>
              p.name.toLowerCase().includes(s) ||
              p.description.toLowerCase().includes(s) ||
              (p.tags || []).some((t) => t.toLowerCase().includes(s))
          );
        }

        if (category) {
          filtered = filtered.filter((p) => p.category === category);
        }

        if (sort === 'priceAsc') filtered.sort((a, b) => a.price - b.price);
        if (sort === 'priceDesc') filtered.sort((a, b) => b.price - a.price);
        if (sort === 'rating') filtered.sort((a, b) => (b.rating || 0) - (a.rating || 0));

        return filtered;
      })
    );
  }

  /**
   * ✅ Get a single product by ID
   */
  get(id: string): Observable<Product | undefined> {
    return of(this.products.find((p) => p.id === id));
  }

  /**
   * ✅ Get featured product
   */
  featured(): Observable<Product[]> {
    return of(this.products.filter((p) => this.featuredIds.has(p.id)));
  }

  /**
   * ✅ Get all unique categories
   */
  categories(): Observable<string[]> {
    return of([...new Set(this.products.map((p) => p.category))]);
  }

  /**
   * 🧠 Generate fake product using faker
   */
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
        rating: faker.number.float({ min: 2, max: 5}),
        tags: [category.toLowerCase(), faker.commerce.department()],
        image: `https://picsum.photos/seed/p${i}/400/300`,
      });
    }

    return list;
  }
}
