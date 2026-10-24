# Performance run 20261024T152340Z

- Started: 2026-10-24T15:23:40+00:00
- Commit: 2c801ac (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 3000 orders, 16 concurrent clients (closed loop, persistent connections), 300 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: off (round trips only)
- Command line: `--modes modulith,groups,microservices --no-trace --orders 3000 --warmup 300 --concurrency 16`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith | 354 | 2950 | 50 | 15.7 | 45.1 | 45.5 | 50.7 | 52.2 | 55.4 | 66.2 | - | PASS |
| groups | 237 | 2950 | 50 | 19.8 | 67.3 | 66.9 | 73.8 | 75.9 | 80.3 | 92.2 | - | PASS |
| microservices | 188 | 2950 | 50 | 20.2 | 85.0 | 84.1 | 95.7 | 99.0 | 105.1 | 116.1 | - | PASS |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache on, parallel quotes on, async outbox)

3000 orders in 8.5 s = 354.2 orders/s; 2950 ok, 50 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 2950 | 15.7 | 45.1 | 45.5 | 50.7 | 52.2 | 55.4 | 66.2 |
| placeOrder | declined | 50 | 33.2 | 42.7 | 43.5 | 48.5 | 49.7 | 61.1 | 61.1 |

Raw samples: `.demo/perf/20261024T152340Z/modulith` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

3000 orders in 12.6 s = 237.2 orders/s; 2950 ok, 50 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 2950 | 19.8 | 67.3 | 66.9 | 73.8 | 75.9 | 80.3 | 92.2 |
| placeOrder | declined | 50 | 55.8 | 63.4 | 62.2 | 68.9 | 73.1 | 86.0 | 86.0 |

Raw samples: `.demo/perf/20261024T152340Z/groups` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

3000 orders in 16.0 s = 187.9 orders/s; 2950 ok, 50 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 2950 | 20.2 | 85.0 | 84.1 | 95.7 | 99.0 | 105.1 | 116.1 |
| placeOrder | declined | 50 | 71.5 | 80.4 | 78.3 | 88.6 | 96.3 | 103.6 | 103.6 |

Raw samples: `.demo/perf/20261024T152340Z/microservices` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
