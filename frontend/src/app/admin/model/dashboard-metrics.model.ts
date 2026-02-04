export interface DashboardMetrics {
  todaySales: number;
  totalOrders: number;
  newUsers: number;
  salesTrend: number[];
  topProducts: { name: string; sales: number }[];
}
