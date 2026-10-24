# Performance run 20261024T180619Z

- Started: 2026-10-24T18:06:19+00:00
- Commit: 243b501 (uncommitted changes)
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 500 orders, 4 concurrent clients (closed loop, persistent connections), 60 warmup orders not measured
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 40 lines per order, 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes loopback-http,groups,microservices --wire all --async events --lines 40 --orders 500 --warmup 60 --concurrency 4`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http wire json | 53 | 489 | 11 | 50.2 | 75.6 | 74.0 | 88.2 | 97.7 | 106.1 | 118.7 | 133.9 | PASS |
| loopback-http wire gzip | 50 | 489 | 11 | 53.0 | 79.5 | 75.3 | 95.6 | 103.1 | 110.1 | 123.6 | 133.9 | PASS |
| loopback-http wire bson | 52 | 489 | 11 | 51.2 | 77.2 | 72.5 | 91.8 | 99.3 | 110.7 | 144.3 | 133.9 | PASS |
| loopback-http wire bson-gzip | 49 | 489 | 11 | 63.9 | 81.0 | 80.2 | 92.3 | 98.5 | 122.7 | 155.3 | 133.9 | PASS |
| groups wire json | 73 | 489 | 11 | 42.1 | 54.6 | 51.1 | 66.3 | 69.6 | 103.0 | 118.1 | 133.9 | PASS |
| groups wire gzip | 72 | 489 | 11 | 41.2 | 55.0 | 51.2 | 67.0 | 70.3 | 97.7 | 142.9 | 133.9 | PASS |
| groups wire bson | 73 | 489 | 11 | 41.4 | 54.3 | 50.9 | 66.5 | 70.1 | 109.7 | 156.0 | 133.9 | PASS |
| groups wire bson-gzip | 73 | 489 | 11 | 42.3 | 54.9 | 52.0 | 67.7 | 70.8 | 106.2 | 111.9 | 133.9 | PASS |
| microservices wire json | 47 | 489 | 11 | 54.0 | 85.2 | 76.7 | 117.8 | 136.6 | 157.0 | 196.6 | 133.9 | PASS |
| microservices wire gzip | 46 | 489 | 11 | 51.7 | 87.3 | 76.3 | 119.0 | 126.7 | 160.7 | 220.8 | 133.9 | PASS |
| microservices wire bson | 46 | 489 | 11 | 53.9 | 86.1 | 77.6 | 116.0 | 126.6 | 154.8 | 176.4 | 133.9 | PASS |
| microservices wire bson-gzip | 45 | 489 | 11 | 55.4 | 88.2 | 79.1 | 120.1 | 129.0 | 154.2 | 170.0 | 133.9 | PASS |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http wire json | 55.3 | 21,075 | 21,075 | 100% | 171 to 171 | 210 to 210 |
| loopback-http wire gzip | 55.3 | 21,075 | 13,312 | 63% | 171 to 116 | 210 to 125 |
| loopback-http wire bson | 55.3 | 21,073 | 23,669 | 112% | 171 to 197 | 210 to 231 |
| loopback-http wire bson-gzip | 55.3 | 21,075 | 14,869 | 71% | 171 to 129 | 210 to 140 |
| groups wire json | 8.3 | 11,013 | 11,013 | 100% | 620 to 620 | 708 to 708 |
| groups wire gzip | 8.3 | 11,014 | 3,442 | 31% | 619 to 184 | 708 to 231 |
| groups wire bson | 8.3 | 11,014 | 12,757 | 116% | 619 to 755 | 708 to 782 |
| groups wire bson-gzip | 8.3 | 11,014 | 4,305 | 39% | 619 to 241 | 708 to 278 |
| microservices wire json | 56.1 | 21,300 | 21,300 | 100% | 170 to 170 | 210 to 210 |
| microservices wire gzip | 56.1 | 21,301 | 13,520 | 63% | 170 to 115 | 210 to 126 |
| microservices wire bson | 56.1 | 21,300 | 23,923 | 112% | 170 to 196 | 210 to 231 |
| microservices wire bson-gzip | 56.1 | 21,301 | 15,105 | 71% | 170 to 129 | 210 to 140 |

## Wire format effect (json to each alternative)

Higher orders/s and lower latencies are better; the last column is the bytes on the wire against plain JSON.

| mode (format) | orders/s json | alt | Δ | p50 json | alt | Δ | p95 json | alt | Δ | p99 json | alt | Δ | bytes vs json |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| loopback-http (gzip) | 53 | 50 | -5% | 74.0 | 75.3 | +2% | 97.7 | 103.1 | +5% | 106.1 | 110.1 | +4% | 63% |
| loopback-http (bson) | 53 | 52 | -2% | 74.0 | 72.5 | -2% | 97.7 | 99.3 | +2% | 106.1 | 110.7 | +4% | 112% |
| loopback-http (bson-gzip) | 53 | 49 | -7% | 74.0 | 80.2 | +8% | 97.7 | 98.5 | +1% | 106.1 | 122.7 | +16% | 71% |
| groups (gzip) | 73 | 72 | -1% | 51.1 | 51.2 | +0% | 69.6 | 70.3 | +1% | 103.0 | 97.7 | -5% | 31% |
| groups (bson) | 73 | 73 | +0% | 51.1 | 50.9 | -0% | 69.6 | 70.1 | +1% | 103.0 | 109.7 | +7% | 116% |
| groups (bson-gzip) | 73 | 73 | -1% | 51.1 | 52.0 | +2% | 69.6 | 70.8 | +2% | 103.0 | 106.2 | +3% | 39% |
| microservices (gzip) | 47 | 46 | -2% | 76.7 | 76.3 | -0% | 136.6 | 126.7 | -7% | 157.0 | 160.7 | +2% | 63% |
| microservices (bson) | 47 | 46 | -1% | 76.7 | 77.6 | +1% | 136.6 | 126.6 | -7% | 157.0 | 154.8 | -1% | 112% |
| microservices (bson-gzip) | 47 | 45 | -3% | 76.7 | 79.1 | +3% | 136.6 | 129.0 | -6% | 157.0 | 154.2 | -2% | 71% |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http wire json | loopback-http wire gzip | loopback-http wire bson | loopback-http wire bson-gzip | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 30.9 | 33.6 | 31.8 | 34.5 | 13.3 | 13.5 | 13.4 | 13.3 | 33.0 | 34.7 | 33.7 | 35.3 |
| shipping -> inventory.getStock | 0.7 | 0.8 | 0.7 | 0.8 | 0.3 | 0.3 | 0.3 | 0.3 | 0.7 | 0.8 | 0.8 | 0.8 |
| orders -> inventory.reserve | 11.3 | 11.1 | 11.2 | 11.3 | 12.0 | 12.2 | 11.9 | 12.1 | 11.9 | 11.5 | 11.9 | 11.3 |
| orders -> shipping.createShipment | 8.9 | 9.0 | 9.0 | 9.0 | 9.0 | 9.0 | 8.8 | 8.9 | 9.6 | 9.5 | 9.7 | 9.6 |
| orders -> pricing.quote | 3.6 | 3.9 | 3.7 | 4.0 | 3.5 | 3.6 | 3.6 | 3.7 | 5.8 | 5.7 | 5.8 | 6.2 |
| orders -> payments.authorize | 1.8 | 1.9 | 1.9 | 2.0 | 2.0 | 2.0 | 2.1 | 2.0 | 3.8 | 4.0 | 3.6 | 4.1 |
| orders -> notifications.send | 1.3 | 1.5 | 1.4 | 1.5 | 1.1 | 1.0 | 1.1 | 1.1 | 4.3 | 4.4 | 4.7 | 4.6 |
| inventory -> catalog.productExists | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> catalog.getProduct | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 | 0.1 |
| orders -> payments.capture | 1.1 | 1.1 | 1.0 | 1.1 | 1.4 | 1.4 | 1.4 | 1.4 | 1.8 | 1.9 | 1.8 | 2.1 |
| shipping -> inventory.getReservation | 1.2 | 1.3 | 1.3 | 1.4 | 0.8 | 0.8 | 0.8 | 0.8 | 1.4 | 1.4 | 1.4 | 1.5 |
| pricing -> customers.getCustomer | 0.6 | 0.6 | 0.6 | 0.6 | 0.9 | 1.0 | 0.9 | 0.9 | 1.0 | 0.9 | 0.9 | 1.0 |
| orders -> customers.getCustomer | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.8 | 0.9 | 1.0 | 1.0 | 1.0 | 1.0 |
| shipping -> customers.getCustomer | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.5 | 0.5 | 0.4 | 0.5 | 0.5 | 0.4 | 0.4 |
| payments -> customers.getPaymentMethod | 0.6 | 0.6 | 0.6 | 0.6 | 0.3 | 0.3 | 0.3 | 0.3 | 1.4 | 1.3 | 1.3 | 1.4 |
| notifications -> customers.getCustomer | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 1.5 | 1.2 | 1.4 | 1.3 |
| orders -> inventory.release | 16.1 | 14.9 | 14.5 | 16.7 | 17.0 | 17.9 | 17.5 | 16.6 | 14.8 | 14.5 | 15.2 | 15.1 |
| (calls per order) | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | loopback-http wire json | loopback-http wire gzip | loopback-http wire bson | loopback-http wire bson-gzip | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 40.1 | 44.1 | 41.0 | 44.8 | 17.9 | 18.0 | 17.7 | 17.9 | 44.6 | 44.1 | 46.3 | 42.9 |
| shipping -> inventory.getStock | 1.2 | 1.3 | 1.3 | 1.4 | 0.4 | 0.4 | 0.4 | 0.4 | 1.4 | 1.5 | 1.5 | 1.5 |
| orders -> inventory.reserve | 15.4 | 15.0 | 14.8 | 14.9 | 16.6 | 16.8 | 16.4 | 16.3 | 17.4 | 16.1 | 16.7 | 16.3 |
| orders -> shipping.createShipment | 11.9 | 11.8 | 11.8 | 11.8 | 12.0 | 11.3 | 11.1 | 11.3 | 12.5 | 12.4 | 12.3 | 12.5 |
| orders -> pricing.quote | 7.4 | 8.1 | 7.9 | 8.3 | 6.2 | 6.4 | 6.3 | 6.4 | 11.3 | 11.2 | 11.7 | 12.2 |
| orders -> payments.authorize | 2.4 | 2.6 | 2.5 | 2.7 | 2.9 | 3.1 | 3.3 | 3.2 | 6.4 | 6.5 | 6.2 | 6.6 |
| orders -> notifications.send | 1.6 | 1.9 | 1.7 | 1.8 | 1.4 | 1.5 | 1.6 | 1.5 | 7.5 | 7.5 | 7.7 | 7.2 |
| inventory -> catalog.productExists | 0.6 | 0.6 | 0.6 | 0.6 | 0.2 | 0.2 | 0.2 | 0.2 | 0.7 | 0.7 | 0.7 | 0.7 |
| pricing -> catalog.getProduct | 0.6 | 0.6 | 0.6 | 0.6 | 0.2 | 0.2 | 0.2 | 0.2 | 0.7 | 0.7 | 0.7 | 0.7 |
| orders -> payments.capture | 1.3 | 1.3 | 1.2 | 1.4 | 1.8 | 2.0 | 2.0 | 2.1 | 2.8 | 2.9 | 2.9 | 3.0 |
| shipping -> inventory.getReservation | 1.5 | 1.6 | 1.6 | 1.7 | 1.1 | 1.0 | 1.0 | 1.1 | 1.7 | 1.8 | 1.9 | 1.9 |
| pricing -> customers.getCustomer | 0.9 | 0.9 | 0.9 | 1.0 | 1.5 | 1.8 | 1.7 | 1.7 | 1.7 | 1.6 | 1.6 | 1.6 |
| orders -> customers.getCustomer | 1.0 | 1.0 | 1.0 | 1.0 | 1.2 | 1.5 | 1.5 | 1.5 | 1.8 | 1.7 | 1.7 | 1.8 |
| shipping -> customers.getCustomer | 0.8 | 0.9 | 0.8 | 0.9 | 1.3 | 1.5 | 1.4 | 1.5 | 1.3 | 1.3 | 1.3 | 1.3 |
| payments -> customers.getPaymentMethod | 0.9 | 0.9 | 0.9 | 1.0 | 0.5 | 0.5 | 0.5 | 0.5 | 2.4 | 2.3 | 2.3 | 2.4 |
| notifications -> customers.getCustomer | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 | 2.4 | 2.3 | 2.2 | 2.6 |
| orders -> inventory.release | 23.8 | 24.4 | 22.5 | 32.0 | 23.7 | 34.3 | 34.4 | 23.0 | 24.5 | 22.0 | 31.2 | 24.5 |
| (calls per order) | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 | 133.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | loopback-http wire json | loopback-http wire gzip | loopback-http wire bson | loopback-http wire bson-gzip | groups wire json | groups wire gzip | groups wire bson | groups wire bson-gzip | microservices wire json | microservices wire gzip | microservices wire bson | microservices wire bson-gzip |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.6% |
| catalog.productExists | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.7% | 94.6% |
| customers.getCustomer | 53.2% | 53.1% | 53.3% | 53.2% | 53.2% | 53.1% | 53.1% | 53.1% | 37.7% | 37.7% | 37.7% | 37.9% |
| customers.getPaymentMethod | 22.6% | 22.6% | 22.6% | 22.6% | 22.6% | 22.6% | 22.6% | 22.6% | 22.4% | 22.4% | 22.4% | 22.6% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| loopback-http wire json | 39313 calls, 0.00 ms | - | - | 27632 calls, 1.70 ms | - |
| loopback-http wire gzip | 39312 calls, 0.00 ms | - | - | 27633 calls, 1.81 ms | - |
| loopback-http wire bson | 39315 calls, 0.00 ms | - | - | 27630 calls, 1.74 ms | - |
| loopback-http wire bson-gzip | 39313 calls, 0.00 ms | - | - | 27632 calls, 1.86 ms | - |
| groups wire json | 39313 calls, 0.01 ms | 23483 calls, 0.29 ms | - | - | 4149 calls, 5.28 ms |
| groups wire gzip | 39312 calls, 0.01 ms | 23483 calls, 0.29 ms | - | - | 4150 calls, 5.38 ms |
| groups wire bson | 39312 calls, 0.01 ms | 23483 calls, 0.29 ms | - | - | 4150 calls, 5.30 ms |
| groups wire bson-gzip | 39312 calls, 0.01 ms | 23483 calls, 0.28 ms | - | - | 4150 calls, 5.32 ms |
| microservices wire json | 38918 calls, 0.00 ms | - | - | - | 28027 calls, 1.98 ms |
| microservices wire gzip | 38916 calls, 0.00 ms | - | - | - | 28029 calls, 2.04 ms |
| microservices wire bson | 38919 calls, 0.00 ms | - | - | - | 28026 calls, 2.01 ms |
| microservices wire bson-gzip | 38910 calls, 0.00 ms | - | - | - | 28035 calls, 2.08 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### loopback-http (cache on, parallel quotes on, async events)

500 orders in 9.5 s = 52.8 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 50.2 | 75.6 | 74.0 | 88.2 | 97.7 | 106.1 | 118.7 |
| placeOrder | declined | 11 | 49.9 | 70.4 | 64.7 | 81.3 | 133.0 | 133.0 | 133.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1318 | 53.2% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 500 |  | 30.9 | 29.7 | 40.1 | 45.9 | 48.1 |
| shipping -> inventory.getStock [loopback-http] | 20000 |  | 0.7 | 0.6 | 1.2 | 1.7 | 6.2 |
| orders -> inventory.reserve [loopback-http] | 500 |  | 11.3 | 10.7 | 15.4 | 19.3 | 29.0 |
| orders -> shipping.createShipment [loopback-http] | 489 |  | 8.9 | 8.6 | 11.9 | 13.3 | 14.8 |
| orders -> pricing.quote [loopback-http] | 500 |  | 3.6 | 2.8 | 7.4 | 11.2 | 16.9 |
| orders -> payments.authorize [loopback-http] | 500 | 11 | 1.8 | 1.8 | 2.4 | 5.4 | 6.0 |
| pricing -> catalog.getProduct [loopback-http] | 1059 |  | 0.7 | 0.7 | 0.9 | 3.8 | 5.0 |
| inventory -> catalog.productExists [loopback-http] | 1059 |  | 0.7 | 0.7 | 0.9 | 1.0 | 5.3 |
| orders -> notifications.send [loopback-http] | 489 |  | 1.3 | 1.2 | 1.6 | 4.4 | 5.7 |
| shipping -> inventory.getReservation [loopback-http] | 489 |  | 1.2 | 1.1 | 1.5 | 4.1 | 5.6 |
| orders -> payments.capture [loopback-http] | 489 |  | 1.1 | 1.0 | 1.3 | 4.1 | 5.8 |
| orders -> customers.getCustomer [loopback-http] | 387 |  | 0.8 | 0.8 | 1.0 | 3.9 | 4.1 |
| payments -> customers.getPaymentMethod [loopback-http] | 387 |  | 0.7 | 0.7 | 0.9 | 1.1 | 4.3 |
| shipping -> customers.getCustomer [loopback-http] | 387 |  | 0.7 | 0.7 | 0.9 | 3.7 | 4.2 |
| pricing -> customers.getCustomer [loopback-http] | 386 |  | 0.7 | 0.7 | 0.9 | 3.6 | 4.1 |
| orders -> inventory.release [loopback-http] | 11 |  | 16.1 | 16.1 | 23.8 | 23.8 | 23.8 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.1 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.8 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 114 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39313 calls, mean 0.00 ms, loopback-http 27632 calls, mean 1.70 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/loopback-http-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events)

500 orders in 10.0 s = 50.2 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 53.0 | 79.5 | 75.3 | 95.6 | 103.1 | 110.1 | 123.6 |
| placeOrder | declined | 11 | 52.2 | 71.2 | 60.3 | 75.4 | 141.5 | 141.5 | 141.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1317 | 53.1% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 500 |  | 33.6 | 32.5 | 44.1 | 47.8 | 51.1 |
| shipping -> inventory.getStock [loopback-http] | 20000 |  | 0.8 | 0.7 | 1.3 | 1.9 | 6.1 |
| orders -> inventory.reserve [loopback-http] | 500 |  | 11.1 | 10.5 | 15.0 | 18.3 | 33.2 |
| orders -> shipping.createShipment [loopback-http] | 489 |  | 9.0 | 8.7 | 11.8 | 13.3 | 16.2 |
| orders -> pricing.quote [loopback-http] | 500 |  | 3.9 | 3.0 | 8.1 | 11.9 | 15.0 |
| orders -> payments.authorize [loopback-http] | 500 | 11 | 1.9 | 1.9 | 2.6 | 5.1 | 6.5 |
| pricing -> catalog.getProduct [loopback-http] | 1059 |  | 0.8 | 0.7 | 0.9 | 2.9 | 4.9 |
| inventory -> catalog.productExists [loopback-http] | 1059 |  | 0.8 | 0.7 | 0.9 | 1.3 | 4.9 |
| orders -> notifications.send [loopback-http] | 489 |  | 1.5 | 1.3 | 1.9 | 4.6 | 6.3 |
| shipping -> inventory.getReservation [loopback-http] | 489 |  | 1.3 | 1.2 | 1.6 | 3.9 | 4.8 |
| orders -> payments.capture [loopback-http] | 489 |  | 1.1 | 1.1 | 1.3 | 3.8 | 5.2 |
| orders -> customers.getCustomer [loopback-http] | 387 |  | 0.9 | 0.8 | 1.1 | 3.8 | 5.1 |
| pricing -> customers.getCustomer [loopback-http] | 387 |  | 0.8 | 0.7 | 1.0 | 3.7 | 3.9 |
| payments -> customers.getPaymentMethod [loopback-http] | 387 |  | 0.8 | 0.7 | 1.0 | 1.1 | 4.6 |
| shipping -> customers.getCustomer [loopback-http] | 387 |  | 0.8 | 0.7 | 1.0 | 1.2 | 3.8 |
| orders -> inventory.release [loopback-http] | 11 |  | 14.9 | 14.9 | 24.4 | 24.4 | 24.4 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39312 calls, mean 0.00 ms, loopback-http 27633 calls, mean 1.81 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/loopback-http-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events)

500 orders in 9.6 s = 51.8 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 51.2 | 77.2 | 72.5 | 91.8 | 99.3 | 110.7 | 144.3 |
| placeOrder | declined | 11 | 50.2 | 62.9 | 59.6 | 79.6 | 89.7 | 89.7 | 89.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1320 | 53.3% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 500 |  | 31.8 | 30.8 | 41.0 | 46.1 | 51.5 |
| shipping -> inventory.getStock [loopback-http] | 20000 |  | 0.7 | 0.7 | 1.3 | 1.7 | 6.1 |
| orders -> inventory.reserve [loopback-http] | 500 |  | 11.2 | 10.7 | 14.8 | 18.4 | 42.4 |
| orders -> shipping.createShipment [loopback-http] | 489 |  | 9.0 | 8.7 | 11.8 | 13.3 | 17.1 |
| orders -> pricing.quote [loopback-http] | 500 |  | 3.7 | 3.0 | 7.9 | 9.8 | 14.6 |
| orders -> payments.authorize [loopback-http] | 500 | 11 | 1.9 | 1.9 | 2.5 | 5.2 | 6.0 |
| inventory -> catalog.productExists [loopback-http] | 1059 |  | 0.7 | 0.7 | 0.9 | 3.6 | 5.1 |
| pricing -> catalog.getProduct [loopback-http] | 1059 |  | 0.7 | 0.7 | 0.9 | 1.0 | 4.5 |
| orders -> notifications.send [loopback-http] | 489 |  | 1.4 | 1.3 | 1.7 | 4.7 | 6.2 |
| shipping -> inventory.getReservation [loopback-http] | 489 |  | 1.3 | 1.2 | 1.6 | 3.7 | 5.9 |
| orders -> payments.capture [loopback-http] | 489 |  | 1.0 | 1.0 | 1.2 | 1.5 | 5.0 |
| orders -> customers.getCustomer [loopback-http] | 387 |  | 0.8 | 0.8 | 1.1 | 4.1 | 4.8 |
| shipping -> customers.getCustomer [loopback-http] | 385 |  | 0.8 | 0.7 | 1.0 | 3.9 | 4.9 |
| pricing -> customers.getCustomer [loopback-http] | 386 |  | 0.8 | 0.7 | 1.0 | 3.7 | 4.9 |
| payments -> customers.getPaymentMethod [loopback-http] | 387 |  | 0.8 | 0.7 | 0.9 | 1.1 | 4.0 |
| orders -> inventory.release [loopback-http] | 11 |  | 14.5 | 11.7 | 22.5 | 22.5 | 22.5 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 604 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 114 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39315 calls, mean 0.00 ms, loopback-http 27630 calls, mean 1.74 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/loopback-http-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### loopback-http (cache on, parallel quotes on, async events)

500 orders in 10.1 s = 49.3 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 63.9 | 81.0 | 80.2 | 92.3 | 98.5 | 122.7 | 155.3 |
| placeOrder | declined | 11 | 57.6 | 74.4 | 71.6 | 86.1 | 104.3 | 104.3 | 104.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1318 | 53.2% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [loopback-http] | 500 |  | 34.5 | 33.5 | 44.8 | 49.7 | 51.4 |
| shipping -> inventory.getStock [loopback-http] | 20000 |  | 0.8 | 0.7 | 1.4 | 2.0 | 6.3 |
| orders -> inventory.reserve [loopback-http] | 500 |  | 11.3 | 10.8 | 14.9 | 18.1 | 31.5 |
| orders -> shipping.createShipment [loopback-http] | 489 |  | 9.0 | 8.7 | 11.8 | 14.2 | 15.3 |
| orders -> pricing.quote [loopback-http] | 500 |  | 4.0 | 3.3 | 8.3 | 11.0 | 17.5 |
| orders -> payments.authorize [loopback-http] | 500 | 11 | 2.0 | 2.0 | 2.7 | 5.5 | 8.0 |
| inventory -> catalog.productExists [loopback-http] | 1059 |  | 0.8 | 0.7 | 1.0 | 3.8 | 5.0 |
| pricing -> catalog.getProduct [loopback-http] | 1059 |  | 0.8 | 0.8 | 1.0 | 3.3 | 5.2 |
| orders -> notifications.send [loopback-http] | 489 |  | 1.5 | 1.4 | 1.8 | 4.5 | 5.2 |
| shipping -> inventory.getReservation [loopback-http] | 489 |  | 1.4 | 1.3 | 1.7 | 4.3 | 5.2 |
| orders -> payments.capture [loopback-http] | 489 |  | 1.1 | 1.1 | 1.4 | 3.6 | 4.0 |
| orders -> customers.getCustomer [loopback-http] | 387 |  | 0.9 | 0.8 | 1.1 | 2.1 | 4.1 |
| shipping -> customers.getCustomer [loopback-http] | 387 |  | 0.8 | 0.8 | 1.0 | 3.7 | 4.3 |
| payments -> customers.getPaymentMethod [loopback-http] | 387 |  | 0.8 | 0.8 | 1.0 | 1.2 | 4.6 |
| pricing -> customers.getCustomer [loopback-http] | 386 |  | 0.8 | 0.8 | 1.0 | 3.7 | 4.4 |
| orders -> inventory.release [loopback-http] | 11 |  | 16.7 | 16.4 | 32.0 | 32.0 | 32.0 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.7 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 114 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39313 calls, mean 0.00 ms, loopback-http 27632 calls, mean 1.86 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/loopback-http-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

500 orders in 6.8 s = 73.3 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 42.1 | 54.6 | 51.1 | 66.3 | 69.6 | 103.0 | 118.1 |
| placeOrder | declined | 11 | 37.7 | 41.4 | 40.4 | 45.2 | 50.3 | 50.3 | 50.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1318 | 53.2% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 13.3 | 12.2 | 17.9 | 21.0 | 70.5 |
| orders -> inventory.reserve [remote] | 500 |  | 12.0 | 11.2 | 16.6 | 23.4 | 56.7 |
| shipping -> inventory.getStock [local] | 20000 |  | 0.3 | 0.2 | 0.4 | 0.6 | 4.7 |
| orders -> shipping.createShipment [remote] | 489 |  | 9.0 | 8.6 | 12.0 | 13.5 | 54.5 |
| orders -> pricing.quote [remote] | 500 |  | 3.5 | 2.9 | 6.2 | 8.4 | 55.7 |
| orders -> payments.authorize [remote] | 500 | 11 | 2.0 | 1.8 | 2.9 | 3.7 | 43.4 |
| orders -> payments.capture [remote] | 489 |  | 1.4 | 1.2 | 1.8 | 2.6 | 44.7 |
| orders -> notifications.send [local] | 489 |  | 1.1 | 0.9 | 1.4 | 2.0 | 56.4 |
| pricing -> customers.getCustomer [remote] | 386 |  | 1.2 | 0.9 | 1.6 | 4.5 | 51.4 |
| shipping -> customers.getCustomer [remote] | 387 |  | 1.1 | 0.9 | 1.7 | 4.3 | 52.9 |
| shipping -> inventory.getReservation [local] | 489 |  | 0.8 | 0.7 | 1.1 | 3.4 | 4.9 |
| orders -> customers.getCustomer [remote] | 387 |  | 1.0 | 0.9 | 1.3 | 2.6 | 2.9 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| inventory -> catalog.productExists [local] | 1059 |  | 0.3 | 0.2 | 0.3 | 0.4 | 3.8 |
| pricing -> catalog.getProduct [local] | 1059 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.5 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.2 |
| orders -> inventory.release [remote] | 11 |  | 17.0 | 16.9 | 23.7 | 23.7 | 23.7 |
| payments -> customers.getPaymentMethod [local] | 387 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.8 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 114 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39313 calls, mean 0.01 ms, local 23483 calls, mean 0.29 ms, remote 4149 calls, mean 5.28 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/groups-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

500 orders in 6.9 s = 72.3 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 41.2 | 55.0 | 51.2 | 67.0 | 70.3 | 97.7 | 142.9 |
| placeOrder | declined | 11 | 34.1 | 54.7 | 40.8 | 101.3 | 131.8 | 131.8 | 131.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1317 | 53.1% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 13.5 | 12.3 | 18.0 | 21.2 | 89.8 |
| orders -> inventory.reserve [remote] | 500 |  | 12.2 | 11.2 | 16.8 | 24.7 | 87.6 |
| shipping -> inventory.getStock [local] | 20000 |  | 0.3 | 0.2 | 0.4 | 0.6 | 4.5 |
| orders -> shipping.createShipment [remote] | 489 |  | 9.0 | 8.6 | 11.3 | 12.9 | 55.6 |
| orders -> pricing.quote [remote] | 500 |  | 3.6 | 3.0 | 6.4 | 8.3 | 77.9 |
| orders -> payments.authorize [remote] | 500 | 11 | 2.0 | 1.9 | 3.1 | 4.0 | 8.3 |
| orders -> payments.capture [remote] | 489 |  | 1.4 | 1.3 | 2.0 | 2.8 | 4.0 |
| orders -> notifications.send [local] | 489 |  | 1.0 | 0.9 | 1.5 | 2.7 | 7.6 |
| shipping -> customers.getCustomer [remote] | 387 |  | 1.3 | 0.9 | 2.0 | 2.7 | 72.2 |
| pricing -> customers.getCustomer [remote] | 387 |  | 1.3 | 0.9 | 1.9 | 2.8 | 72.2 |
| orders -> customers.getCustomer [remote] | 387 |  | 1.1 | 1.0 | 1.6 | 2.7 | 3.0 |
| shipping -> inventory.getReservation [local] | 489 |  | 0.8 | 0.7 | 1.0 | 2.5 | 3.9 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.0 |
| inventory -> catalog.productExists [local] | 1059 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.1 |
| pricing -> catalog.getProduct [local] | 1059 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.5 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.0 |
| orders -> inventory.release [remote] | 11 |  | 17.9 | 16.4 | 34.3 | 34.3 | 34.3 |
| payments -> customers.getPaymentMethod [local] | 387 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.2 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.1 | 0.0 | 0.1 | 0.1 | 0.2 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39312 calls, mean 0.01 ms, local 23483 calls, mean 0.29 ms, remote 4150 calls, mean 5.38 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/groups-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

500 orders in 6.8 s = 73.3 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 41.4 | 54.3 | 50.9 | 66.5 | 70.1 | 109.7 | 156.0 |
| placeOrder | declined | 11 | 35.8 | 52.8 | 39.5 | 57.7 | 162.3 | 162.3 | 162.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1317 | 53.1% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 13.4 | 12.3 | 17.7 | 21.7 | 89.3 |
| orders -> inventory.reserve [remote] | 500 |  | 11.9 | 11.1 | 16.4 | 24.7 | 82.0 |
| shipping -> inventory.getStock [local] | 20000 |  | 0.3 | 0.2 | 0.4 | 0.6 | 63.3 |
| orders -> shipping.createShipment [remote] | 489 |  | 8.8 | 8.5 | 11.1 | 12.9 | 15.1 |
| orders -> pricing.quote [remote] | 500 |  | 3.6 | 3.0 | 6.3 | 8.8 | 71.5 |
| orders -> payments.authorize [remote] | 500 | 11 | 2.1 | 1.8 | 3.3 | 4.1 | 54.9 |
| orders -> payments.capture [remote] | 489 |  | 1.4 | 1.3 | 2.0 | 3.4 | 3.6 |
| orders -> notifications.send [local] | 489 |  | 1.1 | 0.9 | 1.6 | 3.3 | 56.7 |
| pricing -> customers.getCustomer [remote] | 387 |  | 1.2 | 0.9 | 1.8 | 3.0 | 64.8 |
| shipping -> customers.getCustomer [remote] | 387 |  | 1.2 | 0.9 | 1.7 | 2.7 | 65.3 |
| orders -> customers.getCustomer [remote] | 387 |  | 1.0 | 0.9 | 1.5 | 3.2 | 4.7 |
| shipping -> inventory.getReservation [local] | 489 |  | 0.8 | 0.7 | 1.0 | 1.3 | 4.7 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.8 |
| inventory -> catalog.productExists [local] | 1059 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| pricing -> catalog.getProduct [local] | 1059 |  | 0.2 | 0.2 | 0.3 | 0.4 | 3.2 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.9 |
| orders -> inventory.release [remote] | 11 |  | 17.5 | 16.7 | 34.4 | 34.4 | 34.4 |
| payments -> customers.getPaymentMethod [local] | 387 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.0 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.1 | 0.0 | 0.1 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.4 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39312 calls, mean 0.01 ms, local 23483 calls, mean 0.29 ms, remote 4150 calls, mean 5.30 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/groups-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events)

500 orders in 6.9 s = 72.8 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 42.3 | 54.9 | 52.0 | 67.7 | 70.8 | 106.2 | 111.9 |
| placeOrder | declined | 11 | 34.5 | 42.9 | 42.0 | 47.5 | 54.5 | 54.5 | 54.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18941 | 94.7% |
| catalog.productExists | 20000 | 18941 | 94.7% |
| customers.getCustomer | 2478 | 1317 | 53.1% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 13.3 | 12.4 | 17.9 | 21.1 | 63.6 |
| orders -> inventory.reserve [remote] | 500 |  | 12.1 | 11.3 | 16.3 | 21.1 | 62.5 |
| shipping -> inventory.getStock [local] | 20000 |  | 0.3 | 0.2 | 0.4 | 0.6 | 4.8 |
| orders -> shipping.createShipment [remote] | 489 |  | 8.9 | 8.6 | 11.3 | 13.3 | 13.8 |
| orders -> pricing.quote [remote] | 500 |  | 3.7 | 3.3 | 6.4 | 9.5 | 12.3 |
| orders -> payments.authorize [remote] | 500 | 11 | 2.0 | 1.9 | 3.2 | 3.9 | 4.9 |
| orders -> payments.capture [remote] | 489 |  | 1.4 | 1.4 | 2.1 | 3.0 | 4.4 |
| orders -> notifications.send [local] | 489 |  | 1.1 | 0.9 | 1.5 | 1.8 | 54.7 |
| shipping -> customers.getCustomer [remote] | 387 |  | 1.1 | 1.0 | 1.8 | 2.8 | 4.9 |
| orders -> customers.getCustomer [remote] | 387 |  | 1.1 | 1.0 | 1.6 | 4.2 | 6.1 |
| pricing -> customers.getCustomer [remote] | 387 |  | 1.1 | 1.0 | 1.9 | 2.6 | 4.4 |
| shipping -> inventory.getReservation [local] | 489 |  | 0.8 | 0.8 | 1.1 | 1.6 | 4.4 |
| inventory -> catalog.productExists [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 4.4 |
| inventory -> catalog.productExists [local] | 1059 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.0 |
| pricing -> catalog.getProduct [local] | 1059 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.0 |
| pricing -> catalog.getProduct [cache] | 18941 |  | 0.0 | 0.0 | 0.0 | 0.0 | 3.0 |
| orders -> inventory.release [remote] | 11 |  | 16.6 | 16.8 | 23.0 | 23.0 | 23.0 |
| payments -> customers.getPaymentMethod [local] | 387 |  | 0.3 | 0.3 | 0.5 | 0.7 | 0.9 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.1 | 0.0 | 0.1 | 0.1 | 0.2 |
| notifications -> customers.getCustomer [cache] | 489 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 39312 calls, mean 0.01 ms, local 23483 calls, mean 0.28 ms, remote 4150 calls, mean 5.32 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/groups-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

500 orders in 10.7 s = 46.9 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 54.0 | 85.2 | 76.7 | 117.8 | 136.6 | 157.0 | 196.6 |
| placeOrder | declined | 11 | 49.8 | 71.5 | 58.4 | 98.2 | 150.8 | 150.8 | 150.8 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18935 | 94.7% |
| catalog.productExists | 20000 | 18936 | 94.7% |
| customers.getCustomer | 2478 | 935 | 37.7% |
| customers.getPaymentMethod | 500 | 112 | 22.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 33.0 | 31.0 | 44.6 | 57.1 | 101.7 |
| shipping -> inventory.getStock [remote] | 20000 |  | 0.7 | 0.7 | 1.4 | 2.1 | 54.3 |
| orders -> inventory.reserve [remote] | 500 |  | 11.9 | 11.2 | 17.4 | 20.7 | 66.4 |
| orders -> shipping.createShipment [remote] | 489 |  | 9.6 | 9.1 | 12.5 | 15.0 | 59.9 |
| orders -> pricing.quote [remote] | 500 |  | 5.8 | 4.9 | 11.3 | 18.2 | 67.5 |
| orders -> notifications.send [remote] | 489 |  | 4.3 | 3.2 | 7.5 | 54.9 | 65.7 |
| orders -> payments.authorize [remote] | 500 | 11 | 3.8 | 3.2 | 6.4 | 8.6 | 63.9 |
| pricing -> catalog.getProduct [remote] | 1065 |  | 1.0 | 0.9 | 1.5 | 1.8 | 3.7 |
| inventory -> catalog.productExists [remote] | 1064 |  | 1.0 | 0.9 | 1.4 | 1.6 | 3.7 |
| orders -> payments.capture [remote] | 489 |  | 1.8 | 1.5 | 2.8 | 3.8 | 57.0 |
| notifications -> customers.getCustomer [remote] | 379 |  | 1.9 | 1.4 | 2.6 | 7.1 | 60.0 |
| shipping -> inventory.getReservation [remote] | 489 |  | 1.4 | 1.3 | 1.7 | 2.5 | 3.8 |
| payments -> customers.getPaymentMethod [remote] | 388 |  | 1.7 | 1.5 | 2.7 | 4.4 | 45.7 |
| orders -> customers.getCustomer [remote] | 388 |  | 1.3 | 1.1 | 1.9 | 3.2 | 56.9 |
| pricing -> customers.getCustomer [remote] | 388 |  | 1.3 | 1.0 | 1.9 | 3.4 | 55.4 |
| shipping -> customers.getCustomer [remote] | 388 |  | 1.1 | 0.9 | 1.5 | 2.9 | 52.6 |
| orders -> inventory.release [remote] | 11 |  | 14.8 | 13.0 | 24.5 | 24.5 | 24.5 |
| inventory -> catalog.productExists [cache] | 18936 |  | 0.0 | 0.0 | 0.0 | 0.0 | 1.9 |
| pricing -> catalog.getProduct [cache] | 18935 |  | 0.0 | 0.0 | 0.0 | 0.0 | 2.4 |
| shipping -> customers.getCustomer [cache] | 601 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 110 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 38918 calls, mean 0.00 ms, remote 28027 calls, mean 1.98 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/microservices-wire-json` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

