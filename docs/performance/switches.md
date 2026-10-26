# Cache, parallel quotes and follow-up delivery

Three optional features, each with an off-switch, each measured by running the same workload with the switch off and on.
Same conditions as [the mode comparison](modes.md): four clients, 1,200 measured orders, deterministic order stream.

## Result cache

[`@Cached`](../features/cache.md) lets four read methods (`customers.getCustomer`, `customers.getPaymentMethod`, `catalog.getProduct`,
`catalog.productExists`) reuse their result for 30 seconds. Every mode, cache off against on:

--8<-- "performance/_generated/cache-chart.md"

--8<-- "performance/_generated/cache-table.md"

**What it shows**

* The more a call costs, the more a hit saves. In the split modes the cache adds **13 to 26%** throughput and takes **6 to 21%** off the median order.
  In a single process the same cache adds 1 to 10%.
* Read differences of about 5% as noise: `modulith` and `macro-modulith` are the same code and show +1% and +8%.
* About 29% of all calls in a run were answered by the cache (7,050 of 24,015 in `modulith`). A hit costs 0.00 to 0.03 ms.

**Hit rates** (share of calls to each method answered from the cache, whole run):

--8<-- "performance/_generated/cache-hits.md"

The rates are moderate because the runs are short (a few seconds for 1,200 orders), the keys are spread over 10,000 customers, and each of the eight
processes in the two eight-service modes warms its own cache (the customer rate falls from 50% to 44% there). A longer run would raise them; the 30-second
time-to-live was not the limit here.

## Parallel quotes

[Parallel quotes](../features/parallel.md) run the customer check and the two quotes at the start of `placeOrder` at the same time instead of one after the other.
Sequential against parallel, for the modes where calls cost something (the cache is on in both):

--8<-- "performance/_generated/parallel-chart.md"

--8<-- "performance/_generated/parallel-table.md"

**What it shows**

* It helps in proportion to how slow the calls are: **+2%** in one process, **+9%** over HTTP to itself, **+12 to 14%** with three processes, **+18%** with eight services.
  The median order falls by 3%, 10%, 10 to 12% and 15% respectively.
* The effect is predictable from the per-call table: in `microservices` the three reads cost 0.74 + 2.85 + 4.35 = 7.9 ms one after the other and about
  4.4 ms together (mean latencies). The saving, about 3.5 ms, matches the observed fall in the median order, 25.9 to 22.0 ms.
* It removes waiting, not work. It does not make the machine do less, so the gain shrinks at saturation (see [load](load.md)).

## Follow-up delivery: outbox or events

[Follow-ups](../features/follow-ups.md) (the confirmation message, the release of a reservation, the voiding of a payment) are made durable in the order's
transaction and delivered afterwards, either by the **outbox** relay or as **Spring Modulith events**. On the request path:

--8<-- "performance/_generated/async-chart.md"

--8<-- "performance/_generated/async-table.md"

**What it shows**

* In one process the two are identical (326 against 326 orders/s).
* In the split modes events were **6% lower** in throughput in this run and the median order 2 to 3% higher. The cause was not investigated (the registry's
  extra status updates and the asynchronous executor are candidates). It is a small cost, close to the run-to-run noise.
* The `microservices` p99 (41.8 against 78.9 ms) is one slow stretch in one run; the p50 and p95 do not move with it.
* Where the two really differ is **how quickly the follow-ups are delivered**, which a round-trip measurement cannot see. That is [the next page](load.md#delivery-of-follow-ups).
