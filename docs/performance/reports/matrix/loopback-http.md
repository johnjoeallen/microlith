# Performance run 20261027T174423Z

- Started: 2026-10-27T17:44:23+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-http --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/loopback-http`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http cache off parallel off async outbox | 258 | 1473 | 27 | 30.0 | 61.8 | 59.0 | 76.5 | 86.6 | 97.7 | 111.7 | 19.9 | PASS |
| loopback-http cache off parallel off async events | 253 | 1473 | 27 | 26.7 | 63.0 | 60.3 | 77.8 | 85.0 | 93.8 | 116.4 | 19.9 | PASS |
| loopback-http cache off parallel on async outbox | 293 | 1473 | 27 | 23.7 | 54.4 | 52.6 | 66.6 | 71.7 | 77.7 | 88.9 | 19.9 | PASS |
| loopback-http cache off parallel on async events | 278 | 1473 | 27 | 22.1 | 57.3 | 55.1 | 72.0 | 76.4 | 83.8 | 95.5 | 19.9 | PASS |
| loopback-http cache on parallel off async outbox | 313 | 1473 | 27 | 20.5 | 51.0 | 47.4 | 67.4 | 71.1 | 77.1 | 82.5 | 19.9 | PASS |
| loopback-http cache on parallel off async events | 305 | 1473 | 27 | 23.0 | 52.2 | 48.7 | 68.9 | 73.3 | 81.5 | 98.8 | 19.9 | PASS |
| loopback-http cache on parallel on async outbox | 326 | 1473 | 27 | 21.3 | 48.9 | 46.4 | 62.5 | 66.8 | 75.5 | 87.1 | 19.9 | PASS |
| loopback-http cache on parallel on async events | 315 | 1473 | 27 | 22.8 | 50.6 | 47.7 | 65.3 | 71.5 | 81.2 | 102.1 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (parallel off) (async outbox) | 258 | 313 | +21% | 59.0 | 47.4 | -20% | 86.6 | 71.1 | -18% | 97.7 | 77.1 | -21% |
| loopback-http (parallel off) (async events) | 253 | 305 | +20% | 60.3 | 48.7 | -19% | 85.0 | 73.3 | -14% | 93.8 | 81.5 | -13% |
| loopback-http (parallel on) (async outbox) | 293 | 326 | +11% | 52.6 | 46.4 | -12% | 71.7 | 66.8 | -7% | 77.7 | 75.5 | -3% |
| loopback-http (parallel on) (async events) | 278 | 315 | +13% | 55.1 | 47.7 | -13% | 76.4 | 71.5 | -7% | 83.8 | 81.2 | -3% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (cache off) (async outbox) | 258 | 293 | +14% | 59.0 | 52.6 | -11% | 86.6 | 71.7 | -17% | 97.7 | 77.7 | -20% |
| loopback-http (cache off) (async events) | 253 | 278 | +10% | 60.3 | 55.1 | -9% | 85.0 | 76.4 | -10% | 93.8 | 83.8 | -11% |
| loopback-http (cache on) (async outbox) | 313 | 326 | +4% | 47.4 | 46.4 | -2% | 71.1 | 66.8 | -6% | 77.1 | 75.5 | -2% |
| loopback-http (cache on) (async events) | 305 | 315 | +3% | 48.7 | 47.7 | -2% | 73.3 | 71.5 | -3% | 81.5 | 81.2 | -0% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (cache off) (parallel off) | 258 | 253 | -2% | 59.0 | 60.3 | +2% | 86.6 | 85.0 | -2% | 97.7 | 93.8 | -4% |
| loopback-http (cache off) (parallel on) | 293 | 278 | -5% | 52.6 | 55.1 | +5% | 71.7 | 76.4 | +7% | 77.7 | 83.8 | +8% |
| loopback-http (cache on) (parallel off) | 313 | 305 | -2% | 47.4 | 48.7 | +3% | 71.1 | 73.3 | +3% | 77.1 | 81.5 | +6% |
| loopback-http (cache on) (parallel on) | 326 | 315 | -3% | 46.4 | 47.7 | +3% | 66.8 | 71.5 | +7% | 75.5 | 81.2 | +8% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http cache off parallel off async outbox | 20.1 | 5,818 | 5,818 | 100% | 130 to 130 | 160 to 160 |
| loopback-http cache off parallel off async events | 19.9 | 5,761 | 5,761 | 100% | 130 to 130 | 159 to 159 |
| loopback-http cache off parallel on async outbox | 20.1 | 5,818 | 5,818 | 100% | 130 to 130 | 160 to 160 |
| loopback-http cache off parallel on async events | 19.9 | 5,761 | 5,761 | 100% | 130 to 130 | 159 to 159 |
| loopback-http cache on parallel off async outbox | 12.7 | 4,082 | 4,082 | 100% | 151 to 151 | 169 to 169 |
| loopback-http cache on parallel off async events | 12.1 | 3,891 | 3,891 | 100% | 153 to 153 | 168 to 168 |
| loopback-http cache on parallel on async outbox | 14.0 | 4,476 | 4,476 | 100% | 146 to 146 | 173 to 173 |
| loopback-http cache on parallel on async events | 13.4 | 4,285 | 4,285 | 100% | 148 to 148 | 172 to 172 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http cache off parallel off async outbox | loopback-http cache off parallel off async events | loopback-http cache off parallel on async outbox | loopback-http cache off parallel on async events | loopback-http cache on parallel off async outbox | loopback-http cache on parallel off async events | loopback-http cache on parallel on async outbox | loopback-http cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 2.2 | 2.3 | 2.4 | 2.6 | 1.4 | 1.4 | 1.7 | 1.8 |
| orders -> shipping.createShipment | 1.9 | 2.0 | 2.0 | 2.1 | 1.6 | 1.7 | 1.7 | 1.7 |
| shipping -> inventory.getStock | 0.8 | 0.8 | 0.8 | 0.9 | 0.5 | 0.5 | 0.5 | 0.6 |
| orders -> inventory.reserve | 1.2 | 1.3 | 1.4 | 1.5 | 1.3 | 1.3 | 1.4 | 1.5 |
| orders -> pricing.quote | 1.6 | 1.6 | 1.7 | 1.9 | 0.7 | 0.7 | 1.0 | 1.1 |
| orders -> payments.authorize | 1.2 | 1.3 | 1.3 | 1.3 | 1.2 | 1.2 | 1.2 | 1.2 |
| orders -> notifications.send | 1.2 | 1.3 | 1.2 | 1.4 | 1.0 | 0.9 | 1.1 | 1.0 |
| orders -> payments.capture | 0.7 | 0.7 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 |
| shipping -> inventory.getReservation | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| pricing -> catalog.getProduct | 0.4 | 0.4 | 0.4 | 0.4 | 0.1 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists | 0.4 | 0.4 | 0.4 | 0.4 | 0.1 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer | 0.4 | 0.4 | 0.4 | 0.4 | 0.0 | 0.0 | 0.1 | 0.2 |
| orders -> customers.getCustomer | 0.4 | 0.4 | 0.4 | 0.5 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.4 | 0.4 | 0.4 | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.4 | 0.4 | 0.4 | 0.5 | 0.0 | 0.0 | 0.3 | 0.3 |
| notifications -> customers.getCustomer | 0.4 | 0.4 | 0.4 | 0.4 | 0.2 | 0.0 | 0.3 | 0.0 |
| orders -> inventory.release | 1.6 | 1.3 | 1.6 | 1.6 | 1.5 | 1.3 | 1.7 | 1.4 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http cache off parallel off async outbox | loopback-http cache off parallel off async events | loopback-http cache off parallel on async outbox | loopback-http cache off parallel on async events | loopback-http cache on parallel off async outbox | loopback-http cache on parallel off async events | loopback-http cache on parallel on async outbox | loopback-http cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 3.9 | 3.9 | 4.2 | 4.5 | 2.9 | 2.8 | 3.3 | 3.5 |
| orders -> shipping.createShipment | 2.8 | 2.8 | 2.8 | 2.9 | 2.3 | 2.3 | 2.3 | 2.5 |
| shipping -> inventory.getStock | 1.1 | 1.1 | 1.2 | 1.3 | 1.1 | 1.1 | 1.1 | 1.1 |
| orders -> inventory.reserve | 1.9 | 1.9 | 2.0 | 2.2 | 2.0 | 2.1 | 2.1 | 2.2 |
| orders -> pricing.quote | 2.5 | 2.5 | 2.6 | 3.0 | 1.4 | 1.5 | 1.9 | 2.1 |
| orders -> payments.authorize | 1.7 | 1.7 | 1.7 | 1.8 | 1.8 | 1.9 | 1.9 | 1.9 |
| orders -> notifications.send | 2.3 | 1.7 | 2.2 | 1.9 | 1.7 | 1.3 | 1.9 | 1.4 |
| orders -> payments.capture | 0.9 | 1.0 | 0.9 | 1.0 | 1.0 | 1.0 | 1.0 | 1.1 |
| shipping -> inventory.getReservation | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 |
| pricing -> catalog.getProduct | 0.5 | 0.5 | 0.6 | 0.6 | 0.5 | 0.5 | 0.5 | 0.6 |
| inventory -> catalog.productExists | 0.6 | 0.6 | 0.6 | 0.6 | 0.5 | 0.5 | 0.5 | 0.6 |
| shipping -> customers.getCustomer | 0.5 | 0.5 | 0.6 | 0.6 | 0.0 | 0.0 | 0.5 | 0.6 |
| orders -> customers.getCustomer | 0.6 | 0.6 | 0.6 | 0.7 | 0.6 | 0.7 | 0.7 | 0.7 |
| payments -> customers.getPaymentMethod | 0.6 | 0.5 | 0.6 | 0.6 | 0.6 | 0.6 | 0.6 | 0.6 |
| pricing -> customers.getCustomer | 0.5 | 0.5 | 0.6 | 0.7 | 0.0 | 0.0 | 0.6 | 0.7 |
| notifications -> customers.getCustomer | 0.8 | 0.6 | 0.8 | 0.6 | 0.6 | 0.0 | 0.8 | 0.0 |
| orders -> inventory.release | 2.2 | 2.2 | 2.4 | 2.6 | 2.0 | 1.9 | 2.5 | 2.5 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-http cache off parallel off async outbox | loopback-http cache off parallel off async events | loopback-http cache off parallel on async outbox | loopback-http cache off parallel on async events | loopback-http cache on parallel off async outbox | loopback-http cache on parallel off async events | loopback-http cache on parallel on async outbox | loopback-http cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.6% | 87.0% | 50.6% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-http cache off parallel off async outbox | - | - | - | 29919 calls, 0.80 ms | - |
| loopback-http cache off parallel off async events | - | - | - | 29919 calls, 0.81 ms | - |
| loopback-http cache off parallel on async outbox | - | - | - | 29919 calls, 0.85 ms | - |
| loopback-http cache off parallel on async events | - | - | - | 29919 calls, 0.92 ms | - |
| loopback-http cache on parallel off async outbox | 10941 calls, 0.00 ms | - | - | 18978 calls, 0.85 ms | - |
| loopback-http cache on parallel off async events | 11716 calls, 0.00 ms | - | - | 18203 calls, 0.87 ms | - |
| loopback-http cache on parallel on async outbox | 9003 calls, 0.00 ms | - | - | 20916 calls, 0.88 ms | - |
| loopback-http cache on parallel on async events | 9781 calls, 0.00 ms | - | - | 20138 calls, 0.93 ms | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-http (cache off, parallel quotes off, async outbox, dbs shared)

1500 orders in 5.8 s = 257.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.0 | 61.8 | 59.0 | 76.5 | 86.6 | 97.7 | 111.7 |
| placeOrder | declined | 27 | 47.7 | 56.9 | 54.8 | 64.9 | 70.1 | 94.5 | 94.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.2 | 2.0 | 3.9 | 5.4 | 7.5 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.9 | 1.8 | 2.8 | 4.0 | 6.2 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.8 | 0.7 | 1.1 | 1.4 | 5.2 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.6 | 1.4 | 2.5 | 4.0 | 5.2 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.2 | 1.2 | 1.9 | 2.5 | 7.1 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.7 | 2.2 | 4.2 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.2 | 1.1 | 2.3 | 3.0 | 5.6 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.3 | 0.6 | 0.7 | 4.5 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.7 | 4.0 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.5 | 0.7 | 4.4 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.7 | 0.7 | 0.9 | 1.2 | 5.4 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 4.1 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.4 | 0.8 | 1.1 | 4.4 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.6 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.5 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.3 | 0.5 | 0.7 | 4.0 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.6 | 2.2 | 3.0 | 3.0 |

By binding: loopback-http 29919 calls, mean 0.80 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes off, async events, dbs shared)

1500 orders in 5.9 s = 253.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.7 | 63.0 | 60.3 | 77.8 | 85.0 | 93.8 | 116.4 |
| placeOrder | declined | 27 | 49.9 | 57.4 | 54.9 | 70.0 | 76.2 | 89.2 | 89.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.3 | 2.1 | 3.9 | 5.1 | 7.4 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.0 | 1.9 | 2.8 | 3.6 | 6.3 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.6 | 1.5 | 2.5 | 3.3 | 5.3 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.8 | 0.7 | 1.1 | 1.4 | 4.6 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.3 | 1.2 | 1.9 | 2.6 | 4.9 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.2 | 1.7 | 2.2 | 5.7 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.1 | 4.3 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.9 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.5 | 0.7 | 3.1 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.5 | 0.7 | 3.8 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.2 | 3.7 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 4.2 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.8 | 3.6 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.5 | 0.7 | 4.6 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.7 | 2.0 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.5 | 0.7 | 2.1 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.3 | 1.1 | 2.2 | 2.5 | 2.5 |

By binding: loopback-http 29919 calls, mean 0.81 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async outbox, dbs shared)

1500 orders in 5.1 s = 292.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 23.7 | 54.4 | 52.6 | 66.6 | 71.7 | 77.7 | 88.9 |
| placeOrder | declined | 27 | 41.6 | 49.7 | 48.3 | 56.8 | 57.4 | 72.1 | 72.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.4 | 2.2 | 4.2 | 6.0 | 7.5 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.0 | 1.9 | 2.8 | 3.3 | 6.7 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.7 | 1.6 | 2.6 | 3.8 | 5.8 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.8 | 0.8 | 1.2 | 1.5 | 4.6 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.4 | 1.3 | 2.0 | 2.6 | 5.3 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.2 | 1.7 | 2.1 | 5.2 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.2 | 1.1 | 2.2 | 2.8 | 3.5 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.9 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.7 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.6 | 0.7 | 4.5 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 0.9 | 1.1 | 4.9 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 3.5 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.8 | 3.9 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.8 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.4 | 0.8 | 1.1 | 1.5 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.9 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.5 | 2.4 | 2.6 | 2.6 |

By binding: loopback-http 29919 calls, mean 0.85 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async events, dbs shared)

1500 orders in 5.4 s = 277.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 22.1 | 57.3 | 55.1 | 72.0 | 76.4 | 83.8 | 95.5 |
| placeOrder | declined | 27 | 45.9 | 53.2 | 50.9 | 64.3 | 64.8 | 78.5 | 78.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.6 | 2.4 | 4.5 | 6.2 | 11.7 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.1 | 2.0 | 2.9 | 3.8 | 7.1 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.9 | 1.7 | 3.0 | 4.8 | 7.6 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.9 | 0.8 | 1.3 | 1.6 | 5.3 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.5 | 1.4 | 2.2 | 2.7 | 8.7 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.4 | 1.3 | 1.9 | 3.1 | 7.3 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.3 | 1.8 | 2.4 | 4.9 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.5 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.7 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.1 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.0 | 1.2 | 4.0 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 4.3 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.5 | 0.4 | 0.7 | 0.9 | 4.4 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.5 | 0.4 | 0.7 | 0.9 | 4.5 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.5 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.8 | 3.8 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.3 | 2.6 | 6.2 | 6.2 |

By binding: loopback-http 29919 calls, mean 0.92 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes off, async outbox, dbs shared)

1500 orders in 4.8 s = 312.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.5 | 51.0 | 47.4 | 67.4 | 71.1 | 77.1 | 82.5 |
| placeOrder | declined | 27 | 37.6 | 47.0 | 45.1 | 58.8 | 59.0 | 69.7 | 69.7 |

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
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.6 | 1.5 | 2.3 | 3.7 | 6.6 |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.4 | 1.2 | 2.9 | 4.1 | 6.6 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.3 | 1.2 | 2.0 | 2.5 | 5.5 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.8 | 2.6 | 6.1 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.5 | 0.4 | 1.1 | 1.3 | 3.9 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.0 | 1.0 | 1.7 | 2.5 | 3.3 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.0 | 1.2 | 6.5 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 0.7 | 0.5 | 1.4 | 2.0 | 5.1 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 4.9 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 5.0 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.8 | 3.2 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.4 | 0.4 | 0.8 | 1.1 | 1.3 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.8 | 3.1 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.8 | 2.8 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.5 | 1.4 | 2.0 | 2.2 | 2.2 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10941 calls, mean 0.00 ms, loopback-http 18978 calls, mean 0.85 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes off, async events, dbs shared)

1500 orders in 4.9 s = 305.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 23.0 | 52.2 | 48.7 | 68.9 | 73.3 | 81.5 | 98.8 |
| placeOrder | declined | 27 | 38.5 | 47.5 | 46.2 | 57.8 | 59.8 | 77.3 | 77.3 |

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
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.3 | 2.8 | 5.7 |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.4 | 1.2 | 2.8 | 3.9 | 5.7 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.3 | 1.2 | 2.1 | 2.5 | 7.3 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.9 | 2.7 | 5.6 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.5 | 0.4 | 1.1 | 1.4 | 3.8 |
| orders -> notifications.send [loopback-http] | 1473 |  | 0.9 | 0.9 | 1.3 | 1.7 | 5.4 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.0 | 1.2 | 5.2 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 0.7 | 0.5 | 1.5 | 2.5 | 5.1 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 4.1 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 3.4 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.5 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 4.6 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.8 | 2.4 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.3 | 1.2 | 1.9 | 2.4 | 2.4 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 11716 calls, mean 0.00 ms, loopback-http 18203 calls, mean 0.87 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 4.6 s = 325.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 21.3 | 48.9 | 46.4 | 62.5 | 66.8 | 75.5 | 87.1 |
| placeOrder | declined | 27 | 37.6 | 45.4 | 44.7 | 51.2 | 63.2 | 66.1 | 66.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3764 | 50.6% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.7 | 1.5 | 3.3 | 4.6 | 7.0 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.3 | 3.0 | 5.9 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.4 | 1.4 | 2.1 | 2.7 | 7.9 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.9 | 2.3 | 5.9 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.5 | 0.4 | 1.1 | 1.4 | 4.5 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.9 | 3.9 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.0 | 0.9 | 1.9 | 2.8 | 5.3 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.0 | 1.2 | 5.0 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 4.9 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.5 |
| shipping -> customers.getCustomer [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.9 | 4.5 |
| pricing -> customers.getCustomer [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.9 | 3.3 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.8 | 4.3 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.5 | 0.4 | 0.9 | 1.3 | 1.4 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 1.4 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.8 | 1.5 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.6 | 2.5 | 2.5 | 2.5 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9003 calls, mean 0.00 ms, loopback-http 20916 calls, mean 0.88 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events, dbs shared)

1500 orders in 4.8 s = 314.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 22.8 | 50.6 | 47.7 | 65.3 | 71.5 | 81.2 | 102.1 |
| placeOrder | declined | 27 | 38.2 | 46.8 | 44.3 | 53.4 | 56.5 | 74.8 | 74.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4542 | 61.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.8 | 1.6 | 3.5 | 5.0 | 8.1 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.5 | 3.0 | 6.1 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.5 | 1.4 | 2.2 | 2.8 | 8.5 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.9 | 2.4 | 6.5 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.4 | 1.1 | 1.5 | 5.1 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.1 | 0.9 | 2.1 | 3.7 | 6.1 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.0 | 0.9 | 1.4 | 2.3 | 5.5 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.4 | 5.3 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 3.2 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 1.3 | 4.3 |
| shipping -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 1.1 | 4.5 |
| pricing -> customers.getCustomer [loopback-http] | 966 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.2 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.9 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.1 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 2.7 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.4 | 1.3 | 2.5 | 2.8 | 2.8 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 534 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.8 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9781 calls, mean 0.00 ms, loopback-http 20138 calls, mean 0.93 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
