# Performance run 20261024T181223Z

- Started: 2026-10-24T18:12:23+00:00
- Commit: 243b501 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 250 orders, 4 concurrent clients (closed loop, persistent connections), 30 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 150 lines per order, 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes groups,microservices --wire all --async events --lines 150 --orders 250 --warmup 30 --concurrency 4`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups wire json | 25 | 244 | 6 | 86.1 | 158.1 | 149.5 | 201.7 | 210.0 | 223.9 | 291.9 | 463.9 | PASS |
| groups wire gzip | 25 | 244 | 6 | 108.5 | 157.9 | 149.0 | 203.3 | 217.1 | 250.1 | 302.3 | 463.9 | PASS |
| groups wire bson | 26 | 244 | 6 | 116.5 | 153.5 | 146.4 | 189.9 | 200.5 | 218.3 | 281.7 | 463.9 | PASS |
| groups wire bson-gzip | 25 | 244 | 6 | 124.3 | 158.8 | 151.6 | 192.7 | 205.2 | 229.8 | 294.3 | 463.9 | PASS |
| microservices wire json | 17 | 244 | 6 | 172.6 | 241.1 | 221.1 | 295.4 | 307.5 | 399.9 | 481.1 | 463.9 | PASS |
| microservices wire gzip | 16 | 244 | 6 | 194.2 | 253.1 | 238.2 | 309.8 | 325.5 | 425.6 | 504.5 | 463.9 | PASS |
| microservices wire bson | 16 | 244 | 6 | 173.3 | 247.4 | 242.7 | 290.8 | 311.6 | 432.0 | 495.7 | 463.9 | PASS |
| microservices wire bson-gzip | 15 | 244 | 6 | 192.4 | 259.4 | 252.6 | 311.6 | 341.8 | 440.9 | 523.2 | 463.9 | PASS |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups wire json | 8.6 | 34,521 | 34,521 | 100% | 1906 to 1906 | 2123 to 2123 |
| groups wire gzip | 8.6 | 34,521 | 5,796 | 17% | 1906 to 289 | 2123 to 387 |
| groups wire bson | 8.6 | 34,516 | 41,043 | 119% | 1909 to 2406 | 2127 to 2393 |
| groups wire bson-gzip | 8.6 | 34,521 | 8,313 | 24% | 1906 to 455 | 2123 to 515 |
| microservices wire json | 168.0 | 68,833 | 68,833 | 100% | 182 to 182 | 228 to 228 |
| microservices wire gzip | 167.9 | 68,825 | 39,028 | 57% | 182 to 112 | 228 to 121 |
| microservices wire bson | 167.9 | 68,817 | 78,220 | 114% | 182 to 213 | 228 to 253 |
| microservices wire bson-gzip | 167.9 | 68,825 | 43,768 | 64% | 182 to 125 | 228 to 135 |

## Wire format effect (json to each alternative)

Higher orders/s and lower latencies are better; the last column is the bytes on the wire against plain JSON.

| mode (format) | orders/s json | alt | Δ | p50 json | alt | Δ | p95 json | alt | Δ | p99 json | alt | Δ | bytes vs json |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (gzip) | 25 | 25 | +0% | 149.5 | 149.0 | -0% | 210.0 | 217.1 | +3% | 223.9 | 250.1 | +12% | 17% |
| groups (bson) | 25 | 26 | +3% | 149.5 | 146.4 | -2% | 210.0 | 200.5 | -5% | 223.9 | 218.3 | -3% | 119% |
| groups (bson-gzip) | 25 | 25 | -0% | 149.5 | 151.6 | +1% | 210.0 | 205.2 | -2% | 223.9 | 229.8 | +3% | 24% |
| microservices (gzip) | 17 | 16 | -5% | 221.1 | 238.2 | +8% | 307.5 | 325.5 | +6% | 399.9 | 425.6 | +6% | 57% |
| microservices (bson) | 17 | 16 | -3% | 221.1 | 242.7 | +10% | 307.5 | 311.6 | +1% | 399.9 | 432.0 | +8% | 114% |
| microservices (bson-gzip) | 17 | 15 | -7% | 221.1 | 252.6 | +14% | 307.5 | 341.8 | +11% | 399.9 | 440.9 | +10% | 64% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 42.7 | 42.4 | 41.9 | 42.9 | 105.7 | 114.2 | 110.7 | 119.5 |
| shipping -> inventory.getStock | 0.2 | 0.2 | 0.2 | 0.2 | 0.7 | 0.7 | 0.7 | 0.7 |
| orders -> inventory.reserve | 42.3 | 42.0 | 40.1 | 42.5 | 37.8 | 37.6 | 37.7 | 36.9 |
| orders -> shipping.createShipment | 25.6 | 24.8 | 24.4 | 25.8 | 24.7 | 25.5 | 24.7 | 25.4 |
| orders -> pricing.quote | 6.3 | 6.7 | 7.1 | 7.1 | 8.4 | 8.8 | 9.1 | 9.8 |
| orders -> payments.authorize | 2.5 | 3.1 | 2.7 | 2.6 | 5.4 | 5.5 | 5.7 | 6.1 |
| orders -> notifications.send | 1.2 | 1.1 | 1.4 | 1.1 | 5.8 | 5.7 | 6.0 | 6.8 |
| inventory -> catalog.productExists | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> payments.capture | 1.6 | 2.3 | 2.2 | 1.7 | 2.5 | 2.6 | 2.7 | 2.8 |
| shipping -> inventory.getReservation | 1.2 | 1.2 | 1.2 | 1.2 | 2.0 | 2.0 | 2.1 | 2.3 |
| orders -> inventory.release | 70.9 | 62.4 | 56.3 | 68.2 | 54.7 | 57.7 | 58.7 | 60.7 |
| orders -> customers.getCustomer | 1.0 | 1.1 | 1.3 | 1.2 | 1.5 | 1.5 | 1.8 | 2.1 |
| pricing -> customers.getCustomer | 1.0 | 1.4 | 1.1 | 1.3 | 1.1 | 1.3 | 1.4 | 1.4 |
| shipping -> customers.getCustomer | 0.5 | 0.7 | 0.6 | 0.6 | 0.6 | 0.6 | 0.7 | 0.7 |
| payments -> customers.getPaymentMethod | 0.3 | 0.3 | 0.3 | 0.3 | 2.1 | 1.9 | 2.1 | 2.3 |
| notifications -> customers.getCustomer | 0.0 | 0.0 | 0.0 | 0.0 | 1.8 | 2.0 | 1.8 | 2.1 |
| (calls per order) | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 57.6 | 57.3 | 56.4 | 56.5 | 145.5 | 157.6 | 151.1 | 159.3 |
| shipping -> inventory.getStock | 0.3 | 0.3 | 0.3 | 0.3 | 0.9 | 0.9 | 0.9 | 1.0 |
| orders -> inventory.reserve | 66.3 | 70.9 | 67.0 | 63.6 | 53.5 | 50.8 | 52.0 | 50.8 |
| orders -> shipping.createShipment | 30.9 | 30.0 | 29.5 | 31.5 | 29.9 | 29.7 | 30.6 | 29.2 |
| orders -> pricing.quote | 12.0 | 10.7 | 13.3 | 13.4 | 20.0 | 21.6 | 22.3 | 23.4 |
| orders -> payments.authorize | 3.8 | 4.7 | 4.0 | 3.8 | 8.4 | 8.3 | 8.7 | 9.3 |
| orders -> notifications.send | 1.8 | 1.9 | 1.7 | 1.8 | 8.9 | 8.3 | 8.5 | 9.7 |
| inventory -> catalog.productExists | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> payments.capture | 2.8 | 3.3 | 3.1 | 2.9 | 3.4 | 3.7 | 3.9 | 4.0 |
| shipping -> inventory.getReservation | 1.7 | 1.8 | 1.8 | 1.8 | 2.6 | 2.7 | 2.7 | 3.0 |
| orders -> inventory.release | 95.2 | 72.2 | 72.1 | 74.4 | 74.9 | 66.9 | 87.6 | 83.4 |
| orders -> customers.getCustomer | 2.4 | 2.6 | 2.5 | 2.3 | 3.0 | 2.9 | 3.0 | 3.3 |
| pricing -> customers.getCustomer | 2.3 | 2.3 | 2.0 | 2.3 | 2.3 | 2.0 | 2.0 | 2.2 |
| shipping -> customers.getCustomer | 1.8 | 2.0 | 1.9 | 1.8 | 2.0 | 2.0 | 1.8 | 2.0 |
| payments -> customers.getPaymentMethod | 0.7 | 0.7 | 0.6 | 0.6 | 3.6 | 3.6 | 3.7 | 4.0 |
| notifications -> customers.getCustomer | 0.0 | 0.0 | 0.0 | 0.0 | 3.0 | 2.9 | 2.9 | 3.1 |
| (calls per order) | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 | 463.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 98.1% | 98.2% | 98.1% | 98.2% | 98.1% | 98.1% | 98.1% | 98.1% |
| catalog.productExists | 98.1% | 98.2% | 98.1% | 98.2% | 98.1% | 98.1% | 98.1% | 98.1% |
| customers.getCustomer | 47.7% | 47.7% | 48.0% | 47.7% | 31.0% | 30.7% | 30.7% | 30.7% |
| customers.getPaymentMethod | 13.6% | 13.6% | 14.0% | 13.6% | 14.0% | 13.6% | 13.6% | 13.6% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| groups wire json | 74232 calls, 0.01 ms | 39596 calls, 0.25 ms | - | - | 2142 calls, 14.61 ms |
| groups wire gzip | 74242 calls, 0.01 ms | 39586 calls, 0.25 ms | - | - | 2142 calls, 14.70 ms |
| groups wire bson | 74215 calls, 0.01 ms | 39617 calls, 0.25 ms | - | - | 2138 calls, 14.35 ms |
| groups wire bson-gzip | 74242 calls, 0.01 ms | 39586 calls, 0.25 ms | - | - | 2142 calls, 14.85 ms |
| microservices wire json | 73971 calls, 0.00 ms | - | - | - | 41999 calls, 1.82 ms |
| microservices wire gzip | 73987 calls, 0.00 ms | - | - | - | 41983 calls, 1.93 ms |
| microservices wire bson | 74000 calls, 0.00 ms | - | - | - | 41970 calls, 1.89 ms |
| microservices wire bson-gzip | 73988 calls, 0.00 ms | - | - | - | 41982 calls, 2.00 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### groups (cache on, parallel quotes on, async events)

250 orders in 9.9 s = 25.3 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 86.1 | 158.1 | 149.5 | 201.7 | 210.0 | 223.9 | 291.9 |
| placeOrder | declined | 6 | 112.8 | 126.4 | 118.8 | 152.5 | 152.5 | 152.5 | 152.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36804 | 98.1% |
| catalog.productExists | 37500 | 36804 | 98.1% |
| customers.getCustomer | 1238 | 590 | 47.7% |
| customers.getPaymentMethod | 250 | 34 | 13.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 42.7 | 40.7 | 57.6 | 66.0 | 103.7 |
| orders -> inventory.reserve [remote] | 250 |  | 42.3 | 38.7 | 66.3 | 89.3 | 111.5 |
| shipping -> inventory.getStock [local] | 37500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 63.0 |
| orders -> shipping.createShipment [remote] | 244 |  | 25.6 | 25.1 | 30.9 | 33.6 | 88.2 |
| orders -> pricing.quote [remote] | 250 |  | 6.3 | 5.4 | 12.0 | 17.0 | 19.3 |
| orders -> payments.authorize [remote] | 250 | 6 | 2.5 | 2.2 | 3.8 | 5.4 | 50.5 |
| inventory -> catalog.productExists [cache] | 36804 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.5 |
| pricing -> catalog.getProduct [cache] | 36804 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.2 |
| orders -> inventory.release [remote] | 6 |  | 70.9 | 65.3 | 95.2 | 95.2 | 95.2 |
| orders -> payments.capture [remote] | 244 |  | 1.6 | 1.5 | 2.8 | 3.1 | 3.8 |
| shipping -> inventory.getReservation [local] | 244 |  | 1.2 | 1.2 | 1.7 | 2.0 | 2.1 |
| orders -> notifications.send [local] | 244 |  | 1.2 | 1.1 | 1.8 | 2.8 | 3.5 |
| shipping -> customers.getCustomer [remote] | 216 |  | 1.2 | 1.1 | 2.4 | 3.1 | 5.0 |
| pricing -> customers.getCustomer [remote] | 216 |  | 1.2 | 1.1 | 2.4 | 3.6 | 6.0 |
| orders -> customers.getCustomer [remote] | 216 |  | 1.2 | 1.0 | 2.6 | 3.4 | 4.2 |
| inventory -> catalog.productExists [local] | 696 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.7 |
| pricing -> catalog.getProduct [local] | 696 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.9 |
| payments -> customers.getPaymentMethod [local] | 216 |  | 0.4 | 0.3 | 0.7 | 0.9 | 1.1 |
| notifications -> customers.getCustomer [cache] | 244 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.3 |
| payments -> customers.getPaymentMethod [cache] | 34 |  | 0.1 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 278 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 74232 calls, mean 0.01 ms, local 39596 calls, mean 0.25 ms, remote 2142 calls, mean 14.61 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/groups-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

250 orders in 9.9 s = 25.3 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 108.5 | 157.9 | 149.0 | 203.3 | 217.1 | 250.1 | 302.3 |
| placeOrder | declined | 6 | 113.8 | 126.6 | 124.8 | 137.6 | 137.6 | 137.6 | 137.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36809 | 98.2% |
| catalog.productExists | 37500 | 36809 | 98.2% |
| customers.getCustomer | 1238 | 590 | 47.7% |
| customers.getPaymentMethod | 250 | 34 | 13.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 42.4 | 40.5 | 57.3 | 90.6 | 108.4 |
| orders -> inventory.reserve [remote] | 250 |  | 42.0 | 38.7 | 70.9 | 91.0 | 113.8 |
| shipping -> inventory.getStock [local] | 37500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 68.1 |
| orders -> shipping.createShipment [remote] | 244 |  | 24.8 | 24.5 | 30.0 | 36.4 | 37.6 |
| orders -> pricing.quote [remote] | 250 |  | 6.7 | 5.7 | 10.7 | 18.9 | 72.8 |
| orders -> payments.authorize [remote] | 250 | 6 | 3.1 | 2.1 | 4.7 | 53.0 | 84.8 |
| inventory -> catalog.productExists [cache] | 36809 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.9 |
| orders -> payments.capture [remote] | 244 |  | 2.3 | 1.5 | 3.3 | 4.3 | 93.1 |
| pricing -> catalog.getProduct [cache] | 36809 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.6 |
| orders -> inventory.release [remote] | 6 |  | 62.4 | 64.5 | 72.2 | 72.2 | 72.2 |
| shipping -> customers.getCustomer [remote] | 216 |  | 1.6 | 1.2 | 2.5 | 3.4 | 68.1 |
| pricing -> customers.getCustomer [remote] | 216 |  | 1.6 | 1.1 | 2.5 | 3.1 | 68.2 |
| shipping -> inventory.getReservation [local] | 244 |  | 1.2 | 1.2 | 1.8 | 2.2 | 5.0 |
| orders -> notifications.send [local] | 244 |  | 1.1 | 1.1 | 1.9 | 2.5 | 3.8 |
| orders -> customers.getCustomer [remote] | 216 |  | 1.3 | 1.0 | 2.7 | 3.9 | 4.0 |
| inventory -> catalog.productExists [local] | 691 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.2 |
| pricing -> catalog.getProduct [local] | 691 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.7 |
| payments -> customers.getPaymentMethod [local] | 216 |  | 0.4 | 0.3 | 0.8 | 0.9 | 1.2 |
| notifications -> customers.getCustomer [cache] | 244 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 34 |  | 0.1 | 0.0 | 0.1 | 0.2 | 0.2 |
| shipping -> customers.getCustomer [cache] | 278 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 74242 calls, mean 0.01 ms, local 39586 calls, mean 0.25 ms, remote 2142 calls, mean 14.70 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/groups-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

250 orders in 9.6 s = 26.0 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 116.5 | 153.5 | 146.4 | 189.9 | 200.5 | 218.3 | 281.7 |
| placeOrder | declined | 6 | 106.6 | 120.0 | 116.8 | 134.5 | 134.5 | 134.5 | 134.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36793 | 98.1% |
| catalog.productExists | 37500 | 36793 | 98.1% |
| customers.getCustomer | 1238 | 594 | 48.0% |
| customers.getPaymentMethod | 250 | 35 | 14.0% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 41.9 | 40.3 | 56.4 | 65.6 | 94.8 |
| orders -> inventory.reserve [remote] | 250 |  | 40.1 | 36.9 | 67.0 | 92.2 | 104.9 |
| shipping -> inventory.getStock [local] | 37500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 6.9 |
| orders -> shipping.createShipment [remote] | 244 |  | 24.4 | 23.9 | 29.5 | 35.1 | 36.7 |
| orders -> pricing.quote [remote] | 250 |  | 7.1 | 6.2 | 13.3 | 17.7 | 58.9 |
| orders -> payments.authorize [remote] | 250 | 6 | 2.7 | 2.3 | 4.0 | 5.6 | 48.3 |
| inventory -> catalog.productExists [cache] | 36793 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.6 |
| orders -> payments.capture [remote] | 244 |  | 2.2 | 1.7 | 3.1 | 4.0 | 56.2 |
| pricing -> catalog.getProduct [cache] | 36793 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.6 |
| orders -> notifications.send [local] | 244 |  | 1.4 | 1.1 | 1.7 | 3.0 | 54.4 |
| orders -> inventory.release [remote] | 6 |  | 56.3 | 56.9 | 72.1 | 72.1 | 72.1 |
| orders -> customers.getCustomer [remote] | 215 |  | 1.5 | 1.2 | 2.7 | 3.5 | 52.3 |
| shipping -> inventory.getReservation [local] | 244 |  | 1.2 | 1.2 | 1.8 | 2.3 | 4.7 |
| shipping -> customers.getCustomer [remote] | 215 |  | 1.3 | 1.2 | 2.5 | 3.2 | 4.1 |
| pricing -> customers.getCustomer [remote] | 214 |  | 1.3 | 1.2 | 2.2 | 3.1 | 4.2 |
| inventory -> catalog.productExists [local] | 707 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.6 |
| pricing -> catalog.getProduct [local] | 707 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.9 |
| payments -> customers.getPaymentMethod [local] | 215 |  | 0.4 | 0.3 | 0.6 | 0.9 | 1.1 |
| notifications -> customers.getCustomer [cache] | 244 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 35 |  | 0.1 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 279 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 35 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 36 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 74215 calls, mean 0.01 ms, local 39617 calls, mean 0.25 ms, remote 2138 calls, mean 14.35 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/groups-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

250 orders in 9.9 s = 25.2 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 124.3 | 158.8 | 151.6 | 192.7 | 205.2 | 229.8 | 294.3 |
| placeOrder | declined | 6 | 120.8 | 127.4 | 124.0 | 145.7 | 145.7 | 145.7 | 145.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36809 | 98.2% |
| catalog.productExists | 37500 | 36809 | 98.2% |
| customers.getCustomer | 1238 | 590 | 47.7% |
| customers.getPaymentMethod | 250 | 34 | 13.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 42.9 | 41.2 | 56.5 | 86.8 | 99.0 |
| orders -> inventory.reserve [remote] | 250 |  | 42.5 | 39.8 | 63.6 | 94.7 | 108.9 |
| shipping -> inventory.getStock [local] | 37500 |  | 0.2 | 0.2 | 0.3 | 0.5 | 6.3 |
| orders -> shipping.createShipment [remote] | 244 |  | 25.8 | 25.2 | 31.5 | 36.4 | 79.7 |
| orders -> pricing.quote [remote] | 250 |  | 7.1 | 6.1 | 13.4 | 17.7 | 51.1 |
| orders -> payments.authorize [remote] | 250 | 6 | 2.6 | 2.1 | 3.8 | 5.4 | 47.7 |
| inventory -> catalog.productExists [cache] | 36809 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.6 |
| pricing -> catalog.getProduct [cache] | 36809 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.7 |
| orders -> inventory.release [remote] | 6 |  | 68.2 | 66.0 | 74.4 | 74.4 | 74.4 |
| orders -> payments.capture [remote] | 244 |  | 1.7 | 1.5 | 2.9 | 3.8 | 4.2 |
| shipping -> customers.getCustomer [remote] | 216 |  | 1.5 | 1.1 | 2.3 | 3.2 | 42.8 |
| pricing -> customers.getCustomer [remote] | 216 |  | 1.5 | 1.2 | 2.3 | 2.8 | 42.8 |
| orders -> customers.getCustomer [remote] | 216 |  | 1.4 | 1.1 | 2.5 | 3.5 | 44.0 |
| shipping -> inventory.getReservation [local] | 244 |  | 1.2 | 1.2 | 1.8 | 2.2 | 4.8 |
| orders -> notifications.send [local] | 244 |  | 1.1 | 1.1 | 1.8 | 2.2 | 3.1 |
| inventory -> catalog.productExists [local] | 691 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| pricing -> catalog.getProduct [local] | 691 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.9 |
| payments -> customers.getPaymentMethod [local] | 216 |  | 0.4 | 0.3 | 0.7 | 0.8 | 1.1 |
| notifications -> customers.getCustomer [cache] | 244 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 34 |  | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 278 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 74242 calls, mean 0.01 ms, local 39586 calls, mean 0.25 ms, remote 2142 calls, mean 14.85 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/groups-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

250 orders in 15.1 s = 16.5 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 172.6 | 241.1 | 221.1 | 295.4 | 307.5 | 399.9 | 481.1 |
| placeOrder | declined | 6 | 167.0 | 216.4 | 180.9 | 335.4 | 335.4 | 335.4 | 335.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36776 | 98.1% |
| catalog.productExists | 37500 | 36776 | 98.1% |
| customers.getCustomer | 1238 | 384 | 31.0% |
| customers.getPaymentMethod | 250 | 35 | 14.0% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 105.7 | 101.9 | 145.5 | 154.0 | 160.6 |
| shipping -> inventory.getStock [remote] | 37500 |  | 0.7 | 0.6 | 0.9 | 1.9 | 11.2 |
| orders -> inventory.reserve [remote] | 250 |  | 37.8 | 35.3 | 53.5 | 84.7 | 106.1 |
| orders -> shipping.createShipment [remote] | 244 |  | 24.7 | 24.5 | 29.9 | 35.7 | 36.8 |
| orders -> pricing.quote [remote] | 250 |  | 8.4 | 6.7 | 20.0 | 31.4 | 54.5 |
| orders -> notifications.send [remote] | 244 |  | 5.8 | 4.9 | 8.9 | 54.8 | 58.0 |
| orders -> payments.authorize [remote] | 250 | 6 | 5.4 | 4.7 | 8.4 | 30.4 | 60.7 |
| inventory -> catalog.productExists [remote] | 724 |  | 1.0 | 0.9 | 1.5 | 1.8 | 5.2 |
| pricing -> catalog.getProduct [remote] | 724 |  | 0.9 | 0.8 | 1.5 | 1.9 | 5.3 |
| orders -> payments.capture [remote] | 244 |  | 2.5 | 2.1 | 3.4 | 5.1 | 58.4 |
| payments -> customers.getPaymentMethod [remote] | 215 |  | 2.4 | 1.9 | 3.7 | 4.3 | 56.9 |
| shipping -> inventory.getReservation [remote] | 244 |  | 2.0 | 2.0 | 2.6 | 4.0 | 5.3 |
| notifications -> customers.getCustomer [remote] | 209 |  | 2.1 | 1.7 | 3.0 | 6.0 | 53.7 |
| orders -> customers.getCustomer [remote] | 215 |  | 1.7 | 1.3 | 3.1 | 4.3 | 54.5 |
| orders -> inventory.release [remote] | 6 |  | 54.7 | 46.7 | 74.9 | 74.9 | 74.9 |
| pricing -> customers.getCustomer [remote] | 215 |  | 1.3 | 1.2 | 2.4 | 3.2 | 4.1 |
| shipping -> customers.getCustomer [remote] | 215 |  | 1.2 | 1.1 | 2.3 | 2.9 | 4.4 |
| inventory -> catalog.productExists [cache] | 36776 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 36776 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 279 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| orders -> customers.getCustomer [cache] | 35 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 35 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 35 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 35 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 73971 calls, mean 0.00 ms, remote 41999 calls, mean 1.82 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/microservices-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

250 orders in 15.9 s = 15.7 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 194.2 | 253.1 | 238.2 | 309.8 | 325.5 | 425.6 | 504.5 |
| placeOrder | declined | 6 | 181.7 | 233.0 | 198.4 | 305.3 | 305.3 | 305.3 | 305.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36786 | 98.1% |
| catalog.productExists | 37500 | 36787 | 98.1% |
| customers.getCustomer | 1238 | 380 | 30.7% |
| customers.getPaymentMethod | 250 | 34 | 13.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 114.2 | 110.2 | 157.6 | 168.0 | 168.6 |
| shipping -> inventory.getStock [remote] | 37500 |  | 0.7 | 0.7 | 0.9 | 2.1 | 52.8 |
| orders -> inventory.reserve [remote] | 250 |  | 37.6 | 35.9 | 50.8 | 78.6 | 134.0 |
| orders -> shipping.createShipment [remote] | 244 |  | 25.5 | 25.0 | 29.7 | 34.3 | 78.2 |
| orders -> pricing.quote [remote] | 250 |  | 8.8 | 6.5 | 21.6 | 35.7 | 54.1 |
| orders -> notifications.send [remote] | 244 |  | 5.7 | 4.8 | 8.3 | 52.3 | 60.7 |
| orders -> payments.authorize [remote] | 250 | 6 | 5.5 | 4.6 | 8.3 | 39.4 | 59.8 |
| inventory -> catalog.productExists [remote] | 713 |  | 1.1 | 1.0 | 1.5 | 1.8 | 4.2 |
| pricing -> catalog.getProduct [remote] | 714 |  | 1.0 | 0.9 | 1.6 | 1.9 | 3.8 |
| orders -> payments.capture [remote] | 244 |  | 2.6 | 2.3 | 3.7 | 4.2 | 56.8 |
| shipping -> inventory.getReservation [remote] | 244 |  | 2.0 | 2.0 | 2.7 | 3.2 | 4.4 |
| notifications -> customers.getCustomer [remote] | 210 |  | 2.3 | 1.7 | 3.0 | 4.2 | 56.9 |
| payments -> customers.getPaymentMethod [remote] | 216 |  | 2.2 | 1.9 | 3.7 | 4.2 | 10.7 |
| orders -> customers.getCustomer [remote] | 216 |  | 1.8 | 1.3 | 3.0 | 3.9 | 53.9 |
| orders -> inventory.release [remote] | 6 |  | 57.7 | 58.3 | 66.9 | 66.9 | 66.9 |
| pricing -> customers.getCustomer [remote] | 216 |  | 1.5 | 1.2 | 2.1 | 2.6 | 44.7 |
| shipping -> customers.getCustomer [remote] | 216 |  | 1.3 | 1.1 | 2.3 | 2.7 | 3.5 |
| inventory -> catalog.productExists [cache] | 36787 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.4 |
| pricing -> catalog.getProduct [cache] | 36786 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 278 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| orders -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 73987 calls, mean 0.00 ms, remote 41983 calls, mean 1.93 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/microservices-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

250 orders in 15.6 s = 16.1 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 173.3 | 247.4 | 242.7 | 290.8 | 311.6 | 432.0 | 495.7 |
| placeOrder | declined | 6 | 177.6 | 248.6 | 236.7 | 359.4 | 359.4 | 359.4 | 359.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36793 | 98.1% |
| catalog.productExists | 37500 | 36793 | 98.1% |
| customers.getCustomer | 1238 | 380 | 30.7% |
| customers.getPaymentMethod | 250 | 34 | 13.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 110.7 | 107.3 | 151.1 | 162.6 | 168.1 |
| shipping -> inventory.getStock [remote] | 37500 |  | 0.7 | 0.6 | 0.9 | 2.0 | 10.2 |
| orders -> inventory.reserve [remote] | 250 |  | 37.7 | 36.1 | 52.0 | 74.4 | 101.4 |
| orders -> shipping.createShipment [remote] | 244 |  | 24.7 | 24.4 | 30.6 | 33.0 | 36.5 |
| orders -> pricing.quote [remote] | 250 |  | 9.1 | 7.4 | 22.3 | 32.2 | 57.3 |
| orders -> notifications.send [remote] | 244 |  | 6.0 | 5.2 | 8.5 | 52.4 | 57.0 |
| orders -> payments.authorize [remote] | 250 | 6 | 5.7 | 4.9 | 8.7 | 34.0 | 63.8 |
| inventory -> catalog.productExists [remote] | 707 |  | 1.0 | 1.0 | 1.6 | 1.8 | 3.4 |
| pricing -> catalog.getProduct [remote] | 707 |  | 1.0 | 0.9 | 1.6 | 2.0 | 3.4 |
| orders -> payments.capture [remote] | 244 |  | 2.7 | 2.2 | 3.9 | 4.7 | 59.2 |
| payments -> customers.getPaymentMethod [remote] | 216 |  | 2.4 | 2.0 | 3.8 | 4.5 | 51.3 |
| shipping -> inventory.getReservation [remote] | 244 |  | 2.1 | 2.0 | 2.7 | 3.4 | 4.5 |
| orders -> customers.getCustomer [remote] | 216 |  | 2.1 | 1.6 | 3.2 | 4.7 | 52.8 |
| notifications -> customers.getCustomer [remote] | 210 |  | 2.1 | 1.7 | 3.0 | 4.0 | 48.8 |
| orders -> inventory.release [remote] | 6 |  | 58.7 | 54.9 | 87.6 | 87.6 | 87.6 |
| pricing -> customers.getCustomer [remote] | 216 |  | 1.6 | 1.2 | 2.1 | 3.4 | 47.3 |
| shipping -> customers.getCustomer [remote] | 216 |  | 1.5 | 1.1 | 2.4 | 3.1 | 47.2 |
| inventory -> catalog.productExists [cache] | 36793 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 36793 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 278 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 74000 calls, mean 0.00 ms, remote 41970 calls, mean 1.89 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/microservices-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

250 orders in 16.3 s = 15.3 orders/s; 244 ok, 6 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 244 | 192.4 | 259.4 | 252.6 | 311.6 | 341.8 | 440.9 | 523.2 |
| placeOrder | declined | 6 | 184.1 | 274.7 | 285.7 | 357.8 | 357.8 | 357.8 | 357.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 37500 | 36787 | 98.1% |
| catalog.productExists | 37500 | 36787 | 98.1% |
| customers.getCustomer | 1238 | 380 | 30.7% |
| customers.getPaymentMethod | 250 | 34 | 13.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 250 |  | 119.5 | 114.8 | 159.3 | 173.0 | 222.0 |
| shipping -> inventory.getStock [remote] | 37500 |  | 0.7 | 0.7 | 1.0 | 2.1 | 7.8 |
| orders -> inventory.reserve [remote] | 250 |  | 36.9 | 35.2 | 50.8 | 72.1 | 95.5 |
| orders -> shipping.createShipment [remote] | 244 |  | 25.4 | 25.2 | 29.2 | 34.7 | 38.5 |
| orders -> pricing.quote [remote] | 250 |  | 9.8 | 7.4 | 23.4 | 56.9 | 64.2 |
| orders -> notifications.send [remote] | 244 |  | 6.8 | 5.7 | 9.7 | 54.5 | 63.3 |
| orders -> payments.authorize [remote] | 250 | 6 | 6.1 | 5.0 | 9.3 | 54.0 | 64.1 |
| inventory -> catalog.productExists [remote] | 713 |  | 1.1 | 1.0 | 1.6 | 1.8 | 4.2 |
| pricing -> catalog.getProduct [remote] | 713 |  | 1.0 | 0.9 | 1.7 | 2.0 | 3.5 |
| orders -> payments.capture [remote] | 244 |  | 2.8 | 2.4 | 4.0 | 5.1 | 60.3 |
| payments -> customers.getPaymentMethod [remote] | 216 |  | 2.7 | 2.0 | 4.1 | 4.8 | 57.7 |
| shipping -> inventory.getReservation [remote] | 244 |  | 2.3 | 2.2 | 3.0 | 3.9 | 5.0 |
| notifications -> customers.getCustomer [remote] | 210 |  | 2.5 | 1.9 | 3.2 | 4.6 | 58.0 |
| orders -> customers.getCustomer [remote] | 216 |  | 2.4 | 1.5 | 3.4 | 49.6 | 56.7 |
| orders -> inventory.release [remote] | 6 |  | 60.7 | 58.7 | 83.4 | 83.4 | 83.4 |
| pricing -> customers.getCustomer [remote] | 216 |  | 1.6 | 1.3 | 2.3 | 3.1 | 48.1 |
| shipping -> customers.getCustomer [remote] | 216 |  | 1.5 | 1.2 | 2.5 | 2.7 | 48.0 |
| inventory -> catalog.productExists [cache] | 36787 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 36787 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.8 |
| shipping -> customers.getCustomer [cache] | 278 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 34 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 73988 calls, mean 0.00 ms, remote 41982 calls, mean 2.00 ms. 463.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T181223Z/microservices-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
