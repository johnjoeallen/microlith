# Cache and parallel quotes

Two optional features, each with an off-switch, each measured by running the same workload with the switch off and on.
Same conditions as [the mode comparison](modes.md): 16 clients, 1,500 measured orders, deterministic order stream. The figures come from [the full matrix](matrix.md), where every combination of the switches was run once, with the other switches at their defaults (cache on, parallel quotes on, outbox, one shared database instance). Each cell is one run, so read differences of about 5% as noise.

## Result cache

[`@Cached`](../features/cache.md) lets four read methods (`customers.getCustomer`, `customers.getPaymentMethod`, `catalog.getProduct`,
`catalog.productExists`) reuse their result for 30 seconds. Every mode, cache off against on:

--8<-- "performance/_generated/cache-chart.md"

--8<-- "performance/_generated/cache-table.md"

**What it shows**

* The more a call costs, the more a hit saves. Over HTTP to itself the cache adds **11%**, with three processes **5 to 10%**, with eight services **18%**, and it takes **5 to 18%** off the median order in those modes.
  In a single process the same cache adds 3 to 5%.
* Read differences of about 5% as noise: `modulith` and `macro-modulith` are the same code and show +5% and +3%.
* About 30% of all calls in a run were answered by the cache (8,994 of 29,919 in `modulith`). A hit costs 0.00 to 0.01 ms.

**Hit rates** (share of calls to each method answered from the cache, whole run):

--8<-- "performance/_generated/cache-hits.md"

The rates are moderate because the runs are short (a few seconds for 1,500 orders), the keys are spread over 10,000 customers, and each of the eight
processes in the two eight-service modes warms its own cache (the customer rate falls from 50% to 46% there). A longer run would raise them; the 30-second
time-to-live was not the limit here.

## Parallel quotes

[Parallel quotes](../features/parallel.md) run the customer check and the two quotes at the start of `placeOrder` at the same time instead of one after the other.
Sequential against parallel, every mode (the cache is on in both):

--8<-- "performance/_generated/parallel-chart.md"

--8<-- "performance/_generated/parallel-table.md"

**What it shows**

* It helps in proportion to how slow the calls are: **nothing measurable** in one process (-2% to +1%), **+4%** over HTTP to itself, **+7 to 9%** with three processes, **+15 to 16%** with eight services.
  The median order falls by 0 to 2%, 2%, 5 to 6% and 11 to 12% respectively.
* The effect is predictable from the per-call table: in `microservices` the three reads cost 0.78 + 3.46 + 4.69 = 8.9 ms one after the other and about
  4.7 ms together (mean latencies, single run), a saving of about 4 ms per order. At 16 clients the median also contains queueing, so the fall in the median (108 to 96 ms) is not a simple sum of the saving.
* It removes waiting, not work. It does not make the machine do less, so the gain shrinks when the machine is saturated.
