# Performance run 20261025T235140Z

- Started: 2026-10-25T23:51:40+00:00
- Commit: 3e2d553
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes all --cache both --orders 1500 --warmup 200 --concurrency 16`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith cache off | 438 | 1473 | 27 | 16.4 | 36.4 | 34.9 | 43.2 | 47.8 | 61.1 | 73.9 | 19.9 | PASS |
| modulith cache on | 465 | 1473 | 27 | 12.6 | 34.2 | 32.7 | 41.7 | 47.5 | 53.9 | 58.1 | 19.9 | PASS |
| macro-modulith cache off | 440 | 1473 | 27 | 13.8 | 36.2 | 34.7 | 43.6 | 46.9 | 55.1 | 62.2 | 19.9 | PASS |
| macro-modulith cache on | 466 | 1473 | 27 | 12.8 | 34.2 | 32.6 | 43.2 | 47.2 | 54.6 | 59.1 | 19.9 | PASS |
| groups cache off | 239 | 1473 | 27 | 26.5 | 66.8 | 63.6 | 91.5 | 96.7 | 107.3 | 122.7 | 19.9 | PASS |
| groups cache on | 251 | 1473 | 27 | 20.2 | 63.4 | 61.8 | 81.5 | 87.2 | 100.3 | 108.2 | 19.9 | PASS |
| microservices cache off | 141 | 1473 | 27 | 41.2 | 113.1 | 107.4 | 152.1 | 170.5 | 199.9 | 224.7 | 19.9 | PASS |
| microservices cache on | 167 | 1473 | 27 | 34.1 | 95.3 | 93.6 | 125.9 | 134.8 | 175.0 | 210.0 | 19.9 | PASS |
| microservices-macro cache off | 141 | 1473 | 27 | 40.3 | 113.3 | 109.3 | 152.5 | 164.4 | 197.1 | 224.4 | 19.9 | PASS |
| microservices-macro cache on | 173 | 1473 | 27 | 38.6 | 92.3 | 89.6 | 120.2 | 132.2 | 168.5 | 185.2 | 19.9 | PASS |
| loopback-codec cache off | 436 | 1473 | 27 | 15.5 | 36.5 | 35.2 | 43.3 | 49.1 | 55.1 | 63.0 | 19.9 | PASS |
| loopback-codec cache on | 464 | 1473 | 27 | 11.1 | 34.4 | 32.8 | 42.2 | 47.9 | 53.2 | 57.8 | 19.9 | PASS |
| loopback-http cache off | 292 | 1473 | 27 | 21.2 | 54.7 | 52.0 | 69.2 | 75.3 | 84.4 | 99.3 | 19.9 | PASS |
| loopback-http cache on | 332 | 1473 | 27 | 21.8 | 48.0 | 45.4 | 61.3 | 65.5 | 78.9 | 88.9 | 19.9 | PASS |
| split-dbs cache off | 94 | 1473 | 27 | 54.6 | 169.9 | 173.0 | 222.4 | 244.6 | 373.2 | 422.8 | 19.9 | PASS |
| split-dbs cache on | 272 | 1473 | 27 | 29.1 | 58.6 | 55.7 | 76.2 | 83.9 | 104.3 | 134.1 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith | 438 | 465 | +6% | 34.9 | 32.7 | -6% | 47.8 | 47.5 | -1% | 61.1 | 53.9 | -12% |
| macro-modulith | 440 | 466 | +6% | 34.7 | 32.6 | -6% | 46.9 | 47.2 | +1% | 55.1 | 54.6 | -1% |
| groups | 239 | 251 | +5% | 63.6 | 61.8 | -3% | 96.7 | 87.2 | -10% | 107.3 | 100.3 | -6% |
| microservices | 141 | 167 | +19% | 107.4 | 93.6 | -13% | 170.5 | 134.8 | -21% | 199.9 | 175.0 | -12% |
| microservices-macro | 141 | 173 | +23% | 109.3 | 89.6 | -18% | 164.4 | 132.2 | -20% | 197.1 | 168.5 | -15% |
| loopback-codec | 436 | 464 | +6% | 35.2 | 32.8 | -7% | 49.1 | 47.9 | -2% | 55.1 | 53.2 | -3% |
| loopback-http | 292 | 332 | +14% | 52.0 | 45.4 | -13% | 75.3 | 65.5 | -13% | 84.4 | 78.9 | -7% |
| split-dbs | 94 | 272 | +190% | 173.0 | 55.7 | -68% | 244.6 | 83.9 | -66% | 373.2 | 104.3 | -72% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups cache off | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| groups cache on | 8.4 | 3,039 | 3,039 | 100% | 160 to 160 | 200 to 200 |
| microservices cache off | 20.1 | 5,796 | 5,796 | 100% | 130 to 130 | 159 to 159 |
| microservices cache on | 14.3 | 4,557 | 4,557 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache off | 20.1 | 5,795 | 5,795 | 100% | 130 to 130 | 159 to 159 |
| microservices-macro cache on | 14.3 | 4,557 | 4,557 | 100% | 145 to 145 | 173 to 173 |
| loopback-http cache off | 20.1 | 5,818 | 5,818 | 100% | 130 to 130 | 160 to 160 |
| loopback-http cache on | 14.0 | 4,474 | 4,474 | 100% | 146 to 146 | 173 to 173 |
| split-dbs cache off | 11.0 | 3,814 | 3,814 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache on | 8.4 | 3,039 | 3,039 | 100% | 160 to 160 | 200 to 200 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.0 | 0.7 | 1.0 | 0.7 | 2.3 | 1.9 | 6.3 | 4.5 | 6.1 | 4.2 | 1.0 | 0.7 | 2.5 | 1.7 | 1.9 | 1.7 |
| orders -> shipping.createShipment | 1.3 | 1.2 | 1.3 | 1.1 | 2.6 | 2.0 | 4.4 | 3.4 | 4.4 | 3.3 | 1.3 | 1.2 | 2.1 | 1.7 | 4.9 | 1.8 |
| orders -> pricing.quote | 0.7 | 0.4 | 0.7 | 0.5 | 2.0 | 1.6 | 5.0 | 3.3 | 4.8 | 3.1 | 0.8 | 0.5 | 1.8 | 1.0 | 1.7 | 1.5 |
| orders -> inventory.reserve | 1.1 | 1.1 | 1.0 | 1.1 | 1.8 | 1.9 | 2.3 | 2.2 | 2.3 | 2.2 | 1.0 | 1.1 | 1.3 | 1.3 | 4.0 | 1.7 |
| orders -> payments.authorize | 0.7 | 0.7 | 0.7 | 0.7 | 1.6 | 1.7 | 3.1 | 2.8 | 3.1 | 2.7 | 0.8 | 0.7 | 1.3 | 1.2 | 3.9 | 1.5 |
| shipping -> inventory.getStock | 0.4 | 0.2 | 0.4 | 0.2 | 0.5 | 0.3 | 2.2 | 1.5 | 2.2 | 1.4 | 0.4 | 0.2 | 0.9 | 0.5 | 0.4 | 0.3 |
| orders -> notifications.send | 0.6 | 0.6 | 0.6 | 0.6 | 1.1 | 0.9 | 2.6 | 2.4 | 2.6 | 2.4 | 0.6 | 0.6 | 1.2 | 1.8 | 4.2 | 0.9 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 1.0 | 1.1 | 1.5 | 1.4 | 1.5 | 1.4 | 0.5 | 0.5 | 0.7 | 0.7 | 3.2 | 1.1 |
| shipping -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.7 | 0.3 | 0.8 | 0.4 | 0.8 | 0.3 | 0.2 | 0.1 | 0.4 | 0.2 | 0.6 | 0.3 |
| inventory -> catalog.productExists | 0.2 | 0.0 | 0.2 | 0.0 | 0.2 | 0.1 | 1.2 | 0.4 | 1.2 | 0.4 | 0.2 | 0.0 | 0.4 | 0.1 | 0.2 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.0 | 0.2 | 0.1 | 0.2 | 0.1 | 1.1 | 0.4 | 1.0 | 0.4 | 0.2 | 0.0 | 0.4 | 0.1 | 0.2 | 0.1 |
| shipping -> inventory.getReservation | 0.2 | 0.2 | 0.3 | 0.3 | 0.3 | 0.4 | 1.0 | 1.1 | 1.1 | 1.0 | 0.3 | 0.3 | 0.5 | 0.5 | 0.3 | 0.3 |
| pricing -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.8 | 0.6 | 1.0 | 0.9 | 1.0 | 0.8 | 0.2 | 0.1 | 0.4 | 0.3 | 0.6 | 0.6 |
| orders -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.7 | 0.6 | 0.9 | 0.8 | 0.9 | 0.7 | 0.2 | 0.1 | 0.5 | 0.3 | 0.6 | 0.5 |
| payments -> customers.getPaymentMethod | 0.2 | 0.1 | 0.2 | 0.1 | 0.2 | 0.2 | 1.2 | 1.0 | 1.2 | 0.9 | 0.2 | 0.1 | 0.4 | 0.3 | 0.2 | 0.2 |
| notifications -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.1 | 0.5 | 0.3 | 0.9 | 0.8 | 0.9 | 0.8 | 0.1 | 0.1 | 0.4 | 0.3 | 0.6 | 0.3 |
| orders -> inventory.release | 1.2 | 1.2 | 1.2 | 1.2 | 2.1 | 2.2 | 2.2 | 2.3 | 2.3 | 2.2 | 1.2 | 1.2 | 1.6 | 2.1 | 5.5 | 2.2 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.7 | 1.3 | 1.7 | 1.3 | 4.0 | 3.7 | 12.0 | 9.5 | 11.4 | 8.8 | 1.7 | 1.3 | 4.5 | 3.5 | 3.1 | 3.3 |
| orders -> shipping.createShipment | 1.8 | 1.6 | 1.7 | 1.6 | 4.1 | 2.9 | 6.8 | 5.2 | 6.5 | 4.9 | 1.8 | 1.6 | 3.0 | 2.4 | 8.7 | 2.9 |
| orders -> pricing.quote | 1.1 | 0.8 | 1.2 | 0.8 | 3.5 | 3.1 | 8.6 | 6.9 | 8.2 | 6.3 | 1.2 | 0.9 | 2.9 | 2.0 | 2.6 | 2.8 |
| orders -> inventory.reserve | 1.7 | 1.7 | 1.6 | 1.7 | 2.9 | 3.0 | 3.6 | 3.4 | 3.7 | 3.4 | 1.6 | 1.6 | 2.0 | 2.1 | 7.5 | 2.7 |
| orders -> payments.authorize | 1.1 | 1.0 | 1.0 | 1.1 | 2.6 | 2.8 | 5.2 | 4.9 | 4.9 | 4.7 | 1.1 | 1.1 | 1.8 | 1.8 | 7.5 | 2.5 |
| shipping -> inventory.getStock | 0.6 | 0.4 | 0.5 | 0.5 | 0.8 | 0.7 | 3.9 | 3.6 | 3.7 | 3.4 | 0.5 | 0.5 | 1.3 | 1.0 | 0.6 | 0.6 |
| orders -> notifications.send | 1.1 | 0.9 | 1.1 | 1.0 | 2.0 | 1.6 | 5.2 | 4.4 | 5.4 | 4.7 | 1.1 | 0.9 | 2.2 | 4.8 | 8.9 | 1.6 |
| orders -> payments.capture | 0.6 | 0.6 | 0.6 | 0.7 | 1.6 | 1.8 | 2.3 | 2.3 | 2.4 | 2.3 | 0.6 | 0.6 | 1.0 | 1.0 | 6.9 | 1.6 |
| shipping -> customers.getCustomer | 0.3 | 0.2 | 0.2 | 0.2 | 1.3 | 1.3 | 1.5 | 1.5 | 1.4 | 1.3 | 0.3 | 0.2 | 0.6 | 0.5 | 1.0 | 1.1 |
| inventory -> catalog.productExists | 0.3 | 0.2 | 0.3 | 0.2 | 0.4 | 0.3 | 2.2 | 2.4 | 2.0 | 2.1 | 0.3 | 0.2 | 0.6 | 0.5 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 1.8 | 2.2 | 1.7 | 1.9 | 0.2 | 0.2 | 0.6 | 0.5 | 0.3 | 0.3 |
| shipping -> inventory.getReservation | 0.4 | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 | 1.7 | 1.7 | 1.7 | 1.6 | 0.4 | 0.4 | 0.7 | 0.7 | 0.5 | 0.5 |
| pricing -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 1.3 | 1.5 | 1.7 | 1.8 | 1.5 | 1.7 | 0.3 | 0.3 | 0.6 | 0.6 | 1.0 | 1.3 |
| orders -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 1.3 | 1.4 | 1.7 | 1.8 | 1.6 | 1.6 | 0.3 | 0.3 | 0.7 | 0.7 | 1.1 | 1.3 |
| payments -> customers.getPaymentMethod | 0.2 | 0.3 | 0.2 | 0.2 | 0.4 | 0.5 | 2.1 | 2.2 | 1.9 | 2.0 | 0.3 | 0.3 | 0.6 | 0.6 | 0.4 | 0.5 |
| notifications -> customers.getCustomer | 0.3 | 0.2 | 0.3 | 0.2 | 1.0 | 0.9 | 1.7 | 1.8 | 1.7 | 1.8 | 0.3 | 0.2 | 0.8 | 0.7 | 1.0 | 0.9 |
| orders -> inventory.release | 1.6 | 1.4 | 1.6 | 1.7 | 2.8 | 2.9 | 2.7 | 3.0 | 3.4 | 3.0 | 1.6 | 1.6 | 2.1 | 4.8 | 8.3 | 3.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | 78.0% | - | 78.0% | - | 78.0% | - | 77.7% | - | 77.7% | - | 78.0% | - | 78.0% | - | 78.0% |
| catalog.productExists | - | 78.0% | - | 78.0% | - | 78.0% | - | 77.7% | - | 77.7% | - | 78.0% | - | 78.0% | - | 78.0% |
| customers.getCustomer | - | 50.4% | - | 50.4% | - | 50.7% | - | 45.9% | - | 45.9% | - | 50.4% | - | 50.7% | - | 50.6% |
| customers.getPaymentMethod | - | 35.4% | - | 35.4% | - | 35.4% | - | 35.2% | - | 35.2% | - | 35.4% | - | 35.4% | - | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith cache off | - | 29919 calls, 0.42 ms | - | - | - |
| modulith cache on | 8992 calls, 0.01 ms | 20927 calls, 0.47 ms | - | - | - |
| macro-modulith cache off | - | 29919 calls, 0.42 ms | - | - | - |
| macro-modulith cache on | 8993 calls, 0.01 ms | 20926 calls, 0.47 ms | - | - | - |
| groups cache off | - | 13500 calls, 0.39 ms | - | - | 16419 calls, 1.35 ms |
| groups cache on | 9012 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12646 calls, 1.46 ms |
| microservices cache off | - | - | - | - | 29919 calls, 2.05 ms |
| microservices cache on | 8634 calls, 0.01 ms | - | - | - | 21285 calls, 2.10 ms |
| microservices-macro cache off | - | - | - | - | 29919 calls, 2.02 ms |
| microservices-macro cache on | 8632 calls, 0.01 ms | - | - | - | 21287 calls, 2.00 ms |
| loopback-codec cache off | - | - | 29919 calls, 0.44 ms | - | - |
| loopback-codec cache on | 8991 calls, 0.00 ms | - | 20928 calls, 0.48 ms | - | - |
| loopback-http cache off | - | - | - | 29919 calls, 0.86 ms | - |
| loopback-http cache on | 9012 calls, 0.00 ms | - | - | 20907 calls, 0.93 ms | - |
| split-dbs cache off | - | 13500 calls, 0.68 ms | - | - | 16419 calls, 2.08 ms |
| split-dbs cache on | 9010 calls, 0.01 ms | 8261 calls, 0.41 ms | - | - | 12648 calls, 1.34 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache off, parallel quotes on, async outbox)

1500 orders in 3.4 s = 437.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.4 | 36.4 | 34.9 | 43.2 | 47.8 | 61.1 | 73.9 |
| placeOrder | declined | 27 | 27.4 | 33.3 | 33.4 | 35.7 | 35.9 | 56.9 | 56.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.8 | 2.3 | 6.0 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.3 | 9.9 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.4 | 10.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.1 | 1.3 | 1.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.6 | 0.7 | 8.3 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.1 | 1.7 | 9.2 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.6 | 1.9 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 1.8 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.9 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.8 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 8.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.2 | 0.2 | 0.4 | 0.4 | 0.6 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.7 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 0.4 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.5 | 0.7 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.6 | 2.1 | 2.1 |

By binding: local 29919 calls, mean 0.42 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/modulith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox)

1500 orders in 3.2 s = 465.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 12.6 | 34.2 | 32.7 | 41.7 | 47.5 | 53.9 | 58.1 |
| placeOrder | declined | 27 | 26.1 | 31.7 | 30.5 | 38.1 | 38.3 | 50.9 | 50.9 |

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
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.1 | 8.5 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.3 | 7.5 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.0 | 1.3 | 7.1 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.3 | 2.1 | 7.5 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 0.9 | 1.5 | 2.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.8 | 5.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 2.4 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.8 | 1.3 | 7.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.7 |
| shipping -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 6.7 |
| pricing -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.7 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 2.8 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.0 |
| notifications -> customers.getCustomer [local] | 787 |  | 0.2 | 0.1 | 0.4 | 0.6 | 0.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.3 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 1.4 | 2.2 | 2.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.5 |

By binding: cache 8992 calls, mean 0.01 ms, local 20927 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/modulith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes on, async outbox)

1500 orders in 3.4 s = 440.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 13.8 | 36.2 | 34.7 | 43.6 | 46.9 | 55.1 | 62.2 |
| placeOrder | declined | 27 | 27.5 | 33.6 | 33.0 | 38.6 | 41.8 | 53.0 | 53.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.3 | 1.2 | 1.7 | 2.1 | 12.1 |
| orders -> inventory.reserve [local] | 1500 |  | 1.0 | 1.0 | 1.6 | 2.2 | 6.7 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.3 | 8.2 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.0 | 1.3 | 2.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.7 | 6.4 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.2 | 1.5 | 7.2 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.6 | 2.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 2.6 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 6.0 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 1.0 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 6.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.3 | 0.4 | 6.2 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.9 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 1.2 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.4 | 0.6 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.6 | 1.8 | 1.8 |

By binding: local 29919 calls, mean 0.42 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/macro-modulith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes on, async outbox)

1500 orders in 3.2 s = 465.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 12.8 | 34.2 | 32.6 | 43.2 | 47.2 | 54.6 | 59.1 |
| placeOrder | declined | 27 | 25.4 | 31.2 | 29.3 | 35.8 | 37.1 | 53.7 | 53.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3754 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 2.0 | 2.6 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.2 | 12.3 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.1 | 1.3 | 1.5 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.9 | 9.6 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.4 | 2.1 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 6.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 7.7 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.8 | 1.2 | 8.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.4 | 0.4 | 0.5 |
| orders -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 0.7 |
| shipping -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| pricing -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 0.8 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| notifications -> customers.getCustomer [local] | 787 |  | 0.2 | 0.1 | 0.3 | 0.6 | 0.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 7.5 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 1.7 | 1.7 | 1.7 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.4 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 6.1 |
| orders -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.1 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8993 calls, mean 0.01 ms, local 20926 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/macro-modulith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async outbox)

1500 orders in 6.3 s = 238.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.5 | 66.8 | 63.6 | 91.5 | 96.7 | 107.3 | 122.7 |
| placeOrder | declined | 27 | 44.3 | 61.0 | 56.9 | 81.2 | 85.9 | 86.6 | 86.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.4 | 4.1 | 4.9 | 8.7 |
| orders -> shipping.quote [remote] | 1500 |  | 2.3 | 2.1 | 4.0 | 5.2 | 11.6 |
| orders -> pricing.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.5 | 4.5 | 11.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.9 | 3.6 | 5.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.6 | 3.5 | 5.8 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.7 | 1.3 | 1.7 | 9.4 |
| orders -> notifications.send [local] | 1473 |  | 1.1 | 1.0 | 2.0 | 2.9 | 3.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 1.0 | 1.6 | 2.1 | 4.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.4 | 0.8 | 1.0 | 2.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.3 | 1.7 | 9.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.3 | 2.0 | 10.0 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.0 | 1.7 | 2.0 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 0.8 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.5 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 2.1 | 2.8 | 2.9 | 2.9 |

By binding: local 13500 calls, mean 0.39 ms, remote 16419 calls, mean 1.35 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/groups-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

1500 orders in 6.0 s = 251.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.2 | 63.4 | 61.8 | 81.5 | 87.2 | 100.3 | 108.2 |
| placeOrder | declined | 27 | 40.2 | 58.5 | 55.3 | 75.9 | 76.8 | 89.3 | 89.3 |

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
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.9 | 2.9 | 3.8 | 5.9 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.7 | 4.7 | 9.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.0 | 3.8 | 7.7 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.8 | 3.6 | 9.3 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.1 | 4.1 | 9.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.1 | 1.8 | 2.2 | 3.3 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.6 | 2.8 | 3.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 0.9 | 3.6 |
| shipping -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.6 | 2.2 | 8.4 |
| pricing -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.7 | 2.3 | 7.5 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.6 | 2.4 | 7.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.6 | 0.9 |
| notifications -> customers.getCustomer [remote] | 767 |  | 0.6 | 0.5 | 1.3 | 1.8 | 1.9 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.6 | 3.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.6 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 3.7 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.0 | 2.9 | 3.7 | 3.7 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 706 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9012 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12646 calls, mean 1.46 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/groups-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async outbox)

1500 orders in 10.6 s = 141.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 41.2 | 113.1 | 107.4 | 152.1 | 170.5 | 199.9 | 224.7 |
| placeOrder | declined | 27 | 70.2 | 100.5 | 98.4 | 122.0 | 124.2 | 191.1 | 191.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.3 | 5.6 | 12.0 | 15.9 | 58.4 |
| orders -> pricing.quote [remote] | 1500 |  | 5.0 | 4.5 | 8.6 | 11.9 | 49.2 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.2 | 2.1 | 3.9 | 5.2 | 47.5 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.4 | 4.1 | 6.8 | 8.7 | 40.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 2.9 | 5.2 | 6.6 | 9.5 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.3 | 5.2 | 6.7 | 9.7 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.2 | 1.1 | 2.2 | 2.8 | 45.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.1 | 3.6 | 4.6 | 48.0 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 1.8 | 2.5 | 8.7 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.8 | 1.5 | 2.0 | 7.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.3 | 2.8 | 3.8 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.1 | 2.7 | 7.3 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.0 | 0.9 | 1.7 | 2.1 | 34.8 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.1 | 7.5 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.9 | 0.8 | 1.7 | 2.4 | 45.6 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.7 | 2.3 | 6.4 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.2 | 2.7 | 2.8 | 2.8 |

By binding: remote 29919 calls, mean 2.05 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/microservices-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

1500 orders in 9.0 s = 167.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 34.1 | 95.3 | 93.6 | 125.9 | 134.8 | 175.0 | 210.0 |
| placeOrder | declined | 27 | 54.0 | 86.7 | 85.0 | 116.4 | 116.9 | 136.8 | 136.8 |

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
| orders -> shipping.quote [remote] | 1500 |  | 4.5 | 3.9 | 9.5 | 13.2 | 47.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.4 | 3.3 | 5.2 | 6.4 | 51.3 |
| orders -> pricing.quote [remote] | 1500 |  | 3.3 | 2.9 | 6.9 | 9.2 | 14.9 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.6 | 4.6 | 41.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.8 | 2.7 | 4.9 | 6.6 | 11.4 |
| orders -> notifications.send [remote] | 1473 |  | 2.4 | 2.2 | 4.4 | 6.3 | 10.0 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.2 | 2.0 | 3.4 | 4.3 | 5.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.4 | 1.3 | 2.3 | 3.3 | 5.5 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.2 | 47.8 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.5 | 1.4 | 2.3 | 2.9 | 9.2 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.0 | 1.9 | 3.0 | 3.6 | 5.8 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.2 | 2.0 | 3.3 | 7.1 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.8 | 1.7 | 2.8 | 3.7 | 8.0 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 0.9 | 1.9 | 2.6 | 7.4 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.0 | 2.6 | 7.4 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 1.8 | 4.9 | 7.9 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 3.0 | 3.2 | 3.2 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8634 calls, mean 0.01 ms, remote 21285 calls, mean 2.10 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/microservices-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async outbox)

1500 orders in 10.6 s = 140.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 40.3 | 113.3 | 109.3 | 152.5 | 164.4 | 197.1 | 224.4 |
| placeOrder | declined | 27 | 67.4 | 103.8 | 103.2 | 133.0 | 141.9 | 172.1 | 172.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.1 | 5.6 | 11.4 | 14.8 | 20.9 |
| orders -> pricing.quote [remote] | 1500 |  | 4.8 | 4.5 | 8.2 | 10.5 | 13.0 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.2 | 2.1 | 3.7 | 4.7 | 11.6 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.4 | 4.2 | 6.5 | 8.0 | 50.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 3.0 | 4.9 | 5.9 | 12.9 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.3 | 5.4 | 6.8 | 11.2 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.2 | 1.1 | 2.0 | 2.6 | 9.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.7 | 4.5 | 54.8 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.0 | 1.0 | 1.7 | 2.2 | 7.7 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.8 | 1.4 | 1.9 | 6.7 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.0 | 4.2 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 1.9 | 2.5 | 9.5 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.2 | 44.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.5 | 2.2 | 6.8 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.9 | 0.8 | 1.6 | 2.2 | 8.1 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.7 | 2.3 | 6.6 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 3.4 | 3.4 | 3.4 |

By binding: remote 29919 calls, mean 2.02 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/microservices-macro-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox)

1500 orders in 8.7 s = 172.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 38.6 | 92.3 | 89.6 | 120.2 | 132.2 | 168.5 | 185.2 |
| placeOrder | declined | 27 | 63.2 | 86.8 | 79.3 | 115.2 | 125.5 | 140.7 | 140.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2344 | 77.7% |
| catalog.productExists | 3018 | 2344 | 77.7% |
| customers.getCustomer | 7446 | 3416 | 45.9% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.2 | 3.6 | 8.8 | 12.4 | 54.5 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.3 | 3.2 | 4.9 | 6.2 | 51.2 |
| orders -> pricing.quote [remote] | 1500 |  | 3.1 | 2.7 | 6.3 | 8.8 | 16.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 1.0 | 3.4 | 4.6 | 49.6 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.7 | 2.6 | 4.7 | 5.7 | 11.5 |
| orders -> notifications.send [remote] | 1473 |  | 2.4 | 2.1 | 4.7 | 6.4 | 9.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.2 | 2.0 | 3.4 | 4.2 | 9.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.4 | 1.3 | 2.3 | 2.9 | 6.7 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.0 | 0.9 | 1.6 | 1.9 | 47.9 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.4 | 1.3 | 2.2 | 2.7 | 8.7 |
| inventory -> catalog.productExists [remote] | 674 |  | 1.8 | 1.7 | 2.9 | 4.3 | 46.9 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 1.8 | 2.4 | 3.7 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.0 | 0.9 | 1.9 | 2.5 | 5.0 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.6 | 1.5 | 2.6 | 4.0 | 8.6 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.0 | 1.8 | 2.2 | 4.4 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.0 | 0.9 | 1.6 | 2.0 | 5.5 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 3.0 | 3.4 | 3.4 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8632 calls, mean 0.01 ms, remote 21287 calls, mean 2.00 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/microservices-macro-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async outbox)

1500 orders in 3.4 s = 436.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.5 | 36.5 | 35.2 | 43.3 | 49.1 | 55.1 | 63.0 |
| placeOrder | declined | 27 | 27.2 | 32.9 | 32.4 | 36.3 | 37.1 | 54.7 | 54.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.2 | 1.8 | 2.3 | 6.5 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.0 | 1.0 | 1.6 | 2.0 | 7.3 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.4 | 8.1 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.2 | 1.7 | 6.7 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 1.6 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.7 | 6.2 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.6 | 2.8 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 7.7 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.6 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.6 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 5.8 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.8 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.6 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.6 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.7 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.5 | 0.9 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.2 | 1.6 | 1.7 | 1.7 |

By binding: loopback-codec 29919 calls, mean 0.44 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/loopback-codec-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox)

1500 orders in 3.2 s = 463.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 11.1 | 34.4 | 32.8 | 42.2 | 47.9 | 53.2 | 57.8 |
| placeOrder | declined | 27 | 26.0 | 31.5 | 30.6 | 35.8 | 36.2 | 52.5 | 52.5 |

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
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.1 | 6.7 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.6 | 2.2 | 13.2 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.7 | 0.7 | 1.1 | 1.3 | 3.2 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.9 | 7.3 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.5 | 0.9 | 1.5 | 1.8 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 1.2 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.7 | 1.0 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.2 | 6.6 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.5 | 5.3 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.4 | 0.4 | 5.9 |
| pricing -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.9 |
| shipping -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.4 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| notifications -> customers.getCustomer [loopback-codec] | 787 |  | 0.2 | 0.1 | 0.4 | 0.6 | 1.0 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.2 | 1.1 | 1.6 | 2.2 | 2.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8991 calls, mean 0.00 ms, loopback-codec 20928 calls, mean 0.48 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/loopback-codec-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async outbox)

1500 orders in 5.1 s = 291.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 21.2 | 54.7 | 52.0 | 69.2 | 75.3 | 84.4 | 99.3 |
| placeOrder | declined | 27 | 41.1 | 50.0 | 48.0 | 57.3 | 59.1 | 69.6 | 69.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.5 | 2.2 | 4.5 | 6.0 | 9.4 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.1 | 1.9 | 3.0 | 3.8 | 6.6 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.8 | 1.6 | 2.9 | 4.4 | 6.1 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.9 | 0.8 | 1.3 | 1.6 | 5.2 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.3 | 1.2 | 2.0 | 2.6 | 6.4 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.3 | 1.2 | 1.8 | 2.2 | 4.4 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.2 | 1.1 | 2.2 | 2.8 | 4.7 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.3 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.5 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 3.7 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.1 | 3.9 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 4.7 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.5 | 0.4 | 0.7 | 0.9 | 3.6 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.3 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.4 | 0.8 | 1.1 | 2.0 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.8 | 2.6 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.6 | 1.6 | 2.1 | 2.6 | 2.6 |

By binding: loopback-http 29919 calls, mean 0.86 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/loopback-http-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox)

1500 orders in 4.5 s = 332.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 21.8 | 48.0 | 45.4 | 61.3 | 65.5 | 78.9 | 88.9 |
| placeOrder | declined | 27 | 35.0 | 44.5 | 43.5 | 50.7 | 56.2 | 71.4 | 71.4 |

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
| orders -> notifications.send [loopback-http] | 1473 |  | 1.8 | 1.1 | 4.8 | 7.0 | 202.9 |
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.7 | 1.5 | 3.5 | 4.8 | 6.4 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.4 | 3.4 | 7.3 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.3 | 1.2 | 2.1 | 2.6 | 7.1 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.1 | 1.8 | 2.3 | 5.1 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.5 | 0.4 | 1.0 | 1.5 | 4.8 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.0 | 0.9 | 2.0 | 3.4 | 6.0 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.2 | 4.7 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 4.2 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 1.0 | 4.6 |
| pricing -> customers.getCustomer [loopback-http] | 964 |  | 0.5 | 0.4 | 0.7 | 1.2 | 4.8 |
| shipping -> customers.getCustomer [loopback-http] | 965 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.7 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.4 | 0.4 | 0.7 | 0.8 | 4.0 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.5 | 0.4 | 0.8 | 1.2 | 1.5 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.8 | 0.9 | 4.1 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 4.2 |
| orders -> inventory.release [loopback-http] | 27 |  | 2.1 | 1.6 | 4.8 | 5.6 | 5.6 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2008 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 536 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9012 calls, mean 0.00 ms, loopback-http 20907 calls, mean 0.93 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/loopback-http-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache off, parallel quotes on, async outbox)

1500 orders in 16.0 s = 93.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 54.6 | 169.9 | 173.0 | 222.4 | 244.6 | 373.2 | 422.8 |
| placeOrder | declined | 27 | 79.9 | 161.5 | 171.4 | 211.3 | 211.8 | 365.5 | 365.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.9 | 4.4 | 8.7 | 11.4 | 198.8 |
| orders -> notifications.send [local] | 1473 |  | 4.2 | 3.8 | 8.9 | 12.6 | 21.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 4.0 | 3.6 | 7.5 | 10.7 | 15.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.9 | 3.5 | 7.5 | 9.9 | 196.7 |
| orders -> payments.capture [remote] | 1473 |  | 3.2 | 2.9 | 6.9 | 9.4 | 21.7 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.1 | 4.3 | 6.1 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 2.6 | 3.5 | 7.4 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.6 | 0.6 | 1.0 | 1.4 | 7.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.9 | 3.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.6 | 1.1 | 1.4 | 4.0 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.6 | 1.0 | 1.2 | 3.6 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.6 | 0.6 | 1.0 | 1.6 | 2.5 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.1 | 0.3 | 0.3 | 0.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 2.5 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.5 | 0.7 |
| orders -> inventory.release [remote] | 27 |  | 5.5 | 5.3 | 8.3 | 8.7 | 8.7 |

By binding: local 13500 calls, mean 0.68 ms, remote 16419 calls, mean 2.08 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/split-dbs-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox)

1500 orders in 5.5 s = 272.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.1 | 58.6 | 55.7 | 76.2 | 83.9 | 104.3 | 134.1 |
| placeOrder | declined | 27 | 39.3 | 55.9 | 53.3 | 68.1 | 74.1 | 80.8 | 80.8 |

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
| orders -> shipping.createShipment [remote] | 1473 |  | 1.8 | 1.7 | 2.9 | 3.5 | 5.1 |
| orders -> shipping.quote [remote] | 1500 |  | 1.7 | 1.6 | 3.3 | 4.5 | 10.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.7 | 1.6 | 2.7 | 3.5 | 4.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.5 | 1.4 | 2.5 | 3.2 | 4.7 |
| orders -> pricing.quote [remote] | 1500 |  | 1.5 | 1.4 | 2.8 | 4.2 | 10.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.6 | 2.0 | 5.2 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.6 | 2.9 | 3.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.6 | 0.9 | 3.4 |
| pricing -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.4 | 2.1 | 9.6 |
| shipping -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.3 | 2.0 | 9.3 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.7 | 1.4 | 1.9 | 9.3 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 0.8 |
| notifications -> customers.getCustomer [remote] | 768 |  | 0.6 | 0.5 | 1.4 | 1.9 | 2.2 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.6 | 0.8 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.5 | 1.3 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.9 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.2 | 3.1 | 3.3 | 3.3 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 705 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9010 calls, mean 0.01 ms, local 8261 calls, mean 0.41 ms, remote 12648 calls, mean 1.34 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T235140Z/split-dbs-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
