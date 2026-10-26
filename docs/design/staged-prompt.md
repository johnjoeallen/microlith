# Staged Claude Code prompt: macro service

How to use this file:

1. Put the design document in the repository as `docs/design/macro-service.md`.
2. Start Claude Code in the repository root and paste the **Master prompt** once.
3. Paste **one stage prompt at a time**. Do not move on until the stage's definition of done is met and you have reviewed the commit.
4. If Claude Code wants to deviate from the design, it must stop and ask, as the master prompt requires.

---

## Master prompt (paste first, once)

```text
You are building the "macro service" described in docs/design/macro-service.md. Read that file completely before doing anything else. It is the authoritative design. Where this prompt and the design disagree, stop and ask me.

What we are building, in short:
- One Spring Boot 4 / Java 25 codebase using Spring Modulith and PostgreSQL, built with Maven (multi-module reactor).
- Business modules are split into <module>-api and <module>-impl artifacts. Only -api artifacts may be depended on by other modules.
- JSON-RPC 2.0 is the ONLY interaction between modules. When two modules are in one process, the call is a direct method call on the @RpcApi interface; when they are split, it is a JSON-RPC request. No cross-module events, no broker, no shared tables, no shared state, no shared transactions.
- Each module owns its own PostgreSQL database/schema, configured under modules.<name>.datasource.*, with its own Flyway migrations.
- The Java @RpcApi interface is the source of truth. OpenAPI and OpenRPC are generated from it.
- From the same code we build several app artifacts: app-modulith (all impls, all local, RPC server off), app-macro (all impls, fully config-driven), and per-service apps such as app-orders-service and app-inventory-service (only their own impl plus other modules' -api).

Build plan: stages 1 to 4 prove the mechanism with two pilot modules (orders, inventory). Stage 5 grows them into the full eight-module demo domain with real interdependence (section 16.1 of the design). Stage 6 builds the local demo harness around that domain.

Working agreement:
1. Work in stages. I will give you one stage at a time. Do only that stage.
2. Before writing code in a stage, state your plan in a few lines, including any decision you are making that the design leaves open.
3. Do not guess dependency versions. Look up the current Spring Boot 4 release and the matching Spring Modulith, Flyway, Testcontainers, ArchUnit and springdoc/swagger-core versions from authoritative sources (Maven Central, release notes) and use the BOMs. Record the chosen versions in the parent POM. Pick versions at least two weeks old.
4. Boot 4 uses Jackson 3 (package tools.jackson). Do not pull in Jackson 2 or a JSON-RPC library that requires it. Implement JSON-RPC ourselves in the starter.
5. Never use an implementation class of another module. Never add a dependency from one -impl to another -impl. If you think you need to, stop and ask.
6. Keep each stage small and reviewable. At the end of each stage: run `mvn -q verify` from the root, fix everything, then make one git commit with a message starting "stage N:". Do not start the next stage.
7. At the end of each stage, report: what you built, decisions you made, anything you could not do, and the exact commands I can run to verify it.
8. Use the placeholder root package com.acme (modules are com.acme.orders, com.acme.inventory). I will rename it later.
9. Prefer records for DTOs, constructor injection, and no Lombok.
10. We deploy on AWS and cannot use PgBouncer or any external connection pooler. Never design for or depend on one. Connections go straight from each module's HikariCP pool to PostgreSQL, and the application manages its own connection budget (see section 9.1 of the design).
11. The immediate target is a LOCAL DEMONSTRATION of the deployment modes on one developer machine (see section 16 of the design). Optimise for a clear, repeatable demo: everything runs on localhost, PostgreSQL runs in Docker, the dev profile allows loopback bindings, and security between services is out of scope for now. Do not build production-only machinery (cloud deployment, image publishing, service-to-service security) unless a stage asks for it.

Reply with a short summary of your understanding of the design (maximum 15 lines) and the list of open questions you have. Do not write code yet.
```

---

## Stage 1: Repository skeleton and build

