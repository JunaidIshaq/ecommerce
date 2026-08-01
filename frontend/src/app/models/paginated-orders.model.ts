import { Order } from './order.model';

export interface PaginatedOrdersResponse {
  items: Order[];
  totalItems: number;
  totalPages: number;
  page: number;
  size: number;
}
