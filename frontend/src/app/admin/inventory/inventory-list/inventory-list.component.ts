import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AdminApiService} from '../../services/admin-api.service';
import {StockAdjustmentDialogComponent} from '../stock-adjustment-dialog/stock-adjustment-dialog.component';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'inventory-list',
  standalone: true,
  imports: [
    FormsModule,
    AdminCardComponent,
    StockAdjustmentDialogComponent,
    CommonModule,
  ],
  templateUrl: './inventory-list.component.html',
  styleUrl: './inventory-list.component.css'
})
export class InventoryListComponent {
  inventory: any[] = [];
  searchTerm = '';
  protected selectedItem: any;
  protected showDialog: boolean | undefined;

  constructor(private adminApiService: AdminApiService) {
  }

  loadInventory() {
    this.adminApiService.getInventory().subscribe(data => this.inventory = data);
  }

  filteredInventory() {
    return this.inventory.filter(p =>
      p.productName.toLowerCase().includes(this.searchTerm.toLowerCase())
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
