# Performance run 20261024T180026Z

- Started: 2026-10-24T18:00:26+00:00
- Commit: 243b501 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1200 orders, 4 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-http,groups,microservices --wire all --async events --orders 1200 --warmup 200 --concurrency 4`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http wire json | 208 | 1179 | 21 | 9.8 | 19.3 | 18.7 | 25.6 | 27.3 | 31.4 | 36.8 | 20.0 | PASS |
| loopback-http wire gzip | 219 | 1179 | 21 | 9.8 | 18.3 | 17.7 | 24.3 | 26.0 | 30.2 | 38.0 | 20.0 | PASS |
| loopback-http wire bson | 196 | 1179 | 21 | 10.7 | 20.4 | 20.1 | 26.4 | 28.3 | 31.6 | 35.0 | 20.0 | PASS |
| loopback-http wire bson-gzip | 208 | 1179 | 21 | 9.8 | 19.2 | 18.6 | 25.5 | 27.8 | 31.2 | 38.9 | 20.0 | PASS |
| groups wire json | 225 | 1179 | 21 | 10.8 | 17.8 | 17.4 | 23.5 | 25.1 | 27.7 | 37.5 | 20.0 | PASS |
| groups wire gzip | 219 | 1179 | 21 | 9.7 | 18.3 | 17.4 | 24.9 | 26.7 | 31.5 | 39.4 | 20.0 | PASS |
| groups wire bson | 218 | 1179 | 21 | 9.9 | 18.3 | 17.8 | 24.2 | 26.7 | 30.1 | 35.2 | 20.0 | PASS |
| groups wire bson-gzip | 225 | 1179 | 21 | 9.9 | 17.8 | 16.5 | 24.5 | 27.3 | 31.3 | 95.9 | 20.0 | PASS |
| microservices wire json | 164 | 1179 | 21 | 12.9 | 24.4 | 23.1 | 32.4 | 35.4 | 62.7 | 91.6 | 20.0 | PASS |
| microservices wire gzip | 151 | 1179 | 21 | 14.0 | 26.4 | 25.1 | 34.4 | 38.3 | 77.7 | 113.5 | 20.0 | PASS |
| microservices wire bson | 155 | 1179 | 21 | 12.2 | 25.8 | 24.0 | 34.4 | 37.8 | 84.1 | 107.1 | 20.0 | PASS |
| microservices wire bson-gzip | 151 | 1179 | 21 | 13.8 | 26.6 | 24.9 | 35.5 | 38.7 | 73.8 | 103.9 | 20.0 | PASS |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http wire json | 13.7 | 4,340 | 4,340 | 100% | 147 to 147 | 171 to 171 |
| loopback-http wire gzip | 13.7 | 4,340 | 3,880 | 89% | 147 to 134 | 171 to 151 |
| loopback-http wire bson | 13.7 | 4,340 | 4,654 | 107% | 147 to 159 | 171 to 182 |
| loopback-http wire bson-gzip | 13.7 | 4,339 | 4,235 | 98% | 147 to 147 | 171 to 163 |
| groups wire json | 8.0 | 2,911 | 2,911 | 100% | 163 to 163 | 200 to 200 |
| groups wire gzip | 8.0 | 2,910 | 2,476 | 85% | 163 to 141 | 200 to 168 |
| groups wire bson | 8.0 | 2,911 | 3,126 | 107% | 163 to 178 | 200 to 213 |
| groups wire bson-gzip | 8.0 | 2,910 | 2,726 | 94% | 163 to 157 | 200 to 184 |
| microservices wire json | 14.3 | 4,533 | 4,533 | 100% | 144 to 144 | 172 to 172 |
| microservices wire gzip | 14.3 | 4,533 | 4,054 | 89% | 144 to 132 | 172 to 151 |
| microservices wire bson | 14.3 | 4,533 | 4,870 | 107% | 144 to 156 | 172 to 184 |
| microservices wire bson-gzip | 14.3 | 4,533 | 4,433 | 98% | 144 to 145 | 172 to 165 |

## Wire format effect (json to each alternative)

Higher orders/s and lower latencies are better; the last column is the bytes on the wire against plain JSON.

| mode (format) | orders/s json | alt | Δ | p50 json | alt | Δ | p95 json | alt | Δ | p99 json | alt | Δ | bytes vs json |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (gzip) | 208 | 219 | +5% | 18.7 | 17.7 | -5% | 27.3 | 26.0 | -5% | 31.4 | 30.2 | -4% | 89% |
| loopback-http (bson) | 208 | 196 | -5% | 18.7 | 20.1 | +7% | 27.3 | 28.3 | +4% | 31.4 | 31.6 | +1% | 107% |
| loopback-http (bson-gzip) | 208 | 208 | +0% | 18.7 | 18.6 | -1% | 27.3 | 27.8 | +2% | 31.4 | 31.2 | -1% | 98% |
| groups (gzip) | 225 | 219 | -3% | 17.4 | 17.4 | +0% | 25.1 | 26.7 | +7% | 27.7 | 31.5 | +14% | 85% |
| groups (bson) | 225 | 218 | -3% | 17.4 | 17.8 | +3% | 25.1 | 26.7 | +6% | 27.7 | 30.1 | +9% | 107% |
| groups (bson-gzip) | 225 | 225 | -0% | 17.4 | 16.5 | -5% | 25.1 | 27.3 | +9% | 27.7 | 31.3 | +13% | 94% |
| microservices (gzip) | 164 | 151 | -8% | 23.1 | 25.1 | +9% | 35.4 | 38.3 | +8% | 62.7 | 77.7 | +24% | 89% |
| microservices (bson) | 164 | 155 | -6% | 23.1 | 24.0 | +4% | 35.4 | 37.8 | +7% | 62.7 | 84.1 | +34% | 107% |
| microservices (bson-gzip) | 164 | 151 | -8% | 23.1 | 24.9 | +8% | 35.4 | 38.7 | +9% | 62.7 | 73.8 | +18% | 98% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http wire json | loopback-http wire gzip | loopback-http wire bson | loopback-http wire bson-gzip | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 3.4 | 3.3 | 3.6 | 3.5 | 2.3 | 2.4 | 2.4 | 2.4 | 4.4 | 5.1 | 4.8 | 5.0 |
| orders -> shipping.createShipment | 2.7 | 2.6 | 2.9 | 2.7 | 2.3 | 2.4 | 2.4 | 2.3 | 3.4 | 3.6 | 3.8 | 3.6 |
| orders -> pricing.quote | 2.0 | 2.0 | 2.2 | 2.1 | 2.0 | 2.1 | 2.1 | 2.1 | 2.9 | 3.3 | 3.2 | 3.4 |
| orders -> inventory.reserve | 2.0 | 1.9 | 2.2 | 2.0 | 2.1 | 2.2 | 2.2 | 2.2 | 2.4 | 2.6 | 2.4 | 2.5 |
| orders -> payments.authorize | 1.9 | 1.8 | 2.0 | 1.9 | 1.7 | 1.8 | 1.8 | 1.8 | 2.7 | 2.9 | 2.7 | 2.9 |
| shipping -> inventory.getStock | 1.1 | 1.0 | 1.1 | 1.1 | 0.4 | 0.4 | 0.4 | 0.4 | 1.4 | 1.6 | 1.5 | 1.6 |
| orders -> notifications.send | 1.5 | 1.4 | 1.6 | 1.5 | 1.0 | 1.0 | 1.0 | 1.1 | 3.5 | 3.5 | 3.2 | 3.8 |
| orders -> payments.capture | 1.2 | 1.1 | 1.2 | 1.2 | 1.3 | 1.3 | 1.3 | 1.3 | 1.4 | 1.5 | 1.6 | 1.7 |
| shipping -> inventory.getReservation | 0.9 | 0.9 | 1.0 | 0.9 | 0.4 | 0.4 | 0.4 | 0.4 | 1.1 | 1.2 | 1.3 | 1.2 |
| orders -> customers.getCustomer | 0.6 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.8 | 0.9 | 0.8 | 0.9 |
| pricing -> customers.getCustomer | 0.6 | 0.6 | 0.6 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.9 | 0.8 | 0.9 |
| shipping -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| payments -> customers.getPaymentMethod | 0.6 | 0.5 | 0.6 | 0.6 | 0.2 | 0.2 | 0.2 | 0.2 | 0.8 | 1.0 | 0.9 | 0.9 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 | 0.3 | 0.4 | 0.4 | 0.4 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 | 0.3 | 0.4 | 0.3 | 0.4 |
| notifications -> customers.getCustomer | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.9 | 0.9 | 0.9 | 0.9 |
| orders -> inventory.release | 2.0 | 1.9 | 2.5 | 2.4 | 2.4 | 2.3 | 2.5 | 2.1 | 2.4 | 2.4 | 2.5 | 2.5 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http wire json | loopback-http wire gzip | loopback-http wire bson | loopback-http wire bson-gzip | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 6.2 | 6.4 | 6.5 | 6.6 | 3.6 | 3.8 | 3.8 | 3.7 | 7.8 | 8.7 | 8.2 | 8.7 |
| orders -> shipping.createShipment | 3.4 | 3.4 | 3.5 | 3.4 | 2.9 | 3.0 | 3.0 | 3.0 | 4.5 | 4.6 | 4.7 | 4.6 |
| orders -> pricing.quote | 3.5 | 3.9 | 3.7 | 3.9 | 2.8 | 3.0 | 2.9 | 3.1 | 4.9 | 5.8 | 5.3 | 5.9 |
| orders -> inventory.reserve | 2.8 | 2.6 | 3.0 | 2.8 | 2.9 | 3.1 | 3.1 | 2.9 | 3.3 | 3.4 | 3.3 | 3.4 |
| orders -> payments.authorize | 2.4 | 2.5 | 2.5 | 2.6 | 2.2 | 2.4 | 2.3 | 2.4 | 3.8 | 4.1 | 4.0 | 4.1 |
| shipping -> inventory.getStock | 1.8 | 1.8 | 1.8 | 1.9 | 0.8 | 0.8 | 0.8 | 0.7 | 2.7 | 3.0 | 2.8 | 3.0 |
| orders -> notifications.send | 1.7 | 1.8 | 1.8 | 1.8 | 1.2 | 1.3 | 1.2 | 1.2 | 5.0 | 5.0 | 4.8 | 5.2 |
| orders -> payments.capture | 1.4 | 1.3 | 1.4 | 1.4 | 1.5 | 1.6 | 1.5 | 1.6 | 1.8 | 2.0 | 1.9 | 2.0 |
| shipping -> inventory.getReservation | 1.0 | 1.1 | 1.1 | 1.1 | 0.5 | 0.5 | 0.5 | 0.5 | 1.4 | 1.6 | 1.5 | 1.6 |
| orders -> customers.getCustomer | 1.1 | 1.1 | 1.1 | 1.1 | 1.1 | 1.3 | 1.2 | 1.3 | 1.5 | 1.6 | 1.5 | 1.6 |
| pricing -> customers.getCustomer | 1.0 | 1.0 | 1.0 | 1.0 | 1.1 | 1.2 | 1.2 | 1.2 | 1.4 | 1.5 | 1.5 | 1.6 |
| shipping -> customers.getCustomer | 0.9 | 0.9 | 0.9 | 1.0 | 1.1 | 1.2 | 1.1 | 1.2 | 1.2 | 1.4 | 1.3 | 1.4 |
| payments -> customers.getPaymentMethod | 0.9 | 0.9 | 1.0 | 1.0 | 0.4 | 0.4 | 0.4 | 0.4 | 1.5 | 1.8 | 1.8 | 1.8 |
| inventory -> catalog.productExists | 0.9 | 0.8 | 0.9 | 0.9 | 0.4 | 0.4 | 0.4 | 0.4 | 1.5 | 1.7 | 1.6 | 1.7 |
| pricing -> catalog.getProduct | 0.9 | 0.9 | 1.0 | 1.0 | 0.4 | 0.4 | 0.4 | 0.3 | 1.5 | 1.7 | 1.7 | 1.7 |
| notifications -> customers.getCustomer | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 1.6 | 1.7 | 1.7 | 1.8 |
| orders -> inventory.release | 2.9 | 2.8 | 6.2 | 5.3 | 4.6 | 3.2 | 3.4 | 3.5 | 3.8 | 3.5 | 3.5 | 3.9 |
| (calls per order) | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 | 20.0 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-http wire json | loopback-http wire gzip | loopback-http wire bson | loopback-http wire bson-gzip | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.7% | 75.7% | 75.7% | 75.7% |
| catalog.productExists | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.8% | 75.7% | 75.7% | 75.7% | 75.7% |
| customers.getCustomer | 59.3% | 59.3% | 59.3% | 59.3% | 59.3% | 59.3% | 59.3% | 59.4% | 46.0% | 46.0% | 46.0% | 46.0% |
| customers.getPaymentMethod | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% | 32.7% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-http wire json | 7625 calls, 0.00 ms | - | - | 16390 calls, 1.53 ms | - |
| loopback-http wire gzip | 7625 calls, 0.00 ms | - | - | 16390 calls, 1.47 ms | - |
| loopback-http wire bson | 7625 calls, 0.00 ms | - | - | 16390 calls, 1.62 ms | - |
| loopback-http wire bson-gzip | 7626 calls, 0.00 ms | - | - | 16389 calls, 1.56 ms | - |
| groups wire json | 7625 calls, 0.02 ms | 6788 calls, 0.50 ms | - | - | 9602 calls, 1.71 ms |
| groups wire gzip | 7626 calls, 0.02 ms | 6788 calls, 0.50 ms | - | - | 9601 calls, 1.79 ms |
| groups wire bson | 7624 calls, 0.02 ms | 6788 calls, 0.51 ms | - | - | 9603 calls, 1.79 ms |
| groups wire bson-gzip | 7629 calls, 0.02 ms | 6788 calls, 0.49 ms | - | - | 9598 calls, 1.76 ms |
| microservices wire json | 6829 calls, 0.01 ms | - | - | - | 17186 calls, 2.08 ms |
| microservices wire gzip | 6827 calls, 0.01 ms | - | - | - | 17188 calls, 2.29 ms |
| microservices wire bson | 6829 calls, 0.01 ms | - | - | - | 17186 calls, 2.20 ms |
| microservices wire bson-gzip | 6827 calls, 0.01 ms | - | - | - | 17188 calls, 2.31 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-http (cache on, parallel quotes on, async events)

1200 orders in 5.8 s = 207.7 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.8 | 19.3 | 18.7 | 25.6 | 27.3 | 31.4 | 36.8 |
| placeOrder | declined | 21 | 7.1 | 13.0 | 12.1 | 18.1 | 18.6 | 25.5 | 25.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3535 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1200 |  | 3.4 | 3.2 | 6.2 | 8.3 | 9.8 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 2.7 | 2.6 | 3.4 | 6.8 | 7.9 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 1.1 | 0.9 | 1.8 | 4.0 | 6.1 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 2.0 | 1.9 | 2.8 | 5.5 | 7.2 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 2.0 | 1.9 | 3.5 | 5.9 | 7.8 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 1.9 | 2.0 | 2.4 | 5.4 | 8.5 |
| orders -> notifications.send [loopback-http] | 1179 |  | 1.5 | 1.4 | 1.7 | 5.5 | 6.2 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.2 | 1.1 | 1.4 | 1.7 | 5.9 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 0.9 | 0.9 | 1.0 | 4.0 | 5.6 |
| orders -> customers.getCustomer [loopback-http] | 808 |  | 0.9 | 0.9 | 1.1 | 4.2 | 5.1 |
| pricing -> customers.getCustomer [loopback-http] | 808 |  | 0.9 | 0.8 | 1.0 | 4.6 | 5.4 |
| shipping -> customers.getCustomer [loopback-http] | 807 |  | 0.8 | 0.8 | 1.0 | 4.0 | 4.9 |
| payments -> customers.getPaymentMethod [loopback-http] | 808 |  | 0.8 | 0.8 | 1.0 | 1.1 | 5.4 |
| pricing -> catalog.getProduct [loopback-http] | 591 |  | 0.9 | 0.8 | 1.0 | 1.2 | 5.4 |
| inventory -> catalog.productExists [loopback-http] | 591 |  | 0.8 | 0.8 | 1.0 | 4.0 | 5.2 |
| orders -> inventory.release [loopback-http] | 21 |  | 2.0 | 1.8 | 2.9 | 3.6 | 3.6 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1572 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.7 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7625 calls, mean 0.00 ms, loopback-http 16390 calls, mean 1.53 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/loopback-http-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events)

1200 orders in 5.5 s = 218.9 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.8 | 18.3 | 17.7 | 24.3 | 26.0 | 30.2 | 38.0 |
| placeOrder | declined | 21 | 7.5 | 12.6 | 12.5 | 17.8 | 18.2 | 25.8 | 25.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3535 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1200 |  | 3.3 | 2.9 | 6.4 | 8.2 | 12.6 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 2.6 | 2.5 | 3.4 | 6.0 | 8.4 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 1.0 | 0.8 | 1.8 | 4.1 | 8.6 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 2.0 | 1.7 | 3.9 | 5.8 | 9.8 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 1.9 | 1.8 | 2.6 | 4.8 | 6.2 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 1.8 | 1.9 | 2.5 | 5.0 | 8.0 |
| orders -> notifications.send [loopback-http] | 1179 |  | 1.4 | 1.3 | 1.8 | 4.7 | 6.0 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.1 | 1.1 | 1.3 | 3.8 | 4.9 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 0.9 | 0.8 | 1.1 | 3.5 | 4.3 |
| orders -> customers.getCustomer [loopback-http] | 808 |  | 0.9 | 0.8 | 1.1 | 4.0 | 5.0 |
| pricing -> customers.getCustomer [loopback-http] | 807 |  | 0.8 | 0.8 | 1.0 | 3.9 | 8.6 |
| shipping -> customers.getCustomer [loopback-http] | 808 |  | 0.8 | 0.8 | 1.0 | 3.9 | 8.7 |
| payments -> customers.getPaymentMethod [loopback-http] | 808 |  | 0.8 | 0.7 | 1.0 | 1.1 | 4.5 |
| pricing -> catalog.getProduct [loopback-http] | 591 |  | 0.8 | 0.8 | 1.0 | 3.9 | 4.7 |
| inventory -> catalog.productExists [loopback-http] | 591 |  | 0.8 | 0.7 | 1.0 | 4.1 | 4.7 |
| orders -> inventory.release [loopback-http] | 21 |  | 1.9 | 1.6 | 2.8 | 5.0 | 5.0 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 393 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7625 calls, mean 0.00 ms, loopback-http 16390 calls, mean 1.47 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/loopback-http-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events)

1200 orders in 6.1 s = 196.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.7 | 20.4 | 20.1 | 26.4 | 28.3 | 31.6 | 35.0 |
| placeOrder | declined | 21 | 8.9 | 14.6 | 13.6 | 20.1 | 20.6 | 22.1 | 22.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3535 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1200 |  | 3.6 | 3.3 | 6.5 | 8.2 | 10.5 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 2.9 | 2.8 | 3.5 | 6.8 | 8.0 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 1.1 | 0.9 | 1.8 | 2.0 | 6.0 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 2.2 | 2.0 | 3.0 | 6.1 | 9.0 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 2.2 | 1.9 | 3.7 | 6.1 | 7.6 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 2.0 | 2.1 | 2.5 | 5.7 | 6.7 |
| orders -> notifications.send [loopback-http] | 1179 |  | 1.6 | 1.5 | 1.8 | 5.5 | 6.1 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.2 | 1.2 | 1.4 | 4.6 | 5.4 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 1.0 | 0.9 | 1.1 | 1.3 | 5.1 |
| orders -> customers.getCustomer [loopback-http] | 808 |  | 1.0 | 0.9 | 1.1 | 4.7 | 5.2 |
| pricing -> customers.getCustomer [loopback-http] | 807 |  | 0.9 | 0.9 | 1.0 | 1.5 | 5.0 |
| shipping -> customers.getCustomer [loopback-http] | 808 |  | 0.9 | 0.8 | 1.0 | 1.3 | 5.0 |
| payments -> customers.getPaymentMethod [loopback-http] | 808 |  | 0.9 | 0.8 | 1.0 | 1.1 | 4.7 |
| pricing -> catalog.getProduct [loopback-http] | 591 |  | 0.9 | 0.9 | 1.1 | 4.4 | 4.8 |
| inventory -> catalog.productExists [loopback-http] | 591 |  | 0.9 | 0.8 | 1.0 | 1.1 | 5.1 |
| orders -> inventory.release [loopback-http] | 21 |  | 2.5 | 1.9 | 6.2 | 6.5 | 6.5 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 393 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7625 calls, mean 0.00 ms, loopback-http 16390 calls, mean 1.62 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/loopback-http-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events)

1200 orders in 5.8 s = 208.5 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.8 | 19.2 | 18.6 | 25.5 | 27.8 | 31.2 | 38.9 |
| placeOrder | declined | 21 | 8.5 | 13.3 | 12.9 | 17.6 | 18.2 | 19.7 | 19.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3536 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 1200 |  | 3.5 | 3.1 | 6.6 | 8.5 | 10.8 |
| orders -> shipping.createShipment [loopback-http] | 1179 |  | 2.7 | 2.6 | 3.4 | 5.9 | 9.7 |
| shipping -> inventory.getStock [loopback-http] | 2440 |  | 1.1 | 0.9 | 1.9 | 4.0 | 5.6 |
| orders -> pricing.quote [loopback-http] | 1200 |  | 2.1 | 1.9 | 3.9 | 6.1 | 7.9 |
| orders -> inventory.reserve [loopback-http] | 1200 |  | 2.0 | 1.9 | 2.8 | 5.4 | 7.5 |
| orders -> payments.authorize [loopback-http] | 1200 | 21 | 1.9 | 2.0 | 2.6 | 5.3 | 8.7 |
| orders -> notifications.send [loopback-http] | 1179 |  | 1.5 | 1.4 | 1.8 | 4.8 | 6.7 |
| orders -> payments.capture [loopback-http] | 1179 |  | 1.2 | 1.1 | 1.4 | 4.0 | 5.5 |
| shipping -> inventory.getReservation [loopback-http] | 1179 |  | 0.9 | 0.9 | 1.1 | 1.2 | 4.5 |
| orders -> customers.getCustomer [loopback-http] | 808 |  | 1.0 | 0.9 | 1.2 | 4.1 | 5.4 |
| pricing -> customers.getCustomer [loopback-http] | 808 |  | 0.9 | 0.8 | 1.1 | 4.0 | 5.4 |
| shipping -> customers.getCustomer [loopback-http] | 806 |  | 0.9 | 0.8 | 1.1 | 4.1 | 5.4 |
| payments -> customers.getPaymentMethod [loopback-http] | 808 |  | 0.8 | 0.8 | 1.0 | 1.2 | 5.3 |
| pricing -> catalog.getProduct [loopback-http] | 591 |  | 0.9 | 0.8 | 1.1 | 4.1 | 4.8 |
| inventory -> catalog.productExists [loopback-http] | 591 |  | 0.8 | 0.8 | 1.0 | 1.2 | 4.4 |
| orders -> inventory.release [loopback-http] | 21 |  | 2.4 | 2.0 | 5.3 | 6.0 | 6.0 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1573 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7626 calls, mean 0.00 ms, loopback-http 16389 calls, mean 1.56 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/loopback-http-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

1200 orders in 5.3 s = 225.3 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 10.8 | 17.8 | 17.4 | 23.5 | 25.1 | 27.7 | 37.5 |
| placeOrder | declined | 21 | 7.0 | 12.4 | 11.9 | 18.3 | 19.2 | 19.2 | 19.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3535 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.3 | 2.3 | 3.6 | 5.4 | 7.1 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.3 | 2.3 | 2.9 | 5.6 | 6.8 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.1 | 2.0 | 2.9 | 3.5 | 6.4 |
| orders -> pricing.quote [remote] | 1200 |  | 2.0 | 2.0 | 2.8 | 4.9 | 6.3 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.7 | 1.7 | 2.2 | 3.4 | 5.3 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.2 | 1.5 | 1.7 | 5.9 |
| orders -> notifications.send [local] | 1179 |  | 1.0 | 1.0 | 1.2 | 1.4 | 5.7 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.8 | 0.8 | 4.4 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.2 | 1.7 | 5.1 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 0.9 | 1.2 | 1.8 | 4.8 |
| shipping -> customers.getCustomer [remote] | 807 |  | 1.0 | 0.9 | 1.2 | 1.7 | 5.0 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 3.8 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.6 | 3.8 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 3.6 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 2.7 |
| orders -> inventory.release [remote] | 21 |  | 2.4 | 2.1 | 4.6 | 5.5 | 5.5 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1572 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7625 calls, mean 0.02 ms, local 6788 calls, mean 0.50 ms, remote 9602 calls, mean 1.71 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/groups-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

1200 orders in 5.5 s = 219.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.7 | 18.3 | 17.4 | 24.9 | 26.7 | 31.5 | 39.4 |
| placeOrder | declined | 21 | 8.0 | 12.6 | 13.5 | 16.0 | 16.6 | 19.2 | 19.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3536 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.4 | 2.4 | 3.8 | 5.2 | 7.0 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.4 | 2.3 | 3.0 | 4.8 | 5.9 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.2 | 2.1 | 3.1 | 4.6 | 6.1 |
| orders -> pricing.quote [remote] | 1200 |  | 2.1 | 2.1 | 3.0 | 4.7 | 6.7 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.8 | 1.8 | 2.4 | 3.3 | 8.1 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.3 | 1.6 | 2.8 | 7.4 |
| orders -> notifications.send [local] | 1179 |  | 1.0 | 1.0 | 1.3 | 1.8 | 5.4 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.8 | 0.9 | 3.9 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.1 | 1.4 | 3.2 | 5.0 |
| shipping -> customers.getCustomer [remote] | 806 |  | 1.0 | 1.0 | 1.3 | 1.5 | 4.1 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.3 | 1.6 | 3.9 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 3.1 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.8 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 1.6 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.7 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| orders -> inventory.release [remote] | 21 |  | 2.3 | 2.2 | 3.2 | 4.9 | 4.9 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.3 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1573 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7626 calls, mean 0.02 ms, local 6788 calls, mean 0.50 ms, remote 9601 calls, mean 1.79 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/groups-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

1200 orders in 5.5 s = 218.4 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.9 | 18.3 | 17.8 | 24.2 | 26.7 | 30.1 | 35.2 |
| placeOrder | declined | 21 | 7.7 | 12.9 | 13.0 | 18.1 | 19.1 | 19.1 | 19.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3534 | 59.3% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.4 | 2.4 | 3.8 | 5.7 | 7.5 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.4 | 2.3 | 3.0 | 5.3 | 6.7 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.2 | 2.1 | 3.1 | 4.2 | 7.1 |
| orders -> pricing.quote [remote] | 1200 |  | 2.1 | 2.1 | 2.9 | 5.2 | 6.5 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.8 | 1.8 | 2.3 | 3.0 | 6.9 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.3 | 1.5 | 1.8 | 5.7 |
| orders -> notifications.send [local] | 1179 |  | 1.0 | 1.0 | 1.2 | 1.4 | 5.4 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.8 | 0.9 | 4.5 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.2 | 1.7 | 5.3 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.2 | 3.5 | 5.0 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.0 | 1.0 | 1.2 | 3.0 | 5.1 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 4.5 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.9 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 2.5 |
| orders -> inventory.release [remote] | 21 |  | 2.5 | 2.1 | 3.4 | 6.2 | 6.2 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7624 calls, mean 0.02 ms, local 6788 calls, mean 0.51 ms, remote 9603 calls, mean 1.79 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/groups-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

1200 orders in 5.3 s = 225.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 9.9 | 17.8 | 16.5 | 24.5 | 27.3 | 31.3 | 95.9 |
| placeOrder | declined | 21 | 7.1 | 11.5 | 10.8 | 15.5 | 17.2 | 19.2 | 19.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1849 | 75.8% |
| catalog.productExists | 2440 | 1849 | 75.8% |
| customers.getCustomer | 5958 | 3539 | 59.4% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 2.4 | 2.3 | 3.7 | 5.3 | 65.9 |
| orders -> shipping.createShipment [remote] | 1179 |  | 2.3 | 2.2 | 3.0 | 4.7 | 5.6 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.2 | 2.0 | 2.9 | 4.5 | 63.2 |
| orders -> pricing.quote [remote] | 1200 |  | 2.1 | 2.0 | 3.1 | 4.7 | 63.0 |
| orders -> payments.authorize [remote] | 1200 | 21 | 1.8 | 1.7 | 2.4 | 3.6 | 5.1 |
| orders -> payments.capture [remote] | 1179 |  | 1.3 | 1.2 | 1.6 | 1.8 | 4.8 |
| orders -> notifications.send [local] | 1179 |  | 1.1 | 0.9 | 1.2 | 3.0 | 64.5 |
| shipping -> inventory.getStock [local] | 2440 |  | 0.4 | 0.4 | 0.7 | 0.9 | 3.3 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.3 | 3.1 | 63.3 |
| shipping -> customers.getCustomer [remote] | 805 |  | 1.0 | 1.0 | 1.3 | 3.1 | 5.7 |
| pricing -> customers.getCustomer [remote] | 806 |  | 1.0 | 1.0 | 1.3 | 3.0 | 5.3 |
| shipping -> inventory.getReservation [local] | 1179 |  | 0.4 | 0.4 | 0.5 | 0.6 | 0.9 |
| payments -> customers.getPaymentMethod [local] | 808 |  | 0.3 | 0.3 | 0.4 | 0.5 | 2.3 |
| inventory -> catalog.productExists [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.6 | 0.6 |
| pricing -> catalog.getProduct [local] | 591 |  | 0.3 | 0.3 | 0.4 | 0.4 | 2.4 |
| pricing -> catalog.getProduct [cache] | 1849 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| orders -> inventory.release [remote] | 21 |  | 2.1 | 2.0 | 3.5 | 3.7 | 3.7 |
| inventory -> catalog.productExists [cache] | 1849 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 1574 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1179 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 394 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 7629 calls, mean 0.02 ms, local 6788 calls, mean 0.49 ms, remote 9598 calls, mean 1.76 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/groups-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

1200 orders in 7.3 s = 164.1 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 12.9 | 24.4 | 23.1 | 32.4 | 35.4 | 62.7 | 91.6 |
| placeOrder | declined | 21 | 9.9 | 15.6 | 15.9 | 20.9 | 23.0 | 24.1 | 24.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2741 | 46.0% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.4 | 4.0 | 7.8 | 9.7 | 19.0 |
| orders -> notifications.send [remote] | 1179 |  | 3.5 | 2.8 | 5.0 | 41.6 | 63.3 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.4 | 3.3 | 4.5 | 5.7 | 58.5 |
| orders -> pricing.quote [remote] | 1200 |  | 2.9 | 2.6 | 4.9 | 6.8 | 59.5 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.4 | 1.1 | 2.7 | 3.7 | 8.7 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.7 | 2.6 | 3.8 | 5.0 | 51.8 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.4 | 2.2 | 3.3 | 4.1 | 56.9 |
| orders -> payments.capture [remote] | 1179 |  | 1.4 | 1.4 | 1.8 | 2.3 | 4.6 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.1 | 1.1 | 1.4 | 2.1 | 4.2 |
| notifications -> customers.getCustomer [remote] | 793 |  | 1.3 | 1.1 | 1.8 | 2.6 | 59.9 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.2 | 1.1 | 1.7 | 2.2 | 49.1 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.2 | 1.1 | 1.5 | 2.1 | 4.4 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.1 | 1.5 | 2.5 | 4.4 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.0 | 1.4 | 1.7 | 3.8 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.4 | 1.2 | 1.8 | 2.9 | 54.7 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.3 | 1.2 | 2.2 | 3.7 | 6.5 |
| orders -> inventory.release [remote] | 21 |  | 2.4 | 2.3 | 3.8 | 3.8 | 3.8 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| notifications -> customers.getCustomer [cache] | 386 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6829 calls, mean 0.01 ms, remote 17186 calls, mean 2.08 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/microservices-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

1200 orders in 7.9 s = 151.1 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 14.0 | 26.4 | 25.1 | 34.4 | 38.3 | 77.7 | 113.5 |
| placeOrder | declined | 21 | 8.9 | 22.1 | 18.3 | 28.7 | 41.8 | 69.4 | 69.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1847 | 75.7% |
| catalog.productExists | 2440 | 1847 | 75.7% |
| customers.getCustomer | 5958 | 2741 | 46.0% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 5.1 | 4.6 | 8.7 | 10.8 | 66.7 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.6 | 3.6 | 4.6 | 6.3 | 10.7 |
| orders -> notifications.send [remote] | 1179 |  | 3.5 | 3.1 | 5.0 | 13.7 | 68.0 |
| orders -> pricing.quote [remote] | 1200 |  | 3.3 | 2.8 | 5.8 | 8.4 | 63.7 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.6 | 1.2 | 3.0 | 3.9 | 59.9 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.9 | 2.9 | 4.1 | 5.4 | 59.0 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.6 | 2.4 | 3.4 | 4.6 | 59.5 |
| orders -> payments.capture [remote] | 1179 |  | 1.5 | 1.5 | 2.0 | 2.6 | 6.9 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.2 | 1.2 | 1.6 | 1.8 | 3.9 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.5 | 1.2 | 1.9 | 2.8 | 54.7 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.3 | 1.2 | 1.6 | 2.3 | 51.7 |
| notifications -> customers.getCustomer [remote] | 793 |  | 1.3 | 1.2 | 1.8 | 2.3 | 4.1 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.3 | 1.2 | 1.7 | 2.5 | 7.0 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.2 | 1.1 | 1.5 | 2.3 | 4.4 |
| inventory -> catalog.productExists [remote] | 593 |  | 1.6 | 1.4 | 2.5 | 3.4 | 57.9 |
| pricing -> catalog.getProduct [remote] | 593 |  | 1.4 | 1.4 | 2.1 | 3.2 | 4.2 |
| orders -> inventory.release [remote] | 21 |  | 2.4 | 2.2 | 3.5 | 3.5 | 3.5 |
| inventory -> catalog.productExists [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 386 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 6827 calls, mean 0.01 ms, remote 17188 calls, mean 2.29 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/microservices-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

1200 orders in 7.7 s = 155.0 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 12.2 | 25.8 | 24.0 | 34.4 | 37.8 | 84.1 | 107.1 |
| placeOrder | declined | 21 | 9.5 | 21.9 | 19.8 | 30.0 | 30.1 | 79.6 | 79.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1848 | 75.7% |
| catalog.productExists | 2440 | 1848 | 75.7% |
| customers.getCustomer | 5958 | 2741 | 46.0% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 4.8 | 4.3 | 8.2 | 10.4 | 66.5 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.8 | 3.4 | 4.7 | 6.2 | 63.7 |
| orders -> pricing.quote [remote] | 1200 |  | 3.2 | 2.7 | 5.3 | 8.3 | 66.5 |
| orders -> notifications.send [remote] | 1179 |  | 3.2 | 2.9 | 4.8 | 6.3 | 67.0 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.5 | 1.1 | 2.8 | 3.5 | 62.0 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.7 | 2.8 | 4.0 | 4.9 | 61.8 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.4 | 2.3 | 3.3 | 4.2 | 12.4 |
| orders -> payments.capture [remote] | 1179 |  | 1.6 | 1.4 | 1.9 | 2.2 | 58.8 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.3 | 1.1 | 1.5 | 2.2 | 60.0 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.3 | 1.2 | 1.9 | 2.5 | 58.5 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.2 | 1.1 | 1.5 | 3.1 | 54.4 |
| notifications -> customers.getCustomer [remote] | 793 |  | 1.3 | 1.2 | 1.8 | 2.7 | 3.7 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.2 | 1.2 | 1.6 | 3.0 | 4.8 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.1 | 1.1 | 1.5 | 3.1 | 3.9 |
| inventory -> catalog.productExists [remote] | 592 |  | 1.5 | 1.4 | 2.0 | 3.3 | 60.5 |
| pricing -> catalog.getProduct [remote] | 592 |  | 1.4 | 1.4 | 2.0 | 2.4 | 4.6 |
| orders -> inventory.release [remote] | 21 |  | 2.5 | 2.2 | 3.5 | 4.7 | 4.7 |
| inventory -> catalog.productExists [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 1848 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 386 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 6829 calls, mean 0.01 ms, remote 17186 calls, mean 2.20 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/microservices-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

1200 orders in 8.0 s = 150.6 orders/s; 1179 ok, 21 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1179 | 13.8 | 26.6 | 24.9 | 35.5 | 38.7 | 73.8 | 103.9 |
| placeOrder | declined | 21 | 10.6 | 19.2 | 17.0 | 27.3 | 30.0 | 45.3 | 45.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 2440 | 1847 | 75.7% |
| catalog.productExists | 2440 | 1847 | 75.7% |
| customers.getCustomer | 5958 | 2741 | 46.0% |
| customers.getPaymentMethod | 1200 | 392 | 32.7% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1200 |  | 5.0 | 4.7 | 8.7 | 11.0 | 62.6 |
| orders -> notifications.send [remote] | 1179 |  | 3.8 | 3.1 | 5.2 | 44.8 | 65.6 |
| orders -> shipping.createShipment [remote] | 1179 |  | 3.6 | 3.5 | 4.6 | 6.1 | 7.7 |
| orders -> pricing.quote [remote] | 1200 |  | 3.4 | 3.0 | 5.9 | 8.2 | 62.6 |
| shipping -> inventory.getStock [remote] | 2440 |  | 1.6 | 1.2 | 3.0 | 3.8 | 6.3 |
| orders -> payments.authorize [remote] | 1200 | 21 | 2.9 | 3.0 | 4.1 | 5.2 | 55.4 |
| orders -> inventory.reserve [remote] | 1200 |  | 2.5 | 2.4 | 3.4 | 4.5 | 8.7 |
| orders -> payments.capture [remote] | 1179 |  | 1.7 | 1.5 | 2.0 | 2.5 | 50.1 |
| shipping -> inventory.getReservation [remote] | 1179 |  | 1.2 | 1.2 | 1.6 | 2.7 | 4.9 |
| pricing -> customers.getCustomer [remote] | 808 |  | 1.3 | 1.2 | 1.7 | 3.1 | 55.0 |
| payments -> customers.getPaymentMethod [remote] | 808 |  | 1.3 | 1.3 | 1.9 | 2.2 | 4.2 |
| notifications -> customers.getCustomer [remote] | 793 |  | 1.4 | 1.3 | 1.9 | 2.7 | 3.0 |
| orders -> customers.getCustomer [remote] | 808 |  | 1.3 | 1.2 | 1.7 | 2.1 | 4.5 |
| shipping -> customers.getCustomer [remote] | 808 |  | 1.2 | 1.2 | 1.6 | 2.7 | 4.3 |
| inventory -> catalog.productExists [remote] | 593 |  | 1.5 | 1.4 | 2.3 | 3.3 | 4.0 |
| pricing -> catalog.getProduct [remote] | 593 |  | 1.5 | 1.4 | 2.1 | 2.8 | 3.9 |
| orders -> inventory.release [remote] | 21 |  | 2.5 | 2.2 | 3.9 | 4.6 | 4.6 |
| inventory -> catalog.productExists [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 1847 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 1571 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 386 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 392 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 6827 calls, mean 0.01 ms, remote 17188 calls, mean 2.31 ms. 20.0 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180026Z/microservices-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
