# Principles: using a macro service well

How to decide what to build, where to draw boundaries, and what to deploy, for a team of 5 to 10 building a product. These are the working principles behind the
design; the numbers come from [the measurements](performance/index.md) and the module graph of the demo domain, and where something is a practice that the code does
not yet support, it says so ([parked ideas](#parked-ideas) lists them).

The argument that follows from these principles is on [the case page](case.md).

## 1. What a macro service is, and is not

A **macro service** is one team's bounded context, built from modules whose boundaries the build enforces, and deployable as one unit or as several by configuration.
It is not a big monolith, and it does not mean "put everything in one deployable".

| | Macro service | Monster monolith | Microservices |
| --- | --- | --- | --- |
| Scope | one team's bounded context | many teams, a whole company | one narrow capability each |
| Ownership | one team of 5 to 10 | nobody, or everybody | one team per service |
| Inside | modules, **build-enforced**, no cycles | packages, by convention | process boundaries |
| Data | schema and roles per module, no shared tables | shared tables and joins | a database per service |
| Contracts | `@RpcApi` per module, compatibility-checked | method calls and shared entities | network APIs |
| Release | one unit per team | one train for the company | per service |
| Growth | bounded by guardrails, then **another macro service** | unbounded | sprawl inside a team |
| Split-readiness | proven continuously (loopback modes) | unknown | already split |

**What keeps it a macro service**

* A written **scope statement**: one bounded context, one owning team. A different bounded context is a different macro service, not one more module.
* Boundaries that the build enforces: no cross-module table access, no cross-module transactions, an acyclic module graph, compatible APIs.
* **Budgets** that trigger a review when crossed: team size, number of modules, build time, start-up time. These are guardrails to tune, not measured thresholds. For scale, the demo has 8 modules and
  its full build and tests take roughly 8 minutes.
* **Exit ramps** agreed in advance: a second team, a diverging vocabulary, or a persistent release conflict means a second macro service, and the module graph shows where to cut. A company is then many macro
  services connected by published contracts.

## 2. Boundaries, placement, replication, routing and data are separate choices

| Lever | Question | Set by |
| --- | --- | --- |
| Logical boundary | what are the modules and their contracts? | the code, enforced |
| Placement | which modules does a process host? | `modules.<m>.enabled` and the binding |
| Replication | how many identical instances? | the deployer |
| Routing | which traffic reaches which instances? | an ingress, gateway or router |
| Data placement | which schema or database instance holds each module's data? | the datasource URLs |

The [eight modes](modes/index.md) vary **placement** (with one replica). They are points in a larger space. A cluster of identical pods, each hosting every module, behind a router that
sends `/rpc/orders` to one pool and `/rpc/pricing` to another, is also a deployment, and the database does not change.

| | Identical pods + routing | Separate processes per module |
| --- | --- | --- |
| Hop on a module-to-module call | none, it stays in-process | a network hop, about 1.2 ms here |
| Unit of version | the pod | the module |
| Per-API scaling and canary | yes, for externally entered calls | yes, for all calls |
| Version skew between modules | none | grows with the edges |
| Moving parts | one image, router rules | N deployables |

Things to know about routing:

* It partitions **entry** traffic only. If `orders` runs in a pod, its calls to `pricing` run in that same pod whatever the router says about `/rpc/pricing`.
* A canary is per pod, not per module, so a module-level canary seen by every caller needs that module as its own deployable.
* Isolation is partial: a leaky module called internally still affects the pods it runs in.
* With every module in every pod, every `/rpc/<module>` exists on every pod, so internal-only modules must be blocked at the ingress. The code has no published-versus-internal marker yet.
* Connection pools are lazy and per module, so a pod only holds connections for modules it uses; the budget is about pods times active modules times pool size. Caches are per pod.
* Running several replicas against one database has not been tested here. The outbox claims with a lease and the event registry resubmits with a compare-and-set, which looks designed for it.

## 3. Separate logically first, physically only when forced

Logical separation gives most of what a small team needs, at nearly no cost, and deployed as one unit it removes most of the operational burden.

| Satisfied by logical separation (one deployable) | Needs physical separation |
| --- | --- |
| ownership, contracts, data ownership, dependency direction, independent tests, change locality, partitioned cognitive load | independent release cadence, independent scaling, failure and resource isolation, a security or network zone, a different runtime, a vendor or other team owning the lifecycle |

What one deployable costs you, measured on this domain: version-skew pairs 0 (against 3 for three groups and 13 for eight services), one image and pipeline, one database instance, and
`macro-modulith` at the same throughput as `modulith` (462 against 457 orders/s).

What it does **not** give you: no CPU or memory isolation between modules (one leaky module affects all), one classpath, and one release unit, so a bad commit reaches everything. The controls are
per-module bulkheads and connection pools, timeouts, asynchronous follow-ups, the runtime switches, and canarying the whole unit. If a requirement needs real isolation, that is a physical
force, and you act on that module or group alone.

**Split-readiness is a test, not a claim.** `loopback-codec` in CI (97% of the throughput) passes only if no module relies on shared objects or a shared transaction, `loopback-http` adds a real HTTP hop, and
a periodic `microservices-macro` run in staging proves the full split from the same artifact and shows its cost.

## 4. Start small and let boundaries appear

A new team should not begin with "how many services do we need?". Separate on evidence, merge on doubt.

| Phase | Work | Output |
| --- | --- | --- |
| Understand the product | outcomes, capability map, quality targets **with numbers**, what is unknown or volatile | requirements with numbers, an uncertainty map |
| Find the seams | walk the top journeys; list the invariants (what must change atomically stays together), data ownership, integrations, what changes together | candidate modules and the invariants not to split |
| Score the forces | the table below, with evidence | "not yet" or "yes" for each candidate boundary |
| Choose the start and the triggers | pick the shape, write the extraction triggers, cost being wrong | one-page decision record |
| Instrument and revisit | boundary metrics, a review cadence | decisions driven by data |

| Force | Question | Default for one team of 5 to 10 |
| --- | --- | --- |
| Release cadence | must parts ship independently of each other? | no: one team, one train |
| Scale | does a part need about 10 times the capacity? | scale the whole thing; extract a measured hot spot |
| Failure isolation | must a failure in X not take down Y? | bulkheads, timeouts and async follow-ups in-process |
| Security or compliance | is there an audit or network-zone boundary? | separate schema and roles; extract only if the audit scope demands it |
| Consistency | which operations need atomic updates? | keep them in one module and one transaction |
| Ownership | how many teams in 12 months? | one team means one deployable |
| Technology | does a part need a different runtime? | no |
| Latency | what is the end-to-end budget and call-chain depth? | keep the hot path in-process |
| Reuse | does a second consumer exist? | not until it does |
| Operability | can the team run N deployables and be on call for them? | one |

**Cut by capability and by what must stay consistent, never by technical layer.** A layered split (an API service, a business service, a data service) makes every feature a coordinated release.
As an extrapolation from the measured per-hop cost (a lower bound on one machine), the demo's 20 calls per order each routed through two extra layer services would be about 60 hops, roughly +70 ms, against about +11 ms measured for the business-aligned split.

**Boundaries can appear organically.** Start with one module, or a few, and split as seams show up. For that to be cheap:

* Inside a module, keep package-by-feature **slices** that are cycle-free, talk to each other through a small interface, and share no tables, so promoting a slice to a module later is mechanical.
* Let the prototype show the seams: calls per journey across a candidate boundary, files that change together, and agreed triggers.
* Creating a module has to be cheap. Today it is not: the documented procedure is nine manual steps touching the root pom, `bootstrap.sql`, 15 profile files in `app-macro` (72 "URL of the disabled module" lines
  in total) and more. That friction works against organic splitting and is the first item in [parked ideas](#parked-ideas).

## 5. Changing a boundary later: splitting is easier than re-combining

The design work is the same in both directions and dominates the cost: decide who owns each table, remove joins, foreign keys and shared transactions across the line, and move or copy the data.
In this codebase that is done by construction from day one: there are no cross-schema foreign keys (the only foreign key in any migration is inside inventory), the roles stop one module reading another's tables, and
there are no cross-module transactions.

| Move | Mechanics | Cost |
| --- | --- | --- |
| A table to another schema, same instance | a metadata change (`ALTER TABLE … SET SCHEMA`), transactional, no copy | very low |
| A schema to another database instance | copy the data (dump and restore, or logical replication), coordinate the cutover, flip the module's datasource URL | moderate |

The code change is one datasource property either way. The `split-dbs` mode proves only the code side, because its databases start empty.

| | Splitting | Merging back |
| --- | --- | --- |
| Ownership | creates it | reconciles two owners |
| Contract | write an API at the cut | remove it and inline its consumers |
| Data | already isolated per module | collisions: every schema has its own `flyway_schema_history`, so merging schemas means merging migration histories and table names |
| Consumers | none or few yet | everyone who adopted the API changes |
| Organisation | one team becomes two | two teams must agree to become one |

Merging is also less necessary: you can merge two modules' **code** and keep the **schemas separate**. The deployment level is symmetric and cheap (enable a module locally, or point it at a URL). The asymmetry is at
the module level.

## 6. Place modules by call weight

Whether a call is a method call or a network hop depends only on whether the two modules share a process. In an `a → b → c` chain with one module per process, that is two sequential hops; with `b` and `c` together
it is one.

=== "All separate"

    ```mermaid
    graph LR
      A1[a] ==>|"network"| B1[b] ==>|"network"| C1[c]
    ```

=== "b and c together"

    ```mermaid
    graph LR
      subgraph p["one process"]
        B2[b] --> C2[c]
      end
      A2[a] ==>|"network"| B2
    ```

The measurements show it. Keeping the chatty chains inside one process in `groups` cut the remote calls per order from 14.4 (eight services) to 8.5, and recovered 53% of the throughput (263 against 172 orders/s, C2 JVMs, 16 clients).

**Cost of a boundary.** Calls per order that would cross a boundary around each candidate, from the module call counts of one run. Each crossing call costs about 1.1 to 1.3 ms extra here (remote 1.7 to 2.0 ms against local 0.5 to 0.7 ms), **on loopback, with no TLS, authentication or proxies, so a floor**, against a
median of 11.6 ms for the whole order in one process (small JVMs, four clients; the per-call figures come from that run).

| If you split out... | Crossing calls/order | About +ms per order | Notes |
| --- | --- | --- | --- |
| `notifications` | 2.0 | about 0 | already asynchronous, off the request path |
| `payments` | 3.0 | +3.6 | also the strongest compliance reason |
| `pricing` | 4.0 | +4.8 | |
| `catalog` | 4.1 | +4.9 | cached reads, so less in practice |
| `customers` | 6.0 | +7.2 | five consumers; about half the calls are cache hits |
| `inventory` | 6.1 | +7.3 | chatty with `shipping` |
| `shipping` with `inventory` | 7.0 | +8.4 | the chain stays inside |

**The best grouping, computed.** Every way of partitioning the 8 modules (4,140 of them) was scored by the calls per order crossing a boundary, and the minimum kept for each number of deployables:

| Deployables | Fewest remote calls/order, raw (cache off) | After the cache |
| --- | --- | --- |
| 1 | 0 | 0 |
| 2 | 2.0 (`notifications` alone) | 1.0 (`catalog` alone) |
| 3 | 4.9 (`notifications`, `payments`, the rest) | 2.7 (`catalog`, `pricing`, the rest) |
| 4 | 8.0 | 4.4 |
| 8 | 20.0 | 14.4 |
| demo `groups` (front, fulfilment, accounts) | **10.9** | **8.8** |

Reading it:

* The demo's grouping was chosen along team and domain lines, not by call weight, and is far from the minimum for three groups. A pure minimum peels off the weakly coupled leaves (`notifications`, `payments`, `catalog`), so those are the cheapest to extract.
* **The cache changes the answer.** After caching, `catalog` is nearly free to separate, because most of its calls never leave the caller. Recompute the best grouping after tuning caches.
* A minimum cut is one input. It ignores ownership, compliance, scaling and balance, so add those as constraints (for example, `payments` must be separate, `shipping` and `inventory` must stay together).
* The weights depend on the order mix.
* A prediction, **not measured**: a `groups`-style profile built on the computed cut should keep much more of the single-process throughput than the domain-based `groups` (67%).

??? note "How the numbers were computed"

    From the `comparison.json` saved by a `perf.sh --modes ... --cache both` run: for one run of `microservices`, the per-order call count of each caller→callee pair (`count / orders`;
    after the cache, `(count - hits) / orders`), then for every partition of the 8 modules the sum of the pair weights that cross a block.

    ```python
    weights[frozenset((caller, callee))] += (count - (hits if after_cache else 0)) / orders
    # for each partition of the modules:  crossing = sum(w for pair, w in weights.items() if blocks differ)
    ```

## 7. Many teams: a macro service per team, contracts between them

For an organisation of many teams, the macro service is the team-level unit. Inside it, modules are the logical boundaries; between teams there are published, versioned contracts.

* **Version skew shrinks to the team boundary.** Inside a team's macro service everything is one commit and one release unit. Only published APIs need independent compatibility.
* **Teams form a directed graph.** Heavily used upstream teams (the demo's `customers` has five consumers) must publish stable, additive APIs with an SLA. Orchestrating teams (like `orders`, with six dependencies) are the fragile ones: use asynchronous follow-ups, timeouts and caches. Cycles between teams must be rejected, as the group graph must stay acyclic.
* **New features are modules by default.** A new deployable needs a reason from the forces table.

Missing for this scenario: a published-versus-internal marker on `@RpcApi`, per-team artifact versioning and a registry of contracts with cycle and fan-in detection, cross-team asynchronous messaging (the design forbids a broker, and Modulith events are in-process),
service-to-service authentication, discovery and canary routing, and wire-level contract tests between versions.

## Parked ideas

Not built, in roughly the order that would pay off:

1. `scripts/new-module.sh`, which scaffolds a module (poms, schema and roles, datasource, profiles, ownership), plus a default-URL convention so a new module does not need a line in every profile.
2. ArchUnit rules for slices inside a module (no cycles, one table per slice, no cross-slice joins).
3. A "boundary radar": per-module coupling, cross-slice table use and files that change together.
4. A `boundary-cost` script that scores a proposed grouping from traces (crossing calls and added latency) and searches for minimum-cost groupings under constraints, and a measurement of the prediction above.
5. An `exposure = INTERNAL | PUBLISHED` attribute on `@RpcApi`, with an ArchUnit rule and `japicmp` on published APIs only.
6. A `--replicas N` option and a small path router for `demo.sh`, to demonstrate and measure scale-out and routed pools, and to test several replicas against one database.
7. A documented procedure for promoting a slice to a module, and for merging two modules.
8. The organisation-level items in section 7.
