# Performance: what each shape costs

All eight [deployment modes](../modes/index.md) run the same code and the same tests. This section measures what the different shapes cost, and what
three optional features win back. Everything is measured on one machine with a deterministic, skewed workload ([method](method.md)).

## The short version

<div class="grid cards" markdown>

-   **The boundary is cheap. The network is not.**

    In one process, going through the API costs nothing measurable. Serialising every call costs 7% of the throughput, an HTTP hop to yourself 29%,
    three processes 33%, eight services about half.

-   **One artifact, no penalty.**

    The configurable macro jar performs like the dedicated assemblies: `macro-monolith` is within 2% of `modulith`, `microservices-macro`
    within noise of `microservices`, and `split-dbs` equals `groups`.

-   **Cost is per call.**

    A call costs about 0.7 ms in process, 1.3 ms over HTTP to yourself and 1.7 to 2.0 ms between processes, and an order makes about 20 of them.

-   **Two features win a good part back.**

    A result cache adds 13 to 26% throughput in the split modes (1 to 10% in one process). Running three independent reads in parallel adds 9 to 18%
    in the modes where calls are remote, and nothing in a single process (+2%).

</div>

## Headline numbers

Four concurrent clients, realistic workload, result cache on, parallel quotes on. Full tables on [the next page](modes.md).

--8<-- "performance/_generated/modes-throughput.md"

| Mode | Orders/s | Share of `modulith` | Median order (p50) |
| --- | ---: | ---: | ---: |
| `modulith` | 336 | 100% | 11.6 ms |
| `macro-monolith` | 331 | 98% | 11.6 ms |
| `loopback-codec` | 314 | 93% | 12.4 ms |
| `loopback-http` | 238 | 71% | 16.3 ms |
| `groups` | 225 | 67% | 17.4 ms |
| `split-dbs` | 230 | 68% | 16.7 ms |
| `microservices` | 166 | 49% | 22.7 ms |
| `microservices-macro` | 178 | 53% | 21.5 ms |

## How to read this, and what to take from it

| Finding | Evidence |
| --- | --- |
| **Splitting costs roughly 1 ms per call between processes, on one machine.** With about 20 calls per order that is the whole difference between 11.6 ms and 22.7 ms. | [per-call latency](modes.md#where-the-time-goes) |
| **Group the chatty parts.** `groups` keeps 40% of the calls that reach a module local and is about 35% faster than eight services (225 against 166 orders/s). | [modes](modes.md) |
| **Use the cache for reference data, and parallel reads where hops are real.** Both are off-switches, so they can be measured on your own workload. | [cache, parallel, delivery](switches.md) |
| **The follow-up mechanism does not affect the request path, but it decides how fast background work drains.** Outbox and events perform alike on an order; the outbox delivered about 20 notifications a second, events delivered immediately. | [load and delivery](load.md#delivery-of-follow-ups) |
| **The system saturates at about four concurrent orders on this machine.** More clients only queue. | [load](load.md#more-clients) |

!!! warning "Treat the absolute figures with care"
    One machine, demo-sized JVMs (320 MB heap, C1-only JIT, serial GC), cheap calls and loopback networking with no TLS, authentication or proxies. Compare the *shapes with each other*; do not
    quote 336 orders per second as a capacity. A real network, TLS and any gateway or mesh make the split shapes cost *more* than shown. The [method page](method.md) lists
    what the measurements do and do not say.
