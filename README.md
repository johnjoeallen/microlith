# microlith: the macro service

One Spring Boot 4 / Java 25 codebase that runs as a monolith or as several services, chosen by configuration.
Modules talk to each other only through `@RpcApi` interfaces (direct call in-process, JSON-RPC 2.0 when split).

Developer guide: [`docs/README-dev.md`](docs/README-dev.md). Design: [`docs/design/macro-service.md`](docs/design/macro-service.md). Build plan: [`docs/design/staged-prompt.md`](docs/design/staged-prompt.md).

## Quick start: `scripts/demo.sh`

```bash
scripts/demo.sh <mode> start      # reset PostgreSQL to a clean initialised state, build the jars on first use, start the mode, wait until ready
scripts/demo.sh <mode> status     # which processes are up
scripts/demo.sh <mode> stop       # stop the processes, then PostgreSQL
```

Modes: `modulith`, `macro-monolith`, `groups`, `microservices`, `microservices-macro`, `loopback-codec`, `loopback-http`, `split-dbs`.
Only one mode runs at a time; `start` refuses while another is running. Logs and PID files are under `.demo/`.

The orders entry point and the shop are on **9181** in every mode:

- Shop: <http://localhost:9181/shop>
- Swagger UI: <http://localhost:9181/swagger-ui>
- Topology: <http://localhost:9181/demo/topology>
- JSON-RPC: `POST http://localhost:9181/rpc/orders`

