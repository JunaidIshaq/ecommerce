import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class BackendAdminService {

  private readonly baseUrl = environment.baseDomain
    ? `${environment.baseDomain}`
    : `http://localhost:${environment.adminPort}`;

  constructor(private http: HttpClient) {}

  getBaseUrl(): string {
    return this.baseUrl;
  }
}
