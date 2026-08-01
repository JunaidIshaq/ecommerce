import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order } from '../models/order.model';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import { Inject, PLATFORM_ID } from '@angular/core';
import { PaginatedOrdersResponse } from '../models/paginated-orders.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private baseUrl = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.checkoutPort}`;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  private getUserId(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    try {
      const user = JSON.parse(userStr);
      return user?.id || null;
    } catch {
      return null;
    }
  }

  getOrderById(id: string | number): Observable<Order> {
    const userId = this.getUserId();
    const headers = userId ? new HttpHeaders().set('userId', userId) : new HttpHeaders();
    return this.http.get<Order>(`${this.baseUrl}/api/v1/order/${id}`, { headers });
  }

  getOrders(pageNumber: number = 1, pageSize: number = 10, userId?: string): Observable<PaginatedOrdersResponse> {
    const resolvedUserId = userId || this.getUserId();
    const headers = resolvedUserId ? new HttpHeaders().set('userId', resolvedUserId) : new HttpHeaders();
    return this.http.get<PaginatedOrdersResponse>(`${this.baseUrl}/api/v1/order`, {
      headers,
      params: {
        pageNumber: pageNumber.toString(),
        pageSize: pageSize.toString()
      }
    });
  }
}
