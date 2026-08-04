# ShopFast E-Commerce Platform - Complete Backend Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATIONS                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────────────┐   │
│  │   Frontend   │  │    Mobile    │  │        Admin Dashboard           │   │
│  │  (Angular)   │  │     App      │  │   (Product/Category/Order/       │   │
│  │  Port: 4200  │  │  (iOS/Android)│  │    User/Inventory/Coupon Mgmt)   │   │
│  └──────┬───────┘  └──────┬───────┘  └──────────────────────────────────┘   │
└─────────┼─────────────────┼────────────────────────────────────────────────┘
          │                 │
          └─────────────────┼────────────────────────────────────────────────┘
                            │ HTTPS
┌───────────────────────────▼─────────────────────────────────────────────────┐
│                        API GATEWAY (Port: 8080)                               │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  • JWT Authentication & Authorization                                   │ │
│  │  • Rate Limiting (Redis-backed, per IP)                                 │ │
│  │  • Dynamic Routing via Eureka Service Discovery                         │ │
│  │  • CORS Configuration                                                   │ │
│  │  • Request/Response Logging                                             │ │
│  │  • Circuit Breaker (Resilience4j)                                       │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────────────────────┘
                            │
                            │ Routes to services via Eureka
                            │
┌───────────────────────────▼─────────────────────────────────────────────────┐
│                    SERVICE DISCOVERY (Eureka Server: 8761)                    │
│  • Service Registry & Health Monitoring                                      │
│  • Dynamic Service Location                                                  │
│  • Load Balancing                                                            │
└──────────────────────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┬───────────────────┐
        │                   │                   │                   │
┌───────▼────────┐  ┌────────▼────────┐  ┌──────▼────────┐  ┌──────▼────────┐
│  AUTH SERVICE  │  │  USER SERVICE   │  │ PRODUCT SVC   │  │ CATEGORY SVC  │
│   Port: 8087   │  │   Port: 8086    │  │  Port: 8081   │  │  Port: 8082   │
│                │  │                 │  │               │  │               │
│ • Login/Logout │  │ • User Profile  │  │ • Product CRUD│  │ • Category    │
│ • JWT Generation│ │ • User List     │  │ • Product     │  │   Management  │
│ • Token Refresh │  │ • User Search   │  │   Search      │  │ • Category    │
│ • Password      │  │ • User Events   │  │ • Product     │  │   Hierarchy   │
│   Encryption    │  │                 │  │   Events      │  │               │
│                │  │                 │  │               │  │               │
│ Database:      │  │ Database:       │  │ Database:     │  │ Database:     │
│  auth_db       │  │  user_db        │  │  product_db   │  │  category_db  │
│                │  │                 │  │               │  │               │
│ Kafka:         │  │ Kafka:          │  │ Kafka:        │  │ Kafka:        │
│  (Consumer)    │  │  user.events    │  │ product.events│  │ (Producer)    │
│                │  │                 │  │               │  │               │
│ Redis:         │  │ Redis:          │  │ Redis:        │  │ Redis:        │
│  Session Cache │  │  User Cache     │  │ Product Cache │  │ Category Cache│
└────────────────┘  └─────────────────┘  └───────────────┘  └───────────────┘

┌────────────────┐  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│ CART SERVICE   │  │ COUPON SERVICE │  │ REVIEW SERVICE │  │ INVENTORY SVC  │
│  Port: 8088    │  │  Port: 8089    │  │  Port: 8090    │  │  Port: 8083    │
│                │  │                │  │                │  │                │
│ • Cart CRUD    │  │ • Coupon       │  │ • Review CRUD  │  │ • Stock Mgmt   │
│ • Cart Merge   │  │   Management   │  │ • Rating       │  │ • Reservation  │
│   (Guest→User) │  │ • Coupon       │  │   System       │  │ • Availability │
│ • Cart Events  │  │   Validation   │  │ • Review       │  │ • Low Stock    │
│                │  │ • Discount      │  │   Moderation   │  │   Alerts       │
│                │  │   Calculation  │  │                │  │                │
│ Database:      │  │ Database:      │  │ Database:      │  │ Database:      │
│  cart_db       │  │  coupon_db     │  │  review_db     │  │  inventory_db  │
│                │  │                │  │                │  │                │
│ Kafka:         │  │ Kafka:         │  │ Kafka:         │  │ Kafka:         │
│  (Consumer)    │  │  coupon.events │  │  (Consumer)    │  │  order.commands│
│                │  │                │  │                │  │                │
│ Redis:         │  │ Redis:         │  │ Redis:         │  │ Redis:         │
│  Cart Cache    │  │  (Minimal)     │  │  Review Cache  │  │  Stock Cache   │
└────────────────┘  └────────────────┘  └────────────────┘  └────────────────┘

