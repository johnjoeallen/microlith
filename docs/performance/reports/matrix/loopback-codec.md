# Performance run 20261027T173622Z

- Started: 2026-10-27T17:36:22+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-codec --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/loopback-codec`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec cache off parallel off async outbox | 398 | 1473 | 27 | 20.4 | 40.1 | 38.8 | 47.7 | 51.5 | 58.6 | 64.1 | 19.9 | PASS |
| loopback-codec cache off parallel off async events | 377 | 1473 | 27 | 21.3 | 42.2 | 40.2 | 49.5 | 55.8 | 90.7 | 98.9 | 19.9 | PASS |
| loopback-codec cache off parallel on async outbox | 429 | 1473 | 27 | 15.5 | 37.1 | 36.0 | 43.1 | 46.7 | 50.9 | 56.6 | 19.9 | PASS |
| loopback-codec cache off parallel on async events | 402 | 1473 | 27 | 18.2 | 39.6 | 37.7 | 47.4 | 52.5 | 84.5 | 93.7 | 19.9 | PASS |
| loopback-codec cache on parallel off async outbox | 449 | 1473 | 27 | 15.9 | 35.5 | 34.0 | 43.0 | 46.3 | 53.2 | 60.7 | 19.9 | PASS |
| loopback-codec cache on parallel off async events | 419 | 1473 | 27 | 20.4 | 38.0 | 35.8 | 47.1 | 50.8 | 83.4 | 87.8 | 19.9 | PASS |
| loopback-codec cache on parallel on async outbox | 449 | 1473 | 27 | 17.2 | 35.4 | 33.3 | 44.0 | 48.1 | 83.2 | 89.9 | 19.9 | PASS |
| loopback-codec cache on parallel on async events | 425 | 1473 | 27 | 19.1 | 37.5 | 35.1 | 47.3 | 51.2 | 84.0 | 88.3 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec (parallel off) (async outbox) | 398 | 449 | +13% | 38.8 | 34.0 | -12% | 51.5 | 46.3 | -10% | 58.6 | 53.2 | -9% |
| loopback-codec (parallel off) (async events) | 377 | 419 | +11% | 40.2 | 35.8 | -11% | 55.8 | 50.8 | -9% | 90.7 | 83.4 | -8% |
| loopback-codec (parallel on) (async outbox) | 429 | 449 | +5% | 36.0 | 33.3 | -8% | 46.7 | 48.1 | +3% | 50.9 | 83.2 | +64% |
| loopback-codec (parallel on) (async events) | 402 | 425 | +6% | 37.7 | 35.1 | -7% | 52.5 | 51.2 | -2% | 84.5 | 84.0 | -1% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec (cache off) (async outbox) | 398 | 429 | +8% | 38.8 | 36.0 | -7% | 51.5 | 46.7 | -9% | 58.6 | 50.9 | -13% |
| loopback-codec (cache off) (async events) | 377 | 402 | +7% | 40.2 | 37.7 | -6% | 55.8 | 52.5 | -6% | 90.7 | 84.5 | -7% |
| loopback-codec (cache on) (async outbox) | 449 | 449 | +0% | 34.0 | 33.3 | -2% | 46.3 | 48.1 | +4% | 53.2 | 83.2 | +56% |
| loopback-codec (cache on) (async events) | 419 | 425 | +1% | 35.8 | 35.1 | -2% | 50.8 | 51.2 | +1% | 83.4 | 84.0 | +1% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-codec (cache off) (parallel off) | 398 | 377 | -5% | 38.8 | 40.2 | +4% | 51.5 | 55.8 | +8% | 58.6 | 90.7 | +55% |
| loopback-codec (cache off) (parallel on) | 429 | 402 | -6% | 36.0 | 37.7 | +5% | 46.7 | 52.5 | +12% | 50.9 | 84.5 | +66% |
| loopback-codec (cache on) (parallel off) | 449 | 419 | -7% | 34.0 | 35.8 | +5% | 46.3 | 50.8 | +10% | 53.2 | 83.4 | +57% |
| loopback-codec (cache on) (parallel on) | 449 | 425 | -5% | 33.3 | 35.1 | +6% | 48.1 | 51.2 | +7% | 83.2 | 84.0 | +1% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-codec cache off parallel off async outbox | loopback-codec cache off parallel off async events | loopback-codec cache off parallel on async outbox | loopback-codec cache off parallel on async events | loopback-codec cache on parallel off async outbox | loopback-codec cache on parallel off async events | loopback-codec cache on parallel on async outbox | loopback-codec cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.2 | 1.3 | 1.3 | 1.4 | 1.1 | 1.2 | 1.1 | 1.2 |
| orders -> inventory.reserve | 1.1 | 1.2 | 1.1 | 1.2 | 1.1 | 1.2 | 1.1 | 1.2 |
| orders -> shipping.quote | 0.9 | 0.9 | 1.0 | 1.1 | 0.5 | 0.5 | 0.8 | 0.8 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 0.8 | 0.7 | 0.8 | 0.8 | 0.8 |
| orders -> notifications.send | 0.7 | 0.8 | 0.7 | 0.9 | 0.6 | 0.7 | 0.6 | 0.7 |
| shipping -> inventory.getStock | 0.3 | 0.3 | 0.4 | 0.4 | 0.2 | 0.2 | 0.3 | 0.3 |
| orders -> pricing.quote | 0.7 | 0.7 | 0.7 | 0.8 | 0.3 | 0.3 | 0.5 | 0.5 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.2 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.0 | 0.1 | 0.0 |
| orders -> inventory.release | 1.2 | 1.1 | 1.3 | 1.2 | 1.3 | 1.1 | 1.2 | 3.0 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-codec cache off parallel off async outbox | loopback-codec cache off parallel off async events | loopback-codec cache off parallel on async outbox | loopback-codec cache off parallel on async events | loopback-codec cache on parallel off async outbox | loopback-codec cache on parallel off async events | loopback-codec cache on parallel on async outbox | loopback-codec cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.6 | 1.7 | 1.6 | 1.8 | 1.5 | 1.6 | 1.6 | 1.7 |
| orders -> inventory.reserve | 1.6 | 1.7 | 1.7 | 1.8 | 1.7 | 1.7 | 1.7 | 1.8 |
| orders -> shipping.quote | 1.5 | 1.6 | 1.7 | 1.8 | 1.1 | 1.1 | 1.4 | 1.5 |
| orders -> payments.authorize | 1.0 | 1.0 | 1.0 | 1.1 | 1.0 | 1.1 | 1.1 | 1.2 |
| orders -> notifications.send | 1.1 | 1.1 | 1.2 | 1.2 | 1.0 | 0.9 | 1.0 | 1.0 |
| shipping -> inventory.getStock | 0.5 | 0.5 | 0.5 | 0.6 | 0.4 | 0.4 | 0.5 | 0.5 |
| orders -> pricing.quote | 1.0 | 1.1 | 1.2 | 1.3 | 0.6 | 0.7 | 0.9 | 0.9 |
| orders -> payments.capture | 0.6 | 0.7 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.3 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.3 | 0.3 | 0.2 | 0.2 | 0.2 | 0.3 |
| shipping -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| orders -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| notifications -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.0 | 0.3 | 0.0 |
| orders -> inventory.release | 1.6 | 1.5 | 1.8 | 2.2 | 1.8 | 1.8 | 1.9 | 2.0 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-codec cache off parallel off async outbox | loopback-codec cache off parallel off async events | loopback-codec cache off parallel on async outbox | loopback-codec cache off parallel on async events | loopback-codec cache on parallel off async outbox | loopback-codec cache on parallel off async events | loopback-codec cache on parallel on async outbox | loopback-codec cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.4% | 87.0% | 50.4% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-codec cache off parallel off async outbox | - | - | 29919 calls, 0.41 ms | - | - |
| loopback-codec cache off parallel off async events | - | - | 29919 calls, 0.45 ms | - | - |
| loopback-codec cache off parallel on async outbox | - | - | 29919 calls, 0.44 ms | - | - |
| loopback-codec cache off parallel on async events | - | - | 29919 calls, 0.48 ms | - | - |
| loopback-codec cache on parallel off async outbox | 10930 calls, 0.00 ms | - | 18989 calls, 0.48 ms | - | - |
| loopback-codec cache on parallel off async events | 11716 calls, 0.00 ms | - | 18203 calls, 0.53 ms | - | - |
| loopback-codec cache on parallel on async outbox | 8992 calls, 0.00 ms | - | 20927 calls, 0.50 ms | - | - |
| loopback-codec cache on parallel on async events | 9779 calls, 0.00 ms | - | 20140 calls, 0.55 ms | - | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-codec (cache off, parallel quotes off, async outbox, dbs shared)

1500 orders in 3.8 s = 397.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.4 | 40.1 | 38.8 | 47.7 | 51.5 | 58.6 | 64.1 |
| placeOrder | declined | 27 | 29.2 | 36.2 | 35.4 | 41.9 | 46.3 | 54.2 | 54.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.2 | 1.6 | 2.0 | 4.5 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.6 | 2.0 | 7.9 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.9 | 0.8 | 1.5 | 2.0 | 2.9 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.2 | 5.8 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.7 | 0.6 | 1.0 | 1.4 | 6.5 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.5 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.6 | 1.1 | 1.6 | 2.1 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 1.1 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 5.7 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.6 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.5 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.1 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 0.4 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 1.7 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.1 | 0.3 | 0.4 | 0.8 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.1 | 1.6 | 1.8 | 1.8 |

By binding: loopback-codec 29919 calls, mean 0.41 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes off, async events, dbs shared)

1500 orders in 4.0 s = 377.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 21.3 | 42.2 | 40.2 | 49.5 | 55.8 | 90.7 | 98.9 |
| placeOrder | declined | 27 | 32.8 | 39.5 | 37.3 | 42.0 | 51.7 | 94.8 | 94.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.1 | 7.8 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.2 | 51.9 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.9 | 0.8 | 1.6 | 2.1 | 5.0 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.3 | 51.1 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.2 | 6.8 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.7 | 0.7 | 1.1 | 1.5 | 51.5 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.3 | 0.5 | 0.6 | 3.1 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.7 | 6.8 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 50.3 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 6.5 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 2.7 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 4.4 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.3 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.5 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.5 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.1 | 1.0 | 1.5 | 1.7 | 1.7 |

By binding: loopback-codec 29919 calls, mean 0.45 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.5 s = 429.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.5 | 37.1 | 36.0 | 43.1 | 46.7 | 50.9 | 56.6 |
| placeOrder | declined | 27 | 29.1 | 33.0 | 32.4 | 35.9 | 36.7 | 44.8 | 44.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.2 | 1.6 | 2.0 | 8.7 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.0 | 8.8 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.3 | 8.1 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.1 | 6.8 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.7 | 0.7 | 1.2 | 1.5 | 7.9 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.6 | 1.7 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.6 | 1.2 | 1.7 | 4.2 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 0.9 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.3 | 7.1 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.3 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.5 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 7.2 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 7.2 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.1 | 0.3 | 0.5 | 0.9 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.3 | 1.2 | 1.8 | 1.9 | 1.9 |

By binding: loopback-codec 29919 calls, mean 0.44 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async events, dbs shared)

1500 orders in 3.7 s = 402.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.2 | 39.6 | 37.7 | 47.4 | 52.5 | 84.5 | 93.7 |
| placeOrder | declined | 27 | 31.8 | 36.0 | 34.9 | 39.2 | 43.1 | 54.5 | 54.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.4 | 1.3 | 1.8 | 2.4 | 49.3 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 9.0 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.8 | 2.5 | 9.9 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.9 | 0.8 | 1.2 | 1.4 | 8.8 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 4.6 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.3 | 1.7 | 8.8 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 7.9 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.0 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 7.5 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.6 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 7.6 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 47.3 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.5 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 3.4 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.1 | 2.2 | 2.7 | 2.7 |

By binding: loopback-codec 29919 calls, mean 0.48 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-off-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes off, async outbox, dbs shared)

1500 orders in 3.3 s = 448.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.9 | 35.5 | 34.0 | 43.0 | 46.3 | 53.2 | 60.7 |
| placeOrder | declined | 27 | 27.6 | 33.2 | 32.0 | 37.9 | 38.5 | 53.2 | 53.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5691 | 76.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.1 | 5.3 |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.1 | 1.1 | 1.5 | 1.8 | 7.0 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.7 | 0.7 | 1.0 | 1.2 | 4.5 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.6 | 2.3 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.7 | 6.1 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.5 | 6.2 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 5.7 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.9 | 1.9 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.8 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| notifications -> customers.getCustomer [loopback-codec] | 786 |  | 0.2 | 0.1 | 0.4 | 0.5 | 1.2 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.8 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.3 | 1.1 | 1.8 | 2.0 | 2.0 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 687 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10930 calls, mean 0.00 ms, loopback-codec 18989 calls, mean 0.48 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes off, async events, dbs shared)

1500 orders in 3.6 s = 418.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.4 | 38.0 | 35.8 | 47.1 | 50.8 | 83.4 | 87.8 |
| placeOrder | declined | 27 | 29.7 | 34.5 | 32.8 | 40.6 | 41.1 | 46.5 | 46.5 |

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
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.3 | 51.3 |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.0 | 51.8 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 7.0 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.6 | 0.9 | 1.1 | 6.2 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.7 | 6.9 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 5.0 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 6.1 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 50.5 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.3 | 0.2 | 0.7 | 0.9 | 2.4 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.5 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.1 | 1.0 | 1.8 | 1.9 | 1.9 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 11716 calls, mean 0.00 ms, loopback-codec 18203 calls, mean 0.53 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.3 s = 449.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.2 | 35.4 | 33.3 | 44.0 | 48.1 | 83.2 | 89.9 |
| placeOrder | declined | 27 | 27.8 | 31.7 | 30.7 | 38.5 | 39.1 | 41.1 | 41.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3753 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.1 | 1.1 | 1.6 | 1.9 | 3.9 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.1 | 4.1 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.2 | 2.0 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.8 | 0.6 | 1.4 | 2.0 | 50.9 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.6 | 2.0 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 8.0 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.6 | 50.8 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.2 | 7.4 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| shipping -> customers.getCustomer [loopback-codec] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.6 |
| pricing -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| notifications -> customers.getCustomer [loopback-codec] | 787 |  | 0.2 | 0.1 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.2 | 1.9 | 2.2 | 2.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8992 calls, mean 0.00 ms, loopback-codec 20927 calls, mean 0.50 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async events, dbs shared)

1500 orders in 3.5 s = 425.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 19.1 | 37.5 | 35.1 | 47.3 | 51.2 | 84.0 | 88.3 |
| placeOrder | declined | 27 | 28.1 | 33.1 | 32.4 | 38.2 | 41.1 | 45.8 | 45.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4540 | 61.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.4 | 51.0 |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.1 | 1.7 | 2.0 | 5.2 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.2 | 1.4 | 4.7 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.1 | 50.8 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.3 | 6.8 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 50.0 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.4 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.3 | 4.0 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.3 |
| pricing -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.7 |
| shipping -> customers.getCustomer [loopback-codec] | 968 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.8 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| orders -> inventory.release [loopback-codec] | 27 |  | 3.0 | 1.0 | 2.0 | 52.2 | 52.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9779 calls, mean 0.00 ms, loopback-codec 20140 calls, mean 0.55 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-cache-on-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
