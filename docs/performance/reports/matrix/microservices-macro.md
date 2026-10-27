# Performance run 20261027T183917Z

- Started: 2026-10-27T18:39:17+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes microservices-macro --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/microservices-macro`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices-macro cache off parallel off async outbox dbs shared | 114 | 1473 | 27 | 47.0 | 139.6 | 132.3 | 191.3 | 210.1 | 238.6 | 265.4 | 19.9 | PASS |
| microservices-macro cache off parallel off async outbox dbs split | 114 | 1473 | 27 | 51.6 | 140.4 | 134.2 | 191.6 | 211.7 | 245.5 | 264.9 | 19.9 | PASS |
| microservices-macro cache off parallel off async events dbs shared | 111 | 1473 | 27 | 67.6 | 143.9 | 137.6 | 197.1 | 216.9 | 244.5 | 274.6 | 19.9 | PASS |
| microservices-macro cache off parallel off async events dbs split | 110 | 1473 | 27 | 52.8 | 145.3 | 139.1 | 194.2 | 217.1 | 251.4 | 275.3 | 19.9 | PASS |
| microservices-macro cache off parallel on async outbox dbs shared | 136 | 1473 | 27 | 41.1 | 117.0 | 114.5 | 149.5 | 163.8 | 204.3 | 239.7 | 19.9 | PASS |
| microservices-macro cache off parallel on async outbox dbs split | 132 | 1473 | 27 | 46.9 | 121.0 | 116.3 | 159.7 | 177.0 | 211.8 | 251.0 | 19.9 | PASS |
| microservices-macro cache off parallel on async events dbs shared | 130 | 1473 | 27 | 46.3 | 122.4 | 118.7 | 160.1 | 177.4 | 218.2 | 235.2 | 19.9 | PASS |
| microservices-macro cache off parallel on async events dbs split | 129 | 1473 | 27 | 41.5 | 123.9 | 118.0 | 162.8 | 181.6 | 224.0 | 242.3 | 19.9 | PASS |
| microservices-macro cache on parallel off async outbox dbs shared | 139 | 1473 | 27 | 45.9 | 114.7 | 107.2 | 152.4 | 169.7 | 209.5 | 247.0 | 19.9 | PASS |
| microservices-macro cache on parallel off async outbox dbs split | 135 | 1473 | 27 | 40.4 | 117.9 | 111.6 | 158.3 | 176.7 | 211.5 | 235.1 | 19.9 | PASS |
| microservices-macro cache on parallel off async events dbs shared | 133 | 1473 | 27 | 45.5 | 119.5 | 114.8 | 160.9 | 179.5 | 213.9 | 253.1 | 19.9 | PASS |
| microservices-macro cache on parallel off async events dbs split | 131 | 1473 | 27 | 40.7 | 121.5 | 114.0 | 165.1 | 178.9 | 217.2 | 252.9 | 19.9 | PASS |
| microservices-macro cache on parallel on async outbox dbs shared | 161 | 1473 | 27 | 40.4 | 99.3 | 94.4 | 132.5 | 146.1 | 170.7 | 193.5 | 19.9 | PASS |
| microservices-macro cache on parallel on async outbox dbs split | 157 | 1473 | 27 | 42.3 | 101.7 | 97.5 | 137.0 | 150.5 | 174.0 | 198.9 | 19.9 | PASS |
| microservices-macro cache on parallel on async events dbs shared | 150 | 1473 | 27 | 48.3 | 106.3 | 101.2 | 139.8 | 149.6 | 171.8 | 196.9 | 19.9 | PASS |
| microservices-macro cache on parallel on async events dbs split | 150 | 1473 | 27 | 38.3 | 106.2 | 101.7 | 139.3 | 148.8 | 190.4 | 218.2 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices-macro (parallel off) (async outbox) (dbs shared) | 114 | 139 | +22% | 132.3 | 107.2 | -19% | 210.1 | 169.7 | -19% | 238.6 | 209.5 | -12% |
| microservices-macro (parallel off) (async outbox) (dbs split) | 114 | 135 | +19% | 134.2 | 111.6 | -17% | 211.7 | 176.7 | -17% | 245.5 | 211.5 | -14% |
| microservices-macro (parallel off) (async events) (dbs shared) | 111 | 133 | +20% | 137.6 | 114.8 | -17% | 216.9 | 179.5 | -17% | 244.5 | 213.9 | -13% |
| microservices-macro (parallel off) (async events) (dbs split) | 110 | 131 | +20% | 139.1 | 114.0 | -18% | 217.1 | 178.9 | -18% | 251.4 | 217.2 | -14% |
| microservices-macro (parallel on) (async outbox) (dbs shared) | 136 | 161 | +18% | 114.5 | 94.4 | -18% | 163.8 | 146.1 | -11% | 204.3 | 170.7 | -16% |
| microservices-macro (parallel on) (async outbox) (dbs split) | 132 | 157 | +19% | 116.3 | 97.5 | -16% | 177.0 | 150.5 | -15% | 211.8 | 174.0 | -18% |
| microservices-macro (parallel on) (async events) (dbs shared) | 130 | 150 | +15% | 118.7 | 101.2 | -15% | 177.4 | 149.6 | -16% | 218.2 | 171.8 | -21% |
| microservices-macro (parallel on) (async events) (dbs split) | 129 | 150 | +17% | 118.0 | 101.7 | -14% | 181.6 | 148.8 | -18% | 224.0 | 190.4 | -15% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices-macro (cache off) (async outbox) (dbs shared) | 114 | 136 | +19% | 132.3 | 114.5 | -13% | 210.1 | 163.8 | -22% | 238.6 | 204.3 | -14% |
| microservices-macro (cache off) (async outbox) (dbs split) | 114 | 132 | +16% | 134.2 | 116.3 | -13% | 211.7 | 177.0 | -16% | 245.5 | 211.8 | -14% |
| microservices-macro (cache off) (async events) (dbs shared) | 111 | 130 | +18% | 137.6 | 118.7 | -14% | 216.9 | 177.4 | -18% | 244.5 | 218.2 | -11% |
| microservices-macro (cache off) (async events) (dbs split) | 110 | 129 | +17% | 139.1 | 118.0 | -15% | 217.1 | 181.6 | -16% | 251.4 | 224.0 | -11% |
| microservices-macro (cache on) (async outbox) (dbs shared) | 139 | 161 | +16% | 107.2 | 94.4 | -12% | 169.7 | 146.1 | -14% | 209.5 | 170.7 | -18% |
| microservices-macro (cache on) (async outbox) (dbs split) | 135 | 157 | +16% | 111.6 | 97.5 | -13% | 176.7 | 150.5 | -15% | 211.5 | 174.0 | -18% |
| microservices-macro (cache on) (async events) (dbs shared) | 133 | 150 | +12% | 114.8 | 101.2 | -12% | 179.5 | 149.6 | -17% | 213.9 | 171.8 | -20% |
| microservices-macro (cache on) (async events) (dbs split) | 131 | 150 | +14% | 114.0 | 101.7 | -11% | 178.9 | 148.8 | -17% | 217.2 | 190.4 | -12% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices-macro (cache off) (parallel off) (dbs shared) | 114 | 111 | -3% | 132.3 | 137.6 | +4% | 210.1 | 216.9 | +3% | 238.6 | 244.5 | +3% |
| microservices-macro (cache off) (parallel off) (dbs split) | 114 | 110 | -3% | 134.2 | 139.1 | +4% | 211.7 | 217.1 | +3% | 245.5 | 251.4 | +2% |
| microservices-macro (cache off) (parallel on) (dbs shared) | 136 | 130 | -4% | 114.5 | 118.7 | +4% | 163.8 | 177.4 | +8% | 204.3 | 218.2 | +7% |
| microservices-macro (cache off) (parallel on) (dbs split) | 132 | 129 | -2% | 116.3 | 118.0 | +1% | 177.0 | 181.6 | +3% | 211.8 | 224.0 | +6% |
| microservices-macro (cache on) (parallel off) (dbs shared) | 139 | 133 | -4% | 107.2 | 114.8 | +7% | 169.7 | 179.5 | +6% | 209.5 | 213.9 | +2% |
| microservices-macro (cache on) (parallel off) (dbs split) | 135 | 131 | -3% | 111.6 | 114.0 | +2% | 176.7 | 178.9 | +1% | 211.5 | 217.2 | +3% |
| microservices-macro (cache on) (parallel on) (dbs shared) | 161 | 150 | -7% | 94.4 | 101.2 | +7% | 146.1 | 149.6 | +2% | 170.7 | 171.8 | +1% |
| microservices-macro (cache on) (parallel on) (dbs split) | 157 | 150 | -4% | 97.5 | 101.7 | +4% | 150.5 | 148.8 | -1% | 174.0 | 190.4 | +9% |

## Database layout effect (one instance to one per deployable unit)

Higher orders/s and lower latencies are better.

| mode | orders/s shared | split | change | p50 shared | split | change | p95 shared | split | change | p99 shared | split | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices-macro (cache off) (parallel off) (async outbox) | 114 | 114 | -1% | 132.3 | 134.2 | +1% | 210.1 | 211.7 | +1% | 238.6 | 245.5 | +3% |
| microservices-macro (cache off) (parallel off) (async events) | 111 | 110 | -1% | 137.6 | 139.1 | +1% | 216.9 | 217.1 | +0% | 244.5 | 251.4 | +3% |
| microservices-macro (cache off) (parallel on) (async outbox) | 136 | 132 | -3% | 114.5 | 116.3 | +2% | 163.8 | 177.0 | +8% | 204.3 | 211.8 | +4% |
| microservices-macro (cache off) (parallel on) (async events) | 130 | 129 | -1% | 118.7 | 118.0 | -1% | 177.4 | 181.6 | +2% | 218.2 | 224.0 | +3% |
| microservices-macro (cache on) (parallel off) (async outbox) | 139 | 135 | -3% | 107.2 | 111.6 | +4% | 169.7 | 176.7 | +4% | 209.5 | 211.5 | +1% |
| microservices-macro (cache on) (parallel off) (async events) | 133 | 131 | -2% | 114.8 | 114.0 | -1% | 179.5 | 178.9 | -0% | 213.9 | 217.2 | +2% |
| microservices-macro (cache on) (parallel on) (async outbox) | 161 | 157 | -2% | 94.4 | 97.5 | +3% | 146.1 | 150.5 | +3% | 170.7 | 174.0 | +2% |
| microservices-macro (cache on) (parallel on) (async events) | 150 | 150 | +0% | 101.2 | 101.7 | +0% | 149.6 | 148.8 | -0% | 171.8 | 190.4 | +11% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices-macro cache off parallel off async outbox dbs shared | 20.0 | 5,787 | 5,787 | 100% | 130 to 130 | 159 to 159 |
| microservices-macro cache off parallel off async outbox dbs split | 20.0 | 5,787 | 5,787 | 100% | 130 to 130 | 159 to 159 |
| microservices-macro cache off parallel off async events dbs shared | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices-macro cache off parallel off async events dbs split | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices-macro cache off parallel on async outbox dbs shared | 20.1 | 5,794 | 5,794 | 100% | 130 to 130 | 159 to 159 |
| microservices-macro cache off parallel on async outbox dbs split | 20.1 | 5,794 | 5,794 | 100% | 130 to 130 | 159 to 159 |
| microservices-macro cache off parallel on async events dbs shared | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices-macro cache off parallel on async events dbs split | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices-macro cache on parallel off async outbox dbs shared | 14.3 | 4,546 | 4,546 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache on parallel off async outbox dbs split | 14.3 | 4,538 | 4,538 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache on parallel off async events dbs shared | 14.1 | 4,475 | 4,475 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache on parallel off async events dbs split | 14.1 | 4,475 | 4,475 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache on parallel on async outbox dbs shared | 14.3 | 4,556 | 4,556 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache on parallel on async outbox dbs split | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache on parallel on async events dbs shared | 14.1 | 4,475 | 4,475 | 100% | 145 to 145 | 173 to 173 |
| microservices-macro cache on parallel on async events dbs split | 14.1 | 4,476 | 4,476 | 100% | 145 to 145 | 173 to 173 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | microservices-macro cache off parallel off async outbox dbs shared | microservices-macro cache off parallel off async outbox dbs split | microservices-macro cache off parallel off async events dbs shared | microservices-macro cache off parallel off async events dbs split | microservices-macro cache off parallel on async outbox dbs shared | microservices-macro cache off parallel on async outbox dbs split | microservices-macro cache off parallel on async events dbs shared | microservices-macro cache off parallel on async events dbs split | microservices-macro cache on parallel off async outbox dbs shared | microservices-macro cache on parallel off async outbox dbs split | microservices-macro cache on parallel off async events dbs shared | microservices-macro cache on parallel off async events dbs split | microservices-macro cache on parallel on async outbox dbs shared | microservices-macro cache on parallel on async outbox dbs split | microservices-macro cache on parallel on async events dbs shared | microservices-macro cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 5.7 | 5.8 | 5.8 | 6.0 | 6.3 | 6.7 | 6.8 | 7.1 | 4.3 | 4.4 | 4.4 | 4.5 | 4.6 | 4.7 | 5.0 | 5.0 |
| orders -> pricing.quote | 4.6 | 4.6 | 4.9 | 4.9 | 5.0 | 5.2 | 5.4 | 5.6 | 3.2 | 3.2 | 3.4 | 3.4 | 3.5 | 3.5 | 3.7 | 3.5 |
| orders -> shipping.createShipment | 4.3 | 4.4 | 4.5 | 4.4 | 4.4 | 4.6 | 4.6 | 4.7 | 3.4 | 3.6 | 3.5 | 3.5 | 3.5 | 3.6 | 3.7 | 3.9 |
| shipping -> inventory.getStock | 2.0 | 2.1 | 2.1 | 2.1 | 2.2 | 2.4 | 2.4 | 2.6 | 1.4 | 1.5 | 1.5 | 1.5 | 1.5 | 1.5 | 1.7 | 1.6 |
| orders -> payments.authorize | 3.2 | 3.2 | 3.3 | 3.3 | 3.4 | 3.5 | 3.5 | 3.5 | 2.8 | 2.9 | 3.0 | 3.1 | 3.0 | 3.0 | 3.2 | 3.1 |
| orders -> notifications.send | 2.5 | 2.8 | 3.8 | 3.9 | 2.5 | 2.7 | 4.3 | 4.4 | 2.5 | 2.7 | 3.4 | 3.2 | 2.6 | 2.6 | 3.7 | 3.7 |
| orders -> inventory.reserve | 2.3 | 2.3 | 2.3 | 2.4 | 2.5 | 2.4 | 2.6 | 2.5 | 2.3 | 2.3 | 2.3 | 2.3 | 2.3 | 2.4 | 2.5 | 2.6 |
| inventory -> catalog.productExists | 1.0 | 1.0 | 1.1 | 1.1 | 1.2 | 1.3 | 1.3 | 1.4 | 0.4 | 0.4 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| orders -> payments.capture | 1.6 | 1.5 | 1.6 | 1.6 | 1.6 | 1.6 | 1.7 | 1.6 | 1.4 | 1.5 | 1.5 | 1.6 | 1.5 | 1.6 | 1.6 | 1.6 |
| pricing -> catalog.getProduct | 1.0 | 1.0 | 1.1 | 1.1 | 1.1 | 1.1 | 1.2 | 1.2 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.5 | 0.4 |
| shipping -> customers.getCustomer | 0.8 | 0.9 | 0.9 | 0.8 | 0.9 | 0.9 | 0.9 | 0.9 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| shipping -> inventory.getReservation | 1.1 | 1.0 | 1.2 | 1.1 | 1.1 | 1.1 | 1.1 | 1.2 | 1.1 | 1.1 | 1.1 | 1.1 | 1.1 | 1.2 | 1.2 | 1.2 |
| payments -> customers.getPaymentMethod | 1.2 | 1.2 | 1.2 | 1.2 | 1.2 | 1.3 | 1.3 | 1.3 | 0.9 | 0.9 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| notifications -> customers.getCustomer | 0.9 | 0.9 | 1.2 | 1.2 | 0.9 | 0.9 | 1.3 | 1.4 | 0.9 | 0.9 | 1.0 | 1.0 | 0.9 | 0.9 | 1.0 | 1.0 |
| pricing -> customers.getCustomer | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.1 | 1.1 | 1.1 | 0.8 | 0.8 | 0.8 | 0.9 | 0.9 | 0.9 | 1.0 | 0.9 |
| orders -> customers.getCustomer | 0.8 | 0.8 | 0.9 | 0.8 | 1.0 | 1.0 | 1.0 | 1.0 | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.8 | 0.8 | 0.8 |
| orders -> inventory.release | 2.3 | 2.3 | 2.0 | 2.2 | 2.4 | 2.5 | 2.4 | 2.1 | 2.3 | 2.3 | 2.0 | 2.0 | 2.5 | 2.4 | 2.4 | 2.5 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | microservices-macro cache off parallel off async outbox dbs shared | microservices-macro cache off parallel off async outbox dbs split | microservices-macro cache off parallel off async events dbs shared | microservices-macro cache off parallel off async events dbs split | microservices-macro cache off parallel on async outbox dbs shared | microservices-macro cache off parallel on async outbox dbs split | microservices-macro cache off parallel on async events dbs shared | microservices-macro cache off parallel on async events dbs split | microservices-macro cache on parallel off async outbox dbs shared | microservices-macro cache on parallel off async outbox dbs split | microservices-macro cache on parallel off async events dbs shared | microservices-macro cache on parallel off async events dbs split | microservices-macro cache on parallel on async outbox dbs shared | microservices-macro cache on parallel on async outbox dbs split | microservices-macro cache on parallel on async events dbs shared | microservices-macro cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 10.8 | 11.1 | 11.2 | 11.0 | 11.4 | 12.6 | 12.5 | 13.6 | 9.0 | 9.4 | 9.3 | 9.5 | 9.7 | 10.0 | 10.7 | 10.6 |
| orders -> pricing.quote | 8.0 | 8.0 | 8.5 | 8.5 | 8.4 | 8.7 | 9.2 | 9.6 | 6.6 | 6.7 | 7.1 | 6.9 | 7.3 | 7.6 | 7.8 | 7.4 |
| orders -> shipping.createShipment | 7.0 | 7.0 | 7.2 | 7.0 | 6.6 | 7.2 | 6.9 | 7.4 | 5.5 | 5.9 | 5.4 | 5.7 | 5.8 | 5.7 | 5.9 | 6.0 |
| shipping -> inventory.getStock | 3.4 | 3.5 | 3.6 | 3.6 | 3.8 | 4.2 | 4.2 | 4.6 | 3.5 | 3.7 | 3.5 | 3.7 | 3.8 | 3.9 | 4.2 | 4.1 |
| orders -> payments.authorize | 5.4 | 5.3 | 5.5 | 5.4 | 5.3 | 5.5 | 5.8 | 5.5 | 4.9 | 4.9 | 5.3 | 5.4 | 5.2 | 5.6 | 5.5 | 5.6 |
| orders -> notifications.send | 5.2 | 5.6 | 6.6 | 6.8 | 5.3 | 5.5 | 7.5 | 7.6 | 5.3 | 5.7 | 6.1 | 5.8 | 5.5 | 5.6 | 6.5 | 6.6 |
| orders -> inventory.reserve | 3.6 | 3.7 | 3.6 | 3.6 | 3.8 | 3.9 | 3.9 | 4.0 | 3.6 | 3.7 | 3.7 | 3.7 | 3.8 | 3.9 | 4.0 | 4.0 |
| inventory -> catalog.productExists | 1.7 | 1.8 | 1.9 | 1.8 | 2.2 | 2.3 | 2.3 | 2.6 | 2.2 | 2.3 | 2.2 | 2.3 | 2.5 | 2.5 | 2.7 | 2.6 |
| orders -> payments.capture | 2.6 | 2.5 | 2.5 | 2.4 | 2.6 | 2.6 | 2.7 | 2.5 | 2.2 | 2.3 | 2.5 | 2.7 | 2.5 | 2.7 | 2.6 | 2.6 |
| pricing -> catalog.getProduct | 1.9 | 1.8 | 2.0 | 1.9 | 1.8 | 1.9 | 2.0 | 2.2 | 2.0 | 2.1 | 2.2 | 2.3 | 2.2 | 2.2 | 2.5 | 2.4 |
| shipping -> customers.getCustomer | 1.5 | 1.6 | 1.6 | 1.5 | 1.6 | 1.6 | 1.7 | 1.7 | 1.5 | 1.4 | 1.4 | 1.5 | 1.5 | 1.6 | 1.6 | 1.6 |
| shipping -> inventory.getReservation | 1.8 | 1.8 | 1.9 | 1.9 | 1.7 | 1.9 | 1.8 | 2.0 | 1.8 | 1.9 | 1.9 | 2.0 | 1.9 | 2.0 | 2.0 | 2.1 |
| payments -> customers.getPaymentMethod | 2.1 | 2.2 | 2.2 | 2.1 | 2.2 | 2.2 | 2.2 | 2.2 | 2.2 | 2.1 | 2.3 | 2.3 | 2.3 | 2.6 | 2.4 | 2.5 |
| notifications -> customers.getCustomer | 1.7 | 1.8 | 2.2 | 2.1 | 1.7 | 1.7 | 2.4 | 2.4 | 1.9 | 2.1 | 2.4 | 2.2 | 2.0 | 2.1 | 2.4 | 2.4 |
| pricing -> customers.getCustomer | 1.6 | 1.6 | 1.8 | 1.7 | 1.6 | 1.7 | 2.0 | 1.9 | 1.8 | 1.8 | 1.9 | 1.9 | 2.0 | 2.2 | 2.2 | 2.1 |
| orders -> customers.getCustomer | 1.4 | 1.7 | 1.5 | 1.5 | 1.8 | 1.8 | 1.9 | 1.9 | 1.7 | 1.6 | 1.7 | 1.7 | 2.0 | 2.2 | 1.9 | 2.0 |
| orders -> inventory.release | 3.2 | 3.0 | 3.5 | 4.0 | 3.3 | 3.2 | 4.2 | 3.3 | 3.4 | 3.1 | 3.2 | 3.6 | 4.0 | 3.3 | 4.0 | 4.2 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | microservices-macro cache off parallel off async outbox dbs shared | microservices-macro cache off parallel off async outbox dbs split | microservices-macro cache off parallel off async events dbs shared | microservices-macro cache off parallel off async events dbs split | microservices-macro cache off parallel on async outbox dbs shared | microservices-macro cache off parallel on async outbox dbs split | microservices-macro cache off parallel on async events dbs shared | microservices-macro cache off parallel on async events dbs split | microservices-macro cache on parallel off async outbox dbs shared | microservices-macro cache on parallel off async outbox dbs split | microservices-macro cache on parallel off async events dbs shared | microservices-macro cache on parallel off async events dbs split | microservices-macro cache on parallel on async outbox dbs shared | microservices-macro cache on parallel on async outbox dbs split | microservices-macro cache on parallel on async events dbs shared | microservices-macro cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | - | - | - | - | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% |
| catalog.productExists | - | - | - | - | - | - | - | - | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% | 77.7% |
| customers.getCustomer | - | - | - | - | - | - | - | - | 46.0% | 45.9% | 48.1% | 48.1% | 45.9% | 45.9% | 48.1% | 48.0% |
| customers.getPaymentMethod | - | - | - | - | - | - | - | - | 35.3% | 35.3% | 35.3% | 35.3% | 35.2% | 35.2% | 35.3% | 35.2% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| microservices-macro cache off parallel off async outbox dbs shared | - | - | - | - | 29919 calls, 1.95 ms |
| microservices-macro cache off parallel off async outbox dbs split | - | - | - | - | 29919 calls, 1.97 ms |
| microservices-macro cache off parallel off async events dbs shared | - | - | - | - | 29919 calls, 2.10 ms |
| microservices-macro cache off parallel off async events dbs split | - | - | - | - | 29919 calls, 2.10 ms |
| microservices-macro cache off parallel on async outbox dbs shared | - | - | - | - | 29919 calls, 2.08 ms |
| microservices-macro cache off parallel on async outbox dbs split | - | - | - | - | 29919 calls, 2.18 ms |
| microservices-macro cache off parallel on async events dbs shared | - | - | - | - | 29919 calls, 2.32 ms |
| microservices-macro cache off parallel on async events dbs split | - | - | - | - | 29919 calls, 2.37 ms |
| microservices-macro cache on parallel off async outbox dbs shared | 8642 calls, 0.01 ms | - | - | - | 21277 calls, 2.06 ms |
| microservices-macro cache on parallel off async outbox dbs split | 8642 calls, 0.01 ms | - | - | - | 21277 calls, 2.13 ms |
| microservices-macro cache on parallel off async events dbs shared | 8800 calls, 0.01 ms | - | - | - | 21119 calls, 2.23 ms |
| microservices-macro cache on parallel off async events dbs split | 8799 calls, 0.01 ms | - | - | - | 21120 calls, 2.25 ms |
| microservices-macro cache on parallel on async outbox dbs shared | 8632 calls, 0.01 ms | - | - | - | 21287 calls, 2.19 ms |
| microservices-macro cache on parallel on async outbox dbs split | 8636 calls, 0.01 ms | - | - | - | 21283 calls, 2.24 ms |
| microservices-macro cache on parallel on async events dbs shared | 8801 calls, 0.01 ms | - | - | - | 21118 calls, 2.45 ms |
| microservices-macro cache on parallel on async events dbs split | 8795 calls, 0.01 ms | - | - | - | 21124 calls, 2.43 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### microservices-macro (cache off, parallel quotes off, async outbox, dbs shared)

1500 orders in 13.1 s = 114.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 47.0 | 139.6 | 132.3 | 191.3 | 210.1 | 238.6 | 265.4 |
| placeOrder | declined | 27 | 79.6 | 131.0 | 125.4 | 181.2 | 186.1 | 188.7 | 188.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.7 | 5.1 | 10.8 | 14.5 | 52.2 |
| orders -> pricing.quote [remote] | 1500 |  | 4.6 | 4.3 | 8.0 | 9.8 | 12.6 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.3 | 4.0 | 7.0 | 8.9 | 55.2 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.0 | 1.8 | 3.4 | 4.3 | 41.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.2 | 3.0 | 5.4 | 6.5 | 12.3 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.2 | 5.2 | 6.6 | 10.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.6 | 4.4 | 10.0 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.0 | 1.0 | 1.7 | 2.3 | 7.8 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.0 | 0.9 | 1.9 | 2.6 | 7.1 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.8 | 1.5 | 2.2 | 7.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.4 | 2.6 | 3.2 | 5.4 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.1 | 2.8 | 10.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 0.9 | 1.8 | 2.4 | 49.1 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.6 | 2.3 | 4.3 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.7 | 2.3 | 5.2 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.4 | 1.9 | 8.3 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.2 | 3.2 | 3.4 | 3.4 |

By binding: remote 29919 calls, mean 1.95 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes off, async outbox, dbs split)

1500 orders in 13.2 s = 113.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 51.6 | 140.4 | 134.2 | 191.6 | 211.7 | 245.5 | 264.9 |
| placeOrder | declined | 27 | 82.5 | 129.4 | 126.9 | 186.5 | 200.4 | 225.3 | 225.3 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.8 | 5.2 | 11.1 | 14.5 | 53.1 |
| orders -> pricing.quote [remote] | 1500 |  | 4.6 | 4.3 | 8.0 | 10.4 | 19.5 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.4 | 4.1 | 7.0 | 9.0 | 14.6 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.1 | 1.9 | 3.5 | 4.6 | 45.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.2 | 3.0 | 5.3 | 7.2 | 13.8 |
| orders -> notifications.send [remote] | 1473 |  | 2.8 | 2.3 | 5.6 | 7.1 | 11.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.1 | 3.7 | 4.5 | 5.4 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.0 | 1.0 | 1.8 | 2.4 | 8.1 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.0 | 0.9 | 1.8 | 2.4 | 9.0 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.2 | 6.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.2 | 6.2 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.2 | 2.9 | 9.5 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.0 | 0.9 | 1.8 | 2.4 | 9.0 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.6 | 2.3 | 9.2 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.8 | 2.2 | 7.0 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.7 | 2.3 | 7.0 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 3.0 | 3.1 | 3.1 |

By binding: remote 29919 calls, mean 1.97 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes off, async events, dbs shared)

1500 orders in 13.5 s = 110.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 67.6 | 143.9 | 137.6 | 197.1 | 216.9 | 244.5 | 274.6 |
| placeOrder | declined | 27 | 87.8 | 132.8 | 130.1 | 171.2 | 172.1 | 261.8 | 261.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.8 | 5.3 | 11.2 | 14.1 | 21.9 |
| orders -> pricing.quote [remote] | 1500 |  | 4.9 | 4.5 | 8.5 | 10.2 | 12.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.1 | 7.2 | 9.9 | 55.5 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.1 | 1.9 | 3.6 | 4.8 | 11.9 |
| orders -> notifications.send [remote] | 1473 |  | 3.8 | 3.4 | 6.6 | 8.3 | 57.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.3 | 3.1 | 5.5 | 6.8 | 12.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.6 | 4.9 | 6.5 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.1 | 1.0 | 1.9 | 2.7 | 10.3 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 2.0 | 2.7 | 7.5 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.4 | 9.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.5 | 3.6 | 5.8 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.2 | 2.9 | 7.3 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.2 | 1.1 | 2.2 | 2.9 | 7.4 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 1.9 | 2.7 | 50.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.8 | 2.4 | 4.4 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.9 | 0.7 | 1.5 | 2.7 | 51.7 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 1.9 | 3.5 | 3.9 | 3.9 |

By binding: remote 29919 calls, mean 2.10 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes off, async events, dbs split)

1500 orders in 13.7 s = 109.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 52.8 | 145.3 | 139.1 | 194.2 | 217.1 | 251.4 | 275.3 |
| placeOrder | declined | 27 | 91.0 | 136.3 | 127.2 | 170.2 | 230.1 | 231.5 | 231.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.0 | 5.4 | 11.0 | 14.5 | 61.9 |
| orders -> pricing.quote [remote] | 1500 |  | 4.9 | 4.5 | 8.5 | 10.3 | 14.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.4 | 4.1 | 7.0 | 8.7 | 51.0 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.1 | 1.9 | 3.6 | 4.6 | 54.7 |
| orders -> notifications.send [remote] | 1473 |  | 3.9 | 3.5 | 6.8 | 8.9 | 49.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.3 | 3.1 | 5.4 | 6.9 | 12.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.6 | 4.7 | 8.8 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.1 | 1.0 | 1.8 | 2.5 | 11.2 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 1.9 | 2.6 | 9.0 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.5 | 2.0 | 7.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.4 | 3.3 | 6.5 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.2 | 1.1 | 2.1 | 3.1 | 8.6 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.1 | 2.7 | 9.2 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.6 | 8.5 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.6 | 7.4 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.4 | 49.3 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 1.8 | 4.0 | 4.5 | 4.5 |

By binding: remote 29919 calls, mean 2.10 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async outbox, dbs shared)

1500 orders in 11.0 s = 136.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 41.1 | 117.0 | 114.5 | 149.5 | 163.8 | 204.3 | 239.7 |
| placeOrder | declined | 27 | 67.5 | 112.9 | 116.1 | 139.2 | 143.2 | 229.5 | 229.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.3 | 5.7 | 11.4 | 14.9 | 51.3 |
| orders -> pricing.quote [remote] | 1500 |  | 5.0 | 4.7 | 8.4 | 10.7 | 51.3 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.2 | 2.1 | 3.8 | 4.7 | 47.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.4 | 4.2 | 6.6 | 8.2 | 48.7 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.4 | 3.1 | 5.3 | 7.0 | 50.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 3.8 | 4.9 | 51.1 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.1 | 5.3 | 6.8 | 10.0 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.2 | 1.1 | 2.2 | 2.8 | 8.2 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 1.8 | 2.5 | 6.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.2 | 7.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.5 | 4.8 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.2 | 3.0 | 8.9 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.4 | 44.1 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 1.0 | 1.6 | 2.2 | 7.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.8 | 1.8 | 2.6 | 51.3 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.7 | 1.7 | 2.3 | 7.0 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.3 | 3.3 | 3.8 | 3.8 |

By binding: remote 29919 calls, mean 2.08 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async outbox, dbs split)

1500 orders in 11.4 s = 132.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 46.9 | 121.0 | 116.3 | 159.7 | 177.0 | 211.8 | 251.0 |
| placeOrder | declined | 27 | 73.2 | 109.0 | 104.0 | 131.2 | 136.3 | 228.5 | 228.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.7 | 5.9 | 12.6 | 16.5 | 65.2 |
| orders -> pricing.quote [remote] | 1500 |  | 5.2 | 4.8 | 8.7 | 10.9 | 58.6 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.4 | 2.2 | 4.2 | 5.8 | 52.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.6 | 4.3 | 7.2 | 9.3 | 53.7 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.5 | 3.3 | 5.5 | 6.8 | 11.0 |
| orders -> notifications.send [remote] | 1473 |  | 2.7 | 2.3 | 5.5 | 6.8 | 11.9 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.3 | 1.1 | 2.3 | 3.1 | 11.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.3 | 3.9 | 4.8 | 7.2 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 1.9 | 2.6 | 7.1 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.2 | 8.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.6 | 7.6 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.3 | 1.2 | 2.2 | 2.8 | 8.1 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.7 | 8.6 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.1 | 1.0 | 1.7 | 2.2 | 6.7 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.8 | 2.4 | 48.8 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.7 | 2.3 | 8.1 |
| orders -> inventory.release [remote] | 27 |  | 2.5 | 2.5 | 3.2 | 3.3 | 3.3 |

By binding: remote 29919 calls, mean 2.18 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async events, dbs shared)

1500 orders in 11.5 s = 130.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 46.3 | 122.4 | 118.7 | 160.1 | 177.4 | 218.2 | 235.2 |
| placeOrder | declined | 27 | 73.7 | 111.4 | 101.8 | 145.2 | 161.9 | 226.2 | 226.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.8 | 6.2 | 12.5 | 16.4 | 61.0 |
| orders -> pricing.quote [remote] | 1500 |  | 5.4 | 4.9 | 9.2 | 11.8 | 53.7 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.4 | 2.2 | 4.2 | 5.6 | 14.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.6 | 4.3 | 6.9 | 8.5 | 54.8 |
| orders -> notifications.send [remote] | 1473 |  | 4.3 | 3.8 | 7.5 | 10.0 | 50.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.5 | 3.2 | 5.8 | 7.0 | 12.3 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.3 | 1.2 | 2.3 | 3.3 | 11.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.6 | 2.4 | 3.9 | 5.1 | 51.2 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.2 | 1.1 | 2.0 | 2.8 | 7.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.7 | 2.3 | 8.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.7 | 1.5 | 2.7 | 3.5 | 5.7 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.3 | 1.2 | 2.4 | 3.5 | 10.0 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.3 | 1.1 | 2.2 | 3.0 | 8.8 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.1 | 1.0 | 2.0 | 2.8 | 7.4 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.6 | 50.4 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.9 | 3.0 | 43.3 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.1 | 4.2 | 4.7 | 4.7 |

By binding: remote 29919 calls, mean 2.32 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache off, parallel quotes on, async events, dbs split)

1500 orders in 11.7 s = 128.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 41.5 | 123.9 | 118.0 | 162.8 | 181.6 | 224.0 | 242.3 |
| placeOrder | declined | 27 | 76.9 | 114.9 | 112.4 | 144.1 | 173.1 | 227.4 | 227.4 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 7.1 | 6.3 | 13.6 | 17.1 | 56.5 |
| orders -> pricing.quote [remote] | 1500 |  | 5.6 | 5.1 | 9.6 | 12.0 | 49.9 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.6 | 2.3 | 4.6 | 6.2 | 55.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.7 | 4.4 | 7.4 | 8.9 | 55.7 |
| orders -> notifications.send [remote] | 1473 |  | 4.4 | 3.9 | 7.6 | 9.5 | 50.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.5 | 3.2 | 5.5 | 6.6 | 49.5 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.4 | 1.2 | 2.6 | 3.6 | 52.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.4 | 4.0 | 4.9 | 51.7 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.2 | 1.1 | 2.2 | 2.9 | 8.5 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.7 | 2.3 | 10.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.5 | 3.3 | 8.4 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.4 | 1.2 | 2.4 | 3.3 | 11.4 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.3 | 1.1 | 2.2 | 3.0 | 4.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 2.0 | 2.7 | 51.1 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.1 | 1.0 | 1.9 | 2.9 | 11.0 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.8 | 1.9 | 3.0 | 49.9 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 2.1 | 3.3 | 4.1 | 4.1 |

By binding: remote 29919 calls, mean 2.37 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-off-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes off, async outbox, dbs shared)

1500 orders in 10.8 s = 139.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 45.9 | 114.7 | 107.2 | 152.4 | 169.7 | 209.5 | 247.0 |
| placeOrder | declined | 27 | 69.9 | 108.2 | 103.6 | 137.0 | 138.9 | 218.6 | 218.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3422 | 46.0% |
| customers.getPaymentMethod | 1500 | 530 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.3 | 3.7 | 9.0 | 12.0 | 59.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.4 | 3.2 | 5.5 | 6.8 | 9.4 |
| orders -> pricing.quote [remote] | 1500 |  | 3.2 | 2.8 | 6.6 | 8.9 | 52.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 1.0 | 3.5 | 4.8 | 49.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.8 | 2.7 | 4.9 | 6.1 | 12.3 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.2 | 5.3 | 6.8 | 9.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.1 | 3.6 | 4.4 | 49.5 |
| orders -> payments.capture [remote] | 1473 |  | 1.4 | 1.3 | 2.2 | 2.9 | 5.1 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.4 | 5.8 |
| payments -> customers.getPaymentMethod [remote] | 970 |  | 1.4 | 1.3 | 2.4 | 3.1 | 8.9 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.0 | 2.6 | 6.5 |
| inventory -> catalog.productExists [remote] | 673 |  | 1.9 | 1.7 | 3.1 | 3.7 | 8.8 |
| pricing -> customers.getCustomer [remote] | 970 |  | 1.3 | 1.1 | 2.0 | 2.9 | 8.8 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.7 | 1.6 | 2.9 | 4.0 | 6.1 |
| shipping -> customers.getCustomer [remote] | 970 |  | 1.1 | 1.0 | 1.8 | 2.4 | 2.9 |
| orders -> customers.getCustomer [remote] | 970 |  | 1.0 | 0.9 | 1.9 | 2.6 | 3.3 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.2 | 3.4 | 3.5 | 3.5 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2003 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8642 calls, mean 0.01 ms, remote 21277 calls, mean 2.06 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes off, async outbox, dbs split)

1500 orders in 11.1 s = 135.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 40.4 | 117.9 | 111.6 | 158.3 | 176.7 | 211.5 | 235.1 |
| placeOrder | declined | 27 | 78.3 | 110.8 | 106.4 | 142.4 | 146.5 | 196.5 | 196.5 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2346 | 77.7% |
| catalog.productExists | 3018 | 2346 | 77.7% |
| customers.getCustomer | 7446 | 3420 | 45.9% |
| customers.getPaymentMethod | 1500 | 530 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.4 | 3.8 | 9.4 | 12.5 | 64.9 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.6 | 3.3 | 5.9 | 7.8 | 47.5 |
| orders -> pricing.quote [remote] | 1500 |  | 3.2 | 2.9 | 6.7 | 8.9 | 14.2 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.7 | 5.0 | 55.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.9 | 2.8 | 4.9 | 6.6 | 47.9 |
| orders -> notifications.send [remote] | 1473 |  | 2.7 | 2.3 | 5.7 | 7.2 | 8.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.1 | 3.7 | 4.4 | 9.6 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.3 | 3.0 | 5.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.6 | 39.8 |
| payments -> customers.getPaymentMethod [remote] | 970 |  | 1.4 | 1.3 | 2.4 | 3.2 | 7.6 |
| inventory -> catalog.productExists [remote] | 672 |  | 2.0 | 1.7 | 3.2 | 4.0 | 52.6 |
| notifications -> customers.getCustomer [remote] | 1116 |  | 1.2 | 1.0 | 2.2 | 2.9 | 3.8 |
| pricing -> customers.getCustomer [remote] | 970 |  | 1.3 | 1.2 | 2.0 | 2.9 | 8.4 |
| pricing -> catalog.getProduct [remote] | 672 |  | 1.8 | 1.6 | 2.9 | 3.9 | 8.9 |
| orders -> customers.getCustomer [remote] | 970 |  | 1.1 | 1.0 | 1.8 | 2.4 | 43.7 |
| shipping -> customers.getCustomer [remote] | 970 |  | 1.1 | 1.0 | 1.8 | 2.5 | 3.1 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 3.1 | 3.5 | 3.5 |
| inventory -> catalog.productExists [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2003 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 530 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 357 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8642 calls, mean 0.01 ms, remote 21277 calls, mean 2.13 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes off, async events, dbs shared)

1500 orders in 11.2 s = 133.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 45.5 | 119.5 | 114.8 | 160.9 | 179.5 | 213.9 | 253.1 |
| placeOrder | declined | 27 | 77.5 | 109.9 | 108.2 | 132.9 | 136.3 | 209.3 | 209.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3581 | 48.1% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.4 | 3.8 | 9.3 | 14.0 | 57.2 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.4 | 6.8 | 49.0 |
| orders -> notifications.send [remote] | 1473 |  | 3.4 | 3.2 | 6.1 | 7.9 | 46.3 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 2.9 | 7.1 | 9.6 | 13.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.3 | 6.8 | 12.7 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.5 | 4.7 | 52.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.7 | 4.6 | 49.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.1 | 9.3 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.7 | 5.5 |
| notifications -> customers.getCustomer [remote] | 952 |  | 1.5 | 1.4 | 2.5 | 3.5 | 9.9 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.4 | 3.3 | 10.0 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.0 | 1.8 | 3.1 | 4.0 | 50.1 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.2 | 2.8 | 8.2 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.9 | 1.7 | 3.1 | 3.9 | 6.6 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.9 | 2.9 | 46.2 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.8 | 2.5 | 8.0 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 1.9 | 3.2 | 3.7 | 3.7 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.4 |
| notifications -> customers.getCustomer [cache] | 521 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8800 calls, mean 0.01 ms, remote 21119 calls, mean 2.23 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes off, async events, dbs split)

1500 orders in 11.4 s = 131.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 40.7 | 121.5 | 114.0 | 165.1 | 178.9 | 217.2 | 252.9 |
| placeOrder | declined | 27 | 68.5 | 109.0 | 100.8 | 146.8 | 147.7 | 176.7 | 176.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3580 | 48.1% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.5 | 3.8 | 9.5 | 13.0 | 57.2 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.2 | 5.7 | 6.9 | 46.1 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 6.9 | 8.9 | 50.2 |
| orders -> notifications.send [remote] | 1473 |  | 3.2 | 3.0 | 5.8 | 7.0 | 51.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 2.9 | 5.4 | 6.8 | 50.0 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.7 | 5.1 | 54.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.1 | 3.7 | 4.7 | 9.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.7 | 3.5 | 11.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 2.0 | 2.7 | 42.6 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.5 | 3.4 | 5.9 |
| notifications -> customers.getCustomer [remote] | 953 |  | 1.5 | 1.4 | 2.4 | 3.2 | 9.8 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.0 | 1.8 | 3.2 | 4.4 | 50.2 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.9 | 1.8 | 3.1 | 4.5 | 9.6 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.1 | 2.8 | 3.7 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.9 | 2.8 | 6.8 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 2.0 | 2.7 | 7.3 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 1.9 | 3.6 | 4.7 | 4.7 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 520 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8799 calls, mean 0.01 ms, remote 21120 calls, mean 2.25 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 9.3 s = 160.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 40.4 | 99.3 | 94.4 | 132.5 | 146.1 | 170.7 | 193.5 |
| placeOrder | declined | 27 | 59.4 | 90.8 | 88.9 | 111.7 | 111.7 | 140.3 | 140.3 |

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
| orders -> shipping.quote [remote] | 1500 |  | 4.6 | 3.9 | 9.7 | 13.5 | 55.6 |
| orders -> pricing.quote [remote] | 1500 |  | 3.5 | 3.1 | 7.3 | 9.9 | 17.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.8 | 7.4 | 54.7 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.8 | 5.5 | 53.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.9 | 5.2 | 6.5 | 11.3 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.4 | 5.5 | 7.1 | 11.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.8 | 4.9 | 53.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.2 | 9.2 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.7 | 50.4 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.5 | 1.4 | 2.4 | 3.3 | 9.0 |
| inventory -> catalog.productExists [remote] | 674 |  | 2.1 | 1.9 | 3.6 | 4.6 | 51.6 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.4 | 1.3 | 2.2 | 3.1 | 9.0 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.2 | 2.8 | 5.1 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.9 | 1.7 | 3.1 | 4.3 | 9.5 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.1 | 2.6 | 7.4 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.0 | 1.9 | 2.4 | 7.3 |
| orders -> inventory.release [remote] | 27 |  | 2.5 | 2.3 | 4.0 | 4.2 | 4.2 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8632 calls, mean 0.01 ms, remote 21287 calls, mean 2.19 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async outbox, dbs split)

1500 orders in 9.6 s = 156.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 42.3 | 101.7 | 97.5 | 137.0 | 150.5 | 174.0 | 198.9 |
| placeOrder | declined | 27 | 56.1 | 92.4 | 90.2 | 116.6 | 120.2 | 180.0 | 180.0 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2346 | 77.7% |
| catalog.productExists | 3018 | 2346 | 77.7% |
| customers.getCustomer | 7446 | 3416 | 45.9% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.7 | 4.0 | 10.0 | 14.0 | 60.6 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.6 | 3.4 | 5.7 | 7.3 | 51.6 |
| orders -> pricing.quote [remote] | 1500 |  | 3.5 | 3.0 | 7.6 | 10.5 | 18.7 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.1 | 3.9 | 5.6 | 55.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.9 | 5.6 | 7.5 | 12.8 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.3 | 5.6 | 7.1 | 8.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.9 | 5.1 | 8.5 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.4 | 2.7 | 3.4 | 8.8 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 2.0 | 2.7 | 45.8 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.6 | 1.4 | 2.8 | 3.7 | 4.6 |
| inventory -> catalog.productExists [remote] | 672 |  | 2.1 | 1.9 | 3.5 | 4.8 | 54.6 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.4 | 1.3 | 2.3 | 3.4 | 9.8 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.2 | 1.0 | 2.2 | 3.0 | 5.9 |
| pricing -> catalog.getProduct [remote] | 672 |  | 1.9 | 1.7 | 3.2 | 4.3 | 13.9 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.1 | 2.4 | 3.5 | 6.8 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.2 | 3.1 | 7.1 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.4 | 3.3 | 4.1 | 4.1 |
| inventory -> catalog.productExists [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.9 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8636 calls, mean 0.01 ms, remote 21283 calls, mean 2.24 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async events, dbs shared)

1500 orders in 10.0 s = 150.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 48.3 | 106.3 | 101.2 | 139.8 | 149.6 | 171.8 | 196.9 |
| placeOrder | declined | 27 | 62.6 | 95.7 | 96.4 | 114.5 | 120.9 | 134.2 | 134.2 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2346 | 77.7% |
| catalog.productExists | 3018 | 2346 | 77.7% |
| customers.getCustomer | 7446 | 3580 | 48.1% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.0 | 4.3 | 10.7 | 15.0 | 59.0 |
| orders -> pricing.quote [remote] | 1500 |  | 3.7 | 3.3 | 7.8 | 10.4 | 15.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.7 | 3.5 | 5.9 | 8.1 | 12.9 |
| orders -> notifications.send [remote] | 1473 |  | 3.7 | 3.5 | 6.5 | 8.4 | 13.8 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.7 | 1.2 | 4.2 | 6.5 | 53.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.2 | 3.1 | 5.5 | 6.7 | 12.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.4 | 4.0 | 5.0 | 51.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.2 | 6.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.1 | 2.0 | 2.8 | 6.0 |
| notifications -> customers.getCustomer [remote] | 953 |  | 1.6 | 1.5 | 2.6 | 3.4 | 4.1 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.6 | 1.5 | 2.6 | 3.3 | 9.0 |
| inventory -> catalog.productExists [remote] | 672 |  | 2.3 | 2.1 | 4.1 | 5.1 | 11.0 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.5 | 1.4 | 2.4 | 3.4 | 10.0 |
| pricing -> catalog.getProduct [remote] | 672 |  | 2.1 | 1.9 | 3.7 | 4.7 | 10.1 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.1 | 3.0 | 5.0 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.0 | 2.8 | 7.4 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.1 | 4.0 | 4.8 | 4.8 |
| inventory -> catalog.productExists [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 520 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8801 calls, mean 0.01 ms, remote 21118 calls, mean 2.45 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices-macro (cache on, parallel quotes on, async events, dbs split)

1500 orders in 10.0 s = 150.2 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 38.3 | 106.2 | 101.7 | 139.3 | 148.8 | 190.4 | 218.2 |
| placeOrder | declined | 27 | 67.4 | 100.2 | 97.8 | 126.9 | 128.4 | 154.6 | 154.6 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2345 | 77.7% |
| catalog.productExists | 3018 | 2345 | 77.7% |
| customers.getCustomer | 7446 | 3577 | 48.0% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.0 | 4.4 | 10.6 | 13.9 | 20.2 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.9 | 3.6 | 6.0 | 7.6 | 44.3 |
| orders -> notifications.send [remote] | 1473 |  | 3.7 | 3.5 | 6.6 | 8.2 | 14.8 |
| orders -> pricing.quote [remote] | 1500 |  | 3.5 | 3.1 | 7.4 | 10.0 | 16.3 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.6 | 1.2 | 4.1 | 5.9 | 12.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 2.9 | 5.6 | 7.0 | 12.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.6 | 2.3 | 4.0 | 5.2 | 53.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.5 | 9.2 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.1 | 2.1 | 2.9 | 38.5 |
| notifications -> customers.getCustomer [remote] | 953 |  | 1.6 | 1.5 | 2.5 | 3.5 | 8.3 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.6 | 1.4 | 2.7 | 3.6 | 8.7 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.2 | 2.0 | 3.7 | 5.0 | 11.0 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.4 | 1.3 | 2.3 | 3.0 | 7.1 |
| pricing -> catalog.getProduct [remote] | 673 |  | 2.0 | 1.8 | 3.3 | 5.1 | 10.8 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.1 | 2.2 | 3.1 | 5.4 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.1 | 2.8 | 5.5 |
| orders -> inventory.release [remote] | 27 |  | 2.5 | 2.3 | 4.2 | 5.8 | 5.8 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 520 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8795 calls, mean 0.01 ms, remote 21124 calls, mean 2.43 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-macro-cache-on-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
