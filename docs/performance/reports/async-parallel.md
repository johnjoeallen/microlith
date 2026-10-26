# Performance run 20261026T185537Z

- Started: 2026-10-26T18:55:37+00:00
- Commit: 3e82736 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes modulith,groups,microservices --async both --parallel both --orders 1500 --warmup 200 --concurrency 16`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith parallel off async outbox | 454 | 1473 | 27 | 17.9 | 35.1 | 33.0 | 45.3 | 49.6 | 54.6 | 62.9 | 19.9 | PASS |
| modulith parallel off async events | 425 | 1473 | 27 | 19.8 | 37.5 | 36.0 | 45.9 | 51.0 | 57.2 | 65.3 | 19.9 | PASS |
| modulith parallel on async outbox | 451 | 1473 | 27 | 17.0 | 35.2 | 33.0 | 43.7 | 47.6 | 85.2 | 91.2 | 19.9 | PASS |
| modulith parallel on async events | 439 | 1473 | 27 | 18.8 | 36.2 | 34.4 | 45.5 | 48.5 | 57.9 | 65.5 | 19.9 | PASS |
| groups parallel off async outbox | 230 | 1473 | 27 | 37.1 | 69.3 | 66.3 | 86.2 | 93.2 | 140.9 | 164.1 | 19.9 | PASS |
| groups parallel off async events | 228 | 1473 | 27 | 30.8 | 70.0 | 65.9 | 93.7 | 98.5 | 108.3 | 124.2 | 19.9 | PASS |
| groups parallel on async outbox | 253 | 1473 | 27 | 33.3 | 62.9 | 60.4 | 82.4 | 87.8 | 96.0 | 112.7 | 19.9 | PASS |
| groups parallel on async events | 244 | 1473 | 27 | 29.8 | 65.4 | 63.2 | 85.9 | 91.4 | 105.2 | 122.0 | 19.9 | PASS |
| microservices parallel off async outbox | 138 | 1473 | 27 | 47.9 | 115.3 | 110.1 | 155.2 | 169.0 | 217.5 | 245.9 | 19.9 | PASS |
| microservices parallel off async events | 133 | 1473 | 27 | 56.3 | 120.2 | 112.5 | 163.8 | 178.9 | 213.8 | 251.6 | 19.9 | PASS |
| microservices parallel on async outbox | 160 | 1473 | 27 | 37.3 | 99.8 | 96.0 | 133.1 | 143.9 | 170.1 | 190.1 | 19.9 | PASS |
| microservices parallel on async events | 150 | 1473 | 27 | 37.9 | 105.9 | 102.5 | 140.7 | 151.8 | 173.8 | 202.6 | 19.9 | PASS |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (async outbox) | 454 | 451 | -0% | 33.0 | 33.0 | +0% | 49.6 | 47.6 | -4% | 54.6 | 85.2 | +56% |
| modulith (async events) | 425 | 439 | +3% | 36.0 | 34.4 | -5% | 51.0 | 48.5 | -5% | 57.2 | 57.9 | +1% |
| groups (async outbox) | 230 | 253 | +10% | 66.3 | 60.4 | -9% | 93.2 | 87.8 | -6% | 140.9 | 96.0 | -32% |
| groups (async events) | 228 | 244 | +7% | 65.9 | 63.2 | -4% | 98.5 | 91.4 | -7% | 108.3 | 105.2 | -3% |
| microservices (async outbox) | 138 | 160 | +15% | 110.1 | 96.0 | -13% | 169.0 | 143.9 | -15% | 217.5 | 170.1 | -22% |
| microservices (async events) | 133 | 150 | +13% | 112.5 | 102.5 | -9% | 178.9 | 151.8 | -15% | 213.8 | 173.8 | -19% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| modulith (parallel off) | 454 | 425 | -6% | 33.0 | 36.0 | +9% | 49.6 | 51.0 | +3% | 54.6 | 57.2 | +5% |
| modulith (parallel on) | 451 | 439 | -3% | 33.0 | 34.4 | +4% | 47.6 | 48.5 | +2% | 85.2 | 57.9 | -32% |
| groups (parallel off) | 230 | 228 | -1% | 66.3 | 65.9 | -1% | 93.2 | 98.5 | +6% | 140.9 | 108.3 | -23% |
| groups (parallel on) | 253 | 244 | -4% | 60.4 | 63.2 | +5% | 87.8 | 91.4 | +4% | 96.0 | 105.2 | +10% |
| microservices (parallel off) | 138 | 133 | -4% | 110.1 | 112.5 | +2% | 169.0 | 178.9 | +6% | 217.5 | 213.8 | -2% |
| microservices (parallel on) | 160 | 150 | -6% | 96.0 | 102.5 | +7% | 143.9 | 151.8 | +5% | 170.1 | 173.8 | +2% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups parallel off async outbox | 7.8 | 2,840 | 2,840 | 100% | 165 to 165 | 200 to 200 |
| groups parallel off async events | 7.3 | 2,686 | 2,686 | 100% | 170 to 170 | 199 to 199 |
| groups parallel on async outbox | 8.4 | 3,039 | 3,039 | 100% | 160 to 160 | 200 to 200 |
| groups parallel on async events | 7.9 | 2,883 | 2,883 | 100% | 164 to 164 | 200 to 200 |
| microservices parallel off async outbox | 14.3 | 4,550 | 4,550 | 100% | 145 to 145 | 173 to 173 |
| microservices parallel off async events | 14.1 | 4,475 | 4,475 | 100% | 145 to 145 | 173 to 173 |
| microservices parallel on async outbox | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |
| microservices parallel on async events | 14.1 | 4,475 | 4,475 | 100% | 145 to 145 | 173 to 173 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith parallel off async outbox | modulith parallel off async events | modulith parallel on async outbox | modulith parallel on async events | groups parallel off async outbox | groups parallel off async events | groups parallel on async outbox | groups parallel on async events | microservices parallel off async outbox | microservices parallel off async events | microservices parallel on async outbox | microservices parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 0.5 | 0.5 | 0.7 | 0.7 | 1.1 | 1.1 | 1.9 | 2.0 | 4.2 | 4.4 | 4.6 | 5.0 |
| orders -> shipping.createShipment | 1.1 | 1.1 | 1.1 | 1.2 | 1.8 | 1.9 | 1.9 | 2.0 | 3.4 | 3.5 | 3.6 | 3.7 |
| orders -> payments.authorize | 0.7 | 0.8 | 0.7 | 0.8 | 1.7 | 1.7 | 1.7 | 1.8 | 2.9 | 3.0 | 3.0 | 3.2 |
| orders -> pricing.quote | 0.3 | 0.3 | 0.4 | 0.5 | 1.6 | 1.6 | 1.6 | 1.7 | 3.2 | 3.5 | 3.4 | 3.8 |
| orders -> inventory.reserve | 1.1 | 1.2 | 1.2 | 1.1 | 1.9 | 1.9 | 1.9 | 2.0 | 2.3 | 2.3 | 2.4 | 2.5 |
| orders -> notifications.send | 0.6 | 0.7 | 0.6 | 0.7 | 0.9 | 0.9 | 0.9 | 1.0 | 2.4 | 3.3 | 2.4 | 3.7 |
| shipping -> inventory.getStock | 0.2 | 0.2 | 0.3 | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 1.4 | 1.4 | 1.5 | 1.6 |
| orders -> payments.capture | 0.5 | 0.5 | 0.5 | 0.5 | 1.2 | 1.2 | 1.2 | 1.2 | 1.5 | 1.5 | 1.6 | 1.6 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 1.1 | 1.2 | 1.1 | 1.2 |
| pricing -> customers.getCustomer | 0.0 | 0.0 | 0.1 | 0.1 | 0.7 | 0.7 | 0.6 | 0.7 | 0.8 | 0.9 | 0.9 | 1.0 |
| orders -> customers.getCustomer | 0.1 | 0.1 | 0.1 | 0.1 | 0.6 | 0.6 | 0.6 | 0.6 | 0.7 | 0.7 | 0.8 | 0.8 |
| payments -> customers.getPaymentMethod | 0.1 | 0.1 | 0.1 | 0.1 | 0.2 | 0.2 | 0.2 | 0.2 | 0.9 | 1.0 | 1.0 | 1.1 |
| inventory -> catalog.productExists | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.4 | 0.4 | 0.5 | 0.5 |
| shipping -> customers.getCustomer | 0.0 | 0.0 | 0.1 | 0.1 | 0.0 | 0.0 | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 |
| notifications -> customers.getCustomer | 0.1 | 0.0 | 0.1 | 0.0 | 0.3 | 0.0 | 0.3 | 0.0 | 0.8 | 1.0 | 0.8 | 1.1 |
| pricing -> catalog.getProduct | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.4 | 0.4 | 0.4 | 0.5 |
| orders -> inventory.release | 1.2 | 1.1 | 1.2 | 1.1 | 2.2 | 1.7 | 2.2 | 2.0 | 2.4 | 2.3 | 2.2 | 2.3 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | modulith parallel off async outbox | modulith parallel off async events | modulith parallel on async outbox | modulith parallel on async events | groups parallel off async outbox | groups parallel off async events | groups parallel on async outbox | groups parallel on async events | microservices parallel off async outbox | microservices parallel off async events | microservices parallel on async outbox | microservices parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.1 | 1.1 | 1.3 | 1.4 | 2.1 | 2.2 | 3.4 | 3.8 | 8.9 | 9.2 | 9.8 | 10.6 |
| orders -> shipping.createShipment | 1.6 | 1.6 | 1.6 | 1.6 | 2.8 | 3.0 | 3.0 | 3.2 | 5.5 | 5.4 | 5.8 | 5.7 |
| orders -> payments.authorize | 1.0 | 1.1 | 1.0 | 1.1 | 2.8 | 2.8 | 2.9 | 3.0 | 5.1 | 5.3 | 5.3 | 5.8 |
| orders -> pricing.quote | 0.6 | 0.6 | 0.8 | 0.9 | 3.0 | 3.3 | 2.9 | 3.1 | 6.5 | 7.3 | 7.0 | 7.9 |
| orders -> inventory.reserve | 1.7 | 1.8 | 1.8 | 1.8 | 2.8 | 3.0 | 3.0 | 3.1 | 3.6 | 3.6 | 3.8 | 4.0 |
| orders -> notifications.send | 1.0 | 0.9 | 1.0 | 1.0 | 1.7 | 1.3 | 1.6 | 1.5 | 4.9 | 6.0 | 5.1 | 6.7 |
| shipping -> inventory.getStock | 0.4 | 0.4 | 0.5 | 0.5 | 0.6 | 0.6 | 0.6 | 0.7 | 3.5 | 3.5 | 3.9 | 4.1 |
| orders -> payments.capture | 0.7 | 0.7 | 0.6 | 0.7 | 1.8 | 1.8 | 1.9 | 2.1 | 2.4 | 2.4 | 2.5 | 2.6 |
| shipping -> inventory.getReservation | 0.4 | 0.4 | 0.4 | 0.4 | 0.5 | 0.5 | 0.5 | 0.6 | 1.9 | 1.9 | 2.0 | 2.0 |
| pricing -> customers.getCustomer | 0.0 | 0.0 | 0.3 | 0.3 | 1.6 | 1.7 | 1.4 | 1.6 | 1.9 | 2.0 | 2.0 | 2.3 |
| orders -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 1.2 | 1.3 | 1.4 | 1.4 | 1.6 | 1.8 | 2.0 | 2.1 |
| payments -> customers.getPaymentMethod | 0.2 | 0.3 | 0.3 | 0.3 | 0.5 | 0.5 | 0.5 | 0.5 | 2.2 | 2.3 | 2.3 | 2.5 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 2.2 | 2.3 | 2.5 | 2.6 |
| shipping -> customers.getCustomer | 0.0 | 0.0 | 0.2 | 0.3 | 0.0 | 0.0 | 1.2 | 1.4 | 1.4 | 1.5 | 1.6 | 1.7 |
| notifications -> customers.getCustomer | 0.2 | 0.0 | 0.2 | 0.0 | 0.9 | 0.0 | 0.8 | 0.0 | 1.8 | 2.4 | 1.9 | 2.5 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 2.1 | 2.2 | 2.2 | 2.4 |
| orders -> inventory.release | 1.9 | 1.7 | 1.8 | 1.6 | 2.9 | 2.5 | 3.0 | 2.9 | 3.4 | 3.6 | 2.9 | 3.7 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | modulith parallel off async outbox | modulith parallel off async events | modulith parallel on async outbox | modulith parallel on async events | groups parallel off async outbox | groups parallel off async events | groups parallel on async outbox | groups parallel on async events | microservices parallel off async outbox | microservices parallel off async events | microservices parallel on async outbox | microservices parallel on async events |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 77.7% | 77.7% | 77.8% | 77.6% |
| catalog.productExists | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 77.7% | 77.7% | 77.8% | 77.6% |
| customers.getCustomer | 76.4% | 87.0% | 50.4% | 61.0% | 63.8% | 74.0% | 50.7% | 61.0% | 45.9% | 48.1% | 45.9% | 48.1% |
| customers.getPaymentMethod | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.2% | 35.3% | 35.3% | 35.3% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| modulith parallel off async outbox | 10930 calls, 0.01 ms | 18989 calls, 0.47 ms | - | - | - |
| modulith parallel off async events | 11716 calls, 0.01 ms | 18203 calls, 0.51 ms | - | - | - |
| modulith parallel on async outbox | 8993 calls, 0.01 ms | 20926 calls, 0.48 ms | - | - | - |
| modulith parallel on async events | 9779 calls, 0.01 ms | 20140 calls, 0.52 ms | - | - | - |
| groups parallel off async outbox | 9987 calls, 0.01 ms | 8261 calls, 0.41 ms | - | - | 11671 calls, 1.39 ms |
| groups parallel off async events | 10747 calls, 0.01 ms | 8261 calls, 0.41 ms | - | - | 10911 calls, 1.48 ms |
| groups parallel on async outbox | 9012 calls, 0.01 ms | 8261 calls, 0.42 ms | - | - | 12646 calls, 1.45 ms |
| groups parallel on async events | 9779 calls, 0.01 ms | 8261 calls, 0.45 ms | - | - | 11879 calls, 1.58 ms |
| microservices parallel off async outbox | 8632 calls, 0.01 ms | - | - | - | 21287 calls, 2.06 ms |
| microservices parallel off async events | 8802 calls, 0.01 ms | - | - | - | 21117 calls, 2.24 ms |
| microservices parallel on async outbox | 8642 calls, 0.01 ms | - | - | - | 21277 calls, 2.18 ms |
| microservices parallel on async events | 8796 calls, 0.01 ms | - | - | - | 21123 calls, 2.46 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### modulith (cache on, parallel quotes off, async outbox)

1500 orders in 3.3 s = 453.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.9 | 35.1 | 33.0 | 45.3 | 49.6 | 54.6 | 62.9 |
| placeOrder | declined | 27 | 25.0 | 31.7 | 30.7 | 37.1 | 38.5 | 47.0 | 47.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 5691 | 76.4% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.2 | 3.2 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.0 | 1.6 | 2.1 | 6.5 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.0 | 1.2 | 8.6 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.6 | 2.2 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 3.1 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.5 | 3.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 3.2 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.9 | 6.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.4 | 0.4 | 0.7 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.3 | 1.2 |
| notifications -> customers.getCustomer [local] | 786 |  | 0.2 | 0.1 | 0.4 | 0.6 | 1.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.8 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.9 | 2.0 | 2.0 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 687 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 10930 calls, mean 0.01 ms, local 18989 calls, mean 0.47 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/modulith-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes off, async events)

1500 orders in 3.5 s = 425.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 19.8 | 37.5 | 36.0 | 45.9 | 51.0 | 57.2 | 65.3 |
| placeOrder | declined | 27 | 28.0 | 33.6 | 33.0 | 38.5 | 39.5 | 44.9 | 44.9 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.3 | 7.8 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 2.0 | 7.4 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.7 | 1.1 | 1.3 | 7.7 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.6 | 0.9 | 1.2 | 7.9 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 5.8 |
| orders -> shipping.quote [local] | 1500 |  | 0.5 | 0.4 | 1.1 | 1.5 | 6.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 6.3 |
| orders -> pricing.quote [local] | 1500 |  | 0.3 | 0.2 | 0.6 | 0.8 | 7.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.4 | 0.4 | 0.5 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 2.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.1 | 1.7 | 1.9 | 1.9 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 1500 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.4 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 11716 calls, mean 0.01 ms, local 18203 calls, mean 0.51 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/modulith-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async outbox)

1500 orders in 3.3 s = 451.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 17.0 | 35.2 | 33.0 | 43.7 | 47.6 | 85.2 | 91.2 |
| placeOrder | declined | 27 | 25.7 | 34.1 | 31.0 | 39.0 | 53.6 | 89.3 | 89.3 |

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
| orders -> inventory.reserve [local] | 1500 |  | 1.2 | 1.1 | 1.8 | 2.5 | 7.6 |
| orders -> shipping.createShipment [local] | 1473 |  | 1.1 | 1.1 | 1.6 | 2.0 | 9.0 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.7 | 0.7 | 1.0 | 1.3 | 2.1 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.3 | 2.0 | 55.0 |
| orders -> notifications.send [local] | 1473 |  | 0.6 | 0.5 | 1.0 | 1.5 | 2.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.6 | 54.3 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.6 | 0.8 | 4.5 |
| orders -> pricing.quote [local] | 1500 |  | 0.4 | 0.4 | 0.8 | 1.2 | 7.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.4 | 0.5 | 4.4 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 1.0 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.4 | 6.9 |
| notifications -> customers.getCustomer [local] | 786 |  | 0.2 | 0.1 | 0.4 | 0.5 | 0.9 |
| orders -> inventory.release [local] | 27 |  | 1.2 | 1.2 | 1.8 | 1.8 | 1.8 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 687 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8993 calls, mean 0.01 ms, local 20926 calls, mean 0.48 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/modulith-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### modulith (cache on, parallel quotes on, async events)

1500 orders in 3.4 s = 439.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 18.8 | 36.2 | 34.4 | 45.5 | 48.5 | 57.9 | 65.5 |
| placeOrder | declined | 27 | 27.8 | 32.9 | 31.9 | 35.2 | 38.4 | 58.1 | 58.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4540 | 61.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [local] | 1473 |  | 1.2 | 1.1 | 1.6 | 2.1 | 12.5 |
| orders -> inventory.reserve [local] | 1500 |  | 1.1 | 1.0 | 1.8 | 2.5 | 12.6 |
| orders -> payments.authorize [local] | 1500 | 27 | 0.8 | 0.8 | 1.1 | 1.3 | 7.8 |
| orders -> shipping.quote [local] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.2 | 7.8 |
| orders -> notifications.send [local] | 1473 |  | 0.7 | 0.7 | 1.0 | 1.4 | 6.6 |
| orders -> payments.capture [local] | 1473 |  | 0.5 | 0.5 | 0.7 | 0.8 | 6.3 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 6.0 |
| orders -> pricing.quote [local] | 1500 |  | 0.5 | 0.4 | 0.9 | 1.6 | 7.6 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.2 | 0.4 | 0.4 | 7.4 |
| orders -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.6 | 7.4 |
| shipping -> customers.getCustomer [local] | 968 |  | 0.2 | 0.2 | 0.3 | 0.6 | 7.5 |
| pricing -> customers.getCustomer [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.5 | 7.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.5 | 5.7 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.3 | 0.5 | 5.8 |
| orders -> inventory.release [local] | 27 |  | 1.1 | 1.0 | 1.6 | 1.8 | 1.8 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.5 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 9779 calls, mean 0.01 ms, local 20140 calls, mean 0.52 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/modulith-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async outbox)

1500 orders in 6.5 s = 229.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 37.1 | 69.3 | 66.3 | 86.2 | 93.2 | 140.9 | 164.1 |
| placeOrder | declined | 27 | 46.7 | 65.8 | 62.8 | 82.1 | 84.9 | 146.4 | 146.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4748 | 63.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.7 | 2.8 | 3.7 | 12.6 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.8 | 1.7 | 2.8 | 3.7 | 9.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.8 | 3.7 | 12.1 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.0 | 4.0 | 51.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.8 | 2.2 | 3.1 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 1.0 | 2.1 | 2.9 | 3.5 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.7 | 3.2 | 4.1 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.8 | 2.3 | 4.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.8 | 1.7 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.7 | 1.4 | 2.2 | 50.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 0.9 |
| notifications -> customers.getCustomer [remote] | 760 |  | 0.6 | 0.5 | 1.4 | 2.2 | 2.7 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.7 | 2.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.3 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 0.8 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 2.9 | 3.3 | 3.3 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.3 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 713 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9987 calls, mean 0.01 ms, local 8261 calls, mean 0.41 ms, remote 11671 calls, mean 1.39 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/groups-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async events)

1500 orders in 6.6 s = 227.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.8 | 70.0 | 65.9 | 93.7 | 98.5 | 108.3 | 124.2 |
| placeOrder | declined | 27 | 45.6 | 66.1 | 61.6 | 87.5 | 89.7 | 104.8 | 104.8 |

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
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.0 | 3.9 | 7.2 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.0 | 3.9 | 12.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.8 | 3.6 | 4.8 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.3 | 4.2 | 5.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.8 | 2.3 | 7.4 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 1.0 | 2.2 | 2.8 | 4.9 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.7 | 11.9 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.9 | 2.6 | 4.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.6 | 0.8 | 3.1 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.4 | 2.2 | 8.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 1.1 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.8 | 1.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.5 | 1.3 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.1 |
| orders -> inventory.release [remote] | 27 |  | 1.7 | 1.6 | 2.5 | 2.5 | 2.5 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.4 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10747 calls, mean 0.01 ms, local 8261 calls, mean 0.41 ms, remote 10911 calls, mean 1.48 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/groups-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox)

1500 orders in 5.9 s = 253.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 33.3 | 62.9 | 60.4 | 82.4 | 87.8 | 96.0 | 112.7 |
| placeOrder | declined | 27 | 41.5 | 61.6 | 61.1 | 83.7 | 88.7 | 91.1 | 91.1 |

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
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.0 | 3.7 | 4.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.7 | 3.0 | 4.1 | 9.1 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.4 | 4.5 | 8.7 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.9 | 4.0 | 12.3 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 2.9 | 3.8 | 8.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.6 | 4.8 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.6 | 3.0 | 4.0 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.6 | 0.9 | 1.6 |
| shipping -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.5 | 2.2 | 4.9 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.5 | 2.2 | 5.0 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.5 | 2.2 | 5.2 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 0.9 |
| notifications -> customers.getCustomer [remote] | 767 |  | 0.6 | 0.5 | 1.4 | 2.0 | 2.6 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.8 | 1.1 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.2 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.2 | 3.0 | 3.2 | 3.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 706 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9012 calls, mean 0.01 ms, local 8261 calls, mean 0.42 ms, remote 12646 calls, mean 1.45 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/groups-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

1500 orders in 6.2 s = 243.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 29.8 | 65.4 | 63.2 | 85.9 | 91.4 | 105.2 | 122.0 |
| placeOrder | declined | 27 | 43.5 | 57.9 | 57.4 | 71.6 | 72.8 | 78.2 | 78.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4540 | 61.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 3.1 | 4.0 | 8.2 |
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.8 | 5.2 | 6.9 |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.8 | 3.2 | 4.2 | 12.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 3.0 | 3.8 | 12.5 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 3.1 | 4.4 | 6.5 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 2.1 | 2.8 | 7.9 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 0.9 | 1.5 | 1.9 | 2.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.1 | 2.6 |
| shipping -> customers.getCustomer [remote] | 968 |  | 1.0 | 0.9 | 1.7 | 2.3 | 4.2 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.3 | 3.4 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.6 | 2.2 | 4.9 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.8 | 3.0 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.7 | 4.2 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.8 | 1.6 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 2.5 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 2.0 | 2.9 | 3.6 | 3.6 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.7 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.3 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2005 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9779 calls, mean 0.01 ms, local 8261 calls, mean 0.45 ms, remote 11879 calls, mean 1.58 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/groups-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async outbox)

1500 orders in 10.8 s = 138.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 47.9 | 115.3 | 110.1 | 155.2 | 169.0 | 217.5 | 245.9 |
| placeOrder | declined | 27 | 65.6 | 107.1 | 104.9 | 138.6 | 139.1 | 164.7 | 164.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3414 | 45.9% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.2 | 3.7 | 8.9 | 12.2 | 51.9 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.4 | 3.2 | 5.5 | 7.1 | 50.2 |
| orders -> pricing.quote [remote] | 1500 |  | 3.2 | 2.7 | 6.5 | 8.3 | 50.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.9 | 2.7 | 5.1 | 6.5 | 10.9 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 1.0 | 3.5 | 4.9 | 49.1 |
| orders -> notifications.send [remote] | 1473 |  | 2.4 | 2.1 | 4.9 | 6.5 | 8.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.1 | 3.6 | 4.9 | 50.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.3 | 2.4 | 3.0 | 6.8 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.5 | 47.3 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.4 | 1.3 | 2.4 | 3.4 | 8.2 |
| inventory -> catalog.productExists [remote] | 673 |  | 1.9 | 1.7 | 3.1 | 3.8 | 47.3 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.0 | 2.8 | 7.0 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.7 | 1.6 | 2.8 | 3.5 | 7.1 |
| notifications -> customers.getCustomer [remote] | 1116 |  | 1.0 | 0.9 | 1.9 | 2.6 | 3.3 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.0 | 0.9 | 1.8 | 2.4 | 9.0 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.0 | 0.9 | 1.8 | 2.5 | 3.8 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.2 | 3.4 | 3.8 | 3.8 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 357 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8632 calls, mean 0.01 ms, remote 21287 calls, mean 2.06 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/microservices-parallel-off-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async events)

1500 orders in 11.3 s = 132.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 56.3 | 120.2 | 112.5 | 163.8 | 178.9 | 213.8 | 251.6 |
| placeOrder | declined | 27 | 74.4 | 113.5 | 108.1 | 153.9 | 157.8 | 188.0 | 188.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2346 | 77.7% |
| catalog.productExists | 3018 | 2346 | 77.7% |
| customers.getCustomer | 7446 | 3581 | 48.1% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.4 | 3.8 | 9.2 | 12.6 | 56.1 |
| orders -> pricing.quote [remote] | 1500 |  | 3.5 | 3.1 | 7.3 | 9.6 | 14.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.4 | 6.9 | 47.7 |
| orders -> notifications.send [remote] | 1473 |  | 3.3 | 3.1 | 6.0 | 7.8 | 10.7 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.3 | 6.5 | 9.8 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 1.0 | 3.5 | 4.9 | 49.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.6 | 4.9 | 7.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.0 | 4.9 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 1.9 | 2.8 | 43.1 |
| notifications -> customers.getCustomer [remote] | 952 |  | 1.5 | 1.4 | 2.6 | 3.6 | 8.3 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.6 | 3.2 | 8.1 |
| inventory -> catalog.productExists [remote] | 672 |  | 1.9 | 1.7 | 3.1 | 4.5 | 46.8 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.2 | 2.7 | 5.8 |
| pricing -> catalog.getProduct [remote] | 672 |  | 1.9 | 1.7 | 3.2 | 4.1 | 8.1 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 2.0 | 2.9 | 4.7 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.9 | 2.6 | 5.5 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.0 | 3.6 | 4.6 | 4.6 |
| inventory -> catalog.productExists [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 521 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8802 calls, mean 0.01 ms, remote 21117 calls, mean 2.24 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/microservices-parallel-off-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox)

1500 orders in 9.4 s = 159.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 37.3 | 99.8 | 96.0 | 133.1 | 143.9 | 170.1 | 190.1 |
| placeOrder | declined | 27 | 59.3 | 92.5 | 85.8 | 124.0 | 132.3 | 133.5 | 133.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2347 | 77.8% |
| catalog.productExists | 3018 | 2347 | 77.8% |
| customers.getCustomer | 7446 | 3419 | 45.9% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.6 | 4.0 | 9.8 | 13.7 | 55.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.6 | 3.3 | 5.8 | 7.2 | 45.2 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.1 | 7.0 | 9.6 | 14.5 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.9 | 5.3 | 50.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.9 | 5.3 | 6.4 | 10.8 |
| orders -> notifications.send [remote] | 1473 |  | 2.4 | 2.2 | 5.1 | 6.6 | 9.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.8 | 4.7 | 9.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.5 | 3.3 | 5.2 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 2.0 | 2.7 | 41.1 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.5 | 3.3 | 8.3 |
| inventory -> catalog.productExists [remote] | 671 |  | 2.1 | 1.9 | 3.5 | 4.8 | 48.0 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.4 | 1.3 | 2.3 | 3.0 | 6.6 |
| pricing -> catalog.getProduct [remote] | 671 |  | 1.9 | 1.7 | 3.1 | 4.4 | 9.2 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.3 | 3.5 | 7.8 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 0.9 | 2.0 | 2.6 | 6.4 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.0 | 1.9 | 2.7 | 9.1 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 2.9 | 3.1 | 3.1 |
| inventory -> catalog.productExists [cache] | 2347 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.5 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2347 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8642 calls, mean 0.01 ms, remote 21277 calls, mean 2.18 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/microservices-parallel-on-async-outbox` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

