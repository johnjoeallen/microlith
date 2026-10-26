# Performance run 20261025T211735Z

- Started: 2026-10-25T21:17:35+00:00
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
| modulith cache off | 306 | 1473 | 27 | 12.6 | 52.1 | 50.9 | 55.4 | 56.9 | 160.9 | 173.0 | 19.9 | PASS |
| modulith cache on | 330 | 1473 | 27 | 16.7 | 48.3 | 46.9 | 50.9 | 52.3 | 161.1 | 167.2 | 19.9 | PASS |
| macro-monolith cache off | 305 | 1473 | 27 | 17.3 | 52.2 | 51.2 | 55.3 | 56.5 | 153.7 | 161.1 | 19.9 | PASS |
| macro-monolith cache on | 339 | 1473 | 27 | 15.7 | 47.0 | 46.8 | 50.8 | 52.2 | 55.3 | 58.8 | 19.9 | PASS |
| groups cache off | 200 | 1473 | 27 | 20.8 | 79.5 | 78.8 | 87.3 | 89.6 | 93.9 | 97.4 | 19.9 | PASS |
| groups cache on | 227 | 1473 | 27 | 17.9 | 70.0 | 69.2 | 77.4 | 80.0 | 83.1 | 92.5 | 19.9 | PASS |
| microservices cache off | 140 | 1473 | 27 | 29.2 | 113.9 | 111.8 | 126.1 | 133.3 | 190.4 | 220.6 | 19.9 | PASS |
| microservices cache on | 165 | 1473 | 27 | 25.8 | 96.9 | 92.0 | 107.6 | 118.9 | 209.4 | 334.5 | 19.9 | PASS |
| microservices-macro cache off | 140 | 1473 | 27 | 29.2 | 113.6 | 111.1 | 124.7 | 132.9 | 186.4 | 269.2 | 19.9 | PASS |
| microservices-macro cache on | 169 | 1473 | 27 | 28.3 | 94.4 | 91.4 | 105.2 | 111.2 | 179.3 | 235.2 | 19.9 | PASS |
| loopback-codec cache off | 287 | 1473 | 27 | 16.2 | 55.6 | 54.7 | 59.5 | 60.9 | 128.8 | 135.2 | 19.9 | PASS |
| loopback-codec cache on | 314 | 1473 | 27 | 16.1 | 50.8 | 49.4 | 53.9 | 55.7 | 163.5 | 171.9 | 19.9 | PASS |
| loopback-http cache off | 181 | 1473 | 27 | 24.1 | 87.9 | 88.5 | 96.7 | 99.0 | 103.3 | 110.8 | 19.9 | PASS |
| loopback-http cache on | 209 | 1473 | 27 | 20.0 | 76.1 | 75.8 | 83.6 | 86.1 | 91.9 | 95.6 | 19.9 | PASS |
| split-dbs cache off | 206 | 1473 | 27 | 24.2 | 77.3 | 76.7 | 84.8 | 88.1 | 93.7 | 100.8 | 19.9 | PASS |
| split-dbs cache on | 235 | 1473 | 27 | 22.5 | 67.8 | 67.4 | 74.6 | 77.7 | 84.2 | 92.6 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith | 306 | 330 | +8% | 50.9 | 46.9 | -8% | 56.9 | 52.3 | -8% | 160.9 | 161.1 | +0% |
| macro-monolith | 305 | 339 | +11% | 51.2 | 46.8 | -8% | 56.5 | 52.2 | -8% | 153.7 | 55.3 | -64% |
| groups | 200 | 227 | +14% | 78.8 | 69.2 | -12% | 89.6 | 80.0 | -11% | 93.9 | 83.1 | -12% |
| microservices | 140 | 165 | +18% | 111.8 | 92.0 | -18% | 133.3 | 118.9 | -11% | 190.4 | 209.4 | +10% |
| microservices-macro | 140 | 169 | +20% | 111.1 | 91.4 | -18% | 132.9 | 111.2 | -16% | 186.4 | 179.3 | -4% |
| loopback-codec | 287 | 314 | +9% | 54.7 | 49.4 | -10% | 60.9 | 55.7 | -9% | 128.8 | 163.5 | +27% |
| loopback-http | 181 | 209 | +15% | 88.5 | 75.8 | -14% | 99.0 | 86.1 | -13% | 103.3 | 91.9 | -11% |
| split-dbs | 206 | 235 | +14% | 76.7 | 67.4 | -12% | 88.1 | 77.7 | -12% | 93.7 | 84.2 | -10% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups cache off | 11.0 | 3,819 | 3,819 | 100% | 145 to 145 | 201 to 201 |
| groups cache on | 8.4 | 3,039 | 3,039 | 100% | 160 to 160 | 200 to 200 |
| microservices cache off | 20.1 | 5,804 | 5,804 | 100% | 130 to 130 | 159 to 159 |
| microservices cache on | 14.3 | 4,557 | 4,557 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache off | 20.1 | 5,804 | 5,804 | 100% | 130 to 130 | 159 to 159 |
| microservices-macro cache on | 14.3 | 4,557 | 4,557 | 100% | 145 to 145 | 173 to 173 |
| loopback-http cache off | 20.1 | 5,825 | 5,825 | 100% | 130 to 130 | 160 to 160 |
| loopback-http cache on | 14.0 | 4,480 | 4,480 | 100% | 146 to 146 | 173 to 173 |
| split-dbs cache off | 11.0 | 3,820 | 3,820 | 100% | 145 to 145 | 201 to 201 |
| split-dbs cache on | 8.4 | 3,040 | 3,040 | 100% | 160 to 160 | 200 to 200 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-monolith cache off | macro-monolith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.7 | 1.2 | 1.7 | 1.1 | 3.0 | 2.3 | 6.2 | 4.4 | 6.3 | 4.3 | 2.0 | 1.3 | 4.6 | 3.3 | 3.0 | 2.2 |
| orders -> shipping.createShipment | 1.9 | 1.6 | 1.9 | 1.6 | 3.2 | 2.3 | 4.5 | 3.5 | 4.5 | 3.3 | 2.1 | 1.7 | 3.5 | 2.8 | 3.2 | 2.2 |
| orders -> pricing.quote | 1.3 | 0.8 | 1.3 | 0.7 | 2.6 | 1.9 | 4.5 | 2.9 | 4.5 | 2.9 | 1.5 | 0.9 | 3.3 | 1.9 | 2.6 | 1.8 |
| shipping -> inventory.getStock | 0.7 | 0.4 | 0.6 | 0.4 | 0.7 | 0.4 | 2.2 | 1.4 | 2.2 | 1.4 | 0.7 | 0.4 | 1.6 | 1.0 | 0.6 | 0.4 |
| orders -> inventory.reserve | 1.5 | 1.4 | 1.4 | 1.4 | 2.1 | 2.1 | 2.3 | 2.3 | 2.3 | 2.3 | 1.5 | 1.6 | 2.0 | 2.0 | 2.0 | 2.0 |
| orders -> payments.authorize | 1.1 | 1.1 | 1.1 | 1.0 | 1.8 | 1.7 | 3.0 | 2.7 | 3.0 | 2.6 | 1.2 | 1.1 | 2.1 | 1.9 | 1.8 | 1.7 |
| orders -> notifications.send | 0.9 | 0.8 | 0.9 | 0.8 | 1.7 | 1.3 | 2.8 | 2.5 | 2.8 | 2.5 | 1.0 | 0.9 | 2.0 | 1.7 | 1.7 | 1.3 |
| orders -> payments.capture | 0.6 | 0.6 | 0.6 | 0.6 | 1.3 | 1.3 | 1.5 | 1.5 | 1.4 | 1.4 | 0.7 | 0.7 | 1.1 | 1.2 | 1.2 | 1.2 |
| shipping -> customers.getCustomer | 0.3 | 0.1 | 0.3 | 0.1 | 0.9 | 0.3 | 1.0 | 0.3 | 1.0 | 0.3 | 0.3 | 0.1 | 0.8 | 0.3 | 0.9 | 0.3 |
| inventory -> catalog.productExists | 0.3 | 0.1 | 0.3 | 0.1 | 0.3 | 0.1 | 1.1 | 0.4 | 1.1 | 0.3 | 0.3 | 0.1 | 0.8 | 0.2 | 0.3 | 0.1 |
| pricing -> catalog.getProduct | 0.3 | 0.1 | 0.3 | 0.1 | 0.3 | 0.1 | 1.0 | 0.3 | 1.0 | 0.3 | 0.3 | 0.1 | 0.8 | 0.2 | 0.3 | 0.1 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 1.2 | 1.2 | 1.1 | 1.1 | 0.4 | 0.4 | 0.9 | 0.9 | 0.4 | 0.4 |
| orders -> customers.getCustomer | 0.3 | 0.2 | 0.3 | 0.2 | 1.0 | 0.7 | 1.1 | 0.7 | 1.1 | 0.7 | 0.4 | 0.2 | 0.9 | 0.6 | 1.0 | 0.6 |
| pricing -> customers.getCustomer | 0.4 | 0.2 | 0.3 | 0.2 | 0.9 | 0.6 | 1.0 | 0.7 | 1.0 | 0.8 | 0.3 | 0.3 | 0.8 | 0.5 | 0.9 | 0.6 |
| notifications -> customers.getCustomer | 0.2 | 0.1 | 0.2 | 0.1 | 0.9 | 0.5 | 1.1 | 0.8 | 1.1 | 0.8 | 0.3 | 0.2 | 0.8 | 0.4 | 0.9 | 0.5 |
| payments -> customers.getPaymentMethod | 0.3 | 0.2 | 0.3 | 0.2 | 0.3 | 0.2 | 1.1 | 0.8 | 1.1 | 0.8 | 0.3 | 0.3 | 0.8 | 0.5 | 0.3 | 0.2 |
| orders -> inventory.release | 1.4 | 1.5 | 1.5 | 1.4 | 3.1 | 2.9 | 2.9 | 2.8 | 2.9 | 3.0 | 1.6 | 1.5 | 2.5 | 2.2 | 2.9 | 3.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith cache off | modulith cache on | macro-monolith cache off | macro-monolith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 2.7 | 2.0 | 2.7 | 2.0 | 4.3 | 3.4 | 10.0 | 7.5 | 10.1 | 7.4 | 3.2 | 2.3 | 7.9 | 5.7 | 4.2 | 3.4 |
| orders -> shipping.createShipment | 2.3 | 2.1 | 2.3 | 2.1 | 3.8 | 2.8 | 5.5 | 4.3 | 5.4 | 4.2 | 2.5 | 2.2 | 4.0 | 3.3 | 3.7 | 2.7 |
| orders -> pricing.quote | 1.8 | 1.2 | 1.8 | 1.2 | 3.3 | 2.7 | 6.7 | 4.7 | 6.4 | 4.8 | 2.1 | 1.3 | 5.0 | 3.1 | 3.2 | 2.6 |
| shipping -> inventory.getStock | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.7 | 2.8 | 2.6 | 2.7 | 2.6 | 0.8 | 0.7 | 1.8 | 1.7 | 0.8 | 0.7 |
| orders -> inventory.reserve | 2.1 | 2.1 | 2.1 | 2.1 | 2.8 | 2.8 | 3.2 | 3.1 | 3.2 | 3.1 | 2.2 | 2.2 | 2.7 | 2.7 | 2.7 | 2.7 |
| orders -> payments.authorize | 1.3 | 1.2 | 1.3 | 1.2 | 2.1 | 2.2 | 3.8 | 3.7 | 3.8 | 3.7 | 1.4 | 1.4 | 2.4 | 2.4 | 2.1 | 2.1 |
| orders -> notifications.send | 1.5 | 1.3 | 1.5 | 1.3 | 2.9 | 2.3 | 4.8 | 4.3 | 4.9 | 4.3 | 1.7 | 1.4 | 3.2 | 2.6 | 2.8 | 2.4 |
| orders -> payments.capture | 0.8 | 0.8 | 0.8 | 0.8 | 1.5 | 1.5 | 1.9 | 1.7 | 1.9 | 1.8 | 0.8 | 0.8 | 1.3 | 1.3 | 1.5 | 1.5 |
| shipping -> customers.getCustomer | 0.4 | 0.3 | 0.4 | 0.3 | 1.1 | 1.1 | 1.3 | 1.2 | 1.2 | 1.2 | 0.4 | 0.4 | 0.9 | 0.9 | 1.1 | 1.0 |
| inventory -> catalog.productExists | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 1.5 | 1.5 | 1.5 | 1.5 | 0.4 | 0.3 | 0.9 | 0.8 | 0.4 | 0.4 |
| pricing -> catalog.getProduct | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.3 | 1.4 | 1.4 | 1.4 | 1.4 | 0.4 | 0.4 | 0.9 | 0.9 | 0.4 | 0.3 |
| shipping -> inventory.getReservation | 0.5 | 0.5 | 0.5 | 0.5 | 0.6 | 0.5 | 1.4 | 1.4 | 1.4 | 1.4 | 0.5 | 0.6 | 1.0 | 1.1 | 0.5 | 0.5 |
| orders -> customers.getCustomer | 0.4 | 0.4 | 0.4 | 0.4 | 1.2 | 1.2 | 1.4 | 1.4 | 1.4 | 1.4 | 0.5 | 0.4 | 1.0 | 1.0 | 1.1 | 1.1 |
| pricing -> customers.getCustomer | 0.4 | 0.4 | 0.4 | 0.4 | 1.1 | 1.1 | 1.3 | 1.3 | 1.3 | 1.4 | 0.4 | 0.4 | 0.9 | 0.9 | 1.1 | 1.1 |
| notifications -> customers.getCustomer | 0.4 | 0.3 | 0.4 | 0.3 | 1.6 | 1.4 | 1.7 | 1.8 | 1.8 | 1.7 | 0.5 | 0.4 | 1.2 | 1.0 | 1.7 | 1.5 |
| payments -> customers.getPaymentMethod | 0.4 | 0.3 | 0.4 | 0.3 | 0.4 | 0.4 | 1.5 | 1.6 | 1.5 | 1.6 | 0.4 | 0.4 | 0.9 | 0.9 | 0.4 | 0.4 |
| orders -> inventory.release | 2.2 | 2.1 | 1.9 | 2.0 | 4.0 | 4.0 | 4.0 | 3.7 | 3.5 | 4.4 | 2.2 | 2.4 | 3.7 | 2.7 | 3.9 | 4.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith cache off | modulith cache on | macro-monolith cache off | macro-monolith cache on | groups cache off | groups cache on | microservices cache off | microservices cache on | microservices-macro cache off | microservices-macro cache on | loopback-codec cache off | loopback-codec cache on | loopback-http cache off | loopback-http cache on | split-dbs cache off | split-dbs cache on |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | 78.0% | - | 78.0% | - | 78.0% | - | 77.7% | - | 77.7% | - | 78.0% | - | 78.0% | - | 78.0% |
| catalog.productExists | - | 78.0% | - | 78.0% | - | 78.0% | - | 77.7% | - | 77.7% | - | 78.0% | - | 78.0% | - | 78.0% |
| customers.getCustomer | - | 50.3% | - | 50.4% | - | 50.7% | - | 45.9% | - | 45.9% | - | 50.3% | - | 50.5% | - | 50.6% |
| customers.getPaymentMethod | - | 35.4% | - | 35.4% | - | 35.4% | - | 35.2% | - | 35.2% | - | 35.4% | - | 35.4% | - | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith cache off | - | 29919 calls, 0.68 ms | - | - | - |
| modulith cache on | 8987 calls, 0.03 ms | 20932 calls, 0.71 ms | - | - | - |
| macro-monolith cache off | - | 29919 calls, 0.67 ms | - | - | - |
| macro-monolith cache on | 8989 calls, 0.03 ms | 20930 calls, 0.68 ms | - | - | - |
| groups cache off | - | 13500 calls, 0.55 ms | - | - | 16419 calls, 1.69 ms |
| groups cache on | 9011 calls, 0.02 ms | 8261 calls, 0.55 ms | - | - | 12647 calls, 1.65 ms |
| microservices cache off | - | - | - | - | 29919 calls, 2.04 ms |
| microservices cache on | 8632 calls, 0.01 ms | - | - | - | 21287 calls, 2.02 ms |
| microservices-macro cache off | - | - | - | - | 29919 calls, 2.04 ms |
| microservices-macro cache on | 8631 calls, 0.01 ms | - | - | - | 21288 calls, 1.99 ms |
| loopback-codec cache off | - | - | 29919 calls, 0.75 ms | - | - |
| loopback-codec cache on | 8987 calls, 0.00 ms | - | 20932 calls, 0.79 ms | - | - |
| loopback-http cache off | - | - | - | 29919 calls, 1.53 ms | - |
| loopback-http cache on | 8996 calls, 0.00 ms | - | - | 20923 calls, 1.50 ms | - |
| split-dbs cache off | - | 13500 calls, 0.54 ms | - | - | 16419 calls, 1.66 ms |
| split-dbs cache on | 9008 calls, 0.02 ms | 8261 calls, 0.54 ms | - | - | 12650 calls, 1.60 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache off, parallel quotes on, async outbox)