┌────────────────┐  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│  ORDER SERVICE │  │ PAYMENT SERVICE│  │ NOTIFICATION   │  │  ELASTIC SVC   │
│  Port: 8084    │  │  Port: 8085    │  │    SERVICE     │  │  Port: 8092    │
│                │  │                │  │  Port: 8091    │  │                │
│ • Order CRUD   │  │ • Payment      │  │                │  │ • Full-Text    │
│ • Checkout     │  │   Processing   │  │ • Email        │  │   Search       │
│ • Order Status │  │ • Stripe/       │  │ • SMS          │  │ • Product      │
│   Management   │  │   Razorpay     │  │ • Push         │  │   Indexing     │
│ • Order Events │  │   Integration  │  │   Notifications│  │ • Search       │
│ • Saga Pattern │  │ • Refund       │  │ • Template     │  │   Suggestions  │
│ • Order Search │  │   Processing   │  │   Management   │  │ • Faceted      │
│                │  │ • Payment      │  │ • Notification │  │   Search       │
│                │  │   Events       │  │   History      │  │                │
│ Database:      │  │ Database:      │  │ Database:      │  │ Database:      │
│  order_db      │  │  payment_db    │  │  notification_ │  │  (None - uses  │
│                │  │                │  │     db         │  │  Elasticsearch)│
│ Kafka:         │  │ Kafka:         │  │ Kafka:         │  │ Kafka:         │
│  order.commands│  │  payment.events│  │  notification. │  │  product.events│
│  payment.events│  │                │  │     events     │  │                │
│  notification. │  │                │  │                │  │                │
│     events     │  │                │  │                │  │                │
│                │  │                │  │                │  │                │
│ Redis:         │  │ Redis:         │  │ Redis:         │  │ Redis:         │
│  Order Cache   │  │  (Minimal)     │  │  (Minimal)     │  │  Search Cache  │
└────────────────┘  └────────────────┘  └────────────────┘  └────────────────┘

