# Performance run 20261027T172817Z

- Started: 2026-10-27T17:28:17+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes macro-modulith --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/macro-modulith`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith cache off parallel off async outbox | 412 | 1473 | 27 | 18.5 | 38.6 | 37.6 | 45.1 | 49.0 | 58.8 | 63.5 | 19.9 | PASS |
| macro-modulith cache off parallel off async events | 382 | 1473 | 27 | 20.3 | 41.7 | 40.0 | 49.8 | 56.4 | 67.3 | 75.0 | 19.9 | PASS |
| macro-modulith cache off parallel on async outbox | 431 | 1473 | 27 | 15.4 | 36.9 | 35.9 | 42.6 | 45.4 | 54.8 | 60.0 | 19.9 | PASS |
| macro-modulith cache off parallel on async events | 404 | 1473 | 27 | 18.4 | 39.4 | 37.7 | 47.3 | 52.4 | 64.1 | 68.7 | 19.9 | PASS |
| macro-modulith cache on parallel off async outbox | 452 | 1473 | 27 | 19.2 | 35.3 | 33.4 | 44.3 | 48.6 | 54.0 | 66.3 | 19.9 | PASS |
| macro-modulith cache on parallel off async events | 432 | 1473 | 27 | 18.8 | 36.8 | 35.2 | 46.4 | 50.7 | 57.3 | 66.4 | 19.9 | PASS |
| macro-modulith cache on parallel on async outbox | 444 | 1473 | 27 | 16.9 | 35.9 | 34.2 | 44.3 | 48.3 | 55.2 | 63.6 | 19.9 | PASS |
| macro-modulith cache on parallel on async events | 449 | 1473 | 27 | 17.0 | 35.5 | 33.9 | 43.9 | 47.6 | 55.9 | 65.4 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith (parallel off) (async outbox) | 412 | 452 | +10% | 37.6 | 33.4 | -11% | 49.0 | 48.6 | -1% | 58.8 | 54.0 | -8% |
| macro-modulith (parallel off) (async events) | 382 | 432 | +13% | 40.0 | 35.2 | -12% | 56.4 | 50.7 | -10% | 67.3 | 57.3 | -15% |
| macro-modulith (parallel on) (async outbox) | 431 | 444 | +3% | 35.9 | 34.2 | -5% | 45.4 | 48.3 | +6% | 54.8 | 55.2 | +1% |
| macro-modulith (parallel on) (async events) | 404 | 449 | +11% | 37.7 | 33.9 | -10% | 52.4 | 47.6 | -9% | 64.1 | 55.9 | -13% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith (cache off) (async outbox) | 412 | 431 | +5% | 37.6 | 35.9 | -4% | 49.0 | 45.4 | -7% | 58.8 | 54.8 | -7% |
| macro-modulith (cache off) (async events) | 382 | 404 | +6% | 40.0 | 37.7 | -6% | 56.4 | 52.4 | -7% | 67.3 | 64.1 | -5% |
| macro-modulith (cache on) (async outbox) | 452 | 444 | -2% | 33.4 | 34.2 | +2% | 48.6 | 48.3 | -1% | 54.0 | 55.2 | +2% |
| macro-modulith (cache on) (async events) | 432 | 449 | +4% | 35.2 | 33.9 | -4% | 50.7 | 47.6 | -6% | 57.3 | 55.9 | -3% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith (cache off) (parallel off) | 412 | 382 | -7% | 37.6 | 40.0 | +6% | 49.0 | 56.4 | +15% | 58.8 | 67.3 | +14% |
| macro-modulith (cache off) (parallel on) | 431 | 404 | -6% | 35.9 | 37.7 | +5% | 45.4 | 52.4 | +15% | 54.8 | 64.1 | +17% |
| macro-modulith (cache on) (parallel off) | 452 | 432 | -4% | 33.4 | 35.2 | +5% | 48.6 | 50.7 | +4% | 54.0 | 57.3 | +6% |
| macro-modulith (cache on) (parallel on) | 444 | 449 | +1% | 34.2 | 33.9 | -1% | 48.3 | 47.6 | -1% | 55.2 | 55.9 | +1% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | macro-modulith cache off parallel off async outbox | macro-modulith cache off parallel off async events | macro-modulith cache off parallel on async outbox | macro-modulith cache off parallel on async events | macro-modulith cache on parallel off async outbox | macro-modulith cache on parallel off async events | macro-modulith cache on parallel on async outbox | macro-modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.2 | 1.3 | 1.2 | 1.3 | 1.1 | 1.1 | 1.2 | 1.1 |
| orders -> inventory.reserve | 1.1 | 1.2 | 1.1 | 1.2 | 1.0 | 1.1 | 1.2 | 1.2 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 0.8 | 0.7 | 0.8 | 0.8 | 0.8 |
| orders -> shipping.quote | 0.8 | 0.9 | 1.0 | 1.1 | 0.5 | 0.5 | 0.7 | 0.7 |
| orders -> notifications.send | 0.6 | 0.8 | 0.7 | 0.9 | 0.6 | 0.7 | 0.6 | 0.7 |
| shipping -> inventory.getStock | 0.3 | 0.3 | 0.4 | 0.4 | 0.2 | 0.2 | 0.2 | 0.2 |
| orders -> pricing.quote | 0.6 | 0.7 | 0.7 | 0.8 | 0.3 | 0.3 | 0.4 | 0.4 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.2 | 0.3 | 0.3 | 0.3 |
| inventory -> catalog.productExists | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.0 |
| pricing -> catalog.getProduct | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.1 | 0.1 | 0.0 |
| shipping -> customers.getCustomer | 0.1 | 0.1 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod | 0.1 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer | 0.1 | 0.2 | 0.1 | 0.2 | 0.1 | 0.0 | 0.1 | 0.0 |
| orders -> inventory.release | 1.3 | 1.4 | 1.3 | 1.2 | 1.2 | 1.1 | 1.1 | 1.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | macro-modulith cache off parallel off async outbox | macro-modulith cache off parallel off async events | macro-modulith cache off parallel on async outbox | macro-modulith cache off parallel on async events | macro-modulith cache on parallel off async outbox | macro-modulith cache on parallel off async events | macro-modulith cache on parallel on async outbox | macro-modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.5 | 1.7 | 1.6 | 1.8 | 1.5 | 1.6 | 1.6 | 1.5 |
| orders -> inventory.reserve | 1.6 | 1.8 | 1.7 | 1.8 | 1.6 | 1.7 | 1.8 | 1.8 |
| orders -> payments.authorize | 1.0 | 1.0 | 1.0 | 1.1 | 1.0 | 1.0 | 1.1 | 1.1 |
| orders -> shipping.quote | 1.4 | 1.6 | 1.7 | 1.9 | 1.0 | 1.1 | 1.4 | 1.3 |
| orders -> notifications.send | 1.2 | 1.1 | 1.2 | 1.2 | 1.1 | 0.9 | 0.9 | 0.9 |
| shipping -> inventory.getStock | 0.4 | 0.5 | 0.5 | 0.6 | 0.4 | 0.4 | 0.5 | 0.4 |
| orders -> pricing.quote | 0.9 | 1.1 | 1.1 | 1.3 | 0.6 | 0.6 | 0.8 | 0.8 |
| orders -> payments.capture | 0.6 | 0.7 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 |
| shipping -> inventory.getReservation | 0.3 | 0.4 | 0.4 | 0.4 | 0.3 | 0.4 | 0.4 | 0.4 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.3 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| shipping -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.3 | 0.0 | 0.0 | 0.2 | 0.2 |
| orders -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| notifications -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.0 | 0.2 | 0.0 |
| orders -> inventory.release | 2.1 | 1.8 | 2.1 | 1.9 | 1.8 | 2.1 | 1.5 | 1.5 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | macro-modulith cache off parallel off async outbox | macro-modulith cache off parallel off async events | macro-modulith cache off parallel on async outbox | macro-modulith cache off parallel on async events | macro-modulith cache on parallel off async outbox | macro-modulith cache on parallel off async events | macro-modulith cache on parallel on async outbox | macro-modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.4% | 87.0% | 50.4% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| macro-modulith cache off parallel off async outbox | - | 29919 calls, 0.39 ms | - | - | - |
| macro-modulith cache off parallel off async events | - | 29919 calls, 0.43 ms | - | - | - |
| macro-modulith cache off parallel on async outbox | - | 29919 calls, 0.43 ms | - | - | - |
| macro-modulith cache off parallel on async events | - | 29919 calls, 0.47 ms | - | - | - |
| macro-modulith cache on parallel off async outbox | 10930 calls, 0.01 ms | 18989 calls, 0.47 ms | - | - | - |
| macro-modulith cache on parallel off async events | 11716 calls, 0.01 ms | 18203 calls, 0.51 ms | - | - | - |
| macro-modulith cache on parallel on async outbox | 8990 calls, 0.01 ms | 20929 calls, 0.49 ms | - | - | - |
| macro-modulith cache on parallel on async events | 9779 calls, 0.01 ms | 20140 calls, 0.50 ms | - | - | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### macro-modulith (cache off, parallel quotes off, async outbox, dbs shared)

1500 orders in 3.6 s = 411.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.5 | 38.6 | 37.6 | 45.1 | 49.0 | 58.8 | 63.5 |
| placeOrder | declined | 27 | 29.2 | 35.6 | 35.4 | 41.2 | 41.5 | 51.6 | 51.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.5 | 2.0 | 15.7 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.6 | 2.0 | 4.2 |
| orders -> shipping.quote [local] | 1500 |  | 0.8 | 0.7 | 1.4 | 1.8 | 9.8 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.0 | 1.2 | 15.2 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.2 | 1.7 | 2.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.5 | 9.5 |
| orders -> pricing.quote [local] | 1500 |  | 0.6 | 0.5 | 0.9 | 1.2 | 4.8 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 8.2 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.8 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.7 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.1 | 0.1 | 0.2 | 0.2 | 2.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.3 | 0.4 | 6.4 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 6.5 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.5 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.5 | 0.7 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.2 | 0.4 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.2 | 2.1 | 2.2 | 2.2 |

By binding: local 29919 calls, mean 0.39 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes off, async events, dbs shared)

1500 orders in 3.9 s = 382.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.3 | 41.7 | 40.0 | 49.8 | 56.4 | 67.3 | 75.0 |
| placeOrder | declined | 27 | 31.1 | 37.2 | 36.0 | 41.2 | 48.2 | 52.6 | 52.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.2 | 8.4 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.4 | 3.2 |
| orders -> shipping.quote [local] | 1500 |  | 0.9 | 0.8 | 1.6 | 2.1 | 9.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.3 | 7.0 |
| orders -> notifications.send [local] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.4 | 2.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.5 | 0.6 | 8.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.6 | 1.1 | 1.4 | 9.8 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 2.9 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 8.3 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 8.1 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 1.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.1 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.3 | 2.1 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 0.6 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 2.1 |
| orders -> inventory.release [local] | 27 |  | 1.4 | 1.0 | 1.8 | 10.5 | 10.5 |

By binding: local 29919 calls, mean 0.43 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.5 s = 431.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.4 | 36.9 | 35.9 | 42.6 | 45.4 | 54.8 | 60.0 |
| placeOrder | declined | 27 | 28.9 | 33.9 | 32.7 | 37.4 | 39.7 | 56.1 | 56.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.2 | 1.6 | 2.0 | 8.9 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.1 | 7.1 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.2 | 7.3 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.2 | 7.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.6 | 6.3 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.1 | 1.4 | 6.7 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 1.2 | 1.6 | 2.1 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 7.7 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 6.0 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 6.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.9 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.9 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 5.8 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.5 | 0.7 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.1 | 2.1 | 2.2 | 2.2 |

By binding: local 29919 calls, mean 0.43 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes on, async events, dbs shared)

1500 orders in 3.7 s = 403.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.4 | 39.4 | 37.7 | 47.3 | 52.4 | 64.1 | 68.7 |
| placeOrder | declined | 27 | 31.4 | 35.6 | 34.3 | 39.2 | 40.2 | 52.6 | 52.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.8 | 2.2 | 3.2 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.5 | 5.8 |
| orders -> shipping.quote [local] | 1500 |  | 1.1 | 0.9 | 1.9 | 2.8 | 9.4 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.2 | 1.7 | 9.6 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.4 | 5.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.8 | 9.0 |
| orders -> pricing.quote [local] | 1500 |  | 0.8 | 0.7 | 1.3 | 1.9 | 9.2 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 9.1 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 8.5 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.5 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 8.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.8 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.3 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.5 | 8.5 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.1 | 0.3 | 0.3 | 2.4 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 1.9 | 1.9 | 1.9 |

By binding: local 29919 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes off, async outbox, dbs shared)

1500 orders in 3.3 s = 451.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 19.2 | 35.3 | 33.4 | 44.3 | 48.6 | 54.0 | 66.3 |
| placeOrder | declined | 27 | 28.5 | 32.2 | 31.3 | 35.0 | 35.2 | 50.5 | 50.5 |

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
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.0 | 1.5 | 1.8 | 4.8 |
| orders -> inventory.reserve [local] | 1500 |  | 1.0 | 1.0 | 1.6 | 2.2 | 3.8 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.0 | 1.3 | 4.5 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.1 | 1.6 | 2.4 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.7 | 9.5 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.0 | 1.4 | 11.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 9.9 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.8 | 8.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.9 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| notifications -> customers.getCustomer [local] | 786 |  | 0.2 | 0.1 | 0.4 | 0.5 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.8 | 2.1 | 2.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 8.4 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 687 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 10930 calls, mean 0.01 ms, local 18989 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes off, async events, dbs shared)

1500 orders in 3.5 s = 432.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.8 | 36.8 | 35.2 | 46.4 | 50.7 | 57.3 | 66.4 |
| placeOrder | declined | 27 | 27.3 | 33.4 | 32.7 | 39.9 | 40.2 | 44.3 | 44.3 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.2 | 5.3 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 1.9 | 7.7 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.0 | 1.2 | 6.9 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 0.9 | 1.4 | 9.0 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 8.6 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.6 | 9.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 8.0 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.9 | 6.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 4.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.7 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 7.8 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.1 | 2.1 | 2.3 | 2.3 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 7.1 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 11716 calls, mean 0.01 ms, local 18203 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.4 s = 443.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.9 | 35.9 | 34.2 | 44.3 | 48.3 | 55.2 | 63.6 |
| placeOrder | declined | 27 | 27.9 | 31.9 | 31.1 | 36.8 | 36.9 | 46.5 | 46.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3751 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.5 | 7.5 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.0 | 8.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 3.4 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.1 | 7.7 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 0.9 | 1.4 | 1.7 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 3.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.7 | 6.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.8 | 1.2 | 6.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.2 |
| shipping -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.9 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.6 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.5 |
| notifications -> customers.getCustomer [local] | 788 |  | 0.1 | 0.1 | 0.3 | 0.5 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.1 | 1.5 | 1.8 | 1.8 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 685 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8990 calls, mean 0.01 ms, local 20929 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes on, async events, dbs shared)

1500 orders in 3.3 s = 448.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.0 | 35.5 | 33.9 | 43.9 | 47.6 | 55.9 | 65.4 |
| placeOrder | declined | 27 | 28.1 | 32.7 | 32.1 | 36.3 | 36.8 | 49.6 | 49.6 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 8.2 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.5 | 2.0 | 8.4 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 6.3 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.9 | 8.2 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 0.9 | 1.1 | 2.9 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 7.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.3 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.8 | 1.2 | 7.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.4 | 0.4 | 6.8 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.9 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.8 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.5 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.0 | 1.5 | 1.8 | 1.8 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.8 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9779 calls, mean 0.01 ms, local 20140 calls, mean 0.50 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
