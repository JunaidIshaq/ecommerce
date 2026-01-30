import {ChangeDetectorRef, Component, ElementRef, HostListener, NgZone, OnInit} from '@angular/core';
import {CartService} from '../../services/cart.service';
import {AuthService} from '../../services/auth.service';
import {map, take} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {User} from '../../models/user.model';
import {FormsModule} from '@angular/forms';
import {AsyncPipe, CommonModule, DatePipe, NgClass, NgForOf, NgIf} from '@angular/common';
import {RouterLink} from '@angular/router';
import {Notification, NotificationService} from '../../services/notification.service';
import {SearchService} from '../../services/search.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  imports: [CommonModule, FormsModule, AsyncPipe, NgIf, RouterLink, DatePipe, NgForOf, NgClass],
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  cartCount$: Observable<number>;
  user$: Observable<User | null>;
  query = '';
  menuOpen = false;

  // NOTIFICATION fields
  notifications: Notification[] = [];
  unreadCount = 0;
  isDropdownOpen = false;
  userId = '28e2ac7f-09ef-4e7e-94df-042a987fa9c9';

  // 👉 Pagination fields
  page = 1;
  size = 12;
  loading = false;
  lastPage = false;

  constructor(
    private cart: CartService,
    private auth: AuthService,
    private notificationService: NotificationService,
    private searchService: SearchService,
    private cd: ChangeDetectorRef,
    private zone: NgZone,
    private eRef: ElementRef
  ) {
    this.cartCount$ = this.cart.getCart().pipe(map(() => this.cart.count()));
    this.user$ = this.auth.currentUser();
  }

  ngOnInit(): void {
    // this.loadNotifications();
  }

  // Load latest 10 notifications
  loadNotifications(): void {
    if (this.loading || this.lastPage) return;

    this.loading = true;

    this.notificationService.getUserNotifications(this.userId, this.page, this.size)
      .pipe(take(1))
      .subscribe({
        next: (res) => {
          const newData = res?.content ?? [];

          // 🔥 FORCE Angular to notice the update
          this.zone.run(() => {
            this.notifications = [...this.notifications, ...newData];
            // Update unread count
            this.unreadCount = this.notifications.filter(n => n.readAt === null).length;
            if (newData.length < this.size) {
              this.lastPage = true;
            } else {
              this.page++;
            }

            this.loading = false;
            this.cd.detectChanges();
          });
        },
        error: () => {
          this.zone.run(() => this.loading = false);
        }
      });
  }


  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;

    if (this.isDropdownOpen) {
      this.page = 1;
      this.lastPage = false;
      this.loading = false;
      this.unreadCount = 0;
      this.notifications = [];
      this.loadNotifications();
    }

  }

  onScroll(event: any) {
    const element = event.target;

    const atBottom =
      element.scrollHeight - element.scrollTop <= element.clientHeight + 10;

    if (atBottom) {
      this.loadNotifications();
    }
  }


  markAsRead(n: Notification) {
    if (n.readAt !== null) return;

    this.notificationService.markAsRead(n.id)
      .subscribe((updated: { status: any; readAt: any; }) => {
        n.status = updated.status;
        n.readAt = updated.readAt;
        this.unreadCount--;
      });
  }

  logout() {
    this.auth.logout();
  }

  doSearch(): void {
    this.searchService.emitSearch(this.query.trim());
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu() {
    this.menuOpen = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.isDropdownOpen) return;

    const clickedInside = this.eRef.nativeElement.querySelector('.notif-wrapper')?.contains(event.target);

    if (!clickedInside) {
      this.isDropdownOpen = false;
      this.notifications = [];
      this.unreadCount = 0;
    }
  }

  @HostListener('window:scroll')
  onWindowScroll() {
    this.isDropdownOpen = false;
  }


}