┌────────────────┐
│  ADMIN SERVICE │
│  Port: 8093    │
│                │
│ • Dashboard    │
│ • User Mgmt    │
│ • Product Mgmt │
│ • Order Mgmt   │
│ • Category Mgmt│
│ • Inventory    │
│   Mgmt         │
│ • Coupon Mgmt  │
│ • Cart Mgmt    │
│ • Analytics    │
│                │
│ Database:      │
│  admin_db      │
│                │
│ Kafka:         │
│  (Producer)    │
│                │
│ Redis:         │
│  (Minimal)     │
└────────────────┘
```

---

## Detailed Service Communication Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYNCHRONOUS (REST) COMMUNICATION                      │
└─────────────────────────────────────────────────────────────────────────────┘

    Client
      │
      ▼
  [API Gateway] ─────────────────────────────────────────────────────────────┐
      │                                                                        │
      ├──► [Auth Service] ◄──── REST ──── [User Service]                      │
      │         │                        (Validate user, get profile)          │
      │         │                                                               │
      ├──► [Product Service] ◄── REST ── [Category Service]                   │
      │         │                        (Get category details)                │
      │         │                                                               │
      ├──► [Cart Service] ◄──── REST ── [Product Service]                     │
      │         │                        (Get product details, price)          │
      │         │                                                               │
      ├──► [Order Service] ─── REST ──► [Payment Service]                     │
      │         │                        (Process payment)                     │
      │         │                                                               │
      ├──► [Order Service] ─── REST ──► [Product Service]                     │
      │         │                        (Get product details)                 │
      │         │                                                               │
      ├──► [Coupon Service] ── REST ──► [Product Service]                     │
      │         │                        (Validate coupon for products)        │
      │         │                                                               │
      ├──► [Review Service] ── REST ──► [Product Service]                     │
      │         │                        (Get product for review)              │
      │         │                                                               │
      └──► [Admin Service] ─── REST ──► [User Service]                        │
                │                        (Manage users)                        │
                ├── REST ──► [Order Service]  (Manage orders)                 │
                ├── REST ──► [Product Service] (Manage products)              │
                ├── REST ──► [Category Service] (Manage categories)           │
                ├── REST ──► [Inventory Service] (Manage inventory)           │
                ├── REST ──► [Coupon Service]  (Manage coupons)               │
                └── REST ──► [Cart Service]    (Manage carts)                 │

┌─────────────────────────────────────────────────────────────────────────────┐
│                      ASYNCHRONOUS (Kafka) COMMUNICATION                       │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────────┐
    │                         KAFKA CLUSTER                                 │
    │  Topics: order.commands | payment.events | product.events           │
    │          inventory.events | notification.events | user.events        │
    │          coupon.events | search.events                               │
    └─────────────────────────────────────────────────────────────────────┘
                                     ▲
                                     │
    ┌────────────────────────────────┼────────────────────────────────┐
    │                                │                                │
    │   PRODUCERS                    │   CONSUMERS                    │
    │                                │                                │
[Order Service]                     │  [Inventory Service]            │
    │  • Publish: OrderCommand        │      │                         │
    │    (RESERVE, CONFIRM,           │      └── Consume: Reserve      │
    │     RELEASE)                    │            Inventory           │
    │                                │      • Publish: InventoryEvent │
    │                                │        (RESERVED, CONFIRMED,   │
    │                                │         RELEASED, FAILED)      │
    │                                │                                │
[Payment Service]                   │  [Order Service]                │
    │  • Publish: PaymentEvent       │      │                         │
    │    (SUCCESS, FAILED,            │      └── Consume: Update       │
    │     REFUNDED)                   │            Order Status        │
    │                                │                                │
[Product Service]                   │  [Notification Service]         │
    │  • Publish: ProductEvent       │      │                         │
    │    (CREATED, UPDATED,           │      └── Consume: Send         │
    │     DELETED)                    │            Email/SMS/Push      │
    │                                │                                │
[User Service]                      │  [Elastic Service]              │
    │  • Publish: UserEvent          │      │                         │
    │    (CREATED, UPDATED)           │      └── Consume: Update       │
    │                                │            Search Index        │
[Coupon Service]                    │                                │
    │  • Publish: CouponEvent        │                                │
    │    (CREATED, VALIDATED)         │                                │
    │                                │                                │
[Admin Service]                     │                                │
    │  • Publish: AdminEvent         │                                │
    │    (ACTION_PERFORMED)           │                                │
    │                                │                                │
    └────────────────────────────────┼────────────────────────────────┘
                                     │
                                     ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                    EVENT-DRIVEN ORDER FLOW (Saga Pattern)            │
    │                                                                       │
    │  1. Order Service publishes: OrderCommand (RESERVE)                  │
    │                    │                                                 │
    │                    ▼                                                 │
    │  2. Inventory Service consumes & reserves stock                      │
    │                    │                                                 │
    │                    ▼                                                 │
    │  3. Inventory Service publishes: InventoryEvent (RESERVED/FAILED)    │
    │                    │                                                 │
    │                    ▼                                                 │
    │  4. Order Service consumes & updates order status                    │
    │                    │                                                 │
    │                    ▼                                                 │
    │  5. (If RESERVED) Payment Service processes payment                  │
    │                    │                                                 │
    │                    ▼                                                 │
    │  6. Payment Service publishes: PaymentEvent (SUCCESS/FAILED)         │
    │                    │                                                 │
    │                    ▼                                                 │
    │  7. Order Service consumes & confirms/rejects order                  │
    │                    │                                                 │
    │                    ▼                                                 │
    │  8. Order Service publishes: NotificationEvent                       │
    │                    │                                                 │
    │                    ▼                                                 │
    │  9. Notification Service sends email/SMS to customer                 │
    │                                                                       │
    │  Compensation: If payment fails → Order Service publishes            │
    │  OrderCommand (RELEASE) → Inventory Service releases stock           │
    └─────────────────────────────────────────────────────────────────────┘
```

