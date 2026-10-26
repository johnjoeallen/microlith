# Performance run 20261026T184505Z

- Started: 2026-10-26T18:45:05+00:00
- Commit: 3e82736 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-http,split-dbs,microservices-macro --parallel both --orders 1500 --warmup 200 --concurrency 16`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http parallel off | 301 | 1473 | 27 | 24.7 | 53.0 | 49.8 | 68.3 | 72.7 | 83.0 | 101.4 | 19.9 | PASS |
| loopback-http parallel on | 326 | 1473 | 27 | 21.5 | 48.9 | 46.3 | 62.6 | 67.5 | 73.7 | 89.3 | 19.9 | PASS |
| split-dbs parallel off | 247 | 1473 | 27 | 29.2 | 64.6 | 61.7 | 85.4 | 91.7 | 102.9 | 110.8 | 19.9 | PASS |
| split-dbs parallel on | 253 | 1473 | 27 | 29.7 | 62.9 | 61.1 | 81.8 | 87.9 | 99.5 | 115.4 | 19.9 | PASS |
| microservices-macro parallel off | 138 | 1473 | 27 | 50.8 | 115.9 | 108.6 | 158.0 | 175.6 | 211.1 | 247.1 | 19.9 | PASS |
| microservices-macro parallel on | 161 | 1473 | 27 | 36.4 | 98.9 | 95.3 | 130.5 | 140.8 | 167.6 | 195.5 | 19.9 | PASS |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http | 301 | 326 | +8% | 49.8 | 46.3 | -7% | 72.7 | 67.5 | -7% | 83.0 | 73.7 | -11% |
| split-dbs | 247 | 253 | +3% | 61.7 | 61.1 | -1% | 91.7 | 87.9 | -4% | 102.9 | 99.5 | -3% |
| microservices-macro | 138 | 161 | +17% | 108.6 | 95.3 | -12% | 175.6 | 140.8 | -20% | 211.1 | 167.6 | -21% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http parallel off | 12.7 | 4,082 | 4,082 | 100% | 151 to 151 | 169 to 169 |
| loopback-http parallel on | 14.0 | 4,475 | 4,475 | 100% | 146 to 146 | 173 to 173 |
| split-dbs parallel off | 7.8 | 2,841 | 2,841 | 100% | 165 to 165 | 200 to 200 |
| split-dbs parallel on | 8.4 | 3,037 | 3,037 | 100% | 160 to 160 | 200 to 200 |
| microservices-macro parallel off | 14.3 | 4,545 | 4,545 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro parallel on | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http parallel off | loopback-http parallel on | split-dbs parallel off | split-dbs parallel on | microservices-macro parallel off | microservices-macro parallel on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.4 | 1.7 | 1.0 | 1.9 | 4.2 | 4.5 |
| orders -> shipping.createShipment | 1.7 | 1.7 | 1.8 | 1.9 | 3.5 | 3.5 |
| orders -> payments.authorize | 1.2 | 1.2 | 1.6 | 1.7 | 3.0 | 3.0 |
| orders -> pricing.quote | 0.7 | 1.0 | 1.5 | 1.6 | 3.1 | 3.3 |
| orders -> inventory.reserve | 1.4 | 1.4 | 1.7 | 1.9 | 2.2 | 2.4 |
| shipping -> inventory.getStock | 0.6 | 0.5 | 0.3 | 0.3 | 1.4 | 1.5 |
| orders -> notifications.send | 1.1 | 1.1 | 0.9 | 0.9 | 2.6 | 2.5 |
| orders -> payments.capture | 0.8 | 0.8 | 1.1 | 1.2 | 1.5 | 1.5 |
| shipping -> inventory.getReservation | 0.6 | 0.5 | 0.3 | 0.4 | 1.1 | 1.1 |
| pricing -> customers.getCustomer | 0.0 | 0.3 | 0.6 | 0.6 | 0.8 | 0.9 |
| orders -> customers.getCustomer | 0.3 | 0.3 | 0.5 | 0.6 | 0.7 | 0.8 |
| payments -> customers.getPaymentMethod | 0.3 | 0.3 | 0.2 | 0.2 | 0.9 | 1.0 |
| notifications -> customers.getCustomer | 0.2 | 0.2 | 0.3 | 0.3 | 0.9 | 0.8 |
| inventory -> catalog.productExists | 0.1 | 0.1 | 0.1 | 0.1 | 0.4 | 0.5 |
| shipping -> customers.getCustomer | 0.0 | 0.1 | 0.0 | 0.3 | 0.3 | 0.4 |
| pricing -> catalog.getProduct | 0.1 | 0.1 | 0.1 | 0.1 | 0.4 | 0.4 |
| orders -> inventory.release | 1.7 | 1.6 | 2.4 | 2.2 | 2.3 | 2.4 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http parallel off | loopback-http parallel on | split-dbs parallel off | split-dbs parallel on | microservices-macro parallel off | microservices-macro parallel on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 3.0 | 3.3 | 1.9 | 3.5 | 9.1 | 9.6 |
| orders -> shipping.createShipment | 2.4 | 2.3 | 2.7 | 2.9 | 5.6 | 5.4 |
| orders -> payments.authorize | 1.9 | 1.8 | 2.5 | 2.8 | 5.3 | 5.2 |
| orders -> pricing.quote | 1.4 | 2.0 | 2.9 | 3.0 | 6.6 | 6.8 |
| orders -> inventory.reserve | 2.1 | 2.1 | 2.6 | 2.9 | 3.5 | 3.8 |
| shipping -> inventory.getStock | 1.1 | 1.1 | 0.5 | 0.7 | 3.5 | 3.7 |
| orders -> notifications.send | 1.8 | 1.9 | 1.6 | 1.7 | 5.5 | 5.2 |
| orders -> payments.capture | 1.0 | 1.0 | 1.7 | 1.8 | 2.4 | 2.4 |
| shipping -> inventory.getReservation | 0.8 | 0.7 | 0.5 | 0.5 | 1.7 | 1.8 |
| pricing -> customers.getCustomer | 0.0 | 0.6 | 1.4 | 1.5 | 1.8 | 1.9 |
| orders -> customers.getCustomer | 0.7 | 0.7 | 1.1 | 1.5 | 1.7 | 1.9 |
| payments -> customers.getPaymentMethod | 0.7 | 0.6 | 0.5 | 0.5 | 2.2 | 2.3 |
| notifications -> customers.getCustomer | 0.7 | 0.7 | 0.9 | 0.8 | 2.0 | 1.9 |
| inventory -> catalog.productExists | 0.5 | 0.6 | 0.3 | 0.3 | 2.2 | 2.4 |
| shipping -> customers.getCustomer | 0.0 | 0.5 | 0.0 | 1.3 | 1.4 | 1.5 |
| pricing -> catalog.getProduct | 0.5 | 0.5 | 0.3 | 0.3 | 2.1 | 2.1 |
| orders -> inventory.release | 2.4 | 2.5 | 3.3 | 3.3 | 3.1 | 3.7 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-http parallel off | loopback-http parallel on | split-dbs parallel off | split-dbs parallel on | microservices-macro parallel off | microservices-macro parallel on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 78.0% | 78.0% | 78.0% | 78.0% | 77.7% | 77.7% |
| catalog.productExists | 78.0% | 78.0% | 78.0% | 78.0% | 77.7% | 77.7% |
| customers.getCustomer | 76.6% | 50.6% | 63.7% | 50.8% | 46.0% | 45.9% |
| customers.getPaymentMethod | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.2% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-http parallel off | 10941 calls, 0.00 ms | - | - | 18978 calls, 0.89 ms | - |
| loopback-http parallel on | 9009 calls, 0.00 ms | - | - | 20910 calls, 0.88 ms | - |
| split-dbs parallel off | 9983 calls, 0.01 ms | 8261 calls, 0.39 ms | - | - | 11675 calls, 1.29 ms |
| split-dbs parallel on | 9022 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12636 calls, 1.46 ms |
| microservices-macro parallel off | 8646 calls, 0.01 ms | - | - | - | 21273 calls, 2.07 ms |
| microservices-macro parallel on | 8634 calls, 0.01 ms | - | - | - | 21285 calls, 2.15 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-http (cache on, parallel quotes off, async outbox)

1500 orders in 5.0 s = 300.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 24.7 | 53.0 | 49.8 | 68.3 | 72.7 | 83.0 | 101.4 |
| placeOrder | declined | 27 | 38.4 | 50.0 | 45.9 | 63.3 | 65.6 | 87.0 | 87.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5702 | 76.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.4 | 3.6 | 7.2 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.4 | 1.4 | 2.1 | 2.7 | 8.1 |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.4 | 1.2 | 3.0 | 4.5 | 6.8 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.9 | 2.4 | 5.3 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.4 | 1.1 | 1.5 | 5.7 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.8 | 3.5 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.0 | 1.2 | 3.2 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 0.7 | 0.5 | 1.4 | 2.4 | 5.4 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.6 | 0.5 | 0.8 | 0.9 | 5.1 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 3.9 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 3.0 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.5 | 0.4 | 0.9 | 1.2 | 1.7 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 3.8 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 1.6 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.6 | 2.4 | 3.3 | 3.3 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10941 calls, mean 0.00 ms, loopback-http 18978 calls, mean 0.89 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T184505Z/loopback-http-parallel-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox)

1500 orders in 4.6 s = 325.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 21.5 | 48.9 | 46.3 | 62.6 | 67.5 | 73.7 | 89.3 |
| placeOrder | declined | 27 | 35.6 | 44.8 | 43.6 | 51.6 | 56.0 | 66.4 | 66.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3770 | 50.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.7 | 1.5 | 3.3 | 4.6 | 7.6 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.3 | 3.1 | 5.6 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.4 | 1.3 | 2.1 | 2.8 | 4.6 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.8 | 2.3 | 7.0 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.5 | 0.4 | 1.1 | 1.4 | 4.0 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.6 | 3.5 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.0 | 0.9 | 2.0 | 2.9 | 6.0 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.0 | 1.3 | 4.7 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 3.4 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 0.9 | 3.1 |
| shipping -> customers.getCustomer [loopback-http] | 967 |  | 0.4 | 0.4 | 0.6 | 0.8 | 2.0 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.8 | 4.0 |
| pricing -> customers.getCustomer [loopback-http] | 965 |  | 0.4 | 0.4 | 0.7 | 0.8 | 2.2 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.5 | 0.4 | 0.9 | 1.1 | 1.5 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 1.0 | 2.4 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 2.4 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.6 | 2.5 | 2.7 | 2.7 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 535 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9009 calls, mean 0.00 ms, loopback-http 20910 calls, mean 0.88 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T184505Z/loopback-http-parallel-on` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes off, async outbox)

