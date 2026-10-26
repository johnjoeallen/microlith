# Glossary

Short definitions of the terms used across this site. Several of them sound alike, so the modes come first.

## Modes that are easy to confuse

| Mode | Artifact | Processes | Calls between modules | PostgreSQL | One-line meaning |
| --- | --- | --- | --- | --- | --- |
| `modulith` | `app-modulith`, a fixed all-in-one assembly | 1 | direct, in-process | **one instance**, a schema per module | every module in one process; the shape cannot be changed by configuration |
| `macro-modulith` | `app-macro` (the configurable jar), profile `monolith` | 1 | direct, in-process | one instance | the same all-local shape, built from the configurable jar; a modulith like `modulith`, but built from the configurable jar: every mode is built from modules, so there is no genuine monolith, and here each deployment has all modules enabled, in one process |
| `loopback-codec` | `app-macro`, profile `loopback-codec` | 1 | every call encoded to JSON-RPC and decoded, in memory | one instance | one process, but every call crosses a serialisation boundary |
| `loopback-http` | `app-macro`, profile `loopback-http` | 1 | every call a real HTTP request to the same process | one instance | one process, but every call crosses an HTTP boundary |
| `groups` | `app-macro`, profiles `front`, `fulfilment`, `accounts` | 3 | direct inside a group, JSON-RPC over HTTP between groups | one instance | a few processes, each hosting several modules |
| `split-dbs` | as `groups` | 3 | as `groups` | **three instances** (A, B, C), one per group | `groups`, each group on its own PostgreSQL instance |
| `microservices` | eight `app-<module>-service` jars | 8 | all JSON-RPC over HTTP | **one instance**, a schema per module; each service has its own schema, roles and pool | one dedicated, generated jar per module |
| `microservices-macro` | `app-macro` run eight times, profile = module | 8 | all JSON-RPC over HTTP | **one instance**, a schema per module; each process has its own schema, roles and pool | the same eight processes, built from the single configurable jar |

Every mode except `split-dbs` uses **one PostgreSQL instance with one schema per module**, even the eight-process modes: separate processes do not mean separate database servers. Where a module's data lives is a separate choice (its datasource URL), and any module or group can be pointed at its own instance without a code change; `split-dbs` is the mode that demonstrates it.

Only the two loopback modes put a serialisation or network boundary between modules that share a process. `macro-modulith` does not. Every mode serves the orders entry point and the shop on port 9181.

## Terms

`@Cached`
:   An annotation on an `@RpcApi` method that lets callers reuse its result for a short time. Applied to four read methods; switched off with `rpc.cache.enabled=false` or `demo.sh --cache off`. See [the cache](features/cache.md).

`@RpcApi`
:   The Java interface through which one module is called by another. A direct method call in the same process, a JSON-RPC 2.0 request when the module is elsewhere. It is the source of the server dispatch, the client proxies and the generated OpenAPI and OpenRPC documents.

`-api` and `-impl`
:   The two artifacts of a module: the `@RpcApi` interface with its data types and exceptions, and the implementation. A module may depend only on the `-api` artifacts of the modules it calls.

Binding
:   How a caller reaches a module: `local` (the implementation itself), `loopback-codec`, `loopback-http` or `remote`. It follows from what is enabled in the process. In call traces a call answered by the result cache is shown as binding `cache`, although that is not a configured binding.