---

## Database Schema per Service

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATABASE PER SERVICE                                │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│    AUTH_DB      │  │    USER_DB      │  │  PRODUCT_DB     │  │  CATEGORY_DB    │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • users         │  │ • users         │  │ • products      │  │ • categories    │
│   - id (PK)     │  │   - id (PK)     │  │   - id (PK)     │  │   - id (PK)     │
│   - email       │  │   - email       │  │   - name        │  │   - name        │
│   - password    │  │   - password    │  │   - description │  │   - slug        │
│   - role        │  │   - first_name  │  │   - price       │  │   - parent_id   │
│   - created_at  │  │   - last_name   │  │   - category_id │  │   - image_url   │
│                 │  │   - phone       │  │   - image_url   │  │   - active      │
│                 │  │   - address     │  │   - active      │  │   - created_at  │
│                 │  │   - created_at  │  │   - created_at  │  │                 │
│                 │  │   - updated_at  │  │   - updated_at  │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  INVENTORY_DB   │  │    ORDER_DB     │  │  ORDER_ITEM_DB  │  │  PAYMENT_DB     │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • inventory     │  │ • orders        │  │ • order_items   │  │ • payments      │
│   - id (PK)     │  │   - id (PK)     │  │   - id (PK)     │  │   - id (PK)     │
│   - product_id  │  │   - user_id     │  │   - order_id    │  │   - order_id    │
│   - quantity    │  │   - order_number│  │   - product_id  │  │   - amount      │
│   - reserved    │  │   - status      │  │   - quantity    │  │   - method      │
│   - available   │  │   - subtotal    │  │   - price       │  │   - status      │
│   - updated_at  │  │   - total       │  │   - created_at  │  │   - tx_id       │
│                 │  │   - created_at  │  │                 │  │   - created_at  │
│                 │  │   - updated_at  │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│    CART_DB      │  │   COUPON_DB     │  │   REVIEW_DB     │  │ NOTIFICATION_DB │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • cart_items    │  │ • coupons       │  │ • reviews       │  │ • notifications │
│   - id (PK)     │  │   - id (PK)     │  │   - id (PK)     │  │   - id (PK)     │
│   - user_id     │  │   - code        │  │   - product_id  │  │   - user_id     │
│   - product_id  │  │   - type        │  │   - user_id     │  │   - type        │
│   - quantity    │  │   - value       │  │   - rating      │  │   - channel     │
│   - created_at  │  │   - min_amount  │  │   - comment     │  │   - subject     │
│   - updated_at  │  │   - valid_from  │  │   - created_at  │  │   - content     │
│                 │  │   - valid_until │  │   - updated_at  │  │   - status      │
│                 │  │   - active      │  │                 │  │   - created_at  │
│                 │  │   - created_at  │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘

