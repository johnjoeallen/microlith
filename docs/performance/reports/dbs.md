# Performance run 20261027T170230Z

- Started: 2026-10-27T17:02:30+00:00
- Commit: 72bd55f
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes groups,microservices,microservices-macro --dbs both --cache on --orders 1500 --warmup 200 --concurrency 16`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups dbs shared | 250 | 1473 | 27 | 29.4 | 63.8 | 61.7 | 81.6 | 87.5 | 96.9 | 109.5 | 19.9 | PASS |
| groups dbs split | 253 | 1473 | 27 | 25.6 | 63.1 | 62.5 | 79.0 | 84.2 | 94.5 | 106.2 | 19.9 | PASS |
| microservices dbs shared | 161 | 1473 | 27 | 39.0 | 99.2 | 94.6 | 128.9 | 138.5 | 182.2 | 210.9 | 19.9 | PASS |
| microservices dbs split | 159 | 1473 | 27 | 36.8 | 100.5 | 96.3 | 132.9 | 141.9 | 186.0 | 213.2 | 19.9 | PASS |
| microservices-macro dbs shared | 159 | 1473 | 27 | 42.2 | 100.4 | 96.2 | 133.9 | 144.3 | 177.8 | 198.9 | 19.9 | PASS |
| microservices-macro dbs split | 158 | 1473 | 27 | 37.4 | 101.3 | 95.7 | 133.5 | 146.9 | 175.8 | 205.0 | 19.9 | PASS |

## Database layout effect (one instance to one per deployable unit)

Higher orders/s and lower latencies are better.

| mode | orders/s shared | split | change | p50 shared | split | change | p95 shared | split | change | p99 shared | split | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups | 250 | 253 | +1% | 61.7 | 62.5 | +1% | 87.5 | 84.2 | -4% | 96.9 | 94.5 | -2% |
| microservices | 161 | 159 | -1% | 94.6 | 96.3 | +2% | 138.5 | 141.9 | +2% | 182.2 | 186.0 | +2% |
| microservices-macro | 159 | 158 | -1% | 96.2 | 95.7 | -1% | 144.3 | 146.9 | +2% | 177.8 | 175.8 | -1% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups dbs shared | 8.4 | 3,038 | 3,038 | 100% | 160 to 160 | 200 to 200 |
| groups dbs split | 8.4 | 3,037 | 3,037 | 100% | 160 to 160 | 200 to 200 |
| microservices dbs shared | 14.3 | 4,548 | 4,548 | 100% | 145 to 145 | 173 to 173 |
| microservices dbs split | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro dbs shared | 14.3 | 4,553 | 4,553 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro dbs split | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups dbs shared | groups dbs split | microservices dbs shared | microservices dbs split | microservices-macro dbs shared | microservices-macro dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.9 | 1.9 | 4.6 | 4.6 | 4.6 | 4.8 |
| orders -> shipping.createShipment | 1.9 | 1.9 | 3.5 | 3.6 | 3.6 | 3.7 |
| orders -> pricing.quote | 1.6 | 1.6 | 3.4 | 3.4 | 3.4 | 3.5 |
| orders -> payments.authorize | 1.7 | 1.7 | 3.0 | 3.1 | 3.0 | 3.0 |
| shipping -> inventory.getStock | 0.3 | 0.3 | 1.5 | 1.5 | 1.5 | 1.6 |
| orders -> inventory.reserve | 1.8 | 1.9 | 2.4 | 2.3 | 2.4 | 2.4 |
| orders -> notifications.send | 0.9 | 0.9 | 2.5 | 2.6 | 2.5 | 2.6 |
| orders -> payments.capture | 1.2 | 1.2 | 1.5 | 1.6 | 1.5 | 1.5 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 1.1 | 1.2 | 1.2 | 1.2 |
| pricing -> customers.getCustomer | 0.6 | 0.6 | 0.8 | 0.9 | 0.9 | 0.9 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 1.0 | 1.0 | 1.0 | 1.0 |
| shipping -> customers.getCustomer | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 |
| orders -> customers.getCustomer | 0.6 | 0.6 | 0.8 | 0.8 | 0.8 | 0.8 |
| inventory -> catalog.productExists | 0.1 | 0.1 | 0.5 | 0.5 | 0.5 | 0.5 |
| notifications -> customers.getCustomer | 0.3 | 0.3 | 0.8 | 0.9 | 0.8 | 0.9 |
| pricing -> catalog.getProduct | 0.1 | 0.1 | 0.4 | 0.4 | 0.4 | 0.4 |
| orders -> inventory.release | 2.2 | 2.4 | 2.4 | 2.4 | 2.4 | 2.3 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups dbs shared | groups dbs split | microservices dbs shared | microservices dbs split | microservices-macro dbs shared | microservices-macro dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 3.5 | 3.4 | 9.8 | 9.7 | 9.8 | 10.1 |
| orders -> shipping.createShipment | 2.9 | 3.0 | 5.4 | 5.6 | 5.7 | 5.8 |
| orders -> pricing.quote | 3.0 | 2.9 | 7.0 | 6.9 | 7.1 | 7.3 |
| orders -> payments.authorize | 2.9 | 2.8 | 5.2 | 5.3 | 5.5 | 5.2 |
| shipping -> inventory.getStock | 0.7 | 0.7 | 3.9 | 3.8 | 4.0 | 4.0 |
| orders -> inventory.reserve | 2.9 | 3.0 | 3.7 | 3.8 | 3.9 | 3.8 |
| orders -> notifications.send | 1.7 | 1.7 | 5.2 | 5.5 | 5.1 | 5.5 |
| orders -> payments.capture | 1.8 | 1.9 | 2.5 | 2.6 | 2.5 | 2.5 |
| shipping -> inventory.getReservation | 0.5 | 0.5 | 1.8 | 1.9 | 2.0 | 1.9 |
| pricing -> customers.getCustomer | 1.5 | 1.4 | 1.9 | 2.0 | 2.0 | 2.0 |
| payments -> customers.getPaymentMethod | 0.5 | 0.5 | 2.3 | 2.3 | 2.4 | 2.3 |
| shipping -> customers.getCustomer | 1.3 | 1.2 | 1.5 | 1.5 | 1.6 | 1.5 |
| orders -> customers.getCustomer | 1.5 | 1.4 | 1.8 | 1.9 | 1.9 | 1.8 |
| inventory -> catalog.productExists | 0.3 | 0.3 | 2.5 | 2.5 | 2.5 | 2.5 |
| notifications -> customers.getCustomer | 0.9 | 0.9 | 2.0 | 2.1 | 1.9 | 2.0 |
| pricing -> catalog.getProduct | 0.3 | 0.3 | 2.3 | 2.3 | 2.2 | 2.3 |
| orders -> inventory.release | 2.8 | 2.9 | 3.4 | 3.3 | 3.3 | 2.9 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | groups dbs shared | groups dbs split | microservices dbs shared | microservices dbs split | microservices-macro dbs shared | microservices-macro dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 78.0% | 78.0% | 77.7% | 77.7% | 77.8% | 77.7% |
| catalog.productExists | 78.0% | 78.0% | 77.7% | 77.7% | 77.8% | 77.7% |
| customers.getCustomer | 50.7% | 50.8% | 45.9% | 45.9% | 45.9% | 45.9% |
| customers.getPaymentMethod | 35.4% | 35.4% | 35.2% | 35.3% | 35.3% | 35.3% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| groups dbs shared | 9017 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12641 calls, 1.46 ms |
| groups dbs split | 9018 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12640 calls, 1.45 ms |
| microservices dbs shared | 8631 calls, 0.01 ms | - | - | - | 21288 calls, 2.17 ms |
| microservices dbs split | 8641 calls, 0.01 ms | - | - | - | 21278 calls, 2.20 ms |
| microservices-macro dbs shared | 8642 calls, 0.01 ms | - | - | - | 21277 calls, 2.19 ms |
| microservices-macro dbs split | 8636 calls, 0.01 ms | - | - | - | 21283 calls, 2.23 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### groups (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 6.0 s = 250.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.4 | 63.8 | 61.7 | 81.6 | 87.5 | 96.9 | 109.5 |
| placeOrder | declined | 27 | 40.6 | 59.4 | 58.7 | 77.2 | 78.4 | 82.7 | 82.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3778 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.5 | 4.5 | 7.8 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 2.9 | 4.0 | 7.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.9 | 3.8 | 8.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.9 | 3.8 | 6.8 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.0 | 4.1 | 6.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.8 | 2.5 | 15.2 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.7 | 2.9 | 4.7 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.0 | 1.7 |
| pricing -> customers.getCustomer [remote] | 966 |  | 1.0 | 0.9 | 1.7 | 2.2 | 3.9 |
| shipping -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.6 | 2.1 | 3.8 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.6 | 2.2 | 3.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 3.1 |
| notifications -> customers.getCustomer [remote] | 766 |  | 0.6 | 0.5 | 1.3 | 2.0 | 2.6 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.4 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.2 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.7 | 1.2 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.3 | 2.8 | 3.7 | 3.7 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 707 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 534 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 9017 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12641 calls, mean 1.46 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox, dbs split)

1500 orders in 5.9 s = 252.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 25.6 | 63.1 | 62.5 | 79.0 | 84.2 | 94.5 | 106.2 |
| placeOrder | declined | 27 | 42.6 | 58.1 | 57.1 | 74.7 | 77.0 | 81.2 | 81.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3779 | 50.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.0 | 3.6 | 7.5 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.4 | 4.6 | 9.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.0 | 3.8 | 5.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.8 | 3.8 | 7.2 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 2.9 | 3.5 | 8.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.8 | 12.8 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.7 | 3.1 | 3.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 0.9 | 4.1 |
| shipping -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.5 | 2.0 | 6.6 |
| pricing -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.5 | 2.0 | 4.7 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.5 | 2.4 | 4.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 1.9 |
| notifications -> customers.getCustomer [remote] | 762 |  | 0.6 | 0.5 | 1.4 | 2.1 | 2.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.7 | 0.9 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.6 | 2.9 | 3.0 | 3.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.4 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| notifications -> customers.getCustomer [cache] | 711 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9018 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12640 calls, mean 1.45 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 9.3 s = 160.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 39.0 | 99.2 | 94.6 | 128.9 | 138.5 | 182.2 | 210.9 |
| placeOrder | declined | 27 | 64.8 | 91.9 | 87.0 | 120.0 | 125.4 | 127.3 | 127.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2344 | 77.7% |
| catalog.productExists | 3018 | 2344 | 77.7% |
| customers.getCustomer | 7446 | 3415 | 45.9% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.6 | 4.0 | 9.8 | 12.8 | 54.6 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.4 | 6.9 | 57.6 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 7.0 | 9.4 | 13.0 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.9 | 5.4 | 49.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.2 | 6.4 | 11.5 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.2 | 5.2 | 6.8 | 10.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.7 | 4.7 | 56.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.1 | 5.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.6 | 52.4 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.5 | 1.4 | 2.5 | 3.3 | 8.9 |
| inventory -> catalog.productExists [remote] | 674 |  | 2.1 | 1.9 | 3.5 | 4.7 | 7.7 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.9 | 1.8 | 3.1 | 4.1 | 8.6 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.2 | 2.1 | 3.0 | 7.5 |
| notifications -> customers.getCustomer [remote] | 1115 |  | 1.1 | 1.0 | 2.1 | 2.8 | 3.6 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.0 | 2.0 | 3.3 | 7.8 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.0 | 1.9 | 2.5 | 8.1 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.5 | 3.4 | 3.5 | 3.5 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 358 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8631 calls, mean 0.01 ms, remote 21288 calls, mean 2.17 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox, dbs split)

1500 orders in 9.4 s = 158.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 36.8 | 100.5 | 96.3 | 132.9 | 141.9 | 186.0 | 213.2 |
| placeOrder | declined | 27 | 62.6 | 91.8 | 89.8 | 110.1 | 122.7 | 152.8 | 152.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3421 | 45.9% |
| customers.getPaymentMethod | 1500 | 530 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.6 | 4.0 | 9.7 | 13.4 | 47.8 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.6 | 3.3 | 5.6 | 7.2 | 53.6 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 6.9 | 10.0 | 13.6 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 2.9 | 5.3 | 7.1 | 12.7 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.8 | 5.7 | 46.8 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.3 | 5.5 | 6.9 | 9.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.8 | 5.1 | 7.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.4 | 2.6 | 3.4 | 6.2 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 1.9 | 2.7 | 50.3 |
| payments -> customers.getPaymentMethod [remote] | 970 |  | 1.5 | 1.4 | 2.6 | 3.4 | 10.9 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.1 | 1.9 | 3.6 | 4.7 | 9.2 |
| pricing -> customers.getCustomer [remote] | 970 |  | 1.3 | 1.2 | 2.2 | 2.8 | 7.7 |
| notifications -> customers.getCustomer [remote] | 1115 |  | 1.1 | 1.0 | 2.3 | 3.0 | 7.2 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.9 | 1.8 | 3.1 | 4.2 | 9.0 |
| orders -> customers.getCustomer [remote] | 970 |  | 1.2 | 1.1 | 2.0 | 2.8 | 3.3 |
| shipping -> customers.getCustomer [remote] | 970 |  | 1.1 | 1.0 | 1.9 | 2.6 | 7.2 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.3 | 3.3 | 3.6 | 3.6 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2003 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 358 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8641 calls, mean 0.01 ms, remote 21278 calls, mean 2.20 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 9.4 s = 158.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 42.2 | 100.4 | 96.2 | 133.9 | 144.3 | 177.8 | 198.9 |
| placeOrder | declined | 27 | 61.8 | 92.0 | 87.1 | 126.2 | 133.2 | 134.7 | 134.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2347 | 77.8% |
| catalog.productExists | 3018 | 2347 | 77.8% |
| customers.getCustomer | 7446 | 3419 | 45.9% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.6 | 4.0 | 9.8 | 13.7 | 57.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.6 | 3.4 | 5.7 | 7.5 | 48.3 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 7.1 | 10.1 | 14.5 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.1 | 4.0 | 5.0 | 51.7 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.5 | 6.9 | 12.7 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.2 | 5.1 | 6.8 | 8.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.9 | 5.1 | 48.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.3 | 4.3 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 2.0 | 2.7 | 43.7 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.7 | 3.5 | 9.4 |
| inventory -> catalog.productExists [remote] | 671 |  | 2.2 | 1.9 | 3.5 | 4.7 | 50.4 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.4 | 1.2 | 2.2 | 3.3 | 9.4 |
| pricing -> catalog.getProduct [remote] | 671 |  | 1.9 | 1.8 | 3.1 | 4.1 | 9.3 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 2.8 | 5.2 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.1 | 3.2 | 9.7 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.0 | 2.6 | 9.6 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.4 | 3.3 | 3.3 | 3.3 |
| inventory -> catalog.productExists [cache] | 2347 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2347 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8642 calls, mean 0.01 ms, remote 21277 calls, mean 2.19 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox, dbs split)

1500 orders in 9.5 s = 157.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 37.4 | 101.3 | 95.7 | 133.5 | 146.9 | 175.8 | 205.0 |
| placeOrder | declined | 27 | 66.7 | 92.5 | 84.6 | 122.5 | 123.3 | 167.1 | 167.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2344 | 77.7% |
| catalog.productExists | 3018 | 2344 | 77.7% |
| customers.getCustomer | 7446 | 3419 | 45.9% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.8 | 4.1 | 10.1 | 14.9 | 58.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.7 | 3.5 | 5.8 | 7.4 | 12.3 |
| orders -> pricing.quote [remote] | 1500 |  | 3.5 | 3.0 | 7.3 | 10.2 | 13.6 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.6 | 1.1 | 4.0 | 5.5 | 54.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.2 | 6.8 | 14.6 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.2 | 5.5 | 7.2 | 8.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.3 | 3.8 | 5.1 | 49.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.1 | 7.2 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.6 | 5.4 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.5 | 3.5 | 12.2 |
| inventory -> catalog.productExists [remote] | 674 |  | 2.2 | 1.9 | 3.5 | 4.6 | 53.3 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.4 | 1.3 | 2.3 | 3.2 | 8.2 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.9 | 1.7 | 3.3 | 4.5 | 10.0 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 2.8 | 5.3 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.0 | 3.2 | 6.3 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.0 | 2.9 | 7.6 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 2.9 | 3.3 | 3.3 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8636 calls, mean 0.01 ms, remote 21283 calls, mean 2.23 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
