# Performance run 20261027T200332Z

- Started: 2026-10-27T20:03:32+00:00
- Commit: a3f7372 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes groups --cache both --parallel both --async both --dbs per-module --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/groups-8db`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups cache off parallel off async outbox | 165 | 1473 | 27 | 39.2 | 96.4 | 93.2 | 134.0 | 147.3 | 215.1 | 251.2 | 19.9 | PASS |
| groups cache off parallel off async events | 200 | 1473 | 27 | 35.7 | 79.6 | 74.7 | 107.7 | 117.7 | 153.9 | 173.8 | 19.9 | PASS |
| groups cache off parallel on async outbox | 239 | 1473 | 27 | 30.0 | 66.7 | 64.1 | 88.6 | 95.5 | 105.7 | 116.2 | 19.9 | PASS |
| groups cache off parallel on async events | 227 | 1473 | 27 | 26.6 | 70.3 | 67.0 | 93.4 | 100.1 | 114.4 | 124.7 | 19.9 | PASS |
| groups cache on parallel off async outbox | 227 | 1473 | 27 | 26.6 | 70.1 | 67.6 | 91.7 | 99.6 | 109.7 | 120.5 | 19.9 | PASS |
| groups cache on parallel off async events | 233 | 1473 | 27 | 31.9 | 68.6 | 64.3 | 90.5 | 98.8 | 106.5 | 122.5 | 19.9 | PASS |
| groups cache on parallel on async outbox | 251 | 1473 | 27 | 27.9 | 63.6 | 60.8 | 82.7 | 87.3 | 96.8 | 111.6 | 19.9 | PASS |
| groups cache on parallel on async events | 244 | 1473 | 27 | 26.0 | 65.4 | 63.1 | 84.4 | 92.9 | 101.0 | 106.1 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (parallel off) (async outbox) | 165 | 227 | +37% | 93.2 | 67.6 | -27% | 147.3 | 99.6 | -32% | 215.1 | 109.7 | -49% |
| groups (parallel off) (async events) | 200 | 233 | +16% | 74.7 | 64.3 | -14% | 117.7 | 98.8 | -16% | 153.9 | 106.5 | -31% |
| groups (parallel on) (async outbox) | 239 | 251 | +5% | 64.1 | 60.8 | -5% | 95.5 | 87.3 | -9% | 105.7 | 96.8 | -8% |
| groups (parallel on) (async events) | 227 | 244 | +7% | 67.0 | 63.1 | -6% | 100.1 | 92.9 | -7% | 114.4 | 101.0 | -12% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (cache off) (async outbox) | 165 | 239 | +45% | 93.2 | 64.1 | -31% | 147.3 | 95.5 | -35% | 215.1 | 105.7 | -51% |
| groups (cache off) (async events) | 200 | 227 | +13% | 74.7 | 67.0 | -10% | 117.7 | 100.1 | -15% | 153.9 | 114.4 | -26% |
| groups (cache on) (async outbox) | 227 | 251 | +10% | 67.6 | 60.8 | -10% | 99.6 | 87.3 | -12% | 109.7 | 96.8 | -12% |
| groups (cache on) (async events) | 233 | 244 | +5% | 64.3 | 63.1 | -2% | 98.8 | 92.9 | -6% | 106.5 | 101.0 | -5% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (cache off) (parallel off) | 165 | 200 | +21% | 93.2 | 74.7 | -20% | 147.3 | 117.7 | -20% | 215.1 | 153.9 | -28% |
| groups (cache off) (parallel on) | 239 | 227 | -5% | 64.1 | 67.0 | +5% | 95.5 | 100.1 | +5% | 105.7 | 114.4 | +8% |
| groups (cache on) (parallel off) | 227 | 233 | +2% | 67.6 | 64.3 | -5% | 99.6 | 98.8 | -1% | 109.7 | 106.5 | -3% |
| groups (cache on) (parallel on) | 251 | 244 | -3% | 60.8 | 63.1 | +4% | 87.3 | 92.9 | +6% | 96.8 | 101.0 | +4% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups cache off parallel off async outbox | 11.0 | 3,812 | 3,812 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel off async events | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel on async outbox | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel on async events | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| groups cache on parallel off async outbox | 7.8 | 2,837 | 2,837 | 100% | 165 to 165 | 200 to 200 |
| groups cache on parallel off async events | 7.3 | 2,686 | 2,686 | 100% | 170 to 170 | 199 to 199 |
| groups cache on parallel on async outbox | 8.4 | 3,036 | 3,036 | 100% | 160 to 160 | 200 to 200 |
| groups cache on parallel on async events | 7.9 | 2,883 | 2,883 | 100% | 164 to 164 | 200 to 200 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups cache off parallel off async outbox | groups cache off parallel off async events | groups cache off parallel on async outbox | groups cache off parallel on async events | groups cache on parallel off async outbox | groups cache on parallel off async events | groups cache on parallel on async outbox | groups cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 3.2 | 2.6 | 2.6 | 2.7 | 1.9 | 1.9 | 1.9 | 2.0 |
| orders -> shipping.quote | 2.6 | 2.1 | 2.3 | 2.5 | 1.1 | 1.1 | 1.9 | 2.0 |
| orders -> inventory.reserve | 2.2 | 1.8 | 1.8 | 1.8 | 1.8 | 1.8 | 1.9 | 2.0 |
| orders -> pricing.quote | 2.4 | 1.9 | 2.0 | 2.2 | 1.6 | 1.6 | 1.6 | 1.7 |
| orders -> payments.authorize | 2.0 | 1.6 | 1.6 | 1.7 | 1.8 | 1.7 | 1.7 | 1.8 |
| orders -> notifications.send | 1.2 | 1.6 | 1.2 | 1.8 | 1.0 | 0.9 | 1.0 | 1.0 |
| orders -> payments.capture | 1.3 | 1.0 | 1.0 | 1.1 | 1.2 | 1.1 | 1.2 | 1.2 |
| shipping -> customers.getCustomer | 0.9 | 0.7 | 0.7 | 0.8 | 0.0 | 0.0 | 0.3 | 0.3 |
| shipping -> inventory.getStock | 0.5 | 0.4 | 0.5 | 0.5 | 0.3 | 0.3 | 0.3 | 0.4 |
| pricing -> customers.getCustomer | 0.9 | 0.7 | 0.7 | 0.8 | 0.7 | 0.7 | 0.6 | 0.7 |
| orders -> customers.getCustomer | 0.8 | 0.7 | 0.7 | 0.8 | 0.5 | 0.5 | 0.6 | 0.6 |
| shipping -> inventory.getReservation | 0.5 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| notifications -> customers.getCustomer | 0.5 | 0.6 | 0.5 | 0.7 | 0.3 | 0.0 | 0.3 | 0.0 |
| inventory -> catalog.productExists | 0.3 | 0.2 | 0.2 | 0.3 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod | 0.3 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 |
| orders -> inventory.release | 2.1 | 2.2 | 2.1 | 1.9 | 2.4 | 1.7 | 2.2 | 1.9 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups cache off parallel off async outbox | groups cache off parallel off async events | groups cache off parallel on async outbox | groups cache off parallel on async events | groups cache on parallel off async outbox | groups cache on parallel off async events | groups cache on parallel on async outbox | groups cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 5.6 | 4.2 | 3.9 | 4.4 | 3.0 | 3.1 | 3.1 | 3.2 |
| orders -> shipping.quote | 5.0 | 3.6 | 3.9 | 4.4 | 2.2 | 2.1 | 3.5 | 3.8 |
| orders -> inventory.reserve | 3.7 | 2.9 | 2.8 | 3.0 | 2.9 | 2.8 | 3.1 | 3.1 |
| orders -> pricing.quote | 4.3 | 3.3 | 3.3 | 3.9 | 3.2 | 3.2 | 3.0 | 3.3 |
| orders -> payments.authorize | 3.3 | 2.8 | 2.6 | 2.8 | 2.9 | 2.8 | 2.8 | 2.9 |
| orders -> notifications.send | 2.4 | 2.4 | 2.0 | 2.9 | 1.7 | 1.3 | 1.7 | 1.5 |
| orders -> payments.capture | 2.1 | 1.6 | 1.6 | 1.7 | 1.9 | 1.7 | 1.9 | 2.0 |
| shipping -> customers.getCustomer | 1.7 | 1.2 | 1.2 | 1.5 | 0.0 | 0.0 | 1.2 | 1.4 |
| shipping -> inventory.getStock | 1.0 | 0.8 | 0.8 | 0.9 | 0.6 | 0.6 | 0.7 | 0.7 |
| pricing -> customers.getCustomer | 1.7 | 1.3 | 1.3 | 1.5 | 1.7 | 1.6 | 1.4 | 1.6 |
| orders -> customers.getCustomer | 1.8 | 1.2 | 1.3 | 1.4 | 1.3 | 1.2 | 1.3 | 1.6 |
| shipping -> inventory.getReservation | 0.7 | 0.6 | 0.5 | 0.6 | 0.5 | 0.5 | 0.6 | 0.6 |
| notifications -> customers.getCustomer | 1.1 | 1.1 | 1.0 | 1.4 | 0.9 | 0.0 | 0.9 | 0.0 |
| inventory -> catalog.productExists | 0.5 | 0.4 | 0.4 | 0.4 | 0.3 | 0.3 | 0.3 | 0.4 |
| pricing -> catalog.getProduct | 0.4 | 0.3 | 0.3 | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.6 | 0.4 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| orders -> inventory.release | 3.0 | 4.3 | 2.9 | 2.9 | 3.4 | 3.1 | 3.1 | 2.7 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | groups cache off parallel off async outbox | groups cache off parallel off async events | groups cache off parallel on async outbox | groups cache off parallel on async events | groups cache on parallel off async outbox | groups cache on parallel off async events | groups cache on parallel on async outbox | groups cache on parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | 64.0% | 74.0% | 50.8% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| groups cache off parallel off async outbox | - | 13500 calls, 0.44 ms | - | - | 16419 calls, 1.60 ms |
| groups cache off parallel off async events | - | 13500 calls, 0.43 ms | - | - | 16419 calls, 1.31 ms |
| groups cache off parallel on async outbox | - | 13500 calls, 0.40 ms | - | - | 16419 calls, 1.34 ms |
| groups cache off parallel on async events | - | 13500 calls, 0.49 ms | - | - | 16419 calls, 1.47 ms |
| groups cache on parallel off async outbox | 10003 calls, 0.01 ms | 8261 calls, 0.42 ms | - | - | 11655 calls, 1.41 ms |
| groups cache on parallel off async events | 10747 calls, 0.01 ms | 8261 calls, 0.41 ms | - | - | 10911 calls, 1.44 ms |
| groups cache on parallel on async outbox | 9023 calls, 0.01 ms | 8261 calls, 0.44 ms | - | - | 12635 calls, 1.47 ms |
| groups cache on parallel on async events | 9780 calls, 0.01 ms | 8261 calls, 0.47 ms | - | - | 11878 calls, 1.60 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### groups (cache off, parallel quotes off, async outbox, dbs per-module)

1500 orders in 9.1 s = 165.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 39.2 | 96.4 | 93.2 | 134.0 | 147.3 | 215.1 | 251.2 |
| placeOrder | declined | 27 | 57.2 | 90.4 | 86.6 | 124.0 | 126.2 | 225.6 | 225.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.2 | 2.9 | 5.6 | 7.9 | 10.3 |
| orders -> shipping.quote [remote] | 1500 |  | 2.6 | 2.2 | 5.0 | 6.3 | 16.7 |
| orders -> pricing.quote [remote] | 1500 |  | 2.4 | 2.0 | 4.3 | 6.7 | 63.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.2 | 2.1 | 3.7 | 5.1 | 15.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.0 | 1.7 | 3.3 | 4.6 | 11.1 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.7 | 1.7 | 3.0 | 11.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.3 | 1.1 | 2.1 | 3.3 | 8.3 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.1 | 2.4 | 3.2 | 5.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.5 | 1.0 | 1.4 | 3.8 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.9 | 0.8 | 1.7 | 2.8 | 9.7 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.8 | 3.5 | 6.4 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.5 | 1.1 | 1.7 | 4.8 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 2.9 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.9 | 15.9 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.5 | 0.4 | 0.7 | 1.1 | 5.9 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.3 | 0.6 | 0.8 | 8.9 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 2.0 | 3.0 | 3.4 | 3.4 |

By binding: local 13500 calls, mean 0.44 ms, remote 16419 calls, mean 1.60 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes off, async events, dbs per-module)

1500 orders in 7.5 s = 200.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 35.7 | 79.6 | 74.7 | 107.7 | 117.7 | 153.9 | 173.8 |
| placeOrder | declined | 27 | 52.1 | 72.9 | 66.6 | 100.3 | 104.3 | 121.0 | 121.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.4 | 4.2 | 5.1 | 12.4 |
| orders -> shipping.quote [remote] | 1500 |  | 2.1 | 1.9 | 3.6 | 4.8 | 10.3 |
| orders -> pricing.quote [remote] | 1500 |  | 1.9 | 1.7 | 3.3 | 4.5 | 5.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.9 | 3.8 | 5.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.8 | 3.7 | 8.7 |
| orders -> notifications.send [local] | 1473 |  | 1.6 | 1.4 | 2.4 | 3.1 | 52.0 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.6 | 1.2 | 1.7 | 7.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 1.0 | 1.6 | 2.1 | 4.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.8 | 1.0 | 2.3 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.6 | 3.1 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.2 | 1.7 | 48.5 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.6 | 0.5 | 1.1 | 1.6 | 3.5 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.2 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.7 | 2.8 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.7 | 2.0 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 1.6 | 4.3 | 11.8 | 11.8 |

By binding: local 13500 calls, mean 0.43 ms, remote 16419 calls, mean 1.31 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async outbox, dbs per-module)

1500 orders in 6.3 s = 239.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.0 | 66.7 | 64.1 | 88.6 | 95.5 | 105.7 | 116.2 |
| placeOrder | declined | 27 | 44.5 | 60.3 | 59.1 | 76.8 | 85.0 | 90.8 | 90.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.4 | 3.9 | 4.9 | 9.5 |
| orders -> shipping.quote [remote] | 1500 |  | 2.3 | 2.1 | 3.9 | 5.0 | 8.6 |
| orders -> pricing.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.3 | 4.2 | 5.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.8 | 3.7 | 6.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.6 | 3.4 | 8.2 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.7 | 1.2 | 1.6 | 7.3 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.0 | 2.0 | 3.0 | 4.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 1.0 | 1.6 | 2.1 | 4.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.4 | 0.8 | 1.0 | 3.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.7 | 1.3 | 1.7 | 3.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.8 | 4.2 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.0 | 1.7 | 2.3 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 3.6 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.5 | 3.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 1.8 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.5 | 0.6 | 1.6 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 2.1 | 2.9 | 3.7 | 3.7 |

By binding: local 13500 calls, mean 0.40 ms, remote 16419 calls, mean 1.34 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async events, dbs per-module)

1500 orders in 6.6 s = 226.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.6 | 70.3 | 67.0 | 93.4 | 100.1 | 114.4 | 124.7 |
| placeOrder | declined | 27 | 46.4 | 65.0 | 61.2 | 82.8 | 87.3 | 111.5 | 111.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.7 | 2.5 | 4.4 | 5.5 | 9.0 |
| orders -> shipping.quote [remote] | 1500 |  | 2.5 | 2.3 | 4.4 | 5.7 | 8.2 |
| orders -> pricing.quote [remote] | 1500 |  | 2.2 | 2.0 | 3.9 | 5.0 | 7.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 3.0 | 3.9 | 6.9 |
| orders -> notifications.send [local] | 1473 |  | 1.8 | 1.6 | 2.9 | 3.7 | 5.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.8 | 3.7 | 4.6 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.5 | 2.1 | 6.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.3 | 4.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.5 | 0.9 | 1.2 | 3.5 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.1 | 4.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.4 | 2.1 | 5.2 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.7 | 0.6 | 1.4 | 1.9 | 4.5 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.2 | 0.4 | 0.7 | 3.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 3.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.7 | 2.1 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.2 | 0.5 | 0.7 | 1.1 |
| orders -> inventory.release [remote] | 27 |  | 1.9 | 1.8 | 2.9 | 3.0 | 3.0 |

By binding: local 13500 calls, mean 0.49 ms, remote 16419 calls, mean 1.47 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async outbox, dbs per-module)

1500 orders in 6.6 s = 227.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.6 | 70.1 | 67.6 | 91.7 | 99.6 | 109.7 | 120.5 |
| placeOrder | declined | 27 | 42.1 | 65.2 | 60.5 | 84.7 | 91.5 | 112.5 | 112.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4764 | 64.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.0 | 4.2 | 8.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.9 | 3.7 | 11.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 2.9 | 3.7 | 6.2 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.6 | 3.2 | 4.1 | 7.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.4 | 7.0 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 1.0 | 2.2 | 3.0 | 4.6 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 0.9 | 1.7 | 3.0 | 4.5 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.9 | 2.5 | 7.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.6 | 0.9 | 1.8 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.7 | 1.4 | 2.1 | 4.3 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 4.7 |
| notifications -> customers.getCustomer [remote] | 744 |  | 0.6 | 0.5 | 1.4 | 1.9 | 2.9 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.7 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.8 | 1.4 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.1 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.4 | 3.4 | 3.9 | 3.9 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.6 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 729 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10003 calls, mean 0.01 ms, local 8261 calls, mean 0.42 ms, remote 11655 calls, mean 1.41 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async events, dbs per-module)

1500 orders in 6.4 s = 232.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 31.9 | 68.6 | 64.3 | 90.5 | 98.8 | 106.5 | 122.5 |
| placeOrder | declined | 27 | 47.7 | 62.9 | 61.2 | 77.8 | 80.6 | 102.1 | 102.1 |

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
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.7 | 3.1 | 3.9 | 6.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.8 | 3.6 | 5.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.8 | 3.6 | 5.2 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.2 | 4.4 | 6.7 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.1 | 1.7 | 2.2 | 6.7 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 1.0 | 2.1 | 3.0 | 4.5 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.7 | 3.8 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.8 | 2.4 | 4.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.8 | 3.1 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.7 | 1.4 | 2.3 | 7.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 3.8 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.8 | 1.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.5 | 0.6 | 1.1 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 1.7 | 1.6 | 3.1 | 3.3 | 3.3 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.3 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10747 calls, mean 0.01 ms, local 8261 calls, mean 0.41 ms, remote 10911 calls, mean 1.44 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox, dbs per-module)

1500 orders in 6.0 s = 250.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 27.9 | 63.6 | 60.8 | 82.7 | 87.3 | 96.8 | 111.6 |
| placeOrder | declined | 27 | 43.5 | 60.5 | 58.9 | 80.1 | 80.9 | 85.7 | 85.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3784 | 50.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.1 | 3.9 | 6.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.1 | 3.8 | 12.4 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.5 | 4.6 | 8.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.8 | 3.6 | 8.9 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.0 | 4.2 | 7.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.7 | 4.0 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 0.9 | 1.7 | 3.0 | 4.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 0.9 | 1.4 |
| pricing -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.5 | 2.2 | 6.6 |
| shipping -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.5 | 2.2 | 6.7 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.5 | 2.1 | 6.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.7 | 2.7 |
| notifications -> customers.getCustomer [remote] | 759 |  | 0.6 | 0.5 | 1.3 | 1.9 | 2.6 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.7 | 1.8 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 3.1 | 3.2 | 3.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 714 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 533 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9023 calls, mean 0.01 ms, local 8261 calls, mean 0.44 ms, remote 12635 calls, mean 1.47 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events, dbs per-module)

1500 orders in 6.2 s = 243.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.0 | 65.4 | 63.1 | 84.4 | 92.9 | 101.0 | 106.1 |
| placeOrder | declined | 27 | 45.2 | 61.1 | 58.2 | 80.8 | 83.9 | 86.0 | 86.0 |

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
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.9 | 3.8 | 4.9 | 8.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 3.1 | 3.7 | 5.8 |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.8 | 3.2 | 3.9 | 5.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 2.9 | 3.7 | 7.4 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 3.3 | 4.5 | 8.7 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 2.0 | 2.4 | 3.9 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 1.0 | 1.5 | 2.0 | 12.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.7 | 1.0 | 2.3 |
| shipping -> customers.getCustomer [remote] | 968 |  | 1.1 | 1.0 | 1.7 | 2.4 | 7.9 |
| pricing -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.7 | 2.3 | 7.8 |
| orders -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.8 | 1.7 | 2.6 | 8.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.7 | 1.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.8 | 1.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.1 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.1 |
| orders -> inventory.release [remote] | 27 |  | 1.9 | 1.9 | 2.7 | 2.9 | 2.9 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9780 calls, mean 0.01 ms, local 8261 calls, mean 0.47 ms, remote 11878 calls, mean 1.60 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
