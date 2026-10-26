# Performance: what each shape costs

All eight [deployment modes](../modes/index.md) run the same code and the same tests. This section measures what the different shapes cost, and what
three optional features win back. Everything is measured on one machine with a deterministic, skewed workload ([method](method.md)).

## The short version

<div class="grid cards" markdown>

-   **The boundary is cheap. The network is not.**

    In one process, going through the API costs nothing measurable. Serialising every call costs 3% of the throughput, an HTTP hop to yourself 29%,
    three processes 42%, eight services 62%.

-   **One artifact, no penalty.**

    The configurable macro jar performs like the dedicated assemblies: `macro-modulith` is within 1% of `modulith`, `microservices-macro`
    within noise of `microservices`, and `split-dbs` equals `groups`.

-   **Cost is per call.**

    A call costs about 0.7 ms in process, 1.3 ms over HTTP to yourself and 1.7 to 2.0 ms between processes, and an order makes about 20 of them.

-   **Two features win a good part back.**

    A result cache adds 13 to 26% throughput in the split modes (1 to 10% in one process). Running three independent reads in parallel adds 9 to 18%
    in the modes where calls are remote, and nothing in a single process (+2%).

</div>

## Headline numbers

Realistic workload, result cache on, parallel quotes on, 16 concurrent clients, JVMs with the full C2 JIT. The figures are means over 20 runs per mode: 1 GB and 2 GB heaps, serial and parallel GC, five repetitions each
([how they were run](jvm-settings.md#heap-size-and-collector-repeated)). Heap and collector make no measurable difference to throughput or median, so one row per mode is enough.

| Mode | Orders/s | Share of `modulith` | Median order (p50) | Small JVMs, 4 clients: orders/s, p50 |
| --- | ---: | ---: | ---: | ---: |
| `modulith` | 457 | 100% | 32.5 ms | 336, 11.6 ms |
| `macro-modulith` | 461 | 101% | 32.4 ms | 331, 11.6 ms |
| `loopback-codec` | 444 | 97% | 33.3 ms | 314, 12.4 ms |
| `loopback-http` | 325 | 71% | 45.8 ms | 238, 16.3 ms |
| `groups` | 263 | 58% | 58.4 ms | 225, 17.4 ms |
| `split-dbs` | 266 | 58% | 57.5 ms | 230, 16.7 ms |
| `microservices` | 171 | 37% | 90.4 ms | 166, 22.7 ms |
| `microservices-macro` | 170 | 37% | 90.0 ms | 178, 21.5 ms |

The median is higher than in the right-hand column because 16 clients queue behind each other; it is not the time one order takes alone. The last column is the
[mode comparison](modes.md), run with the demo-sized JVMs (320 MB heap, C1-only JIT, serial GC) and four clients, which is the run the per-call latencies and the call counts come from.

--8<-- "performance/_generated/jvm-matrix-chart-throughput.md"

## How to read this, and what to take from it

| Finding | Evidence |
| --- | --- |
| **Splitting costs roughly 1 ms per call between processes, on one machine.** With about 20 calls per order that is the whole difference between 11.6 ms and 22.7 ms (small JVMs, 4 clients). | [per-call latency](modes.md#where-the-time-goes) |
| **Group the chatty parts.** `groups` keeps 40% of the calls that reach a module local and is about 53% faster than eight services (263 against 171 orders/s). | [modes](modes.md) |
| **Use the cache for reference data, and parallel reads where hops are real.** Both are off-switches, so they can be measured on your own workload. | [cache, parallel, delivery](switches.md) |
| **The follow-up mechanism does not affect the request path, but it decides how fast background work drains.** Outbox and events perform alike on an order; the outbox delivered about 20 notifications a second, events delivered immediately. | [load and delivery](load.md#delivery-of-follow-ups) |
| **The system saturates at about four concurrent orders on this machine.** More clients only queue. | [load](load.md#more-clients) |

!!! warning "Treat the absolute figures with care"
    One machine, 1 or 2 GB JVMs with C2 (the demo's own JVMs are smaller and slower; see [JVM settings](jvm-settings.md)), cheap calls and loopback networking with no TLS, authentication or proxies. Compare the *shapes with each other*; do not
    quote 457 orders per second as a capacity. A real network, TLS and any gateway or mesh make the split shapes cost *more* than shown. The [method page](method.md) lists
    what the measurements do and do not say.
