# Performance run 20261026T091926Z

- Started: 2026-10-26T09:19:26+00:00
- Commit: 3e2d553 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes split-dbs --cache both --orders 1500 --warmup 200 --concurrency 16`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs cache off | 240 | 1473 | 27 | 27.6 | 66.4 | 64.2 | 87.8 | 92.5 | 107.5 | 119.2 | 19.9 | PASS |
| split-dbs cache on | 263 | 1473 | 27 | 30.0 | 60.6 | 58.5 | 78.7 | 85.6 | 98.0 | 121.7 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs | 240 | 263 | +10% | 64.2 | 58.5 | -9% | 92.5 | 85.6 | -7% | 107.5 | 98.0 | -9% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs cache off | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache on | 8.4 | 3,039 | 3,039 | 100% | 160 to 160 | 200 to 200 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: |
| orders -> shipping.createShipment | 2.6 | 1.9 |
| orders -> shipping.quote | 2.3 | 1.8 |
| orders -> pricing.quote | 2.0 | 1.6 |
| orders -> inventory.reserve | 1.8 | 1.8 |
| orders -> payments.authorize | 1.6 | 1.6 |
| shipping -> customers.getCustomer | 0.7 | 0.3 |
| orders -> payments.capture | 1.0 | 1.1 |
| orders -> notifications.send | 1.2 | 0.9 |
| shipping -> inventory.getStock | 0.5 | 0.3 |
| pricing -> customers.getCustomer | 0.8 | 0.6 |
| orders -> customers.getCustomer | 0.7 | 0.6 |
| notifications -> customers.getCustomer | 0.5 | 0.3 |
| shipping -> inventory.getReservation | 0.4 | 0.3 |
| inventory -> catalog.productExists | 0.2 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.1 |
| payments -> customers.getPaymentMethod | 0.3 | 0.2 |
| orders -> inventory.release | 2.2 | 2.3 |
| (calls per order) | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: |
| orders -> shipping.createShipment | 4.0 | 2.8 |
| orders -> shipping.quote | 3.8 | 3.5 |
| orders -> pricing.quote | 3.3 | 3.0 |
| orders -> inventory.reserve | 2.9 | 2.8 |
| orders -> payments.authorize | 2.7 | 2.6 |
| shipping -> customers.getCustomer | 1.3 | 1.3 |
| orders -> payments.capture | 1.6 | 1.7 |
| orders -> notifications.send | 2.2 | 1.6 |
| shipping -> inventory.getStock | 0.7 | 0.7 |
| pricing -> customers.getCustomer | 1.3 | 1.5 |
| orders -> customers.getCustomer | 1.3 | 1.4 |
| notifications -> customers.getCustomer | 1.0 | 0.9 |
| shipping -> inventory.getReservation | 0.5 | 0.5 |
| inventory -> catalog.productExists | 0.4 | 0.3 |
| pricing -> catalog.getProduct | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.4 | 0.5 |
| orders -> inventory.release | 3.1 | 3.0 |
| (calls per order) | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: |
| catalog.getProduct | - | 78.0% |
| catalog.productExists | - | 78.0% |
| customers.getCustomer | - | 50.7% |
| customers.getPaymentMethod | - | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| split-dbs cache off | - | 13500 calls, 0.39 ms | - | - | 16419 calls, 1.35 ms |
| split-dbs cache on | 9012 calls, 0.01 ms | 8261 calls, 0.42 ms | - | - | 12646 calls, 1.40 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### split-dbs (cache off, parallel quotes on, async outbox)

1500 orders in 6.2 s = 240.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 27.6 | 66.4 | 64.2 | 87.8 | 92.5 | 107.5 | 119.2 |
| placeOrder | declined | 27 | 47.2 | 60.1 | 54.6 | 76.3 | 76.8 | 83.5 | 83.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.4 | 4.0 | 5.5 | 9.0 |
| orders -> shipping.quote [remote] | 1500 |  | 2.3 | 2.1 | 3.8 | 5.0 | 8.4 |
| orders -> pricing.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.3 | 4.1 | 8.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.6 | 2.9 | 3.5 | 6.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.7 | 3.2 | 5.6 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.7 | 1.3 | 1.7 | 7.6 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.1 | 2.2 | 3.0 | 3.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 1.0 | 1.6 | 2.0 | 6.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.4 | 0.7 | 1.0 | 3.2 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.3 | 1.7 | 7.7 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.7 | 1.3 | 1.7 | 2.6 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.0 | 1.7 | 2.4 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.0 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 3.5 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.2 | 0.4 | 0.6 | 2.3 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 3.1 | 3.4 | 3.4 |

By binding: local 13500 calls, mean 0.39 ms, remote 16419 calls, mean 1.35 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T091926Z/split-dbs-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox)

1500 orders in 5.7 s = 263.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.0 | 60.6 | 58.5 | 78.7 | 85.6 | 98.0 | 121.7 |
| placeOrder | declined | 27 | 39.4 | 57.9 | 55.0 | 75.7 | 88.4 | 92.3 | 92.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3773 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 2.8 | 3.7 | 6.3 |
| orders -> shipping.quote [remote] | 1500 |  | 1.8 | 1.7 | 3.5 | 4.6 | 7.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.8 | 3.5 | 5.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.6 | 3.5 | 4.4 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.0 | 4.1 | 6.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.1 | 3.6 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.6 | 2.9 | 3.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 0.9 | 2.7 |
| shipping -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.6 | 2.3 | 4.8 |
| pricing -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.6 | 2.3 | 4.3 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.5 | 2.1 | 5.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.7 | 3.6 |
| notifications -> customers.getCustomer [remote] | 769 |  | 0.6 | 0.5 | 1.3 | 1.9 | 2.2 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.6 | 0.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 2.5 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.2 | 3.0 | 3.5 | 3.5 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 704 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9012 calls, mean 0.01 ms, local 8261 calls, mean 0.42 ms, remote 12646 calls, mean 1.40 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T091926Z/split-dbs-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