500 orders in 10.9 s = 45.9 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 51.7 | 87.3 | 76.3 | 119.0 | 126.7 | 160.7 | 220.8 |
| placeOrder | declined | 11 | 53.5 | 69.5 | 64.4 | 89.8 | 92.9 | 92.9 | 92.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18934 | 94.7% |
| catalog.productExists | 20000 | 18935 | 94.7% |
| customers.getCustomer | 2478 | 935 | 37.7% |
| customers.getPaymentMethod | 500 | 112 | 22.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 34.7 | 33.3 | 44.1 | 59.0 | 91.1 |
| shipping -> inventory.getStock [remote] | 20000 |  | 0.8 | 0.7 | 1.5 | 2.3 | 53.7 |
| orders -> inventory.reserve [remote] | 500 |  | 11.5 | 10.7 | 16.1 | 20.2 | 26.6 |
| orders -> shipping.createShipment [remote] | 489 |  | 9.5 | 9.1 | 12.4 | 14.5 | 63.8 |
| orders -> pricing.quote [remote] | 500 |  | 5.7 | 4.8 | 11.2 | 15.0 | 55.0 |
| orders -> notifications.send [remote] | 489 |  | 4.4 | 3.4 | 7.5 | 51.5 | 65.6 |
| orders -> payments.authorize [remote] | 500 | 11 | 4.0 | 3.3 | 6.5 | 23.7 | 65.9 |
| pricing -> catalog.getProduct [remote] | 1066 |  | 1.0 | 1.0 | 1.6 | 1.9 | 3.9 |
| inventory -> catalog.productExists [remote] | 1065 |  | 1.0 | 0.9 | 1.4 | 1.8 | 3.8 |
| orders -> payments.capture [remote] | 489 |  | 1.9 | 1.6 | 2.9 | 4.0 | 49.7 |
| shipping -> inventory.getReservation [remote] | 489 |  | 1.4 | 1.4 | 1.8 | 2.3 | 3.7 |
| payments -> customers.getPaymentMethod [remote] | 388 |  | 1.7 | 1.4 | 2.6 | 3.7 | 47.3 |
| notifications -> customers.getCustomer [remote] | 379 |  | 1.6 | 1.3 | 2.6 | 3.3 | 49.2 |
| orders -> customers.getCustomer [remote] | 388 |  | 1.3 | 1.1 | 1.8 | 2.9 | 46.6 |
| pricing -> customers.getCustomer [remote] | 388 |  | 1.2 | 1.0 | 1.7 | 3.0 | 44.6 |
| shipping -> customers.getCustomer [remote] | 388 |  | 1.2 | 0.9 | 1.7 | 3.3 | 45.3 |
| orders -> inventory.release [remote] | 11 |  | 14.5 | 13.8 | 22.0 | 22.0 | 22.0 |
| inventory -> catalog.productExists [cache] | 18935 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| pricing -> catalog.getProduct [cache] | 18934 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| shipping -> customers.getCustomer [cache] | 601 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 110 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 38916 calls, mean 0.00 ms, remote 28029 calls, mean 2.04 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/microservices-wire-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

