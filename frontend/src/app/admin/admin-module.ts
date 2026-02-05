import {NgModule} from '@angular/core';
import {AdminRoutingModule} from './admin-routing-module';

console.log('AdminModule: Importing module');

@NgModule({
  declarations: [],
  imports: [AdminRoutingModule]
})
export class AdminModule {
  constructor() {
    console.log('AdminModule: Module constructed');
  }
}