Other processes: `groups` and `split-dbs` use 9181 `front`, 9182 `fulfilment`, 9183 `accounts`; `microservices` and
`microservices-macro` use 9181-9188 (orders, catalog, pricing, inventory, customers, payments, shipping, notifications).
Every `start` wipes and re-initialises the databases, so each run begins clean. Details: [Running the demo modes](#running-the-demo-modes-stage-6).

## Performance: `scripts/perf.sh`

With a mode running (`scripts/demo.sh <mode> start`):

```bash
scripts/perf.sh                                  # 200 orders, 4 concurrent clients, 20 warmup orders
scripts/perf.sh --orders 1000 --concurrency 16
scripts/perf.sh --duration 60 --concurrency 8    # timed run instead of a fixed count
scripts/perf.sh --no-trace                       # client round trips only (least overhead)
scripts/perf.sh --modes groups,microservices     # start, measure and stop each mode on a clean database, then compare
scripts/perf.sh --modes all --orders 300         # all eight modes, one comparison table at the end
```

A realistic comparison (the `realistic` profile, which is the default), with the `@Cached` layer off and on, in two split modes:

```bash
scripts/perf.sh --modes groups,microservices --cache both --duration 120 --warmup 500 --concurrency 8
```

That starts each mode on a clean database, loads 10,000 customers and 2,000 SKUs, warms up with 500 orders, sends 120 s of load from 8
clients (about four times the cache TTL, so hit rates settle), stops the mode, and prints one comparison table. The run takes about
10 minutes (four measured runs plus start-up). The report is written to `.demo/perf/<timestamp>/perf-<timestamp>.md`.
For a quick look at one running mode instead: `scripts/perf.sh --orders 2000 --warmup 200 --concurrency 8`.

The workload is the `realistic` profile by default: 10,000 customers and 2,000 SKUs with Zipf-skewed popularity (a few popular SKUs
and repeat customers), 1-4 lines per order, 8% gold and 2% always-declined customers (declined orders are reported separately, not
as failures). `--profile small|realistic|large` picks another (`small` = the demo's two customers and two SKUs, `large` = 100,000
and 20,000); `--customers`, `--skus`, `--customer-skew`, `--sku-skew` and `--seed` override. The driver bulk-loads the customers,
products and stock first (`--no-seed` to skip), so every `--modes` run starts from a clean database.
Use runs of at least three times the cache TTL (30 s) for meaningful hit rates.

It reports the client round trip of every `orders.placeOrder` (orders/s, error count, min/mean/p50/p90/p95/p99/max) and the
latency of every module-to-module call inside those orders, grouped by `caller -> callee.method` and binding
(`local`, `loopback-codec`, `loopback-http`, `remote`), plus calls per order. The call figures come from the
`/demo/trace` records of every running process, so they are measured on the caller's side and include the JSON-RPC hop
in the split modes. Raw samples (`orders.csv`, `calls.csv`) and `summary.json` go to `.demo/perf/<timestamp>/` (one sub-directory per mode, plus
`comparison.json`, with `--modes`).

Each run also writes a timestamped markdown report, `perf-<timestamp>.md`, next to the raw data: the parameters, commit, host,
the comparison table across modes and, per mode, the round-trip and per-call tables.
`--cache on|off|both` (with `--modes`) starts the modes with the `@Cached` layer on, off, or both, so you can compare them.
`--parallel on|off|both` does the same for orders' parallel quotes (`modules.orders.parallel-quotes`).

Notes: the clients are a closed loop on persistent connections. Tracing adds a little overhead (compare with `--no-trace`).
Each process keeps its 200,000 most recent calls (`demo.trace.capacity`), so a very long or fast run can still overwrite
traces before they are read; the driver warns when that happens (raise the capacity or lower `--concurrency`). The demo seeds only 150 units of stock, so the driver raises it
first (`--no-stock` to skip). Later async work (the notification, delivered by an event listener) is picked up by re-reading
traces until they stop changing (`--settle`, `--drain`).

## Caching: `@Cached`

An `@RpcApi` method marked `@Cached(ttlSeconds = 30, maxSize = 10_000)` has its result reused by callers (a Caffeine cache per
method, keyed by the arguments). It sits in front of **every** binding, local included, so a call costs the same wherever the module
runs, and it is skipped for the one call the RPC dispatcher makes for a remote caller (that caller has its own cache). Failures and
`null` results are never cached. Use it only for reads of slowly changing reference data: today `customers.getCustomer`,
`customers.getPaymentMethod`, `catalog.getProduct` and `catalog.productExists`. Not for anything that changes state or reads
fast-moving state (`inventory.getStock`, reservations). Results are shared between callers, so they must be immutable.
Switch it off with `rpc.cache.enabled=false` (env `RPC_CACHE_ENABLED=false`). In the demo call trace a hit has binding `cache`.

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
curl -s -X POST localhost:9181/rpc/orders -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"orders.placeOrder","params":{"request":{"requestId":"m-1","customerId":"cust-1","sku":"sku-1","quantity":2}},"id":1}'
```

(`app-modulith` serves no RPC by default; the override is only there so you can use curl.)

### Shape 2: two processes, two PostgreSQL instances (orders on A, inventory on B)

```bash
java -jar apps/app-inventory-service/target/app-inventory-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo &   # :9184, instance B
java -jar apps/app-orders-service/target/app-orders-service-0.1.0-SNAPSHOT.jar &                                     # :9181, instance A
curl -s -X POST localhost:9181/rpc/orders -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"orders.placeOrder","params":{"request":{"requestId":"s-1","customerId":"cust-1","sku":"sku-1","quantity":2}},"id":1}'
```

## Two services, raw JSON-RPC and the alias (stage 3)

```bash
java -jar apps/app-inventory-service/target/app-inventory-service-0.1.0-SNAPSHOT.jar &   # :9184
java -jar apps/app-orders-service/target/app-orders-service-0.1.0-SNAPSHOT.jar &         # :9181, calls inventory over JSON-RPC

# raw JSON-RPC 2.0 envelope (orders calls inventory over HTTP)
curl -s -X POST localhost:9181/rpc/orders -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"orders.placeOrder","params":{"request":{"requestId":"req-1","customerId":"cust-1","sku":"sku-1","quantity":2}},"id":1}'

# convenience alias: bare params in, bare result out (HTTP status carries errors)
curl -s -X POST localhost:9184/rpc/inventory/getStock -H 'Content-Type: application/json' -d '{"sku":"sku-1"}'
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
where the order stopped. Compensations (`inventory.release`, `payments.void`) and the confirmation message are Spring Modulith
events (`OrderEvents`) published in the same transaction as the state change: the event publication registry stores them in the
orders schema (`event_publication`), the listeners run asynchronously after the commit and call the other module through its
`@RpcApi`, and a failed one is redelivered with exponential backoff (`modules.orders.events.*`), so a callee outage never fails
the order. Redelivery scans the database only after a failure, shortly after start and on a slow idle scan (`idle-scan-interval`,
60s), so an idle module holds no connection. See `docs/design/spring-modulith-events.md`.

