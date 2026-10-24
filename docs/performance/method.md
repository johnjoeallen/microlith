# Method

How the numbers on these pages are produced, and how far to trust them.

## The machine

Everything runs on **one machine**: 32 CPUs, Linux 6.12, JDK 25, PostgreSQL 17 in Docker, the load generator and all application processes together.
Each JVM is limited to `-Xmx320m` with the serial GC, so that eight of them fit comfortably.

!!! warning "What a single machine means"
    A hop between processes here is a **loopback** TCP connection on one host, typically 1 to 3 ms. There is **no TLS or mTLS, no authentication on each call, no load balancer, gateway or
    service mesh, no service discovery, no real network latency or loss, and no cross-zone traffic**. A real deployment has most of these, so the *cost of splitting* shown here is a
    **lower bound** for its network and security part. Client and servers also compete for the same CPUs, which flattens differences once the machine is busy.

## The workload

`scripts/perf.sh` drives `orders.placeOrder` with the **`realistic`** profile:

| Aspect | Value |
| --- | --- |
| Customers | 10,000, popularity skewed (Zipf, exponent 0.8) so a few customers order often |
| Products | 2,000, popularity skewed (Zipf, exponent 1.0) |
| Lines per order | 1 to 4 (mean 2.0), 1 to 3 units per line |
| Customer mix | 8% gold tier, 2% whose payment is always declined |
| Order stream | deterministic for a given seed, whatever the thread timing |

Skew matters more than size: real traffic reuses a few customers and products, and that reuse is what a cache can exploit. Declined orders are an
expected business outcome; they are counted and shown separately, and excluded from the latency figures.

## The load

* **Closed loop:** each of *N* concurrent clients sends its next order as soon as the previous one returns. *N* is 4 unless stated otherwise.
* **Persistent connections**, so connection set-up is not part of the measured round trip.
* **Warm-up:** 200 to 400 orders, not measured, so JIT compilation and connection pools are settled and the cache has seen its popular keys.
* **Measured run:** 1,500 orders (4,000 for the load test).
* **Clean database every run:** `demo.sh start` wipes and re-initialises PostgreSQL, the driver bulk-loads the customers, products and stock, and
  every start is from the same state.

## What is measured

Two layers, from the same run:

1. **Client round trip** of each `placeOrder`: orders per second, and min, mean, p50, p90, p95, p99 and max in milliseconds.
2. **Every module-to-module call inside those orders.** Each process records the calls it makes (caller, callee, method, the binding that was
   actually used, duration). The driver reads them back per order and reports count, mean and percentiles per call and per binding. About 20 calls happen inside one order on average (17 for a one-line order), so a single run yields tens of thousands of call samples.

A call served by the [result cache](../features/cache.md) is recorded as its own binding, `cache`, so hits are visible and are not mistaken for fast network calls.

## What it does not tell you

!!! note "Read the figures with these in mind"
    * **One run per configuration.** Run-to-run noise on this machine is about 3 to 5% for throughput and p50. Differences smaller than that are
      noise; p99 and max are noisier still (single outliers of 50 to 100 ms appear in otherwise identical runs).
    * **Call tracing has a cost.** Recording each call adds a little overhead to every configuration equally. `--no-trace` measures round trips only.
    * **It is a demo domain.** Calls are cheap (one primary-key query each), so the network and framework share of the time is larger than it would
      be for services doing real work.
    * **No real network and nothing in the path:** loopback only, no TLS, no authentication, no proxies or mesh. No real data volume, and no failures during the measurement.

## Reproducing it

```bash
scripts/perf.sh --modes all --cache both --orders 1500 --warmup 200 --concurrency 4          # the mode comparison
scripts/perf.sh --modes loopback-http,split-dbs,microservices-macro --parallel both ...      # parallel quotes
scripts/perf.sh --modes modulith,groups,microservices --async both --parallel both ...       # delivery and parallel
scripts/perf.sh --modes modulith,groups,microservices --orders 4000 --warmup 400 --concurrency 16   # more load
```

Each run writes a timestamped markdown report with the modes side by side, plus the raw samples, to `.demo/perf/<timestamp>/`. The tables and charts
on these pages are generated from those saved summaries by `scripts/perf_site.py`, never typed by hand.
