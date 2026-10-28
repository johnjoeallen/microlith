# Performance run 20261027T195450Z

- Started: 2026-10-27T19:54:50+00:00
- Commit: a3f7372 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-http --cache both --parallel both --async both --dbs per-module --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/loopback-http-8db`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http cache off parallel off async outbox | 243 | 1473 | 27 | 26.9 | 65.6 | 63.1 | 80.4 | 86.1 | 93.1 | 109.2 | 19.9 | PASS |
| loopback-http cache off parallel off async events | 194 | 1473 | 27 | 30.1 | 82.0 | 78.2 | 101.2 | 115.1 | 138.8 | 156.5 | 19.9 | PASS |
| loopback-http cache off parallel on async outbox | 272 | 1473 | 27 | 19.9 | 58.6 | 56.5 | 71.3 | 75.0 | 81.3 | 87.7 | 19.9 | PASS |
| loopback-http cache off parallel on async events | 237 | 1473 | 27 | 25.4 | 67.3 | 65.3 | 81.3 | 91.7 | 107.9 | 114.3 | 19.9 | PASS |
| loopback-http cache on parallel off async outbox | 284 | 1473 | 27 | 22.1 | 56.0 | 53.0 | 72.0 | 79.4 | 87.3 | 96.9 | 19.9 | PASS |
| loopback-http cache on parallel off async events | 271 | 1473 | 27 | 26.1 | 58.8 | 52.1 | 89.3 | 98.1 | 113.6 | 120.4 | 19.9 | PASS |
| loopback-http cache on parallel on async outbox | 279 | 1473 | 27 | 22.3 | 57.1 | 52.7 | 77.9 | 89.3 | 95.1 | 102.3 | 19.9 | PASS |
| loopback-http cache on parallel on async events | 295 | 1473 | 27 | 20.8 | 54.0 | 51.0 | 69.5 | 75.9 | 83.3 | 88.0 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (parallel off) (async outbox) | 243 | 284 | +17% | 63.1 | 53.0 | -16% | 86.1 | 79.4 | -8% | 93.1 | 87.3 | -6% |
| loopback-http (parallel off) (async events) | 194 | 271 | +39% | 78.2 | 52.1 | -33% | 115.1 | 98.1 | -15% | 138.8 | 113.6 | -18% |
| loopback-http (parallel on) (async outbox) | 272 | 279 | +3% | 56.5 | 52.7 | -7% | 75.0 | 89.3 | +19% | 81.3 | 95.1 | +17% |
| loopback-http (parallel on) (async events) | 237 | 295 | +25% | 65.3 | 51.0 | -22% | 91.7 | 75.9 | -17% | 107.9 | 83.3 | -23% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (cache off) (async outbox) | 243 | 272 | +12% | 63.1 | 56.5 | -10% | 86.1 | 75.0 | -13% | 93.1 | 81.3 | -13% |
| loopback-http (cache off) (async events) | 194 | 237 | +22% | 78.2 | 65.3 | -16% | 115.1 | 91.7 | -20% | 138.8 | 107.9 | -22% |
| loopback-http (cache on) (async outbox) | 284 | 279 | -2% | 53.0 | 52.7 | -0% | 79.4 | 89.3 | +12% | 87.3 | 95.1 | +9% |
| loopback-http (cache on) (async events) | 271 | 295 | +9% | 52.1 | 51.0 | -2% | 98.1 | 75.9 | -23% | 113.6 | 83.3 | -27% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (cache off) (parallel off) | 243 | 194 | -20% | 63.1 | 78.2 | +24% | 86.1 | 115.1 | +34% | 93.1 | 138.8 | +49% |
| loopback-http (cache off) (parallel on) | 272 | 237 | -13% | 56.5 | 65.3 | +16% | 75.0 | 91.7 | +22% | 81.3 | 107.9 | +33% |
| loopback-http (cache on) (parallel off) | 284 | 271 | -5% | 53.0 | 52.1 | -2% | 79.4 | 98.1 | +23% | 87.3 | 113.6 | +30% |
| loopback-http (cache on) (parallel on) | 279 | 295 | +6% | 52.7 | 51.0 | -3% | 89.3 | 75.9 | -15% | 95.1 | 83.3 | -12% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http cache off parallel off async outbox | 20.1 | 5,809 | 5,809 | 100% | 130 to 130 | 159 to 159 |
| loopback-http cache off parallel off async events | 19.9 | 5,761 | 5,761 | 100% | 130 to 130 | 159 to 159 |
| loopback-http cache off parallel on async outbox | 20.1 | 5,809 | 5,809 | 100% | 130 to 130 | 159 to 159 |
| loopback-http cache off parallel on async events | 19.9 | 5,761 | 5,761 | 100% | 130 to 130 | 159 to 159 |
| loopback-http cache on parallel off async outbox | 12.7 | 4,074 | 4,074 | 100% | 151 to 151 | 169 to 169 |
| loopback-http cache on parallel off async events | 12.1 | 3,891 | 3,891 | 100% | 153 to 153 | 168 to 168 |
| loopback-http cache on parallel on async outbox | 14.0 | 4,468 | 4,468 | 100% | 146 to 146 | 173 to 173 |
| loopback-http cache on parallel on async events | 13.4 | 4,285 | 4,285 | 100% | 148 to 148 | 172 to 172 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http cache off parallel off async outbox | loopback-http cache off parallel off async events | loopback-http cache off parallel on async outbox | loopback-http cache off parallel on async events | loopback-http cache on parallel off async outbox | loopback-http cache on parallel off async events | loopback-http cache on parallel on async outbox | loopback-http cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 2.4 | 2.9 | 2.6 | 3.2 | 1.5 | 1.6 | 2.1 | 2.0 |
| orders -> shipping.createShipment | 2.1 | 2.6 | 2.2 | 2.5 | 1.8 | 1.9 | 2.0 | 1.8 |
| orders -> inventory.reserve | 1.3 | 1.8 | 1.5 | 1.8 | 1.5 | 1.5 | 1.7 | 1.6 |
| shipping -> inventory.getStock | 0.8 | 1.0 | 0.9 | 1.1 | 0.6 | 0.6 | 0.6 | 0.6 |
| orders -> pricing.quote | 1.7 | 2.2 | 1.9 | 2.3 | 0.8 | 0.8 | 1.2 | 1.2 |
| orders -> payments.authorize | 1.3 | 1.6 | 1.4 | 1.6 | 1.3 | 1.4 | 1.4 | 1.3 |
| orders -> notifications.send | 1.1 | 1.7 | 1.2 | 1.7 | 0.9 | 1.0 | 1.0 | 1.0 |
| orders -> payments.capture | 0.8 | 0.9 | 0.8 | 0.9 | 0.8 | 0.9 | 0.9 | 0.8 |
| pricing -> catalog.getProduct | 0.4 | 0.5 | 0.4 | 0.5 | 0.1 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists | 0.4 | 0.5 | 0.4 | 0.5 | 0.1 | 0.1 | 0.1 | 0.1 |
| shipping -> inventory.getReservation | 0.5 | 0.7 | 0.6 | 0.7 | 0.6 | 0.6 | 0.6 | 0.6 |
| shipping -> customers.getCustomer | 0.4 | 0.5 | 0.4 | 0.5 | 0.0 | 0.0 | 0.2 | 0.2 |
| orders -> customers.getCustomer | 0.4 | 0.6 | 0.5 | 0.6 | 0.3 | 0.4 | 0.4 | 0.4 |
| payments -> customers.getPaymentMethod | 0.4 | 0.5 | 0.4 | 0.5 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.4 | 0.5 | 0.4 | 0.6 | 0.0 | 0.0 | 0.4 | 0.3 |
| notifications -> customers.getCustomer | 0.4 | 0.6 | 0.4 | 0.6 | 0.2 | 0.0 | 0.2 | 0.0 |
| orders -> inventory.release | 1.5 | 1.9 | 1.6 | 1.7 | 1.5 | 1.6 | 1.6 | 1.6 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http cache off parallel off async outbox | loopback-http cache off parallel off async events | loopback-http cache off parallel on async outbox | loopback-http cache off parallel on async events | loopback-http cache on parallel off async outbox | loopback-http cache on parallel off async events | loopback-http cache on parallel on async outbox | loopback-http cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 4.1 | 5.2 | 4.6 | 5.5 | 3.2 | 3.6 | 4.2 | 3.8 |
| orders -> shipping.createShipment | 2.8 | 3.8 | 2.9 | 3.5 | 2.6 | 3.1 | 3.2 | 2.6 |
| orders -> inventory.reserve | 2.0 | 2.7 | 2.2 | 2.7 | 2.2 | 2.6 | 2.7 | 2.4 |
| shipping -> inventory.getStock | 1.2 | 1.6 | 1.3 | 1.6 | 1.2 | 1.4 | 1.4 | 1.3 |
| orders -> pricing.quote | 2.5 | 3.6 | 3.0 | 3.7 | 1.6 | 2.0 | 2.5 | 2.3 |
| orders -> payments.authorize | 1.8 | 2.2 | 1.8 | 2.2 | 1.9 | 2.4 | 2.4 | 2.0 |
| orders -> notifications.send | 1.8 | 2.5 | 2.2 | 2.6 | 1.6 | 1.8 | 1.7 | 1.6 |
| orders -> payments.capture | 1.0 | 1.2 | 1.0 | 1.2 | 1.1 | 1.3 | 1.3 | 1.1 |
| pricing -> catalog.getProduct | 0.6 | 0.8 | 0.6 | 0.8 | 0.6 | 0.6 | 0.7 | 0.6 |
| inventory -> catalog.productExists | 0.6 | 0.8 | 0.6 | 0.8 | 0.6 | 0.6 | 0.7 | 0.6 |
| shipping -> inventory.getReservation | 0.7 | 1.0 | 0.7 | 0.9 | 0.8 | 1.0 | 1.0 | 0.8 |
| shipping -> customers.getCustomer | 0.6 | 0.8 | 0.6 | 0.8 | 0.0 | 0.0 | 0.7 | 0.7 |
| orders -> customers.getCustomer | 0.6 | 0.9 | 0.7 | 0.9 | 0.7 | 1.0 | 1.0 | 0.8 |
| payments -> customers.getPaymentMethod | 0.6 | 0.8 | 0.6 | 0.7 | 0.7 | 0.8 | 0.8 | 0.7 |
| pricing -> customers.getCustomer | 0.5 | 0.8 | 0.6 | 0.9 | 0.0 | 0.0 | 0.8 | 0.8 |
| notifications -> customers.getCustomer | 0.7 | 0.9 | 0.8 | 0.9 | 0.5 | 0.0 | 0.6 | 0.0 |
| orders -> inventory.release | 1.9 | 3.6 | 2.5 | 3.1 | 1.9 | 3.0 | 2.4 | 3.2 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-http cache off parallel off async outbox | loopback-http cache off parallel off async events | loopback-http cache off parallel on async outbox | loopback-http cache off parallel on async events | loopback-http cache on parallel off async outbox | loopback-http cache on parallel off async events | loopback-http cache on parallel on async outbox | loopback-http cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 76.7% | 87.0% | 50.7% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-http cache off parallel off async outbox | - | - | - | 29919 calls, 0.84 ms | - |
| loopback-http cache off parallel off async events | - | - | - | 29919 calls, 1.08 ms | - |
| loopback-http cache off parallel on async outbox | - | - | - | 29919 calls, 0.92 ms | - |
| loopback-http cache off parallel on async events | - | - | - | 29919 calls, 1.10 ms | - |
| loopback-http cache on parallel off async outbox | 10950 calls, 0.00 ms | - | - | 18969 calls, 0.92 ms | - |
| loopback-http cache on parallel off async events | 11716 calls, 0.00 ms | - | - | 18203 calls, 1.00 ms | - |
| loopback-http cache on parallel on async outbox | 9015 calls, 0.00 ms | - | - | 20904 calls, 1.03 ms | - |
| loopback-http cache on parallel on async events | 9780 calls, 0.00 ms | - | - | 20139 calls, 1.00 ms | - |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-http (cache off, parallel quotes off, async outbox, dbs per-module)

1500 orders in 6.2 s = 243.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.9 | 65.6 | 63.1 | 80.4 | 86.1 | 93.1 | 109.2 |
| placeOrder | declined | 27 | 50.0 | 60.4 | 57.3 | 72.6 | 72.8 | 80.3 | 80.3 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.4 | 2.2 | 4.1 | 5.3 | 7.9 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.1 | 1.9 | 2.8 | 3.6 | 7.9 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.8 | 0.8 | 1.2 | 1.4 | 5.1 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.7 | 1.5 | 2.5 | 3.3 | 5.9 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.3 | 1.8 | 2.3 | 6.1 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.3 | 1.2 | 2.0 | 2.5 | 6.1 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.6 | 4.8 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 4.5 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 4.2 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.6 | 0.7 | 4.0 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.0 | 1.1 | 4.1 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 0.9 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.7 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.7 | 4.3 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.5 | 0.6 | 4.0 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.3 | 0.7 | 0.9 | 1.4 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.5 | 1.4 | 1.9 | 2.2 | 2.2 |

By binding: loopback-http 29919 calls, mean 0.84 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes off, async events, dbs per-module)

1500 orders in 7.7 s = 194.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.1 | 82.0 | 78.2 | 101.2 | 115.1 | 138.8 | 156.5 |
| placeOrder | declined | 27 | 63.3 | 77.8 | 71.6 | 109.8 | 119.2 | 129.8 | 129.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.9 | 2.6 | 5.2 | 7.9 | 14.1 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.6 | 2.4 | 3.8 | 5.0 | 10.9 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 2.2 | 2.0 | 3.6 | 5.9 | 10.6 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 1.0 | 0.9 | 1.6 | 2.3 | 10.3 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.8 | 1.7 | 2.7 | 3.4 | 8.6 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.7 | 1.5 | 2.5 | 5.2 | 10.9 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.6 | 1.5 | 2.2 | 3.7 | 8.4 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.5 | 0.5 | 0.8 | 1.1 | 5.1 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.5 | 0.5 | 0.8 | 1.2 | 5.6 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.5 | 0.4 | 0.8 | 1.2 | 4.2 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.9 | 0.9 | 1.2 | 1.8 | 5.2 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.7 | 6.5 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.6 | 0.5 | 0.9 | 1.9 | 6.7 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.6 | 0.5 | 0.9 | 1.5 | 8.7 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.5 | 0.5 | 0.8 | 1.5 | 9.7 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.5 | 0.5 | 0.8 | 1.1 | 4.2 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.9 | 1.6 | 3.6 | 5.9 | 5.9 |

By binding: loopback-http 29919 calls, mean 1.08 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async outbox, dbs per-module)

1500 orders in 5.5 s = 271.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 19.9 | 58.6 | 56.5 | 71.3 | 75.0 | 81.3 | 87.7 |
| placeOrder | declined | 27 | 42.0 | 53.7 | 51.9 | 64.6 | 66.5 | 71.4 | 71.4 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.6 | 2.4 | 4.6 | 5.8 | 9.3 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.2 | 2.1 | 2.9 | 3.7 | 7.4 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.9 | 1.8 | 3.0 | 3.6 | 7.8 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.9 | 0.9 | 1.3 | 1.5 | 5.2 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.5 | 1.4 | 2.2 | 2.7 | 5.1 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.4 | 1.3 | 1.8 | 2.1 | 6.6 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.2 | 1.1 | 2.2 | 2.8 | 3.6 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.8 | 3.5 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 5.0 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.6 | 0.8 | 5.0 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.0 | 1.1 | 5.3 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.6 | 0.5 | 0.7 | 0.9 | 5.1 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.5 | 0.5 | 0.7 | 0.8 | 4.0 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.8 | 1.4 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.7 | 4.6 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.4 | 0.8 | 1.2 | 1.6 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.5 | 2.5 | 2.6 | 2.6 |

By binding: loopback-http 29919 calls, mean 0.92 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async events, dbs per-module)

1500 orders in 6.3 s = 236.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 25.4 | 67.3 | 65.3 | 81.3 | 91.7 | 107.9 | 114.3 |
| placeOrder | declined | 27 | 49.0 | 63.4 | 60.7 | 81.6 | 82.2 | 96.3 | 96.3 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 3.2 | 2.9 | 5.5 | 7.4 | 11.9 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.5 | 2.4 | 3.5 | 4.7 | 12.9 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 2.3 | 2.2 | 3.7 | 5.4 | 9.2 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 1.1 | 1.0 | 1.6 | 2.5 | 7.0 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.8 | 1.7 | 2.7 | 3.6 | 6.6 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.6 | 4.3 | 7.8 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.6 | 1.5 | 2.2 | 2.9 | 11.8 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.5 | 0.5 | 0.8 | 1.3 | 6.3 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.5 | 0.5 | 0.8 | 1.2 | 6.2 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.5 | 0.5 | 0.8 | 1.1 | 6.3 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.9 | 0.9 | 1.2 | 1.6 | 6.9 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.7 | 0.6 | 0.9 | 1.2 | 3.1 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.6 | 0.5 | 0.9 | 1.5 | 4.4 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.6 | 0.5 | 0.9 | 1.5 | 6.0 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.6 | 0.5 | 0.9 | 1.5 | 6.2 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.5 | 0.5 | 0.7 | 1.0 | 2.5 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.6 | 3.1 | 3.4 | 3.4 |

By binding: loopback-http 29919 calls, mean 1.10 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-off-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes off, async outbox, dbs per-module)

1500 orders in 5.3 s = 284.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 22.1 | 56.0 | 53.0 | 72.0 | 79.4 | 87.3 | 96.9 |
| placeOrder | declined | 27 | 41.6 | 52.5 | 49.3 | 64.4 | 65.0 | 88.2 | 88.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5711 | 76.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.8 | 1.7 | 2.6 | 3.3 | 5.2 |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.5 | 1.3 | 3.2 | 4.6 | 6.6 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.5 | 1.4 | 2.2 | 2.8 | 5.2 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.3 | 1.9 | 2.3 | 5.3 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.5 | 1.2 | 1.6 | 4.7 |
| orders -> notifications.send [loopback-http] | 1473 |  | 0.9 | 0.9 | 1.6 | 2.4 | 3.5 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.2 | 4.4 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 0.8 | 0.6 | 1.6 | 2.4 | 4.6 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.6 | 0.5 | 0.8 | 1.0 | 3.8 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 1.2 | 4.0 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.8 | 1.6 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.5 | 0.8 | 1.0 | 2.8 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.5 | 0.8 | 0.9 | 4.3 |
| notifications -> customers.getCustomer [loopback-http] | 766 |  | 0.4 | 0.3 | 0.8 | 1.0 | 1.3 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.5 | 1.4 | 1.9 | 2.1 | 2.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 707 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10950 calls, mean 0.00 ms, loopback-http 18969 calls, mean 0.92 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes off, async events, dbs per-module)

1500 orders in 5.5 s = 271.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.1 | 58.8 | 52.1 | 89.3 | 98.1 | 113.6 | 120.4 |
| placeOrder | declined | 27 | 40.3 | 54.4 | 49.6 | 77.2 | 79.1 | 99.1 | 99.1 |

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
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.9 | 1.7 | 3.1 | 4.4 | 6.1 |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.6 | 1.4 | 3.6 | 5.2 | 16.7 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.5 | 1.3 | 2.6 | 4.3 | 12.0 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.4 | 1.3 | 2.4 | 3.8 | 12.4 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.5 | 1.4 | 2.0 | 9.5 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.0 | 0.9 | 1.8 | 2.6 | 5.3 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 0.8 | 0.6 | 2.0 | 3.4 | 5.4 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.7 | 4.7 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 4.7 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.6 | 0.5 | 1.1 | 2.3 | 4.5 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.9 | 1.4 | 5.2 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.6 | 0.5 | 1.1 | 1.6 | 3.6 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.5 | 0.9 | 1.7 | 4.0 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.3 | 3.0 | 3.1 | 3.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 11716 calls, mean 0.00 ms, loopback-http 18203 calls, mean 1.00 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 5.4 s = 279.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 22.3 | 57.1 | 52.7 | 77.9 | 89.3 | 95.1 | 102.3 |
| placeOrder | declined | 27 | 43.0 | 51.9 | 48.5 | 62.8 | 73.6 | 91.9 | 91.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3776 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.1 | 1.8 | 4.2 | 5.7 | 9.0 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.0 | 1.8 | 3.2 | 3.8 | 7.0 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.7 | 1.6 | 2.7 | 3.6 | 6.0 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.4 | 1.3 | 2.4 | 3.3 | 5.9 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.5 | 1.4 | 1.8 | 5.0 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.2 | 1.0 | 2.5 | 3.6 | 6.5 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.0 | 1.0 | 1.7 | 2.7 | 3.8 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.6 | 5.4 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.6 | 0.6 | 1.0 | 1.3 | 1.6 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.6 | 0.5 | 1.1 | 1.5 | 3.8 |
| pricing -> customers.getCustomer [loopback-http] | 968 |  | 0.5 | 0.5 | 0.9 | 1.3 | 4.5 |
| shipping -> customers.getCustomer [loopback-http] | 967 |  | 0.5 | 0.5 | 0.9 | 1.2 | 4.3 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.5 | 0.9 | 1.2 | 3.3 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.6 | 0.5 | 0.9 | 1.2 | 3.0 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.6 | 0.5 | 0.9 | 1.2 | 2.8 |
| notifications -> customers.getCustomer [loopback-http] | 766 |  | 0.4 | 0.4 | 0.8 | 1.2 | 1.6 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.6 | 2.4 | 2.4 | 2.4 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 707 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9015 calls, mean 0.00 ms, loopback-http 20904 calls, mean 1.03 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events, dbs per-module)

1500 orders in 5.1 s = 295.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.8 | 54.0 | 51.0 | 69.5 | 75.9 | 83.3 | 88.0 |
| placeOrder | declined | 27 | 41.1 | 48.9 | 47.0 | 64.2 | 64.8 | 74.0 | 74.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4541 | 61.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.0 | 1.7 | 3.8 | 5.7 | 9.0 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.8 | 1.7 | 2.6 | 3.3 | 6.2 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.6 | 1.5 | 2.4 | 3.2 | 12.0 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.3 | 2.0 | 2.4 | 12.8 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.5 | 1.3 | 1.7 | 5.1 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.2 | 1.0 | 2.3 | 3.7 | 6.9 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.0 | 0.9 | 1.6 | 2.1 | 11.8 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.3 | 5.2 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.6 | 0.6 | 0.8 | 1.0 | 4.6 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.6 | 0.5 | 0.9 | 1.2 | 5.9 |
| shipping -> customers.getCustomer [loopback-http] | 968 |  | 0.5 | 0.5 | 0.8 | 1.1 | 5.4 |
| pricing -> customers.getCustomer [loopback-http] | 968 |  | 0.5 | 0.5 | 0.8 | 1.0 | 4.4 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 5.1 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.6 | 0.5 | 0.9 | 1.1 | 4.6 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.5 | 0.8 | 1.1 | 4.5 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.4 | 3.2 | 3.2 | 3.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9780 calls, mean 0.00 ms, loopback-http 20139 calls, mean 1.00 ms. 19.9 module-to-module calls per order.

Raw samples: `loopback-http-cache-on-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
