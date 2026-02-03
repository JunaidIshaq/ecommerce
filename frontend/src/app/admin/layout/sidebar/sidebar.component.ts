import {Component} from '@angular/core';
import {RouterLink, RouterLinkActive} from '@angular/router';

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  templateUrl: './sidebar.component.html',
  imports: [
    RouterLink,
    RouterLinkActive
  ],
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {}