┌─────────────────┐
│    ADMIN_DB     │
├─────────────────┤
│ • admins        │
│   - id (PK)     │
│   - email       │
│   - role        │
│   - created_at  │
└─────────────────┘
```

---

## Infrastructure Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                                   │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │   PostgreSQL    │     │      Redis      │     │   Elasticsearch │
    │   Port: 5433    │     │   Port: 6379    │     │   Port: 9200    │
    ├─────────────────┤     ├─────────────────┤     ├─────────────────┤
    │                 │     │                 │     │                 │
    │  Databases:     │     │  Usage:         │     │  Usage:         │
    │  • auth_db      │     │  • Rate Limiter │     │  • Product      │
    │  • user_db      │     │  • Session Cache│     │    Search       │
    │  • product_db   │     │  • Product Cache│     │  • Autocomplete │
    │  • category_db  │     │  • Category Cache│    │  • Faceted      │
    │  • inventory_db │     │  • Cart Cache   │     │    Filtering    │
    │  • order_db     │     │  • Order Cache  │     │  • Analytics    │
    │  • payment_db   │     │  • JWT Blacklist│     │                 │
    │  • cart_db      │     │                 │     │                 │
    │  • coupon_db    │     │                 │     │                 │
    │  • review_db    │     │                 │     │                 │
    │  • notif_db     │     │                 │     │                 │
    │  • admin_db     │     │                 │     │                 │
    │                 │     │                 │     │                 │
    │  Volumes:       │     │  Volumes:       │     │  Volumes:       │
    │  • pgdata       │     │  • redis_data   │     │  • es_data      │
    └─────────────────┘     └─────────────────┘     └─────────────────┘

    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │     Kafka       │     │   Zookeeper     │     │    Kafka UI     │
    │   Port: 9092    │     │   Port: 2181    │     │   Port: 9093    │
    ├─────────────────┤     ├─────────────────┤     ├─────────────────┤
    │                 │     │                 │     │                 │
    │  Topics:        │     │  Coordination:  │     │  Management:    │
    │  • order.       │     │  • Leader       │     │  • Topic        │
    │    commands     │     │    Election     │     │    Browser      │
    │  • payment.     │     │  • Configuration│     │  • Message      │
    │    events       │     │    Management   │     │    Viewer       │
    │  • product.     │     │  • Cluster      │     │  • Consumer     │
    │    events       │     │    State        │     │    Lag Monitor  │
    │  • inventory.   │     │                 │     │                 │
    │    events       │     │                 │     │                 │
    │  • notification.│     │                 │     │                 │
    │    events       │     │                 │     │                 │
    │  • user.events  │     │                 │     │                 │
    │  • coupon.events│     │                 │     │                 │
    │                 │     │                 │     │                 │
    │  Partitions:    │     │                 │     │                 │
    │  • 3 per topic  │     │                 │     │                 │
    │  • Replication: │     │                 │     │                 │
    │    1            │     │                 │     │                 │
    └─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## Security Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY FLOW                                        │
└─────────────────────────────────────────────────────────────────────────────┘

    Client
      │
      │ 1. POST /api/v1/auth/login
      │    { email, password }
      ▼
  [API Gateway]
      │
      │ 2. Forward to Auth Service
      ▼
  [Auth Service]
      │
      │ 3. Validate credentials (AES-256-GCM encrypted passwords)
      │ 4. Generate JWT (Access: 1h, Refresh: 24h)
      │ 5. Return JWT tokens
      ▼
  Client
      │
      │ 6. Subsequent requests with Authorization: Bearer <token>
      ▼
  [API Gateway]
      │
      │ 7. JWT Filter validates token
      │ 8. Extract user ID & roles
      │ 9. Forward to downstream service
      ▼
  [Microservice]
      │
      │ 10. Process request with authenticated user context
      ▼
  Response

    Security Layers:
    ┌─────────────────────────────────────────────────────────────┐
    │ 1. Network: Docker network isolation (product-net)          │
    │ 2. Transport: HTTPS/TLS (production)                        │
    │ 3. Gateway: JWT validation, rate limiting, CORS             │
    │ 4. Service: Internal authentication (optional)              │
    │ 5. Data: AES-256-GCM password encryption                    │
    │ 6. Database: Connection pooling, parameterized queries      │
    └─────────────────────────────────────────────────────────────┘
```

---

## Technology Stack Summary

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 17 | Primary language |
| **Framework** | Spring Boot | 3.3.5 | Application framework |
| **Cloud** | Spring Cloud | 2023.0.3 | Microservices patterns |
| **Gateway** | Spring Cloud Gateway | - | API Gateway |
| **Discovery** | Netflix Eureka | - | Service registry |
| **Messaging** | Apache Kafka | 7.6.1 | Event streaming |
| **Coordination** | Zookeeper | 7.6.1 | Kafka cluster management |
| **Database** | PostgreSQL | 15 | Primary data store |
| **Cache** | Redis | 7 | Caching & rate limiting |
| **Search** | Elasticsearch | 8.19.5 | Full-text search |
| **Security** | JJWT | 0.11.5 | JWT authentication |
| **Encryption** | AES-256-GCM | - | Password encryption |
| **Resilience** | Resilience4j | 2.2.0 | Circuit breaker, retry |
| **Mapping** | MapStruct | 1.6.0 | DTO mapping |
| **Boilerplate** | Lombok | 1.18.34 | Code generation |
| **API Docs** | SpringDoc OpenAPI | 2.6.0 | Swagger UI |
| **Testing** | TestContainers | 1.20.3 | Integration testing |
| **Build** | Maven | - | Dependency management |
| **Container** | Docker | - | Containerization |
| **Frontend** | Angular | 20.1.0 | Customer & Admin UI |
| **Payment** | Stripe SDK | - | Payment processing |

