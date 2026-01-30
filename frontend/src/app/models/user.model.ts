export interface User {
  id?: string;
  name?: string;
  email?: string;
  token?: string;
  addresses?: Address[];
  role? : string;
}

export interface Address {
  fullName: string;
  street: string;
  city: string;
  state: string;
  zip: string;
  country: string;
  phone?: string;
}
