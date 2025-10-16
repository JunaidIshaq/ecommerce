import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast { message: string; type?: 'info'|'success'|'error'; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  toasts$ = new BehaviorSubject<Toast | null>(null);
  show(message: string, type: Toast['type']='info') { this.toasts$.next({ message, type }); }
  clear() { this.toasts$.next(null); }
}