---

## Key Architectural Patterns

### 1. Microservices Architecture
- **16 independent services** with clear domain boundaries
- **Database-per-service** pattern for data isolation
- **Independent deployability** and scalability

### 2. Event-Driven Architecture
- **Apache Kafka** for async, decoupled communication
- **Event sourcing** for order state management
- **Eventual consistency** across services

### 3. Saga Pattern
- **Orchestration-based** saga for order processing
- **Compensating transactions** for failure scenarios
- **Event choreography** via Kafka

### 4. CQRS (Command Query Responsibility Segregation)
- **Write model**: PostgreSQL (ACID transactions)
- **Read model**: Elasticsearch (full-text search)
- **Event synchronization** between models

### 5. API Gateway Pattern
- **Single entry point** for all clients
- **Cross-cutting concerns**: Auth, rate limiting, logging
- **Dynamic routing** via service discovery

### 6. Circuit Breaker Pattern
- **Resilience4j** for fault tolerance
- **Fallback mechanisms** for service failures
- **Retry with exponential backoff**

### 7. Cache-Aside Pattern
- **Redis** for frequently accessed data
- **Cache invalidation** on data updates
- **TTL-based expiration**

---

## Service Dependency Graph

```
                        ┌─────────────┐
                        │   Client    │
                        └──────┬──────┘
                               │
                               ▼
                        ┌─────────────┐
                        │ API Gateway │
                        └──────┬──────┘
                               │
                               ▼
                        ┌─────────────┐
                        │   Eureka    │
                        │   Server    │
                        └──────┬──────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│ Auth Service │      │ User Service │      │   Product    │
│   (8087)     │      │   (8086)     │      │   Service    │
│              │      │              │      │   (8081)     │
│ Depends on:  │      │ Depends on:  │      │              │
│ • User Svc   │      │ • Kafka      │      │ Depends on:  │
│ • Kafka      │      │ • Redis      │      │ • Category   │
│ • Redis      │      │ • Eureka     │      │ • Kafka      │
│ • PostgreSQL │      │ • PostgreSQL │      │ • Redis      │
└──────────────┘      └──────────────┘      │ • PostgreSQL │
                                                │ • Elasticsearch│
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Cart       │      │   Coupon     │      │   Review     │
│   Service    │      │   Service    │      │   Service    │
│   (8088)     │      │   (8089)     │      │   (8090)     │
│              │      │              │      │              │
│ Depends on:  │      │ Depends on:  │      │ Depends on:  │
│ • Product    │      │ • Product    │      │ • Product    │
│ • Kafka      │      │ • Kafka      │      │ • Kafka      │
│ • Redis      │      │ • Redis      │      │ • Redis      │
│ • PostgreSQL │      │ • PostgreSQL │      │ • PostgreSQL │
└──────────────┘      └──────────────┘      └──────────────┘

        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  Inventory   │      │    Order     │      │   Payment    │
│   Service    │      │   Service    │      │   Service    │
│   (8083)     │      │   (8084)     │      │   (8085)     │
│              │      │              │      │              │
│ Depends on:  │      │ Depends on:  │      │ Depends on:  │
│ • Product    │      │ • Payment    │      │ • Kafka      │
│ • Kafka      │      │ • Inventory  │      │ • Redis      │
│ • Redis      │      │ • Kafka      │      │ • PostgreSQL │
│ • PostgreSQL │      │ • Redis      │      │ • Eureka     │
│ • Eureka     │      │ • PostgreSQL │      │              │
└──────────────┘      │ • Eureka     │      └──────────────┘
                       └──────────────┘

        │                      │
        │                      ▼
        │              ┌──────────────┐
        │              │ Notification │
        │              │   Service    │
        │              │   (8091)     │
        │              │              │
        │              │ Depends on:  │
        │              │ • Kafka      │
        │              │ • PostgreSQL │
        │              └──────────────┘
        │
        ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│    Elastic   │      │    Admin     │      │  Category    │
│   Service    │      │   Service    │      │   Service    │
│   (8092)     │      │   (8093)     │      │   (8082)     │
│              │      │              │      │              │
│ Depends on:  │      │ Depends on:  │      │ Depends on:  │
│ • Product    │      │ • All Svc    │      │ • Product    │
│ • Kafka      │      │ • Kafka      │      │ • Redis      │
│ • Redis      │      │ • PostgreSQL │      │ • PostgreSQL │
│ • Elasticsearch│     │ • Eureka     │      │ • Eureka     │
└──────────────┘      └──────────────┘      └──────────────┘
```

