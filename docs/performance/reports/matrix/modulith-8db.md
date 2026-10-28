# Performance run 20261027T192931Z

- Started: 2026-10-27T19:29:31+00:00
- Commit: a3f7372 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes modulith --cache both --parallel both --async both --dbs per-module --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/modulith-8db`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith cache off parallel off async outbox | 394 | 1473 | 27 | 16.5 | 40.4 | 39.1 | 48.7 | 53.4 | 59.5 | 68.2 | 19.9 | PASS |
| modulith cache off parallel off async events | 368 | 1473 | 27 | 20.8 | 43.4 | 41.9 | 51.9 | 56.9 | 63.9 | 71.7 | 19.9 | PASS |
| modulith cache off parallel on async outbox | 417 | 1473 | 27 | 16.5 | 38.2 | 37.2 | 44.0 | 48.7 | 57.0 | 62.6 | 19.9 | PASS |
| modulith cache off parallel on async events | 399 | 1473 | 27 | 16.4 | 39.9 | 38.5 | 48.3 | 52.1 | 57.4 | 60.2 | 19.9 | PASS |
| modulith cache on parallel off async outbox | 447 | 1473 | 27 | 18.5 | 35.6 | 34.0 | 44.1 | 48.3 | 53.5 | 58.4 | 19.9 | PASS |
| modulith cache on parallel off async events | 418 | 1473 | 27 | 17.2 | 38.1 | 36.4 | 46.9 | 51.6 | 57.3 | 62.7 | 19.9 | PASS |
| modulith cache on parallel on async outbox | 440 | 1473 | 27 | 16.7 | 36.2 | 34.7 | 44.8 | 48.0 | 53.2 | 58.6 | 19.9 | PASS |
| modulith cache on parallel on async events | 422 | 1473 | 27 | 16.5 | 37.7 | 36.0 | 46.2 | 49.2 | 55.0 | 59.7 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (parallel off) (async outbox) | 394 | 447 | +13% | 39.1 | 34.0 | -13% | 53.4 | 48.3 | -10% | 59.5 | 53.5 | -10% |
| modulith (parallel off) (async events) | 368 | 418 | +14% | 41.9 | 36.4 | -13% | 56.9 | 51.6 | -9% | 63.9 | 57.3 | -10% |
| modulith (parallel on) (async outbox) | 417 | 440 | +5% | 37.2 | 34.7 | -7% | 48.7 | 48.0 | -1% | 57.0 | 53.2 | -7% |
| modulith (parallel on) (async events) | 399 | 422 | +6% | 38.5 | 36.0 | -6% | 52.1 | 49.2 | -6% | 57.4 | 55.0 | -4% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (cache off) (async outbox) | 394 | 417 | +6% | 39.1 | 37.2 | -5% | 53.4 | 48.7 | -9% | 59.5 | 57.0 | -4% |
| modulith (cache off) (async events) | 368 | 399 | +9% | 41.9 | 38.5 | -8% | 56.9 | 52.1 | -8% | 63.9 | 57.4 | -10% |
| modulith (cache on) (async outbox) | 447 | 440 | -2% | 34.0 | 34.7 | +2% | 48.3 | 48.0 | -1% | 53.5 | 53.2 | -0% |
| modulith (cache on) (async events) | 418 | 422 | +1% | 36.4 | 36.0 | -1% | 51.6 | 49.2 | -5% | 57.3 | 55.0 | -4% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (cache off) (parallel off) | 394 | 368 | -7% | 39.1 | 41.9 | +7% | 53.4 | 56.9 | +7% | 59.5 | 63.9 | +7% |
| modulith (cache off) (parallel on) | 417 | 399 | -4% | 37.2 | 38.5 | +4% | 48.7 | 52.1 | +7% | 57.0 | 57.4 | +1% |
| modulith (cache on) (parallel off) | 447 | 418 | -6% | 34.0 | 36.4 | +7% | 48.3 | 51.6 | +7% | 53.5 | 57.3 | +7% |
| modulith (cache on) (parallel on) | 440 | 422 | -4% | 34.7 | 36.0 | +4% | 48.0 | 49.2 | +3% | 53.2 | 55.0 | +3% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off parallel off async outbox | modulith cache off parallel off async events | modulith cache off parallel on async outbox | modulith cache off parallel on async events | modulith cache on parallel off async outbox | modulith cache on parallel off async events | modulith cache on parallel on async outbox | modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.3 | 1.3 | 1.3 | 1.3 | 1.1 | 1.2 | 1.2 | 1.2 |
| orders -> inventory.reserve | 1.0 | 1.2 | 1.1 | 1.1 | 1.0 | 1.2 | 1.2 | 1.2 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 |
| orders -> shipping.quote | 0.9 | 0.9 | 1.0 | 1.1 | 0.5 | 0.5 | 0.7 | 0.8 |
| orders -> notifications.send | 0.7 | 0.9 | 0.6 | 0.9 | 0.6 | 0.7 | 0.6 | 0.7 |
| shipping -> inventory.getStock | 0.3 | 0.4 | 0.4 | 0.4 | 0.2 | 0.2 | 0.2 | 0.3 |
| orders -> pricing.quote | 0.6 | 0.7 | 0.7 | 0.8 | 0.3 | 0.3 | 0.5 | 0.5 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> catalog.getProduct | 0.1 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.2 | 0.2 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.0 | 0.0 | 0.1 | 0.2 |
| notifications -> customers.getCustomer | 0.2 | 0.2 | 0.1 | 0.2 | 0.1 | 0.0 | 0.1 | 0.0 |
| orders -> inventory.release | 1.4 | 1.2 | 1.3 | 1.1 | 1.3 | 1.2 | 1.3 | 1.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off parallel off async outbox | modulith cache off parallel off async events | modulith cache off parallel on async outbox | modulith cache off parallel on async events | modulith cache on parallel off async outbox | modulith cache on parallel off async events | modulith cache on parallel on async outbox | modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.7 | 1.8 | 1.7 | 1.9 | 1.6 | 1.6 | 1.6 | 1.6 |
| orders -> inventory.reserve | 1.6 | 1.8 | 1.7 | 1.8 | 1.6 | 1.8 | 1.8 | 1.8 |
| orders -> payments.authorize | 1.0 | 1.1 | 1.1 | 1.1 | 1.1 | 1.1 | 1.1 | 1.1 |
| orders -> shipping.quote | 1.5 | 1.6 | 1.8 | 1.9 | 1.0 | 1.1 | 1.4 | 1.5 |
| orders -> notifications.send | 1.3 | 1.2 | 1.2 | 1.3 | 1.1 | 0.9 | 1.1 | 1.0 |
| shipping -> inventory.getStock | 0.5 | 0.5 | 0.5 | 0.6 | 0.4 | 0.5 | 0.5 | 0.5 |
| orders -> pricing.quote | 1.0 | 1.1 | 1.2 | 1.3 | 0.6 | 0.7 | 0.9 | 0.9 |
| orders -> payments.capture | 0.6 | 0.7 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.4 | 0.3 | 0.4 | 0.4 | 0.4 |
| inventory -> catalog.productExists | 0.2 | 0.3 | 0.3 | 0.3 | 0.2 | 0.2 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.2 | 0.3 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| shipping -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| orders -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 |
| notifications -> customers.getCustomer | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 | 0.0 | 0.3 | 0.0 |
| orders -> inventory.release | 2.3 | 2.3 | 2.0 | 1.6 | 2.0 | 2.0 | 1.8 | 1.9 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith cache off parallel off async outbox | modulith cache off parallel off async events | modulith cache off parallel on async outbox | modulith cache off parallel on async events | modulith cache on parallel off async outbox | modulith cache on parallel off async events | modulith cache on parallel on async outbox | modulith cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.6% | 87.0% | 50.6% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith cache off parallel off async outbox | - | 29919 calls, 0.41 ms | - | - | - |
| modulith cache off parallel off async events | - | 29919 calls, 0.45 ms | - | - | - |
| modulith cache off parallel on async outbox | - | 29919 calls, 0.45 ms | - | - | - |
| modulith cache off parallel on async events | - | 29919 calls, 0.48 ms | - | - | - |
| modulith cache on parallel off async outbox | 10943 calls, 0.01 ms | 18976 calls, 0.47 ms | - | - | - |
| modulith cache on parallel off async events | 11716 calls, 0.01 ms | 18203 calls, 0.52 ms | - | - | - |
| modulith cache on parallel on async outbox | 9006 calls, 0.01 ms | 20913 calls, 0.51 ms | - | - | - |
| modulith cache on parallel on async events | 9779 calls, 0.01 ms | 20140 calls, 0.54 ms | - | - | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache off, parallel quotes off, async outbox, dbs per-module)

1500 orders in 3.8 s = 394.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.5 | 40.4 | 39.1 | 48.7 | 53.4 | 59.5 | 68.2 |
| placeOrder | declined | 27 | 27.7 | 35.8 | 36.0 | 39.3 | 39.4 | 46.9 | 46.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.1 | 12.1 |
| orders -> inventory.reserve [local] | 1500 |  | 1.0 | 1.0 | 1.6 | 2.0 | 13.1 |
| orders -> shipping.quote [local] | 1500 |  | 0.9 | 0.8 | 1.5 | 2.0 | 6.2 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.1 | 6.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.5 | 0.6 | 5.5 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 1.3 | 1.7 | 2.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.6 | 0.6 | 1.0 | 1.3 | 10.1 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.8 | 11.9 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 5.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.9 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 8.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.2 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 5.2 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.1 | 0.4 | 0.5 | 1.1 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 8.6 |
| orders -> inventory.release [local] | 27 |  | 1.4 | 1.3 | 2.3 | 2.9 | 2.9 |

By binding: local 29919 calls, mean 0.41 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache off, parallel quotes off, async events, dbs per-module)

1500 orders in 4.1 s = 367.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.8 | 43.4 | 41.9 | 51.9 | 56.9 | 63.9 | 71.7 |
| placeOrder | declined | 27 | 33.2 | 39.6 | 39.7 | 41.7 | 43.3 | 60.7 | 60.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.3 | 1.8 | 2.4 | 7.9 |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 3.5 |
| orders -> shipping.quote [local] | 1500 |  | 0.9 | 0.8 | 1.6 | 2.4 | 8.1 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.2 | 1.5 | 12.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 2.0 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.1 | 1.6 | 8.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.7 | 2.0 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.2 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 6.9 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.8 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.3 | 6.5 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.5 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 6.5 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.0 | 2.3 | 2.5 | 2.5 |

By binding: local 29919 calls, mean 0.45 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache off, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.6 s = 417.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.5 | 38.2 | 37.2 | 44.0 | 48.7 | 57.0 | 62.6 |
| placeOrder | declined | 27 | 29.5 | 34.3 | 33.5 | 37.8 | 38.6 | 48.2 | 48.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.3 | 1.7 | 2.2 | 7.0 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.1 | 1.7 | 2.2 | 9.6 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 0.9 | 1.8 | 2.3 | 5.7 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.2 | 6.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.5 | 0.7 | 1.8 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.2 | 1.5 | 5.7 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.2 | 1.6 | 2.4 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 2.5 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.0 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.3 | 5.8 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 6.0 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.9 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.2 | 0.3 | 0.4 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.5 | 0.7 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.2 | 2.0 | 2.4 | 2.4 |

By binding: local 29919 calls, mean 0.45 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache off, parallel quotes on, async events, dbs per-module)

1500 orders in 3.8 s = 399.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.4 | 39.9 | 38.5 | 48.3 | 52.1 | 57.4 | 60.2 |
| placeOrder | declined | 27 | 31.2 | 35.4 | 35.2 | 38.9 | 39.3 | 48.0 | 48.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.3 | 1.9 | 2.3 | 8.9 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.8 | 2.3 | 7.4 |
| orders -> shipping.quote [local] | 1500 |  | 1.1 | 1.0 | 1.9 | 2.7 | 6.2 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.6 | 6.3 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 2.0 |
| orders -> pricing.quote [local] | 1500 |  | 0.8 | 0.8 | 1.3 | 1.7 | 5.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 5.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.1 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.5 | 5.2 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 5.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 5.3 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.2 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.2 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.0 | 1.6 | 1.8 | 1.8 |

By binding: local 29919 calls, mean 0.48 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-off-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes off, async outbox, dbs per-module)

1500 orders in 3.4 s = 446.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.5 | 35.6 | 34.0 | 44.1 | 48.3 | 53.5 | 58.4 |
| placeOrder | declined | 27 | 26.9 | 32.4 | 32.7 | 36.7 | 38.7 | 40.7 | 40.7 |

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
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 2.0 | 3.0 |
| orders -> inventory.reserve [local] | 1500 |  | 1.0 | 1.0 | 1.6 | 2.1 | 2.8 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 6.0 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.6 | 2.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 9.3 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.0 | 1.4 | 2.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.9 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.9 | 2.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.3 | 0.4 | 0.7 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.6 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 |
| notifications -> customers.getCustomer [local] | 773 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.3 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.2 | 2.0 | 2.0 | 2.0 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 700 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 10943 calls, mean 0.01 ms, local 18976 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes off, async events, dbs per-module)

1500 orders in 3.6 s = 418.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.2 | 38.1 | 36.4 | 46.9 | 51.6 | 57.3 | 62.7 |
| placeOrder | declined | 27 | 30.1 | 35.4 | 33.6 | 44.3 | 46.0 | 54.2 | 54.2 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 12.7 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.0 | 3.2 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.4 | 8.9 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 0.9 | 1.1 | 11.9 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.5 | 8.2 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 7.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.3 | 0.7 | 0.9 | 9.3 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.9 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.6 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 9.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 2.0 | 2.1 | 2.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 11716 calls, mean 0.01 ms, local 18203 calls, mean 0.52 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.4 s = 439.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.7 | 36.2 | 34.7 | 44.8 | 48.0 | 53.2 | 58.6 |
| placeOrder | declined | 27 | 27.9 | 32.8 | 31.6 | 35.3 | 38.7 | 51.1 | 51.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3767 | 50.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.4 | 7.1 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.1 | 8.4 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 8.2 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.7 | 1.4 | 1.9 | 4.1 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.5 | 2.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 6.4 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 1.1 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.2 | 3.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 7.0 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.9 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.8 |
| pricing -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.5 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.4 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| notifications -> customers.getCustomer [local] | 774 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.3 | 1.8 | 1.8 | 1.8 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| notifications -> customers.getCustomer [cache] | 699 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 9006 calls, mean 0.01 ms, local 20913 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async events, dbs per-module)

1500 orders in 3.6 s = 422.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.5 | 37.7 | 36.0 | 46.2 | 49.2 | 55.0 | 59.7 |
| placeOrder | declined | 27 | 30.3 | 35.0 | 33.8 | 41.8 | 42.4 | 58.0 | 58.0 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.5 | 9.5 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.1 | 11.8 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 8.7 |
| orders -> shipping.quote [local] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.0 | 6.7 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.2 | 12.0 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 4.4 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.3 | 6.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 8.5 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 5.9 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 5.8 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 5.8 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.1 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.0 | 1.9 | 2.3 | 2.3 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.8 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 9779 calls, mean 0.01 ms, local 20140 calls, mean 0.54 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-cache-on-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
