import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {DashboardMetrics} from '../model/dashboard-metrics.model';
import {environment} from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AdminApiService {

  private baseUrl = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.adminPort}`;

  private baseUrlOrder = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.checkoutPort}`;

  private baseUrlProduct = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.productPort}`;

  constructor(private http: HttpClient) {}

  getUsers(pageNumber: number = 1, pageSize: number = 10, userId?: string) {
    return this.http.get(`${this.baseUrlOrder}/api/v1/admin/users/pageNumber/${pageNumber}/pageSize/${pageSize}`, {
      headers: userId ? { 'userId': userId } : {}
    });
  }

  blockUser(id: number) {
    return this.http.put(`${this.baseUrl}/users/${id}/block`, {});
  }

  unblockUser(id: number) {
    return this.http.put(`${this.baseUrl}/users/${id}/unblock`, {});
  }

  getOrders(pageNumber: number = 1, pageSize: number = 10, userId?: string) {
    return this.http.get(`${this.baseUrlOrder}/api/v1/admin/orders/pageNumber/${pageNumber}/pageSize/${pageSize}`, {
      headers: userId ? { 'userId': userId } : {}
    });
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

  getProducts(pageNumber: number = 1, pageSize: number = 10, userId?: string) {
    return this.http.get(`${this.baseUrlOrder}/api/v1/admin/product/pageNumber/${pageNumber}/pageSize/${pageSize}`, {
      headers: userId ? { 'userId': userId } : {}
    });
  }

  toggleProduct(id: string) {
    return this.http.put(`${this.baseUrlOrder}/api/v1/admin/product/${id}/toggle`, {});
  }

  updateProductStock(id: string, stock: number) {
    return this.http.put(`${this.baseUrlOrder}/api/v1/admin/product/${id}/stock`, { stock });
  }

  getPayments() {
    return this.http.get<any[]>(`${this.baseUrl}/payments`);
  }


}
