import {Component, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AdminCardComponent} from '../../shared/admin-card/admin-card.component';
import {AdminApiService} from '../../services/admin-api.service';
import {StockAdjustmentDialogComponent} from '../stock-adjustment-dialog/stock-adjustment-dialog.component';
import {CommonModule} from '@angular/common';

const MOCK_INVENTORY = [
  {
    productId: 1,
    productName: 'iPhone 15 Pro',
    sku: 'APL-IP15P-256',
    stock: 12
  },
  {
    productId: 2,
    productName: 'Samsung Galaxy S24',
    sku: 'SMS-S24-128',
    stock: 3
  },
  {
    productId: 3,
    productName: 'Sony WH-1000XM5',
    sku: 'SNY-WH1000XM5',
    stock: 0
  },
  {
    productId: 4,
    productName: 'Gaming Mechanical Keyboard',
    sku: 'GMG-KB-RGB',
    stock: 7
  },
  {
    productId: 5,
    productName: 'Apple Watch Series 9',
    sku: 'APL-WCH-S9',
    stock: 2
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
  ],
  templateUrl: './inventory-list.component.html',
  styleUrl: './inventory-list.component.css'
})
export class InventoryListComponent implements OnInit {



  inventory: any[] = [];
  searchTerm = '';
  protected selectedItem: any;
  protected showDialog: boolean | undefined;

  constructor(private adminApiService: AdminApiService) {
  }

  ngOnInit() {
    this.inventory = MOCK_INVENTORY; // show mock immediately
    this.loadInventory();            // try real API
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