Boundary (logical and physical)
:   A *logical* boundary is a module boundary the build enforces (contracts, schema, one-way dependencies). A *physical* boundary is a process, database instance or pipeline. A macro service has logical boundaries everywhere and physical ones only where they are needed. See [principles](principles.md#3-separate-logically-first-physically-only-when-forced).

Boundary cost
:   The calls per order that would cross a proposed boundary, multiplied by the extra cost of one remote call (about 1.1 to 1.3 ms on loopback here). A floor for the network part. See [place modules by call weight](principles.md#6-place-modules-by-call-weight).

Call (module-to-module)
:   One invocation of an `@RpcApi` method by another module. An order makes about 20.

Closed loop
:   The load pattern of the performance driver: each of *N* clients sends its next order as soon as the previous one returns.

Crossing call
:   A call between two modules that would be remote if a boundary were drawn between them.

Declined order
:   An order for a customer whose payment is always declined (2% of the seeded customers). An expected business outcome, reported separately, not a failure.

Deployable
:   Something built, started and released as a unit: a process type. A macro service is *one image* that can be deployed as one deployable or as several by configuration.

Distributed monolith
:   Services that must change and deploy together, so they have the costs of both models. What an early, wrong split tends to produce.

Event publication registry
:   Spring Modulith's store of events in the publisher's transaction (the `orders.event_publication` table), redelivered until their listener completes. Used when `modules.orders.async=events`.

Follow-up
:   A call to another module that must happen after an order changes state (release a reservation, void a payment, confirm to the customer). Made durable in the order's transaction and delivered after the commit, by the outbox or by events. See [follow-up delivery](features/follow-ups.md).

Group
:   A set of modules hosted together in one process. The demo has `front` (orders, notifications), `fulfilment` (catalog, pricing, inventory, shipping) and `accounts` (customers, payments).

Idempotent
:   Safe to repeat. Calls that change state carry a request id derived from the order's, so a repeated or retried call causes one effect.

Macro jar (`app-macro`)
:   The one jar that contains every module and takes its shape from configuration: which modules a process enables and where the others are. Used by every mode except `modulith` and `microservices`.

Macro service
:   One team's bounded context, built from modules whose boundaries the build enforces, deployable as one unit or as several by configuration. It is not a [monster monolith](principles.md#1-what-a-macro-service-is-and-is-not).

Module
:   The unit of code ownership: an `-api`/`-impl` pair with its own database schema and roles, its own connection pool and its own migrations, calling other modules only through their `@RpcApi`.

Modulith (modular monolith)
:   A single deployable divided into modules with enforced boundaries. Spring Modulith is the library used to verify the module structure; the `modulith` mode is the fixed all-in-one assembly of it.

Monster monolith
:   A shared, unenforced codebase for many teams, with shared tables and one release train, that keeps growing. The thing this project is *not* advocating.

Outbox
:   A table in the orders schema holding follow-ups written in the order's transaction, and a relay that delivers them with retries. The default, `modules.orders.async=outbox`.

Parallel quotes
:   The customer check and the two quotes at the start of `placeOrder` run at the same time. On by default; `modules.orders.parallel-quotes=false` or `demo.sh --parallel off` restores the sequential code. See [parallel quotes](features/parallel.md).

Percentiles (p50, p95, p99)
:   The value below which 50%, 95% or 99% of the orders completed. p50 is the median order.

Placement
:   Which modules a process hosts, set by `modules.<m>.enabled` and the binding. The eight modes differ in placement.

Profile (Spring profile)
:   A named set of configuration. In `app-macro`, profiles such as `monolith`, `front` or `orders` choose the shape. Not the same as a workload profile.

Replication and routing
:   Replication is how many identical instances run; routing is which traffic reaches which instances. Both are independent of placement. See [the levers](principles.md#2-boundaries-placement-replication-routing-and-data-are-separate-choices).

Published and internal API
:   A planned distinction between an `@RpcApi` other teams may rely on and one only the owning team may use. **Not implemented yet**; see the [parked ideas](principles.md#parked-ideas).

Saga
:   The `orders.placeOrder` flow: reserve, authorize, ship, capture, with compensations (release, void) when a step fails. The ordering is explicit in the orders module; there is no cross-module transaction.

Schema and roles
:   Each module has a PostgreSQL schema, a migration role that owns it and a runtime role with DML only on it. A module cannot read another module's tables.

Slice
:   A package-by-feature area inside a module. A practice, not a feature: the rules that keep slices independent are in the [parked ideas](principles.md#parked-ideas).

Version-skew pair
:   A caller and callee that must stay compatible while they run different versions during a rollout. Zero inside one deployable, three between the demo's three groups, thirteen between eight services.

Workload profile
:   The customers, products and skew the load driver uses: `small`, `realistic` (default) or `large`. Not a Spring profile. See [method](performance/method.md).
