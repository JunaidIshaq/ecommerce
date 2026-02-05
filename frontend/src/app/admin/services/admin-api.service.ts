import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {DashboardMetrics} from '../model/dashboard-metrics.model';
import {User} from '../../models/user.model';

@Injectable({ providedIn: 'root' })
export class AdminApiService {

  // private baseUrl = environment.apiUrl + '/admin';

  private baseUrl = 'https://shopfast.live'; // ✅ plural endpoint

  private baseUrlOrder = 'http://localhost:8084';

  constructor(private http: HttpClient) {}

  getUsers() {
    return this.http.get<User>(`${this.baseUrl}/users`);
  }

  blockUser(id: number) {
    return this.http.put(`${this.baseUrl}/users/${id}/block`, {});
  }

  unblockUser(id: number) {
    return this.http.put(`${this.baseUrl}/users/${id}/unblock`, {});
  }

  getOrders(id: string | undefined) {
    let params = new HttpParams()
      .set('pageNumber', 1)
      .set('pageSize', 10);
    return this.http.get(`${this.baseUrl}/api/v1/order/internal/admin/orders/${id}/status/`, {params});
  }

  updateOrderStatus(id: number, status: string) {
    return this.http.put(`${this.baseUrl}/orders/${id}/status?status=${status}`, {});
  }

  getOrderById(id: string) {
    return this.http.get(`${this.baseUrl}/orders/${id}`);
  }

  refundOrder(orderId: number, amount: number, reason: string) {
    return this.http.post(`${this.baseUrl}/payments/refund`, {
      orderId,
      amount,
      reason
    });
  }


  getDashboardMetrics() {
    return this.http.get<DashboardMetrics>(`${this.baseUrl}/dashboard/metrics`);
  }

  getInventory() {
    return this.http.get<any[]>(`${this.baseUrl}/inventory`);
  }

  updateStock(productId: number, quantity: number) {
    return this.http.put(`${this.baseUrl}/inventory/${productId}/stock?quantity=${quantity}`, {});
  }

  getCoupons() {
    return this.http.get<any[]>(`${this.baseUrl}/coupons`);
  }

  createCoupon(data: any) {
    return this.http.post(`${this.baseUrl}/coupons`, data);
  }

  updateCoupon(id: number, data: any) {
    return this.http.put(`${this.baseUrl}/coupons/${id}`, data);
  }

  toggleCoupon(id: number) {
    return this.http.put(`${this.baseUrl}/coupons/${id}/toggle`, {});
  }

  getReviews() {
    return this.http.get<any[]>(`${this.baseUrl}/reviews`);
  }

  approveReview(id: number) {
    return this.http.put(`${this.baseUrl}/reviews/${id}/approve`, {});
  }

  deleteReview(id: number) {
    return this.http.delete(`${this.baseUrl}/reviews/${id}`);
  }

  getNotifications() {
    return this.http.get<any[]>(`${this.baseUrl}/notifications`);
  }

  sendNotification(data: any) {
    return this.http.post(`${this.baseUrl}/notifications`, data);
  }

  getAuditLogs() {
    return this.http.get<any[]>(`${this.baseUrl}/audit-logs`);
  }

  getProducts() {
    return this.http.get<any[]>(`${this.baseUrl}/products`);
  }

  toggleProduct(id: number) {
    return this.http.put(`${this.baseUrl}/products/${id}/toggle`, {});
  }

  getPayments() {
    return this.http.get<any[]>(`${this.baseUrl}/payments`);
  }


}
