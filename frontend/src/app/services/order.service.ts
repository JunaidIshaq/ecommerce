import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Order } from '../models/order.model';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import { Inject, PLATFORM_ID } from '@angular/core';
import { PaginatedOrdersResponse } from '../models/paginated-orders.model';
import { getGuestOrderToken } from '../utils/guest-order-tokens';

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

  /**
   * The owner is taken from the bearer token server-side; the `userId` header it
   * used to send proved nothing and let any caller read any order by id.
   *
   * A guest has no token, so the capability token issued at their checkout is
   * presented instead - it is what makes the confirmation page work without an
   * account, while keeping the order closed to anyone who merely knows the id.
   */
  getOrderById(id: string | number): Observable<Order> {
    const orderToken = getGuestOrderToken(String(id));
    const headers = orderToken
      ? new HttpHeaders().set('X-Order-Token', orderToken)
      : new HttpHeaders();
    return this.http.get<any>(`${this.baseUrl}/api/v1/order/${id}`, { headers }).pipe(
      map((res: any) => res?.data as Order)
    );
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
