# The eight modes compared

Same code, same API, same tests, same workload, and (except `split-dbs`) the **same single PostgreSQL instance**: the eight-process modes share one database server, so the comparison measures the cost of the process boundary, not of separate databases. Separate instances per deployable unit or per module are compared [at the end of this page](#one-database-instance-one-per-deployable-unit-or-one-per-module). Sixteen concurrent clients, 1,500 measured orders per run, result cache on, parallel quotes on,
follow-ups through the outbox, a `VACUUM ANALYZE` after the warm-up ([method](method.md)). The charts and the first table are means over 5 runs; the table under "All numbers" is the first of those runs, which also holds the call counts.

--8<-- "performance/_generated/headline-chart-throughput.md"

--8<-- "performance/_generated/headline-chart-latency.md"

--8<-- "performance/_generated/headline-table.md"

--8<-- "performance/_generated/headline-dropped.md"

The median is not the time one order takes alone: with 16 clients the orders queue behind each other, so the median is roughly *clients ÷ throughput*.

## All numbers

--8<-- "performance/_generated/modes-table.md"

"Calls by binding" counts every module-to-module call in the run and gives its mean latency. A call answered by the result cache has binding `cache`;
a call between modules in the same process is `local`; the other bindings are described in [the architecture](../architecture.md#one-interface-four-bindings).

## What the numbers say

### In one process, the module boundary is almost free

* `modulith` and `macro-modulith` are the same thing built two ways, and they measure the same (445 and 442 orders/s).
* `loopback-codec` forces every call through JSON encoding and decoding, in memory: 98% of the throughput, median 35.2 ms against 34.2 ms. Serialisation is a
  small tax, and it is the price of proving that no object identity or shared transaction crosses a boundary.
* `loopback-http` makes every call a real HTTP request to the same process: 70% of the throughput and a median of 48.1 ms. The cost of the *protocol*,
  without a second JVM, is already 30% of the throughput.

### Between processes, the price is paid per call

* Three processes (`groups`): 56% of the throughput, median 62.2 ms. Calls inside a group stay local at about 0.4 ms, calls between groups cost about 1.5 ms.
* Eight services (`microservices`): 36%, median 96.9 ms, and every call is remote at about 2.2 ms.
* `split-dbs` equals `groups` (250 against 247 orders/s): putting each group on its own PostgreSQL instance cost nothing measurable here. (On one machine
  the database servers do not compete for the same network link; across real hosts it would.)
* `microservices-macro`, the same eight processes built from the single macro jar, matches `microservices` (156 against 159, inside the run-to-run
  noise). The one-artifact claim costs nothing at runtime.

### Where the time goes

Mean latency of the most expensive module-to-module calls, over all bindings used:

--8<-- "performance/_generated/calls-table.md"

Read across a row: `orders -> shipping.quote` takes 0.70 ms inside one process and 4.69 ms between eight services. Reading down the table shows that a "local"
call is not free either: even in `modulith` each call is one database query, which is why the expensive rows (quote, create shipment, reserve) are the ones
that do the most work.

## One database instance, one per deployable unit, or one per module

The tables and charts above use the **shared** layout: one PostgreSQL instance with a schema per module. Two other layouts were measured in every mode and every combination of the other switches ([the full matrix](matrix.md)):

* **`split`** gives every deployable unit its own instance: three for `groups`, eight for the services. A one-process mode has a single unit, so for it split is the same as shared and was not repeated.
* **`per-module`** gives each of the eight modules its own instance in any mode, so even `modulith` (one process) talks to eight database servers. For the services it is the same as `split`.

Cache on, parallel quotes on, outbox ([how to run them](../running.md)):

--8<-- "performance/_generated/dbs-chart.md"

--8<-- "performance/_generated/dbs-table.md"

**What it shows**

* **Where processes already cross the network, more database instances cost nothing measurable.** `groups` and the two eight-service modes were within 2% of shared in this table, and across all 8 switch combinations between 23% below and 4% above, 1% below at the median (`groups` has one low cell, cache and parallel quotes both off with the outbox: 165 against 214 orders/s, which its three-instance run, 213, does not repeat).
* **In one process the batch figures read 3 to 10% lower on eight instances (median across the combinations), but most of that was drift.** The shared and the eight-instance runs of those modes were made about two hours apart. Repeating `modulith`, `loopback-codec` and `loopback-http` three times with shared and eight instances alternating gave means of 449 against 449, 440 against 418 and 324 against 319 orders/s (+0%, -5% and -2%; the -5% comes from a single slow run, 364 orders/s, and the medians of the three runs differ by 1%) ([reports](reports.md)). Treat the cost of eight instances in one process as at most a few percent.
* **One machine.** The database servers share the same CPUs, memory and loopback, so a server per unit or per module costs only its own overhead here. A real split adds what one machine cannot show: network latency to each database, and a server per service to provision, secure, back up and size. Treat it as a floor ([where the evidence stops](../case.md#where-the-evidence-stops)).

## Choosing

| If you value... | Pick |
| --- | --- |
| The best latency and throughput | `modulith` (or `macro-modulith`, equal) |
| A boundary that is proven to hold, at 2% cost | `loopback-codec` as a test configuration |
| A few deployable units with most of the speed | `groups`: more than half of the monolith's throughput with three processes instead of eight |
| Independent deployment and scaling of every module | `microservices` or `microservices-macro`, at about a third of the throughput on this machine |

`groups` is the interesting middle: it keeps the chatty chains (`pricing → catalog`, `shipping → inventory → catalog`) inside one process, so those calls stay
at local cost.