1500 orders in 4.9 s = 306.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 12.6 | 52.1 | 50.9 | 55.4 | 56.9 | 160.9 | 173.0 |
| placeOrder | declined | 27 | 41.4 | 46.6 | 46.0 | 50.3 | 50.9 | 54.7 | 54.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.9 | 1.8 | 2.3 | 2.5 | 6.6 |
| orders -> shipping.quote [local] | 1500 |  | 1.7 | 1.5 | 2.7 | 3.0 | 109.8 |
| orders -> inventory.reserve [local] | 1500 |  | 1.5 | 1.4 | 2.1 | 2.6 | 112.5 |
| orders -> pricing.quote [local] | 1500 |  | 1.3 | 1.2 | 1.8 | 2.0 | 109.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.7 | 0.6 | 0.7 | 0.8 | 107.6 |
| orders -> payments.authorize [local] | 1500 | 27 | 1.1 | 1.1 | 1.3 | 1.4 | 3.2 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.5 | 2.1 | 3.0 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.3 | 0.3 | 0.4 | 107.0 |
| orders -> payments.capture [local] | 1473 |  | 0.6 | 0.6 | 0.8 | 0.8 | 1.5 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.3 | 0.3 | 0.3 | 0.4 | 107.0 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.3 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.6 | 0.6 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.4 | 0.3 | 0.4 | 0.5 | 106.7 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.2 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.7 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.4 | 1.3 | 2.2 | 2.5 | 2.5 |

