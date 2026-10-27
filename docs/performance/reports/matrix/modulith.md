# Performance run 20261027T172012Z

- Started: 2026-10-27T17:20:12+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes modulith --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/modulith`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith cache off parallel off async outbox | 408 | 1473 | 27 | 18.0 | 39.0 | 37.7 | 46.6 | 51.4 | 59.5 | 68.9 | 19.9 | PASS |
| modulith cache off parallel off async events | 387 | 1473 | 27 | 21.5 | 41.2 | 39.8 | 49.5 | 55.8 | 64.5 | 75.2 | 19.9 | PASS |
| modulith cache off parallel on async outbox | 430 | 1473 | 27 | 16.7 | 37.1 | 35.9 | 43.5 | 46.9 | 53.7 | 63.2 | 19.9 | PASS |
| modulith cache off parallel on async events | 401 | 1473 | 27 | 19.7 | 39.7 | 37.9 | 49.2 | 53.9 | 61.7 | 78.3 | 19.9 | PASS |
| modulith cache on parallel off async outbox | 446 | 1473 | 27 | 16.7 | 35.7 | 34.3 | 43.3 | 48.0 | 51.9 | 55.5 | 19.9 | PASS |
| modulith cache on parallel off async events | 429 | 1473 | 27 | 19.4 | 37.1 | 35.7 | 44.2 | 48.9 | 56.7 | 67.5 | 19.9 | PASS |
| modulith cache on parallel on async outbox | 450 | 1473 | 27 | 15.8 | 35.4 | 34.1 | 42.5 | 46.8 | 56.2 | 59.8 | 19.9 | PASS |
| modulith cache on parallel on async events | 433 | 1473 | 27 | 20.7 | 36.8 | 35.0 | 46.2 | 49.0 | 59.9 | 69.4 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (parallel off) (async outbox) | 408 | 446 | +9% | 37.7 | 34.3 | -9% | 51.4 | 48.0 | -7% | 59.5 | 51.9 | -13% |
| modulith (parallel off) (async events) | 387 | 429 | +11% | 39.8 | 35.7 | -10% | 55.8 | 48.9 | -12% | 64.5 | 56.7 | -12% |
| modulith (parallel on) (async outbox) | 430 | 450 | +5% | 35.9 | 34.1 | -5% | 46.9 | 46.8 | -0% | 53.7 | 56.2 | +5% |
| modulith (parallel on) (async events) | 401 | 433 | +8% | 37.9 | 35.0 | -7% | 53.9 | 49.0 | -9% | 61.7 | 59.9 | -3% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (cache off) (async outbox) | 408 | 430 | +5% | 37.7 | 35.9 | -5% | 51.4 | 46.9 | -9% | 59.5 | 53.7 | -10% |
| modulith (cache off) (async events) | 387 | 401 | +4% | 39.8 | 37.9 | -5% | 55.8 | 53.9 | -3% | 64.5 | 61.7 | -4% |
| modulith (cache on) (async outbox) | 446 | 450 | +1% | 34.3 | 34.1 | -1% | 48.0 | 46.8 | -2% | 51.9 | 56.2 | +8% |
| modulith (cache on) (async events) | 429 | 433 | +1% | 35.7 | 35.0 | -2% | 48.9 | 49.0 | +0% | 56.7 | 59.9 | +6% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (cache off) (parallel off) | 408 | 387 | -5% | 37.7 | 39.8 | +6% | 51.4 | 55.8 | +9% | 59.5 | 64.5 | +8% |
| modulith (cache off) (parallel on) | 430 | 401 | -7% | 35.9 | 37.9 | +6% | 46.9 | 53.9 | +15% | 53.7 | 61.7 | +15% |
| modulith (cache on) (parallel off) | 446 | 429 | -4% | 34.3 | 35.7 | +4% | 48.0 | 48.9 | +2% | 51.9 | 56.7 | +9% |
| modulith (cache on) (parallel on) | 450 | 433 | -4% | 34.1 | 35.0 | +3% | 46.8 | 49.0 | +5% | 56.2 | 59.9 | +7% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off parallel off async outbox | modulith cache off parallel off async events | modulith cache off parallel on async outbox | modulith cache off parallel on async events | modulith cache on parallel off async outbox | modulith cache on parallel off async events | modulith cache on parallel on async outbox | modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.2 | 1.2 | 1.3 | 1.3 | 1.1 | 1.1 | 1.1 | 1.2 |
| orders -> inventory.reserve | 1.0 | 1.1 | 1.1 | 1.2 | 1.1 | 1.2 | 1.2 | 1.2 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 0.8 | 0.7 | 0.8 | 0.8 | 0.8 |
| orders -> shipping.quote | 0.8 | 0.9 | 1.0 | 1.1 | 0.5 | 0.5 | 0.7 | 0.7 |
| orders -> notifications.send | 0.6 | 0.8 | 0.6 | 0.9 | 0.6 | 0.7 | 0.6 | 0.7 |
| shipping -> inventory.getStock | 0.3 | 0.3 | 0.4 | 0.4 | 0.2 | 0.2 | 0.2 | 0.2 |
| orders -> pricing.quote | 0.6 | 0.6 | 0.7 | 0.8 | 0.3 | 0.3 | 0.4 | 0.5 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| shipping -> inventory.getReservation | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| inventory -> catalog.productExists | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct | 0.1 | 0.1 | 0.2 | 0.2 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer | 0.1 | 0.1 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod | 0.1 | 0.1 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer | 0.1 | 0.1 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer | 0.1 | 0.2 | 0.1 | 0.2 | 0.1 | 0.0 | 0.1 | 0.0 |
| orders -> inventory.release | 1.3 | 1.1 | 1.2 | 1.2 | 1.3 | 1.2 | 1.2 | 1.2 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off parallel off async outbox | modulith cache off parallel off async events | modulith cache off parallel on async outbox | modulith cache off parallel on async events | modulith cache on parallel off async outbox | modulith cache on parallel off async events | modulith cache on parallel on async outbox | modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.6 | 1.7 | 1.7 | 1.9 | 1.5 | 1.6 | 1.6 | 1.6 |
| orders -> inventory.reserve | 1.5 | 1.7 | 1.7 | 1.9 | 1.7 | 1.7 | 1.8 | 1.8 |
| orders -> payments.authorize | 1.0 | 1.0 | 1.0 | 1.1 | 1.0 | 1.1 | 1.0 | 1.1 |
| orders -> shipping.quote | 1.5 | 1.6 | 1.7 | 1.8 | 1.0 | 1.0 | 1.3 | 1.4 |
| orders -> notifications.send | 1.1 | 1.1 | 1.1 | 1.3 | 1.1 | 0.9 | 1.0 | 1.0 |
| shipping -> inventory.getStock | 0.4 | 0.5 | 0.5 | 0.6 | 0.4 | 0.4 | 0.4 | 0.5 |
| orders -> pricing.quote | 1.0 | 1.1 | 1.1 | 1.2 | 0.6 | 0.6 | 0.8 | 0.8 |
| orders -> payments.capture | 0.6 | 0.7 | 0.6 | 0.7 | 0.6 | 0.7 | 0.7 | 0.7 |
| shipping -> inventory.getReservation | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.3 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| shipping -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.3 | 0.0 | 0.0 | 0.2 | 0.2 |
| orders -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.3 | 0.2 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.3 | 0.2 | 0.3 |
| pricing -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| notifications -> customers.getCustomer | 0.3 | 0.2 | 0.3 | 0.3 | 0.2 | 0.0 | 0.2 | 0.0 |
| orders -> inventory.release | 1.9 | 2.3 | 1.6 | 2.0 | 1.6 | 2.7 | 1.6 | 1.8 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith cache off parallel off async outbox | modulith cache off parallel off async events | modulith cache off parallel on async outbox | modulith cache off parallel on async events | modulith cache on parallel off async outbox | modulith cache on parallel off async events | modulith cache on parallel on async outbox | modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.4% | 87.0% | 50.4% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith cache off parallel off async outbox | - | 29919 calls, 0.39 ms | - | - | - |
| modulith cache off parallel off async events | - | 29919 calls, 0.42 ms | - | - | - |
| modulith cache off parallel on async outbox | - | 29919 calls, 0.43 ms | - | - | - |
| modulith cache off parallel on async events | - | 29919 calls, 0.47 ms | - | - | - |
| modulith cache on parallel off async outbox | 10929 calls, 0.01 ms | 18990 calls, 0.47 ms | - | - | - |
| modulith cache on parallel off async events | 11716 calls, 0.01 ms | 18203 calls, 0.51 ms | - | - | - |
| modulith cache on parallel on async outbox | 8993 calls, 0.01 ms | 20926 calls, 0.49 ms | - | - | - |
| modulith cache on parallel on async events | 9779 calls, 0.01 ms | 20140 calls, 0.52 ms | - | - | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache off, parallel quotes off, async outbox, dbs shared)

1500 orders in 3.7 s = 408.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.0 | 39.0 | 37.7 | 46.6 | 51.4 | 59.5 | 68.9 |
| placeOrder | declined | 27 | 29.9 | 35.8 | 34.3 | 38.5 | 41.8 | 60.6 | 60.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.2 | 1.6 | 2.1 | 9.6 |
| orders -> inventory.reserve [local] | 1500 |  | 1.0 | 0.9 | 1.5 | 2.0 | 8.9 |
| orders -> shipping.quote [local] | 1500 |  | 0.8 | 0.7 | 1.5 | 1.9 | 8.6 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.0 | 1.3 | 8.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.6 | 7.9 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.7 | 2.3 |
| orders -> pricing.quote [local] | 1500 |  | 0.6 | 0.6 | 1.0 | 1.3 | 8.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.8 | 5.7 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 7.5 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.7 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.1 | 0.1 | 0.2 | 0.2 | 0.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.0 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.4 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 7.4 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.5 | 0.7 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.3 | 1.9 | 1.9 | 1.9 |

By binding: local 29919 calls, mean 0.39 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache off, parallel quotes off, async events, dbs shared)

1500 orders in 3.9 s = 386.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 21.5 | 41.2 | 39.8 | 49.5 | 55.8 | 64.5 | 75.2 |
| placeOrder | declined | 27 | 29.4 | 36.6 | 36.0 | 41.0 | 43.6 | 53.8 | 53.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.2 | 1.7 | 2.1 | 3.2 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.1 | 3.5 |
| orders -> shipping.quote [local] | 1500 |  | 0.9 | 0.8 | 1.6 | 2.2 | 9.5 |
| orders -> notifications.send [local] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.4 | 7.3 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.2 | 7.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.5 | 0.6 | 8.4 |
| orders -> pricing.quote [local] | 1500 |  | 0.6 | 0.6 | 1.1 | 1.4 | 6.8 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.7 | 7.2 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 8.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.9 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.1 | 0.1 | 0.2 | 0.3 | 3.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 1.4 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.9 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.1 | 0.2 | 0.3 | 6.2 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.5 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.6 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.0 | 2.3 | 2.4 | 2.4 |

By binding: local 29919 calls, mean 0.42 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache off, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.5 s = 429.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.7 | 37.1 | 35.9 | 43.5 | 46.9 | 53.7 | 63.2 |
| placeOrder | declined | 27 | 29.6 | 33.3 | 32.3 | 36.0 | 39.6 | 45.1 | 45.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.0 | 9.0 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.0 | 9.7 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.1 | 8.4 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.2 | 9.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.6 | 5.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.6 | 1.1 | 1.4 | 7.9 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.5 | 1.9 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 0.9 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.8 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 6.7 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 1.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 7.0 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.8 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 7.7 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.4 | 0.6 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.6 | 1.8 | 1.8 |

By binding: local 29919 calls, mean 0.43 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache off, parallel quotes on, async events, dbs shared)

1500 orders in 3.7 s = 401.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 19.7 | 39.7 | 37.9 | 49.2 | 53.9 | 61.7 | 78.3 |
| placeOrder | declined | 27 | 30.4 | 35.2 | 34.4 | 37.3 | 37.9 | 55.8 | 55.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.3 | 1.9 | 2.5 | 12.7 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.9 | 2.4 | 4.4 |
| orders -> shipping.quote [local] | 1500 |  | 1.1 | 1.0 | 1.8 | 2.8 | 9.4 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.5 | 9.2 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.4 | 7.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 7.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.8 | 0.7 | 1.2 | 1.7 | 8.5 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 6.4 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 7.1 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.6 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 7.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 5.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 5.5 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.5 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.0 | 2.0 | 2.9 | 2.9 |

By binding: local 29919 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes off, async outbox, dbs shared)

1500 orders in 3.4 s = 445.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.7 | 35.7 | 34.3 | 43.3 | 48.0 | 51.9 | 55.5 |
| placeOrder | declined | 27 | 28.2 | 32.5 | 31.3 | 35.8 | 40.4 | 49.9 | 49.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5690 | 76.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.2 | 7.9 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.5 | 1.8 | 7.4 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.0 | 1.2 | 2.5 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.1 | 1.7 | 2.2 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.8 | 1.5 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.0 | 1.4 | 7.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 6.5 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.8 | 2.3 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 2.2 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 6.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 |
| notifications -> customers.getCustomer [local] | 787 |  | 0.2 | 0.1 | 0.4 | 0.5 | 1.4 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.9 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.7 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.3 | 1.6 | 2.4 | 2.4 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 10929 calls, mean 0.01 ms, local 18990 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes off, async events, dbs shared)

1500 orders in 3.5 s = 429.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 19.4 | 37.1 | 35.7 | 44.2 | 48.9 | 56.7 | 67.5 |
| placeOrder | declined | 27 | 27.1 | 34.4 | 33.8 | 40.1 | 40.8 | 53.5 | 53.5 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.5 | 12.8 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 2.0 | 2.9 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.4 | 2.5 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 0.9 | 1.2 | 12.0 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 3.3 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.0 | 1.5 | 6.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 5.8 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.9 | 7.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.9 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.8 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.5 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.0 | 2.7 | 3.3 | 3.3 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 11716 calls, mean 0.01 ms, local 18203 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.3 s = 449.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.8 | 35.4 | 34.1 | 42.5 | 46.8 | 56.2 | 59.8 |
| placeOrder | declined | 27 | 27.5 | 32.8 | 30.9 | 41.0 | 42.4 | 54.0 | 54.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3754 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.5 | 12.5 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 1.9 | 8.9 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.0 | 1.2 | 5.6 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.9 | 8.2 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 57.6 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 8.4 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 7.5 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.8 | 1.1 | 7.9 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.7 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 4.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| notifications -> customers.getCustomer [local] | 786 |  | 0.2 | 0.1 | 0.4 | 0.6 | 0.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.8 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 6.2 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.6 | 1.9 | 1.9 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 687 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8993 calls, mean 0.01 ms, local 20926 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async events, dbs shared)

1500 orders in 3.5 s = 432.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.7 | 36.8 | 35.0 | 46.2 | 49.0 | 59.9 | 69.4 |
| placeOrder | declined | 27 | 29.1 | 32.5 | 31.6 | 35.2 | 39.1 | 40.9 | 40.9 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 12.8 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.2 | 10.9 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 12.0 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.1 | 6.8 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.4 | 3.8 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 2.4 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.7 | 6.1 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.8 | 1.4 | 3.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 3.3 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 3.3 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 3.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.3 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 1.8 | 2.5 | 2.5 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.0 |

By binding: cache 9779 calls, mean 0.01 ms, local 20140 calls, mean 0.52 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
