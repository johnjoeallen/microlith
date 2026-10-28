# Performance run 20261027T201326Z

- Started: 2026-10-27T20:13:26+00:00
- Commit: a3f7372 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes modulith,loopback-codec,loopback-http --dbs all --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/inter-20261027/rep1`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith dbs shared | 437 | 1473 | 27 | 16.4 | 36.4 | 34.3 | 43.5 | 47.6 | 88.9 | 91.9 | 19.9 | PASS |
| modulith dbs per-module | 444 | 1473 | 27 | 15.0 | 35.9 | 34.6 | 41.8 | 47.0 | 55.9 | 65.1 | 19.9 | PASS |
| loopback-codec dbs shared | 426 | 1473 | 27 | 14.0 | 37.4 | 33.3 | 51.9 | 57.2 | 90.6 | 96.8 | 19.9 | PASS |
| loopback-codec dbs per-module | 364 | 1473 | 27 | 16.7 | 43.8 | 41.3 | 55.1 | 58.5 | 105.8 | 120.2 | 19.9 | PASS |
| loopback-http dbs shared | 321 | 1473 | 27 | 20.3 | 49.6 | 47.4 | 62.2 | 66.8 | 73.4 | 78.7 | 19.9 | PASS |
| loopback-http dbs per-module | 310 | 1473 | 27 | 20.8 | 51.4 | 48.3 | 65.5 | 72.8 | 81.6 | 90.7 | 19.9 | PASS |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http dbs shared | 14.0 | 4,474 | 4,474 | 100% | 146 to 146 | 173 to 173 |
| loopback-http dbs per-module | 14.0 | 4,468 | 4,468 | 100% | 146 to 146 | 173 to 173 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith dbs shared | modulith dbs per-module | loopback-codec dbs shared | loopback-codec dbs per-module | loopback-http dbs shared | loopback-http dbs per-module |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.2 | 1.2 | 1.2 | 1.5 | 1.7 | 1.8 |
| orders -> inventory.reserve | 1.2 | 1.2 | 1.2 | 1.5 | 1.4 | 1.5 |
| orders -> shipping.quote | 0.7 | 0.7 | 0.8 | 0.9 | 1.8 | 1.9 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 1.0 | 1.2 | 1.3 |
| shipping -> inventory.getStock | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 | 0.6 |
| orders -> notifications.send | 0.6 | 0.5 | 0.6 | 0.7 | 1.1 | 1.1 |
| orders -> pricing.quote | 0.4 | 0.5 | 0.5 | 0.6 | 1.1 | 1.1 |
| orders -> payments.capture | 0.5 | 0.5 | 0.6 | 0.7 | 0.8 | 0.8 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| orders -> customers.getCustomer | 0.1 | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 |
| shipping -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.1 | 0.2 | 0.2 |
| pricing -> customers.getCustomer | 0.1 | 0.1 | 0.2 | 0.2 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.1 | 0.1 | 0.1 | 0.2 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 |
| notifications -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.1 | 0.3 | 0.2 |
| orders -> inventory.release | 1.2 | 1.2 | 1.3 | 1.4 | 1.6 | 1.7 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith dbs shared | modulith dbs per-module | loopback-codec dbs shared | loopback-codec dbs per-module | loopback-http dbs shared | loopback-http dbs per-module |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 1.7 | 1.6 | 1.9 | 2.1 | 2.4 | 2.6 |
| orders -> inventory.reserve | 1.8 | 1.7 | 2.0 | 2.3 | 2.1 | 2.3 |
| orders -> shipping.quote | 1.4 | 1.4 | 1.8 | 1.9 | 3.6 | 3.6 |
| orders -> payments.authorize | 1.1 | 1.1 | 1.3 | 1.3 | 1.8 | 1.9 |
| shipping -> inventory.getStock | 0.5 | 0.5 | 0.6 | 0.7 | 1.1 | 1.2 |
| orders -> notifications.send | 1.0 | 1.0 | 1.0 | 1.1 | 1.9 | 1.9 |
| orders -> pricing.quote | 0.9 | 0.9 | 1.2 | 1.3 | 2.1 | 2.2 |
| orders -> payments.capture | 0.7 | 0.7 | 0.7 | 0.8 | 1.0 | 1.1 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.5 | 0.7 | 0.8 |
| orders -> customers.getCustomer | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 | 0.7 |
| shipping -> customers.getCustomer | 0.2 | 0.3 | 0.3 | 0.4 | 0.6 | 0.6 |
| pricing -> customers.getCustomer | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 | 0.7 |
| payments -> customers.getPaymentMethod | 0.2 | 0.3 | 0.3 | 0.3 | 0.6 | 0.7 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 | 0.6 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 | 0.6 |
| notifications -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.3 | 0.7 | 0.7 |
| orders -> inventory.release | 1.8 | 1.7 | 1.9 | 1.9 | 2.2 | 2.2 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith dbs shared | modulith dbs per-module | loopback-codec dbs shared | loopback-codec dbs per-module | loopback-http dbs shared | loopback-http dbs per-module |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | 50.4% | 50.6% | 50.4% | 50.7% | 50.7% | 50.7% |
| customers.getPaymentMethod | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith dbs shared | 8992 calls, 0.01 ms | 20927 calls, 0.50 ms | - | - | - |
| modulith dbs per-module | 9008 calls, 0.01 ms | 20911 calls, 0.50 ms | - | - | - |
| loopback-codec dbs shared | 8992 calls, 0.00 ms | - | 20927 calls, 0.54 ms | - | - |
| loopback-codec dbs per-module | 9013 calls, 0.00 ms | - | 20906 calls, 0.64 ms | - | - |
| loopback-http dbs shared | 9011 calls, 0.00 ms | - | - | 20908 calls, 0.91 ms | - |
| loopback-http dbs per-module | 9016 calls, 0.00 ms | - | - | 20903 calls, 0.93 ms | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.4 s = 437.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.4 | 36.4 | 34.3 | 43.5 | 47.6 | 88.9 | 91.9 |
| placeOrder | declined | 27 | 27.4 | 35.5 | 32.4 | 40.8 | 47.8 | 88.5 | 88.5 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.4 | 10.0 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.7 | 2.1 | 57.3 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 56.9 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 1.9 | 8.1 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 2.4 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 8.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.7 | 7.1 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.9 | 1.1 | 3.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 55.9 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.7 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.7 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.7 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.3 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| notifications -> customers.getCustomer [local] | 787 |  | 0.2 | 0.1 | 0.4 | 0.5 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 1.8 | 2.0 | 2.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8992 calls, mean 0.01 ms, local 20927 calls, mean 0.50 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 3.4 s = 443.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.0 | 35.9 | 34.6 | 41.8 | 47.0 | 55.9 | 65.1 |
| placeOrder | declined | 27 | 29.6 | 33.2 | 32.6 | 36.0 | 38.2 | 47.5 | 47.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3769 | 50.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.1 | 2.9 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.1 | 6.8 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 2.2 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.0 | 10.6 |
| orders -> notifications.send [local] | 1473 |  | 0.5 | 0.5 | 1.0 | 1.5 | 1.8 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 10.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 9.9 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.2 | 10.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 1.1 |
| orders -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| shipping -> customers.getCustomer [local] | 967 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 9.4 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 9.5 |
| notifications -> customers.getCustomer [local] | 773 |  | 0.1 | 0.1 | 0.4 | 0.5 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.7 | 2.0 | 2.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| notifications -> customers.getCustomer [cache] | 700 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9008 calls, mean 0.01 ms, local 20911 calls, mean 0.50 ms. 19.9 module-to-module calls per order.

Raw samples: `modulith-dbs-per-module` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 3.5 s = 426.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 14.0 | 37.4 | 33.3 | 51.9 | 57.2 | 90.6 | 96.8 |
| placeOrder | declined | 27 | 26.4 | 33.9 | 31.5 | 45.9 | 46.7 | 48.0 | 48.0 |

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
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 2.0 | 2.5 | 60.0 |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.3 | 2.9 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.3 | 1.7 | 9.5 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.8 | 2.6 | 8.3 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.6 | 0.5 | 0.7 | 0.9 | 58.0 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 2.4 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.9 | 8.2 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 1.2 | 1.8 | 8.3 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.6 | 1.2 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.3 | 0.2 | 0.5 | 0.7 | 1.2 |
| shipping -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.5 | 0.8 | 2.0 |
| pricing -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.5 | 0.8 | 1.1 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.0 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.3 | 0.2 | 0.4 | 0.8 | 8.1 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.7 |
| notifications -> customers.getCustomer [loopback-codec] | 786 |  | 0.2 | 0.1 | 0.4 | 0.5 | 0.9 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.3 | 1.2 | 1.9 | 2.1 | 2.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 687 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8992 calls, mean 0.00 ms, loopback-codec 20927 calls, mean 0.54 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 4.1 s = 363.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.7 | 43.8 | 41.3 | 55.1 | 58.5 | 105.8 | 120.2 |
| placeOrder | declined | 27 | 30.4 | 41.3 | 38.5 | 43.7 | 57.8 | 108.4 | 108.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3774 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.5 | 1.4 | 2.3 | 3.1 | 10.6 |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.5 | 1.4 | 2.1 | 2.7 | 70.3 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 1.0 | 0.9 | 1.3 | 1.7 | 69.9 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.9 | 0.8 | 1.9 | 2.8 | 7.2 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.7 | 0.6 | 0.8 | 1.2 | 70.5 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.6 | 1.1 | 1.6 | 2.2 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.7 | 1.0 | 5.7 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.6 | 0.5 | 1.3 | 2.0 | 5.5 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.7 | 1.3 |
| orders -> customers.getCustomer [loopback-codec] | 968 |  | 0.3 | 0.3 | 0.6 | 0.9 | 4.6 |
| pricing -> customers.getCustomer [loopback-codec] | 964 |  | 0.3 | 0.3 | 0.6 | 0.9 | 5.2 |
| shipping -> customers.getCustomer [loopback-codec] | 966 |  | 0.3 | 0.3 | 0.5 | 0.9 | 4.9 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.2 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.3 | 0.2 | 0.5 | 0.9 | 5.1 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.3 | 0.2 | 0.5 | 0.9 | 1.8 |
| notifications -> customers.getCustomer [loopback-codec] | 774 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.9 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.4 | 1.4 | 1.9 | 2.1 | 2.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2007 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 699 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 536 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9013 calls, mean 0.00 ms, loopback-codec 20906 calls, mean 0.64 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-codec-dbs-per-module` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 4.7 s = 321.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.3 | 49.6 | 47.4 | 62.2 | 66.8 | 73.4 | 78.7 |
| placeOrder | declined | 27 | 36.2 | 46.9 | 44.6 | 54.8 | 63.5 | 71.6 | 71.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3772 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.8 | 1.6 | 3.6 | 4.9 | 7.6 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.4 | 3.9 | 8.7 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.4 | 1.3 | 2.1 | 2.7 | 6.3 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.8 | 2.2 | 6.7 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.5 | 1.1 | 1.4 | 6.7 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.1 | 1.9 | 2.9 | 3.8 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.1 | 0.9 | 2.1 | 3.4 | 7.4 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.0 | 1.2 | 4.1 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 6.8 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 1.0 | 3.8 |
| shipping -> customers.getCustomer [loopback-http] | 965 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.4 |
| pricing -> customers.getCustomer [loopback-http] | 965 |  | 0.5 | 0.4 | 0.7 | 0.9 | 4.4 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.8 | 1.2 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.5 | 0.5 | 0.9 | 1.3 | 1.6 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.5 | 0.7 | 1.1 | 3.6 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 1.0 | 3.7 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.5 | 2.2 | 2.2 | 2.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2008 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 535 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9011 calls, mean 0.00 ms, loopback-http 20908 calls, mean 0.91 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 4.8 s = 309.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.8 | 51.4 | 48.3 | 65.5 | 72.8 | 81.6 | 90.7 |
| placeOrder | declined | 27 | 38.5 | 48.2 | 44.8 | 64.0 | 65.8 | 74.2 | 74.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3777 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.9 | 1.6 | 3.6 | 5.5 | 9.4 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.8 | 1.6 | 2.6 | 3.4 | 6.3 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.5 | 1.4 | 2.3 | 3.3 | 12.2 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.2 | 1.9 | 2.4 | 8.5 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.5 | 1.2 | 1.6 | 7.1 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.1 | 0.9 | 2.2 | 3.4 | 7.6 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.7 | 3.4 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.3 | 4.9 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.6 | 0.5 | 0.8 | 1.0 | 3.8 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 1.0 | 4.0 |
| shipping -> customers.getCustomer [loopback-http] | 968 |  | 0.5 | 0.4 | 0.7 | 1.0 | 3.8 |
| pricing -> customers.getCustomer [loopback-http] | 965 |  | 0.5 | 0.4 | 0.7 | 1.0 | 3.6 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 5.6 |
| notifications -> customers.getCustomer [loopback-http] | 767 |  | 0.5 | 0.4 | 0.9 | 1.2 | 2.4 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.5 | 0.8 | 1.1 | 4.2 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.5 | 0.8 | 1.1 | 4.0 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.7 | 2.2 | 2.4 | 2.4 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 706 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 535 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9016 calls, mean 0.00 ms, loopback-http 20903 calls, mean 0.93 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-dbs-per-module` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