By binding: local 29919 calls, mean 0.68 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/modulith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox)

1500 orders in 4.5 s = 329.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.7 | 48.3 | 46.9 | 50.9 | 52.3 | 161.1 | 167.2 |
| placeOrder | declined | 27 | 40.3 | 44.0 | 43.5 | 47.7 | 47.7 | 49.9 | 49.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3748 | 50.3% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.6 | 1.6 | 2.1 | 2.3 | 5.3 |
| orders -> inventory.reserve [local] | 1500 |  | 1.4 | 1.4 | 2.1 | 2.4 | 6.3 |
| orders -> shipping.quote [local] | 1500 |  | 1.2 | 1.0 | 2.0 | 2.4 | 111.4 |
| orders -> payments.authorize [local] | 1500 | 27 | 1.1 | 1.0 | 1.2 | 1.4 | 109.6 |
| orders -> pricing.quote [local] | 1500 |  | 0.8 | 0.7 | 1.2 | 1.5 | 110.5 |
| orders -> notifications.send [local] | 1473 |  | 0.8 | 0.7 | 1.3 | 1.8 | 5.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.7 | 0.7 | 6.3 |
| orders -> payments.capture [local] | 1473 |  | 0.6 | 0.6 | 0.8 | 0.9 | 6.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.5 | 0.8 |
| orders -> customers.getCustomer [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.4 | 0.3 | 0.4 | 0.4 | 109.3 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.1 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.1 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| notifications -> customers.getCustomer [local] | 792 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.8 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.1 | 1.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> inventory.release [local] | 27 |  | 1.5 | 1.3 | 2.1 | 2.8 | 2.8 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 681 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8987 calls, mean 0.03 ms, local 20932 calls, mean 0.71 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/modulith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-monolith (cache off, parallel quotes on, async outbox)

1500 orders in 4.9 s = 305.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.3 | 52.2 | 51.2 | 55.3 | 56.5 | 153.7 | 161.1 |
| placeOrder | declined | 27 | 43.0 | 47.0 | 46.3 | 50.7 | 53.0 | 54.0 | 54.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.9 | 1.8 | 2.3 | 2.6 | 6.2 |
| orders -> shipping.quote [local] | 1500 |  | 1.7 | 1.5 | 2.7 | 3.0 | 104.5 |
| orders -> inventory.reserve [local] | 1500 |  | 1.4 | 1.4 | 2.1 | 2.5 | 6.5 |
| orders -> pricing.quote [local] | 1500 |  | 1.3 | 1.2 | 1.8 | 2.0 | 103.4 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.6 | 0.6 | 0.7 | 0.8 | 101.9 |
| orders -> payments.authorize [local] | 1500 | 27 | 1.1 | 1.1 | 1.3 | 1.4 | 5.6 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.5 | 2.1 | 3.0 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.3 | 0.3 | 0.4 | 101.2 |
| orders -> payments.capture [local] | 1473 |  | 0.6 | 0.6 | 0.8 | 0.8 | 1.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.3 | 0.3 | 0.3 | 0.4 | 101.0 |
| shipping -> customers.getCustomer [local] | 2973 |  | 0.3 | 0.3 | 0.4 | 0.5 | 5.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.5 | 0.6 |
| orders -> customers.getCustomer [local] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.6 |
| pricing -> customers.getCustomer [local] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.8 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| notifications -> customers.getCustomer [local] | 1473 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.5 | 1.5 | 1.9 | 2.3 | 2.3 |

By binding: local 29919 calls, mean 0.67 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/macro-monolith-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### macro-monolith (cache on, parallel quotes on, async outbox)

1500 orders in 4.4 s = 339.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 15.7 | 47.0 | 46.8 | 50.8 | 52.2 | 55.3 | 58.8 |
| placeOrder | declined | 27 | 40.5 | 44.1 | 43.0 | 49.7 | 50.3 | 51.7 | 51.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3750 | 50.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.6 | 1.6 | 2.1 | 2.3 | 7.8 |
| orders -> inventory.reserve [local] | 1500 |  | 1.4 | 1.4 | 2.1 | 2.4 | 7.9 |
| orders -> shipping.quote [local] | 1500 |  | 1.1 | 1.0 | 2.0 | 2.4 | 2.9 |
| orders -> payments.authorize [local] | 1500 | 27 | 1.0 | 1.0 | 1.2 | 1.4 | 1.8 |
| orders -> notifications.send [local] | 1473 |  | 0.8 | 0.7 | 1.3 | 1.8 | 2.7 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.7 | 0.7 | 1.0 |
| orders -> pricing.quote [local] | 1500 |  | 0.7 | 0.7 | 1.2 | 1.5 | 2.4 |
| orders -> payments.capture [local] | 1473 |  | 0.6 | 0.6 | 0.8 | 0.8 | 6.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.5 | 6.5 |
| orders -> customers.getCustomer [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.1 |
| shipping -> customers.getCustomer [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.1 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| notifications -> customers.getCustomer [local] | 789 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.3 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> inventory.release [local] | 27 |  | 1.4 | 1.4 | 2.0 | 2.3 | 2.3 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| notifications -> customers.getCustomer [cache] | 684 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8989 calls, mean 0.03 ms, local 20930 calls, mean 0.68 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/macro-monolith-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async outbox)

1500 orders in 7.5 s = 200.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.8 | 79.5 | 78.8 | 87.3 | 89.6 | 93.9 | 97.4 |
| placeOrder | declined | 27 | 67.1 | 75.2 | 73.7 | 84.2 | 84.6 | 84.7 | 84.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.2 | 3.2 | 3.8 | 4.2 | 8.2 |
| orders -> shipping.quote [remote] | 1500 |  | 3.0 | 2.9 | 4.3 | 5.1 | 9.1 |
| orders -> pricing.quote [remote] | 1500 |  | 2.6 | 2.5 | 3.3 | 4.3 | 8.0 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.1 | 2.0 | 2.8 | 3.3 | 4.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.8 | 2.1 | 2.5 | 6.7 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.9 | 1.1 | 1.2 | 5.5 |
| orders -> notifications.send [local] | 1473 |  | 1.7 | 1.6 | 2.9 | 4.0 | 7.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.7 | 0.6 | 0.8 | 0.9 | 5.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.3 | 1.2 | 1.5 | 1.7 | 5.9 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.2 | 1.3 | 6.4 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.9 | 0.9 | 1.1 | 1.3 | 5.3 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.6 | 2.6 | 6.5 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.7 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.5 | 4.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.6 | 4.4 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.7 |
| orders -> inventory.release [remote] | 27 |  | 3.1 | 3.1 | 4.0 | 5.6 | 5.6 |

By binding: local 13500 calls, mean 0.55 ms, remote 16419 calls, mean 1.69 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/groups-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

1500 orders in 6.6 s = 227.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.9 | 70.0 | 69.2 | 77.4 | 80.0 | 83.1 | 92.5 |
| placeOrder | declined | 27 | 60.6 | 66.6 | 65.8 | 72.5 | 74.3 | 76.0 | 76.0 |

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
| orders -> shipping.createShipment [remote] | 1473 |  | 2.3 | 2.3 | 2.8 | 3.4 | 8.6 |
| orders -> shipping.quote [remote] | 1500 |  | 2.3 | 2.2 | 3.4 | 4.1 | 8.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.1 | 2.0 | 2.8 | 3.4 | 6.6 |
| orders -> pricing.quote [remote] | 1500 |  | 1.9 | 2.0 | 2.7 | 3.1 | 7.6 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.7 | 2.2 | 2.6 | 3.9 |
| orders -> notifications.send [local] | 1473 |  | 1.3 | 1.3 | 2.3 | 4.2 | 5.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.3 | 1.2 | 1.5 | 1.7 | 5.7 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.8 | 1.2 |
| orders -> customers.getCustomer [remote] | 969 |  | 1.0 | 1.0 | 1.2 | 1.5 | 5.6 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.2 | 1.4 | 6.4 |
| shipping -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.2 | 1.4 | 6.2 |
| notifications -> customers.getCustomer [remote] | 768 |  | 1.0 | 0.9 | 2.1 | 3.0 | 3.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.6 | 3.6 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| orders -> inventory.release [remote] | 27 |  | 2.9 | 2.8 | 4.0 | 4.1 | 4.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 705 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9011 calls, mean 0.02 ms, local 8261 calls, mean 0.55 ms, remote 12647 calls, mean 1.65 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/groups-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async outbox)

1500 orders in 10.7 s = 139.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.2 | 113.9 | 111.8 | 126.1 | 133.3 | 190.4 | 220.6 |
| placeOrder | declined | 27 | 93.2 | 105.4 | 101.2 | 117.1 | 127.3 | 133.6 | 133.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.2 | 6.0 | 10.0 | 11.3 | 77.4 |
| orders -> pricing.quote [remote] | 1500 |  | 4.5 | 4.3 | 6.7 | 7.9 | 10.8 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.4 | 5.5 | 6.4 | 73.8 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.2 | 2.1 | 2.8 | 3.3 | 70.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 3.8 | 4.8 | 64.7 |
| orders -> notifications.send [remote] | 1473 |  | 2.8 | 2.5 | 4.8 | 6.5 | 72.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.3 | 3.2 | 3.8 | 6.6 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.1 | 1.0 | 1.5 | 1.9 | 69.0 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.0 | 1.0 | 1.4 | 1.7 | 4.1 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 1.0 | 1.0 | 1.3 | 1.5 | 4.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 1.9 | 2.3 | 62.9 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.1 | 1.4 | 1.7 | 69.9 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.1 | 1.1 | 1.5 | 1.8 | 3.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.1 | 1.0 | 1.4 | 1.6 | 5.2 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 1.0 | 1.3 | 1.5 | 4.7 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.5 | 4.7 |
| orders -> inventory.release [remote] | 27 |  | 2.9 | 2.8 | 4.0 | 4.1 | 4.1 |

By binding: remote 29919 calls, mean 2.04 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/microservices-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

1500 orders in 9.1 s = 164.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 25.8 | 96.9 | 92.0 | 107.6 | 118.9 | 209.4 | 334.5 |
| placeOrder | declined | 27 | 71.8 | 90.7 | 84.6 | 99.9 | 104.4 | 198.2 | 198.2 |

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
| orders -> shipping.quote [remote] | 1500 |  | 4.4 | 3.9 | 7.5 | 9.3 | 126.5 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 4.3 | 4.9 | 125.0 |
| orders -> pricing.quote [remote] | 1500 |  | 2.9 | 2.4 | 4.7 | 6.8 | 76.9 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 1.0 | 2.6 | 3.3 | 124.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.7 | 2.6 | 3.7 | 4.5 | 117.9 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.3 | 4.3 | 6.2 | 61.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.1 | 3.8 | 127.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.3 | 1.7 | 2.1 | 112.1 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.1 | 1.4 | 1.7 | 118.5 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 1.9 | 2.7 | 4.0 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.3 | 1.1 | 1.7 | 2.1 | 65.5 |
| inventory -> catalog.productExists [remote] | 674 |  | 1.7 | 1.2 | 2.0 | 3.5 | 123.0 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.1 | 1.5 | 1.7 | 4.3 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.1 | 1.4 | 1.7 | 2.5 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.0 | 1.0 | 1.4 | 1.7 | 6.1 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.4 | 1.1 | 1.8 | 3.7 | 61.7 |
| orders -> inventory.release [remote] | 27 |  | 2.8 | 2.8 | 3.7 | 3.9 | 3.9 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8632 calls, mean 0.01 ms, remote 21287 calls, mean 2.02 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/microservices-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async outbox)