1500 orders in 10.0 s = 150.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 37.9 | 105.9 | 102.5 | 140.7 | 151.8 | 173.8 | 202.6 |
| placeOrder | declined | 27 | 65.2 | 98.2 | 96.0 | 128.1 | 129.4 | 151.6 | 151.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2343 | 77.6% |
| catalog.productExists | 3018 | 2343 | 77.6% |
| customers.getCustomer | 7446 | 3581 | 48.1% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.0 | 4.3 | 10.6 | 14.2 | 60.7 |
| orders -> pricing.quote [remote] | 1500 |  | 3.8 | 3.4 | 7.9 | 10.6 | 17.4 |
| orders -> notifications.send [remote] | 1473 |  | 3.7 | 3.5 | 6.7 | 8.6 | 13.9 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.7 | 3.4 | 5.7 | 7.3 | 43.6 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.6 | 1.1 | 4.1 | 5.9 | 50.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.2 | 3.0 | 5.8 | 7.3 | 14.0 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 4.0 | 5.1 | 50.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.2 | 4.4 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 2.0 | 2.6 | 40.7 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.6 | 1.5 | 2.8 | 3.9 | 11.1 |
| notifications -> customers.getCustomer [remote] | 952 |  | 1.7 | 1.5 | 2.7 | 3.6 | 10.3 |
| inventory -> catalog.productExists [remote] | 675 |  | 2.3 | 2.0 | 3.8 | 5.4 | 47.3 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.5 | 1.4 | 2.5 | 3.2 | 11.1 |
| pricing -> catalog.getProduct [remote] | 675 |  | 2.0 | 1.9 | 3.5 | 4.8 | 5.8 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.1 | 2.3 | 3.1 | 6.7 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.2 | 2.9 | 6.8 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 3.7 | 3.9 | 3.9 |
| inventory -> catalog.productExists [cache] | 2343 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2343 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 521 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8796 calls, mean 0.01 ms, remote 21123 calls, mean 2.46 ms. 19.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261026T185537Z/microservices-parallel-on-async-events` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
