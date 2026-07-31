import {ChangeDetectorRef, Component, Inject, NgZone, OnInit, PLATFORM_ID} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AdminApiService} from '../../services/admin-api.service';
import {StockAdjustmentDialogComponent} from '../stock-adjustment-dialog/stock-adjustment-dialog.component';
import {CommonModule, DecimalPipe, NgFor, NgIf} from '@angular/common';
import {AuthService} from '../../../services/auth.service';
import {take} from 'rxjs/operators';

@Component({
  selector: 'inventory-list',
  standalone: true,
  imports: [
    FormsModule,
    AdminCardComponent,
    StockAdjustmentDialogComponent,
    CommonModule,
    NgFor,
    NgIf,
    DecimalPipe
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

  // Edit state: tracks current input values per inventory id
  editValues: { [id: string]: { available_quantity: number; reserved_quantity: number; sold_quantity: number } } = {};
  originalValues: { [id: string]: { available_quantity: number; reserved_quantity: number; sold_quantity: number } } = {};
  savingItems: Set<string> = new Set();
  saveSuccess: Set<string> = new Set();
  saveError: { [id: string]: string } = {};

  loading = false;
  apiError = '';

  constructor(
    private adminApiService: AdminApiService,
    private authService: AuthService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    this.loadInventory();
  }

  loadInventory() {
    this.loading = true;
    this.apiError = '';

    this.authService.currentUser().pipe(take(1)).subscribe({
      next: (user) => {
        this.userId = user?.id;

        this.adminApiService.getInventory(this.currentPage, this.pageSize, this.userId).subscribe({
          next: (data: any) => {
            this.zone.run(() => {
              if (data && data.items && Array.isArray(data.items)) {
                this.inventory = data.items;
                this.totalItems = data.totalItems ?? data.items.length;
                this.totalPages = data.totalPages ?? Math.ceil(this.totalItems / this.pageSize);
              } else if (Array.isArray(data)) {
                this.inventory = data;
                this.totalItems = data.length;
                this.totalPages = Math.ceil(this.totalItems / this.pageSize);
              }

              this.initEditValues();
              this.loading = false;
              this.cdr.detectChanges();
            });
          },
          error: (err) => {
            console.error('Inventory API failed', err);
            this.loading = false;
            this.apiError = 'Failed to load inventory. Please try again.';
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        console.error('Auth service error:', err);
        this.loading = false;
        this.apiError = 'Authentication error. Please refresh.';
        this.cdr.detectChanges();
      }
    });
  }

  /** Initialise edit state from loaded inventory items */
  private initEditValues() {
    this.editValues = {};
    this.originalValues = {};
    this.savingItems.clear();
    this.saveSuccess.clear();
    this.saveError = {};

    for (const item of this.inventory) {
      const snap = {
        available_quantity: item.available_quantity ?? 0,
        reserved_quantity: item.reserved_quantity ?? 0,
        sold_quantity: item.sold_quantity ?? 0
      };
      this.editValues[item.id] = { ...snap };
      this.originalValues[item.id] = { ...snap };
    }
  }

  isDirty(item: any): boolean {
    const ev = this.editValues[item.id];
    const ov = this.originalValues[item.id];
    if (!ev || !ov) return false;
    return ev.available_quantity !== ov.available_quantity ||
      ev.reserved_quantity !== ov.reserved_quantity ||
      ev.sold_quantity !== ov.sold_quantity;
  }

  isSaving(item: any): boolean {
    return this.savingItems.has(item.id);
  }

  isSaved(item: any): boolean {
    return this.saveSuccess.has(item.id);
  }

  saveInventory(item: any) {
    const ev = this.editValues[item.id];
    if (!ev || this.isSaving(item)) return;

    this.savingItems.add(item.id);
    this.saveSuccess.delete(item.id);
    delete this.saveError[item.id];

    this.adminApiService.updateInventory(
      item.id,
      ev.available_quantity,
      ev.reserved_quantity,
      ev.sold_quantity,
      this.userId
    ).subscribe({
      next: () => {
        this.zone.run(() => {
          // Update original so isDirty resets
          this.originalValues[item.id] = { ...ev };
          // Update item in list
          item.available_quantity = ev.available_quantity;
          item.reserved_quantity = ev.reserved_quantity;
          item.sold_quantity = ev.sold_quantity;

          this.savingItems.delete(item.id);
          this.saveSuccess.add(item.id);

          // Clear success badge after 3s
          setTimeout(() => {
            this.saveSuccess.delete(item.id);
            this.cdr.detectChanges();
          }, 3000);

          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.zone.run(() => {
          this.savingItems.delete(item.id);
          this.saveError[item.id] = 'Save failed. Try again.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  cancelEdit(item: any) {
    const ov = this.originalValues[item.id];
    if (ov) {
      this.editValues[item.id] = { ...ov };
    }
    delete this.saveError[item.id];
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

  onSearch() {
    this.currentPage = 1;
    this.loadInventory();
  }

  filteredInventory() {
    if (!this.searchTerm) return this.inventory;
    const term = this.searchTerm.toLowerCase();
    return this.inventory.filter(item =>
      (item.product?.name && item.product.name.toLowerCase().includes(term)) ||
      (item.product_id && item.product_id.toLowerCase().includes(term)) ||
      (item.id && item.id.toLowerCase().includes(term))
    );
  }

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
