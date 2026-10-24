# Performance run 20261024T151037Z

- Started: 2026-10-24T15:10:37+00:00
- Commit: 2c801ac (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1200 orders, 4 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes modulith,groups,microservices --async both --parallel both --orders 1200 --warmup 200 --concurrency 4`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith parallel off async outbox | 319 | 1179 | 21 | 7.4 | 12.5 | 12.0 | 16.0 | 17.1 | 20.4 | 79.3 | 20.0 | PASS |
| modulith parallel off async events | 314 | 1179 | 21 | 7.5 | 12.8 | 12.4 | 16.4 | 17.6 | 20.2 | 25.7 | 20.0 | PASS |
| modulith parallel on async outbox | 326 | 1179 | 21 | 6.9 | 12.3 | 11.7 | 15.6 | 16.9 | 20.3 | 86.2 | 20.0 | PASS |
| modulith parallel on async events | 326 | 1179 | 21 | 7.1 | 12.3 | 11.8 | 16.5 | 17.5 | 19.8 | 28.0 | 20.0 | PASS |
| groups parallel off async outbox | 204 | 1179 | 21 | 9.8 | 19.6 | 18.9 | 25.4 | 27.7 | 32.8 | 89.4 | 20.0 | PASS |
| groups parallel off async events | 204 | 1179 | 21 | 9.3 | 19.6 | 18.2 | 26.1 | 28.1 | 33.2 | 91.1 | 20.0 | PASS |
| groups parallel on async outbox | 232 | 1179 | 21 | 9.0 | 17.2 | 16.7 | 23.0 | 25.1 | 28.9 | 39.6 | 20.0 | PASS |
| groups parallel on async events | 219 | 1179 | 21 | 11.0 | 18.3 | 17.2 | 24.9 | 26.6 | 30.7 | 79.7 | 20.0 | PASS |
| microservices parallel off async outbox | 146 | 1179 | 21 | 13.7 | 27.4 | 25.9 | 37.4 | 40.7 | 50.7 | 107.2 | 20.0 | PASS |
| microservices parallel off async events | 142 | 1179 | 21 | 11.7 | 28.1 | 26.5 | 38.7 | 42.3 | 65.1 | 93.0 | 20.0 | PASS |
| microservices parallel on async outbox | 173 | 1179 | 21 | 11.2 | 23.2 | 22.0 | 31.8 | 34.7 | 41.8 | 81.5 | 20.0 | PASS |
| microservices parallel on async events | 163 | 1179 | 21 | 12.7 | 24.7 | 22.4 | 34.1 | 36.6 | 78.9 | 96.2 | 20.0 | PASS |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (async outbox) | 319 | 326 | +2% | 12.0 | 11.7 | -3% | 17.1 | 16.9 | -1% | 20.4 | 20.3 | -1% |
| modulith (async events) | 314 | 326 | +4% | 12.4 | 11.8 | -5% | 17.6 | 17.5 | -0% | 20.2 | 19.8 | -2% |
| groups (async outbox) | 204 | 232 | +14% | 18.9 | 16.7 | -12% | 27.7 | 25.1 | -10% | 32.8 | 28.9 | -12% |
| groups (async events) | 204 | 219 | +7% | 18.2 | 17.2 | -6% | 28.1 | 26.6 | -5% | 33.2 | 30.7 | -8% |
| microservices (async outbox) | 146 | 173 | +18% | 25.9 | 22.0 | -15% | 40.7 | 34.7 | -15% | 50.7 | 41.8 | -18% |
| microservices (async events) | 142 | 163 | +14% | 26.5 | 22.4 | -15% | 42.3 | 36.6 | -13% | 65.1 | 78.9 | +21% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (parallel off) | 319 | 314 | -2% | 12.0 | 12.4 | +4% | 17.1 | 17.6 | +3% | 20.4 | 20.2 | -1% |
| modulith (parallel on) | 326 | 326 | +0% | 11.7 | 11.8 | +1% | 16.9 | 17.5 | +4% | 20.3 | 19.8 | -2% |
| groups (parallel off) | 204 | 204 | +0% | 18.9 | 18.2 | -4% | 27.7 | 28.1 | +1% | 32.8 | 33.2 | +1% |
| groups (parallel on) | 232 | 219 | -6% | 16.7 | 17.2 | +3% | 25.1 | 26.6 | +6% | 28.9 | 30.7 | +6% |
| microservices (parallel off) | 146 | 142 | -3% | 25.9 | 26.5 | +2% | 40.7 | 42.3 | +4% | 50.7 | 65.1 | +28% |
| microservices (parallel on) | 173 | 163 | -6% | 22.0 | 22.4 | +2% | 34.7 | 36.6 | +6% | 41.8 | 78.9 | +89% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith parallel off async outbox | modulith parallel off async events | modulith parallel on async outbox | modulith parallel on async events | groups parallel off async outbox | groups parallel off async events | groups parallel on async outbox | groups parallel on async events | microservices parallel off async outbox | microservices parallel off async events | microservices parallel on async outbox | microservices parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 0.8 | 0.9 | 1.2 | 1.2 | 1.5 | 1.5 | 2.2 | 2.4 | 4.2 | 4.2 | 4.2 | 4.5 |
| orders -> shipping.createShipment | 1.6 | 1.7 | 1.6 | 1.7 | 2.3 | 2.3 | 2.2 | 2.4 | 3.2 | 3.3 | 3.3 | 3.4 |
| orders -> inventory.reserve | 1.4 | 1.5 | 1.4 | 1.5 | 2.0 | 2.0 | 2.0 | 2.2 | 2.2 | 2.3 | 2.2 | 2.3 |
| orders -> pricing.quote | 0.6 | 0.5 | 0.8 | 0.8 | 1.9 | 1.9 | 1.9 | 2.0 | 2.7 | 2.8 | 2.8 | 2.9 |
| orders -> payments.authorize | 1.0 | 1.0 | 1.0 | 1.0 | 1.7 | 1.7 | 1.7 | 1.8 | 2.4 | 2.6 | 2.5 | 2.7 |
| orders -> notifications.send | 0.8 | 0.9 | 0.8 | 0.9 | 1.2 | 0.9 | 1.2 | 1.0 | 2.6 | 3.1 | 2.5 | 3.2 |
| shipping -> inventory.getStock | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.5 | 1.3 | 1.3 | 1.3 | 1.4 |
| orders -> payments.capture | 0.6 | 0.6 | 0.6 | 0.6 | 1.2 | 1.3 | 1.2 | 1.3 | 1.3 | 1.4 | 1.3 | 1.5 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 1.1 | 1.1 | 1.1 | 1.1 |
| orders -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 0.6 | 0.6 | 0.7 | 0.7 | 0.7 | 0.8 | 0.7 | 0.8 |
| pricing -> customers.getCustomer | 0.0 | 0.0 | 0.2 | 0.2 | 0.7 | 0.7 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.8 | 0.8 | 0.8 | 0.9 |
| shipping -> customers.getCustomer | 0.0 | 0.0 | 0.1 | 0.1 | 0.0 | 0.0 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 |
| notifications -> customers.getCustomer | 0.1 | 0.0 | 0.1 | 0.0 | 0.5 | 0.0 | 0.5 | 0.0 | 0.9 | 0.9 | 0.8 | 0.8 |
| inventory -> catalog.productExists | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.3 | 0.3 | 0.3 | 0.4 |
| pricing -> catalog.getProduct | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.3 | 0.3 | 0.3 | 0.3 |
| orders -> inventory.release | 1.5 | 1.6 | 1.5 | 1.6 | 2.9 | 1.9 | 3.0 | 2.2 | 2.9 | 2.1 | 3.0 | 2.4 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith parallel off async outbox | modulith parallel off async events | modulith parallel on async outbox | modulith parallel on async events | groups parallel off async outbox | groups parallel off async events | groups parallel on async outbox | groups parallel on async events | microservices parallel off async outbox | microservices parallel off async events | microservices parallel on async outbox | microservices parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.7 | 1.7 | 2.1 | 2.1 | 2.4 | 2.4 | 3.6 | 3.7 | 7.4 | 7.4 | 7.4 | 7.7 |
| orders -> shipping.createShipment | 2.1 | 2.1 | 2.1 | 2.1 | 2.8 | 2.7 | 2.9 | 2.9 | 4.0 | 4.2 | 4.4 | 4.3 |
| orders -> inventory.reserve | 2.0 | 2.1 | 2.1 | 2.2 | 2.7 | 2.7 | 2.9 | 3.0 | 3.0 | 3.0 | 3.1 | 3.2 |
| orders -> pricing.quote | 0.9 | 1.0 | 1.2 | 1.3 | 2.8 | 2.7 | 2.8 | 2.8 | 4.7 | 4.8 | 5.0 | 5.0 |
| orders -> payments.authorize | 1.2 | 1.3 | 1.3 | 1.3 | 2.1 | 2.2 | 2.2 | 2.2 | 3.6 | 3.7 | 3.8 | 3.6 |
| orders -> notifications.send | 1.3 | 1.1 | 1.4 | 1.1 | 2.4 | 1.1 | 2.3 | 1.3 | 4.5 | 4.5 | 4.5 | 4.6 |
| shipping -> inventory.getStock | 0.6 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 2.5 | 2.5 | 2.6 | 2.7 |
| orders -> payments.capture | 0.8 | 0.8 | 0.8 | 0.8 | 1.5 | 1.5 | 1.5 | 1.5 | 1.7 | 1.7 | 1.7 | 1.8 |
| shipping -> inventory.getReservation | 0.4 | 0.5 | 0.5 | 0.4 | 0.5 | 0.5 | 0.5 | 0.5 | 1.4 | 1.4 | 1.4 | 1.4 |
| orders -> customers.getCustomer | 0.3 | 0.4 | 0.4 | 0.4 | 1.1 | 1.0 | 1.1 | 1.2 | 1.2 | 1.3 | 1.4 | 1.4 |
| pricing -> customers.getCustomer | 0.0 | 0.0 | 0.4 | 0.4 | 1.2 | 1.3 | 1.1 | 1.2 | 1.3 | 1.3 | 1.3 | 1.4 |
| payments -> customers.getPaymentMethod | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 1.5 | 1.5 | 1.6 | 1.5 |
| shipping -> customers.getCustomer | 0.0 | 0.0 | 0.3 | 0.3 | 0.0 | 0.0 | 1.0 | 1.1 | 1.1 | 1.2 | 1.2 | 1.2 |
| notifications -> customers.getCustomer | 0.3 | 0.1 | 0.3 | 0.1 | 1.3 | 0.0 | 1.4 | 0.0 | 1.9 | 1.5 | 1.8 | 1.6 |
| inventory -> catalog.productExists | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 1.4 | 1.4 | 1.5 | 1.5 |
| pricing -> catalog.getProduct | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 1.4 | 1.4 | 1.5 | 1.5 |
| orders -> inventory.release | 2.0 | 2.7 | 2.2 | 2.8 | 3.7 | 3.0 | 3.8 | 3.2 | 3.7 | 2.7 | 4.0 | 3.5 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith parallel off async outbox | modulith parallel off async events | modulith parallel on async outbox | modulith parallel on async events | groups parallel off async outbox | groups parallel off async events | groups parallel on async outbox | groups parallel on async events | microservices parallel off async outbox | microservices parallel off async events | microservices parallel on async outbox | microservices parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.7% | 75.7% | 75.7% | 75.7% |
| catalog.productExists | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.7% | 75.7% | 75.7% | 75.7% |
| customers.getCustomer | 76.8% | 86.4% | 49.7% | 59.3% | 63.7% | 72.9% | 50.1% | 59.3% | 44.2% | 46.0% | 44.3% | 46.0% |
| customers.getPaymentMethod | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith parallel off async outbox | 8666 calls, 0.02 ms | 15349 calls, 0.67 ms | - | - | - |
| modulith parallel off async events | 9240 calls, 0.03 ms | 14775 calls, 0.72 ms | - | - | - |
| modulith parallel on async outbox | 7051 calls, 0.04 ms | 16964 calls, 0.70 ms | - | - | - |
| modulith parallel on async events | 7624 calls, 0.03 ms | 16391 calls, 0.73 ms | - | - | - |
| groups parallel off async outbox | 7884 calls, 0.02 ms | 6788 calls, 0.51 ms | - | - | 9343 calls, 1.58 ms |
| groups parallel off async events | 8432 calls, 0.02 ms | 6788 calls, 0.46 ms | - | - | 8795 calls, 1.62 ms |
| groups parallel on async outbox | 7077 calls, 0.02 ms | 6788 calls, 0.53 ms | - | - | 10150 calls, 1.61 ms |
| groups parallel on async events | 7625 calls, 0.02 ms | 6788 calls, 0.52 ms | - | - | 9602 calls, 1.75 ms |
| microservices parallel off async outbox | 6722 calls, 0.01 ms | - | - | - | 17293 calls, 1.89 ms |
| microservices parallel off async events | 6829 calls, 0.01 ms | - | - | - | 17186 calls, 1.98 ms |
| microservices parallel on async outbox | 6725 calls, 0.01 ms | - | - | - | 17290 calls, 1.90 ms |
| microservices parallel on async events | 6829 calls, 0.01 ms | - | - | - | 17186 calls, 2.07 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache on, parallel quotes off, async outbox)

1200 orders in 3.8 s = 318.9 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 7.4 | 12.5 | 12.0 | 16.0 | 17.1 | 20.4 | 79.3 |
| placeOrder | declined | 21 | 5.2 | 8.4 | 7.8 | 12.0 | 12.0 | 12.6 | 12.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 4576 | 76.8% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.6 | 1.6 | 2.1 | 2.5 | 7.4 |
| orders -> inventory.reserve [local] | 1200 |  | 1.4 | 1.3 | 2.0 | 2.7 | 7.9 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.0 | 1.0 | 1.2 | 1.4 | 4.7 |
| orders -> shipping.quote [local] | 1200 |  | 0.8 | 0.7 | 1.7 | 1.9 | 4.5 |
| orders -> notifications.send [local] | 1179 |  | 0.8 | 0.7 | 1.3 | 1.9 | 3.8 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.3 | 0.6 | 0.7 | 3.0 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.8 | 0.8 | 2.8 |
| orders -> pricing.quote [local] | 1200 |  | 0.6 | 0.4 | 0.9 | 1.2 | 63.5 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.3 | 0.4 | 0.5 | 3.8 |
| orders -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.4 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.3 | 0.4 | 3.6 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.3 | 0.4 | 3.7 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.3 | 0.4 | 0.6 |
| notifications -> customers.getCustomer [local] | 574 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.8 |
| shipping -> customers.getCustomer [cache] | 2379 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| orders -> inventory.release [local] | 21 |  | 1.5 | 1.5 | 2.0 | 2.5 | 2.5 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 605 |  | 0.0 | 0.0 | 0.0 | 0.1 | 3.0 |
| pricing -> customers.getCustomer [cache] | 1200 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |

By binding: cache 8666 calls, mean 0.02 ms, local 15349 calls, mean 0.67 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/modulith-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes off, async events)

1200 orders in 3.8 s = 313.6 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 7.5 | 12.8 | 12.4 | 16.4 | 17.6 | 20.2 | 25.7 |
| placeOrder | declined | 21 | 5.1 | 8.4 | 8.2 | 11.4 | 11.4 | 11.6 | 11.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 5150 | 86.4% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.7 | 1.6 | 2.1 | 2.4 | 12.1 |
| orders -> inventory.reserve [local] | 1200 |  | 1.5 | 1.4 | 2.1 | 2.7 | 12.3 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.0 | 1.0 | 1.3 | 1.4 | 7.3 |
| orders -> notifications.send [local] | 1179 |  | 0.9 | 0.9 | 1.1 | 1.2 | 6.0 |
| orders -> shipping.quote [local] | 1200 |  | 0.9 | 0.8 | 1.7 | 1.9 | 6.2 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.3 | 0.6 | 0.7 | 5.1 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.8 | 0.8 | 5.3 |
| orders -> pricing.quote [local] | 1200 |  | 0.5 | 0.5 | 1.0 | 1.2 | 3.7 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.3 | 0.5 | 0.5 | 5.4 |
| orders -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 5.0 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.4 | 1.4 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.3 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.3 | 0.4 | 0.5 |
| shipping -> customers.getCustomer [cache] | 2379 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.1 | 0.1 | 1.0 |
| orders -> inventory.release [local] | 21 |  | 1.6 | 1.3 | 2.7 | 4.1 | 4.1 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 1200 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |

By binding: cache 9240 calls, mean 0.03 ms, local 14775 calls, mean 0.72 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/modulith-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox)

1200 orders in 3.7 s = 326.1 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 6.9 | 12.3 | 11.7 | 15.6 | 16.9 | 20.3 | 86.2 |
| placeOrder | declined | 21 | 5.4 | 7.7 | 7.9 | 9.6 | 10.8 | 11.4 | 11.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2961 | 49.7% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.6 | 1.6 | 2.1 | 2.3 | 8.5 |
| orders -> inventory.reserve [local] | 1200 |  | 1.4 | 1.4 | 2.1 | 3.2 | 8.7 |
| orders -> shipping.quote [local] | 1200 |  | 1.2 | 1.1 | 2.1 | 2.5 | 70.0 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.0 | 1.0 | 1.3 | 1.5 | 8.7 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.3 | 0.7 | 0.7 | 69.4 |
| orders -> pricing.quote [local] | 1200 |  | 0.8 | 0.7 | 1.2 | 1.6 | 69.8 |
| orders -> notifications.send [local] | 1179 |  | 0.8 | 0.7 | 1.4 | 1.9 | 3.8 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.8 | 0.9 | 3.4 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.3 | 0.5 | 0.5 | 3.2 |
| orders -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.6 | 3.3 |
| shipping -> customers.getCustomer [local] | 807 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.2 |
| pricing -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.2 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| notifications -> customers.getCustomer [local] | 574 |  | 0.2 | 0.2 | 0.5 | 0.6 | 1.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.1 | 0.0 | 0.0 | 0.1 | 68.9 |
| shipping -> customers.getCustomer [cache] | 1572 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.9 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> inventory.release [local] | 21 |  | 1.5 | 1.4 | 2.2 | 2.5 | 2.5 |
| notifications -> customers.getCustomer [cache] | 605 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7051 calls, mean 0.04 ms, local 16964 calls, mean 0.70 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/modulith-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async events)

1200 orders in 3.7 s = 326.2 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 7.1 | 12.3 | 11.8 | 16.5 | 17.5 | 19.8 | 28.0 |
| placeOrder | declined | 21 | 4.3 | 8.5 | 8.1 | 13.3 | 13.4 | 14.6 | 14.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3534 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.7 | 1.6 | 2.1 | 2.4 | 8.1 |
| orders -> inventory.reserve [local] | 1200 |  | 1.5 | 1.4 | 2.2 | 2.8 | 8.5 |
| orders -> shipping.quote [local] | 1200 |  | 1.2 | 1.1 | 2.1 | 2.8 | 6.0 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.0 | 1.0 | 1.3 | 1.5 | 13.1 |
| orders -> notifications.send [local] | 1179 |  | 0.9 | 0.9 | 1.1 | 1.3 | 4.7 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.7 | 0.8 | 4.6 |
| orders -> pricing.quote [local] | 1200 |  | 0.8 | 0.7 | 1.3 | 1.9 | 5.3 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.8 | 0.9 | 12.1 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.3 | 0.4 | 0.6 | 2.9 |
| orders -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.5 |
| shipping -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.5 |
| pricing -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.4 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> inventory.release [local] | 21 |  | 1.6 | 1.4 | 2.8 | 3.6 | 3.6 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 7624 calls, mean 0.03 ms, local 16391 calls, mean 0.73 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/modulith-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async outbox)

1200 orders in 5.9 s = 203.9 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.8 | 19.6 | 18.9 | 25.4 | 27.7 | 32.8 | 89.4 |
| placeOrder | declined | 21 | 9.7 | 14.0 | 14.0 | 17.9 | 19.6 | 21.8 | 21.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3794 | 63.7% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.3 | 2.2 | 2.8 | 4.5 | 66.9 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.0 | 1.9 | 2.7 | 3.7 | 8.4 |
| orders -> pricing.quote [remote] | 1200 |  | 1.9 | 1.9 | 2.8 | 3.9 | 68.4 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.7 | 1.6 | 2.1 | 3.3 | 5.6 |
| orders -> shipping.quote [remote] | 1200 |  | 1.5 | 1.4 | 2.4 | 2.9 | 5.3 |
| orders -> notifications.send [local] | 1179 |  | 1.2 | 1.1 | 2.4 | 3.6 | 4.6 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.5 | 1.9 | 3.8 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.7 | 0.8 | 3.1 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.3 | 1.9 | 66.0 |
| orders -> customers.getCustomer [remote] | 808 |  | 0.9 | 0.9 | 1.1 | 1.8 | 4.4 |
| notifications -> customers.getCustomer [remote] | 548 |  | 1.0 | 0.8 | 2.1 | 2.7 | 3.3 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 1.9 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| orders -> inventory.release [remote] | 21 |  | 2.9 | 3.1 | 3.7 | 3.9 | 3.9 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2379 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.7 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 631 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7884 calls, mean 0.02 ms, local 6788 calls, mean 0.51 ms, remote 9343 calls, mean 1.58 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/groups-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async events)

1200 orders in 5.9 s = 204.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.3 | 19.6 | 18.2 | 26.1 | 28.1 | 33.2 | 91.1 |
| placeOrder | declined | 21 | 8.4 | 13.7 | 13.0 | 21.2 | 23.9 | 25.7 | 25.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 4342 | 72.9% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.3 | 2.1 | 2.7 | 4.1 | 68.1 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.0 | 1.9 | 2.7 | 3.9 | 5.3 |
| orders -> pricing.quote [remote] | 1200 |  | 1.9 | 1.9 | 2.7 | 4.1 | 68.0 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.7 | 1.6 | 2.2 | 2.9 | 66.2 |
| orders -> shipping.quote [remote] | 1200 |  | 1.5 | 1.4 | 2.4 | 3.0 | 5.5 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.2 | 1.5 | 1.9 | 66.2 |
| orders -> notifications.send [local] | 1179 |  | 0.9 | 0.9 | 1.1 | 1.3 | 5.2 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.3 | 0.7 | 0.8 | 3.0 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.3 | 1.8 | 65.6 |
| orders -> customers.getCustomer [remote] | 808 |  | 0.9 | 0.9 | 1.1 | 1.3 | 3.5 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 2.9 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.7 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 2.2 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| orders -> inventory.release [remote] | 21 |  | 1.9 | 1.7 | 3.0 | 3.0 | 3.0 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2379 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8432 calls, mean 0.02 ms, local 6788 calls, mean 0.46 ms, remote 8795 calls, mean 1.62 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/groups-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

1200 orders in 5.2 s = 232.1 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.0 | 17.2 | 16.7 | 23.0 | 25.1 | 28.9 | 39.6 |
| placeOrder | declined | 21 | 7.3 | 11.8 | 11.3 | 17.3 | 17.9 | 18.4 | 18.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2987 | 50.1% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.2 | 2.2 | 3.6 | 4.9 | 7.1 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.2 | 2.2 | 2.9 | 4.5 | 8.4 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.0 | 1.9 | 2.9 | 4.1 | 6.2 |
| orders -> pricing.quote [remote] | 1200 |  | 1.9 | 1.9 | 2.8 | 4.5 | 6.6 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.7 | 1.6 | 2.2 | 3.3 | 7.6 |
| orders -> notifications.send [local] | 1179 |  | 1.2 | 1.1 | 2.3 | 3.8 | 4.7 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.5 | 1.7 | 6.6 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.7 | 0.8 | 3.4 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.2 | 2.8 | 5.4 |
| shipping -> customers.getCustomer [remote] | 808 |  | 0.9 | 0.9 | 1.1 | 2.6 | 4.5 |
| pricing -> customers.getCustomer [remote] | 806 |  | 0.9 | 0.9 | 1.1 | 2.5 | 4.6 |
| notifications -> customers.getCustomer [remote] | 549 |  | 1.0 | 0.9 | 2.2 | 2.8 | 3.5 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 3.6 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.2 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.9 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| orders -> inventory.release [remote] | 21 |  | 3.0 | 3.0 | 3.8 | 4.3 | 4.3 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.2 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 630 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 394 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7077 calls, mean 0.02 ms, local 6788 calls, mean 0.53 ms, remote 10150 calls, mean 1.61 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/groups-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

1200 orders in 5.5 s = 218.7 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 11.0 | 18.3 | 17.2 | 24.9 | 26.6 | 30.7 | 79.7 |
| placeOrder | declined | 21 | 7.7 | 12.7 | 11.9 | 19.3 | 20.0 | 20.0 | 20.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3535 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.4 | 2.3 | 3.7 | 5.2 | 8.2 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.4 | 2.3 | 2.9 | 5.5 | 7.0 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.2 | 2.1 | 3.0 | 4.0 | 9.1 |
| orders -> pricing.quote [remote] | 1200 |  | 2.0 | 2.1 | 2.8 | 4.8 | 6.5 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.8 | 1.7 | 2.2 | 3.1 | 58.0 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.2 | 1.5 | 1.7 | 8.0 |
| orders -> notifications.send [local] | 1179 |  | 1.0 | 1.0 | 1.3 | 1.5 | 4.3 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.5 | 0.4 | 0.8 | 0.9 | 3.5 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.2 | 2.4 | 4.6 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.2 | 3.0 | 5.5 |
| shipping -> customers.getCustomer [remote] | 807 |  | 1.0 | 1.0 | 1.2 | 2.4 | 5.5 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 4.1 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.8 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.9 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| orders -> inventory.release [remote] | 21 |  | 2.2 | 2.0 | 3.2 | 3.6 | 3.6 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.1 | 0.0 | 0.1 | 0.2 | 1.8 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.5 |
| shipping -> customers.getCustomer [cache] | 1572 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7625 calls, mean 0.02 ms, local 6788 calls, mean 0.52 ms, remote 9602 calls, mean 1.75 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/groups-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async outbox)

1200 orders in 8.2 s = 146.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 13.7 | 27.4 | 25.9 | 37.4 | 40.7 | 50.7 | 107.2 |
| placeOrder | declined | 21 | 12.2 | 22.6 | 17.4 | 30.7 | 43.2 | 73.1 | 73.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2634 | 44.2% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.2 | 3.8 | 7.4 | 9.2 | 59.0 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.2 | 3.1 | 4.0 | 5.4 | 8.0 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.3 | 1.0 | 2.5 | 3.4 | 54.0 |
| orders -> pricing.quote [remote] | 1200 |  | 2.7 | 2.3 | 4.7 | 6.9 | 58.9 |
| orders -> notifications.send [remote] | 1179 |  | 2.6 | 2.4 | 4.5 | 6.5 | 51.4 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.4 | 2.5 | 3.6 | 5.1 | 8.9 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.2 | 2.1 | 3.0 | 3.7 | 6.8 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.3 | 1.7 | 2.1 | 5.9 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.0 | 1.4 | 1.6 | 4.0 |
| notifications -> customers.getCustomer [remote] | 900 |  | 1.1 | 1.1 | 2.0 | 2.6 | 3.5 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.1 | 1.1 | 1.6 | 2.4 | 4.2 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.4 | 1.7 | 3.6 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.3 | 1.6 | 3.2 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.3 | 1.5 | 4.7 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.2 | 1.2 | 1.9 | 2.8 | 4.0 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.2 | 1.1 | 1.8 | 2.5 | 3.9 |
| orders -> inventory.release [remote] | 21 |  | 2.9 | 2.8 | 3.7 | 3.7 | 3.7 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 279 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6722 calls, mean 0.01 ms, remote 17293 calls, mean 1.89 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/microservices-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async events)

1200 orders in 8.4 s = 142.1 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 11.7 | 28.1 | 26.5 | 38.7 | 42.3 | 65.1 | 93.0 |
| placeOrder | declined | 21 | 11.5 | 23.5 | 19.2 | 32.0 | 39.0 | 76.8 | 76.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2741 | 46.0% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.2 | 3.8 | 7.4 | 9.4 | 63.8 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.3 | 3.2 | 4.2 | 5.6 | 7.1 |
| orders -> notifications.send [remote] | 1179 |  | 3.1 | 2.7 | 4.5 | 8.7 | 64.5 |
| orders -> pricing.quote [remote] | 1200 |  | 2.8 | 2.4 | 4.8 | 6.9 | 63.1 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.3 | 1.0 | 2.5 | 3.1 | 61.3 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.6 | 2.6 | 3.7 | 4.5 | 64.2 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.3 | 2.1 | 3.0 | 4.1 | 61.3 |
| orders -> payments.capture [remote] | 1179 |  | 1.4 | 1.3 | 1.7 | 2.6 | 4.5 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.0 | 1.4 | 1.7 | 4.2 |
| notifications -> customers.getCustomer [remote] | 793 |  | 1.3 | 1.1 | 1.6 | 2.4 | 59.2 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.2 | 1.1 | 1.6 | 2.1 | 3.3 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 2.1 | 62.5 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 2.0 | 4.1 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.3 | 1.5 | 4.5 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.3 | 1.2 | 1.9 | 2.7 | 60.3 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.2 | 1.1 | 1.7 | 2.3 | 2.7 |
| orders -> inventory.release [remote] | 21 |  | 2.1 | 2.1 | 2.7 | 2.7 | 2.7 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 386 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6829 calls, mean 0.01 ms, remote 17186 calls, mean 1.98 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/microservices-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

1200 orders in 7.0 s = 172.6 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 11.2 | 23.2 | 22.0 | 31.8 | 34.7 | 41.8 | 81.5 |
| placeOrder | declined | 21 | 8.4 | 16.0 | 14.6 | 22.2 | 24.6 | 37.7 | 37.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2637 | 44.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.2 | 3.8 | 7.4 | 9.3 | 12.1 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.3 | 3.2 | 4.4 | 6.3 | 9.1 |
| orders -> pricing.quote [remote] | 1200 |  | 2.8 | 2.4 | 5.0 | 7.2 | 54.6 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.3 | 1.0 | 2.6 | 3.4 | 5.3 |
| orders -> notifications.send [remote] | 1179 |  | 2.5 | 2.3 | 4.5 | 6.4 | 55.0 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.5 | 2.5 | 3.8 | 4.5 | 5.4 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.2 | 2.1 | 3.1 | 3.8 | 6.8 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.3 | 1.7 | 2.1 | 6.6 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.0 | 1.4 | 1.7 | 4.6 |
| notifications -> customers.getCustomer [remote] | 897 |  | 1.1 | 1.0 | 1.9 | 2.6 | 3.1 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.1 | 1.1 | 1.7 | 2.1 | 3.6 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 2.1 | 5.3 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.4 | 1.8 | 3.9 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.3 | 1.5 | 3.0 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.3 | 1.2 | 2.0 | 2.6 | 3.7 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.2 | 1.2 | 1.8 | 2.3 | 4.3 |
| orders -> inventory.release [remote] | 21 |  | 3.0 | 2.9 | 4.0 | 4.2 | 4.2 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.8 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 282 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6725 calls, mean 0.01 ms, remote 17290 calls, mean 1.90 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/microservices-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

1200 orders in 7.4 s = 162.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 12.7 | 24.7 | 22.4 | 34.1 | 36.6 | 78.9 | 96.2 |
| placeOrder | declined | 21 | 10.2 | 16.7 | 15.1 | 24.2 | 24.4 | 34.9 | 34.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2741 | 46.0% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.5 | 4.1 | 7.7 | 9.5 | 63.5 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.4 | 3.3 | 4.3 | 5.8 | 13.4 |
| orders -> notifications.send [remote] | 1179 |  | 3.2 | 2.8 | 4.6 | 7.6 | 58.5 |
| orders -> pricing.quote [remote] | 1200 |  | 2.9 | 2.5 | 5.0 | 6.7 | 57.4 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.4 | 1.1 | 2.7 | 3.5 | 59.5 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.7 | 2.6 | 3.6 | 4.6 | 60.2 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.3 | 2.2 | 3.2 | 3.8 | 58.7 |
| orders -> payments.capture [remote] | 1179 |  | 1.5 | 1.4 | 1.8 | 3.6 | 54.1 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.1 | 1.4 | 1.7 | 4.1 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.3 | 1.1 | 1.6 | 2.0 | 55.7 |
| notifications -> customers.getCustomer [remote] | 793 |  | 1.2 | 1.1 | 1.7 | 2.5 | 3.8 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.1 | 1.5 | 2.9 | 3.9 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.1 | 1.5 | 2.6 | 4.2 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.5 | 1.2 | 2.0 | 3.0 | 57.3 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 1.8 | 4.1 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.2 | 1.2 | 1.8 | 2.1 | 4.4 |
| orders -> inventory.release [remote] | 21 |  | 2.4 | 2.3 | 3.5 | 4.4 | 4.4 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 386 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6829 calls, mean 0.01 ms, remote 17186 calls, mean 2.07 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T151037Z/microservices-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
