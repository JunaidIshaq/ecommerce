import { Component, AfterViewInit } from '@angular/core';
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { Chart, registerables } from 'chart.js';
import { AdminApiService } from '../services/admin-api.service';
import { AdminCardComponent } from '../shared/admin-card/admin-card.component';
import { DashboardMetrics } from '../model/dashboard-metrics.model';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, AdminCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements AfterViewInit {

  metrics!: DashboardMetrics;

  constructor(private adminApi: AdminApiService) {}

  ngAfterViewInit() {
    this.adminApi.getDashboardMetrics().subscribe(data => {
      this.metrics = data;
      this.renderChart(data.salesTrend);
    });
  }

  renderChart(trend: number[]) {
    new Chart('salesChart', {
      type: 'line',
      data: {
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
        datasets: [{
          label: 'Sales',
          data: trend,
          borderColor: '#6C63FF',
          backgroundColor: 'rgba(108,99,255,0.1)',
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: true }
        }
      }
    });
  }
}