1500 orders in 10.7 s = 140.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.2 | 113.6 | 111.1 | 124.7 | 132.9 | 186.4 | 269.2 |
| placeOrder | declined | 27 | 92.9 | 102.5 | 101.9 | 109.5 | 111.1 | 121.3 | 121.3 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.3 | 5.9 | 10.1 | 11.1 | 74.8 |
| orders -> pricing.quote [remote] | 1500 |  | 4.5 | 4.4 | 6.4 | 7.7 | 10.3 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.2 | 2.1 | 2.7 | 3.1 | 65.9 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.3 | 5.4 | 6.5 | 65.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 3.8 | 5.3 | 55.7 |
| orders -> notifications.send [remote] | 1473 |  | 2.8 | 2.5 | 4.9 | 6.5 | 71.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.2 | 3.8 | 6.9 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.1 | 1.0 | 1.5 | 1.7 | 63.5 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.0 | 1.0 | 1.4 | 1.6 | 3.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 1.0 | 1.0 | 1.2 | 1.4 | 4.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.4 | 1.4 | 1.9 | 2.2 | 6.5 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.1 | 1.1 | 1.5 | 2.1 | 3.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.1 | 1.0 | 1.4 | 1.6 | 6.6 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.1 | 1.4 | 1.6 | 3.6 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.4 | 4.8 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 1.0 | 1.3 | 1.5 | 4.1 |
| orders -> inventory.release [remote] | 27 |  | 2.9 | 2.9 | 3.5 | 4.1 | 4.1 |

