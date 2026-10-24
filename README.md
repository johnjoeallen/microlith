# microlith: the macro service

One Spring Boot 4 / Java 25 codebase that runs as a monolith or as several services, chosen by configuration.
Modules talk to each other only through `@RpcApi` interfaces (direct call in-process, JSON-RPC 2.0 when split).

Developer guide: [`docs/README-dev.md`](docs/README-dev.md). Design: [`docs/design/macro-service.md`](docs/design/macro-service.md). Build plan: [`docs/design/staged-prompt.md`](docs/design/staged-prompt.md).

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

## The order-fulfilment domain (stage 5)

Eight modules, each with its own `-api`, `-impl`, schema, Flyway location and pool. They call each other only through
`@RpcApi` interfaces (graph from section 16.1, checked exactly by `DependencyGraphTests`):

| Module | Calls |
| --- | --- |
| `catalog`, `customers` | none |
| `pricing` | catalog, customers |
| `inventory` | catalog |
| `payments`, `notifications` | customers |
| `shipping` | customers, inventory |
| `orders` | customers, pricing, shipping, inventory, payments, notifications |

`orders.placeOrder(requestId, customerId, lines)` is a saga that lives in `orders` only: customer check, pricing quote,
shipping quote, reserve stock, authorize payment, create shipment, capture, notify. The order state
(`PENDING -> RESERVED -> AUTHORIZED -> CONFIRMED`, or `REJECTED` / `FAILED`) is persisted, every step is idempotent through
a request id derived from the order's (`<requestId>:reserve`, `:authorize`, `:ship`, `:capture`), and a repeated call resumes
where the order stopped. Compensations (`inventory.release`, `payments.void`) and the confirmation message go through
a module-local outbox written in the same transaction as the state change; a relay retries them with exponential backoff
(`modules.orders.outbox.*`), so a callee outage never fails the order. The relay scans the database only when woken by a
commit, while work is pending, or on a slow idle scan (`idle-scan-interval`, 60s), so an idle module holds no connection.

`payments.voidAuthorization` is the design's `payments.void` (Java cannot name a method `void`).

Demo data (profile `demo`): products `sku-widget` (1000), `sku-gadget` (2500), `sku-oos` (no stock), `sku-undeliverable`
(shipping always fails); customers `cust-standard`, `cust-gold` (10% off), `cust-declined` (card is declined). Amounts are cents.

```bash
call() { curl -s -X POST localhost:8080/rpc/orders -H 'Content-Type: application/json' -d "{\"jsonrpc\":\"2.0\",\"method\":\"orders.placeOrder\",\"params\":{\"requestId\":\"$1\",\"customerId\":\"$2\",\"lines\":[{\"sku\":\"$3\",\"quantity\":$4}]},\"id\":1}"; echo; }
call happy-1      cust-standard sku-widget        2   # CONFIRMED, total 2700
call gold-1       cust-gold     sku-widget        2   # CONFIRMED, 200 off, total 2500
call oos-1        cust-standard sku-oos           1   # error 1001 OutOfStockException
call declined-1   cust-declined sku-widget        1   # REJECTED, reservation released by the relay
call undeliver-1  cust-standard sku-undeliverable 1   # FAILED, payment voided and stock released by the relay
call happy-1      cust-standard sku-widget        2   # same requestId: the same order, nothing duplicated
```

## Running the demo modes (stage 6)

Needs a JDK, Docker, `curl` and `python3`. The jars are built on first use (`./mvnw -q -DskipTests package`).

```bash
scripts/db.sh up                       # three PostgreSQL instances (A 5432, B 5433, C 5434), schemas and roles bootstrapped
scripts/demo.sh groups up              # any mode: modulith macro-monolith groups microservices microservices-macro loopback-codec loopback-http split-dbs
scripts/smoke.sh                       # the scenarios of section 16.5 + the expected binding on every edge + the merged call tree
scripts/trace.sh <correlationId>       # the merged call tree of one order, across every running process
scripts/demo.sh groups status
scripts/demo.sh groups down
scripts/db.sh down                     # wipes the data
scripts/demo-all.sh                    # every mode on a clean database, with a summary table (pass/fail, bindings seen)
```

| Mode | Processes | Cross-module path |
| --- | --- | --- |
| `modulith` | `app-modulith` on 8080 | all local |
| `macro-monolith` | `app-macro` (`monolith`) on 8080 | all local |
| `groups` | `app-macro` as `front` 8081, `fulfilment` 8082, `accounts` 8083 | local inside a group, remote across |
| `microservices` | the eight `app-<module>-service` apps on 8081-8088 | all remote |
| `microservices-macro` | `app-macro` eight times, one module enabled each (profile = module name) | all remote |
| `loopback-codec`, `loopback-http` | `app-macro` on 8080 | every call through the JSON codec / over HTTP to 127.0.0.1 |
| `split-dbs` | as `groups`, profile `split-dbs`: front on A, fulfilment on B, accounts on C | as `groups` |

