import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface Notification {
  id: number;
  userId: number;
  recipient: string;
  subject: string;
  content: string;
  type: string;
  channel: string;
  status: string;
  createdAt: string;
  sentAt: string | null;
  readAt: string | null;
  errorMessage: string | null;
}

// Pagination wrapper returned by Spring Boot
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;  // current page number
  size: number;    // page size
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  // TODO: Update if your backend port changes
  private baseUrl = 'https://shopfast.live/api/v1/notification';
  // private baseUrl = 'http://localhost:8091/api/v1/notification';

  constructor(private http: HttpClient) {}

  /**
   * Fetch a user's notifications (paginated)
   */
  getUserNotifications(
    userId: string = '28e2ac7f-09ef-4e7e-94df-042a987fa9c9',
    page: number = 1,
    size: number = 10
  ): Observable<Page<Notification>> {
    return this.http.get<Page<Notification>>(
      `${this.baseUrl}/user/${userId}?pageNumber=${page}&pageSize=${size}`
    );
  }

  /**
   * Mark a notification as read
   */
  markAsRead(id: number): Observable<Notification> {
    return this.http.patch<Notification>(
      `${this.baseUrl}/${id}/read`,
      {} // empty body
    );
  }
}