---

## Data Flow: Complete Order Journey

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE ORDER JOURNEY (Saga Pattern)                      │
└─────────────────────────────────────────────────────────────────────────────┘

    Customer
      │
      │ 1. Browse Products
      ├──────────────────────────────────────┐
      │                                      │
      ▼                                      ▼
  [API Gateway]                        [Elastic Service]
      │                                      │
      │ 2. Search/Filter                     │ 3. Full-text search
      ▼                                      │
  [Product Service] ◄───────────────────────┘
      │
      │ 4. Get product details
      ▼
  [Category Service]
      │
      │ 5. Get category info
      ▼
  Customer adds to cart
      │
      │ 6. Add to cart
      ▼
  [API Gateway]
      │
      ▼
  [Cart Service]
      │
      │ 7. Save cart item
      ▼
  [Redis] ← Cart cache

    Customer proceeds to checkout
      │
      │ 8. Place order
      ▼
  [API Gateway]
      │
      ▼
  [Order Service]
      │
      │ 9. Validate cart
      ├─────────────────┐
      │                 │
      ▼                 ▼
  [Cart Service]   [Product Service]
      │                 │
      │ 10. Get cart     │ 11. Get product details
      │                 │
      └────────┬────────┘
               │
               ▼
      Calculate totals
               │
               ▼
      12. Save order (PENDING)
               │
               ▼
      [PostgreSQL: order_db]
               │
               ▼
      13. Publish: OrderCommand (RESERVE)
               │
               ▼
      [Kafka: order.commands]
               │
               ▼
      [Inventory Service]
               │
               │ 14. Consume OrderCommand
               │ 15. Reserve inventory
               ▼
      [PostgreSQL: inventory_db]
               │
               ▼
      16. Publish: InventoryEvent (RESERVED)
               │
               ▼
      [Kafka: inventory.events]
               │
               ▼
      [Order Service]
               │
               │ 17. Consume InventoryEvent
               │ 18. Update order status (RESERVED)
               ▼
      [PostgreSQL: order_db]
               │
               ▼
      19. Publish: NotificationEvent (ORDER_CREATED)
               │
               ▼
      [Kafka: notification.events]
               │
               ▼
      [Notification Service]
               │
               │ 20. Consume NotificationEvent
               │ 21. Send order confirmation email
               ▼
      Customer receives email

    Customer initiates payment
      │
      │ 22. POST /api/v1/payment
      ▼
  [API Gateway]
      │
      ▼
  [Payment Service]
      │
      │ 23. Create payment intent
      │ 24. Process with Stripe/Razorpay
      ▼
  [Payment Gateway]
      │
      │ 25. Payment successful
      ▼
  [Payment Service]
      │
      │ 26. Save payment record
      ▼
  [PostgreSQL: payment_db]
      │
      ▼
  27. Publish: PaymentEvent (SUCCESS)
      │
      ▼
  [Kafka: payment.events]
      │
      ▼
  [Order Service]
      │
      │ 28. Consume PaymentEvent
      │ 29. Update order status (CONFIRMED)
      ▼
  [PostgreSQL: order_db]
      │
      ▼
  30. Publish: NotificationEvent (ORDER_CONFIRMED)
      │
      ▼
  [Kafka: notification.events]
      │
      ▼
  [Notification Service]
      │
      │ 31. Send payment confirmation
      ▼
  Customer receives confirmation

    ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─

    FAILURE SCENARIO: Payment Failed
      │
      ▼
  [Payment Service]
      │
      │ 26. Publish: PaymentEvent (FAILED)
      ▼
  [Kafka: payment.events]
      │
      ▼
  [Order Service]
      │
      │ 28. Consume PaymentEvent
      │ 29. Update order status (REJECTED)
      │ 30. Publish: OrderCommand (RELEASE)
      ▼
  [Kafka: order.commands]
      │
      ▼
  [Inventory Service]
      │
      │ 31. Consume OrderCommand (RELEASE)
      │ 32. Release reserved inventory
      ▼
  [PostgreSQL: inventory_db]
      │
      ▼
  33. Publish: InventoryEvent (RELEASED)
      │
      ▼
  [Order Service]
      │
      │ 34. Update order status (CANCELLED)
      ▼
  [PostgreSQL: order_db]
      │
      ▼
  35. Publish: NotificationEvent (ORDER_FAILED)
      │
      ▼
  [Notification Service]
      │
      │ 36. Send failure notification
      ▼
  Customer receives failure notification
