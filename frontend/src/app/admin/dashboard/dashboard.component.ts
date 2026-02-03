import { Component } from '@angular/core';
import {AdminCardComponent} from '../shared/admin-card/admin-card.component';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'dashboard',
  imports: [AdminCardComponent, CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {

}