`app-macro` has no module wiring baked in; profiles do it (`monolith`, `front`, `fulfilment`, `accounts`, one per module,
`loopback-codec`, `loopback-http`, `split-dbs`), or environment variables, for example
`MODULES_INVENTORY_ENABLED=false MODULES_INVENTORY_URL=http://host:8084/rpc/inventory`.
The eight service apps are generated from one template: `scripts/new-service-app.sh --all` (`--check` finds a stale one).
`app-modulith` refuses to start with any module disabled or bound to anything but `local`.

With the `demo` profile (it implies `dev`) every process also serves:

| Endpoint | What |
| --- | --- |
| `GET /demo/topology` | per module: enabled, binding, URL, datasource host, port, schema (never the password) |
| `GET /demo/trace?correlationId=...` | the cross-module calls this process recorded: caller, callee, method, binding actually used, depth, duration, outcome |
| `POST /demo/faults` | `{"notificationsDown":true,"paymentsDecline":true,"inventoryLatencyMs":300}`; only applies to modules enabled in that process |

Every `placeOrder` gets a correlation id (or takes `X-Correlation-Id`); it travels in the MDC, in the `X-Correlation-Id` header on
remote calls and through the outbox, so the relay's compensating calls stay attached to the order. Each call is also logged, for
example `pricing -> catalog.getProduct [local] 1 ms ok`.

## API documentation (stage 7)

OpenAPI 3.1 and OpenRPC documents are generated at runtime from the `@RpcApi` interfaces (module `platform/rpc-docs`); nothing generated is committed. A process serves the documents of the modules it runs, only when `rpc.server.enabled=true`:

| URL | Content |
|---|---|
| `/rpc/{module}/openapi.json`, `/rpc/{module}/openrpc.json` | one module |
| `/rpc/openapi.json`, `/rpc/openrpc.json` | all modules of this process |
| `/swagger-ui` | Swagger UI, **dev profile only** (so also in `demo`) |

```bash
scripts/db.sh up && scripts/demo.sh macro-monolith up
open http://127.0.0.1:8080/swagger-ui        # pick "inventory", try inventory.getStock
curl -s localhost:8080/rpc/inventory/openapi.json | jq '.paths | keys'
```

Descriptions are optional: put `@Operation(summary = ...)` on a method, `@Parameter(description = ...)` on a parameter or `@Schema(description = ...)` on a record or component in the `-api` artifact (`swagger-annotations-jakarta` at `provided` scope; the annotations jar is on every runtime classpath through `rpc-docs`). Records are described as Jackson writes them: all components are required, and a component is nullable only if annotated with an annotation named `Nullable`.

`mvn -pl apps/app-macro -am verify` also writes the documents of all modules to `apps/app-macro/target/api-docs/` (`openapi.json`, `openrpc.json` and one pair per module) for CI to publish.

## Hardening and compatibility (stage 8)

- **Boundary interceptor.** Calls to a *local* module behave like remote ones: they are observed as `macro.rpc.client{module,method,binding,outcome}` (the same series a remote call produces) and the caller's transaction is suspended for the call. Switch the suspension off with `modules.boundary.suspend-transactions=false`.
- **API compatibility.** japicmp compares each `-api` jar with its last release and fails the build on removed or changed methods, constructors, record components and exception types; added methods pass. It is skipped until you name a baseline:

  ```bash
  mvn verify -Dapi.compat.skip=false -Dapi.baseline.version=1.0.0     # the last released version, from your repository
  scripts/check-api-compat.sh                                         # proves the check bites (compatible changes pass, breaking ones fail)
  ```

  To add a field to a record without breaking Java callers, keep the old constructor and delegate to the new one.
- **Failure tests.** `ClientProxyFailureTests` (timeout, refused or cut connection, JSON-RPC error, malformed answer, retry of an idempotent method) and `DomainFailureTests` (eight processes, callee killed mid-call, garbage answer, callee process restarted, orders process restarted with the outbox pending; always exactly one of every side effect).

## Layout

| Path | Purpose |
| --- | --- |
| `platform/rpc-api` | `@RpcApi`, RPC exceptions (no Spring) |
| `platform/module-support` | module stereotypes, enablement, per-module persistence helpers |
| `platform/rpc-spring-boot-starter` | JSON-RPC server, client proxies, registrar |
| `apps/demo-scenarios` | the section 16.5 scenarios and API contracts, written once against the `-api` artifacts (tests only) |
| `modules/<m>/<m>-api` | interface, record DTOs, exceptions; the only thing other modules may depend on |
| `modules/<m>/<m>-impl` | implementation, persistence, migrations |
| `platform/demo-support` | demo profile only: call trace, `/demo/*` endpoints, topology banner |
| `apps/*` | deployable assemblies (`app-modulith`, `app-macro`, `app-<module>-service`) |

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