```text
Stage 1: repository skeleton and build.

Create the Maven reactor and empty modules. No business logic yet. Orders and inventory are the two pilot modules used to prove the mechanism in stages 1 to 4; the other six modules of the demo domain are added in stage 5, so structure everything so that adding a module is mechanical.

Layout:
  pom.xml                         parent POM + BOM imports (Spring Boot 4, Spring Modulith), Java 25, -parameters, enforcer rules
  platform/rpc-api
  platform/rpc-spring-boot-starter
  platform/module-support
  modules/orders/orders-api
  modules/orders/orders-impl
  modules/inventory/inventory-api
  modules/inventory/inventory-impl
  apps/app-modulith
  apps/app-macro
  apps/app-orders-service        (orders-impl + inventory-api)
  apps/app-inventory-service     (inventory-impl)
  docs/design/macro-service.md   (already present)

Requirements:
- Dependency rules enforced by Maven: -api artifacts depend only on rpc-api and the JDK; -impl artifacts depend on their own -api, other modules' -api, module-support and Spring; apps depend on impls/apis and the starter. Use the maven-enforcer-plugin (banned dependencies / dependency convergence) so that an orders-impl -> inventory-impl dependency fails the build. Add a short check that proves it fails.
- Each app has a @SpringBootApplication class in the single root package com.acme and an application.yml. The apps must start (even if they do nothing yet).
- Add package-info.java files with @NamedInterface("api") on com.acme.orders.api and com.acme.inventory.api, and @ApplicationModule(allowedDependencies = "inventory::api") on the orders module where appropriate.
- Add .gitignore, a root README with build instructions, and a CODEOWNERS file with one line per module directory.

Definition of done:
- `mvn -q verify` passes from the root.
- Temporarily adding an orders-impl -> inventory-impl dependency makes the build fail (show me, then revert).
- Each app jar starts and exits cleanly when run with --spring.main.web-application-type=none.
- Versions chosen are listed in your report with where you got them.
```

---

## Stage 2: RPC API, exceptions and module stereotypes

```text
Stage 2: rpc-api and module-support. Still no network code.

In platform/rpc-api (no Spring dependency):
- @RpcApi("name") annotation (type-level, runtime retention) naming the module.
- RpcException hierarchy: a base unchecked exception with an error code, plus RpcUnavailableException (remote unreachable or timed out), RpcProtocolException (malformed request/response), and RpcRemoteException (unexpected server error).
- An @RpcError(code = ...) annotation for business exceptions defined in -api artifacts, so they can be rehydrated on the client.

In platform/module-support:
- @ModuleService, @ModuleComponent and @ModuleConfiguration stereotypes, each with a module() attribute, each meta-annotated with the matching Spring stereotype and @Conditional(ModuleEnabledCondition.class).
- ModuleEnabledCondition reading modules.<name>.enabled (default false).
- ModuleProperties (@ConfigurationProperties "modules") exposing enabled, url, binding and datasource settings per module.
- A startup validator (ApplicationRunner or SmartInitializingSingleton) that, for every @RpcApi interface found on the classpath, checks: if the module is enabled it has a datasource (own or shared spring.datasource); if it is disabled or has no local implementation bean it has a client URL. It fails startup with a clear message such as "Module 'inventory' is disabled but modules.inventory.url is not set".

In the sample modules:
- inventory-api: InventoryApi (@RpcApi("inventory")) with getStock(String sku) and reserve(ReserveRequest request); records StockLevel, ReserveRequest, Reservation; OutOfStockException annotated with @RpcError.
- orders-api: OrdersApi (@RpcApi("orders")) with placeOrder(PlaceOrderRequest request) and getOrder(String orderId); records and one business exception.
- Stub implementations in the -impl modules using @ModuleService, returning in-memory values for now.

Add ArchUnit tests in platform (shared test-jar) that every app inherits:
- Classes in module packages may not use plain @Service, @Component, @Configuration (must use the @Module* stereotypes).
- Event listener methods (@ApplicationModuleListener, @EventListener, @TransactionalEventListener) may not take parameter types from another module.
- Cross-module access may only reference types in a module's api package.

Definition of done:
- `mvn -q verify` passes.
- Unit tests prove: a disabled module's beans are absent; the validator fails with the expected message for each broken configuration; each ArchUnit rule fails when a violating class is added in a test fixture (show the negative tests).
- app-modulith starts with all modules enabled and the stub implementations wired.
```

---

## Stage 3: JSON-RPC server, client proxy, registrar and bindings

