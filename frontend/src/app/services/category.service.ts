import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Category} from '../models/category.model';
import {environment} from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CategoryService {

  private apiUrl = environment.baseDomain
    ? `${environment.baseDomain}/api/v1/category`
    : `http://localhost:${environment.categoryPort}/api/v1/category`;

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