500 orders in 10.8 s = 46.5 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 53.9 | 86.1 | 77.6 | 116.0 | 126.6 | 154.8 | 176.4 |
| placeOrder | declined | 11 | 50.1 | 70.4 | 56.3 | 124.4 | 152.2 | 152.2 | 152.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18936 | 94.7% |
| catalog.productExists | 20000 | 18936 | 94.7% |
| customers.getCustomer | 2478 | 935 | 37.7% |
| customers.getPaymentMethod | 500 | 112 | 22.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 33.7 | 31.9 | 46.3 | 53.4 | 87.7 |
| shipping -> inventory.getStock [remote] | 20000 |  | 0.8 | 0.7 | 1.5 | 2.2 | 52.9 |
| orders -> inventory.reserve [remote] | 500 |  | 11.9 | 11.5 | 16.7 | 21.0 | 26.2 |
| orders -> shipping.createShipment [remote] | 489 |  | 9.7 | 9.3 | 12.3 | 14.1 | 60.4 |
| orders -> pricing.quote [remote] | 500 |  | 5.8 | 4.8 | 11.7 | 16.6 | 60.9 |
| orders -> notifications.send [remote] | 489 |  | 4.7 | 3.5 | 7.7 | 54.0 | 65.1 |
| orders -> payments.authorize [remote] | 500 | 11 | 3.6 | 3.4 | 6.2 | 8.3 | 42.2 |
| pricing -> catalog.getProduct [remote] | 1064 |  | 1.0 | 0.9 | 1.5 | 1.7 | 3.0 |
| inventory -> catalog.productExists [remote] | 1064 |  | 1.0 | 0.9 | 1.4 | 1.7 | 3.4 |
| orders -> payments.capture [remote] | 489 |  | 1.8 | 1.5 | 2.9 | 3.8 | 42.0 |
| shipping -> inventory.getReservation [remote] | 489 |  | 1.4 | 1.4 | 1.9 | 2.3 | 3.8 |
| notifications -> customers.getCustomer [remote] | 379 |  | 1.8 | 1.4 | 2.5 | 3.1 | 61.7 |
| payments -> customers.getPaymentMethod [remote] | 388 |  | 1.7 | 1.5 | 2.7 | 4.4 | 32.6 |
| orders -> customers.getCustomer [remote] | 388 |  | 1.3 | 1.1 | 1.9 | 3.3 | 55.3 |
| pricing -> customers.getCustomer [remote] | 388 |  | 1.2 | 1.0 | 1.7 | 2.4 | 52.9 |
| shipping -> customers.getCustomer [remote] | 388 |  | 1.0 | 0.9 | 1.6 | 2.8 | 3.4 |
| orders -> inventory.release [remote] | 11 |  | 15.2 | 14.5 | 31.2 | 31.2 | 31.2 |
| inventory -> catalog.productExists [cache] | 18936 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 18936 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 601 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 110 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| payments -> customers.getPaymentMethod [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 112 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 38919 calls, mean 0.00 ms, remote 28026 calls, mean 2.01 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/microservices-wire-bson` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events)

500 orders in 11.0 s = 45.3 orders/s; 489 ok, 11 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 489 | 55.4 | 88.2 | 79.1 | 120.1 | 129.0 | 154.2 | 170.0 |
| placeOrder | declined | 11 | 53.4 | 77.4 | 58.9 | 118.7 | 169.7 | 169.7 | 169.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 20000 | 18929 | 94.6% |
| catalog.productExists | 20000 | 18929 | 94.6% |
| customers.getCustomer | 2478 | 939 | 37.9% |
| customers.getPaymentMethod | 500 | 113 | 22.6% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 500 |  | 35.3 | 34.1 | 42.9 | 53.4 | 91.4 |
| shipping -> inventory.getStock [remote] | 20000 |  | 0.8 | 0.7 | 1.5 | 2.4 | 53.6 |
| orders -> inventory.reserve [remote] | 500 |  | 11.3 | 10.8 | 16.3 | 18.8 | 21.4 |
| orders -> shipping.createShipment [remote] | 489 |  | 9.6 | 9.1 | 12.5 | 15.0 | 66.0 |
| orders -> pricing.quote [remote] | 500 |  | 6.2 | 5.2 | 12.2 | 16.9 | 55.5 |
| orders -> notifications.send [remote] | 489 |  | 4.6 | 3.6 | 7.2 | 48.7 | 69.7 |
| orders -> payments.authorize [remote] | 500 | 11 | 4.1 | 3.6 | 6.6 | 15.6 | 57.8 |
| pricing -> catalog.getProduct [remote] | 1071 |  | 1.1 | 1.0 | 1.6 | 1.9 | 3.8 |
| inventory -> catalog.productExists [remote] | 1071 |  | 1.0 | 1.0 | 1.5 | 1.7 | 3.5 |
| orders -> payments.capture [remote] | 489 |  | 2.1 | 1.6 | 3.0 | 4.6 | 53.3 |
| shipping -> inventory.getReservation [remote] | 489 |  | 1.5 | 1.5 | 1.9 | 3.5 | 4.3 |
| payments -> customers.getPaymentMethod [remote] | 387 |  | 1.8 | 1.6 | 2.5 | 4.4 | 53.7 |
| notifications -> customers.getCustomer [remote] | 378 |  | 1.7 | 1.4 | 2.7 | 3.6 | 55.9 |
| pricing -> customers.getCustomer [remote] | 387 |  | 1.3 | 1.2 | 1.7 | 3.1 | 53.2 |
| orders -> customers.getCustomer [remote] | 387 |  | 1.3 | 1.2 | 2.1 | 3.6 | 4.3 |
| shipping -> customers.getCustomer [remote] | 387 |  | 1.1 | 1.1 | 1.7 | 2.7 | 3.2 |
| orders -> inventory.release [remote] | 11 |  | 15.1 | 12.8 | 24.5 | 24.5 | 24.5 |
| inventory -> catalog.productExists [cache] | 18929 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 18929 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 602 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| notifications -> customers.getCustomer [cache] | 111 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 113 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 38910 calls, mean 0.00 ms, remote 28035 calls, mean 2.08 ms. 133.9 module-to-module calls per order.

Raw samples: `.demo/perf/20261024T180619Z/microservices-wire-bson-gzip` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
