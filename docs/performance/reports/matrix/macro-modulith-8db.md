# Performance run 20261027T193757Z

- Started: 2026-10-27T19:37:57+00:00
- Commit: a3f7372 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes macro-modulith --cache both --parallel both --async both --dbs per-module --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/macro-modulith-8db`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith cache off parallel off async outbox | 391 | 1473 | 27 | 17.7 | 40.7 | 39.5 | 47.5 | 51.6 | 62.3 | 68.3 | 19.9 | PASS |
| macro-modulith cache off parallel off async events | 372 | 1473 | 27 | 16.7 | 42.8 | 41.3 | 50.3 | 56.0 | 65.1 | 74.7 | 19.9 | PASS |
| macro-modulith cache off parallel on async outbox | 414 | 1473 | 27 | 16.3 | 38.4 | 37.4 | 44.7 | 48.8 | 55.3 | 62.5 | 19.9 | PASS |
| macro-modulith cache off parallel on async events | 395 | 1473 | 27 | 16.5 | 40.3 | 38.7 | 50.0 | 52.9 | 55.9 | 69.7 | 19.9 | PASS |
| macro-modulith cache on parallel off async outbox | 442 | 1473 | 27 | 14.5 | 36.0 | 33.8 | 45.6 | 48.7 | 87.1 | 89.7 | 19.9 | PASS |
| macro-modulith cache on parallel off async events | 417 | 1473 | 27 | 17.2 | 38.2 | 36.6 | 47.6 | 50.2 | 55.5 | 63.1 | 19.9 | PASS |
| macro-modulith cache on parallel on async outbox | 434 | 1473 | 27 | 16.2 | 36.7 | 34.5 | 46.0 | 48.7 | 88.9 | 96.0 | 19.9 | PASS |
| macro-modulith cache on parallel on async events | 421 | 1473 | 27 | 15.7 | 37.8 | 36.1 | 45.1 | 50.1 | 61.7 | 72.4 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith (parallel off) (async outbox) | 391 | 442 | +13% | 39.5 | 33.8 | -14% | 51.6 | 48.7 | -6% | 62.3 | 87.1 | +40% |
| macro-modulith (parallel off) (async events) | 372 | 417 | +12% | 41.3 | 36.6 | -11% | 56.0 | 50.2 | -10% | 65.1 | 55.5 | -15% |
| macro-modulith (parallel on) (async outbox) | 414 | 434 | +5% | 37.4 | 34.5 | -8% | 48.8 | 48.7 | -0% | 55.3 | 88.9 | +61% |
| macro-modulith (parallel on) (async events) | 395 | 421 | +7% | 38.7 | 36.1 | -7% | 52.9 | 50.1 | -5% | 55.9 | 61.7 | +10% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith (cache off) (async outbox) | 391 | 414 | +6% | 39.5 | 37.4 | -5% | 51.6 | 48.8 | -5% | 62.3 | 55.3 | -11% |
| macro-modulith (cache off) (async events) | 372 | 395 | +6% | 41.3 | 38.7 | -6% | 56.0 | 52.9 | -6% | 65.1 | 55.9 | -14% |
| macro-modulith (cache on) (async outbox) | 442 | 434 | -2% | 33.8 | 34.5 | +2% | 48.7 | 48.7 | +0% | 87.1 | 88.9 | +2% |
| macro-modulith (cache on) (async events) | 417 | 421 | +1% | 36.6 | 36.1 | -1% | 50.2 | 50.1 | -0% | 55.5 | 61.7 | +11% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| macro-modulith (cache off) (parallel off) | 391 | 372 | -5% | 39.5 | 41.3 | +5% | 51.6 | 56.0 | +9% | 62.3 | 65.1 | +5% |
| macro-modulith (cache off) (parallel on) | 414 | 395 | -5% | 37.4 | 38.7 | +3% | 48.8 | 52.9 | +8% | 55.3 | 55.9 | +1% |
| macro-modulith (cache on) (parallel off) | 442 | 417 | -6% | 33.8 | 36.6 | +8% | 48.7 | 50.2 | +3% | 87.1 | 55.5 | -36% |
| macro-modulith (cache on) (parallel on) | 434 | 421 | -3% | 34.5 | 36.1 | +5% | 48.7 | 50.1 | +3% | 88.9 | 61.7 | -31% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | macro-modulith cache off parallel off async outbox | macro-modulith cache off parallel off async events | macro-modulith cache off parallel on async outbox | macro-modulith cache off parallel on async events | macro-modulith cache on parallel off async outbox | macro-modulith cache on parallel off async events | macro-modulith cache on parallel on async outbox | macro-modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.3 | 1.3 | 1.3 | 1.3 | 1.1 | 1.2 | 1.3 | 1.2 |
| orders -> inventory.reserve | 1.1 | 1.2 | 1.2 | 1.2 | 1.1 | 1.2 | 1.2 | 1.2 |
| orders -> shipping.quote | 0.9 | 0.9 | 1.0 | 1.1 | 0.5 | 0.5 | 0.7 | 0.7 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 |
| orders -> notifications.send | 0.7 | 0.8 | 0.7 | 0.9 | 0.6 | 0.7 | 0.6 | 0.7 |
| shipping -> inventory.getStock | 0.3 | 0.3 | 0.4 | 0.4 | 0.2 | 0.2 | 0.3 | 0.3 |
| orders -> pricing.quote | 0.6 | 0.7 | 0.8 | 0.8 | 0.3 | 0.3 | 0.5 | 0.5 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| inventory -> catalog.productExists | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> catalog.getProduct | 0.1 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.2 | 0.2 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.2 |
| notifications -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.0 | 0.1 | 0.0 |
| orders -> inventory.release | 1.3 | 1.1 | 1.3 | 1.2 | 1.3 | 1.1 | 1.3 | 1.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | macro-modulith cache off parallel off async outbox | macro-modulith cache off parallel off async events | macro-modulith cache off parallel on async outbox | macro-modulith cache off parallel on async events | macro-modulith cache on parallel off async outbox | macro-modulith cache on parallel off async events | macro-modulith cache on parallel on async outbox | macro-modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.7 | 1.7 | 1.7 | 1.8 | 1.6 | 1.6 | 1.6 | 1.6 |
| orders -> inventory.reserve | 1.7 | 1.7 | 1.7 | 1.8 | 1.6 | 1.7 | 1.8 | 1.9 |
| orders -> shipping.quote | 1.5 | 1.6 | 1.8 | 1.9 | 1.1 | 1.1 | 1.4 | 1.5 |
| orders -> payments.authorize | 1.0 | 1.0 | 1.0 | 1.1 | 1.1 | 1.1 | 1.1 | 1.1 |
| orders -> notifications.send | 1.3 | 1.1 | 1.2 | 1.2 | 1.1 | 0.9 | 1.1 | 1.0 |
| shipping -> inventory.getStock | 0.5 | 0.5 | 0.5 | 0.6 | 0.4 | 0.4 | 0.5 | 0.5 |
| orders -> pricing.quote | 1.0 | 1.1 | 1.2 | 1.3 | 0.6 | 0.7 | 0.9 | 0.9 |
| orders -> payments.capture | 0.6 | 0.7 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.3 | 0.3 | 0.2 | 0.2 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| shipping -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| orders -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| notifications -> customers.getCustomer | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 | 0.0 | 0.3 | 0.0 |
| orders -> inventory.release | 1.7 | 2.0 | 1.9 | 1.9 | 1.8 | 1.8 | 1.8 | 1.8 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | macro-modulith cache off parallel off async outbox | macro-modulith cache off parallel off async events | macro-modulith cache off parallel on async outbox | macro-modulith cache off parallel on async events | macro-modulith cache on parallel off async outbox | macro-modulith cache on parallel off async events | macro-modulith cache on parallel on async outbox | macro-modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.6% | 87.0% | 50.6% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| macro-modulith cache off parallel off async outbox | - | 29919 calls, 0.42 ms | - | - | - |
| macro-modulith cache off parallel off async events | - | 29919 calls, 0.44 ms | - | - | - |
| macro-modulith cache off parallel on async outbox | - | 29919 calls, 0.45 ms | - | - | - |
| macro-modulith cache off parallel on async events | - | 29919 calls, 0.49 ms | - | - | - |
| macro-modulith cache on parallel off async outbox | 10943 calls, 0.01 ms | 18976 calls, 0.49 ms | - | - | - |
| macro-modulith cache on parallel off async events | 11716 calls, 0.01 ms | 18203 calls, 0.52 ms | - | - | - |
| macro-modulith cache on parallel on async outbox | 9005 calls, 0.01 ms | 20914 calls, 0.51 ms | - | - | - |
| macro-modulith cache on parallel on async events | 9779 calls, 0.01 ms | 20140 calls, 0.54 ms | - | - | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### macro-modulith (cache off, parallel quotes off, async outbox, dbs per-module)

1500 orders in 3.8 s = 390.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.7 | 40.7 | 39.5 | 47.5 | 51.6 | 62.3 | 68.3 |
| placeOrder | declined | 27 | 31.6 | 37.1 | 35.5 | 41.7 | 42.0 | 58.0 | 58.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.1 | 7.6 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.1 | 8.1 |
| orders -> shipping.quote [local] | 1500 |  | 0.9 | 0.8 | 1.5 | 2.0 | 7.2 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.2 | 6.4 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 1.3 | 1.7 | 2.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.5 | 0.6 | 5.5 |
| orders -> pricing.quote [local] | 1500 |  | 0.6 | 0.6 | 1.0 | 1.3 | 2.6 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 1.2 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.4 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 1.6 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.1 | 0.1 | 0.2 | 0.3 | 3.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 5.3 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.9 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.1 | 0.4 | 0.5 | 0.8 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.4 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.2 | 1.7 | 2.2 | 2.2 |

By binding: local 29919 calls, mean 0.42 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes off, async events, dbs per-module)

1500 orders in 4.0 s = 372.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.7 | 42.8 | 41.3 | 50.3 | 56.0 | 65.1 | 74.7 |
| placeOrder | declined | 27 | 32.0 | 38.3 | 37.8 | 42.8 | 43.3 | 57.2 | 57.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.3 | 5.5 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.2 | 12.3 |
| orders -> shipping.quote [local] | 1500 |  | 0.9 | 0.8 | 1.6 | 2.1 | 9.3 |
| orders -> notifications.send [local] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.4 | 10.4 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.3 | 1.8 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.1 | 1.4 | 8.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.4 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.7 | 1.5 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.4 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.4 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 8.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.2 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 8.2 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.3 | 9.2 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.6 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 0.9 | 2.0 | 2.0 | 2.0 |

By binding: local 29919 calls, mean 0.44 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.6 s = 414.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.3 | 38.4 | 37.4 | 44.7 | 48.8 | 55.3 | 62.5 |
| placeOrder | declined | 27 | 29.6 | 35.0 | 33.6 | 38.6 | 41.7 | 61.5 | 61.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.3 | 1.7 | 2.2 | 10.1 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.1 | 10.0 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 1.0 | 1.8 | 2.4 | 8.9 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.3 | 6.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.5 | 0.7 | 8.4 |
| orders -> pricing.quote [local] | 1500 |  | 0.8 | 0.7 | 1.2 | 1.5 | 8.7 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 1.2 | 1.7 | 2.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.8 | 2.3 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.9 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.3 | 7.7 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 4.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.8 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 8.2 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.4 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.1 | 0.3 | 0.5 | 1.8 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.3 | 1.9 | 1.9 | 1.9 |

By binding: local 29919 calls, mean 0.45 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes on, async events, dbs per-module)

1500 orders in 3.8 s = 395.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.5 | 40.3 | 38.7 | 50.0 | 52.9 | 55.9 | 69.7 |
| placeOrder | declined | 27 | 32.6 | 36.4 | 35.6 | 39.7 | 39.9 | 53.1 | 53.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.3 | 1.8 | 2.2 | 2.8 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.2 | 1.8 | 2.4 | 3.2 |
| orders -> shipping.quote [local] | 1500 |  | 1.1 | 1.0 | 1.9 | 2.4 | 9.3 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.2 | 1.6 | 9.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.4 | 9.2 |
| orders -> pricing.quote [local] | 1500 |  | 0.8 | 0.8 | 1.3 | 1.7 | 8.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 2.2 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 12.1 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.6 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 1.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 7.6 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.6 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.5 | 7.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 8.4 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 1.9 | 2.8 | 2.8 |

By binding: local 29919 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-off-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes off, async outbox, dbs per-module)

1500 orders in 3.4 s = 442.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 14.5 | 36.0 | 33.8 | 45.6 | 48.7 | 87.1 | 89.7 |
| placeOrder | declined | 27 | 27.2 | 32.8 | 31.1 | 39.7 | 42.7 | 46.9 | 46.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5704 | 76.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 2.0 | 55.1 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 0.9 | 1.6 | 2.2 | 54.6 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 8.6 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.8 | 2.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 8.8 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.5 | 2.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.5 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.8 | 1.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.4 | 0.4 | 52.8 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| notifications -> customers.getCustomer [local] | 773 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.2 | 1.8 | 2.3 | 2.3 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 700 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10943 calls, mean 0.01 ms, local 18976 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes off, async events, dbs per-module)

1500 orders in 3.6 s = 416.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.2 | 38.2 | 36.6 | 47.6 | 50.2 | 55.5 | 63.1 |
| placeOrder | declined | 27 | 30.3 | 35.1 | 34.2 | 39.2 | 41.4 | 47.5 | 47.5 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.3 | 12.8 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.0 | 10.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 4.9 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.7 | 0.9 | 1.1 | 7.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 0.9 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.5 | 7.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 2.3 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.7 | 1.0 | 7.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.0 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.1 | 1.8 | 2.1 | 2.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 11716 calls, mean 0.01 ms, local 18203 calls, mean 0.52 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.5 s = 434.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.2 | 36.7 | 34.5 | 46.0 | 48.7 | 88.9 | 96.0 |
| placeOrder | declined | 27 | 27.6 | 32.9 | 32.2 | 37.6 | 40.4 | 45.9 | 45.9 |

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
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.1 | 1.6 | 2.1 | 57.9 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.2 | 9.1 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 1.9 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.0 | 9.5 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.7 | 2.4 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 4.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 8.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.2 | 8.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.7 |
| orders -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.3 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| shipping -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| notifications -> customers.getCustomer [local] | 774 |  | 0.2 | 0.1 | 0.4 | 0.6 | 1.0 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.3 | 1.8 | 2.0 | 2.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 8.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| orders -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 699 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 9005 calls, mean 0.01 ms, local 20914 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes on, async events, dbs per-module)

1500 orders in 3.6 s = 421.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.7 | 37.8 | 36.1 | 45.1 | 50.1 | 61.7 | 72.4 |
| placeOrder | declined | 27 | 29.2 | 34.5 | 33.3 | 37.0 | 40.0 | 53.7 | 53.7 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.9 | 2.6 | 12.7 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.1 | 10.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 3.4 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.7 | 1.5 | 2.0 | 7.2 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.4 | 12.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 6.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 2.3 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.4 | 6.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 7.2 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 6.1 |
| pricing -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 6.1 |
| shipping -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 6.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 0.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.5 | 0.7 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.0 | 1.8 | 2.0 | 2.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.4 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.3 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.8 |

By binding: cache 9779 calls, mean 0.01 ms, local 20140 calls, mean 0.54 ms. 19.9 module-to-module calls per order.

Raw samples: `macro-modulith-cache-on-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
