# The eight modes compared

Same code, same API, same tests, same workload. Four concurrent clients, 1,200 measured orders per run, result cache on, parallel quotes on,
follow-ups through the outbox ([method](method.md)).

--8<-- "performance/_generated/modes-throughput.md"

--8<-- "performance/_generated/modes-latency.md"

## All numbers

--8<-- "performance/_generated/modes-table.md"

"Calls by binding" counts every module-to-module call in the run and gives its mean latency. A call answered by the result cache has binding `cache`;
a call between modules in the same process is `local`; the other bindings are described in [the architecture](../architecture.md#one-interface-four-bindings).

## What the numbers say

### In one process, the module boundary is almost free

* `modulith` and `macro-monolith` are the same thing built two ways, and they measure the same (336 and 331 orders/s).
* `loopback-codec` forces every call through JSON encoding and decoding, in memory: 93% of the throughput, median 12.4 ms against 11.6 ms. Serialisation is a
  small tax, and it is the price of proving that no object identity or shared transaction crosses a boundary.
* `loopback-http` makes every call a real HTTP request to the same process: 71% of the throughput and a median of 16.3 ms. The cost of the *protocol*,
  without a second JVM, is already 29% of the throughput.

### Between processes, the price is paid per call

* Three processes (`groups`): 67% of the throughput, median 17.4 ms. Calls inside a group stay local at about 0.5 ms, calls between groups cost 1.7 ms.
* Eight services (`microservices`): 49%, median 22.7 ms, and every call is remote at about 2.0 ms.
* `split-dbs` equals `groups` (230 against 225 orders/s): putting each group on its own PostgreSQL instance cost nothing measurable here. (On one machine
  the database servers do not compete for the same network link; across real hosts it would.)
* `microservices-macro`, the same eight processes built from the single macro jar, matches `microservices` (178 against 166, inside the run-to-run
  noise). The one-artifact claim costs nothing at runtime.

!!! note "A single p99 outlier"
    `microservices` shows p99 77 ms against 44.7 ms for `microservices-macro`. That is one slow stretch in one run, not a property of the shape; compare the
    p50 and p95 columns, which agree.

### Where the time goes

Mean latency of the most expensive module-to-module calls, over all bindings used:

--8<-- "performance/_generated/calls-table.md"

Read across a row: `orders -> shipping.quote` takes 1.16 ms inside one process and 4.35 ms between eight services. Reading down the table shows that a "local"
call is not free either: even in `modulith` each call is one database query, which is why the expensive rows (quote, create shipment, reserve) are the ones
that do the most work.

## Choosing

| If you value... | Pick |
| --- | --- |
| The best latency and throughput | `modulith` (or `macro-monolith`, equal) |
| A boundary that is proven to hold, at 7% cost | `loopback-codec` as a test configuration |
| A few deployable units with most of the speed | `groups`: two thirds of the monolith's throughput with three processes instead of eight |
| Independent deployment and scaling of every module | `microservices` or `microservices-macro`, at about half the throughput on this machine |

`groups` is the interesting middle: it keeps the chatty chains (`pricing → catalog`, `shipping → inventory → catalog`) inside one process, so those calls stay
at local cost.
