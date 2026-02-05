import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AuthService} from './auth.service';
import {User} from '../models/user.model';
import {environment} from '../../environments/environment';

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

  // Uses environment-based URL
  private baseUrl = environment.baseDomain
    ? `${environment.baseDomain}/api/v1/notification`
    : `http://localhost:${environment.notificationPort}/api/v1/notification`;

  user$: Observable<User | null>;


  constructor(private http: HttpClient, private authService: AuthService) {
    this.user$ = this.authService.currentUser();

  }

  /**
   * Fetch a user's notifications (paginated)
   */
  getUserNotifications(
    userId: string | undefined,
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
