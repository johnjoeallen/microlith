# Developer guide

How to run each deployment shape, add a module, change an API and add a service app. Design background:
[`design/macro-service.md`](design/macro-service.md) (section 17 lists where the code differs from it). Short overview: the root [`README.md`](https://github.com/johnjoeallen/microlith/blob/main/README.md).

You need a JDK 25, Docker, `curl` and `python3`. Maven comes from `./mvnw`.

## 1. Run each shape locally

Every shape is the same code; only configuration differs. `scripts/demo.sh` wraps the commands below (it writes PID files and logs under `.demo/`).

```bash
scripts/db.sh up                        # PostgreSQL A 5432, B 5433, C 5434, schemas and roles from docs/db/bootstrap.sql
scripts/demo.sh <mode> start              # start the processes of a mode
scripts/smoke.sh                        # scenarios + expected binding on every edge + merged call tree
scripts/trace.sh <correlationId>        # the call tree of one order across all processes
scripts/demo.sh <mode> stop
scripts/db.sh down                      # stops and wipes the databases
scripts/demo-all.sh                     # every mode on a clean database, summary table
```

| Shape | `<mode>` | Processes | Cross-module calls |
| --- | --- | --- | --- |
| Modulith (one jar, only the modules' `-impl`) | `modulith` | `app-modulith` :9181 | all local, direct |
| Macro service, all modules local | `macro-modulith` | `app-macro` profile `monolith` :9181 | all local |
| Macro service in groups | `groups` | `app-macro` profiles `front` :9181, `fulfilment` :9182, `accounts` :9183 | local inside a group, JSON-RPC across |
| Microservices (eight jars) | `microservices` | the eight `app-<module>-service` :9181-9188 | all JSON-RPC |
| Microservices from the macro jar | `microservices-macro` | `app-macro` eight times, profile = module name | all JSON-RPC |
| Everything through the JSON codec | `loopback-codec` | `app-macro` :9181 | in-process, serialized |
| Everything over HTTP to itself | `loopback-http` | `app-macro` :9181 | HTTP to 127.0.0.1 |
| Groups on three databases | `split-dbs` | as `groups`; front on A, fulfilment on B, accounts on C | as `groups` |

Without the scripts, for example the groups shape (databases up first):

```bash
./mvnw -q -DskipTests package
for g in front fulfilment accounts; do
  java -jar apps/app-macro/target/app-macro-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo,$g &
done
```

Changing a module's place is configuration only: `modules.<m>.enabled=true|false`, `modules.<m>.url=http://host:port/rpc/<m>` for a module that runs
elsewhere, `modules.<m>.binding=local|loopback-codec|loopback-http|remote` to force one, `modules.<m>.datasource.*` for its database.
Environment variables work too, for example `MODULES_INVENTORY_ENABLED=false MODULES_INVENTORY_URL=http://host:9184/rpc/inventory`.

Useful while running (demo profile, which implies dev): `GET /demo/topology`, `GET /demo/trace?correlationId=`, `POST /demo/faults`,
`/swagger-ui`, `/rpc/openapi.json`, `/actuator/health`.

## 2. Build and test

```bash
./mvnw -q verify                                 # everything: boundary rules, ArchUnit, Modulith verify, contract tests, failure tests
./mvnw -q verify -pl modules/orders/orders-impl -am
scripts/check-boundaries.sh                      # forbidden dependencies must break the build
scripts/check-api-compat.sh                      # the API compatibility check must bite
scripts/new-service-app.sh --check               # generated service apps are up to date
```

`verify` writes the generated documents to `apps/app-macro/target/api-docs/` and the Modulith canvases and PlantUML component diagrams to
`docs/generated/` (git-ignored; publish them from CI).

## 3. Add a module

Example: a module `loyalty`. Use `inventory` as the template.

1. **Layout.** `modules/loyalty/loyalty-api` and `modules/loyalty/loyalty-impl`; list both in the root `pom.xml` under `<modules>` and `<dependencyManagement>`.
2. **`-api`** (package `com.acme.loyalty.api`): depends only on `rpc-api` and other `-api` jars (`spring-modulith-api` and `swagger-annotations-jakarta` at `provided`). Contents:
   - an interface annotated `@RpcApi("loyalty")`; no overloaded names; mark reads and request-id-keyed mutations `@Idempotent`;
   - immutable `record`s for parameters and results; enums, `List`, `Map<String,…>` and scalars are fine;
   - business exceptions as unchecked classes annotated `@RpcError(code = …)` with a `(String message)` constructor and a code unique across modules;
   - a `package-info.java` with `@NamedInterface("api")`;
   - optional `@Operation(summary = …)`, `@Parameter(description = …)` and `@Schema(description = …)` for the generated documents.
3. **`-impl`** (package `com.acme.loyalty`): depends on `-api`, `module-support`, and only the `-api` jars of modules it calls. Never another `-impl` (enforced).
   - `package-info.java`: `@ApplicationModule(allowedDependencies = {"customers::api"})` lists what it may call; this is the single source of truth, also read by the service-app generator.
   - Stereotypes from `module-support`: `@ModuleService(module = "loyalty")` on the class implementing the API, `@ModuleComponent` and `@ModuleConfiguration` for the rest. They make the bean conditional on `modules.loyalty.enabled`. Keep everything else in `internal`.
   - Persistence: copy `InventoryPersistenceConfiguration` (own `DataSource`, Flyway, `EntityManagerFactory`, `loyaltyTm`; no `@Primary`) and annotate service methods `@Transactional("loyaltyTm")`.
   - Migrations: `src/main/resources/db/migration/loyalty/V1__init.sql`. Demo seed data, if any, goes in `db/demo/loyalty/`.
   - Calling another module: inject its `-api` interface through the constructor.
4. **Database.** Add the schema and the two roles (`loyalty_migrator`, DDL; `loyalty_app`, DML only) to `docs/db/bootstrap.sql`, following the existing blocks. The connection budget is per module, `modules.loyalty.datasource.max-pool-size` (default 3); keep the sum within the database's `max_connections` (design 9.1). There is no pooler.
5. **Configuration.** In `apps/app-macro/src/main/resources/application.yml` add `modules.loyalty.datasource.*` (url, username, password, migration-username, migration-password); add `application-loyalty.yml` (the one-module profile with all other modules disabled and their URLs, copy `application-inventory.yml`); add the module to its group profiles (`front`, `fulfilment`, `accounts`, `split-dbs`) either enabled or with a URL; every disabled module needs a URL in every profile. Add `loyalty-impl` to `app-modulith` and `app-macro`.
6. **Startup validation** is automatic: an enabled module without a datasource, a disabled one without a URL, a `local` binding without an implementation, or a loopback binding in a `prod` profile stops the application with a message naming the module and the property. Add a test in the style of `ConnectionBehaviourTests` if you add a new rule.
7. **Grouping.** Decide which group (`front`, `fulfilment`, `accounts`) it joins in `scripts/demo_tools.py` (`GROUPS`) and in the group profiles; put its port in `scripts/demo.sh` and `scripts/new-service-app.sh`.
8. **Service app.** `scripts/new-service-app.sh loyalty` (see section 5).
9. **Ownership and checks.** A line in `.github/CODEOWNERS`; `./mvnw -q verify`; `scripts/check-boundaries.sh`.

The shared ArchUnit rules (`AbstractModuleArchitectureTests`, from the `module-support` test-jar) already apply to every app and fail on a stereotype,
a dependency or a cross-module use of anything but an `@RpcApi` type that breaks the rules.

## 4. Change an `@RpcApi` interface safely

During a rolling deployment the new version calls the old one, and the reverse, so every `-api` change must be additive until all deployed
versions have moved on.

| Change | Safe? | How |
| --- | --- | --- |
| New method | yes | Add it; callers of old versions never use it. A caller that already uses it fails with "method not found" against an old callee, so deploy the callee first. |
| New field in a result record | yes for JSON | Keep the old constructor and delegate to the new one: `public Order(a, b) { this(a, b, null); }`. Old clients ignore unknown fields. |
| New field in a parameter record | careful | An old caller omits it, so the callee must accept `null`. |
| Remove or rename a method, parameter type, record component, exception | **no** | Deprecate first, remove a release later. japicmp fails the build. |
| Rename a method **parameter** | **no, not detected** | It renames the JSON field. japicmp cannot see it; review the diff of the generated OpenRPC document. |
| New exception | yes | `@RpcError` with a new unused code. |
| Change an exception's code | **no** | Clients match type and code. |

Procedure: change the interface, run `./mvnw -q verify` (contract tests run against all four bindings), then
`./mvnw verify -Dapi.compat.skip=false -Dapi.baseline.version=<last release>` before merging. Keep new mutating methods idempotent through a
caller-supplied request id, and annotate them `@Idempotent` only if repeating is safe. Mind that a local call and a remote call differ in two ways the
platform already equalizes: parameters are copied (use records) and the caller's transaction is suspended (`modules.boundary.suspend-transactions`).

## 5. Add a deployable service app

A service app is a thin Spring Boot jar for one module: its `-impl`, exactly the `-api` jars it may call, and the platform.

```bash
scripts/new-service-app.sh loyalty      # one module; --all for all; --check fails if a generated app is stale
```

It generates `apps/app-loyalty-service` (pom, `Application`, `application.yml`, `ApplicationTests`, `ArchitectureTests`) from one template. The `-api` set
comes from the module's `package-info.java`, and the generated pom carries a maven-enforcer rule that rejects anything else. Then add the new app to the root
`<modules>` and `<dependencyManagement>` and to `ensure_built` in `scripts/_common.sh`. Do not edit a generated app by hand; change the template in
`scripts/new-service-app.sh` and regenerate, so that all apps stay uniform.

Running a service app needs the URLs of the modules it calls (`modules.<callee>.url`), which the generated `application.yml` sets for the local demo ports.

## 6. Troubleshooting

| Symptom | Cause |
| --- | --- |
| Startup fails with "Module 'x' is disabled but modules.x.url is not set" | Every `@RpcApi` on the classpath needs either a local implementation or a URL. |
| `RpcUnavailableException` | The callee is down, timed out (`modules.<m>.client.read-timeout`, default 10 s; `connect-timeout` 2 s) or its bulkhead rejected the call. |
| Warning about the connection budget at startup | The pool sizes of the enabled modules exceed `platform.db.connection-budget`; lower `max-pool-size` or raise the budget. With `platform.db.enforce-budget=true` startup fails instead. |
| A bean of an `@RpcApi` cannot be injected by its class | Local implementations are wrapped in a proxy; inject the interface. |
| Port already in use after a crash | `scripts/demo.sh <mode> stop`; logs are in `.demo/logs/`. |