```text
Stage 3: the JSON-RPC layer in platform/rpc-spring-boot-starter. Implement it ourselves with Jackson 3.

Server:
- POST /rpc/{module} accepting a JSON-RPC 2.0 envelope. Support single requests, batches and notifications (no id). Return correct JSON-RPC errors for parse error, invalid request, method not found, invalid params and internal error.
- Method name is "<module>.<method>". Build the method index at startup from all local beans implementing an @RpcApi interface, excluding client proxies (use a marker interface on proxies). Reject overloaded method names at startup with a clear message.
- Parameters by name (an object keyed by parameter name; requires -parameters). Results are the returned record.
- Exceptions: a business exception annotated @RpcError becomes a JSON-RPC error with its code and a data object carrying the exception class name and message; anything else becomes an internal error without leaking stack traces.
- Also serve POST /rpc/{module}/{method} as a convenience alias: bare params object in, bare result out, no envelope.
- Switchable with rpc.server.enabled. Bind address configurable with rpc.server.address.

Client:
- A java.lang.reflect.Proxy over RestClient (JDK HttpClient) that builds the envelope, sends it to modules.<name>.url, and maps errors back: @RpcError codes rehydrate the exact exception type from the -api artifact; transport failures become RpcUnavailableException. Per-module connect/read timeouts and an optional retry policy (off by default, safe only for idempotent methods).
- Propagate trace headers and the caller's principal as headers; leave security wiring as an extension point.

Registrar and bindings:
- A BeanDefinitionRegistryPostProcessor that scans the classpath for @RpcApi interfaces and, per module, applies the binding from modules.<name>.binding: local (default when an impl bean exists), loopback-codec (proxy that routes through the JSON codec and the dispatcher in-process, no HTTP), loopback-http (proxy over HTTP to 127.0.0.1 even though the impl is present), remote (proxy over HTTP to modules.<name>.url; the default when no impl bean exists).
- Use beanFactory.getBeanNamesForType(type, true, false) so beans are not instantiated during the check.
- Startup validation rejects loopback-* bindings when a profile named "prod" or "production" is active, and fails fast on missing URLs.

Tests:
- Dispatcher tests for every JSON-RPC error case, batches and notifications.
- A contract test base class parameterised by binding that exercises InventoryApi.getStock and reserve (including OutOfStockException) against local, loopback-codec, loopback-http and remote (two Spring contexts in one test, one serving and one calling over a random port). Results and exceptions must be identical across bindings.

Definition of done:
- `mvn -q verify` passes, including the contract tests for all four bindings.
- I can start app-inventory-service and app-orders-service locally and see orders call inventory over JSON-RPC (give me the curl for one raw JSON-RPC request and the alias path).
```

---

## Stage 4: Per-module persistence, PostgreSQL and Flyway

```text
Stage 4: per-module persistence with PostgreSQL, using Testcontainers.

In platform/module-support:
- A helper to build, from modules.<name>.datasource.* (url, username, password, schema, pool size), a HikariCP DataSource, and fall back to the shared spring.datasource when a module sets no URL (same instance, different schema).
- A helper to build the module's Flyway bean: locations classpath:db/migration/<module>, defaultSchema = the module's schema, history table inside that schema, creating the schema if needed. Support a separate migration credential (modules.<name>.datasource.migration-username/password) falling back to the runtime credential.

In orders-impl and inventory-impl:
- A @ModuleConfiguration that defines that module's DataSource, Flyway, JPA EntityManagerFactory (or JdbcClient if simpler; state which you chose and why) and transaction manager, with explicit bean names (ordersDataSource, ordersTm, ...) and none marked @Primary. Repositories are scoped to the module's own package with explicit EMF and transaction manager refs; services use @Transactional("ordersTm").
- Exclude Boot's single-datasource autoconfigurations (DataSource, HibernateJpa, Flyway, JpaRepositories) in the apps.
- Replace the in-memory stubs with real persistence: inventory has stock and reservations tables; orders has an orders table. A migration per module under its own folder.

Connection budget (no external pooler; see section 9.1 of the design):
- Pool settings per module under modules.<name>.datasource: max-pool-size, minimum-idle (default 0), idle-timeout, max-lifetime, connection-timeout. Defaults must be small.
- Pools are created only for enabled modules, and must not fail application startup when the module's database is temporarily unreachable (configure Hikari accordingly and make readiness reflect it, not liveness).
- A budget check at startup: read platform.db.connection-budget (connections this deployment may use per instance) and log a warning if the sum of enabled modules' max-pool-size exceeds it; fail startup instead when platform.db.enforce-budget=true.
- A per-module concurrency bulkhead (semaphore) in the RPC server dispatcher sized from the module's max-pool-size, with a bounded wait and a clear JSON-RPC error when exhausted (stage 3's dispatcher gets a hook for this; add it here).
- Micrometer metrics for each pool (active, idle, pending, timeouts) tagged by module.

Rules:
- No module reads another module's tables. orders gets stock via InventoryApi only.
- Provide an SQL bootstrap script (docs/db/bootstrap.sql) creating, per module, a schema, a migration role and a runtime role with grants only on that schema and search_path set on the role.

Tests:
- Testcontainers PostgreSQL: one instance with two schemas, and one test with two separate instances, proving both layouts work with only configuration changes.
- A test that the runtime role of orders cannot select from the inventory schema.
- Budget tests: a disabled module opens no connections (check pg_stat_activity); with minimum-idle 0 an idle module holds none; the budget check warns, and fails with enforce-budget=true, when pool sizes exceed the budget; the bulkhead returns the documented error under load while the pool is exhausted, without deadlocking.

Definition of done:
- `mvn -q verify` passes (Docker available).
- app-modulith runs against a single PostgreSQL with both schemas; app-orders-service and app-inventory-service run against different instances; the same placeOrder flow works in both shapes (give me the commands, including docker compose for the databases).
```