1500 orders in 6.1 s = 246.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.2 | 64.6 | 61.7 | 85.4 | 91.7 | 102.9 | 110.8 |
| placeOrder | declined | 27 | 44.3 | 61.9 | 63.0 | 79.5 | 82.0 | 83.4 | 83.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4744 | 63.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 1.7 | 1.6 | 2.6 | 3.2 | 4.7 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.8 | 1.6 | 2.7 | 3.3 | 6.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.4 | 2.5 | 3.2 | 7.9 |
| orders -> pricing.quote [remote] | 1500 |  | 1.5 | 1.4 | 2.9 | 3.6 | 5.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.0 | 4.6 |
| orders -> shipping.quote [remote] | 1500 |  | 1.0 | 0.9 | 1.9 | 2.6 | 5.4 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.6 | 3.0 | 3.6 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.6 | 2.0 | 3.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 3.0 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.7 | 0.7 | 1.2 | 1.5 | 2.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.5 |
| notifications -> customers.getCustomer [remote] | 764 |  | 0.6 | 0.5 | 1.4 | 2.0 | 2.2 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.4 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.2 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.4 | 0.6 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.2 | 3.3 | 3.7 | 3.7 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.7 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 709 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9983 calls, mean 0.01 ms, local 8261 calls, mean 0.39 ms, remote 11675 calls, mean 1.29 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T184505Z/split-dbs-parallel-off` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox)

1500 orders in 5.9 s = 253.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.7 | 62.9 | 61.1 | 81.8 | 87.9 | 99.5 | 115.4 |
| placeOrder | declined | 27 | 41.5 | 61.2 | 58.3 | 79.2 | 80.0 | 86.2 | 86.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3783 | 50.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 2.9 | 4.0 | 7.9 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.5 | 4.4 | 6.2 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 2.9 | 4.0 | 5.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.8 | 3.5 | 8.0 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.0 | 3.8 | 5.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.8 | 2.8 | 6.0 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.7 | 2.8 | 3.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 0.9 | 2.5 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.3 | 4.6 |
| shipping -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.6 | 2.2 | 4.2 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.6 | 2.1 | 2.9 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 1.9 |
| notifications -> customers.getCustomer [remote] | 758 |  | 0.6 | 0.5 | 1.3 | 2.0 | 2.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.7 | 1.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 2.3 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.5 | 0.8 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 3.3 | 3.4 | 3.4 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 715 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9022 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12636 calls, mean 1.46 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T184505Z/split-dbs-parallel-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes off, async outbox)

1500 orders in 10.9 s = 137.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 50.8 | 115.9 | 108.6 | 158.0 | 175.6 | 211.1 | 247.1 |
| placeOrder | declined | 27 | 71.9 | 106.5 | 101.5 | 137.9 | 142.4 | 217.8 | 217.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3425 | 46.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.2 | 3.6 | 9.1 | 11.5 | 63.8 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.2 | 5.6 | 8.4 | 12.7 |
| orders -> pricing.quote [remote] | 1500 |  | 3.1 | 2.7 | 6.6 | 9.2 | 12.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.3 | 7.0 | 12.1 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 0.9 | 3.5 | 4.8 | 54.2 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.3 | 5.5 | 6.9 | 9.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.2 | 2.1 | 3.5 | 4.5 | 8.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.2 | 4.4 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.5 | 5.6 |
| payments -> customers.getPaymentMethod [remote] | 969 |  | 1.4 | 1.3 | 2.4 | 3.0 | 5.6 |
| inventory -> catalog.productExists [remote] | 673 |  | 1.9 | 1.7 | 3.0 | 4.0 | 52.9 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 3.0 | 6.6 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.2 | 1.1 | 2.0 | 2.6 | 8.8 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.8 | 1.6 | 2.9 | 4.0 | 7.0 |
| orders -> customers.getCustomer [remote] | 969 |  | 1.1 | 0.9 | 1.8 | 2.5 | 46.2 |
| shipping -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.9 | 2.4 | 3.1 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 3.1 | 3.9 | 3.9 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8646 calls, mean 0.01 ms, remote 21273 calls, mean 2.07 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T184505Z/microservices-macro-parallel-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox)

1500 orders in 9.3 s = 161.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 36.4 | 98.9 | 95.3 | 130.5 | 140.8 | 167.6 | 195.5 |
| placeOrder | declined | 27 | 58.8 | 89.6 | 89.1 | 110.8 | 139.2 | 150.6 | 150.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3416 | 45.9% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.5 | 3.9 | 9.6 | 13.2 | 53.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.4 | 6.4 | 56.2 |
| orders -> pricing.quote [remote] | 1500 |  | 3.3 | 2.9 | 6.8 | 8.8 | 13.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.9 | 5.2 | 6.5 | 11.9 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.7 | 5.1 | 48.8 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.2 | 5.2 | 6.8 | 9.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.8 | 5.2 | 53.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.0 | 9.4 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.7 | 52.1 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.5 | 1.4 | 2.5 | 3.3 | 8.3 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.0 | 1.8 | 3.5 | 4.8 | 9.7 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.3 | 2.1 | 2.9 | 7.1 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 2.8 | 5.9 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.8 | 1.6 | 3.0 | 3.8 | 5.1 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.1 | 2.8 | 6.2 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.1 | 1.9 | 2.4 | 5.7 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.2 | 3.7 | 4.2 | 4.2 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8634 calls, mean 0.01 ms, remote 21285 calls, mean 2.15 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T184505Z/microservices-macro-parallel-on` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
