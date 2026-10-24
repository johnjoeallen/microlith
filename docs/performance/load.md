# Load and delivery speed

## More clients

The mode comparison used four concurrent clients. Does more load buy more throughput? Same workload, result cache and parallel quotes on, **call tracing off**
for both rows (so tracing overhead does not distort the comparison), 3,000 measured orders.

--8<-- "performance/_generated/load-chart.md"

--8<-- "performance/_generated/load-table.md"

* **Throughput does not grow.** Going from 4 to 16 clients leaves orders per second flat (it falls 2 to 4%). The system is already saturated at about four
  concurrent orders on this machine.
* **Latency grows in proportion.** With a fixed number of clients that each wait for their answer, the median order is roughly *clients ÷ throughput*: 10.5 ms
  becomes 45.5 ms in `modulith`, 16.0 becomes 66.9 in `groups` and 20.0 becomes 84.1 in `microservices`. Extra clients only queue.
* **The ordering of the shapes does not change under load.** The monolith stays ahead of three processes, which stay ahead of eight.
* **The ceiling is mostly the application, not the load generator.** A second generator running at the same time added only about 10% to the combined rate
  (410 against 371 orders/s on `modulith`); that test also ran into out-of-stock failures because the stock was sized for one generator, so treat it as indicative.

!!! warning "Why the ceiling is where it is"
    The demo JVMs are deliberately small (`-Xmx320m`, C1-only JIT, serial GC) so that eight fit on a laptop, and each order makes about 20 module calls,
    most of them a database query. Change them with `DEMO_JAVA_OPTS` before reading anything into the absolute number.

## Delivery of follow-ups

The request path cannot show this, because follow-ups are delivered *after* the order returns. The test: place 978 orders flat out, then time how long until every
confirmation message has actually arrived in the notifications module.

--8<-- "performance/_generated/drain-chart.md"

--8<-- "performance/_generated/drain-table.md"

* **Events** delivered every message **0.1 s** after the last order.
* The **outbox** needed **48.9 s**, about 20 messages a second. That is the relay's design: it claims one batch of 20 entries per one-second poll. It is a tuning limit of
  *this relay*, not of the pattern: a bigger batch or a shorter poll interval (`modules.orders.outbox.poll-interval`) would close most of the gap.
* Under sustained load the outbox backlog keeps growing while orders arrive faster than 20 a second, so a notification can be minutes late, though the order itself is never slowed.

Which to choose is [a judgement, not a benchmark](../features/follow-ups.md#which-is-better): events drain faster out of the box and come with a library's
semantics, at the cost of an adapter and a retry policy to maintain; the outbox is simple, self-contained and can be tuned.