---

## Stage 5: Demo domain with interdependent modules

```text
Stage 5: grow the two pilot modules into the eight-module order-fulfilment domain in section 16.1 of the design. The platform (stages 1 to 4) must not change except where a gap is found; if it must change, tell me why first.

Modules (each with <m>-api and <m>-impl, own schema, own Flyway location, own persistence configuration from stage 4):
  catalog, customers, pricing, inventory, payments, shipping, notifications, orders
Dependencies (and nothing else) are exactly the "Calls" column of the table in section 16.1. Declare them in each module's package-info (@ApplicationModule(allowedDependencies = ...)) and in each -impl pom. The graph must be acyclic; add a test that fails if a cycle or an undeclared dependency appears.

APIs (records for all DTOs; unchecked exceptions annotated @RpcError in the -api artifact; every mutating method takes a requestId and is idempotent):
- catalog: getProduct(sku), productExists(sku).
- customers: getCustomer(customerId) (tier, shipping address, email), getPaymentMethod(customerId).
- pricing: quote(customerId, lines): base price from catalog, tier discount from customers.
- inventory: getStock(sku) (validates the SKU via catalog), reserve(requestId, orderId, lines), release(requestId, reservationId), getReservation(reservationId).
- payments: authorize(requestId, customerId, amount), capture(requestId, authId), void(requestId, authId); looks up the payment method through customers.
- shipping: quote(customerId, lines) (address from customers, warehouse from inventory), createShipment(requestId, orderId, customerId, reservationId).
- notifications: send(requestId, customerId, template, params): resolves the email address through customers and records the message in its own schema; no real email.
- orders: placeOrder(requestId, customerId, lines), getOrder(orderId).

orders.placeOrder is a saga implemented in orders only, following the 8 steps in section 16.1 exactly:
- Order state machine persisted in the orders schema: PENDING, RESERVED, AUTHORIZED, CONFIRMED, REJECTED, FAILED.
- A module-local outbox table in the orders schema, written in the same transaction as each state change, and a scheduled relay that executes outbox entries by calling the target @RpcApi method (idempotent by requestId): the compensations (inventory.release, payments.void) and notifications.send. The relay retries with backoff, and a notifications outage never fails or delays the order.
- Same requestId sent twice returns the same order and causes no duplicate reservation, authorization or shipment.
- No cross-module transaction, no event, no direct database access across modules.

Seed data (loaded only in the "demo" profile from demo-only Flyway locations inside each module): a handful of products with base prices; customers cust-standard, cust-gold (tier discount) and cust-declined (payment method that is declined); stock levels; plus special SKUs sku-oos (no stock) and sku-undeliverable (shipping always fails).

Demo fault hooks (demo profile only, in each module that needs one): notifications can be toggled down, payments can be told to decline, inventory can be given extra latency; controlled in-process through a small FaultControl bean that stage 6 exposes over HTTP.

Tests:
- Module slice tests per module (@ApplicationModuleTest) mocking collaborators' -api.
- Saga tests in orders with mocked APIs for every scenario in section 16.5: happy path, gold discount, out of stock, payment declined (release called), shipping failure (void and release called), notifications down (order still CONFIRMED, notification delivered after recovery), idempotent retry.
- Contract tests per -api, run against every binding from stage 3 for the interdependent chain orders -> shipping -> inventory -> catalog, proving identical results and exception types.
- app-modulith integration test: real PostgreSQL (Testcontainers), all eight modules, the full scenario table from section 16.5.

Definition of done:
- `mvn -q verify` passes.
- With app-modulith running locally, a raw JSON-RPC call to orders.placeOrder for each scenario in section 16.5 gives the expected outcome (give me the curl commands).
- The dependency graph in your report matches the mermaid graph in section 16.1 exactly.
```

