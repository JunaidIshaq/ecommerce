import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Category} from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {

  private apiUrl = 'https://shopfast.live/api/v1/category'; // ✅ plural endpoint
  // private apiUrl = 'http://localhost:8081/api/v1/product'; // ✅ plural endpoint

  constructor(private http: HttpClient) {}

  /**
   * ✅ Fetch products from backend with pagination
   */
  getAllCategories(pageNumber: number, pageSize: number): Observable<Category[]> {
    const params = new HttpParams()
      .set('pageNumber', pageNumber)
      .set('pageSize', pageSize);

    return this.http.get<Category[]>(this.apiUrl, { params });
  }
}
