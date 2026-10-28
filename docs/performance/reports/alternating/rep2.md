# Performance run 20261027T202331Z

- Started: 2026-10-27T20:23:31+00:00
- Commit: a3f7372 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes modulith,loopback-codec,loopback-http --dbs all --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/inter-20261027/rep2`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith dbs shared | 442 | 1473 | 27 | 17.8 | 36.0 | 34.2 | 43.4 | 47.5 | 86.7 | 94.5 | 19.9 | PASS |
| modulith dbs per-module | 445 | 1473 | 27 | 16.2 | 35.8 | 34.3 | 43.7 | 47.2 | 52.1 | 58.4 | 19.9 | PASS |
| loopback-codec dbs shared | 444 | 1473 | 27 | 16.6 | 35.9 | 33.7 | 44.8 | 48.6 | 84.5 | 91.9 | 19.9 | PASS |
| loopback-codec dbs per-module | 450 | 1473 | 27 | 15.9 | 35.4 | 34.0 | 43.7 | 46.6 | 49.5 | 55.9 | 19.9 | PASS |
| loopback-http dbs shared | 326 | 1473 | 27 | 18.5 | 48.9 | 46.5 | 61.2 | 65.6 | 70.5 | 74.4 | 19.9 | PASS |
| loopback-http dbs per-module | 328 | 1473 | 27 | 19.2 | 48.6 | 46.6 | 61.1 | 65.1 | 73.1 | 78.6 | 19.9 | PASS |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http dbs shared | 14.0 | 4,476 | 4,476 | 100% | 146 to 146 | 173 to 173 |
| loopback-http dbs per-module | 14.0 | 4,469 | 4,469 | 100% | 146 to 146 | 173 to 173 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith dbs shared | modulith dbs per-module | loopback-codec dbs shared | loopback-codec dbs per-module | loopback-http dbs shared | loopback-http dbs per-module |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.2 | 1.2 | 1.2 | 1.2 | 1.7 | 1.7 |
| orders -> inventory.reserve | 1.2 | 1.2 | 1.2 | 1.1 | 1.4 | 1.3 |
| orders -> shipping.quote | 0.7 | 0.7 | 0.7 | 0.8 | 1.7 | 1.8 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 0.8 | 1.2 | 1.2 |
| orders -> notifications.send | 0.6 | 0.6 | 0.6 | 0.6 | 1.1 | 1.1 |
| shipping -> inventory.getStock | 0.2 | 0.2 | 0.2 | 0.3 | 0.5 | 0.6 |
| orders -> pricing.quote | 0.4 | 0.5 | 0.5 | 0.5 | 1.0 | 1.0 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 0.8 | 0.8 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.5 | 0.5 |
| orders -> customers.getCustomer | 0.1 | 0.2 | 0.1 | 0.2 | 0.3 | 0.3 |
| shipping -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.2 |
| pricing -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.2 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.1 | 0.1 | 0.1 | 0.1 | 0.3 | 0.3 |
| notifications -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.1 | 0.3 | 0.2 |
| pricing -> catalog.getProduct | 0.1 | 0.1 | 0.0 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists | 0.1 | 0.1 | 0.0 | 0.1 | 0.1 | 0.1 |
| orders -> inventory.release | 1.3 | 1.3 | 1.2 | 1.2 | 1.7 | 1.7 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith dbs shared | modulith dbs per-module | loopback-codec dbs shared | loopback-codec dbs per-module | loopback-http dbs shared | loopback-http dbs per-module |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.6 | 1.6 | 1.6 | 1.6 | 2.3 | 2.3 |
| orders -> inventory.reserve | 1.7 | 1.7 | 1.8 | 1.7 | 2.1 | 2.0 |
| orders -> shipping.quote | 1.3 | 1.4 | 1.4 | 1.4 | 3.3 | 3.4 |
| orders -> payments.authorize | 1.1 | 1.1 | 1.1 | 1.1 | 1.7 | 1.8 |
| orders -> notifications.send | 1.0 | 1.1 | 1.0 | 1.1 | 1.9 | 1.9 |
| shipping -> inventory.getStock | 0.5 | 0.5 | 0.5 | 0.5 | 1.1 | 1.1 |
| orders -> pricing.quote | 0.8 | 0.9 | 0.9 | 0.9 | 1.9 | 2.0 |
| orders -> payments.capture | 0.7 | 0.6 | 0.7 | 0.7 | 1.0 | 1.0 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.4 | 0.7 | 0.7 |
| orders -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 0.7 | 0.7 |
| shipping -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.3 | 0.5 | 0.6 |
| pricing -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 0.6 | 0.6 |
| payments -> customers.getPaymentMethod | 0.2 | 0.3 | 0.3 | 0.3 | 0.6 | 0.6 |
| notifications -> customers.getCustomer | 0.2 | 0.3 | 0.2 | 0.3 | 0.7 | 0.7 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.3 | 0.5 | 0.6 |
| inventory -> catalog.productExists | 0.2 | 0.3 | 0.2 | 0.3 | 0.5 | 0.6 |
| orders -> inventory.release | 1.7 | 1.8 | 1.7 | 1.6 | 2.5 | 2.5 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith dbs shared | modulith dbs per-module | loopback-codec dbs shared | loopback-codec dbs per-module | loopback-http dbs shared | loopback-http dbs per-module |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | 50.4% | 50.6% | 50.4% | 50.6% | 50.6% | 50.6% |
| customers.getPaymentMethod | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith dbs shared | 8992 calls, 0.01 ms | 20927 calls, 0.49 ms | - | - | - |
| modulith dbs per-module | 9005 calls, 0.01 ms | 20914 calls, 0.50 ms | - | - | - |
| loopback-codec dbs shared | 8991 calls, 0.00 ms | - | 20928 calls, 0.51 ms | - | - |
| loopback-codec dbs per-module | 9004 calls, 0.00 ms | - | 20915 calls, 0.51 ms | - | - |
| loopback-http dbs shared | 9005 calls, 0.00 ms | - | - | 20914 calls, 0.89 ms | - |
| loopback-http dbs per-module | 9010 calls, 0.00 ms | - | - | 20909 calls, 0.89 ms | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.4 s = 441.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.8 | 36.0 | 34.2 | 43.4 | 47.5 | 86.7 | 94.5 |
| placeOrder | declined | 27 | 26.7 | 33.5 | 32.1 | 39.1 | 41.1 | 49.1 | 49.1 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.3 | 6.7 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 1.9 | 56.7 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 2.4 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.9 | 8.2 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 2.0 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 6.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 1.6 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.8 | 1.2 | 7.9 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 54.2 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.5 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.6 |
| pricing -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.6 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| notifications -> customers.getCustomer [local] | 788 |  | 0.2 | 0.1 | 0.3 | 0.5 | 0.8 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.3 | 1.7 | 2.2 | 2.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.5 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 685 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.4 |

By binding: cache 8992 calls, mean 0.01 ms, local 20927 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.4 s = 444.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.2 | 35.8 | 34.3 | 43.7 | 47.2 | 52.1 | 58.4 |
| placeOrder | declined | 27 | 27.0 | 33.1 | 31.5 | 38.4 | 41.5 | 57.5 | 57.5 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.2 | 6.4 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 1.9 | 5.6 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 2.0 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.1 | 5.4 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.6 | 2.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 7.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.7 | 5.2 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.4 | 5.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.3 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.6 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.6 |
| notifications -> customers.getCustomer [local] | 774 |  | 0.2 | 0.1 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.2 | 1.8 | 2.2 | 2.2 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.6 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 699 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9005 calls, mean 0.01 ms, local 20914 calls, mean 0.50 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-dbs-per-module` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.4 s = 443.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.6 | 35.9 | 33.7 | 44.8 | 48.6 | 84.5 | 91.9 |
| placeOrder | declined | 27 | 26.2 | 32.4 | 31.8 | 38.7 | 42.8 | 42.9 | 42.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3752 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 52.4 |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.1 | 1.6 | 1.9 | 8.1 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.4 | 52.3 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.1 | 8.1 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 1.9 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 4.6 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.7 | 7.1 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.3 | 7.1 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.8 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.0 |
| pricing -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 6.4 |
| shipping -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.6 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| notifications -> customers.getCustomer [loopback-codec] | 787 |  | 0.2 | 0.1 | 0.4 | 0.5 | 0.9 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.1 | 1.7 | 2.1 | 2.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8991 calls, mean 0.00 ms, loopback-codec 20928 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.3 s = 449.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.9 | 35.4 | 34.0 | 43.7 | 46.6 | 49.5 | 55.9 |
| placeOrder | declined | 27 | 26.6 | 32.4 | 32.0 | 37.5 | 39.0 | 48.3 | 48.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3765 | 50.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.1 | 1.6 | 1.9 | 3.3 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.1 | 12.2 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 1.7 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.4 | 2.0 | 7.5 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.5 | 1.9 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 2.4 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 5.2 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.5 | 0.5 | 0.9 | 1.3 | 6.8 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.4 | 6.2 |
| pricing -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| shipping -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| notifications -> customers.getCustomer [loopback-codec] | 774 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.2 | 1.6 | 1.9 | 1.9 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 699 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9004 calls, mean 0.00 ms, loopback-codec 20915 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-dbs-per-module` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 4.6 s = 326.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.5 | 48.9 | 46.5 | 61.2 | 65.6 | 70.5 | 74.4 |
| placeOrder | declined | 27 | 37.4 | 45.2 | 44.1 | 52.4 | 56.9 | 66.6 | 66.6 |

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
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.7 | 1.6 | 3.3 | 4.8 | 7.8 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.3 | 2.7 | 12.2 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.4 | 1.3 | 2.1 | 2.5 | 5.3 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.7 | 2.2 | 6.1 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.5 | 0.4 | 1.1 | 1.4 | 4.8 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.7 | 3.3 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.0 | 0.9 | 1.9 | 3.0 | 5.8 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.0 | 1.1 | 4.9 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 4.8 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.7 | 1.0 | 4.7 |
| pricing -> customers.getCustomer [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.8 | 2.2 |
| shipping -> customers.getCustomer [loopback-http] | 967 |  | 0.4 | 0.4 | 0.6 | 0.8 | 1.6 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.4 | 0.4 | 0.6 | 0.8 | 2.8 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.5 | 0.4 | 0.9 | 1.2 | 1.3 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.8 | 3.9 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 3.3 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.6 | 2.5 | 2.8 | 2.8 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9005 calls, mean 0.00 ms, loopback-http 20914 calls, mean 0.89 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 4.6 s = 327.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 19.2 | 48.6 | 46.6 | 61.1 | 65.1 | 73.1 | 78.6 |
| placeOrder | declined | 27 | 37.9 | 45.2 | 42.7 | 50.2 | 62.6 | 70.9 | 70.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3771 | 50.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.8 | 1.6 | 3.4 | 4.8 | 6.5 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.3 | 2.8 | 5.3 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.3 | 1.2 | 2.0 | 2.7 | 5.3 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.8 | 2.1 | 6.2 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.4 | 1.1 | 1.4 | 4.2 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.8 | 5.0 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.0 | 0.9 | 2.0 | 3.2 | 6.0 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.0 | 1.2 | 5.2 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.4 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 1.7 | 5.2 |
| pricing -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 5.1 |
| shipping -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.8 | 3.9 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.8 | 4.3 |
| notifications -> customers.getCustomer [loopback-http] | 768 |  | 0.5 | 0.4 | 0.9 | 1.2 | 1.6 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.5 | 0.7 | 0.8 | 2.0 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.8 | 1.7 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.7 | 2.5 | 2.9 | 2.9 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 705 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9010 calls, mean 0.00 ms, loopback-http 20909 calls, mean 0.89 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-dbs-per-module` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