---

## Stage 6: Assembly apps and the local demonstration harness

```text
Stage 6: finalise the app artifacts and build the local demo harness for the eight-module domain (section 16 of the design). Everything runs on localhost; Docker is used only for PostgreSQL.

Apps and baked configuration:
- app-modulith: all eight impls; application.yml sets every module enabled with binding local and rpc.server.enabled=false. Startup validation rejects any remote or loopback-* binding.
- app-macro: all eight impls; no module wiring baked in; everything comes from profiles and environment variables. Profiles: monolith, front, fulfilment, accounts (the groupings in section 16.2), one profile per single module (orders, catalog, pricing, inventory, customers, payments, shipping, notifications), loopback-codec, loopback-http, and the database layouts needed by split-dbs.
- Per-service apps app-<module>-service for all eight modules, generated by scripts/new-service-app.sh from one template so they stay uniform. Each contains its own -impl plus exactly the -api artifacts of the modules it calls (its allowedDependencies), module enabled, rpc.server.enabled=true, every other module remote with a required URL. Add a Maven enforcer rule that fails the build if a service app's -api dependencies differ from its module's allowedDependencies.
- Ports and databases as in section 16.3 of the design (8080 all-in-one; 8081-8083 groups; 8081-8088 one per module; PostgreSQL on 5432, 5433, 5434).

Demo aids (enabled only in the "demo" profile, which implies "dev"):
- GET /demo/topology: for each module in the process, enabled or not, binding (local, loopback-codec, loopback-http, remote), target URL, datasource host/port and schema (never the password).
- Call trace with correlation: a boundary interceptor on every @RpcApi call from one module to another records correlation id, caller, callee, method, binding actually used, depth, duration and outcome in a bounded in-memory buffer. The correlation id is generated at orders.placeOrder (or taken from a request header), propagated in-process and in an HTTP header on remote calls. GET /demo/trace?correlationId=... returns the records of that process. Log one line per call, such as "pricing -> catalog.getProduct [local] 1 ms ok". Keep it minimal; stage 8 extends the interceptor.
- POST /demo/faults to toggle the fault hooks from stage 5 (notifications down, payments decline, inventory latency); it affects only modules enabled in that process.
- Startup banner listing the topology.

Scripts (bash, in scripts/):
- scripts/db.sh up|down: starts/stops the three Postgres containers with docker-compose.demo.yml and runs docs/db/bootstrap.sql for all eight modules (schemas, migration role, runtime role).
- scripts/demo.sh <mode> up|down|status, where mode is one of: modulith, macro-modulith, groups, microservices, microservices-macro, loopback-codec, loopback-http, split-dbs. It starts the right JVMs with the right profiles and ports, writes PID files and logs under .demo/, waits for readiness, and prints the topology URLs. "down" stops everything it started.
- scripts/smoke.sh: runs every scenario in section 16.5 against whichever process hosts orders, checks outcomes and exception types, and prints the merged call tree for one order. Exits non-zero on any difference from the expected results.
- scripts/trace.sh <correlationId>: queries /demo/trace on every running demo process and prints one merged call tree, with the binding shown on each edge.
- scripts/demo-all.sh: runs every mode in turn (up, smoke, down) and prints a summary table of mode, processes, bindings seen (from the traces), and pass/fail.

Out of scope for this stage: image publishing, CI pipelines, service-to-service security, cloud configuration. A Dockerfile per app is optional; do not spend time on it unless asked.

Definition of done:
- `mvn -q verify` passes.
- `scripts/demo-all.sh` passes for all eight modes from a clean checkout with only Docker and a JDK installed. The summary table shows the expected bindings: local for modulith and macro-modulith; mixed local and remote exactly as the edge table in section 16.2 for groups and split-dbs; all remote for microservices and microservices-macro; loopback-* for the loopback modes.
- scripts/trace.sh prints the same call tree shape for one order in every mode, differing only in the binding on each edge.
```

