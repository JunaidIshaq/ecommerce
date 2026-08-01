export interface OrderItem {
  product_id: string;
  product_name?: string;
  price: number;
  quantity: number;
  images?: any;
  image_url?: string | null;
}

export interface Order {
  id: string;
  user_id: string;
  order_number: string;
  status: string;
  sub_total: string;
  discount: string;
  total_amount: string;
  order_status: string | null;
  payment_method: string;
  payment_status: string;
  items: OrderItem[];
  created_at: string;
  updated_at: string;
}
