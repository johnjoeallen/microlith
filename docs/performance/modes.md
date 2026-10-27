# The eight modes compared

Same code, same API, same tests, same workload, and (except `split-dbs`) the **same single PostgreSQL instance**: the eight-process modes share one database server, so the comparison measures the cost of the process boundary, not of separate databases. Sixteen concurrent clients, 1,500 measured orders per run, result cache on, parallel quotes on,
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

## One database instance or one per deployable unit

Everything above ran on one shared PostgreSQL instance. `--dbs split` gives every deployable unit its own instance, which is three for `groups` and eight for the services ([how](../running.md)). The same load, with the result cache on:

| Mode | Shared: orders/s, p50 | Split: orders/s, p50 | Change in orders/s |
| --- | ---: | ---: | ---: |
| `groups` (3 instances) | 250, 61.7 ms | 253, 62.5 ms | +1% |
| `microservices` (8 instances) | 161, 94.6 ms | 159, 96.3 ms | -1% |
| `microservices-macro` (8 instances) | 159, 96.2 ms | 158, 95.7 ms | -1% |

One run per cell, so these are within noise: on this machine, with the database servers sharing the same CPUs and memory and the same loopback, giving each unit its own server cost nothing measurable. A real split adds what one machine cannot show: network latency to each database, and a server per service to provision, secure, back up and size. Treat it as a floor ([where the evidence stops](../case.md#where-the-evidence-stops)). [Raw report](reports/dbs.md).

## Choosing

| If you value... | Pick |
| --- | --- |
| The best latency and throughput | `modulith` (or `macro-modulith`, equal) |
| A boundary that is proven to hold, at 2% cost | `loopback-codec` as a test configuration |
| A few deployable units with most of the speed | `groups`: more than half of the monolith's throughput with three processes instead of eight |
| Independent deployment and scaling of every module | `microservices` or `microservices-macro`, at about a third of the throughput on this machine |

`groups` is the interesting middle: it keeps the chatty chains (`pricing → catalog`, `shipping → inventory → catalog`) inside one process, so those calls stay
at local cost.
