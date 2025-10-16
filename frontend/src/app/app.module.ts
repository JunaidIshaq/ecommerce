import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import { CommonModule } from '@angular/common';  // 👈 ADD THIS


// Shared
import {HeaderComponent} from './shared/header/header.component';
import {FooterComponent} from './shared/footer/footer.component';

// Pages
import {HomeComponent} from './pages/home/home.component';
import {ProductsComponent} from './pages/products/products.component';
import {ProductDetailComponent} from './pages/product-detail/product-detail.component';
import {CartComponent} from './pages/cart/cart.component';
import {CheckoutComponent} from './pages/checkout/checkout.component';
import {LoginComponent} from './pages/login/login.component';
import {SignupComponent} from './pages/signup/signup.component';
import {ProfileComponent} from './pages/profile/profile.component';

// Interceptors
import {AuthInterceptor} from './services/auth.interceptor';

@NgModule({
  declarations: [

  ],
  imports: [BrowserModule, CommonModule, FormsModule, HttpClientModule, AppComponent, AppRoutingModule, HeaderComponent, FooterComponent, HomeComponent, ProductsComponent, CartComponent, CheckoutComponent, ProfileComponent, ProductDetailComponent, LoginComponent, SignupComponent],
  providers: [{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }],
  bootstrap: []
})
export class AppModule {}
