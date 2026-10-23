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
-- Stage 5 adds the remaining six modules in the same shape.

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