By binding: remote 29919 calls, mean 2.04 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/microservices-macro-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox)

1500 orders in 8.9 s = 168.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 28.3 | 94.4 | 91.4 | 105.2 | 111.2 | 179.3 | 235.2 |
| placeOrder | declined | 27 | 76.3 | 86.5 | 83.8 | 98.1 | 102.6 | 121.3 | 121.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2344 | 77.7% |
| catalog.productExists | 3018 | 2344 | 77.7% |
| customers.getCustomer | 7446 | 3415 | 45.9% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.3 | 3.9 | 7.4 | 9.8 | 64.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.3 | 3.3 | 4.2 | 4.9 | 8.7 |
| orders -> pricing.quote [remote] | 1500 |  | 2.9 | 2.4 | 4.8 | 7.3 | 66.6 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 1.1 | 2.6 | 3.6 | 61.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.6 | 2.6 | 3.7 | 4.9 | 63.8 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.3 | 4.3 | 6.4 | 74.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.1 | 3.7 | 7.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.4 | 1.4 | 1.8 | 2.1 | 5.5 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.1 | 1.4 | 1.7 | 4.0 |
| notifications -> customers.getCustomer [remote] | 1115 |  | 1.1 | 1.0 | 2.0 | 2.6 | 3.6 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.2 | 1.1 | 1.7 | 2.1 | 59.9 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 1.4 | 1.7 | 57.9 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.1 | 1.5 | 1.9 | 7.0 |
| inventory -> catalog.productExists [remote] | 674 |  | 1.5 | 1.2 | 2.3 | 4.3 | 59.9 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.0 | 1.0 | 1.3 | 1.6 | 4.2 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.5 | 1.2 | 2.0 | 3.3 | 58.7 |
| orders -> inventory.release [remote] | 27 |  | 3.0 | 2.8 | 4.4 | 7.1 | 7.1 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.4 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 358 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8631 calls, mean 0.01 ms, remote 21288 calls, mean 1.99 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/microservices-macro-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache off, parallel quotes on, async outbox)

