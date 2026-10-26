# Cache, parallel quotes and follow-up delivery

Three optional features, each with an off-switch, each measured by running the same workload with the switch off and on.
Same conditions as [the mode comparison](modes.md): 16 clients, 1,500 measured orders, deterministic order stream.

## Result cache

[`@Cached`](../features/cache.md) lets four read methods (`customers.getCustomer`, `customers.getPaymentMethod`, `catalog.getProduct`,
`catalog.productExists`) reuse their result for 30 seconds. Every mode, cache off against on:

--8<-- "performance/_generated/cache-chart.md"

--8<-- "performance/_generated/cache-table.md"

**What it shows**

* The more a call costs, the more a hit saves. In the split modes the cache adds **6 to 19%** throughput and takes **5 to 14%** off the median order.
  In a single process the same cache adds 3 to 6%.
* Read differences of about 5% as noise: `modulith` and `macro-modulith` are the same code and both show +3%.
* About 30% of all calls in a run were answered by the cache (8,994 of 29,919 in `modulith`). A hit costs 0.00 to 0.01 ms.

**Hit rates** (share of calls to each method answered from the cache, whole run):

--8<-- "performance/_generated/cache-hits.md"

The rates are moderate because the runs are short (a few seconds for 1,500 orders), the keys are spread over 10,000 customers, and each of the eight
processes in the two eight-service modes warms its own cache (the customer rate falls from 50% to 46% there). A longer run would raise them; the 30-second
time-to-live was not the limit here.

## Parallel quotes

[Parallel quotes](../features/parallel.md) run the customer check and the two quotes at the start of `placeOrder` at the same time instead of one after the other.
Sequential against parallel, for the modes where calls cost something (the cache is on in both):

--8<-- "performance/_generated/parallel-chart.md"

--8<-- "performance/_generated/parallel-table.md"

**What it shows**

* It helps in proportion to how slow the calls are: **nothing measurable** in one process, **+8%** over HTTP to itself, **+3 to 10%** with three processes (`split-dbs` and `groups`, which disagree, so read it as a range), **+15 to 17%** with eight services.
  The median order falls by 0%, 7%, 1 to 9% and 12 to 13% respectively.
* The effect is predictable from the per-call table: in `microservices` the three reads cost 0.78 + 3.46 + 4.69 = 8.9 ms one after the other and about
  4.7 ms together (mean latencies, single run), a saving of about 4 ms per order. At 16 clients the median also contains queueing, so the fall in the median (110 to 96 ms) is not a simple sum of the saving.
* It removes waiting, not work. It does not make the machine do less, so the gain shrinks when the machine is saturated.

## Follow-up delivery: outbox or events

[Follow-ups](../features/follow-ups.md) (the confirmation message, the release of a reservation, the voiding of a payment) are made durable in the order's
transaction and delivered afterwards, either by the **outbox** relay or as **Spring Modulith events**. On the request path:

--8<-- "performance/_generated/async-chart.md"

--8<-- "performance/_generated/async-table.md"

**What it shows**

* In one process the two are within noise (451 against 439 orders/s, outbox against events).
* Events were **3 to 6% lower** in throughput in this run (in every mode) and the median order 4 to 7% higher. The cause was not investigated (the registry's
  extra status updates and the asynchronous executor are candidates). It is a small cost, close to the run-to-run noise. Events run inside the same JVM as the orders, so the cost can be expected to grow with the order rate; the outbox moves that work to a table and a relay that can be tuned.
* The p99 swings both ways (`modulith` 85.2 against 57.9 ms, `groups` 96.0 against 105.2) and is one slow stretch in one run; the p50 and p95 do not move with it.
* Where the two really differ is **how quickly the follow-ups are delivered**, which a round-trip measurement cannot see. That is [the next page](load.md).
