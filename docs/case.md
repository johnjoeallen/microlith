# Why microservices should not be the default starting point

**Who wants their applications to go slower?**

Every boundary between processes puts a network hop on every call that crosses it. In this project's measurements, splitting the same application into eight services made an
order take about 2.8 times as long and cut the throughput by more than 60% (32.5 ms against 90 ms median, and 457 against 170 to 171 orders per second, 16 clients; with small JVMs and 4 clients it was twice as long and half the throughput). Nobody sets out to do that, yet it is what starting with microservices does
by default, before any need for the split has been shown.

!!! note "Evidence, and the extrapolation from it"
    The numbers on this page come from this project: one domain, one machine. Carrying them over to real systems is the author's judgement, which is why the argument below is made in
    steps, with its limits stated at the end. In particular, **every network call here was local** (loopback on one machine) with **no TLS, no authentication, no proxy, load balancer or
    service mesh** in the path, so the network cost measured is a floor, not a typical figure.

**Thesis.** Starting a product as a set of microservices should not be the default. The default should be a **macro service**: one team's modular service with
build-enforced module boundaries, deployed as one unit. It is the safe starting point because it keeps every later option open: the same artifact can be deployed as independent, separately scaled services *without
splitting the codebase and without creating separate deployables*, and splitting later is far easier than merging back. Microservices-first needs an analysis that supports it. It is a decision, not a starting position.

