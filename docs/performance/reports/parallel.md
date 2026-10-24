# Performance run 20261024T150139Z

- Started: 2026-10-24T15:01:39+00:00
- Commit: 2c801ac (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1200 orders, 4 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-http,split-dbs,microservices-macro --parallel both --orders 1200 --warmup 200 --concurrency 4`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http parallel off | 193 | 1179 | 21 | 10.9 | 20.8 | 20.5 | 26.9 | 28.7 | 32.9 | 42.8 | 20.0 | PASS |
| loopback-http parallel on | 211 | 1179 | 21 | 9.3 | 19.0 | 18.4 | 24.9 | 26.9 | 30.7 | 38.5 | 20.0 | PASS |
| split-dbs parallel off | 203 | 1179 | 21 | 10.6 | 19.7 | 18.7 | 26.3 | 28.7 | 32.6 | 100.4 | 20.0 | PASS |
| split-dbs parallel on | 227 | 1179 | 21 | 10.6 | 17.7 | 16.9 | 24.1 | 26.1 | 29.2 | 35.6 | 20.0 | PASS |
| microservices-macro parallel off | 143 | 1179 | 21 | 13.9 | 28.0 | 26.8 | 37.9 | 41.4 | 50.0 | 110.8 | 20.0 | PASS |
| microservices-macro parallel on | 169 | 1179 | 21 | 12.7 | 23.6 | 22.7 | 31.4 | 34.0 | 40.5 | 88.6 | 20.0 | PASS |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http | 193 | 211 | +9% | 20.5 | 18.4 | -10% | 28.7 | 26.9 | -6% | 32.9 | 30.7 | -7% |
| split-dbs | 203 | 227 | +12% | 18.7 | 16.9 | -10% | 28.7 | 26.1 | -9% | 32.6 | 29.2 | -10% |
| microservices-macro | 143 | 169 | +18% | 26.8 | 22.7 | -15% | 41.4 | 34.0 | -18% | 50.0 | 40.5 | -19% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http parallel off | loopback-http parallel on | split-dbs parallel off | split-dbs parallel on | microservices-macro parallel off | microservices-macro parallel on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 2.7 | 3.3 | 1.5 | 2.3 | 4.1 | 4.3 |
| orders -> shipping.createShipment | 2.7 | 2.7 | 2.2 | 2.3 | 3.3 | 3.3 |
| orders -> pricing.quote | 1.3 | 1.9 | 2.0 | 1.9 | 2.8 | 2.9 |
| orders -> inventory.reserve | 2.0 | 2.0 | 2.0 | 2.0 | 2.2 | 2.3 |
| orders -> payments.authorize | 1.9 | 1.9 | 1.7 | 1.7 | 2.6 | 2.5 |
| shipping -> inventory.getStock | 1.0 | 1.0 | 0.4 | 0.4 | 1.3 | 1.3 |
| orders -> notifications.send | 1.7 | 1.7 | 1.2 | 1.2 | 2.6 | 2.6 |
| orders -> payments.capture | 1.1 | 1.2 | 1.2 | 1.2 | 1.4 | 1.4 |
| shipping -> inventory.getReservation | 0.9 | 0.9 | 0.4 | 0.4 | 1.1 | 1.1 |
| orders -> customers.getCustomer | 0.6 | 0.6 | 0.6 | 0.7 | 0.7 | 0.8 |
| notifications -> customers.getCustomer | 0.4 | 0.4 | 0.5 | 0.5 | 0.9 | 0.9 |
| pricing -> customers.getCustomer | 0.0 | 0.5 | 0.7 | 0.7 | 0.7 | 0.7 |
| payments -> customers.getPaymentMethod | 0.5 | 0.5 | 0.2 | 0.2 | 0.8 | 0.8 |
| shipping -> customers.getCustomer | 0.0 | 0.3 | 0.0 | 0.3 | 0.3 | 0.4 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.1 | 0.1 | 0.3 | 0.3 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.1 | 0.1 | 0.3 | 0.3 |
| orders -> inventory.release | 2.2 | 2.2 | 2.9 | 3.0 | 3.1 | 2.9 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http parallel off | loopback-http parallel on | split-dbs parallel off | split-dbs parallel on | microservices-macro parallel off | microservices-macro parallel on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 5.0 | 5.9 | 2.3 | 3.6 | 7.3 | 7.6 |
| orders -> shipping.createShipment | 3.2 | 3.3 | 2.7 | 2.8 | 4.1 | 4.2 |
| orders -> pricing.quote | 2.5 | 3.4 | 2.7 | 2.8 | 4.7 | 4.9 |
| orders -> inventory.reserve | 2.6 | 2.9 | 2.6 | 2.7 | 3.0 | 3.2 |
| orders -> payments.authorize | 2.4 | 2.4 | 2.2 | 2.3 | 3.6 | 3.7 |
| shipping -> inventory.getStock | 1.7 | 1.7 | 0.7 | 0.8 | 2.5 | 2.6 |
| orders -> notifications.send | 2.7 | 2.7 | 2.3 | 2.3 | 4.5 | 4.4 |
| orders -> payments.capture | 1.3 | 1.4 | 1.5 | 1.6 | 1.7 | 1.8 |
| shipping -> inventory.getReservation | 1.0 | 1.0 | 0.5 | 0.5 | 1.4 | 1.4 |
| orders -> customers.getCustomer | 0.9 | 1.0 | 1.0 | 1.1 | 1.3 | 1.4 |
| notifications -> customers.getCustomer | 1.1 | 1.1 | 1.3 | 1.4 | 1.8 | 1.8 |
| pricing -> customers.getCustomer | 0.0 | 0.9 | 1.2 | 1.1 | 1.3 | 1.3 |
| payments -> customers.getPaymentMethod | 0.9 | 0.9 | 0.4 | 0.4 | 1.5 | 1.6 |
| shipping -> customers.getCustomer | 0.0 | 0.9 | 0.0 | 1.1 | 1.2 | 1.2 |
| pricing -> catalog.getProduct | 0.8 | 0.9 | 0.3 | 0.4 | 1.4 | 1.4 |
| inventory -> catalog.productExists | 0.8 | 0.8 | 0.3 | 0.4 | 1.4 | 1.5 |
| orders -> inventory.release | 3.1 | 3.2 | 3.8 | 3.9 | 4.1 | 3.9 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-http parallel off | loopback-http parallel on | split-dbs parallel off | split-dbs parallel on | microservices-macro parallel off | microservices-macro parallel on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 75.8% | 75.8% | 75.8% | 75.8% | 75.7% | 75.7% |
| catalog.productExists | 75.8% | 75.8% | 75.8% | 75.8% | 75.7% | 75.7% |
| customers.getCustomer | 77.2% | 49.8% | 63.5% | 50.1% | 44.2% | 44.3% |
| customers.getPaymentMethod | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-http parallel off | 8687 calls, 0.00 ms | - | - | 15328 calls, 1.45 ms | - |
| loopback-http parallel on | 7059 calls, 0.00 ms | - | - | 16956 calls, 1.47 ms | - |
| split-dbs parallel off | 7876 calls, 0.02 ms | 6788 calls, 0.51 ms | - | - | 9351 calls, 1.58 ms |
| split-dbs parallel on | 7076 calls, 0.02 ms | 6788 calls, 0.54 ms | - | - | 10151 calls, 1.65 ms |
| microservices-macro parallel off | 6723 calls, 0.01 ms | - | - | - | 17292 calls, 1.92 ms |
| microservices-macro parallel on | 6723 calls, 0.01 ms | - | - | - | 17292 calls, 1.94 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-http (cache on, parallel quotes off, async outbox)

1200 orders in 6.2 s = 192.7 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.9 | 20.8 | 20.5 | 26.9 | 28.7 | 32.9 | 42.8 |
| placeOrder | declined | 21 | 9.0 | 15.0 | 13.8 | 20.6 | 21.0 | 27.5 | 27.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 4597 | 77.2% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1200 |  | 2.7 | 2.3 | 5.0 | 6.1 | 7.9 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 2.7 | 2.6 | 3.2 | 6.2 | 10.7 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 1.0 | 0.8 | 1.7 | 1.9 | 5.1 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 2.0 | 1.9 | 2.6 | 4.8 | 6.0 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 1.9 | 2.0 | 2.4 | 4.3 | 6.9 |
| orders -> notifications.send [loopback-http] | 1179 |  | 1.7 | 1.6 | 2.7 | 3.8 | 5.0 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 1.3 | 1.0 | 2.5 | 4.1 | 6.2 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.1 | 1.1 | 1.3 | 1.6 | 4.8 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 0.9 | 0.9 | 1.0 | 1.3 | 4.6 |
| orders -> customers.getCustomer [loopback-http] | 808 |  | 0.8 | 0.8 | 0.9 | 1.7 | 4.4 |
| payments -> customers.getPaymentMethod [loopback-http] | 808 |  | 0.8 | 0.8 | 0.9 | 1.0 | 4.1 |
| pricing -> catalog.getProduct [loopback-http] | 591 |  | 0.8 | 0.8 | 0.9 | 1.1 | 4.3 |
| inventory -> catalog.productExists [loopback-http] | 591 |  | 0.8 | 0.8 | 0.9 | 1.0 | 4.2 |
| notifications -> customers.getCustomer [loopback-http] | 553 |  | 0.8 | 0.8 | 1.4 | 1.6 | 2.8 |
| orders -> inventory.release [loopback-http] | 21 |  | 2.2 | 2.0 | 3.1 | 4.0 | 4.0 |
| shipping -> customers.getCustomer [cache] | 2379 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1200 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 626 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8687 calls, mean 0.00 ms, loopback-http 15328 calls, mean 1.45 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T150139Z/loopback-http-parallel-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox)

1200 orders in 5.7 s = 210.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.3 | 19.0 | 18.4 | 24.9 | 26.9 | 30.7 | 38.5 |
| placeOrder | declined | 21 | 8.1 | 13.4 | 11.8 | 18.4 | 22.5 | 27.9 | 27.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2969 | 49.8% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1200 |  | 3.3 | 3.0 | 5.9 | 7.9 | 10.2 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 2.7 | 2.6 | 3.3 | 6.6 | 8.9 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 1.0 | 0.8 | 1.7 | 3.5 | 5.8 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 2.0 | 1.9 | 2.9 | 5.3 | 12.9 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 1.9 | 1.8 | 3.4 | 5.8 | 8.2 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 1.9 | 1.9 | 2.4 | 5.1 | 7.2 |
| orders -> notifications.send [loopback-http] | 1179 |  | 1.7 | 1.7 | 2.7 | 3.9 | 6.7 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.2 | 1.1 | 1.4 | 4.4 | 7.1 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 0.9 | 0.9 | 1.0 | 1.5 | 5.3 |
| orders -> customers.getCustomer [loopback-http] | 808 |  | 0.9 | 0.8 | 1.0 | 3.7 | 5.8 |
| payments -> customers.getPaymentMethod [loopback-http] | 808 |  | 0.8 | 0.8 | 0.9 | 3.9 | 4.6 |
| shipping -> customers.getCustomer [loopback-http] | 808 |  | 0.8 | 0.8 | 0.9 | 1.2 | 4.5 |
| pricing -> customers.getCustomer [loopback-http] | 808 |  | 0.8 | 0.8 | 0.9 | 1.3 | 4.5 |
| pricing -> catalog.getProduct [loopback-http] | 591 |  | 0.8 | 0.8 | 1.0 | 3.9 | 4.7 |
| notifications -> customers.getCustomer [loopback-http] | 565 |  | 0.8 | 0.7 | 1.3 | 1.7 | 5.2 |
| inventory -> catalog.productExists [loopback-http] | 591 |  | 0.8 | 0.8 | 0.9 | 1.3 | 4.2 |
| orders -> inventory.release [loopback-http] | 21 |  | 2.2 | 2.0 | 3.2 | 3.6 | 3.6 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 614 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7059 calls, mean 0.00 ms, loopback-http 16956 calls, mean 1.47 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T150139Z/loopback-http-parallel-on` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes off, async outbox)