`payments.voidAuthorization` is the design's `payments.void` (Java cannot name a method `void`).

Demo data (profile `demo`): products `sku-widget` (1000), `sku-gadget` (2500), `sku-oos` (no stock), `sku-undeliverable`
(shipping always fails); customers `cust-standard`, `cust-gold` (10% off), `cust-declined` (card is declined). Amounts are cents.

```bash
call() { curl -s -X POST localhost:9181/rpc/orders -H 'Content-Type: application/json' -d "{\"jsonrpc\":\"2.0\",\"method\":\"orders.placeOrder\",\"params\":{\"requestId\":\"$1\",\"customerId\":\"$2\",\"lines\":[{\"sku\":\"$3\",\"quantity\":$4}]},\"id\":1}"; echo; }
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
scripts/demo.sh groups start             # any mode: modulith macro-monolith groups microservices microservices-macro loopback-codec loopback-http split-dbs
scripts/smoke.sh                       # the scenarios of section 16.5 + the expected binding on every edge + the merged call tree
scripts/trace.sh <correlationId>       # the merged call tree of one order, across every running process
scripts/demo.sh groups status
scripts/demo.sh groups stop
scripts/db.sh down                     # wipes the data
scripts/demo-all.sh                    # every mode on a clean database, with a summary table (pass/fail, bindings seen)
```

| Mode | Processes | Cross-module path |
| --- | --- | --- |
| `modulith` | `app-modulith` on 9181 | all local |
| `macro-monolith` | `app-macro` (`monolith`) on 9181 | all local |
| `groups` | `app-macro` as `front` 9181, `fulfilment` 9182, `accounts` 9183 | local inside a group, remote across |
| `microservices` | the eight `app-<module>-service` apps on 9181-9188 | all remote |
| `microservices-macro` | `app-macro` eight times, one module enabled each (profile = module name) | all remote |
| `loopback-codec`, `loopback-http` | `app-macro` on 9181 | every call through the JSON codec / over HTTP to 127.0.0.1 |
| `split-dbs` | as `groups`, profile `split-dbs`: front on A, fulfilment on B, accounts on C | as `groups` |

`app-macro` has no module wiring baked in; profiles do it (`monolith`, `front`, `fulfilment`, `accounts`, one per module,
`loopback-codec`, `loopback-http`, `split-dbs`), or environment variables, for example
`MODULES_INVENTORY_ENABLED=false MODULES_INVENTORY_URL=http://host:9184/rpc/inventory`.
The eight service apps are generated from one template: `scripts/new-service-app.sh --all` (`--check` finds a stale one).
`app-modulith` refuses to start with any module disabled or bound to anything but `local`.

With the `demo` profile (it implies `dev`) every process also serves:

| Endpoint | What |
| --- | --- |
| `GET /demo/topology` | per module: enabled, binding, URL, datasource host, port, schema (never the password) |
| `GET /demo/trace?correlationId=...` | the cross-module calls this process recorded: caller, callee, method, binding actually used, depth, duration, outcome |
| `POST /demo/faults` | `{"notificationsDown":true,"paymentsDecline":true,"inventoryLatencyMs":300}`; only applies to modules enabled in that process |

Every `placeOrder` gets a correlation id (or takes `X-Correlation-Id`); it travels in the MDC, in the `X-Correlation-Id` header on
remote calls and in the stored events, so the compensating calls of the listeners stay attached to the order. Each call is also logged, for
example `pricing -> catalog.getProduct [local] 1 ms ok`.

## API documentation (stage 7)

OpenAPI 3.1 and OpenRPC documents are generated at runtime from the `@RpcApi` interfaces (module `platform/rpc-docs`); nothing generated is committed. A process serves the documents of the modules it runs, only when `rpc.server.enabled=true`:

| URL | Content |
|---|---|
| `/rpc/{module}/openapi.json`, `/rpc/{module}/openrpc.json` | one module |
| `/rpc/openapi.json`, `/rpc/openrpc.json` | all modules of this process |
| `/swagger-ui` | Swagger UI, **dev profile only** (so also in `demo`) |

```bash
scripts/db.sh up && scripts/demo.sh macro-monolith start
open http://127.0.0.1:9181/swagger-ui        # pick "inventory", try inventory.getStock
curl -s localhost:9181/rpc/inventory/openapi.json | jq '.paths | keys'
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
