# Performance run 20261027T181847Z

- Started: 2026-10-27T18:18:47+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes microservices --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/microservices`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices cache off parallel off async outbox dbs shared | 116 | 1473 | 27 | 51.2 | 137.7 | 130.0 | 187.6 | 203.7 | 229.0 | 249.4 | 19.9 | PASS |
| microservices cache off parallel off async outbox dbs split | 114 | 1473 | 27 | 45.8 | 140.6 | 134.5 | 191.2 | 211.8 | 239.6 | 257.2 | 19.9 | PASS |
| microservices cache off parallel off async events dbs shared | 112 | 1473 | 27 | 55.5 | 142.3 | 134.9 | 194.6 | 209.5 | 242.5 | 264.3 | 19.9 | PASS |
| microservices cache off parallel off async events dbs split | 111 | 1473 | 27 | 51.4 | 143.6 | 136.6 | 199.7 | 213.5 | 246.7 | 279.4 | 19.9 | PASS |
| microservices cache off parallel on async outbox dbs shared | 136 | 1473 | 27 | 49.1 | 116.9 | 112.4 | 154.8 | 166.7 | 212.4 | 261.9 | 19.9 | PASS |
| microservices cache off parallel on async outbox dbs split | 135 | 1473 | 27 | 44.9 | 118.5 | 114.6 | 153.3 | 172.1 | 225.4 | 252.3 | 19.9 | PASS |
| microservices cache off parallel on async events dbs shared | 131 | 1473 | 27 | 50.9 | 122.2 | 117.4 | 161.5 | 178.2 | 211.6 | 247.5 | 19.9 | PASS |
| microservices cache off parallel on async events dbs split | 130 | 1473 | 27 | 38.7 | 123.0 | 117.2 | 164.6 | 179.4 | 225.9 | 258.3 | 19.9 | PASS |
| microservices cache on parallel off async outbox dbs shared | 140 | 1473 | 27 | 43.0 | 114.2 | 107.8 | 151.8 | 165.9 | 197.5 | 224.6 | 19.9 | PASS |
| microservices cache on parallel off async outbox dbs split | 139 | 1473 | 27 | 33.1 | 115.1 | 109.1 | 159.9 | 174.2 | 212.2 | 238.6 | 19.9 | PASS |
| microservices cache on parallel off async events dbs shared | 133 | 1473 | 27 | 37.9 | 120.1 | 111.7 | 161.0 | 175.1 | 250.6 | 297.6 | 19.9 | PASS |
| microservices cache on parallel off async events dbs split | 132 | 1473 | 27 | 39.5 | 120.8 | 113.8 | 159.9 | 180.5 | 227.5 | 250.9 | 19.9 | PASS |
| microservices cache on parallel on async outbox dbs shared | 161 | 1473 | 27 | 50.2 | 99.1 | 96.4 | 129.2 | 139.3 | 174.0 | 202.2 | 19.9 | PASS |
| microservices cache on parallel on async outbox dbs split | 159 | 1473 | 27 | 34.1 | 100.1 | 96.1 | 131.5 | 141.7 | 179.8 | 213.1 | 19.9 | PASS |
| microservices cache on parallel on async events dbs shared | 151 | 1473 | 27 | 32.5 | 105.4 | 99.3 | 137.6 | 145.6 | 184.3 | 220.1 | 19.9 | PASS |
| microservices cache on parallel on async events dbs split | 150 | 1473 | 27 | 36.0 | 106.6 | 102.4 | 139.3 | 149.8 | 191.0 | 207.9 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices (parallel off) (async outbox) (dbs shared) | 116 | 140 | +21% | 130.0 | 107.8 | -17% | 203.7 | 165.9 | -19% | 229.0 | 197.5 | -14% |
| microservices (parallel off) (async outbox) (dbs split) | 114 | 139 | +22% | 134.5 | 109.1 | -19% | 211.8 | 174.2 | -18% | 239.6 | 212.2 | -11% |
| microservices (parallel off) (async events) (dbs shared) | 112 | 133 | +19% | 134.9 | 111.7 | -17% | 209.5 | 175.1 | -16% | 242.5 | 250.6 | +3% |
| microservices (parallel off) (async events) (dbs split) | 111 | 132 | +19% | 136.6 | 113.8 | -17% | 213.5 | 180.5 | -15% | 246.7 | 227.5 | -8% |
| microservices (parallel on) (async outbox) (dbs shared) | 136 | 161 | +18% | 112.4 | 96.4 | -14% | 166.7 | 139.3 | -16% | 212.4 | 174.0 | -18% |
| microservices (parallel on) (async outbox) (dbs split) | 135 | 159 | +18% | 114.6 | 96.1 | -16% | 172.1 | 141.7 | -18% | 225.4 | 179.8 | -20% |
| microservices (parallel on) (async events) (dbs shared) | 131 | 151 | +16% | 117.4 | 99.3 | -15% | 178.2 | 145.6 | -18% | 211.6 | 184.3 | -13% |
| microservices (parallel on) (async events) (dbs split) | 130 | 150 | +15% | 117.2 | 102.4 | -13% | 179.4 | 149.8 | -16% | 225.9 | 191.0 | -15% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices (cache off) (async outbox) (dbs shared) | 116 | 136 | +18% | 130.0 | 112.4 | -14% | 203.7 | 166.7 | -18% | 229.0 | 212.4 | -7% |
| microservices (cache off) (async outbox) (dbs split) | 114 | 135 | +19% | 134.5 | 114.6 | -15% | 211.8 | 172.1 | -19% | 239.6 | 225.4 | -6% |
| microservices (cache off) (async events) (dbs shared) | 112 | 131 | +16% | 134.9 | 117.4 | -13% | 209.5 | 178.2 | -15% | 242.5 | 211.6 | -13% |
| microservices (cache off) (async events) (dbs split) | 111 | 130 | +17% | 136.6 | 117.2 | -14% | 213.5 | 179.4 | -16% | 246.7 | 225.9 | -8% |
| microservices (cache on) (async outbox) (dbs shared) | 140 | 161 | +15% | 107.8 | 96.4 | -11% | 165.9 | 139.3 | -16% | 197.5 | 174.0 | -12% |
| microservices (cache on) (async outbox) (dbs split) | 139 | 159 | +15% | 109.1 | 96.1 | -12% | 174.2 | 141.7 | -19% | 212.2 | 179.8 | -15% |
| microservices (cache on) (async events) (dbs shared) | 133 | 151 | +14% | 111.7 | 99.3 | -11% | 175.1 | 145.6 | -17% | 250.6 | 184.3 | -26% |
| microservices (cache on) (async events) (dbs split) | 132 | 150 | +13% | 113.8 | 102.4 | -10% | 180.5 | 149.8 | -17% | 227.5 | 191.0 | -16% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices (cache off) (parallel off) (dbs shared) | 116 | 112 | -3% | 130.0 | 134.9 | +4% | 203.7 | 209.5 | +3% | 229.0 | 242.5 | +6% |
| microservices (cache off) (parallel off) (dbs split) | 114 | 111 | -2% | 134.5 | 136.6 | +2% | 211.8 | 213.5 | +1% | 239.6 | 246.7 | +3% |
| microservices (cache off) (parallel on) (dbs shared) | 136 | 131 | -4% | 112.4 | 117.4 | +4% | 166.7 | 178.2 | +7% | 212.4 | 211.6 | -0% |
| microservices (cache off) (parallel on) (dbs split) | 135 | 130 | -4% | 114.6 | 117.2 | +2% | 172.1 | 179.4 | +4% | 225.4 | 225.9 | +0% |
| microservices (cache on) (parallel off) (dbs shared) | 140 | 133 | -5% | 107.8 | 111.7 | +4% | 165.9 | 175.1 | +6% | 197.5 | 250.6 | +27% |
| microservices (cache on) (parallel off) (dbs split) | 139 | 132 | -5% | 109.1 | 113.8 | +4% | 174.2 | 180.5 | +4% | 212.2 | 227.5 | +7% |
| microservices (cache on) (parallel on) (dbs shared) | 161 | 151 | -6% | 96.4 | 99.3 | +3% | 139.3 | 145.6 | +5% | 174.0 | 184.3 | +6% |
| microservices (cache on) (parallel on) (dbs split) | 159 | 150 | -6% | 96.1 | 102.4 | +7% | 141.7 | 149.8 | +6% | 179.8 | 191.0 | +6% |

## Database layout effect (one instance to one per deployable unit)

Higher orders/s and lower latencies are better.

| mode | orders/s shared | split | change | p50 shared | split | change | p95 shared | split | change | p99 shared | split | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices (cache off) (parallel off) (async outbox) | 116 | 114 | -2% | 130.0 | 134.5 | +3% | 203.7 | 211.8 | +4% | 229.0 | 239.6 | +5% |
| microservices (cache off) (parallel off) (async events) | 112 | 111 | -1% | 134.9 | 136.6 | +1% | 209.5 | 213.5 | +2% | 242.5 | 246.7 | +2% |
| microservices (cache off) (parallel on) (async outbox) | 136 | 135 | -1% | 112.4 | 114.6 | +2% | 166.7 | 172.1 | +3% | 212.4 | 225.4 | +6% |
| microservices (cache off) (parallel on) (async events) | 131 | 130 | -1% | 117.4 | 117.2 | -0% | 178.2 | 179.4 | +1% | 211.6 | 225.9 | +7% |
| microservices (cache on) (parallel off) (async outbox) | 140 | 139 | -1% | 107.8 | 109.1 | +1% | 165.9 | 174.2 | +5% | 197.5 | 212.2 | +7% |
| microservices (cache on) (parallel off) (async events) | 133 | 132 | -1% | 111.7 | 113.8 | +2% | 175.1 | 180.5 | +3% | 250.6 | 227.5 | -9% |
| microservices (cache on) (parallel on) (async outbox) | 161 | 159 | -1% | 96.4 | 96.1 | -0% | 139.3 | 141.7 | +2% | 174.0 | 179.8 | +3% |
| microservices (cache on) (parallel on) (async events) | 151 | 150 | -1% | 99.3 | 102.4 | +3% | 145.6 | 149.8 | +3% | 184.3 | 191.0 | +4% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| microservices cache off parallel off async outbox dbs shared | 20.1 | 5,794 | 5,794 | 100% | 130 to 130 | 159 to 159 |
| microservices cache off parallel off async outbox dbs split | 20.0 | 5,783 | 5,783 | 100% | 130 to 130 | 159 to 159 |
| microservices cache off parallel off async events dbs shared | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices cache off parallel off async events dbs split | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices cache off parallel on async outbox dbs shared | 20.1 | 5,794 | 5,794 | 100% | 130 to 130 | 159 to 159 |
| microservices cache off parallel on async outbox dbs split | 20.1 | 5,792 | 5,792 | 100% | 130 to 130 | 159 to 159 |
| microservices cache off parallel on async events dbs shared | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices cache off parallel on async events dbs split | 19.9 | 5,753 | 5,753 | 100% | 129 to 129 | 159 to 159 |
| microservices cache on parallel off async outbox dbs shared | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |
| microservices cache on parallel off async outbox dbs split | 14.3 | 4,538 | 4,538 | 100% | 145 to 145 | 173 to 173 |
| microservices cache on parallel off async events dbs shared | 14.1 | 4,475 | 4,475 | 100% | 145 to 145 | 173 to 173 |
| microservices cache on parallel off async events dbs split | 14.1 | 4,475 | 4,475 | 100% | 145 to 145 | 173 to 173 |
| microservices cache on parallel on async outbox dbs shared | 14.3 | 4,547 | 4,547 | 100% | 145 to 145 | 173 to 173 |
| microservices cache on parallel on async outbox dbs split | 14.3 | 4,548 | 4,548 | 100% | 145 to 145 | 173 to 173 |
| microservices cache on parallel on async events dbs shared | 14.1 | 4,476 | 4,476 | 100% | 145 to 145 | 173 to 173 |
| microservices cache on parallel on async events dbs split | 14.1 | 4,476 | 4,476 | 100% | 145 to 145 | 173 to 173 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | microservices cache off parallel off async outbox dbs shared | microservices cache off parallel off async outbox dbs split | microservices cache off parallel off async events dbs shared | microservices cache off parallel off async events dbs split | microservices cache off parallel on async outbox dbs shared | microservices cache off parallel on async outbox dbs split | microservices cache off parallel on async events dbs shared | microservices cache off parallel on async events dbs split | microservices cache on parallel off async outbox dbs shared | microservices cache on parallel off async outbox dbs split | microservices cache on parallel off async events dbs shared | microservices cache on parallel off async events dbs split | microservices cache on parallel on async outbox dbs shared | microservices cache on parallel on async outbox dbs split | microservices cache on parallel on async events dbs shared | microservices cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 5.7 | 5.9 | 5.8 | 5.9 | 6.3 | 6.4 | 6.8 | 7.1 | 4.1 | 4.4 | 4.5 | 4.6 | 4.6 | 4.7 | 5.0 | 5.1 |
| orders -> pricing.quote | 4.6 | 4.7 | 4.9 | 4.9 | 5.0 | 5.1 | 5.5 | 5.6 | 3.2 | 3.2 | 3.4 | 3.4 | 3.4 | 3.4 | 3.8 | 3.6 |
| orders -> shipping.createShipment | 4.3 | 4.5 | 4.4 | 4.4 | 4.5 | 4.5 | 4.6 | 4.5 | 3.3 | 3.5 | 3.5 | 3.5 | 3.5 | 3.6 | 3.7 | 3.9 |
| shipping -> inventory.getStock | 2.0 | 2.1 | 2.0 | 2.1 | 2.3 | 2.3 | 2.4 | 2.5 | 1.3 | 1.4 | 1.5 | 1.5 | 1.5 | 1.5 | 1.6 | 1.7 |
| orders -> notifications.send | 2.7 | 2.7 | 3.7 | 3.9 | 2.6 | 2.7 | 4.2 | 4.4 | 2.5 | 2.6 | 3.4 | 3.4 | 2.5 | 2.5 | 3.6 | 3.7 |
| orders -> payments.authorize | 3.2 | 3.1 | 3.2 | 3.3 | 3.3 | 3.4 | 3.4 | 3.5 | 2.9 | 2.7 | 2.9 | 3.0 | 3.0 | 2.9 | 3.1 | 3.1 |
| orders -> inventory.reserve | 2.3 | 2.3 | 2.4 | 2.3 | 2.5 | 2.5 | 2.6 | 2.5 | 2.2 | 2.2 | 2.4 | 2.3 | 2.4 | 2.5 | 2.5 | 2.5 |
| inventory -> catalog.productExists | 1.0 | 1.1 | 1.1 | 1.0 | 1.2 | 1.2 | 1.3 | 1.3 | 0.4 | 0.4 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| pricing -> catalog.getProduct | 1.0 | 1.0 | 1.1 | 1.1 | 1.1 | 1.1 | 1.2 | 1.2 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.5 | 0.5 |
| orders -> payments.capture | 1.5 | 1.5 | 1.6 | 1.5 | 1.6 | 1.6 | 1.7 | 1.6 | 1.5 | 1.4 | 1.5 | 1.6 | 1.5 | 1.5 | 1.6 | 1.6 |
| shipping -> customers.getCustomer | 0.8 | 0.9 | 0.8 | 0.8 | 0.9 | 0.9 | 0.9 | 0.9 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| payments -> customers.getPaymentMethod | 1.2 | 1.2 | 1.2 | 1.2 | 1.2 | 1.3 | 1.3 | 1.3 | 0.9 | 0.9 | 0.9 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| shipping -> inventory.getReservation | 1.0 | 1.1 | 1.1 | 1.0 | 1.1 | 1.0 | 1.1 | 1.1 | 1.0 | 1.1 | 1.1 | 1.1 | 1.1 | 1.1 | 1.2 | 1.3 |
| notifications -> customers.getCustomer | 0.9 | 0.9 | 1.2 | 1.3 | 0.9 | 0.9 | 1.4 | 1.4 | 0.9 | 0.9 | 1.0 | 1.0 | 0.8 | 0.9 | 1.0 | 1.1 |
| pricing -> customers.getCustomer | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.1 | 1.1 | 0.8 | 0.8 | 0.8 | 0.8 | 0.9 | 0.9 | 1.0 | 0.9 |
| orders -> customers.getCustomer | 0.8 | 0.8 | 0.8 | 0.8 | 1.0 | 1.0 | 1.0 | 1.1 | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.8 | 0.8 | 0.8 |
| orders -> inventory.release | 2.4 | 2.2 | 2.2 | 2.1 | 2.3 | 2.4 | 2.3 | 2.5 | 2.3 | 2.4 | 2.2 | 2.0 | 2.3 | 2.4 | 2.6 | 2.5 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | microservices cache off parallel off async outbox dbs shared | microservices cache off parallel off async outbox dbs split | microservices cache off parallel off async events dbs shared | microservices cache off parallel off async events dbs split | microservices cache off parallel on async outbox dbs shared | microservices cache off parallel on async outbox dbs split | microservices cache off parallel on async events dbs shared | microservices cache off parallel on async events dbs split | microservices cache on parallel off async outbox dbs shared | microservices cache on parallel off async outbox dbs split | microservices cache on parallel off async events dbs shared | microservices cache on parallel off async events dbs split | microservices cache on parallel on async outbox dbs shared | microservices cache on parallel on async outbox dbs split | microservices cache on parallel on async events dbs shared | microservices cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 10.6 | 10.9 | 10.8 | 10.7 | 12.2 | 12.1 | 12.7 | 12.8 | 8.6 | 9.4 | 9.4 | 9.4 | 9.9 | 9.7 | 10.6 | 10.6 |
| orders -> pricing.quote | 7.8 | 8.2 | 8.5 | 8.4 | 8.6 | 8.6 | 9.3 | 9.6 | 6.7 | 6.4 | 6.9 | 6.9 | 6.8 | 7.2 | 7.8 | 7.6 |
| orders -> shipping.createShipment | 6.8 | 6.9 | 6.6 | 6.8 | 6.6 | 7.2 | 6.9 | 6.9 | 5.1 | 5.6 | 5.5 | 5.7 | 5.6 | 5.7 | 5.6 | 5.9 |
| shipping -> inventory.getStock | 3.3 | 3.4 | 3.5 | 3.4 | 4.0 | 3.9 | 4.3 | 4.4 | 3.4 | 3.5 | 3.7 | 3.7 | 3.8 | 3.8 | 4.2 | 4.1 |
| orders -> notifications.send | 5.5 | 5.6 | 6.3 | 6.9 | 5.3 | 5.9 | 7.1 | 7.7 | 5.1 | 5.5 | 6.3 | 6.1 | 5.1 | 4.8 | 6.7 | 6.6 |
| orders -> payments.authorize | 5.3 | 5.1 | 5.5 | 5.5 | 5.3 | 5.4 | 5.7 | 5.8 | 5.1 | 5.0 | 5.2 | 5.1 | 5.2 | 5.1 | 5.4 | 5.4 |
| orders -> inventory.reserve | 3.6 | 3.6 | 3.6 | 3.6 | 4.0 | 3.8 | 3.9 | 3.8 | 3.4 | 3.6 | 3.7 | 3.7 | 3.9 | 3.8 | 3.9 | 4.0 |
| inventory -> catalog.productExists | 1.8 | 1.9 | 1.8 | 1.8 | 2.2 | 2.2 | 2.5 | 2.5 | 2.2 | 2.2 | 2.4 | 2.4 | 2.4 | 2.5 | 2.7 | 2.6 |
| pricing -> catalog.getProduct | 1.7 | 1.8 | 2.0 | 1.9 | 1.9 | 1.9 | 2.1 | 2.1 | 2.2 | 2.1 | 2.3 | 2.3 | 2.2 | 2.3 | 2.4 | 2.4 |
| orders -> payments.capture | 2.3 | 2.3 | 2.4 | 2.5 | 2.6 | 2.6 | 2.7 | 2.6 | 2.3 | 2.4 | 2.4 | 2.5 | 2.4 | 2.4 | 2.5 | 2.6 |
| shipping -> customers.getCustomer | 1.4 | 1.5 | 1.4 | 1.6 | 1.6 | 1.6 | 1.6 | 1.8 | 1.4 | 1.4 | 1.4 | 1.4 | 1.5 | 1.5 | 1.6 | 1.6 |
| payments -> customers.getPaymentMethod | 2.1 | 2.1 | 2.1 | 2.2 | 2.2 | 2.3 | 2.3 | 2.4 | 2.2 | 2.2 | 2.2 | 2.3 | 2.4 | 2.4 | 2.3 | 2.5 |
| shipping -> inventory.getReservation | 1.8 | 1.7 | 1.8 | 1.7 | 1.7 | 1.8 | 1.8 | 1.7 | 1.7 | 1.8 | 1.9 | 1.9 | 1.9 | 1.9 | 1.9 | 2.0 |
| notifications -> customers.getCustomer | 1.8 | 1.9 | 2.0 | 2.3 | 1.7 | 1.9 | 2.4 | 2.5 | 2.0 | 1.9 | 2.3 | 2.2 | 2.0 | 1.9 | 2.3 | 2.5 |
| pricing -> customers.getCustomer | 1.6 | 1.6 | 1.6 | 1.8 | 1.7 | 1.8 | 1.9 | 2.1 | 1.8 | 1.8 | 1.9 | 1.9 | 1.9 | 2.0 | 2.2 | 2.1 |
| orders -> customers.getCustomer | 1.4 | 1.5 | 1.5 | 1.6 | 1.7 | 1.9 | 1.8 | 2.0 | 1.6 | 1.7 | 1.7 | 1.7 | 1.9 | 1.9 | 2.0 | 1.9 |
| orders -> inventory.release | 3.2 | 3.0 | 4.0 | 4.5 | 2.9 | 3.2 | 3.6 | 4.0 | 3.2 | 3.1 | 4.6 | 3.6 | 2.8 | 3.0 | 4.4 | 5.1 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | microservices cache off parallel off async outbox dbs shared | microservices cache off parallel off async outbox dbs split | microservices cache off parallel off async events dbs shared | microservices cache off parallel off async events dbs split | microservices cache off parallel on async outbox dbs shared | microservices cache off parallel on async outbox dbs split | microservices cache off parallel on async events dbs shared | microservices cache off parallel on async events dbs split | microservices cache on parallel off async outbox dbs shared | microservices cache on parallel off async outbox dbs split | microservices cache on parallel off async events dbs shared | microservices cache on parallel off async events dbs split | microservices cache on parallel on async outbox dbs shared | microservices cache on parallel on async outbox dbs split | microservices cache on parallel on async events dbs shared | microservices cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | - | - | - | - | 77.7% | 77.6% | 77.7% | 77.7% | 77.8% | 77.7% | 77.7% | 77.7% |
| catalog.productExists | - | - | - | - | - | - | - | - | 77.7% | 77.6% | 77.7% | 77.7% | 77.8% | 77.7% | 77.7% | 77.7% |
| customers.getCustomer | - | - | - | - | - | - | - | - | 45.9% | 45.9% | 48.1% | 48.1% | 45.9% | 45.9% | 48.0% | 48.0% |
| customers.getPaymentMethod | - | - | - | - | - | - | - | - | 35.3% | 35.3% | 35.3% | 35.3% | 35.3% | 35.2% | 35.2% | 35.2% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| microservices cache off parallel off async outbox dbs shared | - | - | - | - | 29919 calls, 1.94 ms |
| microservices cache off parallel off async outbox dbs split | - | - | - | - | 29919 calls, 1.98 ms |
| microservices cache off parallel off async events dbs shared | - | - | - | - | 29919 calls, 2.06 ms |
| microservices cache off parallel off async events dbs split | - | - | - | - | 29919 calls, 2.07 ms |
| microservices cache off parallel on async outbox dbs shared | - | - | - | - | 29919 calls, 2.09 ms |
| microservices cache off parallel on async outbox dbs split | - | - | - | - | 29919 calls, 2.12 ms |
| microservices cache off parallel on async events dbs shared | - | - | - | - | 29919 calls, 2.30 ms |
| microservices cache off parallel on async events dbs split | - | - | - | - | 29919 calls, 2.35 ms |
| microservices cache on parallel off async outbox dbs shared | 8638 calls, 0.01 ms | - | - | - | 21281 calls, 2.04 ms |
| microservices cache on parallel off async outbox dbs split | 8634 calls, 0.01 ms | - | - | - | 21285 calls, 2.08 ms |
| microservices cache on parallel off async events dbs shared | 8797 calls, 0.01 ms | - | - | - | 21122 calls, 2.23 ms |
| microservices cache on parallel off async events dbs split | 8798 calls, 0.01 ms | - | - | - | 21121 calls, 2.26 ms |
| microservices cache on parallel on async outbox dbs shared | 8644 calls, 0.01 ms | - | - | - | 21275 calls, 2.18 ms |
| microservices cache on parallel on async outbox dbs split | 8632 calls, 0.01 ms | - | - | - | 21287 calls, 2.20 ms |
| microservices cache on parallel on async events dbs shared | 8795 calls, 0.01 ms | - | - | - | 21124 calls, 2.42 ms |
| microservices cache on parallel on async events dbs split | 8797 calls, 0.01 ms | - | - | - | 21122 calls, 2.45 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### microservices (cache off, parallel quotes off, async outbox, dbs shared)

1500 orders in 13.0 s = 115.8 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 51.2 | 137.7 | 130.0 | 187.6 | 203.7 | 229.0 | 249.4 |
| placeOrder | declined | 27 | 83.7 | 130.8 | 117.6 | 204.5 | 223.5 | 224.8 | 224.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.7 | 5.0 | 10.6 | 14.5 | 62.0 |
| orders -> pricing.quote [remote] | 1500 |  | 4.6 | 4.2 | 7.8 | 10.3 | 51.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.3 | 4.0 | 6.8 | 8.5 | 48.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.0 | 1.8 | 3.3 | 4.4 | 54.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.2 | 3.0 | 5.3 | 6.6 | 11.3 |
| orders -> notifications.send [remote] | 1473 |  | 2.7 | 2.3 | 5.5 | 7.1 | 10.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.6 | 4.9 | 6.8 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.0 | 0.9 | 1.8 | 2.4 | 8.0 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.0 | 0.9 | 1.7 | 2.3 | 4.5 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.4 | 2.0 | 6.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.3 | 2.9 | 3.9 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.0 | 2.1 | 2.8 | 7.8 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.0 | 0.9 | 1.8 | 2.4 | 9.1 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.6 | 2.5 | 7.3 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.8 | 2.5 | 6.3 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.4 | 2.0 | 6.1 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.3 | 3.2 | 3.3 | 3.3 |

By binding: remote 29919 calls, mean 1.94 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes off, async outbox, dbs split)

1500 orders in 13.2 s = 113.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 45.8 | 140.6 | 134.5 | 191.2 | 211.8 | 239.6 | 257.2 |
| placeOrder | declined | 27 | 83.5 | 125.4 | 123.7 | 160.7 | 167.1 | 185.5 | 185.5 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.9 | 5.3 | 10.9 | 13.6 | 62.4 |
| orders -> pricing.quote [remote] | 1500 |  | 4.7 | 4.3 | 8.2 | 10.5 | 14.7 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.2 | 6.9 | 8.4 | 48.3 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.1 | 1.9 | 3.4 | 4.2 | 54.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 2.9 | 5.1 | 6.1 | 11.4 |
| orders -> notifications.send [remote] | 1473 |  | 2.7 | 2.3 | 5.6 | 6.8 | 8.3 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.6 | 4.4 | 10.7 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.1 | 1.0 | 1.9 | 2.4 | 6.3 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.0 | 0.9 | 1.8 | 2.5 | 8.3 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.5 | 1.9 | 8.7 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.3 | 3.1 | 9.1 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.1 | 2.8 | 9.3 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.2 | 42.6 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.6 | 2.4 | 5.6 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.9 | 2.3 | 6.6 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.2 | 49.3 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.0 | 3.0 | 3.1 | 3.1 |

By binding: remote 29919 calls, mean 1.98 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes off, async events, dbs shared)

1500 orders in 13.4 s = 112.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 55.5 | 142.3 | 134.9 | 194.6 | 209.5 | 242.5 | 264.3 |
| placeOrder | declined | 27 | 98.0 | 129.0 | 119.4 | 161.9 | 173.4 | 234.1 | 234.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.8 | 5.3 | 10.8 | 13.3 | 55.3 |
| orders -> pricing.quote [remote] | 1500 |  | 4.9 | 4.5 | 8.5 | 10.7 | 14.8 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.4 | 4.0 | 6.6 | 8.2 | 52.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.0 | 1.9 | 3.5 | 4.5 | 10.1 |
| orders -> notifications.send [remote] | 1473 |  | 3.7 | 3.5 | 6.3 | 8.1 | 11.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.2 | 3.0 | 5.5 | 6.5 | 12.3 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.6 | 4.7 | 8.4 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 0.9 | 2.0 | 2.8 | 9.2 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.1 | 1.0 | 1.8 | 2.4 | 8.5 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.8 | 1.4 | 1.9 | 7.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.4 | 2.4 | 3.0 | 48.1 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.1 | 2.8 | 8.3 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.2 | 1.1 | 2.0 | 2.5 | 8.9 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 0.9 | 1.8 | 2.4 | 46.7 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.6 | 2.0 | 3.7 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.3 | 9.3 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 1.9 | 4.0 | 4.9 | 4.9 |

By binding: remote 29919 calls, mean 2.06 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes off, async events, dbs split)

1500 orders in 13.5 s = 111.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 51.4 | 143.6 | 136.6 | 199.7 | 213.5 | 246.7 | 279.4 |
| placeOrder | declined | 27 | 85.3 | 138.2 | 129.1 | 189.5 | 239.2 | 240.7 | 240.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.9 | 5.3 | 10.7 | 14.3 | 62.6 |
| orders -> pricing.quote [remote] | 1500 |  | 4.9 | 4.6 | 8.4 | 10.0 | 14.7 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.4 | 4.1 | 6.8 | 8.2 | 58.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.1 | 1.9 | 3.4 | 4.4 | 42.4 |
| orders -> notifications.send [remote] | 1473 |  | 3.9 | 3.6 | 6.9 | 8.2 | 13.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.3 | 3.0 | 5.5 | 6.6 | 9.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.2 | 3.6 | 4.7 | 12.0 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 0.9 | 1.9 | 2.6 | 8.4 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.0 | 1.0 | 1.8 | 2.4 | 7.8 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.6 | 2.2 | 8.7 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.5 | 3.3 | 7.4 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.3 | 1.1 | 2.3 | 2.8 | 10.0 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.2 | 2.9 | 7.1 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.0 | 0.9 | 1.7 | 2.2 | 50.7 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.8 | 2.3 | 4.0 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.6 | 2.3 | 4.8 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 1.8 | 4.5 | 4.7 | 4.7 |

By binding: remote 29919 calls, mean 2.07 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async outbox, dbs shared)

1500 orders in 11.0 s = 136.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 49.1 | 116.9 | 112.4 | 154.8 | 166.7 | 212.4 | 261.9 |
| placeOrder | declined | 27 | 72.7 | 108.3 | 103.4 | 147.8 | 179.1 | 202.2 | 202.2 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.3 | 5.8 | 12.2 | 15.4 | 53.8 |
| orders -> pricing.quote [remote] | 1500 |  | 5.0 | 4.6 | 8.6 | 11.7 | 47.1 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.3 | 2.1 | 4.0 | 5.0 | 10.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.2 | 6.6 | 8.0 | 57.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.3 | 3.0 | 5.3 | 7.0 | 12.2 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.2 | 5.3 | 6.8 | 8.6 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 4.0 | 5.4 | 55.6 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.2 | 1.1 | 2.2 | 2.8 | 8.9 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 0.9 | 1.9 | 2.6 | 7.7 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.2 | 9.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.4 | 8.0 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.2 | 1.1 | 2.2 | 3.0 | 9.3 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.3 | 50.6 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.7 | 2.3 | 8.7 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.8 | 1.7 | 2.7 | 47.1 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.7 | 2.5 | 5.8 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.3 | 2.9 | 3.3 | 3.3 |

By binding: remote 29919 calls, mean 2.09 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async outbox, dbs split)

1500 orders in 11.1 s = 134.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 44.9 | 118.5 | 114.6 | 153.3 | 172.1 | 225.4 | 252.3 |
| placeOrder | declined | 27 | 70.2 | 106.5 | 102.4 | 132.4 | 148.6 | 195.3 | 195.3 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.4 | 5.7 | 12.1 | 15.8 | 66.9 |
| orders -> pricing.quote [remote] | 1500 |  | 5.1 | 4.7 | 8.6 | 12.1 | 49.9 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.3 | 2.1 | 3.9 | 5.2 | 52.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.3 | 7.2 | 8.9 | 15.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.4 | 3.2 | 5.4 | 7.2 | 48.2 |
| orders -> notifications.send [remote] | 1473 |  | 2.7 | 2.3 | 5.9 | 7.5 | 11.8 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.2 | 1.1 | 2.2 | 3.0 | 49.5 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 3.8 | 5.0 | 53.4 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.1 | 1.0 | 1.9 | 2.6 | 5.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.4 | 7.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.3 | 6.4 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.3 | 1.2 | 2.3 | 3.2 | 9.5 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.0 | 1.0 | 1.8 | 2.4 | 7.4 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.0 | 1.0 | 1.8 | 2.5 | 4.8 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.9 | 1.9 | 2.4 | 3.1 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.9 | 0.8 | 1.9 | 2.7 | 7.6 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.3 | 3.2 | 3.4 | 3.4 |

By binding: remote 29919 calls, mean 2.12 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async events, dbs shared)

1500 orders in 11.5 s = 130.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 50.9 | 122.2 | 117.4 | 161.5 | 178.2 | 211.6 | 247.5 |
| placeOrder | declined | 27 | 73.9 | 112.1 | 108.8 | 150.0 | 151.7 | 191.7 | 191.7 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 6.8 | 6.1 | 12.7 | 16.6 | 56.6 |
| orders -> pricing.quote [remote] | 1500 |  | 5.5 | 5.0 | 9.3 | 11.6 | 56.6 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.4 | 2.2 | 4.3 | 5.6 | 14.5 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.6 | 4.3 | 6.9 | 8.6 | 40.9 |
| orders -> notifications.send [remote] | 1473 |  | 4.2 | 3.8 | 7.1 | 9.5 | 53.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.4 | 3.2 | 5.7 | 7.2 | 12.2 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.3 | 1.2 | 2.5 | 3.3 | 12.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.6 | 2.4 | 3.9 | 5.1 | 44.2 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.2 | 1.0 | 2.1 | 2.8 | 8.2 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.6 | 2.3 | 9.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.7 | 1.5 | 2.7 | 3.5 | 8.9 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.4 | 1.2 | 2.4 | 3.1 | 10.5 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.3 | 1.1 | 2.3 | 3.3 | 9.3 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.1 | 1.0 | 1.9 | 2.4 | 8.6 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.5 | 36.1 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.0 | 0.8 | 1.8 | 2.7 | 48.9 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.1 | 3.6 | 4.0 | 4.0 |

By binding: remote 29919 calls, mean 2.30 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache off, parallel quotes on, async events, dbs split)

1500 orders in 11.6 s = 129.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 38.7 | 123.0 | 117.2 | 164.6 | 179.4 | 225.9 | 258.3 |
| placeOrder | declined | 27 | 69.5 | 116.3 | 104.6 | 153.6 | 183.0 | 248.8 | 248.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 7.1 | 6.3 | 12.8 | 17.8 | 64.2 |
| orders -> pricing.quote [remote] | 1500 |  | 5.6 | 5.0 | 9.6 | 12.0 | 60.2 |
| shipping -> inventory.getStock [remote] | 3018 |  | 2.5 | 2.3 | 4.4 | 5.7 | 49.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 4.5 | 4.4 | 6.9 | 8.8 | 11.9 |
| orders -> notifications.send [remote] | 1473 |  | 4.4 | 3.9 | 7.7 | 9.3 | 54.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.5 | 3.2 | 5.8 | 7.5 | 11.8 |
| inventory -> catalog.productExists [remote] | 3018 |  | 1.3 | 1.2 | 2.5 | 3.2 | 9.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 3.8 | 4.8 | 54.1 |
| pricing -> catalog.getProduct [remote] | 3018 |  | 1.2 | 1.0 | 2.1 | 2.9 | 10.8 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.9 | 0.8 | 1.8 | 2.7 | 8.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.7 | 8.3 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 1.4 | 1.2 | 2.5 | 3.2 | 8.6 |
| payments -> customers.getPaymentMethod [remote] | 1500 |  | 1.3 | 1.1 | 2.4 | 3.3 | 6.1 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 1.1 | 1.0 | 2.1 | 2.9 | 6.4 |
| orders -> customers.getCustomer [remote] | 1500 |  | 1.1 | 0.8 | 2.0 | 3.0 | 50.8 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.3 | 3.5 |
| orders -> inventory.release [remote] | 27 |  | 2.5 | 2.6 | 4.0 | 4.4 | 4.4 |

By binding: remote 29919 calls, mean 2.35 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-off-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async outbox, dbs shared)

1500 orders in 10.7 s = 139.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 43.0 | 114.2 | 107.8 | 151.8 | 165.9 | 197.5 | 224.6 |
| placeOrder | declined | 27 | 77.7 | 107.2 | 102.6 | 136.8 | 146.5 | 188.9 | 188.9 |

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
| orders -> shipping.quote [remote] | 1500 |  | 4.1 | 3.5 | 8.6 | 12.1 | 50.9 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.3 | 3.1 | 5.1 | 7.0 | 13.0 |
| orders -> pricing.quote [remote] | 1500 |  | 3.2 | 2.9 | 6.7 | 9.1 | 17.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.9 | 2.7 | 5.1 | 6.4 | 14.5 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.3 | 0.9 | 3.4 | 4.6 | 48.3 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.3 | 5.1 | 6.6 | 11.0 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.2 | 2.1 | 3.4 | 4.1 | 45.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.3 | 2.3 | 2.9 | 12.7 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.0 | 0.9 | 1.7 | 2.2 | 7.8 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.4 | 1.3 | 2.4 | 3.0 | 12.2 |
| inventory -> catalog.productExists [remote] | 673 |  | 1.9 | 1.7 | 3.1 | 4.1 | 46.4 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 2.8 | 8.2 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.0 | 2.6 | 3.5 |
| pricing -> catalog.getProduct [remote] | 673 |  | 1.8 | 1.7 | 3.0 | 3.8 | 5.5 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.8 | 2.6 | 6.1 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.0 | 1.0 | 1.7 | 2.4 | 4.4 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.2 | 3.2 | 3.7 | 3.7 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8638 calls, mean 0.01 ms, remote 21281 calls, mean 2.04 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async outbox, dbs split)

1500 orders in 10.8 s = 138.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 33.1 | 115.1 | 109.1 | 159.9 | 174.2 | 212.2 | 238.6 |
| placeOrder | declined | 27 | 67.9 | 105.4 | 95.1 | 138.1 | 140.5 | 205.7 | 205.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2343 | 77.6% |
| catalog.productExists | 3018 | 2343 | 77.6% |
| customers.getCustomer | 7446 | 3419 | 45.9% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.4 | 3.8 | 9.4 | 12.7 | 58.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.6 | 7.2 | 40.3 |
| orders -> pricing.quote [remote] | 1500 |  | 3.2 | 2.8 | 6.4 | 9.0 | 53.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.4 | 1.0 | 3.5 | 4.7 | 49.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.7 | 2.5 | 5.0 | 6.6 | 11.6 |
| orders -> notifications.send [remote] | 1473 |  | 2.6 | 2.2 | 5.5 | 7.2 | 10.0 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.2 | 2.1 | 3.6 | 4.5 | 8.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.4 | 1.3 | 2.4 | 3.3 | 5.2 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.5 | 36.2 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.4 | 1.3 | 2.4 | 3.2 | 6.2 |
| inventory -> catalog.productExists [remote] | 675 |  | 1.9 | 1.8 | 3.0 | 3.7 | 46.1 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 3.0 | 5.4 |
| pricing -> catalog.getProduct [remote] | 675 |  | 1.8 | 1.7 | 2.9 | 3.9 | 9.8 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.0 | 2.6 | 5.0 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.8 | 2.8 | 5.3 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.0 | 0.9 | 1.8 | 2.6 | 7.4 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.5 | 3.1 | 3.2 | 3.2 |
| inventory -> catalog.productExists [cache] | 2343 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2343 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8634 calls, mean 0.01 ms, remote 21285 calls, mean 2.08 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async events, dbs shared)

1500 orders in 11.3 s = 132.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 37.9 | 120.1 | 111.7 | 161.0 | 175.1 | 250.6 | 297.6 |
| placeOrder | declined | 27 | 76.4 | 111.6 | 106.4 | 142.9 | 143.8 | 201.1 | 201.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2344 | 77.7% |
| catalog.productExists | 3018 | 2344 | 77.7% |
| customers.getCustomer | 7446 | 3580 | 48.1% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.5 | 3.8 | 9.4 | 12.7 | 53.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.2 | 5.5 | 7.0 | 43.4 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 6.9 | 9.7 | 14.6 |
| orders -> notifications.send [remote] | 1473 |  | 3.4 | 3.2 | 6.3 | 8.0 | 12.5 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.7 | 4.9 | 11.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.9 | 2.8 | 5.2 | 6.5 | 11.3 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.2 | 3.7 | 5.0 | 49.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.1 | 11.9 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.7 | 38.7 |
| notifications -> customers.getCustomer [remote] | 953 |  | 1.5 | 1.3 | 2.5 | 3.4 | 10.3 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.4 | 1.3 | 2.4 | 3.3 | 8.7 |
| inventory -> catalog.productExists [remote] | 674 |  | 2.0 | 1.9 | 3.2 | 4.5 | 9.7 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.9 | 1.8 | 3.3 | 4.3 | 9.0 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.0 | 2.7 | 6.6 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.8 | 2.5 | 8.6 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.0 | 0.9 | 1.9 | 2.8 | 4.8 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.0 | 4.6 | 4.8 | 4.8 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 520 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8797 calls, mean 0.01 ms, remote 21122 calls, mean 2.23 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes off, async events, dbs split)

1500 orders in 11.4 s = 132.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 39.5 | 120.8 | 113.8 | 159.9 | 180.5 | 227.5 | 250.9 |
| placeOrder | declined | 27 | 75.1 | 113.5 | 108.6 | 144.1 | 148.9 | 226.9 | 226.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2344 | 77.7% |
| catalog.productExists | 3018 | 2344 | 77.7% |
| customers.getCustomer | 7446 | 3581 | 48.1% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.6 | 3.9 | 9.4 | 13.3 | 57.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.4 | 5.7 | 6.9 | 10.4 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 6.9 | 9.7 | 15.1 |
| orders -> notifications.send [remote] | 1473 |  | 3.4 | 3.1 | 6.1 | 8.1 | 48.1 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.0 | 3.7 | 5.2 | 51.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.9 | 5.1 | 6.5 | 11.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.3 | 2.1 | 3.7 | 4.5 | 50.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.5 | 3.4 | 6.1 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.6 | 7.7 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.3 | 2.5 | 3.3 | 9.9 |
| notifications -> customers.getCustomer [remote] | 952 |  | 1.5 | 1.4 | 2.4 | 3.6 | 10.2 |
| inventory -> catalog.productExists [remote] | 674 |  | 2.1 | 1.9 | 3.3 | 4.2 | 47.1 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.9 | 1.8 | 3.1 | 3.9 | 8.2 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.1 | 2.8 | 7.6 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.9 | 2.9 | 48.9 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.8 | 2.5 | 9.0 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 2.0 | 3.6 | 3.7 | 3.7 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 521 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8798 calls, mean 0.01 ms, remote 21121 calls, mean 2.26 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 9.3 s = 161.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 50.2 | 99.1 | 96.4 | 129.2 | 139.3 | 174.0 | 202.2 |
| placeOrder | declined | 27 | 63.2 | 90.7 | 90.2 | 118.7 | 128.0 | 150.7 | 150.7 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2348 | 77.8% |
| catalog.productExists | 3018 | 2348 | 77.8% |
| customers.getCustomer | 7446 | 3419 | 45.9% |
| customers.getPaymentMethod | 1500 | 529 | 35.3% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 4.6 | 3.9 | 9.9 | 13.4 | 58.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.5 | 3.3 | 5.6 | 7.3 | 48.7 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 6.8 | 9.1 | 14.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.1 | 3.8 | 5.3 | 52.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.0 | 2.8 | 5.2 | 6.6 | 11.5 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.2 | 5.1 | 6.7 | 10.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.4 | 2.3 | 3.9 | 4.7 | 6.5 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.4 | 2.4 | 3.3 | 7.0 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.6 | 44.2 |
| payments -> customers.getPaymentMethod [remote] | 971 |  | 1.5 | 1.4 | 2.6 | 3.3 | 9.4 |
| inventory -> catalog.productExists [remote] | 670 |  | 2.1 | 1.9 | 3.3 | 5.0 | 49.8 |
| pricing -> customers.getCustomer [remote] | 971 |  | 1.3 | 1.2 | 2.1 | 2.8 | 8.6 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 2.8 | 7.3 |
| pricing -> catalog.getProduct [remote] | 670 |  | 1.8 | 1.7 | 3.0 | 4.3 | 9.2 |
| orders -> customers.getCustomer [remote] | 971 |  | 1.2 | 1.1 | 2.0 | 2.9 | 5.8 |
| shipping -> customers.getCustomer [remote] | 971 |  | 1.1 | 1.0 | 1.8 | 2.4 | 3.1 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.2 | 2.8 | 2.9 | 2.9 |
| inventory -> catalog.productExists [cache] | 2348 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2002 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2348 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 529 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8644 calls, mean 0.01 ms, remote 21275 calls, mean 2.18 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async outbox, dbs split)

1500 orders in 9.4 s = 159.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 34.1 | 100.1 | 96.1 | 131.5 | 141.7 | 179.8 | 213.1 |
| placeOrder | declined | 27 | 58.3 | 93.6 | 94.0 | 115.3 | 121.1 | 151.3 | 151.3 |

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
| orders -> shipping.quote [remote] | 1500 |  | 4.7 | 4.1 | 9.7 | 13.7 | 60.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.6 | 3.4 | 5.7 | 7.3 | 10.0 |
| orders -> pricing.quote [remote] | 1500 |  | 3.4 | 3.0 | 7.2 | 9.5 | 15.4 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.5 | 1.1 | 3.8 | 5.5 | 52.5 |
| orders -> payments.authorize [remote] | 1500 | 27 | 2.9 | 2.7 | 5.1 | 6.3 | 11.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 3.8 | 5.0 | 49.2 |
| orders -> notifications.send [remote] | 1473 |  | 2.5 | 2.3 | 4.8 | 6.4 | 10.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.5 | 1.3 | 2.4 | 3.1 | 5.7 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.1 | 1.0 | 1.9 | 2.5 | 7.9 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.5 | 1.4 | 2.6 | 3.5 | 9.1 |
| inventory -> catalog.productExists [remote] | 674 |  | 2.1 | 1.9 | 3.6 | 4.6 | 49.4 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.2 | 2.1 | 3.0 | 6.9 |
| notifications -> customers.getCustomer [remote] | 1114 |  | 1.1 | 1.0 | 2.1 | 2.8 | 5.1 |
| pricing -> catalog.getProduct [remote] | 674 |  | 1.9 | 1.7 | 3.3 | 4.2 | 5.3 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.1 | 3.0 | 5.3 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.1 | 1.1 | 1.9 | 2.6 | 5.3 |
| orders -> inventory.release [remote] | 27 |  | 2.4 | 2.4 | 3.0 | 3.3 | 3.3 |
| inventory -> catalog.productExists [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2344 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 359 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 8632 calls, mean 0.01 ms, remote 21287 calls, mean 2.20 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events, dbs shared)

1500 orders in 9.9 s = 151.3 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 32.5 | 105.4 | 99.3 | 137.6 | 145.6 | 184.3 | 220.1 |
| placeOrder | declined | 27 | 67.6 | 98.0 | 94.6 | 122.8 | 138.7 | 155.3 | 155.3 |

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
| orders -> shipping.quote [remote] | 1500 |  | 5.0 | 4.4 | 10.6 | 14.0 | 18.4 |
| orders -> pricing.quote [remote] | 1500 |  | 3.8 | 3.4 | 7.8 | 10.5 | 14.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.7 | 3.5 | 5.6 | 7.2 | 13.2 |
| orders -> notifications.send [remote] | 1473 |  | 3.6 | 3.4 | 6.7 | 8.4 | 11.5 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.6 | 1.1 | 4.2 | 5.9 | 12.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 3.0 | 5.4 | 6.6 | 10.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 3.9 | 5.3 | 50.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.5 | 3.2 | 4.8 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.2 | 1.0 | 1.9 | 2.7 | 8.1 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.6 | 1.4 | 2.6 | 3.5 | 8.7 |
| inventory -> catalog.productExists [remote] | 673 |  | 2.2 | 2.0 | 3.7 | 5.3 | 9.5 |
| notifications -> customers.getCustomer [remote] | 953 |  | 1.6 | 1.4 | 2.7 | 3.7 | 8.3 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.5 | 1.4 | 2.4 | 3.8 | 7.8 |
| pricing -> catalog.getProduct [remote] | 673 |  | 2.1 | 1.9 | 3.4 | 4.5 | 8.8 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.1 | 2.1 | 3.4 | 8.9 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.2 | 3.1 | 7.8 |
| orders -> inventory.release [remote] | 27 |  | 2.6 | 2.3 | 4.4 | 5.2 | 5.2 |
| inventory -> catalog.productExists [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> catalog.getProduct [cache] | 2345 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| notifications -> customers.getCustomer [cache] | 520 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8795 calls, mean 0.01 ms, remote 21124 calls, mean 2.42 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### microservices (cache on, parallel quotes on, async events, dbs split)

1500 orders in 10.0 s = 149.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 36.0 | 106.6 | 102.4 | 139.3 | 149.8 | 191.0 | 207.9 |
| placeOrder | declined | 27 | 67.6 | 96.1 | 89.9 | 120.9 | 125.8 | 176.9 | 176.9 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2346 | 77.7% |
| catalog.productExists | 3018 | 2346 | 77.7% |
| customers.getCustomer | 7446 | 3577 | 48.0% |
| customers.getPaymentMethod | 1500 | 528 | 35.2% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 5.1 | 4.3 | 10.6 | 14.7 | 52.4 |
| orders -> shipping.createShipment [remote] | 1473 |  | 3.9 | 3.7 | 5.9 | 7.7 | 52.7 |
| orders -> pricing.quote [remote] | 1500 |  | 3.6 | 3.2 | 7.6 | 10.3 | 13.2 |
| orders -> notifications.send [remote] | 1473 |  | 3.7 | 3.4 | 6.6 | 8.2 | 14.2 |
| shipping -> inventory.getStock [remote] | 3018 |  | 1.7 | 1.2 | 4.1 | 5.9 | 47.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 3.1 | 3.0 | 5.4 | 6.7 | 11.0 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.5 | 2.3 | 4.0 | 5.1 | 8.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.6 | 1.5 | 2.6 | 3.3 | 3.8 |
| shipping -> inventory.getReservation [remote] | 1473 |  | 1.3 | 1.1 | 2.0 | 3.0 | 48.0 |
| notifications -> customers.getCustomer [remote] | 953 |  | 1.6 | 1.6 | 2.7 | 3.7 | 10.2 |
| payments -> customers.getPaymentMethod [remote] | 972 |  | 1.6 | 1.4 | 2.7 | 3.5 | 9.0 |
| inventory -> catalog.productExists [remote] | 672 |  | 2.2 | 2.1 | 3.8 | 4.7 | 6.6 |
| pricing -> customers.getCustomer [remote] | 972 |  | 1.4 | 1.3 | 2.3 | 3.0 | 8.6 |
| pricing -> catalog.getProduct [remote] | 672 |  | 2.0 | 1.9 | 3.3 | 4.4 | 8.0 |
| shipping -> customers.getCustomer [remote] | 972 |  | 1.3 | 1.2 | 2.2 | 3.0 | 8.1 |
| orders -> customers.getCustomer [remote] | 972 |  | 1.2 | 1.1 | 2.2 | 2.8 | 4.1 |
| orders -> inventory.release [remote] | 27 |  | 2.5 | 2.1 | 5.1 | 5.5 | 5.5 |
| inventory -> catalog.productExists [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2001 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> catalog.getProduct [cache] | 2346 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 520 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| pricing -> customers.getCustomer [cache] | 528 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |

By binding: cache 8797 calls, mean 0.01 ms, remote 21122 calls, mean 2.45 ms. 19.9 module-to-module calls per order.

Raw samples: `microservices-cache-on-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
