-- Per-module schema and roles for the macro service (demo passwords; use a secret store anywhere real).
--
-- For each module <m>:
--   <m>_migrator  owns the schema, runs Flyway, may create/alter objects in it (DDL)
--   <m>_app       runtime role: DML only, on that schema only
-- Neither role can see another module's schema, so a cross-schema query fails with "permission denied".
-- The search_path is set on the role, so unqualified names resolve to the module's own schema.
--
-- Run once per PostgreSQL instance, as a superuser, against the application database:
--   psql -U postgres -d macro -f docs/db/bootstrap.sql

-- ===== catalog =====
CREATE ROLE catalog_migrator LOGIN PASSWORD 'catalog_migrator';
CREATE ROLE catalog_app      LOGIN PASSWORD 'catalog_app';
CREATE SCHEMA catalog AUTHORIZATION catalog_migrator;
REVOKE ALL ON SCHEMA catalog FROM PUBLIC;
GRANT USAGE ON SCHEMA catalog TO catalog_app;
ALTER DEFAULT PRIVILEGES FOR ROLE catalog_migrator IN SCHEMA catalog
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO catalog_app;
ALTER DEFAULT PRIVILEGES FOR ROLE catalog_migrator IN SCHEMA catalog
    GRANT USAGE, SELECT ON SEQUENCES TO catalog_app;
ALTER ROLE catalog_migrator SET search_path = catalog;
ALTER ROLE catalog_app      SET search_path = catalog;

-- ===== customers =====
CREATE ROLE customers_migrator LOGIN PASSWORD 'customers_migrator';
CREATE ROLE customers_app      LOGIN PASSWORD 'customers_app';
CREATE SCHEMA customers AUTHORIZATION customers_migrator;
REVOKE ALL ON SCHEMA customers FROM PUBLIC;
GRANT USAGE ON SCHEMA customers TO customers_app;
ALTER DEFAULT PRIVILEGES FOR ROLE customers_migrator IN SCHEMA customers
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO customers_app;
ALTER DEFAULT PRIVILEGES FOR ROLE customers_migrator IN SCHEMA customers
    GRANT USAGE, SELECT ON SEQUENCES TO customers_app;
ALTER ROLE customers_migrator SET search_path = customers;
ALTER ROLE customers_app      SET search_path = customers;

-- ===== pricing =====
CREATE ROLE pricing_migrator LOGIN PASSWORD 'pricing_migrator';
CREATE ROLE pricing_app      LOGIN PASSWORD 'pricing_app';
CREATE SCHEMA pricing AUTHORIZATION pricing_migrator;
REVOKE ALL ON SCHEMA pricing FROM PUBLIC;
GRANT USAGE ON SCHEMA pricing TO pricing_app;
ALTER DEFAULT PRIVILEGES FOR ROLE pricing_migrator IN SCHEMA pricing
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pricing_app;
ALTER DEFAULT PRIVILEGES FOR ROLE pricing_migrator IN SCHEMA pricing
    GRANT USAGE, SELECT ON SEQUENCES TO pricing_app;
ALTER ROLE pricing_migrator SET search_path = pricing;
ALTER ROLE pricing_app      SET search_path = pricing;

-- ===== inventory =====
CREATE ROLE inventory_migrator LOGIN PASSWORD 'inventory_migrator';
CREATE ROLE inventory_app      LOGIN PASSWORD 'inventory_app';
CREATE SCHEMA inventory AUTHORIZATION inventory_migrator;
REVOKE ALL ON SCHEMA inventory FROM PUBLIC;
GRANT USAGE ON SCHEMA inventory TO inventory_app;
ALTER DEFAULT PRIVILEGES FOR ROLE inventory_migrator IN SCHEMA inventory
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO inventory_app;
ALTER DEFAULT PRIVILEGES FOR ROLE inventory_migrator IN SCHEMA inventory
    GRANT USAGE, SELECT ON SEQUENCES TO inventory_app;
ALTER ROLE inventory_migrator SET search_path = inventory;
ALTER ROLE inventory_app      SET search_path = inventory;

-- ===== payments =====
CREATE ROLE payments_migrator LOGIN PASSWORD 'payments_migrator';
CREATE ROLE payments_app      LOGIN PASSWORD 'payments_app';
CREATE SCHEMA payments AUTHORIZATION payments_migrator;
REVOKE ALL ON SCHEMA payments FROM PUBLIC;
GRANT USAGE ON SCHEMA payments TO payments_app;
ALTER DEFAULT PRIVILEGES FOR ROLE payments_migrator IN SCHEMA payments
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO payments_app;
ALTER DEFAULT PRIVILEGES FOR ROLE payments_migrator IN SCHEMA payments
    GRANT USAGE, SELECT ON SEQUENCES TO payments_app;
ALTER ROLE payments_migrator SET search_path = payments;
ALTER ROLE payments_app      SET search_path = payments;

-- ===== shipping =====
CREATE ROLE shipping_migrator LOGIN PASSWORD 'shipping_migrator';
CREATE ROLE shipping_app      LOGIN PASSWORD 'shipping_app';
CREATE SCHEMA shipping AUTHORIZATION shipping_migrator;
REVOKE ALL ON SCHEMA shipping FROM PUBLIC;
GRANT USAGE ON SCHEMA shipping TO shipping_app;
ALTER DEFAULT PRIVILEGES FOR ROLE shipping_migrator IN SCHEMA shipping
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO shipping_app;
ALTER DEFAULT PRIVILEGES FOR ROLE shipping_migrator IN SCHEMA shipping
    GRANT USAGE, SELECT ON SEQUENCES TO shipping_app;
ALTER ROLE shipping_migrator SET search_path = shipping;
ALTER ROLE shipping_app      SET search_path = shipping;

-- ===== notifications =====
CREATE ROLE notifications_migrator LOGIN PASSWORD 'notifications_migrator';
CREATE ROLE notifications_app      LOGIN PASSWORD 'notifications_app';
CREATE SCHEMA notifications AUTHORIZATION notifications_migrator;
REVOKE ALL ON SCHEMA notifications FROM PUBLIC;
GRANT USAGE ON SCHEMA notifications TO notifications_app;
ALTER DEFAULT PRIVILEGES FOR ROLE notifications_migrator IN SCHEMA notifications
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO notifications_app;
ALTER DEFAULT PRIVILEGES FOR ROLE notifications_migrator IN SCHEMA notifications
    GRANT USAGE, SELECT ON SEQUENCES TO notifications_app;
ALTER ROLE notifications_migrator SET search_path = notifications;
ALTER ROLE notifications_app      SET search_path = notifications;

-- ===== orders =====
CREATE ROLE orders_migrator LOGIN PASSWORD 'orders_migrator';
CREATE ROLE orders_app      LOGIN PASSWORD 'orders_app';
CREATE SCHEMA orders AUTHORIZATION orders_migrator;
REVOKE ALL ON SCHEMA orders FROM PUBLIC;
GRANT USAGE ON SCHEMA orders TO orders_app;
ALTER DEFAULT PRIVILEGES FOR ROLE orders_migrator IN SCHEMA orders
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO orders_app;
ALTER DEFAULT PRIVILEGES FOR ROLE orders_migrator IN SCHEMA orders
    GRANT USAGE, SELECT ON SEQUENCES TO orders_app;
ALTER ROLE orders_migrator SET search_path = orders;
ALTER ROLE orders_app      SET search_path = orders;