1500 orders in 5.2 s = 286.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.2 | 55.6 | 54.7 | 59.5 | 60.9 | 128.8 | 135.2 |
| placeOrder | declined | 27 | 46.8 | 51.3 | 50.1 | 56.2 | 56.6 | 57.7 | 57.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 2.1 | 2.0 | 2.5 | 2.8 | 8.2 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 2.0 | 1.8 | 3.2 | 3.5 | 76.0 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.5 | 1.4 | 2.2 | 2.6 | 6.7 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 1.5 | 1.4 | 2.1 | 2.3 | 74.7 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.7 | 0.7 | 0.8 | 0.9 | 73.1 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 1.2 | 1.2 | 1.4 | 1.5 | 6.2 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 1.0 | 0.9 | 1.7 | 2.3 | 2.9 |
| pricing -> catalog.getProduct [loopback-codec] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.4 | 72.6 |
| inventory -> catalog.productExists [loopback-codec] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.4 | 72.5 |
| shipping -> customers.getCustomer [loopback-codec] | 2973 |  | 0.3 | 0.3 | 0.4 | 0.6 | 4.6 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.7 | 0.6 | 0.8 | 0.9 | 1.0 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.6 | 4.8 |
| orders -> customers.getCustomer [loopback-codec] | 1500 |  | 0.4 | 0.4 | 0.5 | 0.6 | 4.5 |
| pricing -> customers.getCustomer [loopback-codec] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.6 | 4.6 |
| payments -> customers.getPaymentMethod [loopback-codec] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| notifications -> customers.getCustomer [loopback-codec] | 1473 |  | 0.3 | 0.2 | 0.5 | 0.7 | 1.2 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.6 | 1.5 | 2.2 | 2.8 | 2.8 |

