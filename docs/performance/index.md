# Performance: what each shape costs

All eight [deployment modes](../modes/index.md) run the same code and the same tests. This section measures what the different shapes cost, and what
three optional features win back. Everything is measured on one machine with a deterministic, skewed workload ([method](method.md)).

## The short version

<div class="grid cards" markdown>

-   **The boundary is cheap. The network is not.**

    In one process, going through the API costs nothing measurable. Serialising every call costs 2% of the throughput, an HTTP hop to yourself 30%,
    three processes 44%, eight services 64%.

-   **One artifact, no penalty.**

    The configurable macro jar performs like the dedicated assemblies: `macro-modulith` is within 1% of `modulith`, `microservices-macro`
    within noise of `microservices`, and `split-dbs` equals `groups`.

-   **Cost is per call.**

    A call costs about 0.5 ms in process, 0.9 ms over HTTP to yourself and 1.5 to 2.2 ms between processes, and an order makes about 20 of them.

-   **Two features win a good part back.**

    A result cache adds 5 to 18% throughput in the split modes (3 to 5% in one process). Running three independent reads in parallel adds 4 to 16%
    in the modes where calls are remote, and nothing in a single process.

</div>

## Headline numbers

Realistic workload, result cache on, parallel quotes on, 16 concurrent clients, 1,500 measured orders after 200 warm-up orders and a `VACUUM ANALYZE`, each JVM with `-Xmx2g` and the parallel collector.
The figures are means over 5 runs per mode. Full tables on [the next page](modes.md).

--8<-- "performance/_generated/headline-chart-throughput.md"

--8<-- "performance/_generated/headline-table.md"

The median is not the time one order takes alone: with 16 clients that each wait for their answer, the orders queue behind each other, so the median is roughly *clients ÷ throughput*.

## How to read this, and what to take from it

| Finding | Evidence |
| --- | --- |
| **Splitting costs roughly 1 to 1.7 ms per call between processes, on one machine.** With about 20 calls per order that is most of the difference in time per order between one process and eight services. | [per-call latency](modes.md#where-the-time-goes) |
| **Group the chatty parts.** `groups` keeps 40% of the calls that reach a module local and is about 55% faster than eight services (247 against 159 orders/s). | [modes](modes.md) |
| **Use the cache for reference data, and parallel reads where hops are real.** Both are off-switches, so they can be measured on your own workload. | [cache, parallel quotes](switches.md), [every combination](matrix.md) |
| **The follow-up mechanism does not affect the request path, but it decides how fast background work drains.** Outbox and events perform alike on an order; the outbox delivered about 20 notifications a second, events delivered immediately. | [delivery speed](load.md) |

!!! warning "Treat the absolute figures with care"
    One machine, 2 GB JVMs (see [JVM settings](jvm-settings.md) for how that default was chosen), cheap calls and loopback networking with no TLS, authentication or proxies. Compare the *shapes with each other*; do not
    quote 445 orders per second as a capacity. A real network, TLS and any gateway or mesh make the split shapes cost *more* than shown. The [method page](method.md) lists
    what the measurements do and do not say.
