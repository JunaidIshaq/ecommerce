import {Component, Input, model} from '@angular/core';

@Component({
  selector: 'stock-adjustment-dialog',
  standalone: true,
  imports: [],
  templateUrl: './stock-adjustment-dialog.component.html',
  styleUrl: './stock-adjustment-dialog.component.css'
})
export class StockAdjustmentDialogComponent {
  @Input() item!: any;
  updated = model(undefined);

}