```

---

## Common Library (shared across all services)

```
common-lib/
├── src/main/java/com/shopfast/common/
│   ├── constants/
│   │   └── ApplicationConstants.java
│   │
│   ├── dto/
│   │   ├── AdminOrderDto.java
│   │   ├── CategoryDto.java
│   │   ├── GenericApiResponseDto.java
│   │   ├── PagedResponse.java
│   │   ├── ProductDto.java
│   │   └── SearchResult.java
│   │
│   ├── enums/
│   │   ├── NotificationChannel.java
│   │   ├── NotificationStatus.java
│   │   └── NotificationType.java
│   │
│   ├── events/
│   │   ├── CartItemDto.java
│   │   ├── CouponLineItemDto.java
│   │   ├── CouponRedeemRequestDto.java
│   │   ├── CouponValidateRequestDto.java
│   │   ├── CouponValidateResponseDto.java
│   │   ├── InventoryEvent.java
│   │   ├── NotificationEvent.java
│   │   ├── OrderCommand.java
│   │   ├── PaymentEvent.java
│   │   └── ProductEvent.java
│   │
│   ├── exceptions/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   └── BadRequestException.java
│   │
│   ├── model/
│   │   └── elastic/
│   │       └── Product.java
│   │
│   └── utils/
│       └── PasswordEncryptionUtil.java
```

---

## Deployment Architecture (Docker Compose)

```yaml
# docker-compose.yml - Complete Infrastructure
services:
  # Service Discovery
  - eureka-server (8761)

  # API Gateway
  - api-gateway (8080)

  # Microservices
  - auth-service (8087)
  - user-service (8086)
  - product-service (8081)
  - category-service (8082)
  - inventory-service (8083)
  - order-service (8084)
  - payment-service (8085)
  - cart-service (8088)
  - coupon-service (8089)
  - review-service (8090)
  - notification-service (8091)
  - elastic-service (8092)
  - admin-service (8093)

  # Infrastructure
  - postgres (5432)
  - redis (6379)
  - elasticsearch (9200)
  - zookeeper (2181)
  - kafka (9092)
  - kafka-ui (9093)

# Network: product-net (bridge)
# Volumes: pgdata, redis_data, es_data
```

---

## Summary

This architecture represents a **production-grade, cloud-native microservices platform** designed for:

- **Scalability**: Each service scales independently
- **Resilience**: Circuit breakers, retries, and fallbacks
- **Maintainability**: Clear domain boundaries, shared library
- **Observability**: Actuator, Prometheus metrics, distributed tracing ready
- **Security**: JWT, encryption, rate limiting at multiple layers
- **Performance**: Redis caching, Elasticsearch search, async messaging

The system handles the complete e-commerce lifecycle from product browsing to order fulfillment, payment processing, and notifications — all orchestrated through event-driven microservices communication.
