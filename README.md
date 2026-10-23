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

## Run (stage 1: the apps start and do nothing yet)

```bash
java -jar apps/app-modulith/target/app-modulith-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none
```

Likewise `app-macro`, `app-orders-service` and `app-inventory-service`.

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
