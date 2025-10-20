export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  brand?: string;
  category: string;
  rating?: number;
  stock: number;
  images?: string[];
  createdAt?: string;
  tags?: string[];
}
