# Delivery speed of follow-ups

The request path cannot show this, because follow-ups are delivered *after* the order returns. The test: place 978 orders flat out, then time how long until every
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