By binding: loopback-codec 29919 calls, mean 0.75 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/loopback-codec-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-codec (cache on, parallel quotes on, async outbox)

1500 orders in 4.8 s = 313.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 16.1 | 50.8 | 49.4 | 53.9 | 55.7 | 163.5 | 171.9 |
| placeOrder | declined | 27 | 41.7 | 46.4 | 45.4 | 50.9 | 51.6 | 55.9 | 55.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3748 | 50.3% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [loopback-codec] | 1473 |  | 1.7 | 1.7 | 2.2 | 2.5 | 8.2 |
| orders -> inventory.reserve [loopback-codec] | 1500 |  | 1.6 | 1.4 | 2.2 | 2.7 | 114.8 |
| orders -> shipping.quote [loopback-codec] | 1500 |  | 1.3 | 1.2 | 2.3 | 2.7 | 112.5 |
| orders -> payments.authorize [loopback-codec] | 1500 | 27 | 1.1 | 1.1 | 1.4 | 1.5 | 114.7 |
| orders -> pricing.quote [loopback-codec] | 1500 |  | 0.9 | 0.8 | 1.3 | 1.7 | 111.7 |
| orders -> notifications.send [loopback-codec] | 1473 |  | 0.9 | 0.8 | 1.4 | 2.0 | 2.5 |
| shipping -> inventory.getStock [loopback-codec] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.8 | 1.4 |
| orders -> payments.capture [loopback-codec] | 1473 |  | 0.7 | 0.7 | 0.8 | 0.9 | 6.9 |
| shipping -> inventory.getReservation [loopback-codec] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.6 | 0.7 |
| pricing -> customers.getCustomer [loopback-codec] | 969 |  | 0.5 | 0.3 | 0.4 | 0.6 | 109.5 |
| payments -> customers.getPaymentMethod [loopback-codec] | 969 |  | 0.4 | 0.3 | 0.4 | 0.5 | 110.0 |
| orders -> customers.getCustomer [loopback-codec] | 969 |  | 0.4 | 0.4 | 0.5 | 0.5 | 0.9 |
| shipping -> customers.getCustomer [loopback-codec] | 969 |  | 0.3 | 0.3 | 0.4 | 0.6 | 1.2 |
| pricing -> catalog.getProduct [loopback-codec] | 664 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| notifications -> customers.getCustomer [loopback-codec] | 791 |  | 0.3 | 0.2 | 0.5 | 0.7 | 1.1 |
| inventory -> catalog.productExists [loopback-codec] | 664 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| orders -> inventory.release [loopback-codec] | 27 |  | 1.5 | 1.4 | 2.4 | 2.4 | 2.4 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 682 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8987 calls, mean 0.00 ms, loopback-codec 20932 calls, mean 0.79 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/loopback-codec-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache off, parallel quotes on, async outbox)

1500 orders in 8.3 s = 181.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 24.1 | 87.9 | 88.5 | 96.7 | 99.0 | 103.3 | 110.8 |
| placeOrder | declined | 27 | 67.2 | 82.1 | 83.7 | 89.8 | 90.8 | 97.0 | 97.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 4.6 | 4.5 | 7.9 | 9.2 | 15.3 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 3.5 | 3.4 | 4.0 | 4.5 | 11.3 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 3.3 | 3.2 | 5.0 | 7.0 | 12.1 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 1.6 | 1.6 | 1.8 | 2.0 | 8.6 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 2.1 | 2.1 | 2.4 | 2.5 | 12.1 |
| orders -> notifications.send [loopback-http] | 1473 |  | 2.0 | 1.9 | 3.2 | 3.9 | 9.7 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 2.0 | 1.9 | 2.7 | 3.1 | 12.1 |
| pricing -> catalog.getProduct [loopback-http] | 3018 |  | 0.8 | 0.8 | 0.9 | 1.0 | 7.7 |
| inventory -> catalog.productExists [loopback-http] | 3018 |  | 0.8 | 0.7 | 0.9 | 1.0 | 7.3 |
| shipping -> customers.getCustomer [loopback-http] | 2973 |  | 0.8 | 0.8 | 0.9 | 1.0 | 7.9 |
| orders -> payments.capture [loopback-http] | 1473 |  | 1.1 | 1.1 | 1.3 | 1.4 | 7.1 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.9 | 0.9 | 1.0 | 1.1 | 8.6 |
| orders -> customers.getCustomer [loopback-http] | 1500 |  | 0.9 | 0.8 | 1.0 | 1.2 | 8.3 |
| pricing -> customers.getCustomer [loopback-http] | 1500 |  | 0.8 | 0.8 | 0.9 | 1.1 | 6.1 |
| payments -> customers.getPaymentMethod [loopback-http] | 1500 |  | 0.8 | 0.8 | 0.9 | 1.0 | 7.6 |
| notifications -> customers.getCustomer [loopback-http] | 1473 |  | 0.8 | 0.7 | 1.2 | 1.5 | 2.5 |
| orders -> inventory.release [loopback-http] | 27 |  | 2.5 | 2.4 | 3.7 | 3.8 | 3.8 |

By binding: loopback-http 29919 calls, mean 1.53 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/loopback-http-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async outbox)

