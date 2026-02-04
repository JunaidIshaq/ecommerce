import {Component} from '@angular/core';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AdminApiService} from '../../services/admin-api.service';
import {FormsModule} from '@angular/forms';
import {DatePipe, NgForOf} from '@angular/common';

@Component({
  selector: 'audit-logs',
  standalone: true,
  imports: [
    AdminCardComponent,
    FormsModule,
    DatePipe,
    NgForOf
  ],
  templateUrl: './audit-logs.component.html',
  styleUrl: './audit-logs.component.css'
})
export class AuditLogsComponent {

  logs: any[] = [];
  searchTerm = '';

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.adminApi.getAuditLogs().subscribe(data => this.logs = data);
  }

  filteredLogs() {
    return this.logs.filter(l =>
      l.action.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }
}
