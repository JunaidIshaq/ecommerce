import {Component, EventEmitter, Output} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {AdminApiService} from '../../services/admin-api.service';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-notification-form',
  imports: [
    FormsModule
  ],
  templateUrl: './notification-form.component.html',
  styleUrl: './notification-form.component.css'
})
export class NotificationFormComponent {

  constructor(private adminApiService: AdminApiService) {}

  @Output() closed = new EventEmitter<void>();
  @Output() sent = new EventEmitter<void>();

  form = { title: '', message: '', audience: 'ALL' };

  send() {
    this.adminApiService.sendNotification(this.form)
      .subscribe(() => this.sent.emit());
  }

  close() {
    this.closed.emit();
  }

}