---

## Stage 7: API documentation (OpenAPI and OpenRPC)

```text
Stage 7: generated API documentation from the @RpcApi interfaces.

- Generate an OpenRPC document and an OpenAPI 3.1 document at runtime from the @RpcApi interfaces using reflection, with record schemas produced by swagger-core's model converters. First verify which swagger-core/springdoc versions support Jackson 3 and Boot 4; if none do, generate the schemas ourselves from the records and tell me.
- OpenAPI: one operation per method at POST /rpc/{module}/{method} (the alias path from stage 3), operationId = "<module>.<method>", descriptions from optional @Schema/@Operation annotations on the API interfaces.
- Endpoints: /rpc/{module}/openapi.json and /rpc/{module}/openrpc.json per module, plus a merged document at /rpc/openapi.json for deployments that run several modules. Served only when rpc.server.enabled=true.
- A Maven goal or test that writes the generated documents from the compiled -api artifacts to target/ so CI can publish them; nothing generated is committed.
- Serve Swagger UI only in the dev profile.

Definition of done:
- `mvn -q verify` passes; tests assert that adding a method or field to an @RpcApi interface changes the generated documents, and that a document is valid against the OpenAPI 3.1 schema.
- I can open Swagger UI in dev and call inventory.getStock through the alias path.
```

---

## Stage 8: Hardening and compatibility checks

```text
Stage 8: parity, compatibility and failure behaviour.

- Boundary interceptor: an AOP interceptor around local cross-module calls (the @RpcApi beans injected into other modules) that adds Micrometer metrics and tracing identical to remote calls, and, behind a property (modules.boundary.suspend-transactions, default true), suspends any active transaction for the call (PROPAGATION_NOT_SUPPORTED semantics) so local behaviour matches remote behaviour. Tests prove an outer transaction is not joined.
- API compatibility check: a build step (japicmp or equivalent) comparing each -api artifact with its last released version and failing on removed or changed methods, renamed fields, or removed exception types; additive changes pass. Document how to set the baseline version.
- Failure behaviour tests for the client proxy: timeout, connection refused, server returns a JSON-RPC error, malformed response, and a retry on an idempotent method. Each maps to the documented exception type.
- Extend the stage 6 call trace interceptor with the metrics and tracing described above, and add failure tests for the stage 5 saga and outbox: kill the callee mid-call, return a malformed response, restart the relay, and confirm no duplicate side effects (idempotent requestId) across the whole domain.
- Update docs/design/macro-service.md where the implementation differs from it, and list every deviation in your report.

Definition of done:
- `mvn -q verify` passes.
- Every deviation from the design document is either removed or recorded in the document, and I have approved it.
```

---

## Stage 9: Final verification and handover

```text
Stage 9: final verification.

- Run the full reactor build, all contract tests, all four bindings, and the `scripts/demo-all.sh` covering every demo mode (modulith, macro-modulith, groups, microservices, microservices-macro, loopback-codec, loopback-http, split-dbs). Report the results in a table.
- Re-run ApplicationModules.of(Application.class).verify() in app-modulith and attach the module canvas/component diagrams generated by Documenter to docs/generated (CI-generated, not committed).
- Write docs/README-dev.md: how to run each shape locally, how to add a new module (api/impl layout, stereotypes, migrations, datasource config, startup validation, adding it to a grouping, generating its per-service app), how to change an @RpcApi interface safely, and how to add a new deployable service app.
- List any open risks or follow-ups, including anything that depends on library versions that may change.

Definition of done:
- Everything above is committed with the message "stage 9: ..." and the final report lists exact commands for each shape.
```
