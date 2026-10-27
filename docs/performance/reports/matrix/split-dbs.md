# Performance run 20261027T181004Z

- Started: 2026-10-27T18:10:04+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes split-dbs --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/split-dbs`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs cache off parallel off async outbox | 208 | 1473 | 27 | 33.8 | 76.7 | 72.9 | 102.8 | 111.3 | 155.3 | 175.0 | 19.9 | PASS |
| split-dbs cache off parallel off async events | 204 | 1473 | 27 | 34.2 | 78.2 | 72.3 | 106.3 | 120.0 | 162.9 | 183.9 | 19.9 | PASS |
| split-dbs cache off parallel on async outbox | 232 | 1473 | 27 | 30.6 | 68.7 | 66.9 | 91.1 | 96.0 | 108.1 | 124.8 | 19.9 | PASS |
| split-dbs cache off parallel on async events | 229 | 1473 | 27 | 27.5 | 69.5 | 66.2 | 92.9 | 98.7 | 112.6 | 117.0 | 19.9 | PASS |
| split-dbs cache on parallel off async outbox | 235 | 1473 | 27 | 30.1 | 67.8 | 65.1 | 88.8 | 95.3 | 108.3 | 124.6 | 19.9 | PASS |
| split-dbs cache on parallel off async events | 238 | 1473 | 27 | 25.6 | 67.0 | 65.4 | 86.4 | 92.5 | 109.0 | 126.2 | 19.9 | PASS |
| split-dbs cache on parallel on async outbox | 255 | 1473 | 27 | 31.9 | 62.4 | 61.3 | 79.5 | 86.2 | 93.7 | 107.4 | 19.9 | PASS |
| split-dbs cache on parallel on async events | 247 | 1473 | 27 | 26.5 | 64.4 | 61.5 | 86.5 | 92.9 | 104.7 | 114.8 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs (parallel off) (async outbox) | 208 | 235 | +13% | 72.9 | 65.1 | -11% | 111.3 | 95.3 | -14% | 155.3 | 108.3 | -30% |
| split-dbs (parallel off) (async events) | 204 | 238 | +17% | 72.3 | 65.4 | -10% | 120.0 | 92.5 | -23% | 162.9 | 109.0 | -33% |
| split-dbs (parallel on) (async outbox) | 232 | 255 | +10% | 66.9 | 61.3 | -8% | 96.0 | 86.2 | -10% | 108.1 | 93.7 | -13% |
| split-dbs (parallel on) (async events) | 229 | 247 | +8% | 66.2 | 61.5 | -7% | 98.7 | 92.9 | -6% | 112.6 | 104.7 | -7% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs (cache off) (async outbox) | 208 | 232 | +12% | 72.9 | 66.9 | -8% | 111.3 | 96.0 | -14% | 155.3 | 108.1 | -30% |
| split-dbs (cache off) (async events) | 204 | 229 | +12% | 72.3 | 66.2 | -9% | 120.0 | 98.7 | -18% | 162.9 | 112.6 | -31% |
| split-dbs (cache on) (async outbox) | 235 | 255 | +9% | 65.1 | 61.3 | -6% | 95.3 | 86.2 | -9% | 108.3 | 93.7 | -13% |
| split-dbs (cache on) (async events) | 238 | 247 | +4% | 65.4 | 61.5 | -6% | 92.5 | 92.9 | +0% | 109.0 | 104.7 | -4% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs (cache off) (parallel off) | 208 | 204 | -2% | 72.9 | 72.3 | -1% | 111.3 | 120.0 | +8% | 155.3 | 162.9 | +5% |
| split-dbs (cache off) (parallel on) | 232 | 229 | -1% | 66.9 | 66.2 | -1% | 96.0 | 98.7 | +3% | 108.1 | 112.6 | +4% |
| split-dbs (cache on) (parallel off) | 235 | 238 | +1% | 65.1 | 65.4 | +0% | 95.3 | 92.5 | -3% | 108.3 | 109.0 | +1% |
| split-dbs (cache on) (parallel on) | 255 | 247 | -3% | 61.3 | 61.5 | +0% | 86.2 | 92.9 | +8% | 93.7 | 104.7 | +12% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| split-dbs cache off parallel off async outbox | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache off parallel off async events | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache off parallel on async outbox | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache off parallel on async events | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache on parallel off async outbox | 7.8 | 2,840 | 2,840 | 100% | 165 to 165 | 200 to 200 |
| split-dbs cache on parallel off async events | 7.3 | 2,686 | 2,686 | 100% | 170 to 170 | 199 to 199 |
| split-dbs cache on parallel on async outbox | 8.4 | 3,038 | 3,038 | 100% | 160 to 160 | 200 to 200 |
| split-dbs cache on parallel on async events | 7.9 | 2,883 | 2,883 | 100% | 164 to 164 | 200 to 200 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | split-dbs cache off parallel off async outbox | split-dbs cache off parallel off async events | split-dbs cache off parallel on async outbox | split-dbs cache off parallel on async events | split-dbs cache on parallel off async outbox | split-dbs cache on parallel off async events | split-dbs cache on parallel on async outbox | split-dbs cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 2.5 | 2.5 | 2.6 | 2.7 | 1.8 | 1.8 | 1.9 | 2.0 |
| orders -> inventory.reserve | 1.8 | 1.8 | 1.9 | 1.9 | 1.8 | 1.7 | 1.9 | 2.0 |
| orders -> shipping.quote | 2.0 | 2.1 | 2.3 | 2.5 | 1.1 | 1.1 | 1.9 | 1.9 |
| orders -> pricing.quote | 1.8 | 1.9 | 2.0 | 2.2 | 1.6 | 1.6 | 1.6 | 1.7 |
| orders -> payments.authorize | 1.6 | 1.6 | 1.6 | 1.7 | 1.7 | 1.6 | 1.7 | 1.8 |
| orders -> notifications.send | 1.2 | 1.5 | 1.2 | 1.7 | 0.9 | 0.9 | 1.0 | 1.0 |
| orders -> payments.capture | 1.0 | 1.0 | 1.1 | 1.1 | 1.2 | 1.1 | 1.2 | 1.2 |
| shipping -> customers.getCustomer | 0.7 | 0.7 | 0.7 | 0.8 | 0.0 | 0.0 | 0.3 | 0.3 |
| shipping -> inventory.getStock | 0.4 | 0.4 | 0.5 | 0.5 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.7 | 0.7 | 0.8 | 0.8 | 0.7 | 0.7 | 0.6 | 0.7 |
| orders -> customers.getCustomer | 0.6 | 0.6 | 0.7 | 0.8 | 0.5 | 0.5 | 0.6 | 0.6 |
| notifications -> customers.getCustomer | 0.5 | 0.6 | 0.5 | 0.7 | 0.3 | 0.0 | 0.3 | 0.0 |
| shipping -> inventory.getReservation | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 0.3 | 0.4 | 0.4 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.2 | 0.3 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| orders -> inventory.release | 2.3 | 1.9 | 2.3 | 1.9 | 2.2 | 1.8 | 2.3 | 2.0 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | split-dbs cache off parallel off async outbox | split-dbs cache off parallel off async events | split-dbs cache off parallel on async outbox | split-dbs cache off parallel on async events | split-dbs cache on parallel off async outbox | split-dbs cache on parallel off async events | split-dbs cache on parallel on async outbox | split-dbs cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 3.8 | 4.1 | 4.1 | 4.3 | 3.0 | 2.7 | 2.9 | 3.2 |
| orders -> inventory.reserve | 2.8 | 2.9 | 3.2 | 3.1 | 2.7 | 2.7 | 2.9 | 3.1 |
| orders -> shipping.quote | 3.3 | 3.7 | 4.1 | 4.4 | 2.0 | 2.0 | 3.5 | 3.7 |
| orders -> pricing.quote | 3.0 | 3.3 | 3.4 | 3.7 | 3.0 | 3.2 | 3.1 | 3.3 |
| orders -> payments.authorize | 2.6 | 2.7 | 2.6 | 2.8 | 2.7 | 2.5 | 2.6 | 3.1 |
| orders -> notifications.send | 2.1 | 2.4 | 2.1 | 2.8 | 1.7 | 1.3 | 1.8 | 1.5 |
| orders -> payments.capture | 1.6 | 1.7 | 1.7 | 1.7 | 1.7 | 1.7 | 1.7 | 2.0 |
| shipping -> customers.getCustomer | 1.1 | 1.2 | 1.3 | 1.4 | 0.0 | 0.0 | 1.3 | 1.3 |
| shipping -> inventory.getStock | 0.7 | 0.7 | 0.8 | 0.9 | 0.6 | 0.6 | 0.7 | 0.7 |
| pricing -> customers.getCustomer | 1.1 | 1.2 | 1.3 | 1.6 | 1.6 | 1.6 | 1.4 | 1.6 |
| orders -> customers.getCustomer | 1.1 | 1.1 | 1.3 | 1.5 | 1.2 | 1.2 | 1.4 | 1.5 |
| notifications -> customers.getCustomer | 1.0 | 1.1 | 1.1 | 1.4 | 1.0 | 0.0 | 0.9 | 0.0 |
| shipping -> inventory.getReservation | 0.5 | 0.5 | 0.6 | 0.6 | 0.5 | 0.5 | 0.5 | 0.6 |
| inventory -> catalog.productExists | 0.3 | 0.3 | 0.4 | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.3 | 0.3 | 0.4 | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.4 | 0.5 | 0.4 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| orders -> inventory.release | 3.1 | 3.2 | 2.9 | 2.5 | 3.2 | 3.3 | 3.1 | 3.5 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | split-dbs cache off parallel off async outbox | split-dbs cache off parallel off async events | split-dbs cache off parallel on async outbox | split-dbs cache off parallel on async events | split-dbs cache on parallel off async outbox | split-dbs cache on parallel off async events | split-dbs cache on parallel on async outbox | split-dbs cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 63.8% | 74.0% | 50.7% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| split-dbs cache off parallel off async outbox | - | 13500 calls, 0.36 ms | - | - | 16419 calls, 1.24 ms |
| split-dbs cache off parallel off async events | - | 13500 calls, 0.41 ms | - | - | 16419 calls, 1.30 ms |
| split-dbs cache off parallel on async outbox | - | 13500 calls, 0.40 ms | - | - | 16419 calls, 1.38 ms |
| split-dbs cache off parallel on async events | - | 13500 calls, 0.48 ms | - | - | 16419 calls, 1.45 ms |
| split-dbs cache on parallel off async outbox | 9989 calls, 0.01 ms | 8261 calls, 0.41 ms | - | - | 11669 calls, 1.37 ms |
| split-dbs cache on parallel off async events | 10747 calls, 0.01 ms | 8261 calls, 0.39 ms | - | - | 10911 calls, 1.40 ms |
| split-dbs cache on parallel on async outbox | 9014 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12644 calls, 1.45 ms |
| split-dbs cache on parallel on async events | 9780 calls, 0.01 ms | 8261 calls, 0.45 ms | - | - | 11878 calls, 1.57 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### split-dbs (cache off, parallel quotes off, async outbox, dbs split)

1500 orders in 7.2 s = 208.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 33.8 | 76.7 | 72.9 | 102.8 | 111.3 | 155.3 | 175.0 |
| placeOrder | declined | 27 | 52.6 | 69.3 | 65.3 | 87.0 | 87.1 | 106.9 | 106.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.5 | 2.3 | 3.8 | 5.0 | 6.2 |
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.3 | 4.2 | 6.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.8 | 3.7 | 5.7 |
| orders -> pricing.quote [remote] | 1500 |  | 1.8 | 1.6 | 3.0 | 3.9 | 6.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.4 | 2.6 | 3.5 | 8.2 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.6 | 1.1 | 1.7 | 4.4 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.0 | 2.1 | 3.2 | 7.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 1.0 | 1.6 | 2.0 | 3.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.9 | 1.7 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.1 | 1.6 | 2.9 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.5 | 1.1 | 1.6 | 4.1 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.0 | 1.9 | 6.0 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.0 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.6 | 6.2 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 3.1 | 3.4 | 3.4 |

By binding: local 13500 calls, mean 0.36 ms, remote 16419 calls, mean 1.24 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-off-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache off, parallel quotes off, async events, dbs split)

1500 orders in 7.4 s = 204.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 34.2 | 78.2 | 72.3 | 106.3 | 120.0 | 162.9 | 183.9 |
| placeOrder | declined | 27 | 52.3 | 73.3 | 67.1 | 94.5 | 103.7 | 160.4 | 160.4 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.5 | 2.3 | 4.1 | 5.7 | 7.6 |
| orders -> shipping.quote [remote] | 1500 |  | 2.1 | 1.8 | 3.7 | 4.8 | 53.6 |
| orders -> pricing.quote [remote] | 1500 |  | 1.9 | 1.7 | 3.3 | 4.7 | 49.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.9 | 3.8 | 50.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.7 | 3.9 | 6.1 |
| orders -> notifications.send [local] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.4 | 52.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.6 | 1.2 | 1.8 | 4.5 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 0.9 | 1.7 | 2.3 | 3.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 1.1 | 2.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.2 | 1.7 | 7.1 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.6 | 1.1 | 1.8 | 7.1 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.6 | 0.5 | 1.1 | 1.8 | 49.9 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.4 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 1.0 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.5 | 0.7 | 1.9 |
| orders -> inventory.release [remote] | 27 |  | 1.9 | 1.6 | 3.2 | 4.9 | 4.9 |

By binding: local 13500 calls, mean 0.41 ms, remote 16419 calls, mean 1.30 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-off-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache off, parallel quotes on, async outbox, dbs split)

1500 orders in 6.5 s = 232.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.6 | 68.7 | 66.9 | 91.1 | 96.0 | 108.1 | 124.8 |
| placeOrder | declined | 27 | 46.3 | 63.1 | 57.6 | 80.3 | 82.9 | 85.8 | 85.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.5 | 4.1 | 5.5 | 7.9 |
| orders -> shipping.quote [remote] | 1500 |  | 2.3 | 2.2 | 4.1 | 5.0 | 7.8 |
| orders -> pricing.quote [remote] | 1500 |  | 2.0 | 1.9 | 3.4 | 4.2 | 8.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.2 | 3.7 | 6.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.6 | 3.5 | 9.8 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.7 | 1.3 | 1.9 | 5.5 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.1 | 2.1 | 3.2 | 6.5 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.5 | 8.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.4 | 0.8 | 1.2 | 2.1 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.3 | 2.0 | 5.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.7 | 1.3 | 2.1 | 3.8 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.1 | 1.8 | 2.3 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.5 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.8 | 1.1 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.1 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 2.9 | 3.5 | 3.5 |

By binding: local 13500 calls, mean 0.40 ms, remote 16419 calls, mean 1.38 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-off-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache off, parallel quotes on, async events, dbs split)

1500 orders in 6.5 s = 229.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 27.5 | 69.5 | 66.2 | 92.9 | 98.7 | 112.6 | 117.0 |
| placeOrder | declined | 27 | 46.0 | 62.5 | 61.1 | 83.0 | 85.2 | 85.5 | 85.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.7 | 2.4 | 4.3 | 5.3 | 8.5 |
| orders -> shipping.quote [remote] | 1500 |  | 2.5 | 2.2 | 4.4 | 5.6 | 9.4 |
| orders -> pricing.quote [remote] | 1500 |  | 2.2 | 1.9 | 3.7 | 4.9 | 9.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.1 | 3.9 | 7.2 |
| orders -> notifications.send [local] | 1473 |  | 1.7 | 1.6 | 2.8 | 3.8 | 9.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.8 | 3.8 | 5.4 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.4 | 2.0 | 7.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.1 | 3.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.5 | 0.9 | 1.2 | 4.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.6 | 2.0 | 7.9 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.2 | 8.2 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.7 | 0.6 | 1.4 | 2.1 | 8.2 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.2 | 0.4 | 0.6 | 4.3 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 4.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.8 | 1.8 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.2 | 0.5 | 0.7 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 1.9 | 1.8 | 2.5 | 4.1 | 4.1 |

By binding: local 13500 calls, mean 0.48 ms, remote 16419 calls, mean 1.45 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-off-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes off, async outbox, dbs split)

1500 orders in 6.4 s = 235.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.1 | 67.8 | 65.1 | 88.8 | 95.3 | 108.3 | 124.6 |
| placeOrder | declined | 27 | 44.0 | 63.5 | 59.6 | 87.8 | 92.2 | 93.9 | 93.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4750 | 63.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.8 | 1.7 | 3.0 | 3.8 | 6.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.7 | 3.5 | 5.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.7 | 3.5 | 5.1 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.0 | 4.2 | 6.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.7 | 2.4 | 8.1 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 0.9 | 2.0 | 2.8 | 5.5 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.7 | 3.1 | 7.5 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.8 | 2.4 | 4.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.8 | 4.5 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.7 | 1.4 | 2.2 | 6.3 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 4.3 |
| notifications -> customers.getCustomer [remote] | 758 |  | 0.6 | 0.5 | 1.5 | 1.9 | 2.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 0.9 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 0.9 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 3.2 | 3.4 | 3.4 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 715 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9989 calls, mean 0.01 ms, local 8261 calls, mean 0.41 ms, remote 11669 calls, mean 1.37 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-on-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes off, async events, dbs split)

1500 orders in 6.3 s = 237.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 25.6 | 67.0 | 65.4 | 86.4 | 92.5 | 109.0 | 126.2 |
| placeOrder | declined | 27 | 44.7 | 62.3 | 59.0 | 75.0 | 81.1 | 92.7 | 92.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5508 | 74.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.8 | 1.7 | 2.7 | 3.9 | 9.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.7 | 1.6 | 2.7 | 3.4 | 7.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.5 | 3.3 | 5.0 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.2 | 4.2 | 9.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.1 | 1.7 | 2.4 | 7.0 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 1.0 | 2.0 | 2.7 | 4.6 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.7 | 3.9 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.7 | 2.3 | 3.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.8 | 1.5 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.8 | 1.4 | 2.1 | 4.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.7 | 8.2 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.7 | 0.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.9 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.6 |
| orders -> inventory.release [remote] | 27 |  | 1.8 | 1.7 | 3.3 | 3.4 | 3.4 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10747 calls, mean 0.01 ms, local 8261 calls, mean 0.39 ms, remote 10911 calls, mean 1.40 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-on-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox, dbs split)

1500 orders in 5.9 s = 255.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 31.9 | 62.4 | 61.3 | 79.5 | 86.2 | 93.7 | 107.4 |
| placeOrder | declined | 27 | 42.9 | 58.7 | 53.6 | 76.3 | 79.9 | 88.4 | 88.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3775 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.5 | 4.9 | 7.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 2.9 | 3.4 | 5.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 2.9 | 3.6 | 7.6 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.6 | 3.4 | 7.0 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.1 | 4.3 | 7.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.7 | 2.5 | 7.8 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 0.9 | 1.8 | 3.0 | 3.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.0 | 1.9 |
| shipping -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.5 | 2.1 | 5.8 |
| pricing -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.6 | 2.2 | 6.0 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.5 | 2.5 | 5.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 0.9 |
| notifications -> customers.getCustomer [remote] | 766 |  | 0.6 | 0.5 | 1.5 | 1.9 | 2.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.3 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 1.1 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.2 | 3.1 | 3.1 | 3.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 707 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9014 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12644 calls, mean 1.45 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-on-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async events, dbs split)

1500 orders in 6.1 s = 247.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.5 | 64.4 | 61.5 | 86.5 | 92.9 | 104.7 | 114.8 |
| placeOrder | declined | 27 | 43.3 | 61.8 | 58.9 | 84.0 | 86.0 | 86.2 | 86.2 |

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
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.8 | 3.1 | 3.9 | 5.1 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.7 | 5.3 | 8.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.8 | 3.2 | 4.0 | 7.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 3.1 | 4.4 | 10.9 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 3.3 | 4.8 | 7.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 2.0 | 2.8 | 7.4 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 0.9 | 1.5 | 2.0 | 5.4 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.1 | 3.4 |
| shipping -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.8 | 2.6 | 5.4 |
| pricing -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.8 | 2.5 | 5.4 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.7 | 2.5 | 4.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.8 | 4.5 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.8 | 2.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 3.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 2.2 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 1.8 | 3.5 | 3.5 | 3.5 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.2 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 533 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9780 calls, mean 0.01 ms, local 8261 calls, mean 0.45 ms, remote 11878 calls, mean 1.57 ms. 19.9 module-to-module calls per order.

Raw samples: `split-dbs-cache-on-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