1500 orders in 7.2 s = 209.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 20.0 | 76.1 | 75.8 | 83.6 | 86.1 | 91.9 | 95.6 |
| placeOrder | declined | 27 | 62.7 | 71.3 | 69.6 | 77.1 | 79.2 | 81.2 | 81.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3757 | 50.5% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1500 |  | 3.3 | 3.1 | 5.7 | 7.6 | 15.3 |
| orders -> shipping.createShipment [loopback-http] | 1473 |  | 2.8 | 2.7 | 3.3 | 3.6 | 12.4 |
| shipping -> inventory.getStock [loopback-http] | 3018 |  | 1.0 | 0.9 | 1.7 | 1.9 | 8.8 |
| orders -> inventory.reserve [loopback-http] | 1500 |  | 2.0 | 1.9 | 2.7 | 3.2 | 13.2 |
| orders -> pricing.quote [loopback-http] | 1500 |  | 1.9 | 1.8 | 3.1 | 4.6 | 12.2 |
| orders -> payments.authorize [loopback-http] | 1500 | 27 | 1.9 | 2.1 | 2.4 | 2.5 | 9.7 |
| orders -> notifications.send [loopback-http] | 1473 |  | 1.7 | 1.6 | 2.6 | 3.7 | 8.3 |
| orders -> payments.capture [loopback-http] | 1473 |  | 1.2 | 1.1 | 1.3 | 1.4 | 10.6 |
| shipping -> inventory.getReservation [loopback-http] | 1473 |  | 0.9 | 0.9 | 1.1 | 1.1 | 8.7 |
| orders -> customers.getCustomer [loopback-http] | 969 |  | 0.9 | 0.9 | 1.0 | 1.1 | 10.0 |
| shipping -> customers.getCustomer [loopback-http] | 969 |  | 0.9 | 0.8 | 0.9 | 1.2 | 9.9 |
| pricing -> customers.getCustomer [loopback-http] | 968 |  | 0.8 | 0.8 | 0.9 | 1.2 | 10.0 |
| payments -> customers.getPaymentMethod [loopback-http] | 969 |  | 0.8 | 0.8 | 0.9 | 1.1 | 6.7 |
| notifications -> customers.getCustomer [loopback-http] | 783 |  | 0.8 | 0.7 | 1.2 | 1.8 | 2.3 |
| pricing -> catalog.getProduct [loopback-http] | 664 |  | 0.8 | 0.8 | 1.0 | 1.1 | 6.8 |
| inventory -> catalog.productExists [loopback-http] | 664 |  | 0.8 | 0.8 | 0.9 | 1.1 | 6.8 |
| orders -> inventory.release [loopback-http] | 27 |  | 2.2 | 2.2 | 2.7 | 3.4 | 3.4 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 690 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 532 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8996 calls, mean 0.00 ms, loopback-http 20923 calls, mean 1.50 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/loopback-http-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache off, parallel quotes on, async outbox)

1500 orders in 7.3 s = 206.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 24.2 | 77.3 | 76.7 | 84.8 | 88.1 | 93.7 | 100.8 |
| placeOrder | declined | 27 | 62.9 | 71.2 | 71.4 | 78.1 | 80.9 | 82.4 | 82.4 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.2 | 3.1 | 3.7 | 4.1 | 13.7 |
| orders -> shipping.quote [remote] | 1500 |  | 3.0 | 2.9 | 4.2 | 4.9 | 7.3 |
| orders -> pricing.quote [remote] | 1500 |  | 2.6 | 2.5 | 3.2 | 4.1 | 7.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 2.7 | 3.1 | 11.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.7 | 2.1 | 2.5 | 6.2 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.9 | 1.1 | 1.2 | 5.0 |
| orders -> notifications.send [local] | 1473 |  | 1.7 | 1.5 | 2.8 | 4.1 | 5.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.6 | 0.6 | 0.8 | 0.9 | 1.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.2 | 1.5 | 1.6 | 5.0 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.1 | 1.3 | 5.7 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.9 | 0.9 | 1.1 | 1.2 | 5.0 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.7 | 2.6 | 3.3 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.6 | 0.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.6 |
| orders -> inventory.release [remote] | 27 |  | 2.9 | 2.8 | 3.9 | 4.3 | 4.3 |

By binding: local 13500 calls, mean 0.54 ms, remote 16419 calls, mean 1.66 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/split-dbs-cache-off` (`orders.csv`, `calls.csv`, `summary.json`).

### split-dbs (cache on, parallel quotes on, async outbox)

1500 orders in 6.4 s = 234.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 22.5 | 67.8 | 67.4 | 74.6 | 77.7 | 84.2 | 92.6 |
| placeOrder | declined | 27 | 58.3 | 63.6 | 62.7 | 70.3 | 75.4 | 76.8 | 76.8 |

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
| orders -> shipping.createShipment [remote] | 1473 |  | 2.2 | 2.2 | 2.7 | 3.0 | 7.7 |
| orders -> shipping.quote [remote] | 1500 |  | 2.2 | 2.2 | 3.4 | 4.1 | 7.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 2.7 | 3.2 | 12.3 |
| orders -> pricing.quote [remote] | 1500 |  | 1.8 | 1.9 | 2.6 | 3.4 | 7.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.7 | 2.1 | 2.7 | 5.3 |
| orders -> notifications.send [local] | 1473 |  | 1.3 | 1.3 | 2.4 | 4.1 | 5.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.2 | 1.5 | 1.6 | 2.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.8 | 5.5 |
| orders -> customers.getCustomer [remote] | 969 |  | 1.0 | 1.0 | 1.2 | 1.5 | 6.1 |
| shipping -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.9 | 1.1 | 1.4 | 5.2 |
| pricing -> customers.getCustomer [remote] | 966 |  | 0.9 | 0.9 | 1.1 | 1.3 | 5.1 |
| notifications -> customers.getCustomer [remote] | 773 |  | 1.0 | 0.9 | 2.0 | 2.8 | 3.3 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.5 | 0.6 | 0.8 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 |
| orders -> inventory.release [remote] | 27 |  | 3.1 | 3.0 | 4.1 | 4.2 | 4.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 700 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 534 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9008 calls, mean 0.02 ms, local 8261 calls, mean 0.54 ms, remote 12650 calls, mean 1.60 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261025T211735Z/split-dbs-cache-on` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
