import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {

  private apiUrl = 'https://shopfast.live/api/v1/product'; // ✅ plural endpoint
  // private apiUrl = 'http://localhost:8081/api/v1/product'; // ✅ plural endpoint

  constructor(private http: HttpClient) {}

  /**
   * ✅ Fetch products from backend with pagination
   */
  getAllProducts(pageNumber: number, pageSize: number, categoryId?: string | null): Observable<Product[]> {
    let params = new HttpParams()
      .set('pageNumber', pageNumber)
      .set('pageSize', pageSize);

    if(categoryId){
      params = params.set('categoryId', categoryId);
    }

    return this.http.get<Product[]>(this.apiUrl, { params });
  }

  /**
   * ✅ Get paginated + filtered product list (search, category, sort)
   * (frontend-only filtering after fetching)
   */
  listByQuery(
    q?: string,
    category?: string,
    sort?: 'priceAsc' | 'priceDesc' | 'rating',
    pageNumber = 1,
    pageSize = 20
  ): Observable<Product[]> {
    return this.getAllProducts(pageNumber, pageSize).pipe(
      map((list) => {
        let filtered = [...list];

        // 🔍 Search filter
        if (q?.trim()) {
          const s = q.toLowerCase();
          filtered = filtered.filter(
            (p) =>
              p.name.toLowerCase().includes(s) ||
              p.description.toLowerCase().includes(s)
          );
        }

        // 🏷️ Category filter
        if (category) {
          filtered = filtered.filter((p) => p.category === category);
        }

        // 🔢 Sorting
        if (sort === 'priceAsc') filtered.sort((a, b) => a.price - b.price);
        if (sort === 'priceDesc') filtered.sort((a, b) => b.price - a.price);
        if (sort === 'rating') filtered.sort((a, b) => (b.rating || 0) - (a.rating || 0));

        return filtered;
      })
    );
  }

  /**
   * ✅ Get single product by ID from backend
   */
  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  /**
   * ✅ Get featured products (example: first 6 or category-based)
   */
  getFeaturedProducts(): Observable<Product[]> {
    return this.getAllProducts(0, 6);
  }

  /**
   * ✅ Get all unique categories (optional helper)
   * - Usually fetched from backend if available
   */
  getCategories(): Observable<string[]> {
    return this.http.get<Product[]>(this.apiUrl).pipe(
      map(products => [...new Set(products.map(p => p.category))])
    );
  }
}
