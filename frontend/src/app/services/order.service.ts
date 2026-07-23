import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order } from '../models/order.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private baseUrl = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.checkoutPort}`;

  constructor(private http: HttpClient) {}

  getOrderById(id: string | number): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/api/v1/order/${id}`);
  }
}
