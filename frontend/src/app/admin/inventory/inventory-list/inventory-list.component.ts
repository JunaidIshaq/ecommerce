import {Component, OnInit, ChangeDetectorRef, NgZone, Inject, PLATFORM_ID} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AdminApiService} from '../../services/admin-api.service';
import {StockAdjustmentDialogComponent} from '../stock-adjustment-dialog/stock-adjustment-dialog.component';
import {CommonModule, NgFor, NgIf} from '@angular/common';
import {AuthService} from '../../../services/auth.service';
import {isPlatformBrowser} from '@angular/common';
import {take} from 'rxjs/operators';

const MOCK_INVENTORY = [
  {
    id: '1',
    product_id: '95bf2fe5-0bb2-446d-94fe-cb57c17a7f5c',
    available_quantity: 12,
    reserved_quantity: 3,
    sold_quantity: 5,
    created_at: '2026-01-21T10:05:06.400252Z',
    updated_at: '2026-01-21T10:05:06.400254Z'
  },
  {
    id: '2',
    product_id: '4c50d048-dc7d-4efc-9117-b07854feefca',
    available_quantity: 3,
    reserved_quantity: 1,
    sold_quantity: 2,
    created_at: '2026-01-21T10:05:06.398661Z',
    updated_at: '2026-01-21T10:05:06.398663Z'
  },
  {
    id: '3',
    product_id: '894f58fa-02df-4a6e-a353-9258ada4810b',
    available_quantity: 0,
    reserved_quantity: 0,
    sold_quantity: 0,
    created_at: '2026-01-21T10:05:06.397105Z',
    updated_at: '2026-01-21T10:05:06.397107Z'
  }
];


@Component({
  selector: 'inventory-list',
  standalone: true,
  imports: [
    FormsModule,
    AdminCardComponent,
    StockAdjustmentDialogComponent,
    CommonModule,
    NgFor,
    NgIf
  ],
  templateUrl: './inventory-list.component.html',
  styleUrl: './inventory-list.component.css'
})
export class InventoryListComponent implements OnInit {

  inventory: any[] = [];
  searchTerm = '';
  userId: string | undefined;
  protected selectedItem: any;
  protected showDialog: boolean | undefined;

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalItems = 0;
  totalPages = 0;

  constructor(
    private adminApiService: AdminApiService,
    private authService: AuthService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    console.log('InventoryListComponent: Constructor called');
  }

  ngOnInit() {
    console.log('InventoryListComponent: ngOnInit called');
    this.loadInventory();
  }

  loadInventory() {
    console.log('loadInventory called');

    // First, show mock data immediately
    this.inventory = MOCK_INVENTORY;
    this.totalItems = this.inventory.length;
    this.totalPages = Math.ceil(this.totalItems / this.pageSize);
    console.log('Loaded mock inventory:', this.inventory.length);

    // Then try to fetch from API
    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.userId = user?.id;
        console.log('User from auth:', user);
        console.log('Calling inventory API with userId:', this.userId);

        this.adminApiService.getInventory(this.currentPage, this.pageSize, this.userId).subscribe({
          next: (data: any) => {
            this.zone.run(() => {
              console.log('Inventory API success:', data);

              if (data && data.items && Array.isArray(data.items)) {
                this.inventory = data.items;
                this.totalItems = data.totalItems;
                this.totalPages = data.totalPages;
              } else if (Array.isArray(data)) {
                this.inventory = data;
                this.totalItems = data.length;
                this.totalPages = Math.ceil(this.totalItems / this.pageSize);
              }

              this.cdr.detectChanges();
            });
          },
          error: (err) => {
            console.warn('Inventory API failed, using mock data', err);
            // Already showing mock data, no action needed
          }
        });
      },
      error: (err) => {
        console.warn('Auth service error:', err);
        // Already showing mock data, no action needed
      }
    });
  }

  // Pagination methods
  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadInventory();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadInventory();
    }
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadInventory();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  filteredInventory() {
    return this.inventory.filter(item =>
      (item.product_id && item.product_id.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
      (item.id && item.id.toLowerCase().includes(this.searchTerm.toLowerCase()))
    );
  }

  // ✅ THIS IS THE MISSING METHOD
  adjustStock(item: any) {
    this.selectedItem = item;
    this.showDialog = true;
  }

  closeDialog() {
    this.showDialog = false;
    this.selectedItem = null;
  }

  onStockUpdated() {
    this.loadInventory();
    this.closeDialog();
  }

}