This page argues that from the measurements and the design in this project. It is an argument from evidence, not a mathematical proof, and the [last sections](#where-the-evidence-stops) say where it stops.

## The argument in six steps

### 1. The start is made when you know the least

At the beginning of a product the boundaries, the volumes and the change patterns are guesses. Teams that begin with "how many services do we need?" commit to the decomposition at the point of
maximum ignorance, and the usual first answer, `n` layers with `m` services per layer, is the worst one: a technical cut makes every feature a coordinated change across services.
As an extrapolation from the measured cost per hop (a lower bound on one machine), the demo's 20 calls per order, each routed through two extra layer services, would be about 60 hops, roughly **+70 ms**, against **+11 ms** measured for the business-aligned split. Good boundaries come from journeys, invariants and change patterns, and those appear as the product
is built. See [start small and let boundaries appear](principles.md#4-start-small-and-let-boundaries-appear).

### 2. Splitting early has an immediate, measured cost

Same code, same tests, same workload, eight deployment shapes ([the measurements](performance/modes.md)). The chart and the table are the mean of 20 runs per mode with C2 JVMs and 16 clients ([headline numbers](performance/index.md#headline-numbers)); the call counts below are from the small-JVM, four-client comparison:

--8<-- "performance/_generated/jvm-matrix-chart-throughput.md"

| | One unit (`modulith`) | Three groups | Eight services |
| --- | --- | --- | --- |
| Throughput | 457 orders/s | 263 (58%) | 170 to 171 (37%) |
| Median order (16 clients) | 32.5 ms | 58.4 ms | 90.0 to 90.4 ms |
| Remote calls per order | 0 | 8.5 | 14.4 |
| Version-skew pairs to keep compatible | 0 | 3 | 13 |
| Processes to run, patch and be on call for | 1 | 3 | 8 |

Each call between processes costs about 1.1 to 1.3 ms more than a local one (0.5 to 0.7 ms), and an order makes about 20 calls. Cache and parallel reads win part of it back (+13 to 26% and +9 to 18% in the split modes), but those are mitigations
of a cost the team chose to take on. All three columns used the same single PostgreSQL instance (see [where the evidence stops](#where-the-evidence-stops)). For a single team that coordinates perfectly well, the benefit side of that bargain is mostly options it does not yet need.

### 3. Microservices mainly buy team independence; one team has no use for it yet

The benefits of separate services are independent deployment, scaling, failure isolation and ownership. Each is a [force](principles.md#4-start-small-and-let-boundaries-appear) with evidence to gather:
a cadence conflict, a measured hot spot, a differing criticality, an audit boundary, a second team. A team of 5 to 10 that releases together has none of these on day one. What it does have is the *design*
discipline of microservices (clear contracts, ownership, data ownership, one-way dependencies), and the macro service provides that [logically and enforced by the build](principles.md#3-separate-logically-first-physically-only-when-forced),
without running a distributed system.

The failure mode of starting with services is also a team-coordination one: a team that keeps adding services for each feature accumulates pipelines, on-call surface, patching and skew faster than its domain
grows, and ends with a distributed monolith owned by one team. One team means one deployable until a force says otherwise.

### 4. The decision is asymmetric: splitting is cheap later, merging is not

| | Splitting later | Merging later |
| --- | --- | --- |
| Deployment level | configuration: `enabled=false` plus a URL | configuration: enable it locally |
| Contract | write an API at a cut you now understand | remove it and inline every consumer |
| Data | already isolated per module (no cross-schema keys, separate roles); a schema move is a metadata change, an instance move a copy | collisions: each schema has its own migration history and table names |
| Ownership | one team becomes two | two owners must become one |
| A wrong boundary costs | a refactor in the IDE and one commit | a migration across repos, contracts and consumers |

Starting in the cheap-to-be-wrong shape and moving to services on evidence loses almost nothing. Starting with services and discovering the boundaries were wrong costs a migration. See
[changing a boundary later](principles.md#5-changing-a-boundary-later-splitting-is-easier-than-re-combining).

### 5. The macro service already deploys as independent services

The usual objection to "start with a monolith" is that you then have to rewrite it. Here you do not. The same artifact, with different configuration, runs as one unit, as three groups, or as eight independent processes, each of which can be scaled, restarted
and failed on its own:

* `microservices-macro` runs the **single macro jar eight times**, one module enabled in each. It measured the same as the eight dedicated per-service jars (170 against 171 orders/s, within noise). So independent deployments need **no separate codebase and no
  separate artifact**.
* The identical scenarios pass in all eight modes with identical results and exception types, and a failure suite runs eight processes while connections are cut and processes restarted.
* Different deployments can run different versions of the artifact (a service on 1.4 while the rest are on 1.5), as long as the `@RpcApi` changes stay additive. `japicmp` enforces that once you name a baseline release.
* Identical pods behind a router give per-API scaling and canarying with no network hop between modules, [see routing](principles.md#2-boundaries-placement-replication-routing-and-data-are-separate-choices).
* Whether it still holds is a test, not a claim: the contract tests run the same scenarios through every binding, including `loopback-codec` (97% of the single-process throughput), on every build, and they pass only if no module relies on shared objects or a shared transaction.

What it does not give is real isolation between modules *in one process*, or a different runtime per module. Those are physical forces, handled by splitting that module or group when the evidence shows up.

### 6. So analyse first, and the analysis is cheap

The default is the macro service as one unit. Deviate, per module and with a recorded reason, when a force has evidence. The [forces table](principles.md#4-start-small-and-let-boundaries-appear) and the
[boundary-cost numbers](principles.md#6-place-modules-by-call-weight) turn "should we split this?" into a measurement: the calls per order that would cross the boundary times about 1.2 ms, against what the split buys. The cheapest extractions
(`notifications`, `payments`, and read-only reference data behind a cache) are visible in the data, and the expensive ones (`shipping` with `inventory`) are visible too.

## What each starting point commits you to

| | Microservices first | Macro service, deployed as one unit |
| --- | --- | --- |
| Boundary decisions | all at once, with the least information | made as seams appear |
| If a boundary is wrong | a cross-service migration | a refactor |
| Runtime cost on day one | about 2× latency, half the throughput (this domain) | baseline |
| Operational surface | N pipelines, N on-call surfaces | one |
| Version skew | grows with the edges | none inside the unit |
| Can it become independent services? | it already is | yes, by configuration, from the same artifact |
| Can it go back? | hard (merge) | trivially (enable locally) |
| Analysis needed | the one that justifies it (often skipped) | the one that justifies *leaving* the default |

## Decision record

Keep the choice explicit, one page per macro service:

```
Scope:        one bounded context, one owning team
Start:        macro service, deployed as one unit (default)
Deviations:   <module or group> -> <separate deployable>
              because <force: cadence | scale | isolation | security | tech | ownership>
              evidence: <measurement, SLO, audit finding, second team>
              cost: <crossing calls per order x ~1.2 ms>, <operational surface added>
Triggers:     what would make us split another module, and what would make us merge two
Review:       every <n> sprints, with the boundary metrics
```

## What the best counter-arguments say, and the answer

| Objection | Answer |
| --- | --- |
| "Our boundaries are already known and stable." | Then the first split is cheap and justified. Record it as a deviation with its evidence. The default still costs nothing to start from. |
| "We have several teams from day one." | Give each team its own macro service with published contracts. The unit is the team, not the module (see [many teams](principles.md#7-many-teams-a-macro-service-per-team-contracts-between-them)). |
| "Hard isolation is required from the start (compliance, a vendor, another runtime)." | Split exactly that module or group, and leave the rest together. |
| "We have a mature platform, so a service costs little." | The operational part may be cheap. The latency, throughput and version-skew costs remain, and the boundary risk is unchanged. |
| "Real services do real work, so the hop is small by comparison." | True, and it makes the relative cost smaller than measured here. A real network, TLS, authentication and any proxies make the absolute cost larger than the loopback figures. The effects point in opposite directions, so measure your own journeys. |
| "A modular monolith erodes into a big ball of mud." | It does when the boundaries are conventions. Here the build enforces them (no cross-module tables, an acyclic graph, compatibility-checked APIs) and the contract tests exercise them through every binding. |
| "It is harder to hire or scale a team on one codebase." | That is the exit ramp: a second team or a diverging vocabulary means a second macro service. |

## What would change the conclusion

* Measured boundary cost that is negligible for your real journeys (few crossing calls per user action) *and* a force with evidence. Then split.
* A domain where boundaries and volumes are genuinely known up front.
* An organisation where independent release cadence per component is the product requirement, not a hope.

## Where the evidence stops

* **One machine, loopback only, and nothing else in the path.** Every call between processes ran over the loopback interface of one host. There was no TLS or mTLS (no handshakes, no per-call encryption), no authentication or token
  validation on each call, no load balancer, gateway or service-mesh sidecar adding hops, no service discovery, and no real network latency, packet loss, retries or timeouts, and no cross-zone traffic. Real deployments have most of these. That makes
  the measured cost of splitting a **floor** for its network and security part, and the extra hops, handshakes and proxies come on top of every boundary you add.
* **One shared database server.** Every mode except `split-dbs` used a single PostgreSQL instance with a schema per module, so even the eight services shared one database server. That isolates the cost of the
  *process* boundary, which is the point of the comparison, but it does not charge the services for what real microservices usually add: a database instance per service to provision, secure, back up, monitor and size.
  Three instances in `split-dbs` cost nothing measurable here (266 against 263 orders/s), though that is on one machine. So again the cost measured for the split shapes is a floor.
* **Cheap calls.** The demo's calls are one primary-key query each, so the network share of an order is larger than in services that do real work per call. This pushes the other way, and is why the relative cost measured here may overstate
  what a heavy-calls domain would see. The two effects are real and run in opposite directions; measure your own journeys.
* **One domain and one workload.** The demo's order flow and its order mix; a different domain or mix gives different call counts and different weights.
* **One run per configuration.** Differences below about 5% are noise; the parallel and cache effects shown are well above it, the outbox-versus-events difference is not.
* **No long-term operation.** The measurements do not cover on-call load, incidents or a growing team. Those arguments come from the design and from the sprawl scenario, not from data.
* **Parked work.** A few things the argument leans on are practices rather than shipped features: the cheap-module generator, the boundary-cost tool and a published-versus-internal API marker are in the [parked ideas](principles.md#parked-ideas).
* **A default is not a ban.** The claim is that microservices-first needs the analysis, not that it is never right.
