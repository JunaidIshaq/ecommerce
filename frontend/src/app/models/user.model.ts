import {Address} from './address.model';

export interface User {
  id?: string;
  name?: string;
  email?: string;
  token?: string;
  addresses?: Address[];
  role? : string;
}

