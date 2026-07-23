export interface OrderItem {
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  image?: string;
}

export interface Order {
  id: number;
  order_number?: string;
  userEmail?: string;
  shippingAddress?: string;
  phone?: string;
  paymentMethod?: string;
  paymentStatus?: string;
  status?: string;
  subtotal?: number;
  tax?: number;
  discount?: number;
  totalAmount?: number;
  items?: OrderItem[];
  createdAt?: string;
}
