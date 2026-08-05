-- Create databases for all microservices
CREATE DATABASE product_db;
CREATE DATABASE category_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE user_db;
CREATE DATABASE auth_db;
CREATE DATABASE cart_db;
CREATE DATABASE coupon_db;
CREATE DATABASE review_db;
CREATE DATABASE notification_db;
CREATE DATABASE admin_db;
-- Keycloak owns its own schema and runs its own Liquibase migrations against it.
-- It must never share a database with a service or their migrations will collide.
CREATE DATABASE keycloak_db;