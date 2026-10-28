# Performance run 20261027T194622Z

- Started: 2026-10-27T19:46:22+00:00
- Commit: a3f7372 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-codec --cache both --parallel both --async both --dbs per-module --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/loopback-codec-8db`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec cache off parallel off async outbox | 366 | 1473 | 27 | 16.6 | 43.5 | 42.3 | 50.8 | 56.9 | 64.0 | 67.3 | 19.9 | PASS |
| loopback-codec cache off parallel off async events | 358 | 1473 | 27 | 18.4 | 44.5 | 43.0 | 51.9 | 57.2 | 95.3 | 100.3 | 19.9 | PASS |
| loopback-codec cache off parallel on async outbox | 399 | 1473 | 27 | 16.1 | 39.9 | 38.3 | 47.3 | 50.8 | 90.6 | 97.3 | 19.9 | PASS |
| loopback-codec cache off parallel on async events | 351 | 1473 | 27 | 18.9 | 45.4 | 43.8 | 52.2 | 56.4 | 103.3 | 108.6 | 19.9 | PASS |
| loopback-codec cache on parallel off async outbox | 399 | 1473 | 27 | 17.6 | 39.9 | 38.0 | 50.0 | 54.6 | 60.8 | 67.4 | 19.9 | PASS |
| loopback-codec cache on parallel off async events | 400 | 1473 | 27 | 18.8 | 39.8 | 36.9 | 53.4 | 57.6 | 90.3 | 95.3 | 19.9 | PASS |
| loopback-codec cache on parallel on async outbox | 405 | 1473 | 27 | 15.2 | 39.4 | 36.0 | 52.7 | 57.6 | 86.1 | 91.1 | 19.9 | PASS |
| loopback-codec cache on parallel on async events | 406 | 1473 | 27 | 16.6 | 39.2 | 36.3 | 51.7 | 57.0 | 94.5 | 100.0 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec (parallel off) (async outbox) | 366 | 399 | +9% | 42.3 | 38.0 | -10% | 56.9 | 54.6 | -4% | 64.0 | 60.8 | -5% |
| loopback-codec (parallel off) (async events) | 358 | 400 | +12% | 43.0 | 36.9 | -14% | 57.2 | 57.6 | +1% | 95.3 | 90.3 | -5% |
| loopback-codec (parallel on) (async outbox) | 399 | 405 | +2% | 38.3 | 36.0 | -6% | 50.8 | 57.6 | +13% | 90.6 | 86.1 | -5% |
| loopback-codec (parallel on) (async events) | 351 | 406 | +16% | 43.8 | 36.3 | -17% | 56.4 | 57.0 | +1% | 103.3 | 94.5 | -9% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec (cache off) (async outbox) | 366 | 399 | +9% | 42.3 | 38.3 | -9% | 56.9 | 50.8 | -11% | 64.0 | 90.6 | +42% |
| loopback-codec (cache off) (async events) | 358 | 351 | -2% | 43.0 | 43.8 | +2% | 57.2 | 56.4 | -1% | 95.3 | 103.3 | +8% |
| loopback-codec (cache on) (async outbox) | 399 | 405 | +1% | 38.0 | 36.0 | -5% | 54.6 | 57.6 | +6% | 60.8 | 86.1 | +41% |
| loopback-codec (cache on) (async events) | 400 | 406 | +1% | 36.9 | 36.3 | -1% | 57.6 | 57.0 | -1% | 90.3 | 94.5 | +5% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec (cache off) (parallel off) | 366 | 358 | -2% | 42.3 | 43.0 | +2% | 56.9 | 57.2 | +1% | 64.0 | 95.3 | +49% |
| loopback-codec (cache off) (parallel on) | 399 | 351 | -12% | 38.3 | 43.8 | +14% | 50.8 | 56.4 | +11% | 90.6 | 103.3 | +14% |
| loopback-codec (cache on) (parallel off) | 399 | 400 | +0% | 38.0 | 36.9 | -3% | 54.6 | 57.6 | +6% | 60.8 | 90.3 | +48% |
| loopback-codec (cache on) (parallel on) | 405 | 406 | +0% | 36.0 | 36.3 | +1% | 57.6 | 57.0 | -1% | 86.1 | 94.5 | +10% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-codec cache off parallel off async outbox | loopback-codec cache off parallel off async events | loopback-codec cache off parallel on async outbox | loopback-codec cache off parallel on async events | loopback-codec cache on parallel off async outbox | loopback-codec cache on parallel off async events | loopback-codec cache on parallel on async outbox | loopback-codec cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.4 | 1.4 | 1.4 | 1.5 | 1.3 | 1.3 | 1.3 | 1.3 |
| orders -> inventory.reserve | 1.2 | 1.1 | 1.3 | 1.4 | 1.2 | 1.1 | 1.3 | 1.2 |
| orders -> shipping.quote | 1.0 | 1.0 | 1.1 | 1.3 | 0.6 | 0.6 | 0.8 | 0.9 |
| orders -> payments.authorize | 0.8 | 0.9 | 0.8 | 0.9 | 0.9 | 0.9 | 0.9 | 0.8 |
| orders -> notifications.send | 0.7 | 0.9 | 0.7 | 1.0 | 0.6 | 0.7 | 0.6 | 0.8 |
| shipping -> inventory.getStock | 0.4 | 0.4 | 0.4 | 0.5 | 0.3 | 0.3 | 0.3 | 0.3 |
| orders -> pricing.quote | 0.7 | 0.8 | 0.8 | 1.0 | 0.4 | 0.4 | 0.6 | 0.6 |
| orders -> payments.capture | 0.5 | 0.6 | 0.6 | 0.6 | 0.6 | 0.6 | 0.6 | 0.6 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.3 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.2 | 0.2 | 0.0 | 0.1 | 0.1 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.3 | 0.0 | 0.0 | 0.2 | 0.2 |
| notifications -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.0 | 0.1 | 0.0 |
| orders -> inventory.release | 1.3 | 1.1 | 1.5 | 1.4 | 1.2 | 1.1 | 1.3 | 1.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-codec cache off parallel off async outbox | loopback-codec cache off parallel off async events | loopback-codec cache off parallel on async outbox | loopback-codec cache off parallel on async events | loopback-codec cache on parallel off async outbox | loopback-codec cache on parallel off async events | loopback-codec cache on parallel on async outbox | loopback-codec cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.8 | 1.8 | 1.8 | 2.0 | 1.8 | 1.8 | 1.9 | 1.9 |
| orders -> inventory.reserve | 1.8 | 1.7 | 1.7 | 2.1 | 1.9 | 1.8 | 2.0 | 1.9 |
| orders -> shipping.quote | 1.7 | 1.7 | 1.9 | 2.2 | 1.3 | 1.3 | 1.7 | 1.8 |
| orders -> payments.authorize | 1.1 | 1.1 | 1.1 | 1.2 | 1.2 | 1.2 | 1.3 | 1.2 |
| orders -> notifications.send | 1.3 | 1.1 | 1.4 | 1.4 | 1.0 | 1.0 | 1.1 | 1.2 |
| shipping -> inventory.getStock | 0.5 | 0.5 | 0.6 | 0.7 | 0.5 | 0.5 | 0.6 | 0.6 |
| orders -> pricing.quote | 1.2 | 1.2 | 1.3 | 1.6 | 0.8 | 0.8 | 1.1 | 1.2 |
| orders -> payments.capture | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.5 | 0.4 | 0.4 | 0.5 | 0.5 |
| inventory -> catalog.productExists | 0.3 | 0.3 | 0.3 | 0.3 | 0.2 | 0.3 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| shipping -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.4 | 0.0 | 0.0 | 0.3 | 0.3 |
| orders -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.4 | 0.3 | 0.4 | 0.4 | 0.5 |
| payments -> customers.getPaymentMethod | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.5 | 0.0 | 0.0 | 0.4 | 0.4 |
| notifications -> customers.getCustomer | 0.4 | 0.3 | 0.4 | 0.4 | 0.3 | 0.0 | 0.3 | 0.0 |
| orders -> inventory.release | 1.8 | 1.8 | 2.3 | 2.4 | 1.7 | 2.0 | 1.8 | 1.8 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-codec cache off parallel off async outbox | loopback-codec cache off parallel off async events | loopback-codec cache off parallel on async outbox | loopback-codec cache off parallel on async events | loopback-codec cache on parallel off async outbox | loopback-codec cache on parallel off async events | loopback-codec cache on parallel on async outbox | loopback-codec cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.6% | 87.0% | 50.6% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-codec cache off parallel off async outbox | - | - | 29919 calls, 0.45 ms | - | - |
| loopback-codec cache off parallel off async events | - | - | 29919 calls, 0.47 ms | - | - |
| loopback-codec cache off parallel on async outbox | - | - | 29919 calls, 0.49 ms | - | - |
| loopback-codec cache off parallel on async events | - | - | 29919 calls, 0.58 ms | - | - |
| loopback-codec cache on parallel off async outbox | 10942 calls, 0.00 ms | - | 18977 calls, 0.54 ms | - | - |
| loopback-codec cache on parallel off async events | 11716 calls, 0.00 ms | - | 18203 calls, 0.56 ms | - | - |
| loopback-codec cache on parallel on async outbox | 9005 calls, 0.00 ms | - | 20914 calls, 0.57 ms | - | - |
| loopback-codec cache on parallel on async events | 9780 calls, 0.00 ms | - | 20139 calls, 0.60 ms | - | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-codec (cache off, parallel quotes off, async outbox, dbs per-module)

1500 orders in 4.1 s = 365.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.6 | 43.5 | 42.3 | 50.8 | 56.9 | 64.0 | 67.3 |
| placeOrder | declined | 27 | 31.8 | 39.4 | 38.7 | 43.4 | 47.2 | 59.2 | 59.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.4 | 1.3 | 1.8 | 2.2 | 7.7 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.2 | 7.3 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.3 | 5.2 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 3.0 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.7 | 0.7 | 1.2 | 1.6 | 6.9 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.7 | 4.4 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.6 | 1.3 | 1.8 | 2.5 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.1 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.8 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 6.4 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.4 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 5.3 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.1 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.3 | 1.2 | 1.8 | 2.2 | 2.2 |

By binding: loopback-codec 29919 calls, mean 0.45 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes off, async events, dbs per-module)

1500 orders in 4.2 s = 358.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.4 | 44.5 | 43.0 | 51.9 | 57.2 | 95.3 | 100.3 |
| placeOrder | declined | 27 | 33.8 | 40.7 | 40.0 | 45.5 | 47.5 | 60.3 | 60.3 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.4 | 1.3 | 1.8 | 2.2 | 5.3 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.1 | 54.4 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.3 | 56.7 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.9 | 0.8 | 1.1 | 1.3 | 12.2 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.9 | 0.8 | 1.1 | 1.4 | 2.5 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.4 | 0.5 | 0.7 | 53.8 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.2 | 1.6 | 3.0 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.5 | 0.7 | 0.8 | 53.5 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.8 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.7 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.7 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.2 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.6 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.7 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.1 | 1.1 | 1.8 | 1.9 | 1.9 |

By binding: loopback-codec 29919 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.8 s = 398.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.1 | 39.9 | 38.3 | 47.3 | 50.8 | 90.6 | 97.3 |
| placeOrder | declined | 27 | 29.1 | 37.5 | 35.0 | 39.2 | 48.4 | 89.5 | 89.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.4 | 1.3 | 1.8 | 2.2 | 3.4 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.3 | 1.1 | 1.7 | 2.2 | 57.6 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.9 | 2.7 | 9.7 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 8.6 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.8 | 0.8 | 1.3 | 1.8 | 8.8 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.8 | 8.0 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.7 | 1.4 | 1.9 | 2.5 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.5 | 0.7 | 0.8 | 55.0 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.3 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.5 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 7.7 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.8 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 4.3 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.9 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.5 | 1.4 | 2.3 | 2.5 | 2.5 |

By binding: loopback-codec 29919 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async events, dbs per-module)

1500 orders in 4.3 s = 350.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.9 | 45.4 | 43.8 | 52.2 | 56.4 | 103.3 | 108.6 |
| placeOrder | declined | 27 | 35.9 | 41.0 | 40.7 | 44.1 | 47.4 | 56.7 | 56.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.5 | 1.5 | 2.0 | 2.7 | 12.4 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.4 | 1.3 | 2.1 | 2.6 | 59.4 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.3 | 1.2 | 2.2 | 3.1 | 59.4 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 1.0 | 0.9 | 1.6 | 2.2 | 59.1 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 1.0 | 0.9 | 1.4 | 1.9 | 12.1 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.9 | 0.9 | 1.2 | 1.5 | 7.5 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.5 | 0.4 | 0.7 | 0.9 | 5.8 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.6 | 0.7 | 0.9 | 58.0 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.4 | 0.6 | 6.3 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.4 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.6 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.5 | 5.5 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.3 | 0.2 | 0.4 | 0.6 | 58.1 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.3 | 0.2 | 0.5 | 0.6 | 6.3 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.2 | 0.4 | 0.6 | 5.4 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.4 | 1.3 | 2.4 | 2.6 | 2.6 |

By binding: loopback-codec 29919 calls, mean 0.58 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes off, async outbox, dbs per-module)

1500 orders in 3.8 s = 399.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.6 | 39.9 | 38.0 | 50.0 | 54.6 | 60.8 | 67.4 |
| placeOrder | declined | 27 | 29.7 | 37.6 | 36.4 | 41.3 | 47.8 | 65.1 | 65.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5703 | 76.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.2 | 1.8 | 2.2 | 11.0 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.9 | 2.4 | 10.6 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.9 | 0.8 | 1.2 | 1.4 | 10.8 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.6 | 0.5 | 1.3 | 1.8 | 9.4 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.6 | 2.5 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.6 | 0.7 | 0.8 | 1.7 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 9.3 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.4 | 0.3 | 0.8 | 1.1 | 10.6 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 9.3 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.9 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 8.8 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.0 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| notifications -> customers.getCustomer [loopback-codec] | 774 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.6 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.2 | 1.7 | 1.8 | 1.8 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 699 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10942 calls, mean 0.00 ms, loopback-codec 18977 calls, mean 0.54 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes off, async events, dbs per-module)

1500 orders in 3.7 s = 400.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.8 | 39.8 | 36.9 | 53.4 | 57.6 | 90.3 | 95.3 |
| placeOrder | declined | 27 | 28.4 | 35.9 | 34.4 | 43.2 | 46.2 | 52.6 | 52.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 6477 | 87.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.1 | 1.8 | 2.3 | 53.1 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.8 | 2.4 | 4.0 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.9 | 0.8 | 1.2 | 1.6 | 52.4 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.3 | 8.9 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.6 | 0.5 | 1.3 | 2.0 | 10.3 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.5 | 0.7 | 0.9 | 9.2 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 9.1 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.4 | 0.3 | 0.8 | 1.1 | 8.4 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.6 | 51.5 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.5 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.9 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 6.4 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 8.5 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.1 | 1.0 | 2.0 | 2.6 | 2.6 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 11716 calls, mean 0.00 ms, loopback-codec 18203 calls, mean 0.56 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.7 s = 404.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.2 | 39.4 | 36.0 | 52.7 | 57.6 | 86.1 | 91.1 |
| placeOrder | declined | 27 | 29.3 | 36.1 | 34.5 | 44.1 | 54.0 | 55.6 | 55.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3766 | 50.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.2 | 1.9 | 2.7 | 51.6 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.3 | 1.2 | 2.0 | 2.9 | 5.8 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.9 | 0.8 | 1.3 | 1.7 | 8.2 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.7 | 2.6 | 9.0 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.7 | 4.0 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.8 | 7.9 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.6 | 0.5 | 1.1 | 1.8 | 8.1 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.5 | 0.7 | 0.9 | 2.5 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.6 | 49.9 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.3 | 0.2 | 0.4 | 1.0 | 7.4 |
| pricing -> customers.getCustomer [loopback-codec] | 968 |  | 0.3 | 0.2 | 0.4 | 0.8 | 7.7 |
| shipping -> customers.getCustomer [loopback-codec] | 969 |  | 0.3 | 0.2 | 0.4 | 0.7 | 7.3 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.0 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.3 | 0.2 | 0.4 | 0.5 | 7.4 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 7.4 |
| notifications -> customers.getCustomer [loopback-codec] | 774 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.3 | 1.2 | 1.8 | 2.0 | 2.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 699 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9005 calls, mean 0.00 ms, loopback-codec 20914 calls, mean 0.57 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async events, dbs per-module)

1500 orders in 3.7 s = 405.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.6 | 39.2 | 36.3 | 51.7 | 57.0 | 94.5 | 100.0 |
| placeOrder | declined | 27 | 29.8 | 37.5 | 33.4 | 47.6 | 54.0 | 96.1 | 96.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4541 | 61.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.2 | 1.9 | 2.4 | 59.1 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.2 | 1.9 | 2.5 | 6.7 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.9 | 0.7 | 1.8 | 2.5 | 59.9 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.2 | 1.7 | 3.4 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.8 | 0.7 | 1.2 | 1.7 | 59.7 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.6 | 0.5 | 1.2 | 1.7 | 59.0 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.9 | 58.2 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.5 | 0.7 | 0.9 | 2.1 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.2 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.3 | 0.2 | 0.5 | 0.8 | 1.1 |
| pricing -> customers.getCustomer [loopback-codec] | 968 |  | 0.3 | 0.2 | 0.5 | 0.9 | 1.2 |
| shipping -> customers.getCustomer [loopback-codec] | 968 |  | 0.3 | 0.2 | 0.5 | 0.9 | 1.4 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.3 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 1.0 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 1.2 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.1 | 1.0 | 1.8 | 2.5 | 2.5 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9780 calls, mean 0.00 ms, loopback-codec 20139 calls, mean 0.60 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
