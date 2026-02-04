import { Component } from '@angular/core';
import { CommonModule, NgFor, NgIf, DatePipe } from '@angular/common';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';
import { NotificationFormComponent } from '../notification-form/notification-form.component';

@Component({
  selector: 'app-notifications-list',
  standalone: true,
  imports: [
    CommonModule,
    NgFor,
    NgIf,
    DatePipe,
    AdminCardComponent,
    NotificationFormComponent
  ],
  templateUrl: './notifications-list.component.html',
  styleUrls: ['./notifications-list.component.css']
})
export class NotificationsListComponent {

  notifications: any[] = [];
  showForm = false;

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.loadNotifications();
  }

  loadNotifications() {
    this.adminApi.getNotifications()
      .subscribe(data => this.notifications = data);
  }

  openForm() {
    this.showForm = true;
  }

  closeForm() {
    this.showForm = false;
  }

  reload() {
    this.loadNotifications();
    this.closeForm();
  }
}
