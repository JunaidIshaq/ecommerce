import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';
import { AdminApiService } from '../services/admin-api.service';
import { AdminCardComponent } from '../shared/admin-card/admin-card.component';
import { DashboardMetrics } from '../model/dashboard-metrics.model';

Chart.register(...registerables);

const MOCK_METRICS: DashboardMetrics = {
  todaySales: 12450,
  totalOrders: 328,
  newUsers: 57,
  salesTrend: [1200, 1500, 1700, 2100, 1900],
  topProducts: [
    { name: 'iPhone 15', sales: 120 },
    { name: 'AirPods Pro', sales: 95 },
    { name: 'Samsung S24', sales: 88 },
    { name: 'Gaming Mouse', sales: 76 }
  ]
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, AdminCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  @ViewChild('salesCanvas') salesCanvas!: ElementRef<HTMLCanvasElement>;

  metrics!: DashboardMetrics;
  chart: Chart | null = null;

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.setMetrics(MOCK_METRICS);

    this.adminApi.getDashboardMetrics().subscribe({
      next: data => this.setMetrics(data),
      error: () => console.warn('Using mock dashboard data')
    });
  }

  setMetrics(data: DashboardMetrics) {
    this.metrics = data;
    setTimeout(() => this.renderChart(data.salesTrend));
  }

  renderChart(trend: number[]) {
    if (!this.salesCanvas) return;

    if (this.chart) {
      this.chart.destroy();
    }

    const ctx = this.salesCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    this.chart = new Chart(ctx, {
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
        plugins: { legend: { display: true } }
      }
    });
  }
}
