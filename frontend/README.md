# ShopFast - E-Commerce Frontend

A modern, full-featured e-commerce frontend application built with Angular 20, featuring a customer-facing storefront and a comprehensive admin dashboard.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Customer Pages](#customer-pages)
- [Admin Panel](#admin-panel)
- [Services](#services)
- [Models](#models)
- [Routing](#routing)
- [Getting Started](#getting-started)
- [Deployment](#deployment)

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Angular | 20.1.3 | Frontend framework |
| TypeScript | ~5.5 | Programming language |
| RxJS | ~7.8 | Reactive programming |
| Angular Forms | - | Form handling |
| Angular Router | - | Navigation |
| Docker | - | Containerization |
| Nginx | - | Production web server |

---

## Project Structure

```
src/
├── app/
│   ├── admin/                    # Admin dashboard module
│   │   ├── admin-auth.guard.ts   # Route guard for admin pages
│   │   ├── admin-module.ts       # Admin module definition
│   │   ├── admin-routing-module.ts # Admin routes
│   │   ├── analytics/            # Analytics components
│   │   ├── categories/           # Category management
│   │   ├── coupons/              # Coupon management
│   │   ├── dashboard/            # Admin dashboard
│   │   ├── inventory/            # Inventory management
│   │   ├── layout/               # Admin layout (header, sidebar)
│   │   ├── model/                # Admin-specific models
│   │   ├── notifications/        # Notification management
│   │   ├── orders/               # Order management
│   │   ├── payments/             # Payment management
│   │   ├── products/             # Product management
│   │   ├── reviews/              # Review management
│   │   ├── services/             # Admin API services
│   │   ├── settings/             # System settings
│   │   ├── shared/               # Shared admin components
│   │   └── users/                # User management
│   ├── models/                   # TypeScript interfaces
│   ├── pages/                    # Customer-facing pages
│   │   ├── about/                # About page
│   │   ├── cart/                 # Shopping cart
│   │   ├── checkout/             # Checkout with payment
│   │   ├── contact/              # Contact page
│   │   ├── home/                 # Home page
│   │   ├── index/                # Landing page
│   │   ├── login/                # User login
│   │   ├── order-detail/         # Order tracking
│   │   ├── product/              # Product listing
│   │   ├── product-detail/       # Single product view
│   │   ├── profile/              # User profile
│   │   └── signup/               # User registration
│   ├── services/                 # Core services
│   ├── shared/                   # Shared components
│   │   ├── footer/               # Site footer
│   │   ├── header/               # Site header
│   │   └── toast/                # Toast notifications
│   └── utils/                    # Utility functions
├── environments/                 # Environment configs
└── assets/                       # Static assets
```

---

## Customer Pages

### 1. Home Page (`/`)
**Route:** `/`
**Component:** [`HomeComponent`](src/app/pages/home/home.component.ts)

**Layout:**
- Hero banner with welcome message and "Shop Now" CTA button
- Featured products grid with pagination
- Product cards showing image, name, price, and "Add to Cart" button
- Responsive grid layout (4 columns desktop, 2 tablet, 1 mobile)

**Features:**
- Paginated product listing
- Category filtering
- Sort by price/rating
- Add to cart functionality
- Loading and error states

---

### 2. Product Listing Page (`/product`)
**Route:** `/product`
**Component:** [`ProductComponent`](src/app/pages/product/product.component.ts)

**Layout:**
- Filter bar with category dropdown and sort options
- Product grid with hover overlay showing "Add to Cart"
- Pagination controls (Prev/Next with page numbers)

**Features:**
- Category-based filtering
- Sort by price (low/high) and rating
- Hover effect with quick add-to-cart
- Pagination with ellipsis for large page counts

---

### 3. Product Detail Page (`/product/:id`)
**Route:** `/product/:id`
**Component:** [`ProductDetailComponent`](src/app/pages/product-detail/product-detail.component.ts)

**Layout:**
- Large product image
- Product name, description, price
- Quantity selector
- "Add to Cart" button
- Product specifications

**Features:**
- Detailed product information
- Quantity selection
- Add to cart with quantity
- Server-side rendering enabled

---

### 4. Shopping Cart (`/cart`)
**Route:** `/cart`
**Component:** [`CartComponent`](src/app/pages/cart/cart.component.ts)

**Layout:**
- Left: Cart items list with images, details, quantity controls
- Right: Order summary with subtotal, shipping, and total
- "Proceed to Checkout" and "Continue Shopping" buttons

**Features:**
- Quantity increment/decrement
- Remove items
- Real-time subtotal calculation
- Free shipping indicator
- Empty cart state with illustration

---

### 5. Checkout Page (`/checkout`) ⭐ NEW
**Route:** `/checkout`
**Component:** [`CheckoutComponent`](src/app/pages/checkout/checkout.component.ts)

**Layout:**
- Left column: Checkout form
  - Shipping Address section
  - Payment Method selection (COD / Card)
  - Card Details form (conditional)
  - Coupon Code input
  - Place Order button
- Right column: Order Summary card

**Payment Methods:**

#### Cash on Delivery (COD)
- Default selected option
- No additional fields required
- Visual indicator: 💵 Pay on delivery

#### Credit/Debit Card
- Card Number (auto-formatted: XXXX XXXX XXXX XXXX)
- Cardholder Name
- Expiry Date (auto-formatted: MM/YY)
- CVV (password field)
- Visual indicator: 🔒 Secure payment

**Features:**
- Payment method toggle with radio buttons
- Conditional card form display
- Input formatting for card number and expiry
- Form validation (all fields required for card payment)
- Button disabled until form is valid
- Coupon code support
- Order placement with payment details

**Form Validation:**
- Shipping address: Full Name, Street, City, ZIP, Country required
- Card payment: Card number (≥15 digits), cardholder name (≥2 chars), expiry (MM/YY), CVV (≥3 digits)
- COD: No additional validation beyond address

---

### 6. Login Page (`/login`)
**Route:** `/login`
**Component:** [`LoginComponent`](src/app/pages/login/login.component.ts)

**Layout:**
- Centered login card
- Email and password fields
- "Remember me" checkbox
- "Forgot password" link
- "Sign up" link

**Features:**
- Email/password authentication
- JWT token storage
- Redirect after login
- Error message display

---

### 7. Signup Page (`/signup`)
**Route:** `/signup`
**Component:** [`SignupComponent`](src/app/pages/signup/signup.component.ts)

**Layout:**
- Centered registration card
- Name, email, password, confirm password fields
- Terms and conditions checkbox
- "Already have an account?" link

**Features:**
- User registration
- Password confirmation
- Form validation
- Auto-login after registration

---

### 8. Profile Page (`/profile`)
**Route:** `/profile`
**Component:** [`ProfileComponent`](src/app/pages/profile/profile.component.ts)

**Layout:**
- User information display
- Address management
- Order history link

**Features:**
- View/edit profile
- Manage addresses
- View order history

---

### 9. Order Detail Page (`/order/:id`)
**Route:** `/order/:id`
**Component:** [`CustomerOrderDetailComponent`](src/app/pages/order-detail/order-detail.component.ts)

**Layout:**
- Order information header
- Order status timeline
- Items list with quantities and prices
- Shipping address
- Payment method and status

**Features:**
- Order tracking
- Status updates
- Item details
- Delivery information

---

### 10. About Page (`/about`)
**Route:** `/about`
**Component:** [`AboutComponent`](src/app/pages/about/about.component.ts)

**Layout:**
- Company information
- Mission statement
- Team section
- Values

---

### 11. Contact Page (`/contact`)
**Route:** `/contact`
**Component:** [`ContactComponent`](src/app/pages/contact/contact.component.ts)

**Layout:**
- Contact form (name, email, message)
- Contact information
- Map/location (if applicable)

---

## Admin Panel

**Route:** `/admin/*`
**Guard:** [`AdminAuthGuard`](src/app/admin/admin-auth.guard.ts)
**Layout:** [`AdminLayoutComponent`](src/app/admin/layout/admin-layout/admin-layout.component.ts)

### Admin Layout
- Top header with admin info and logout
- Left sidebar with navigation links
- Main content area

### Admin Sections

#### Dashboard (`/admin/dashboard`)
**Component:** [`DashboardComponent`](src/app/admin/dashboard/dashboard.component.ts)

**Layout:**
- Key metrics cards (total orders, revenue, users, products)
- Charts for sales, orders, user growth
- Recent orders table
- Top selling products

**Features:**
- Real-time metrics
- Sales analytics
- Conversion tracking
- Visual charts

---

#### Users Management (`/admin/users`)
**Component:** [`UsersListComponent`](src/app/admin/users/users-list/users-list.component.ts)

**Layout:**
- Users table with search/filter
- User details view
- User activity tracking

**Features:**
- List all users
- View user details
- Track user activity
- User status management

---

#### Orders Management (`/admin/orders`)
**Component:** [`OrdersListComponent`](src/app/admin/orders/orders-list/orders-list.component.ts)

**Layout:**
- Orders table with filters
- Order status badges
- Order details view

**Features:**
- View all orders
- Update order status
- Process refunds
- View order details

**Sub-components:**
- [`OrderDetailsComponent`](src/app/admin/orders/order-details/order-details.component.ts) - Detailed order view
- [`RefundDialogComponent`](src/app/admin/orders/refund-dialog/refund-dialog.component.ts) - Refund processing

---

#### Products Management (`/admin/products`)
**Component:** [`ProductsListComponent`](src/app/admin/products/products-list/products-list.component.ts)

**Layout:**
- Products table with image thumbnails
- Search and filter
- Add/Edit product form

**Features:**
- CRUD operations for products
- Category assignment
- Price management
- Inventory tracking
- Image upload

**Sub-components:**
- [`ProductFormComponent`](src/app/admin/products/product-form/product-form.component.ts) - Add/Edit product
- [`ProductDetailsComponent`](src/app/admin/products/product-details/product-details.component.ts) - Product details view

---

#### Inventory Management (`/admin/inventory`)
**Component:** [`InventoryListComponent`](src/app/admin/inventory/inventory-list/inventory-list.component.ts)

**Layout:**
- Inventory table with stock levels
- Low stock alerts
- Stock adjustment form

**Features:**
- Track stock levels
- Low stock warnings
- Stock adjustments
- Inventory history

**Sub-components:**
- [`StockAdjustmentDialogComponent`](src/app/admin/inventory/stock-adjustment-dialog/stock-adjustment-dialog.component.ts) - Stock adjustment

---

#### Categories Management (`/admin/categories`)
**Components:**
- [`CategoriesListComponent`](src/app/admin/categories/categories-list/categories-list.component.ts)
- [`CategoryFormComponent`](src/app/admin/categories/category-form/category-form.component.ts)

**Features:**
- Create/edit/delete categories
- Category hierarchy
- Product count per category

---

#### Coupons Management (`/admin/coupons`)
**Component:** [`CouponsListComponent`](src/app/admin/coupons/coupons-list/coupons-list.component.ts)

**Layout:**
- Coupons table with code, discount, validity
- Create/edit coupon form
- Usage statistics

**Features:**
- Create discount coupons
- Percentage/fixed amount discounts
- Validity dates
- Usage tracking

**Sub-components:**
- [`CouponFormComponent`](src/app/admin/coupons/coupon-form/coupon-form.component.ts) - Add/Edit coupon
- [`CouponUsageComponent`](src/app/admin/coupons/coupon-usage/coupon-usage.component.ts) - Usage analytics

---

#### Reviews Management (`/admin/reviews`)
**Component:** [`ReviewsListComponent`](src/app/admin/reviews/reviews-list/reviews-list.component.ts)

**Layout:**
- Reviews table with ratings
- Review details view
- Reported reviews section

**Features:**
- View all reviews
- Moderate reviews
- Respond to reviews
- Handle reported reviews

**Sub-components:**
- [`ReviewDetailsComponent`](src/app/admin/reviews/review-details/review-details.component.ts) - Review details
- [`ReportedReviewsComponent`](src/app/admin/reviews/reported-reviews/reported-reviews.component.ts) - Reported reviews

---

#### Payments Management (`/admin/payments`)
**Component:** [`PaymentsListComponent`](src/app/admin/payments/payments-list/payments-list.component.ts)

**Layout:**
- Payments table with transaction details
- Payment status filters
- Refund management

**Features:**
- View all transactions
- Payment status tracking
- Refund processing
- Payment analytics

**Sub-components:**
- [`PaymentDetailsComponent`](src/app/admin/payments/payment-details/payment-details.component.ts) - Payment details
- [`RefundManagementComponent`](src/app/admin/payments/refund-management/refund-management.component.ts) - Refund management

---

#### Notifications Management (`/admin/notifications`)
**Component:** [`NotificationsListComponent`](src/app/admin/notifications/notifications-list/notifications-list.component.ts)

**Layout:**
- Notifications list
- Send notification form
- Notification logs

**Features:**
- Send push notifications
- Email notifications
- Notification history
- Target audience selection

**Sub-components:**
- [`SendNotificationComponent`](src/app/admin/notifications/send-notification/send-notification.component.ts) - Send notification
- [`NotificationFormComponent`](src/app/admin/notifications/notification-form/notification-form.component.ts) - Notification form
- [`NotificationLogsComponent`](src/app/admin/notifications/notification-logs/notification-logs.component.ts) - Notification logs

---

#### Analytics (`/admin/analytics`)
**Components:**
- [`ConversionRateComponent`](src/app/admin/analytics/conversion-rate/conversion-rate.ts) - Conversion analytics
- [`SalesReportComponent`](src/app/admin/analytics/sales-report/sales-report.ts) - Sales reports
- [`TopProductsComponent`](src/app/admin/analytics/top-products/top-products.ts) - Top selling products
- [`UserGrowthComponent`](src/app/admin/analytics/user-growth/user-growth.ts) - User growth metrics

---

#### Settings (`/admin/settings`)
**Components:**
- [`AdminManagementComponent`](src/app/admin/settings/admin-management/admin-management.ts) - Admin user management
- [`AuditLogsComponent`](src/app/admin/settings/audit-logs/audit-logs.component.ts) - System audit logs
- [`FeatureFlagsComponent`](src/app/admin/settings/feature-flags/feature-flags.ts) - Feature toggles
- [`SystemHealthComponent`](src/app/admin/settings/system-health/system-health.ts) - System health monitoring

---

## Services

### Customer Services

| Service | File | Purpose |
|---------|------|---------|
| AuthService | [`auth.service.ts`](src/app/services/auth.service.ts) | User authentication, JWT management |
| CartService | [`cart.service.ts`](src/app/services/cart.service.ts) | Cart operations, checkout |
| ProductService | [`product.service.ts`](src/app/services/product.service.ts) | Product fetching, search |
| CategoryService | [`category.service.ts`](src/app/services/category.service.ts) | Category management |
| OrderService | [`order.service.ts`](src/app/services/order.service.ts) | Order operations |
| ToastService | [`toast.service.ts`](src/app/services/toast.service.ts) | Toast notifications |
| SearchService | [`search.service.ts`](src/app/services/search.service.ts) | Product search |
| NotificationService | [`notification.service.ts`](src/app/services/notification.service.ts) | Push notifications |

### Admin Services

| Service | File | Purpose |
|---------|------|---------|
| AdminApiService | [`admin-api.service.ts`](src/app/admin/services/admin-api.service.ts) | Admin API calls |

### Interceptors

| Interceptor | File | Purpose |
|-------------|------|---------|
| AuthInterceptor | [`auth.interceptor.ts`](src/app/services/auth.interceptor.ts) | Attach JWT to requests |

---

## Models

| Model | File | Properties |
|-------|------|------------|
| User | [`user.model.ts`](src/app/models/user.model.ts) | id, name, email, token, addresses, role |
| Address | [`address.model.ts`](src/app/models/address.model.ts) | fullName, street, city, state, zip, country, phone |
| Product | [`product.model.ts`](src/app/models/product.model.ts) | id, name, price, description, images, category, rating |
| CartItem | [`cart-item.model.ts`](src/app/models/cart-item.model.ts) | productId, title, price, quantity, images, category |
| Order | [`order.model.ts`](src/app/models/order.model.ts) | id, order_number, userEmail, shippingAddress, phone, paymentMethod, paymentStatus, status, subtotal, tax, discount, totalAmount, items, createdAt |
| OrderItem | [`order.model.ts`](src/app/models/order.model.ts) | productId, productName, price, quantity, image |
| Category | [`category.model.ts`](src/app/models/category.model.ts) | id, name, description |
| AuthResponse | [`auth-response.model.ts`](src/app/models/auth-response.model.ts) | token, user |
| DashboardMetrics | [`dashboard-metrics.model.ts`](src/app/admin/model/dashboard-metrics.model.ts) | totalOrders, totalRevenue, totalUsers, totalProducts |

---

## Routing

### Customer Routes

| Path | Component | Description |
|------|-----------|-------------|
| `/` | HomeComponent | Home page with featured products |
| `/product` | ProductComponent | Product listing with filters |
| `/product/:id` | ProductDetailComponent | Single product details |
| `/cart` | CartComponent | Shopping cart |
| `/checkout` | CheckoutComponent | Checkout with payment |
| `/login` | LoginComponent | User login |
| `/signup` | SignupComponent | User registration |
| `/profile` | ProfileComponent | User profile |
| `/order/:id` | CustomerOrderDetailComponent | Order tracking |
| `/about` | AboutComponent | About page |
| `/contact` | ContactComponent | Contact page |
| `/index` | IndexComponent | Landing page |
| `**` | Redirect to `/` | Wildcard route |

### Admin Routes

| Path | Component | Description |
|------|-----------|-------------|
| `/admin/dashboard` | DashboardComponent | Admin dashboard |
| `/admin/users` | UsersListComponent | User management |
| `/admin/orders` | OrdersListComponent | Order management |
| `/admin/orders/:id` | AdminOrderDetailsComponent | Order details |
| `/admin/products` | ProductsListComponent | Product management |
| `/admin/inventory` | InventoryListComponent | Inventory management |
| `/admin/coupons` | CouponsListComponent | Coupon management |
| `/admin/reviews` | ReviewsListComponent | Review management |
| `/admin/payments` | PaymentsListComponent | Payment management |
| `/admin/notifications` | NotificationsListComponent | Notification management |
| `/admin/analytics/conversion-rate` | ConversionRateComponent | Conversion analytics |
| `/admin/analytics/sales-report` | SalesReportComponent | Sales reports |
| `/admin/analytics/top-products` | TopProductsComponent | Top products |
| `/admin/analytics/user-growth` | UserGrowthComponent | User growth |
| `/admin/settings/admin-management` | AdminManagementComponent | Admin management |
| `/admin/settings/audit-logs` | AuditLogsComponent | Audit logs |
| `/admin/settings/feature-flags` | FeatureFlagsComponent | Feature flags |
| `/admin/settings/system-health` | SystemHealthComponent | System health |

---

## Getting Started

### Prerequisites
- Node.js 18+
- npm or yarn
- Angular CLI 20.1.3

### Installation

```bash
# Install dependencies
npm install

# Start development server
ng serve
```

Navigate to `http://localhost:4200/`

### Build

```bash
# Development build
ng build

# Production build
ng build --configuration production
```

---

## Deployment

### Docker

```bash
# Build image
docker build --no-cache -t ecommerce-frontend .

# Run container
docker run -d --name frontend -p 3000:80 ecommerce-frontend
```

### Manual Deployment

```bash
# Build for production
npm run build -- --configuration production

# Copy to web server
sudo rm -rf /var/www/shopfast.live/*
sudo cp -r dist/frontend/browser/* /var/www/shopfast.live/
```

---

## Architecture

### State Management
- RxJS BehaviorSubject for cart state
- LocalStorage for cart persistence
- JWT for authentication state

### API Communication
- HttpClient for API calls
- Auth interceptor for token injection
- Environment-based API URLs

### UI/UX Features
- Responsive design (mobile-first)
- Toast notifications
- Loading states
- Error handling
- Form validation
- Auto-formatting (card numbers, dates)

---

## Key Features

### Customer Features
- Product browsing and search
- Category filtering and sorting
- Shopping cart management
- Secure checkout with multiple payment methods
- Order tracking
- User profile management
- Coupon/discount codes

### Admin Features
- Dashboard with analytics
- User management
- Order management with status updates
- Product and inventory management
- Coupon creation and tracking
- Review moderation
- Payment tracking and refunds
- Notification management
- System settings and audit logs

---

## Payment Methods

### Cash on Delivery (COD)
- No payment information required
- Pay upon delivery
- Default payment method

### Credit/Debit Card
- Card number with auto-formatting
- Cardholder name
- Expiry date (MM/YY format)
- CVV security code
- Form validation before submission

---

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

---

## License

This project is proprietary software. All rights reserved.
