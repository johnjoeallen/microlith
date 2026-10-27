# Every combination

The switches ([result cache](../features/cache.md), [parallel quotes](../features/parallel.md), [follow-up delivery](../features/follow-ups.md) and the
[database layout](../running.md)) were run in **every combination, in every mode**: 88 runs, each with 16 clients, 1,500 measured orders, a 200-order warm-up and a `VACUUM ANALYZE`, one run per cell.
Databases are `shared` (one PostgreSQL instance with a schema per module) or `split` (every deployable unit on its own instance: three for the groups, eight for the services, so
the one-process modes have no split run, and `split-dbs` is always split). A dash means the combination does not exist for that mode.

Because every cell is a single run, differences of about 5% are noise. The other pages use the cells of this table that vary one switch at a time ([cache and parallel quotes](switches.md),
[follow-up delivery](load.md), [the database layout](modes.md#one-database-instance-or-one-per-deployable-unit)).

## Orders per second

--8<-- "performance/_generated/matrix-throughput.md"

## Median order, ms

--8<-- "performance/_generated/matrix-p50.md"

## What the combinations show

* **The mode decides, the switches adjust.** In every combination the order of the modes is the same: `modulith`, `macro-modulith` and `loopback-codec` together at the top, then `loopback-http`, then
  `groups` and `split-dbs`, then the two eight-service modes at the bottom. No setting of any switch changes that.
* **The effects roughly multiply.** In `microservices` the cache is worth +18% and parallel quotes +15%; with both off the throughput is 116 orders/s against 161 with both on, +39%, which is
  about the product of the two. Switching events on costs a further 3 to 7%, so the worst combination is 112 orders/s against 161 for the best (30% lower) in `microservices`, against 387 and 450 (14% lower) in `modulith`.
* **The switches matter most where calls are most expensive.** The worst combination is 14 to 16% below the best in the one-process modes, 22% below in `loopback-http`, 13% below in `groups` and 30 to 31% below with eight services.
* **Outbox against events is a small cost.** Events were behind the outbox in 40 of the 44 pairs of cells, by 4% at the median, within a range of 7% behind to 5% ahead.
* **Splitting the databases changes nothing measurable.** Shared against split is within noise in all three modes that have the choice ([the comparison](modes.md#one-database-instance-or-one-per-deployable-unit)), and in every combination of the other switches.

The raw reports of the 88 runs are in [the raw reports](reports.md).
