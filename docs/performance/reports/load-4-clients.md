# Performance run 20261024T152216Z

- Started: 2026-10-24T15:22:16+00:00
- Commit: 2c801ac (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 3000 orders, 4 concurrent clients (closed loop, persistent connections), 300 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: off (round trips only)
- Command line: `--modes modulith,groups,microservices --no-trace --orders 3000 --warmup 300 --concurrency 4`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith | 371 | 2950 | 50 | 6.1 | 10.8 | 10.5 | 14.2 | 15.5 | 17.7 | 26.5 | - | PASS |
| groups | 243 | 2950 | 50 | 8.9 | 16.5 | 16.0 | 22.1 | 23.9 | 26.8 | 32.7 | - | PASS |
| microservices | 192 | 2950 | 50 | 10.1 | 20.9 | 20.0 | 28.3 | 30.9 | 35.3 | 44.2 | - | PASS |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache on, parallel quotes on, async outbox)

3000 orders in 8.1 s = 370.8 orders/s; 2950 ok, 50 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 2950 | 6.1 | 10.8 | 10.5 | 14.2 | 15.5 | 17.7 | 26.5 |
| placeOrder | declined | 50 | 4.7 | 7.5 | 6.9 | 10.4 | 11.5 | 14.1 | 14.1 |

Raw samples: `.demo/perf/20261024T152216Z/modulith` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

3000 orders in 12.4 s = 242.5 orders/s; 2950 ok, 50 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 2950 | 8.9 | 16.5 | 16.0 | 22.1 | 23.9 | 26.8 | 32.7 |
| placeOrder | declined | 50 | 6.0 | 12.2 | 11.0 | 18.6 | 20.6 | 24.4 | 24.4 |

Raw samples: `.demo/perf/20261024T152216Z/groups` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

3000 orders in 15.7 s = 191.6 orders/s; 2950 ok, 50 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 2950 | 10.1 | 20.9 | 20.0 | 28.3 | 30.9 | 35.3 | 44.2 |
| placeOrder | declined | 50 | 7.6 | 15.6 | 14.2 | 23.2 | 25.3 | 28.9 | 28.9 |

Raw samples: `.demo/perf/20261024T152216Z/microservices` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
