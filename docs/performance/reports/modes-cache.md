# Performance run 20261024T143809Z

- Started: 2026-10-24T14:38:09+00:00
- Commit: 2c801ac (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1200 orders, 4 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes all --cache both --orders 1200 --warmup 200 --concurrency 4`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith cache off | 332 | 1179 | 21 | 6.9 | 12.0 | 11.6 | 16.1 | 17.4 | 20.2 | 25.0 | 20.0 | PASS |
| modulith cache on | 336 | 1179 | 21 | 7.2 | 11.9 | 11.6 | 15.2 | 16.4 | 18.3 | 25.7 | 20.0 | PASS |
| macro-modulith cache off | 306 | 1179 | 21 | 7.7 | 13.1 | 12.7 | 17.0 | 18.0 | 20.0 | 26.1 | 20.0 | PASS |
| macro-modulith cache on | 331 | 1179 | 21 | 7.3 | 12.1 | 11.6 | 15.2 | 16.3 | 18.8 | 83.9 | 20.0 | PASS |
| groups cache off | 200 | 1179 | 21 | 12.2 | 20.1 | 18.6 | 27.6 | 29.6 | 33.1 | 91.2 | 20.0 | PASS |
| groups cache on | 225 | 1179 | 21 | 10.2 | 17.8 | 17.4 | 23.1 | 24.9 | 28.7 | 37.7 | 20.0 | PASS |
| microservices cache off | 140 | 1179 | 21 | 14.2 | 28.7 | 26.1 | 40.2 | 43.6 | 70.4 | 149.5 | 20.0 | PASS |
| microservices cache on | 166 | 1179 | 21 | 12.2 | 24.1 | 22.7 | 31.9 | 34.9 | 77.0 | 100.6 | 20.0 | PASS |
| microservices-macro cache off | 142 | 1179 | 21 | 16.0 | 28.3 | 26.6 | 39.1 | 41.7 | 47.1 | 101.7 | 20.0 | PASS |
| microservices-macro cache on | 178 | 1179 | 21 | 10.4 | 22.4 | 21.5 | 30.5 | 33.7 | 44.7 | 91.0 | 20.0 | PASS |
| loopback-codec cache off | 286 | 1179 | 21 | 8.7 | 14.0 | 13.7 | 18.1 | 19.3 | 22.0 | 31.2 | 20.0 | PASS |
| loopback-codec cache on | 314 | 1179 | 21 | 7.2 | 12.7 | 12.4 | 16.3 | 17.3 | 20.2 | 32.2 | 20.0 | PASS |
| loopback-http cache off | 188 | 1179 | 21 | 11.5 | 21.3 | 20.7 | 28.7 | 31.3 | 36.1 | 42.5 | 20.0 | PASS |
| loopback-http cache on | 238 | 1179 | 21 | 9.4 | 16.8 | 16.3 | 22.0 | 24.3 | 27.9 | 36.0 | 20.0 | PASS |
| split-dbs cache off | 201 | 1179 | 21 | 11.1 | 19.9 | 19.0 | 27.4 | 29.8 | 33.8 | 42.0 | 20.0 | PASS |
| split-dbs cache on | 230 | 1179 | 21 | 10.0 | 17.4 | 16.7 | 23.4 | 25.0 | 28.0 | 38.5 | 20.0 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith | 332 | 336 | +1% | 11.6 | 11.6 | +0% | 17.4 | 16.4 | -6% | 20.2 | 18.3 | -10% |
| macro-modulith | 306 | 331 | +8% | 12.7 | 11.6 | -8% | 18.0 | 16.3 | -9% | 20.0 | 18.8 | -6% |
| groups | 200 | 225 | +13% | 18.6 | 17.4 | -6% | 29.6 | 24.9 | -16% | 33.1 | 28.7 | -13% |
| microservices | 140 | 166 | +18% | 26.1 | 22.7 | -13% | 43.6 | 34.9 | -20% | 70.4 | 77.0 | +9% |
| microservices-macro | 142 | 178 | +26% | 26.6 | 21.5 | -19% | 41.7 | 33.7 | -19% | 47.1 | 44.7 | -5% |
| loopback-codec | 286 | 314 | +10% | 13.7 | 12.4 | -10% | 19.3 | 17.3 | -10% | 22.0 | 20.2 | -8% |
| loopback-http | 188 | 238 | +26% | 20.7 | 16.3 | -21% | 31.3 | 24.3 | -22% | 36.1 | 27.9 | -23% |
| split-dbs | 201 | 230 | +15% | 19.0 | 16.7 | -12% | 29.8 | 25.0 | -16% | 33.8 | 28.0 | -17% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.5 | 1.2 | 1.7 | 1.2 | 3.1 | 2.3 | 6.3 | 4.3 | 6.1 | 4.2 | 1.9 | 1.3 | 4.5 | 2.9 | 3.0 | 2.3 |
| orders -> shipping.createShipment | 1.7 | 1.6 | 1.9 | 1.7 | 3.2 | 2.3 | 4.3 | 3.3 | 4.4 | 3.1 | 2.1 | 1.7 | 3.3 | 2.4 | 3.2 | 2.3 |
| orders -> pricing.quote | 1.1 | 0.8 | 1.2 | 0.8 | 2.7 | 1.9 | 4.6 | 2.9 | 4.4 | 2.8 | 1.5 | 0.9 | 3.2 | 1.7 | 2.6 | 1.9 |
| orders -> inventory.reserve | 1.3 | 1.4 | 1.4 | 1.4 | 2.1 | 2.1 | 2.3 | 2.3 | 2.3 | 2.1 | 1.5 | 1.5 | 1.9 | 1.7 | 2.1 | 2.0 |
| shipping -> inventory.getStock | 0.5 | 0.4 | 0.6 | 0.4 | 0.7 | 0.4 | 2.1 | 1.4 | 2.1 | 1.3 | 0.7 | 0.4 | 1.5 | 0.9 | 0.7 | 0.4 |
| orders -> payments.authorize | 1.0 | 1.0 | 1.1 | 1.0 | 1.8 | 1.8 | 2.9 | 2.7 | 2.9 | 2.5 | 1.2 | 1.1 | 2.0 | 1.7 | 1.8 | 1.7 |
| orders -> notifications.send | 0.9 | 0.8 | 0.9 | 0.8 | 1.7 | 1.2 | 2.9 | 2.6 | 2.7 | 2.5 | 1.0 | 0.9 | 2.1 | 1.6 | 1.7 | 1.2 |
| orders -> payments.capture | 0.6 | 0.6 | 0.6 | 0.6 | 1.2 | 1.2 | 1.4 | 1.5 | 1.5 | 1.3 | 0.7 | 0.7 | 1.1 | 1.0 | 1.2 | 1.2 |
| shipping -> customers.getCustomer | 0.3 | 0.1 | 0.3 | 0.1 | 0.9 | 0.3 | 1.0 | 0.4 | 1.0 | 0.3 | 0.3 | 0.1 | 0.7 | 0.2 | 0.9 | 0.3 |
| inventory -> catalog.productExists | 0.3 | 0.1 | 0.3 | 0.1 | 0.3 | 0.1 | 1.0 | 0.3 | 1.0 | 0.4 | 0.3 | 0.1 | 0.7 | 0.2 | 0.3 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.1 | 0.3 | 0.1 | 0.3 | 0.1 | 1.0 | 0.3 | 1.0 | 0.3 | 0.3 | 0.1 | 0.7 | 0.2 | 0.3 | 0.1 |
| orders -> customers.getCustomer | 0.3 | 0.2 | 0.3 | 0.2 | 0.9 | 0.7 | 1.1 | 0.7 | 1.0 | 0.8 | 0.4 | 0.3 | 0.8 | 0.5 | 0.9 | 0.7 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 1.1 | 1.1 | 1.1 | 1.0 | 0.4 | 0.4 | 0.8 | 0.8 | 0.4 | 0.4 |
| pricing -> customers.getCustomer | 0.3 | 0.2 | 0.3 | 0.3 | 0.9 | 0.6 | 1.1 | 0.8 | 1.0 | 0.7 | 0.3 | 0.3 | 0.8 | 0.5 | 0.9 | 0.6 |
| notifications -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.9 | 0.5 | 1.1 | 0.9 | 1.0 | 0.8 | 0.3 | 0.1 | 0.8 | 0.4 | 0.9 | 0.5 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.3 | 0.2 | 0.3 | 0.2 | 1.1 | 0.8 | 1.1 | 0.8 | 0.3 | 0.2 | 0.7 | 0.5 | 0.3 | 0.2 |
| orders -> inventory.release | 1.4 | 1.4 | 1.6 | 1.5 | 2.8 | 3.2 | 2.9 | 2.9 | 3.0 | 2.9 | 1.5 | 1.5 | 2.2 | 2.4 | 3.0 | 3.0 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 2.6 | 2.1 | 2.9 | 2.1 | 4.3 | 3.6 | 10.1 | 7.6 | 10.0 | 7.8 | 3.2 | 2.3 | 8.0 | 5.3 | 4.3 | 3.5 |
| orders -> shipping.createShipment | 2.2 | 2.1 | 2.3 | 2.1 | 3.8 | 2.8 | 5.4 | 4.1 | 5.4 | 4.3 | 2.5 | 2.2 | 4.0 | 3.0 | 3.9 | 2.8 |
| orders -> pricing.quote | 1.7 | 1.2 | 1.8 | 1.2 | 3.3 | 2.7 | 6.6 | 4.9 | 6.4 | 5.1 | 2.2 | 1.4 | 5.1 | 3.0 | 3.4 | 2.8 |
| orders -> inventory.reserve | 2.0 | 2.1 | 2.1 | 2.1 | 2.9 | 2.8 | 3.2 | 3.1 | 3.2 | 3.0 | 2.2 | 2.2 | 2.7 | 2.4 | 2.9 | 2.8 |
| shipping -> inventory.getStock | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.7 | 2.7 | 2.7 | 2.7 | 2.8 | 0.8 | 0.7 | 1.8 | 1.5 | 0.8 | 0.8 |
| orders -> payments.authorize | 1.2 | 1.2 | 1.3 | 1.3 | 2.2 | 2.2 | 3.8 | 3.7 | 3.7 | 3.8 | 1.4 | 1.4 | 2.4 | 2.3 | 2.3 | 2.2 |
| orders -> notifications.send | 1.5 | 1.4 | 1.5 | 1.3 | 3.1 | 2.3 | 5.2 | 4.4 | 4.9 | 4.3 | 1.7 | 1.4 | 3.5 | 2.4 | 3.0 | 2.4 |
| orders -> payments.capture | 0.7 | 0.8 | 0.8 | 0.8 | 1.4 | 1.5 | 1.8 | 1.7 | 1.8 | 1.8 | 0.8 | 0.8 | 1.3 | 1.2 | 1.5 | 1.5 |
| shipping -> customers.getCustomer | 0.3 | 0.3 | 0.4 | 0.3 | 1.1 | 1.0 | 1.2 | 1.2 | 1.2 | 1.2 | 0.4 | 0.4 | 0.9 | 0.8 | 1.1 | 1.0 |
| inventory -> catalog.productExists | 0.3 | 0.3 | 0.4 | 0.3 | 0.4 | 0.4 | 1.4 | 1.5 | 1.4 | 1.7 | 0.4 | 0.4 | 0.9 | 0.7 | 0.4 | 0.4 |
| pricing -> catalog.getProduct | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.3 | 1.3 | 1.5 | 1.3 | 1.5 | 0.4 | 0.4 | 0.9 | 0.8 | 0.4 | 0.4 |
| orders -> customers.getCustomer | 0.4 | 0.4 | 0.4 | 0.4 | 1.1 | 1.2 | 1.3 | 1.4 | 1.3 | 1.4 | 0.4 | 0.4 | 1.0 | 0.9 | 1.2 | 1.1 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.5 | 0.4 | 0.5 | 0.5 | 1.4 | 1.4 | 1.4 | 1.3 | 0.5 | 0.5 | 1.0 | 0.9 | 0.5 | 0.5 |
| pricing -> customers.getCustomer | 0.3 | 0.3 | 0.4 | 0.3 | 1.1 | 1.1 | 1.3 | 1.3 | 1.3 | 1.3 | 0.4 | 0.4 | 0.9 | 0.8 | 1.1 | 1.1 |
| notifications -> customers.getCustomer | 0.4 | 0.3 | 0.4 | 0.3 | 1.7 | 1.4 | 1.9 | 1.8 | 1.8 | 1.7 | 0.5 | 0.4 | 1.3 | 0.9 | 1.7 | 1.4 |
| payments -> customers.getPaymentMethod | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 1.5 | 1.6 | 1.5 | 1.6 | 0.4 | 0.4 | 0.9 | 0.8 | 0.4 | 0.4 |
| orders -> inventory.release | 2.0 | 1.9 | 2.7 | 2.2 | 3.3 | 3.8 | 3.5 | 3.8 | 4.1 | 3.9 | 1.9 | 1.9 | 3.1 | 3.3 | 3.9 | 3.6 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | 75.8% | - | 75.8% | - | 75.8% | - | 75.7% | - | 75.7% | - | 75.8% | - | 75.8% | - | 75.8% |
| catalog.productExists | - | 75.8% | - | 75.8% | - | 75.8% | - | 75.7% | - | 75.7% | - | 75.8% | - | 75.8% | - | 75.8% |
| customers.getCustomer | - | 49.7% | - | 49.7% | - | 50.1% | - | 44.2% | - | 44.3% | - | 49.7% | - | 49.8% | - | 50.1% |
| customers.getPaymentMethod | - | 32.7% | - | 32.7% | - | 32.7% | - | 32.7% | - | 32.7% | - | 32.7% | - | 32.7% | - | 32.7% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith cache off | - | 24015 calls, 0.60 ms | - | - | - |
| modulith cache on | 7050 calls, 0.03 ms | 16965 calls, 0.69 ms | - | - | - |
| macro-modulith cache off | - | 24015 calls, 0.65 ms | - | - | - |
| macro-modulith cache on | 7050 calls, 0.03 ms | 16965 calls, 0.71 ms | - | - | - |
| groups cache off | - | 10878 calls, 0.54 ms | - | - | 13137 calls, 1.69 ms |
| groups cache on | 7073 calls, 0.02 ms | 6788 calls, 0.54 ms | - | - | 10154 calls, 1.66 ms |
| microservices cache off | - | - | - | - | 24015 calls, 2.02 ms |
| microservices cache on | 6722 calls, 0.01 ms | - | - | - | 17293 calls, 1.98 ms |
| microservices-macro cache off | - | - | - | - | 24015 calls, 1.98 ms |
| microservices-macro cache on | 6726 calls, 0.01 ms | - | - | - | 17289 calls, 1.88 ms |
| loopback-codec cache off | - | - | 24015 calls, 0.74 ms | - | - |
| loopback-codec cache on | 7050 calls, 0.00 ms | - | 16965 calls, 0.77 ms | - | - |
| loopback-http cache off | - | - | - | 24015 calls, 1.47 ms | - |
| loopback-http cache on | 7059 calls, 0.00 ms | - | - | 16956 calls, 1.31 ms | - |
| split-dbs cache off | - | 10878 calls, 0.54 ms | - | - | 13137 calls, 1.67 ms |
| split-dbs cache on | 7075 calls, 0.02 ms | 6788 calls, 0.54 ms | - | - | 10152 calls, 1.63 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache off, parallel quotes on, async outbox)

1200 orders in 3.6 s = 332.4 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 6.9 | 12.0 | 11.6 | 16.1 | 17.4 | 20.2 | 25.0 |
| placeOrder | declined | 21 | 4.8 | 8.0 | 7.5 | 11.7 | 12.4 | 12.5 | 12.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.7 | 1.7 | 2.2 | 3.5 | 7.9 |
| orders -> shipping.quote [local] | 1200 |  | 1.5 | 1.4 | 2.6 | 3.1 | 5.6 |
| orders -> inventory.reserve [local] | 1200 |  | 1.3 | 1.3 | 2.0 | 2.5 | 7.9 |
| orders -> pricing.quote [local] | 1200 |  | 1.1 | 1.0 | 1.7 | 2.0 | 5.0 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.5 | 0.5 | 0.7 | 0.8 | 4.2 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.0 | 1.0 | 1.2 | 1.4 | 7.3 |
| orders -> notifications.send [local] | 1179 |  | 0.9 | 0.8 | 1.5 | 2.1 | 3.5 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.7 | 0.8 | 4.5 |
| inventory -> catalog.productExists [local] | 2440 |  | 0.3 | 0.2 | 0.3 | 0.4 | 3.7 |
| shipping -> customers.getCustomer [local] | 2379 |  | 0.3 | 0.2 | 0.3 | 0.4 | 3.8 |
| pricing -> catalog.getProduct [local] | 2440 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.8 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.3 |
| orders -> customers.getCustomer [local] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.9 |
| pricing -> customers.getCustomer [local] | 1200 |  | 0.3 | 0.3 | 0.3 | 0.4 | 3.8 |
| payments -> customers.getPaymentMethod [local] | 1200 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.6 |
| notifications -> customers.getCustomer [local] | 1179 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.0 |
| orders -> inventory.release [local] | 21 |  | 1.4 | 1.3 | 2.0 | 2.2 | 2.2 |

By binding: local 24015 calls, mean 0.60 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/modulith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox)

1200 orders in 3.6 s = 336.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 7.2 | 11.9 | 11.6 | 15.2 | 16.4 | 18.3 | 25.7 |
| placeOrder | declined | 21 | 5.2 | 8.3 | 8.1 | 11.8 | 12.1 | 12.7 | 12.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2960 | 49.7% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.6 | 1.6 | 2.1 | 2.3 | 5.3 |
| orders -> inventory.reserve [local] | 1200 |  | 1.4 | 1.4 | 2.1 | 2.4 | 7.5 |
| orders -> shipping.quote [local] | 1200 |  | 1.2 | 1.1 | 2.1 | 2.7 | 6.0 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.0 | 1.0 | 1.2 | 1.4 | 8.1 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.3 | 0.7 | 0.7 | 4.9 |
| orders -> notifications.send [local] | 1179 |  | 0.8 | 0.7 | 1.4 | 1.9 | 2.6 |
| orders -> pricing.quote [local] | 1200 |  | 0.8 | 0.7 | 1.2 | 1.6 | 5.4 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.8 | 0.8 | 5.6 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.2 |
| orders -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.2 |
| shipping -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.4 | 4.1 |
| pricing -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.4 | 4.1 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.5 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 4.5 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| notifications -> customers.getCustomer [local] | 574 |  | 0.2 | 0.2 | 0.5 | 0.7 | 1.8 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.1 | 3.6 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> inventory.release [local] | 21 |  | 1.4 | 1.3 | 1.9 | 2.7 | 2.7 |
| notifications -> customers.getCustomer [cache] | 605 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7050 calls, mean 0.03 ms, local 16965 calls, mean 0.69 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/modulith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes on, async outbox)

1200 orders in 3.9 s = 306.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 7.7 | 13.1 | 12.7 | 17.0 | 18.0 | 20.0 | 26.1 |
| placeOrder | declined | 21 | 5.4 | 8.7 | 8.0 | 11.4 | 11.7 | 11.9 | 11.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.9 | 1.8 | 2.3 | 2.7 | 8.3 |
| orders -> shipping.quote [local] | 1200 |  | 1.7 | 1.6 | 2.9 | 3.2 | 5.9 |
| orders -> inventory.reserve [local] | 1200 |  | 1.4 | 1.4 | 2.1 | 2.6 | 8.9 |
| orders -> pricing.quote [local] | 1200 |  | 1.2 | 1.2 | 1.8 | 2.1 | 5.1 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.6 | 0.6 | 0.7 | 0.8 | 4.1 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.1 | 1.1 | 1.3 | 1.4 | 7.4 |
| orders -> notifications.send [local] | 1179 |  | 0.9 | 0.7 | 1.5 | 2.0 | 2.7 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.8 | 0.8 | 2.5 |
| inventory -> catalog.productExists [local] | 2440 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.7 |
| shipping -> customers.getCustomer [local] | 2379 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.4 |
| pricing -> catalog.getProduct [local] | 2440 |  | 0.3 | 0.3 | 0.3 | 0.4 | 3.7 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 2.8 |
| orders -> customers.getCustomer [local] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.9 |
| pricing -> customers.getCustomer [local] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.8 |
| payments -> customers.getPaymentMethod [local] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.4 | 2.6 |
| notifications -> customers.getCustomer [local] | 1179 |  | 0.2 | 0.2 | 0.4 | 0.7 | 1.0 |
| orders -> inventory.release [local] | 21 |  | 1.6 | 1.5 | 2.7 | 2.8 | 2.8 |

By binding: local 24015 calls, mean 0.65 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/macro-modulith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes on, async outbox)

1200 orders in 3.6 s = 330.8 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 7.3 | 12.1 | 11.6 | 15.2 | 16.3 | 18.8 | 83.9 |
| placeOrder | declined | 21 | 5.1 | 8.0 | 7.7 | 10.6 | 11.5 | 12.8 | 12.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2960 | 49.7% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1179 |  | 1.7 | 1.6 | 2.1 | 2.4 | 69.4 |
| orders -> inventory.reserve [local] | 1200 |  | 1.4 | 1.3 | 2.1 | 2.6 | 9.5 |
| orders -> shipping.quote [local] | 1200 |  | 1.2 | 1.1 | 2.1 | 2.6 | 67.4 |
| orders -> payments.authorize [local] | 1200 | 21 | 1.0 | 1.0 | 1.3 | 1.5 | 4.6 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.3 | 0.7 | 0.7 | 66.8 |
| orders -> pricing.quote [local] | 1200 |  | 0.8 | 0.7 | 1.2 | 1.5 | 67.6 |
| orders -> notifications.send [local] | 1179 |  | 0.8 | 0.7 | 1.3 | 1.8 | 2.6 |
| orders -> payments.capture [local] | 1179 |  | 0.6 | 0.6 | 0.8 | 0.8 | 4.0 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.3 | 0.4 | 0.5 | 3.9 |
| pricing -> customers.getCustomer [local] | 808 |  | 0.4 | 0.3 | 0.4 | 0.5 | 66.3 |
| orders -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.5 |
| shipping -> customers.getCustomer [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.4 | 2.6 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.3 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.0 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.7 |
| notifications -> customers.getCustomer [local] | 574 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.8 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> inventory.release [local] | 21 |  | 1.5 | 1.4 | 2.2 | 2.4 | 2.4 |
| notifications -> customers.getCustomer [cache] | 605 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 7050 calls, mean 0.03 ms, local 16965 calls, mean 0.71 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/macro-modulith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async outbox)

1200 orders in 6.0 s = 199.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 12.2 | 20.1 | 18.6 | 27.6 | 29.6 | 33.1 | 91.2 |
| placeOrder | declined | 21 | 8.2 | 14.2 | 13.2 | 21.1 | 21.8 | 24.3 | 24.3 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.2 | 3.1 | 3.8 | 5.9 | 62.6 |
| orders -> shipping.quote [remote] | 1200 |  | 3.1 | 2.9 | 4.3 | 5.7 | 65.8 |
| orders -> pricing.quote [remote] | 1200 |  | 2.7 | 2.5 | 3.3 | 5.1 | 64.9 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.1 | 2.0 | 2.9 | 3.8 | 5.8 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.8 | 1.7 | 2.2 | 3.1 | 8.2 |
| shipping -> customers.getCustomer [remote] | 2379 |  | 0.9 | 0.9 | 1.1 | 2.6 | 4.5 |
| orders -> notifications.send [local] | 1179 |  | 1.7 | 1.6 | 3.1 | 4.2 | 5.4 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.7 | 0.6 | 0.8 | 0.9 | 4.5 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.4 | 1.6 | 3.3 |
| orders -> customers.getCustomer [remote] | 1200 |  | 0.9 | 0.9 | 1.1 | 1.5 | 4.3 |
| pricing -> customers.getCustomer [remote] | 1200 |  | 0.9 | 0.9 | 1.1 | 2.6 | 4.1 |
| notifications -> customers.getCustomer [remote] | 1179 |  | 0.9 | 0.8 | 1.7 | 2.7 | 3.2 |
| inventory -> catalog.productExists [local] | 2440 |  | 0.3 | 0.3 | 0.4 | 0.4 | 4.1 |
| pricing -> catalog.getProduct [local] | 2440 |  | 0.3 | 0.3 | 0.4 | 0.4 | 4.0 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 0.8 |
| payments -> customers.getPaymentMethod [local] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.8 |
| orders -> inventory.release [remote] | 21 |  | 2.8 | 2.8 | 3.3 | 3.5 | 3.5 |

By binding: local 10878 calls, mean 0.54 ms, remote 13137 calls, mean 1.69 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/groups-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

1200 orders in 5.3 s = 225.1 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.2 | 17.8 | 17.4 | 23.1 | 24.9 | 28.7 | 37.7 |
| placeOrder | declined | 21 | 8.1 | 12.2 | 12.3 | 15.3 | 18.0 | 18.1 | 18.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2983 | 50.1% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.3 | 2.2 | 3.6 | 5.3 | 7.0 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.3 | 2.2 | 2.8 | 4.7 | 9.6 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.1 | 2.0 | 2.8 | 3.8 | 6.9 |
| orders -> pricing.quote [remote] | 1200 |  | 1.9 | 2.0 | 2.7 | 4.7 | 6.5 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.8 | 1.7 | 2.2 | 3.4 | 7.8 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.5 | 1.8 | 6.9 |
| orders -> notifications.send [local] | 1179 |  | 1.2 | 1.1 | 2.3 | 4.0 | 5.0 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.7 | 0.8 | 4.4 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.2 | 2.3 | 5.1 |
| pricing -> customers.getCustomer [remote] | 807 |  | 1.0 | 0.9 | 1.1 | 2.5 | 5.1 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.2 | 2.1 | 4.9 |
| notifications -> customers.getCustomer [remote] | 552 |  | 1.0 | 0.8 | 2.0 | 2.9 | 3.1 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 3.5 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| orders -> inventory.release [remote] | 21 |  | 3.2 | 3.1 | 3.8 | 4.3 | 4.3 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.8 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 627 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 393 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7073 calls, mean 0.02 ms, local 6788 calls, mean 0.54 ms, remote 10154 calls, mean 1.66 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/groups-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async outbox)

1200 orders in 8.6 s = 139.8 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 14.2 | 28.7 | 26.1 | 40.2 | 43.6 | 70.4 | 149.5 |
| placeOrder | declined | 21 | 11.0 | 17.7 | 17.4 | 24.5 | 24.9 | 26.2 | 26.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 6.3 | 5.7 | 10.1 | 12.0 | 68.9 |
| orders -> pricing.quote [remote] | 1200 |  | 4.6 | 4.2 | 6.6 | 8.2 | 68.9 |
| shipping -> inventory.getStock [remote] | 2440 |  | 2.1 | 2.0 | 2.7 | 3.7 | 63.4 |
| orders -> shipping.createShipment [remote] | 1179 |  | 4.3 | 4.2 | 5.4 | 6.6 | 48.3 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.9 | 2.8 | 3.8 | 4.9 | 12.1 |
| orders -> notifications.send [remote] | 1179 |  | 2.9 | 2.5 | 5.2 | 6.4 | 66.1 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.3 | 2.2 | 3.2 | 4.2 | 65.0 |
| inventory -> catalog.productExists [remote] | 2440 |  | 1.0 | 1.0 | 1.4 | 1.9 | 61.1 |
| pricing -> catalog.getProduct [remote] | 2440 |  | 1.0 | 0.9 | 1.3 | 1.7 | 47.0 |
| shipping -> customers.getCustomer [remote] | 2379 |  | 1.0 | 0.9 | 1.2 | 1.6 | 5.3 |
| orders -> payments.capture [remote] | 1179 |  | 1.4 | 1.4 | 1.8 | 2.0 | 4.6 |
| orders -> customers.getCustomer [remote] | 1200 |  | 1.1 | 1.0 | 1.3 | 1.7 | 64.9 |
| payments -> customers.getPaymentMethod [remote] | 1200 |  | 1.1 | 1.0 | 1.5 | 2.0 | 4.2 |
| pricing -> customers.getCustomer [remote] | 1200 |  | 1.1 | 1.0 | 1.3 | 1.9 | 47.5 |
| notifications -> customers.getCustomer [remote] | 1179 |  | 1.1 | 1.0 | 1.9 | 2.6 | 4.1 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.0 | 1.4 | 1.6 | 3.4 |
| orders -> inventory.release [remote] | 21 |  | 2.9 | 2.8 | 3.5 | 3.8 | 3.8 |

By binding: remote 24015 calls, mean 2.02 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/microservices-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

1200 orders in 7.3 s = 165.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 12.2 | 24.1 | 22.7 | 31.9 | 34.9 | 77.0 | 100.6 |
| placeOrder | declined | 21 | 9.3 | 20.0 | 14.8 | 29.2 | 38.6 | 72.7 | 72.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1847 | 75.7% |
| catalog.productExists | 2440 | 1847 | 75.7% |
| customers.getCustomer | 5958 | 2636 | 44.2% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.3 | 3.9 | 7.6 | 9.6 | 60.7 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.3 | 3.2 | 4.1 | 5.7 | 58.1 |
| orders -> pricing.quote [remote] | 1200 |  | 2.9 | 2.6 | 4.9 | 7.0 | 59.3 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.4 | 1.0 | 2.7 | 3.6 | 56.4 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.7 | 2.6 | 3.7 | 4.8 | 60.5 |
| orders -> notifications.send [remote] | 1179 |  | 2.6 | 2.4 | 4.4 | 6.5 | 63.2 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.3 | 2.2 | 3.1 | 3.7 | 6.9 |
| orders -> payments.capture [remote] | 1179 |  | 1.5 | 1.3 | 1.7 | 2.2 | 56.2 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.0 | 1.4 | 1.7 | 54.3 |
| notifications -> customers.getCustomer [remote] | 898 |  | 1.1 | 1.1 | 2.0 | 2.6 | 3.9 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.2 | 1.1 | 1.7 | 2.0 | 3.5 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 1.8 | 56.3 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 1.8 | 5.7 |
| inventory -> catalog.productExists [remote] | 593 |  | 1.4 | 1.2 | 2.1 | 3.3 | 54.1 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.3 | 1.5 | 4.6 |
| pricing -> catalog.getProduct [remote] | 593 |  | 1.2 | 1.2 | 1.8 | 2.8 | 3.6 |
| orders -> inventory.release [remote] | 21 |  | 2.9 | 2.7 | 3.8 | 4.1 | 4.1 |
| inventory -> catalog.productExists [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 281 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6722 calls, mean 0.01 ms, remote 17293 calls, mean 1.98 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/microservices-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async outbox)

1200 orders in 8.5 s = 141.6 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 16.0 | 28.3 | 26.6 | 39.1 | 41.7 | 47.1 | 101.7 |
| placeOrder | declined | 21 | 10.6 | 18.7 | 18.7 | 24.4 | 27.8 | 30.7 | 30.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 6.1 | 5.8 | 10.0 | 11.6 | 14.0 |
| orders -> pricing.quote [remote] | 1200 |  | 4.4 | 4.2 | 6.4 | 7.6 | 12.5 |
| orders -> shipping.createShipment [remote] | 1179 |  | 4.4 | 4.3 | 5.4 | 6.9 | 10.3 |
| shipping -> inventory.getStock [remote] | 2440 |  | 2.1 | 2.0 | 2.7 | 4.1 | 5.6 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.9 | 2.8 | 3.7 | 5.0 | 8.0 |
| orders -> notifications.send [remote] | 1179 |  | 2.7 | 2.4 | 4.9 | 6.7 | 55.7 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.3 | 2.2 | 3.2 | 4.3 | 6.1 |
| inventory -> catalog.productExists [remote] | 2440 |  | 1.0 | 1.0 | 1.4 | 1.8 | 3.6 |
| pricing -> catalog.getProduct [remote] | 2440 |  | 1.0 | 0.9 | 1.3 | 1.5 | 3.8 |
| shipping -> customers.getCustomer [remote] | 2379 |  | 1.0 | 0.9 | 1.2 | 1.5 | 4.0 |
| orders -> payments.capture [remote] | 1179 |  | 1.5 | 1.4 | 1.8 | 2.2 | 50.7 |
| payments -> customers.getPaymentMethod [remote] | 1200 |  | 1.1 | 1.1 | 1.5 | 2.2 | 3.4 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.0 | 1.4 | 1.6 | 4.4 |
| orders -> customers.getCustomer [remote] | 1200 |  | 1.0 | 1.0 | 1.3 | 1.8 | 5.2 |
| notifications -> customers.getCustomer [remote] | 1179 |  | 1.0 | 1.0 | 1.8 | 2.4 | 3.6 |
| pricing -> customers.getCustomer [remote] | 1200 |  | 1.0 | 1.0 | 1.3 | 1.6 | 3.3 |
| orders -> inventory.release [remote] | 21 |  | 3.0 | 2.9 | 4.1 | 4.5 | 4.5 |

By binding: remote 24015 calls, mean 1.98 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/microservices-macro-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox)

1200 orders in 6.7 s = 178.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.4 | 22.4 | 21.5 | 30.5 | 33.7 | 44.7 | 91.0 |
| placeOrder | declined | 21 | 7.0 | 17.0 | 14.4 | 26.1 | 27.4 | 39.7 | 39.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2638 | 44.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.2 | 3.7 | 7.8 | 10.7 | 64.9 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.1 | 3.0 | 4.3 | 5.8 | 9.0 |
| orders -> pricing.quote [remote] | 1200 |  | 2.8 | 2.4 | 5.1 | 7.1 | 59.6 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.3 | 1.0 | 2.8 | 4.0 | 49.8 |
| orders -> notifications.send [remote] | 1179 |  | 2.5 | 2.3 | 4.3 | 6.4 | 57.2 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.5 | 2.4 | 3.8 | 4.8 | 59.5 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.1 | 2.0 | 3.0 | 4.0 | 5.3 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.3 | 1.8 | 2.1 | 4.7 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.0 | 0.9 | 1.3 | 1.6 | 4.4 |
| notifications -> customers.getCustomer [remote] | 896 |  | 1.1 | 1.0 | 1.9 | 2.7 | 3.0 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.1 | 1.1 | 1.7 | 2.2 | 3.9 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.5 | 2.3 | 59.7 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.5 | 1.2 | 2.5 | 4.3 | 48.4 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.4 | 1.2 | 2.1 | 4.5 | 48.3 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.4 | 2.0 | 5.1 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.3 | 1.9 | 3.8 |
| orders -> inventory.release [remote] | 21 |  | 2.9 | 2.8 | 3.9 | 4.5 | 4.5 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 283 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6726 calls, mean 0.01 ms, remote 17289 calls, mean 1.88 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/microservices-macro-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async outbox)

1200 orders in 4.2 s = 285.8 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 8.7 | 14.0 | 13.7 | 18.1 | 19.3 | 22.0 | 31.2 |
| placeOrder | declined | 21 | 5.6 | 8.8 | 8.5 | 11.3 | 11.4 | 11.8 | 11.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1179 |  | 2.1 | 2.0 | 2.5 | 5.0 | 12.6 |
| orders -> shipping.quote [loopback-codec] | 1200 |  | 1.9 | 1.8 | 3.2 | 4.4 | 6.1 |
| orders -> inventory.reserve [loopback-codec] | 1200 |  | 1.5 | 1.4 | 2.2 | 2.5 | 5.3 |
| orders -> pricing.quote [loopback-codec] | 1200 |  | 1.5 | 1.4 | 2.2 | 3.9 | 5.2 |
| shipping -> inventory.getStock [loopback-codec] | 2440 |  | 0.7 | 0.7 | 0.8 | 0.9 | 3.9 |
| orders -> payments.authorize [loopback-codec] | 1200 | 21 | 1.2 | 1.2 | 1.4 | 1.5 | 4.1 |
| orders -> notifications.send [loopback-codec] | 1179 |  | 1.0 | 0.9 | 1.7 | 2.2 | 5.2 |
| orders -> payments.capture [loopback-codec] | 1179 |  | 0.7 | 0.6 | 0.8 | 0.9 | 7.4 |
| shipping -> customers.getCustomer [loopback-codec] | 2379 |  | 0.3 | 0.3 | 0.4 | 0.6 | 4.0 |
| pricing -> catalog.getProduct [loopback-codec] | 2440 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.5 |
| inventory -> catalog.productExists [loopback-codec] | 2440 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.5 |
| shipping -> inventory.getReservation [loopback-codec] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 3.6 |
| orders -> customers.getCustomer [loopback-codec] | 1200 |  | 0.4 | 0.4 | 0.4 | 0.6 | 3.8 |
| pricing -> customers.getCustomer [loopback-codec] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.6 | 3.0 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| notifications -> customers.getCustomer [loopback-codec] | 1179 |  | 0.3 | 0.2 | 0.5 | 0.7 | 1.1 |
| orders -> inventory.release [loopback-codec] | 21 |  | 1.5 | 1.4 | 1.9 | 2.5 | 2.5 |

By binding: loopback-codec 24015 calls, mean 0.74 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/loopback-codec-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox)

1200 orders in 3.8 s = 314.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 7.2 | 12.7 | 12.4 | 16.3 | 17.3 | 20.2 | 32.2 |
| placeOrder | declined | 21 | 5.8 | 9.1 | 8.5 | 11.8 | 14.2 | 15.1 | 15.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2960 | 49.7% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1179 |  | 1.7 | 1.7 | 2.2 | 2.5 | 7.5 |
| orders -> inventory.reserve [loopback-codec] | 1200 |  | 1.5 | 1.4 | 2.2 | 2.9 | 5.9 |
| orders -> shipping.quote [loopback-codec] | 1200 |  | 1.3 | 1.2 | 2.3 | 3.2 | 6.1 |
| orders -> payments.authorize [loopback-codec] | 1200 | 21 | 1.1 | 1.1 | 1.4 | 1.6 | 5.8 |
| shipping -> inventory.getStock [loopback-codec] | 2440 |  | 0.4 | 0.4 | 0.7 | 0.9 | 4.7 |
| orders -> pricing.quote [loopback-codec] | 1200 |  | 0.9 | 0.8 | 1.4 | 2.1 | 5.5 |
| orders -> notifications.send [loopback-codec] | 1179 |  | 0.9 | 0.8 | 1.4 | 2.1 | 4.1 |
| orders -> payments.capture [loopback-codec] | 1179 |  | 0.7 | 0.7 | 0.8 | 0.9 | 4.0 |
| shipping -> inventory.getReservation [loopback-codec] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 4.6 |
| orders -> customers.getCustomer [loopback-codec] | 808 |  | 0.4 | 0.4 | 0.5 | 0.8 | 4.8 |
| shipping -> customers.getCustomer [loopback-codec] | 808 |  | 0.4 | 0.3 | 0.4 | 0.9 | 4.9 |
| pricing -> customers.getCustomer [loopback-codec] | 808 |  | 0.4 | 0.3 | 0.4 | 0.9 | 4.9 |
| payments -> customers.getPaymentMethod [loopback-codec] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| pricing -> catalog.getProduct [loopback-codec] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| inventory -> catalog.productExists [loopback-codec] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 3.1 |
| notifications -> customers.getCustomer [loopback-codec] | 574 |  | 0.3 | 0.2 | 0.5 | 0.7 | 3.0 |
| orders -> inventory.release [loopback-codec] | 21 |  | 1.5 | 1.5 | 1.9 | 2.0 | 2.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 605 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7050 calls, mean 0.00 ms, loopback-codec 16965 calls, mean 0.77 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/loopback-codec-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async outbox)

1200 orders in 6.4 s = 188.4 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 11.5 | 21.3 | 20.7 | 28.7 | 31.3 | 36.1 | 42.5 |
| placeOrder | declined | 21 | 6.9 | 14.2 | 12.8 | 19.3 | 19.4 | 19.7 | 19.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1200 |  | 4.5 | 4.3 | 8.0 | 9.9 | 12.1 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 3.3 | 3.2 | 4.0 | 6.9 | 7.8 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 3.2 | 3.0 | 5.1 | 7.2 | 8.9 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 1.5 | 1.5 | 1.8 | 4.6 | 5.7 |
| orders -> notifications.send [loopback-http] | 1179 |  | 2.1 | 1.9 | 3.5 | 4.3 | 8.0 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 2.0 | 2.0 | 2.4 | 5.3 | 8.9 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 1.9 | 1.8 | 2.7 | 4.1 | 6.8 |
| pricing -> catalog.getProduct [loopback-http] | 2440 |  | 0.7 | 0.7 | 0.9 | 1.2 | 4.6 |
| inventory -> catalog.productExists [loopback-http] | 2440 |  | 0.7 | 0.7 | 0.9 | 3.4 | 4.6 |
| shipping -> customers.getCustomer [loopback-http] | 2379 |  | 0.7 | 0.7 | 0.9 | 3.2 | 5.0 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.1 | 1.1 | 1.3 | 3.6 | 7.7 |
| orders -> customers.getCustomer [loopback-http] | 1200 |  | 0.8 | 0.8 | 1.0 | 3.4 | 4.7 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 0.8 | 0.8 | 1.0 | 1.6 | 4.2 |
| pricing -> customers.getCustomer [loopback-http] | 1200 |  | 0.8 | 0.7 | 0.9 | 3.5 | 4.6 |
| notifications -> customers.getCustomer [loopback-http] | 1179 |  | 0.8 | 0.7 | 1.3 | 1.8 | 3.2 |
| payments -> customers.getPaymentMethod [loopback-http] | 1200 |  | 0.7 | 0.7 | 0.9 | 1.2 | 4.8 |
| orders -> inventory.release [loopback-http] | 21 |  | 2.2 | 2.1 | 3.1 | 3.8 | 3.8 |

By binding: loopback-http 24015 calls, mean 1.47 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/loopback-http-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox)

1200 orders in 5.0 s = 237.8 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.4 | 16.8 | 16.3 | 22.0 | 24.3 | 27.9 | 36.0 |
| placeOrder | declined | 21 | 6.3 | 11.4 | 12.1 | 15.8 | 16.7 | 18.1 | 18.1 |

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
| orders -> shipping.quote [loopback-http] | 1200 |  | 2.9 | 2.6 | 5.3 | 7.7 | 8.6 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 2.4 | 2.2 | 3.0 | 5.4 | 12.1 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 0.9 | 0.7 | 1.5 | 3.7 | 5.1 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 1.7 | 1.6 | 2.4 | 4.6 | 7.3 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 1.7 | 1.5 | 3.0 | 5.4 | 6.8 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 1.7 | 1.7 | 2.3 | 4.7 | 6.2 |
| orders -> notifications.send [loopback-http] | 1179 |  | 1.6 | 1.6 | 2.4 | 3.6 | 5.7 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.0 | 1.0 | 1.2 | 1.5 | 8.4 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 0.8 | 0.7 | 0.9 | 1.1 | 4.8 |
| orders -> customers.getCustomer [loopback-http] | 808 |  | 0.8 | 0.7 | 1.0 | 3.9 | 4.3 |
| pricing -> customers.getCustomer [loopback-http] | 807 |  | 0.7 | 0.6 | 0.9 | 3.5 | 4.6 |
| payments -> customers.getPaymentMethod [loopback-http] | 808 |  | 0.7 | 0.7 | 0.9 | 1.1 | 4.2 |
| shipping -> customers.getCustomer [loopback-http] | 808 |  | 0.7 | 0.6 | 0.9 | 1.3 | 4.7 |
| pricing -> catalog.getProduct [loopback-http] | 591 |  | 0.7 | 0.7 | 0.9 | 3.8 | 4.4 |
| notifications -> customers.getCustomer [loopback-http] | 566 |  | 0.7 | 0.7 | 1.1 | 1.5 | 2.9 |
| inventory -> catalog.productExists [loopback-http] | 591 |  | 0.7 | 0.7 | 0.9 | 1.2 | 4.0 |
| orders -> inventory.release [loopback-http] | 21 |  | 2.4 | 2.1 | 3.3 | 3.3 | 3.3 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 613 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 393 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7059 calls, mean 0.00 ms, loopback-http 16956 calls, mean 1.31 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/loopback-http-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache off, parallel quotes on, async outbox)

1200 orders in 6.0 s = 201.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 11.1 | 19.9 | 19.0 | 27.4 | 29.8 | 33.8 | 42.0 |
| placeOrder | declined | 21 | 8.0 | 13.8 | 11.2 | 20.7 | 21.9 | 27.4 | 27.4 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.2 | 3.1 | 3.9 | 5.6 | 7.6 |
| orders -> shipping.quote [remote] | 1200 |  | 3.0 | 2.8 | 4.3 | 6.2 | 7.8 |
| orders -> pricing.quote [remote] | 1200 |  | 2.6 | 2.5 | 3.4 | 5.6 | 7.0 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.1 | 2.0 | 2.9 | 5.1 | 6.4 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.8 | 1.8 | 2.3 | 3.5 | 5.6 |
| shipping -> customers.getCustomer [remote] | 2379 |  | 0.9 | 0.9 | 1.1 | 1.3 | 4.1 |
| orders -> notifications.send [local] | 1179 |  | 1.7 | 1.6 | 3.0 | 4.5 | 5.1 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.7 | 0.6 | 0.8 | 0.9 | 4.7 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.5 | 2.7 | 5.9 |
| orders -> customers.getCustomer [remote] | 1200 |  | 0.9 | 0.9 | 1.2 | 2.3 | 4.6 |
| pricing -> customers.getCustomer [remote] | 1200 |  | 0.9 | 0.9 | 1.1 | 2.2 | 4.2 |
| notifications -> customers.getCustomer [remote] | 1179 |  | 0.9 | 0.8 | 1.7 | 2.7 | 3.1 |
| inventory -> catalog.productExists [local] | 2440 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.3 |
| pricing -> catalog.getProduct [local] | 2440 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.1 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 0.7 |
| payments -> customers.getPaymentMethod [local] | 1200 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.4 |
| orders -> inventory.release [remote] | 21 |  | 3.0 | 2.8 | 3.9 | 4.1 | 4.1 |

By binding: local 10878 calls, mean 0.54 ms, remote 13137 calls, mean 1.67 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/split-dbs-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox)

1200 orders in 5.2 s = 230.2 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.0 | 17.4 | 16.7 | 23.4 | 25.0 | 28.0 | 38.5 |
| placeOrder | declined | 21 | 7.3 | 12.0 | 12.0 | 14.5 | 18.0 | 19.2 | 19.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 2985 | 50.1% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.3 | 2.2 | 3.5 | 5.9 | 7.9 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.3 | 2.2 | 2.8 | 5.0 | 10.6 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.0 | 1.9 | 2.8 | 4.1 | 7.3 |
| orders -> pricing.quote [remote] | 1200 |  | 1.9 | 1.9 | 2.8 | 5.0 | 6.8 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.7 | 1.7 | 2.2 | 3.2 | 6.1 |
| orders -> notifications.send [local] | 1179 |  | 1.2 | 1.1 | 2.4 | 3.9 | 4.8 |
| orders -> payments.capture [remote] | 1179 |  | 1.2 | 1.2 | 1.5 | 1.7 | 5.6 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.8 | 0.8 | 4.9 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.2 | 3.2 | 4.8 |
| shipping -> customers.getCustomer [remote] | 807 |  | 1.0 | 0.9 | 1.2 | 2.4 | 4.8 |
| pricing -> customers.getCustomer [remote] | 808 |  | 0.9 | 0.9 | 1.1 | 2.2 | 5.2 |
| notifications -> customers.getCustomer [remote] | 550 |  | 1.0 | 0.9 | 2.2 | 2.9 | 3.6 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 4.3 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.4 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| orders -> inventory.release [remote] | 21 |  | 3.0 | 2.9 | 3.6 | 3.7 | 3.7 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.3 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 1572 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 629 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7075 calls, mean 0.02 ms, local 6788 calls, mean 0.54 ms, remote 10152 calls, mean 1.63 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T143809Z/split-dbs-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