1200 orders in 5.9 s = 203.2 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.6 | 19.7 | 18.7 | 26.3 | 28.7 | 32.6 | 100.4 |
| placeOrder | declined | 21 | 8.3 | 14.9 | 13.5 | 20.2 | 21.7 | 26.7 | 26.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3786 | 63.5% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.2 | 2.2 | 2.7 | 3.6 | 6.9 |
| orders -> pricing.quote [remote] | 1200 |  | 2.0 | 1.9 | 2.7 | 4.6 | 69.5 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.0 | 1.9 | 2.6 | 3.4 | 6.1 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.7 | 1.7 | 2.2 | 3.3 | 6.0 |
| orders -> shipping.quote [remote] | 1200 |  | 1.5 | 1.4 | 2.3 | 2.8 | 6.0 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.5 | 2.5 | 11.9 |
| orders -> notifications.send [local] | 1179 |  | 1.2 | 1.2 | 2.3 | 3.7 | 4.2 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.3 | 0.7 | 0.8 | 0.9 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.1 | 0.9 | 1.3 | 2.7 | 66.7 |
| orders -> customers.getCustomer [remote] | 808 |  | 0.9 | 0.9 | 1.1 | 2.0 | 4.9 |
| notifications -> customers.getCustomer [remote] | 556 |  | 1.0 | 0.9 | 2.1 | 2.5 | 3.0 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 3.3 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.7 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| orders -> inventory.release [remote] | 21 |  | 2.9 | 2.9 | 3.8 | 4.3 | 4.3 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2379 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 623 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7876 calls, mean 0.02 ms, local 6788 calls, mean 0.51 ms, remote 9351 calls, mean 1.58 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T150139Z/split-dbs-parallel-off` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox)

1200 orders in 5.3 s = 226.7 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.6 | 17.7 | 16.9 | 24.1 | 26.1 | 29.2 | 35.6 |
| placeOrder | declined | 21 | 7.3 | 12.1 | 11.9 | 17.9 | 19.7 | 20.4 | 20.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2986 | 50.1% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.3 | 2.3 | 3.6 | 5.1 | 8.1 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.3 | 2.2 | 2.8 | 4.1 | 5.7 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.0 | 1.9 | 2.7 | 4.1 | 8.9 |
| orders -> pricing.quote [remote] | 1200 |  | 1.9 | 2.0 | 2.8 | 4.9 | 8.1 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.7 | 1.7 | 2.3 | 3.3 | 8.8 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.6 | 1.9 | 4.5 |
| orders -> notifications.send [local] | 1179 |  | 1.2 | 1.2 | 2.3 | 3.8 | 4.8 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.8 | 0.8 | 4.3 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.2 | 2.2 | 4.5 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.2 | 3.3 | 5.4 |
| pricing -> customers.getCustomer [remote] | 807 |  | 1.0 | 0.9 | 1.2 | 3.2 | 5.2 |
| notifications -> customers.getCustomer [remote] | 549 |  | 1.0 | 0.9 | 2.2 | 2.9 | 3.5 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 0.8 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.5 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 3.6 |
| orders -> inventory.release [remote] | 21 |  | 3.0 | 3.0 | 3.9 | 3.9 | 3.9 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 630 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 393 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7076 calls, mean 0.02 ms, local 6788 calls, mean 0.54 ms, remote 10151 calls, mean 1.65 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T150139Z/split-dbs-parallel-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes off, async outbox)

1200 orders in 8.4 s = 143.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 13.9 | 28.0 | 26.8 | 37.9 | 41.4 | 50.0 | 110.8 |
| placeOrder | declined | 21 | 10.0 | 20.6 | 19.0 | 30.0 | 31.6 | 47.0 | 47.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2635 | 44.2% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.1 | 3.8 | 7.3 | 8.9 | 11.9 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.3 | 3.2 | 4.1 | 5.0 | 7.1 |
| orders -> pricing.quote [remote] | 1200 |  | 2.8 | 2.5 | 4.7 | 6.2 | 54.7 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.3 | 1.0 | 2.5 | 3.5 | 4.9 |
| orders -> notifications.send [remote] | 1179 |  | 2.6 | 2.4 | 4.5 | 6.4 | 61.4 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.6 | 2.6 | 3.6 | 4.7 | 60.8 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.2 | 2.1 | 3.0 | 3.9 | 5.4 |
| orders -> payments.capture [remote] | 1179 |  | 1.4 | 1.3 | 1.7 | 2.1 | 56.9 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.1 | 1.4 | 1.6 | 4.6 |
| notifications -> customers.getCustomer [remote] | 899 |  | 1.2 | 1.1 | 2.0 | 2.7 | 3.1 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.2 | 1.1 | 1.6 | 1.9 | 58.2 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 1.7 | 3.9 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.4 | 2.0 | 5.5 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.3 | 1.4 | 3.6 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.3 | 1.1 | 1.7 | 2.4 | 51.2 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.2 | 1.2 | 1.8 | 2.6 | 3.4 |
| orders -> inventory.release [remote] | 21 |  | 3.1 | 2.8 | 4.1 | 5.6 | 5.6 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 280 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 6723 calls, mean 0.01 ms, remote 17292 calls, mean 1.92 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T150139Z/microservices-macro-parallel-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox)

1200 orders in 7.1 s = 169.3 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 12.7 | 23.6 | 22.7 | 31.4 | 34.0 | 40.5 | 88.6 |
| placeOrder | declined | 21 | 9.1 | 16.7 | 17.0 | 23.6 | 29.8 | 33.1 | 33.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1847 | 75.7% |
| catalog.productExists | 2440 | 1847 | 75.7% |
| customers.getCustomer | 5958 | 2637 | 44.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.3 | 3.9 | 7.6 | 9.6 | 15.3 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.3 | 3.2 | 4.2 | 5.9 | 8.5 |
| orders -> pricing.quote [remote] | 1200 |  | 2.9 | 2.5 | 4.9 | 6.7 | 66.3 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.3 | 1.0 | 2.6 | 3.5 | 5.1 |
| orders -> notifications.send [remote] | 1179 |  | 2.6 | 2.3 | 4.4 | 6.6 | 66.8 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.5 | 2.5 | 3.7 | 4.8 | 5.8 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.3 | 2.2 | 3.2 | 4.1 | 8.1 |
| orders -> payments.capture [remote] | 1179 |  | 1.4 | 1.3 | 1.8 | 2.3 | 7.8 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.0 | 1.4 | 1.9 | 4.3 |
| notifications -> customers.getCustomer [remote] | 897 |  | 1.1 | 1.0 | 2.0 | 2.8 | 4.0 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.2 | 1.1 | 1.6 | 2.0 | 3.2 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.1 | 1.5 | 2.0 | 5.3 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.3 | 1.6 | 3.7 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.3 | 1.6 | 4.5 |
| inventory -> catalog.productExists [remote] | 593 |  | 1.3 | 1.2 | 1.9 | 3.3 | 3.6 |
| pricing -> catalog.getProduct [remote] | 593 |  | 1.2 | 1.1 | 1.7 | 2.6 | 4.1 |
| orders -> inventory.release [remote] | 21 |  | 2.9 | 2.7 | 3.9 | 6.2 | 6.2 |
| inventory -> catalog.productExists [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 282 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6723 calls, mean 0.01 ms, remote 17292 calls, mean 1.94 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T150139Z/microservices-macro-parallel-on` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
