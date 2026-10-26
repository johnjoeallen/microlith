# Performance run 20261026T181735Z

- Started: 2026-10-26T18:17:35+00:00
- Commit: 3e82736 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes all --cache both --orders 1500 --warmup 200 --concurrency 16`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith cache off | 432 | 1473 | 27 | 20.8 | 36.9 | 35.6 | 43.9 | 47.5 | 54.3 | 66.9 | 19.9 | PASS |
| modulith cache on | 444 | 1473 | 27 | 17.3 | 35.9 | 33.9 | 43.9 | 48.5 | 82.6 | 90.7 | 19.9 | PASS |
| macro-modulith cache off | 435 | 1473 | 27 | 16.4 | 36.6 | 35.4 | 42.8 | 47.8 | 55.4 | 63.7 | 19.9 | PASS |
| macro-modulith cache on | 446 | 1473 | 27 | 16.1 | 35.7 | 34.0 | 43.6 | 48.8 | 53.3 | 61.2 | 19.9 | PASS |
| groups cache off | 233 | 1473 | 27 | 29.5 | 68.4 | 65.2 | 91.8 | 98.3 | 108.0 | 125.4 | 19.9 | PASS |
| groups cache on | 248 | 1473 | 27 | 31.7 | 64.3 | 62.1 | 84.3 | 90.1 | 100.1 | 119.8 | 19.9 | PASS |
| microservices cache off | 135 | 1473 | 27 | 40.1 | 118.4 | 112.2 | 155.2 | 171.9 | 227.4 | 267.2 | 19.9 | PASS |
| microservices cache on | 160 | 1473 | 27 | 51.4 | 99.5 | 96.1 | 129.7 | 138.7 | 180.9 | 205.2 | 19.9 | PASS |
| microservices-macro cache off | 136 | 1473 | 27 | 44.1 | 116.9 | 111.9 | 155.4 | 172.2 | 205.0 | 238.2 | 19.9 | PASS |
| microservices-macro cache on | 160 | 1473 | 27 | 37.9 | 99.5 | 97.5 | 130.1 | 137.4 | 176.1 | 205.4 | 19.9 | PASS |
| loopback-codec cache off | 419 | 1473 | 27 | 15.8 | 38.0 | 36.9 | 45.4 | 49.6 | 53.9 | 57.7 | 19.9 | PASS |
| loopback-codec cache on | 444 | 1473 | 27 | 16.2 | 35.9 | 34.6 | 42.9 | 45.6 | 50.2 | 59.9 | 19.9 | PASS |
| loopback-http cache off | 281 | 1473 | 27 | 23.8 | 56.8 | 53.9 | 73.4 | 77.7 | 85.1 | 99.6 | 19.9 | PASS |
| loopback-http cache on | 315 | 1473 | 27 | 22.3 | 50.5 | 47.6 | 65.4 | 70.3 | 77.6 | 92.5 | 19.9 | PASS |
| split-dbs cache off | 238 | 1473 | 27 | 30.5 | 67.1 | 64.4 | 88.2 | 95.8 | 106.1 | 117.6 | 19.9 | PASS |
| split-dbs cache on | 255 | 1473 | 27 | 29.7 | 62.6 | 60.2 | 81.9 | 86.3 | 93.5 | 110.4 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith | 432 | 444 | +3% | 35.6 | 33.9 | -5% | 47.5 | 48.5 | +2% | 54.3 | 82.6 | +52% |
| macro-modulith | 435 | 446 | +3% | 35.4 | 34.0 | -4% | 47.8 | 48.8 | +2% | 55.4 | 53.3 | -4% |
| groups | 233 | 248 | +6% | 65.2 | 62.1 | -5% | 98.3 | 90.1 | -8% | 108.0 | 100.1 | -7% |
| microservices | 135 | 160 | +19% | 112.2 | 96.1 | -14% | 171.9 | 138.7 | -19% | 227.4 | 180.9 | -20% |
| microservices-macro | 136 | 160 | +18% | 111.9 | 97.5 | -13% | 172.2 | 137.4 | -20% | 205.0 | 176.1 | -14% |
| loopback-codec | 419 | 444 | +6% | 36.9 | 34.6 | -6% | 49.6 | 45.6 | -8% | 53.9 | 50.2 | -7% |
| loopback-http | 281 | 315 | +12% | 53.9 | 47.6 | -12% | 77.7 | 70.3 | -9% | 85.1 | 77.6 | -9% |
| split-dbs | 238 | 255 | +7% | 64.4 | 60.2 | -7% | 95.8 | 86.3 | -10% | 106.1 | 93.5 | -12% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups cache off | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| groups cache on | 8.4 | 3,039 | 3,039 | 100% | 160 to 160 | 200 to 200 |
| microservices cache off | 20.1 | 5,795 | 5,795 | 100% | 130 to 130 | 159 to 159 |
| microservices cache on | 14.3 | 4,556 | 4,556 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache off | 20.1 | 5,794 | 5,794 | 100% | 130 to 130 | 159 to 159 |
| microservices-macro cache on | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |
| loopback-http cache off | 20.1 | 5,818 | 5,818 | 100% | 130 to 130 | 160 to 160 |
| loopback-http cache on | 14.0 | 4,475 | 4,475 | 100% | 146 to 146 | 173 to 173 |
| split-dbs cache off | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache on | 8.4 | 3,037 | 3,037 | 100% | 160 to 160 | 200 to 200 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.0 | 0.7 | 1.0 | 0.7 | 2.3 | 1.9 | 6.4 | 4.7 | 6.4 | 4.5 | 1.1 | 0.7 | 2.6 | 1.8 | 2.2 | 1.8 |
| orders -> shipping.createShipment | 1.2 | 1.1 | 1.2 | 1.2 | 2.6 | 2.0 | 4.6 | 3.5 | 4.5 | 3.6 | 1.3 | 1.2 | 2.1 | 1.7 | 2.6 | 1.9 |
| orders -> pricing.quote | 0.7 | 0.4 | 0.7 | 0.4 | 2.0 | 1.6 | 5.0 | 3.5 | 5.0 | 3.4 | 0.8 | 0.5 | 1.8 | 1.1 | 2.0 | 1.6 |
| orders -> inventory.reserve | 1.1 | 1.2 | 1.1 | 1.2 | 1.9 | 2.0 | 2.4 | 2.3 | 2.5 | 2.3 | 1.1 | 1.2 | 1.4 | 1.5 | 1.8 | 2.0 |
| orders -> payments.authorize | 0.8 | 0.8 | 0.8 | 0.8 | 1.7 | 1.8 | 3.4 | 3.0 | 3.3 | 3.0 | 0.8 | 0.8 | 1.4 | 1.2 | 1.6 | 1.7 |
| shipping -> inventory.getStock | 0.4 | 0.2 | 0.4 | 0.2 | 0.5 | 0.3 | 2.3 | 1.6 | 2.3 | 1.5 | 0.4 | 0.3 | 0.9 | 0.6 | 0.5 | 0.3 |
| orders -> notifications.send | 0.6 | 0.6 | 0.6 | 0.6 | 1.2 | 0.9 | 2.6 | 2.5 | 2.6 | 2.4 | 0.7 | 0.6 | 1.2 | 1.1 | 1.2 | 0.9 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 1.1 | 1.2 | 1.6 | 1.5 | 1.5 | 1.6 | 0.5 | 0.5 | 0.8 | 0.8 | 1.1 | 1.2 |
| shipping -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.7 | 0.3 | 0.9 | 0.4 | 0.9 | 0.4 | 0.2 | 0.1 | 0.4 | 0.2 | 0.7 | 0.3 |
| inventory -> catalog.productExists | 0.2 | 0.1 | 0.2 | 0.1 | 0.2 | 0.1 | 1.2 | 0.5 | 1.2 | 0.5 | 0.2 | 0.0 | 0.4 | 0.1 | 0.2 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.1 | 0.2 | 0.1 | 0.2 | 0.1 | 1.1 | 0.4 | 1.1 | 0.4 | 0.2 | 0.0 | 0.4 | 0.1 | 0.2 | 0.1 |
| shipping -> inventory.getReservation | 0.2 | 0.3 | 0.2 | 0.3 | 0.4 | 0.4 | 1.2 | 1.1 | 1.1 | 1.2 | 0.3 | 0.3 | 0.5 | 0.5 | 0.4 | 0.4 |
| pricing -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.7 | 0.6 | 1.0 | 0.9 | 1.0 | 0.9 | 0.2 | 0.1 | 0.4 | 0.3 | 0.7 | 0.6 |
| orders -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.7 | 0.6 | 1.0 | 0.8 | 1.0 | 0.8 | 0.2 | 0.2 | 0.5 | 0.3 | 0.7 | 0.6 |
| payments -> customers.getPaymentMethod | 0.2 | 0.1 | 0.1 | 0.1 | 0.2 | 0.2 | 1.3 | 1.0 | 1.2 | 1.0 | 0.2 | 0.1 | 0.4 | 0.3 | 0.2 | 0.2 |
| notifications -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.1 | 0.5 | 0.3 | 0.9 | 0.8 | 0.9 | 0.8 | 0.2 | 0.1 | 0.4 | 0.3 | 0.5 | 0.3 |
| orders -> inventory.release | 1.2 | 1.3 | 1.2 | 1.3 | 2.2 | 2.2 | 2.3 | 2.4 | 2.4 | 2.4 | 1.3 | 1.3 | 1.7 | 1.7 | 2.1 | 2.2 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.7 | 1.4 | 1.7 | 1.4 | 4.0 | 3.6 | 12.0 | 9.6 | 11.7 | 9.5 | 1.9 | 1.4 | 4.5 | 3.5 | 3.8 | 3.5 |
| orders -> shipping.createShipment | 1.7 | 1.6 | 1.7 | 1.6 | 4.1 | 3.1 | 7.0 | 5.4 | 7.1 | 5.6 | 1.8 | 1.5 | 3.0 | 2.5 | 4.1 | 3.0 |
| orders -> pricing.quote | 1.1 | 0.8 | 1.1 | 0.9 | 3.3 | 3.1 | 8.7 | 7.1 | 8.4 | 7.0 | 1.2 | 0.9 | 3.0 | 2.0 | 3.2 | 2.9 |
| orders -> inventory.reserve | 1.6 | 1.8 | 1.7 | 1.8 | 3.1 | 3.1 | 3.9 | 3.7 | 3.9 | 3.8 | 1.7 | 1.7 | 2.1 | 2.2 | 3.0 | 3.1 |
| orders -> payments.authorize | 1.0 | 1.0 | 1.0 | 1.1 | 2.8 | 2.9 | 5.5 | 5.3 | 5.2 | 5.3 | 1.1 | 1.0 | 1.9 | 1.9 | 2.7 | 2.9 |
| shipping -> inventory.getStock | 0.5 | 0.5 | 0.5 | 0.5 | 0.8 | 0.7 | 4.1 | 3.9 | 4.0 | 3.7 | 0.6 | 0.5 | 1.3 | 1.2 | 0.7 | 0.7 |
| orders -> notifications.send | 1.1 | 1.0 | 1.1 | 1.0 | 2.0 | 1.6 | 5.6 | 5.2 | 5.5 | 5.1 | 1.2 | 1.0 | 2.2 | 2.0 | 2.2 | 1.7 |
| orders -> payments.capture | 0.6 | 0.7 | 0.6 | 0.7 | 1.7 | 1.9 | 2.6 | 2.4 | 2.5 | 2.5 | 0.7 | 0.7 | 1.0 | 1.1 | 1.7 | 1.9 |
| shipping -> customers.getCustomer | 0.2 | 0.2 | 0.2 | 0.2 | 1.3 | 1.3 | 1.6 | 1.5 | 1.6 | 1.5 | 0.3 | 0.3 | 0.6 | 0.6 | 1.3 | 1.2 |
| inventory -> catalog.productExists | 0.3 | 0.2 | 0.3 | 0.2 | 0.4 | 0.3 | 2.3 | 2.5 | 2.2 | 2.4 | 0.3 | 0.2 | 0.6 | 0.6 | 0.4 | 0.3 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 1.9 | 2.2 | 1.8 | 2.1 | 0.3 | 0.2 | 0.6 | 0.6 | 0.3 | 0.3 |
| shipping -> inventory.getReservation | 0.3 | 0.4 | 0.3 | 0.4 | 0.5 | 0.6 | 1.8 | 1.8 | 1.9 | 1.9 | 0.4 | 0.4 | 0.7 | 0.8 | 0.5 | 0.5 |
| pricing -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 1.3 | 1.5 | 1.7 | 2.0 | 1.7 | 2.0 | 0.3 | 0.3 | 0.6 | 0.7 | 1.3 | 1.4 |
| orders -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 1.3 | 1.4 | 1.8 | 2.0 | 1.8 | 1.9 | 0.3 | 0.3 | 0.7 | 0.8 | 1.4 | 1.3 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 | 2.2 | 2.4 | 2.1 | 2.4 | 0.3 | 0.3 | 0.6 | 0.7 | 0.4 | 0.5 |
| notifications -> customers.getCustomer | 0.3 | 0.2 | 0.3 | 0.2 | 1.0 | 0.8 | 1.8 | 1.9 | 1.7 | 2.0 | 0.3 | 0.2 | 0.8 | 0.7 | 1.1 | 0.9 |
| orders -> inventory.release | 1.8 | 1.7 | 1.7 | 2.2 | 3.2 | 2.8 | 2.8 | 3.3 | 3.1 | 3.1 | 1.6 | 1.7 | 2.5 | 2.6 | 3.2 | 3.0 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith cache off | modulith cache on | macro-modulith cache off | macro-modulith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | 78.0% | - | 78.0% | - | 78.0% | - | 77.7% | - | 77.7% | - | 78.0% | - | 78.0% | - | 78.0% |
| catalog.productExists | - | 78.0% | - | 78.0% | - | 78.0% | - | 77.7% | - | 77.7% | - | 78.0% | - | 78.0% | - | 78.0% |
| customers.getCustomer | - | 50.4% | - | 50.4% | - | 50.7% | - | 45.9% | - | 45.9% | - | 50.4% | - | 50.6% | - | 50.8% |
| customers.getPaymentMethod | - | 35.4% | - | 35.4% | - | 35.4% | - | 35.3% | - | 35.3% | - | 35.4% | - | 35.4% | - | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith cache off | - | 29919 calls, 0.43 ms | - | - | - |
| modulith cache on | 8994 calls, 0.01 ms | 20925 calls, 0.49 ms | - | - | - |
| macro-modulith cache off | - | 29919 calls, 0.42 ms | - | - | - |
| macro-modulith cache on | 8991 calls, 0.01 ms | 20928 calls, 0.49 ms | - | - | - |
| groups cache off | - | 13500 calls, 0.40 ms | - | - | 16419 calls, 1.36 ms |
| groups cache on | 9011 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12647 calls, 1.48 ms |
| microservices cache off | - | - | - | - | 29919 calls, 2.13 ms |
| microservices cache on | 8635 calls, 0.01 ms | - | - | - | 21284 calls, 2.19 ms |
| microservices-macro cache off | - | - | - | - | 29919 calls, 2.09 ms |
| microservices-macro cache on | 8638 calls, 0.01 ms | - | - | - | 21281 calls, 2.17 ms |
| loopback-codec cache off | - | - | 29919 calls, 0.46 ms | - | - |
| loopback-codec cache on | 8994 calls, 0.00 ms | - | 20925 calls, 0.51 ms | - | - |
| loopback-http cache off | - | - | - | 29919 calls, 0.89 ms | - |
| loopback-http cache on | 9008 calls, 0.00 ms | - | - | 20911 calls, 0.91 ms | - |
| split-dbs cache off | - | 13500 calls, 0.39 ms | - | - | 16419 calls, 1.35 ms |
| split-dbs cache on | 9021 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12637 calls, 1.45 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache off, parallel quotes on, async outbox)

1500 orders in 3.5 s = 432.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.8 | 36.9 | 35.6 | 43.9 | 47.5 | 54.3 | 66.9 |
| placeOrder | declined | 27 | 28.2 | 33.6 | 32.7 | 38.0 | 39.3 | 42.8 | 42.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.2 | 1.7 | 2.0 | 7.4 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.6 | 2.2 | 6.1 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.2 | 8.7 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.3 | 2.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.7 | 8.1 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.1 | 1.5 | 8.3 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.6 | 2.4 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 1.3 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.0 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 6.5 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.2 | 0.3 | 7.9 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.6 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.1 | 0.2 | 0.3 | 0.6 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.4 | 1.1 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.1 | 1.8 | 1.8 | 1.8 |

By binding: local 29919 calls, mean 0.43 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/modulith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox)

1500 orders in 3.4 s = 444.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.3 | 35.9 | 33.9 | 43.9 | 48.5 | 82.6 | 90.7 |
| placeOrder | declined | 27 | 27.8 | 32.4 | 31.9 | 35.3 | 37.4 | 53.1 | 53.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3755 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.4 | 55.5 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 1.9 | 3.5 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.0 | 1.2 | 2.2 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.0 | 9.1 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.6 | 2.5 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 1.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 8.0 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.8 | 1.2 | 3.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 2.5 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| pricing -> customers.getCustomer [local] | 966 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.6 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| notifications -> customers.getCustomer [local] | 788 |  | 0.2 | 0.1 | 0.4 | 0.5 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.3 | 1.7 | 1.7 | 1.7 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 685 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 534 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.4 |

By binding: cache 8994 calls, mean 0.01 ms, local 20925 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/modulith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache off, parallel quotes on, async outbox)

1500 orders in 3.5 s = 434.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.4 | 36.6 | 35.4 | 42.8 | 47.8 | 55.4 | 63.7 |
| placeOrder | declined | 27 | 29.9 | 33.2 | 32.1 | 36.5 | 37.0 | 44.9 | 44.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.2 | 1.7 | 2.2 | 8.9 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.1 | 8.2 |
| orders -> shipping.quote [local] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.3 | 6.2 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.2 | 6.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.5 | 0.6 | 4.9 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.6 | 1.1 | 1.5 | 5.6 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.6 | 1.1 | 1.5 | 6.4 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.7 | 4.9 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 4.6 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.2 | 0.2 | 0.2 | 0.3 | 4.2 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.1 | 0.2 | 0.3 | 1.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.2 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.7 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.1 | 0.1 | 0.2 | 0.3 | 0.4 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.1 | 0.1 | 0.3 | 0.4 | 0.6 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.7 | 1.8 | 1.8 |

By binding: local 29919 calls, mean 0.42 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/macro-modulith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-modulith (cache on, parallel quotes on, async outbox)

1500 orders in 3.4 s = 445.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.1 | 35.7 | 34.0 | 43.6 | 48.8 | 53.3 | 61.2 |
| placeOrder | declined | 27 | 27.2 | 33.2 | 31.3 | 40.6 | 42.7 | 59.2 | 59.2 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 8.3 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 1.9 | 12.1 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 6.3 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 1.8 | 3.8 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 2.0 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 7.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 1.2 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.9 | 1.2 | 3.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.3 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.1 |
| shipping -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.2 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| notifications -> customers.getCustomer [local] | 787 |  | 0.2 | 0.1 | 0.3 | 0.5 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.3 | 1.2 | 2.2 | 2.6 | 2.6 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.6 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 686 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8991 calls, mean 0.01 ms, local 20928 calls, mean 0.49 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/macro-modulith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async outbox)

1500 orders in 6.4 s = 233.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.5 | 68.4 | 65.2 | 91.8 | 98.3 | 108.0 | 125.4 |
| placeOrder | declined | 27 | 45.1 | 59.4 | 58.3 | 74.1 | 77.2 | 81.6 | 81.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.4 | 4.1 | 5.2 | 11.0 |
| orders -> shipping.quote [remote] | 1500 |  | 2.3 | 2.1 | 4.0 | 5.0 | 9.6 |
| orders -> pricing.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.3 | 4.3 | 6.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.1 | 4.1 | 6.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.8 | 3.6 | 9.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.7 | 1.3 | 1.7 | 5.7 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.1 | 2.0 | 3.1 | 4.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.4 | 7.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.4 | 0.8 | 1.0 | 4.8 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.7 | 1.3 | 1.9 | 4.7 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.3 | 1.8 | 4.1 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.0 | 1.9 | 2.4 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 4.2 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.5 | 2.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 2.1 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.6 | 2.5 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.2 | 3.2 | 3.2 | 3.2 |

By binding: local 13500 calls, mean 0.40 ms, remote 16419 calls, mean 1.36 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/groups-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

1500 orders in 6.0 s = 248.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 31.7 | 64.3 | 62.1 | 84.3 | 90.1 | 100.1 | 119.8 |
| placeOrder | declined | 27 | 40.5 | 59.6 | 57.0 | 77.5 | 81.7 | 84.2 | 84.2 |

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
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.8 | 3.1 | 4.0 | 6.6 |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.8 | 3.1 | 4.0 | 8.2 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.6 | 4.9 | 10.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 2.9 | 3.7 | 5.9 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.1 | 4.2 | 10.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.7 | 9.1 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.6 | 3.1 | 3.7 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.0 | 3.5 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.3 | 9.1 |
| shipping -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.2 | 9.2 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.6 | 2.4 | 4.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.7 | 2.3 |
| notifications -> customers.getCustomer [remote] | 767 |  | 0.6 | 0.5 | 1.4 | 1.9 | 2.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 2.1 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 2.8 | 3.9 | 3.9 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.3 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 706 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 9011 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12647 calls, mean 1.48 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/groups-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async outbox)

1500 orders in 11.1 s = 134.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 40.1 | 118.4 | 112.2 | 155.2 | 171.9 | 227.4 | 267.2 |
| placeOrder | declined | 27 | 68.4 | 110.7 | 110.1 | 141.2 | 154.1 | 203.7 | 203.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.4 | 5.8 | 12.0 | 16.2 | 46.5 |
| orders -> pricing.quote [remote] | 1500 |  | 5.0 | 4.7 | 8.7 | 11.2 | 53.3 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.3 | 2.1 | 4.1 | 5.4 | 9.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.6 | 4.2 | 7.0 | 9.0 | 57.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.4 | 3.2 | 5.5 | 6.7 | 12.0 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.2 | 5.6 | 7.1 | 11.6 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.2 | 1.1 | 2.3 | 3.2 | 7.3 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.3 | 3.9 | 4.9 | 7.0 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 1.9 | 2.8 | 7.3 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.2 | 7.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.4 | 48.9 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.3 | 1.1 | 2.2 | 3.0 | 8.9 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 1.8 | 2.9 | 51.9 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.8 | 3.0 | 47.6 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.1 | 7.7 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.8 | 2.4 | 6.2 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 2.8 | 3.3 | 3.3 |

By binding: remote 29919 calls, mean 2.13 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/microservices-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

1500 orders in 9.3 s = 160.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 51.4 | 99.5 | 96.1 | 129.7 | 138.7 | 180.9 | 205.2 |
| placeOrder | declined | 27 | 56.6 | 91.2 | 83.4 | 119.7 | 121.2 | 139.1 | 139.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2344 | 77.7% |
| catalog.productExists | 3018 | 2344 | 77.7% |
| customers.getCustomer | 7446 | 3418 | 45.9% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.7 | 4.1 | 9.6 | 13.2 | 54.2 |
| orders -> pricing.quote [remote] | 1500 |  | 3.5 | 3.1 | 7.1 | 10.0 | 16.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.4 | 6.6 | 13.7 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.6 | 1.1 | 3.9 | 5.2 | 46.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.3 | 6.9 | 12.7 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.2 | 5.2 | 6.7 | 8.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.7 | 4.6 | 6.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.3 | 6.9 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.6 | 7.9 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.6 | 3.3 | 10.0 |
| inventory -> catalog.productExists [remote] | 674 |  | 2.0 | 1.9 | 3.5 | 4.4 | 8.5 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.4 | 1.3 | 2.2 | 3.4 | 7.6 |
| notifications -> customers.getCustomer [remote] | 1115 |  | 1.1 | 1.0 | 2.1 | 2.9 | 3.6 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.8 | 1.7 | 3.1 | 4.0 | 7.7 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.2 | 2.8 | 6.6 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.0 | 2.0 | 2.7 | 7.0 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.5 | 3.3 | 3.3 | 3.3 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 358 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8635 calls, mean 0.01 ms, remote 21284 calls, mean 2.19 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/microservices-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async outbox)

1500 orders in 11.0 s = 136.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 44.1 | 116.9 | 111.9 | 155.4 | 172.2 | 205.0 | 238.2 |
| placeOrder | declined | 27 | 70.5 | 112.2 | 103.4 | 146.2 | 161.8 | 229.6 | 229.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.4 | 5.7 | 11.7 | 16.3 | 60.4 |
| orders -> pricing.quote [remote] | 1500 |  | 5.0 | 4.6 | 8.4 | 11.4 | 54.8 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.3 | 2.1 | 4.0 | 5.1 | 52.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.2 | 7.1 | 8.9 | 50.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.3 | 3.0 | 5.2 | 6.8 | 14.6 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.1 | 5.5 | 6.8 | 10.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 3.9 | 5.1 | 53.1 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.2 | 1.1 | 2.2 | 2.9 | 7.7 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 1.8 | 2.5 | 8.7 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.2 | 8.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.1 | 4.2 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.1 | 2.9 | 12.1 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.5 | 45.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.4 | 6.7 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.8 | 1.8 | 2.5 | 52.4 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.7 | 1.7 | 2.3 | 7.3 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.3 | 3.1 | 3.3 | 3.3 |

By binding: remote 29919 calls, mean 2.09 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/microservices-macro-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox)

1500 orders in 9.4 s = 160.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 37.9 | 99.5 | 97.5 | 130.1 | 137.4 | 176.1 | 205.4 |
| placeOrder | declined | 27 | 61.2 | 91.3 | 89.9 | 115.8 | 117.8 | 157.9 | 157.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3419 | 45.9% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.5 | 4.0 | 9.5 | 13.1 | 19.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.6 | 3.4 | 5.6 | 7.0 | 53.7 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.1 | 7.0 | 9.3 | 15.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.9 | 5.3 | 7.0 | 12.3 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.7 | 5.2 | 11.7 |
| orders -> notifications.send [remote] | 1473 |  | 2.4 | 2.1 | 5.1 | 6.9 | 13.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.8 | 4.6 | 5.7 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.4 | 2.5 | 3.1 | 10.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 1.9 | 2.6 | 48.9 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.6 | 3.5 | 7.6 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.0 | 1.9 | 3.4 | 4.6 | 9.6 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.1 | 2.7 | 10.1 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.8 | 1.7 | 2.9 | 3.8 | 9.2 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 0.9 | 2.1 | 2.8 | 5.8 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.1 | 2.6 | 3.1 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 1.9 | 2.6 | 9.7 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.4 | 3.1 | 3.7 | 3.7 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8638 calls, mean 0.01 ms, remote 21281 calls, mean 2.17 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/microservices-macro-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async outbox)

1500 orders in 3.6 s = 419.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.8 | 38.0 | 36.9 | 45.4 | 49.6 | 53.9 | 57.7 |
| placeOrder | declined | 27 | 27.5 | 34.9 | 34.8 | 38.5 | 38.7 | 50.9 | 50.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.3 | 1.3 | 1.8 | 2.1 | 3.5 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.2 | 5.1 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.1 | 1.0 | 1.9 | 2.5 | 7.1 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 2.7 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.8 | 0.7 | 1.2 | 1.6 | 6.8 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.7 | 1.9 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.7 | 0.6 | 1.2 | 1.7 | 4.1 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.7 | 6.1 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.9 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.0 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.6 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.0 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.4 | 5.9 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.2 | 0.1 | 0.3 | 0.5 | 0.8 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.3 | 1.2 | 1.6 | 2.2 | 2.2 |

By binding: loopback-codec 29919 calls, mean 0.46 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/loopback-codec-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox)

1500 orders in 3.4 s = 443.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.2 | 35.9 | 34.6 | 42.9 | 45.6 | 50.2 | 59.9 |
| placeOrder | declined | 27 | 24.8 | 32.8 | 32.3 | 37.4 | 43.0 | 44.5 | 44.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3755 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.2 | 1.1 | 1.7 | 2.3 | 9.1 |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.2 | 1.1 | 1.5 | 1.9 | 8.8 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 0.8 | 0.8 | 1.0 | 1.3 | 11.8 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 0.7 | 0.7 | 1.4 | 1.9 | 9.2 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.6 | 0.6 | 1.0 | 1.5 | 2.0 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.7 | 1.9 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.6 | 5.9 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.3 | 8.6 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.4 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 8.0 |
| pricing -> customers.getCustomer [loopback-codec] | 968 |  | 0.2 | 0.2 | 0.3 | 0.5 | 8.0 |
| shipping -> customers.getCustomer [loopback-codec] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 7.9 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 2.3 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| notifications -> customers.getCustomer [loopback-codec] | 786 |  | 0.2 | 0.2 | 0.4 | 0.5 | 0.9 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.3 | 1.2 | 1.7 | 1.8 | 1.8 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 687 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8994 calls, mean 0.00 ms, loopback-codec 20925 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/loopback-codec-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async outbox)

1500 orders in 5.3 s = 280.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 23.8 | 56.8 | 53.9 | 73.4 | 77.7 | 85.1 | 99.6 |
| placeOrder | declined | 27 | 42.0 | 51.8 | 49.9 | 63.1 | 65.4 | 76.0 | 76.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 2.6 | 2.3 | 4.5 | 6.2 | 9.7 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.1 | 2.0 | 3.0 | 4.2 | 9.1 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.8 | 1.7 | 3.0 | 4.1 | 6.5 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.9 | 0.8 | 1.3 | 1.8 | 5.6 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.4 | 1.3 | 2.1 | 3.0 | 6.9 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.4 | 1.3 | 1.9 | 2.6 | 5.9 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.2 | 1.1 | 2.2 | 2.9 | 3.6 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.9 | 4.9 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.4 | 0.4 | 0.6 | 0.8 | 5.8 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.4 | 0.4 | 0.6 | 0.8 | 4.5 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.0 | 1.3 | 4.0 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.9 | 6.3 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.5 | 0.4 | 0.7 | 0.9 | 3.0 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.9 | 2.9 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.4 | 0.4 | 0.6 | 0.9 | 4.0 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.4 | 0.4 | 0.8 | 1.1 | 1.4 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.6 | 2.5 | 2.5 | 2.5 |

By binding: loopback-http 29919 calls, mean 0.89 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/loopback-http-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox)

1500 orders in 4.8 s = 315.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 22.3 | 50.5 | 47.6 | 65.4 | 70.3 | 77.6 | 92.5 |
| placeOrder | declined | 27 | 37.7 | 47.3 | 46.0 | 54.3 | 66.0 | 78.8 | 78.8 |

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
| orders -> shipping.quote [loopback-http] | 1500 |  | 1.8 | 1.6 | 3.5 | 5.2 | 7.9 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.5 | 3.1 | 7.0 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 1.5 | 1.4 | 2.2 | 2.8 | 7.5 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.2 | 1.2 | 1.9 | 2.3 | 7.7 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 0.6 | 0.4 | 1.2 | 1.5 | 6.0 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.1 | 1.0 | 2.0 | 2.8 | 3.8 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.1 | 0.9 | 2.0 | 3.3 | 6.2 |
| orders -> payments.capture [loopback-http] | 1473 |  | 0.8 | 0.8 | 1.1 | 1.3 | 3.6 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.5 | 0.5 | 0.8 | 1.0 | 4.9 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.5 | 0.5 | 0.8 | 1.5 | 3.9 |
| pricing -> customers.getCustomer [loopback-http] | 968 |  | 0.5 | 0.4 | 0.7 | 1.0 | 3.8 |
| shipping -> customers.getCustomer [loopback-http] | 965 |  | 0.5 | 0.4 | 0.7 | 0.9 | 1.6 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.5 | 0.4 | 0.7 | 0.9 | 6.0 |
| notifications -> customers.getCustomer [loopback-http] | 775 |  | 0.5 | 0.4 | 0.9 | 1.3 | 2.4 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 0.9 | 4.3 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.5 |
| orders -> inventory.release [loopback-http] | 27 |  | 1.7 | 1.5 | 2.6 | 2.9 | 2.9 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2008 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 698 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9008 calls, mean 0.00 ms, loopback-http 20911 calls, mean 0.91 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/loopback-http-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache off, parallel quotes on, async outbox)

1500 orders in 6.3 s = 237.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.5 | 67.1 | 64.4 | 88.2 | 95.8 | 106.1 | 117.6 |
| placeOrder | declined | 27 | 45.4 | 60.5 | 54.2 | 76.8 | 82.6 | 97.2 | 97.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.4 | 4.1 | 5.2 | 9.5 |
| orders -> shipping.quote [remote] | 1500 |  | 2.2 | 2.1 | 3.8 | 5.0 | 8.7 |
| orders -> pricing.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.2 | 4.3 | 8.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 3.0 | 3.8 | 9.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.7 | 3.6 | 9.6 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.7 | 1.3 | 1.7 | 6.9 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.0 | 2.2 | 3.2 | 8.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.4 | 6.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.4 | 0.7 | 1.0 | 4.0 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.7 | 1.3 | 1.7 | 7.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.4 | 1.9 | 7.6 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.1 | 1.9 | 7.4 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 3.7 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.6 | 2.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 2.0 | 3.2 | 3.2 | 3.2 |

By binding: local 13500 calls, mean 0.39 ms, remote 16419 calls, mean 1.35 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/split-dbs-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox)

1500 orders in 5.9 s = 254.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.7 | 62.6 | 60.2 | 81.9 | 86.3 | 93.5 | 110.4 |
| placeOrder | declined | 27 | 40.5 | 57.8 | 55.7 | 72.0 | 74.0 | 84.2 | 84.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3782 | 50.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.8 | 3.1 | 4.2 | 5.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.0 | 3.7 | 5.3 |
| orders -> shipping.quote [remote] | 1500 |  | 1.8 | 1.7 | 3.5 | 4.3 | 7.7 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.9 | 3.8 | 6.4 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 2.9 | 3.7 | 7.5 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.7 | 8.5 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.7 | 3.1 | 6.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 0.9 | 1.9 |
| shipping -> customers.getCustomer [remote] | 968 |  | 0.9 | 0.9 | 1.5 | 2.3 | 3.3 |
| pricing -> customers.getCustomer [remote] | 967 |  | 0.9 | 0.9 | 1.5 | 2.1 | 3.4 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.4 | 2.1 | 5.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 2.4 |
| notifications -> customers.getCustomer [remote] | 760 |  | 0.6 | 0.5 | 1.4 | 2.0 | 3.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.9 | 2.3 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.2 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.3 | 3.0 | 3.0 | 3.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 713 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 533 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9021 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12637 calls, mean 1.45 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T181735Z/split-dbs-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
