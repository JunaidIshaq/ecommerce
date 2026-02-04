import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UsersListComponent } from './users/users-list/users-list.component';
import { OrdersListComponent } from './orders/orders-list/orders-list.component';
import { ProductsListComponent } from './products/products-list/products-list.component';
import { InventoryListComponent } from './inventory/inventory-list/inventory-list.component';
import { CouponsListComponent } from './coupons/coupons-list/coupons-list.component';
import { ReviewsListComponent } from './reviews/reviews-list/reviews-list.component';
import { PaymentsListComponent } from './payments/payments-list/payments-list.component';
import { NotificationsListComponent } from './notifications/notifications-list/notifications-list.component';
import { AdminAuthGuard } from './admin-auth.guard';
import {OrderDetailsComponent} from './orders/order-details/order-details.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [AdminAuthGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'users', component: UsersListComponent },
      { path: 'orders', component: OrdersListComponent },
      { path: 'products', component: ProductsListComponent },
      { path: 'inventory', component: InventoryListComponent },
      { path: 'coupons', component: CouponsListComponent },
      { path: 'reviews', component: ReviewsListComponent },
      { path: 'payments', component: PaymentsListComponent },
      { path: 'notifications', component: NotificationsListComponent },
      { path: 'orders/:id', component: OrderDetailsComponent },
      { path: '**', redirectTo: '/'}
    ]
  }
];
