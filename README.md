# microlith: the macro service

One Spring Boot 4 / Java 25 codebase that runs as a monolith or as several services, chosen by configuration.
Modules talk to each other only through `@RpcApi` interfaces (direct call in-process, JSON-RPC 2.0 when split).

Design: [`docs/design/macro-service.md`](docs/design/macro-service.md). Build plan: [`docs/design/staged-prompt.md`](docs/design/staged-prompt.md).

## Requirements

JDK 25, Maven 3.9+, Docker (from stage 4, for PostgreSQL and Testcontainers).

## Build

```bash
mvn -q verify                    # everything, including tests and boundary rules
mvn -q verify -pl modules/orders/orders-impl -am   # one module and what it needs
scripts/check-boundaries.sh      # proves the Maven boundary rules fail on forbidden dependencies
```

## Databases (stage 4)

Each module owns a schema, a DDL-capable migration role and a DML-only runtime role (`docs/db/bootstrap.sql`).
Three PostgreSQL instances are available for the local demo (A 5432, B 5433, C 5434), each bootstrapped on first start:

```bash
docker compose -f docker-compose.demo.yml up -d --wait
docker compose -f docker-compose.demo.yml down -v        # stop and wipe
```

Persistence is JPA (Hibernate + Spring Data), one `EntityManagerFactory` and transaction manager per module, and
Flyway per module (`classpath:db/migration/<module>`, plus `classpath:db/demo/<module>` seed data in the `demo` profile).
A database that is down at startup does not stop the application: the module reports not-ready on
`/actuator/health/readiness` and the migration is retried in the background. Liveness is unaffected.

Per-module pool settings (`modules.<name>.datasource.*`): `max-pool-size` (3), `minimum-idle` (0), `idle-timeout` (30s),
`max-lifetime` (10m), `connection-timeout` (5s). `platform.db.connection-budget` warns when the enabled modules' pools add
up to more (`platform.db.enforce-budget=true` fails startup). The RPC server limits concurrent calls per module to the
pool size (`modules.<name>.bulkhead.max-concurrent-calls`, `max-wait`) and answers `-32002` beyond that.
Pool metrics: `macro.db.pool.connections{module,state}` and `macro.db.pool.timeouts{module}`.

### Shape 1: one process, one PostgreSQL

```bash
java -jar apps/app-modulith/target/app-modulith-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo --rpc.server.enabled=true
curl -s -X POST localhost:8080/rpc/orders -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"orders.placeOrder","params":{"request":{"requestId":"m-1","customerId":"cust-1","sku":"sku-1","quantity":2}},"id":1}'
```

(`app-modulith` serves no RPC by default; the override is only there so you can use curl.)

### Shape 2: two processes, two PostgreSQL instances (orders on A, inventory on B)

```bash
java -jar apps/app-inventory-service/target/app-inventory-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo &   # :8084, instance B
java -jar apps/app-orders-service/target/app-orders-service-0.1.0-SNAPSHOT.jar &                                     # :8081, instance A
curl -s -X POST localhost:8081/rpc/orders -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"orders.placeOrder","params":{"request":{"requestId":"s-1","customerId":"cust-1","sku":"sku-1","quantity":2}},"id":1}'
```

## Two services, raw JSON-RPC and the alias (stage 3)

```bash
java -jar apps/app-inventory-service/target/app-inventory-service-0.1.0-SNAPSHOT.jar &   # :8084
java -jar apps/app-orders-service/target/app-orders-service-0.1.0-SNAPSHOT.jar &         # :8081, calls inventory over JSON-RPC

# raw JSON-RPC 2.0 envelope (orders calls inventory over HTTP)
curl -s -X POST localhost:8081/rpc/orders -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"orders.placeOrder","params":{"request":{"requestId":"req-1","customerId":"cust-1","sku":"sku-1","quantity":2}},"id":1}'

# convenience alias: bare params in, bare result out (HTTP status carries errors)
curl -s -X POST localhost:8084/rpc/inventory/getStock -H 'Content-Type: application/json' -d '{"sku":"sku-1"}'
```

`app-modulith` serves no RPC (`rpc.server.enabled=false`); `app-macro` runs all modules (`monolith` profile by default).
Bindings per target module: `modules.<name>.binding` = `local` | `loopback-codec` | `loopback-http` | `remote`.

## Layout

| Path | Purpose |
| --- | --- |
| `platform/rpc-api` | `@RpcApi`, RPC exceptions (no Spring) |
| `platform/module-support` | module stereotypes, enablement, per-module persistence helpers |
| `platform/rpc-spring-boot-starter` | JSON-RPC server, client proxies, registrar |
| `modules/<m>/<m>-api` | interface, record DTOs, exceptions; the only thing other modules may depend on |
| `modules/<m>/<m>-impl` | implementation, persistence, migrations |
| `apps/*` | deployable assemblies (`app-modulith`, `app-macro`, per-service apps) |

Platform code lives under `io.macroservice.platform`, outside the application root package `com.acme`, so that
Spring Modulith does not treat it as a business module.

## Boundary rules (maven-enforcer-plugin)

- `-api` artifacts: only `rpc-api` and other `-api` artifacts at compile/runtime scope.
- `-impl` artifacts: no other `-impl`, no app, no starter.
- Service apps: only their own `-impl`.

## Adding a module

Add `modules/<m>/<m>-api` and `<m>-impl` (copy `inventory`), list both in the root `pom.xml` `<modules>` and
`dependencyManagement`, declare dependencies in the `-impl` pom and in `@ApplicationModule(allowedDependencies = ...)`,
add the `-impl` to `app-modulith` and `app-macro`, and add a CODEOWNERS line.
