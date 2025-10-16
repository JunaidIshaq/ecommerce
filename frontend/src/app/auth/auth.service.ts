import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { AuthResponse } from '../models/auth-response.model';
import {environment_dev} from '../../environments/environment.dev'; // adjust path as needed

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = `${environment_dev.apiUrl}/api/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) private platformId: Object, private http: HttpClient) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  login(credentials: any) {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, credentials, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  signup(userData: any) {
    return this.http.post(`${this.baseUrl}/signup`, userData, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.removeItem(this.TOKEN_KEY);
    }
  }

  isLoggedIn(): boolean {
    return this.isBrowser && !!localStorage.getItem(this.TOKEN_KEY);
  }
}
