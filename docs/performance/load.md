# Follow-up delivery: outbox or events

## On the request path

[Follow-ups](../features/follow-ups.md) (the confirmation message, the release of a reservation, the voiding of a payment) are made durable in the order's
transaction and delivered afterwards, either by the **outbox** relay or as **Spring Modulith events**. On the request path:

--8<-- "performance/_generated/async-chart.md"

--8<-- "performance/_generated/async-table.md"

**What it shows**

* In one process the two are within noise (451 against 439 orders/s, outbox against events).
* Events were **3 to 6% lower** in throughput in this run (in every mode) and the median order 4 to 7% higher. The cause was not investigated (the registry's
  extra status updates and the asynchronous executor are candidates). It is a small cost, close to the run-to-run noise. Events run inside the same JVM as the orders, so the cost can be expected to grow with the order rate; the outbox moves that work to a table and a relay that can be tuned.
* The p99 swings both ways (`modulith` 85.2 against 57.9 ms, `groups` 96.0 against 105.2) and is one slow stretch in one run; the p50 and p95 do not move with it.
* Where the two really differ is **how quickly the follow-ups are delivered**, which a round-trip measurement cannot see. That is the next section.

## Delivery speed

The request path cannot show this, because follow-ups are delivered *after* the order returns. This test is older than the 16-client runs: it used the former small demo JVMs, ran before the `VACUUM ANALYZE` step and is not part of `scripts/perf.sh`, but the result is a property of the relay, not of the machine. The test: place 978 orders flat out, then time how long until every
confirmation message has actually arrived in the notifications module.

--8<-- "performance/_generated/drain-chart.md"

--8<-- "performance/_generated/drain-table.md"

* **Events** delivered every message **0.1 s** after the last order.
* The **outbox** needed **48.9 s**, about 20 messages a second. That is the relay's design: it claims one batch of 20 entries per one-second poll. It is a tuning limit of
  *this relay*, not of the pattern: a bigger batch or a shorter poll interval (`modules.orders.outbox.poll-interval`) would close most of the gap.
* Under sustained load the outbox backlog keeps growing while orders arrive faster than 20 a second, so a notification can be minutes late, though the order itself is never slowed.

Note also how each behaves as the order rate rises. The outbox can be tuned and handles bursty load well: follow-ups queue in a table and drain at the relay's pace, so
a burst is spread over time. Events are all in process, so their work grows with the order rate and loads the same JVM that serves the orders. This test measures neither effect directly; it only times a drain after a flat-out run.

Which to choose is [a judgement, not a benchmark](../features/follow-ups.md#which-is-better): events drain faster out of the box and come with a library's
semantics, at the cost of an adapter and a retry policy to maintain; the outbox is simple, self-contained and can be tuned.
