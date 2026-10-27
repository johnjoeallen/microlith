# Performance run 20261027T175241Z

- Started: 2026-10-27T17:52:41+00:00
- Commit: c0a5e07
- Host: 32 CPUs, Linux 6.12.113+deb13-amd64, Python 3.13.5
- Load: 1500 orders, 16 concurrent clients (closed loop, persistent connections), 200 warmup orders not measured, then VACUUM ANALYZE of every PostgreSQL instance in use
- Workload: realistic: 10,000 customers (Zipf 0.8), 2,000 SKUs (Zipf 1), 1-4 lines per order (mean 2.0), 1-3 units per line, 8% gold and 2% always-declined customers, seed 42
- Call tracing: on, settle 3 s, drain 120 s
- Command line: `--modes groups --cache both --parallel both --async both --dbs both --orders 1500 --warmup 200 --concurrency 16 --out .demo/perf/full-20261027/groups`

## placeOrder round trip

Client side, ms. Declined orders (expected) are excluded from the latencies.

| run | orders/s | ok | declined | min | mean | p50 | p90 | p95 | p99 | max | calls/order | result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups cache off parallel off async outbox dbs shared | 214 | 1473 | 27 | 35.4 | 74.6 | 70.8 | 100.3 | 108.9 | 140.6 | 173.1 | 19.9 | PASS |
| groups cache off parallel off async outbox dbs split | 213 | 1473 | 27 | 26.7 | 74.9 | 69.8 | 103.4 | 110.7 | 138.3 | 164.2 | 19.9 | PASS |
| groups cache off parallel off async events dbs shared | 212 | 1473 | 27 | 32.8 | 75.2 | 69.0 | 98.8 | 110.2 | 142.9 | 161.0 | 19.9 | PASS |
| groups cache off parallel off async events dbs split | 207 | 1473 | 27 | 33.0 | 77.0 | 71.7 | 105.1 | 115.8 | 146.4 | 162.2 | 19.9 | PASS |
| groups cache off parallel on async outbox dbs shared | 232 | 1473 | 27 | 35.7 | 68.6 | 66.1 | 89.6 | 99.3 | 112.7 | 127.5 | 19.9 | PASS |
| groups cache off parallel on async outbox dbs split | 215 | 1473 | 27 | 26.4 | 74.0 | 71.2 | 102.1 | 108.6 | 122.5 | 128.3 | 19.9 | PASS |
| groups cache off parallel on async events dbs shared | 226 | 1473 | 27 | 31.8 | 70.4 | 66.2 | 93.5 | 101.0 | 109.4 | 125.4 | 19.9 | PASS |
| groups cache off parallel on async events dbs split | 226 | 1473 | 27 | 28.4 | 70.7 | 66.1 | 96.3 | 102.6 | 114.9 | 122.4 | 19.9 | PASS |
| groups cache on parallel off async outbox dbs shared | 229 | 1473 | 27 | 26.3 | 69.7 | 66.3 | 92.4 | 100.0 | 116.4 | 133.3 | 19.9 | PASS |
| groups cache on parallel off async outbox dbs split | 238 | 1473 | 27 | 39.9 | 67.0 | 62.5 | 90.7 | 96.3 | 110.6 | 142.9 | 19.9 | PASS |
| groups cache on parallel off async events dbs shared | 223 | 1473 | 27 | 24.1 | 71.3 | 67.6 | 93.0 | 101.6 | 122.4 | 147.4 | 19.9 | PASS |
| groups cache on parallel off async events dbs split | 229 | 1473 | 27 | 31.9 | 69.7 | 65.1 | 93.6 | 100.9 | 110.4 | 120.2 | 19.9 | PASS |
| groups cache on parallel on async outbox dbs shared | 245 | 1473 | 27 | 27.5 | 65.1 | 62.7 | 86.1 | 93.2 | 103.2 | 107.7 | 19.9 | PASS |
| groups cache on parallel on async outbox dbs split | 245 | 1473 | 27 | 27.7 | 65.2 | 63.0 | 85.4 | 93.4 | 115.1 | 134.6 | 19.9 | PASS |
| groups cache on parallel on async events dbs shared | 239 | 1473 | 27 | 30.1 | 66.6 | 62.9 | 86.4 | 91.2 | 104.2 | 118.0 | 19.9 | PASS |
| groups cache on parallel on async events dbs split | 251 | 1473 | 27 | 25.3 | 63.4 | 60.7 | 84.7 | 89.9 | 102.7 | 109.7 | 19.9 | PASS |

## Cache effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (parallel off) (async outbox) (dbs shared) | 214 | 229 | +7% | 70.8 | 66.3 | -6% | 108.9 | 100.0 | -8% | 140.6 | 116.4 | -17% |
| groups (parallel off) (async outbox) (dbs split) | 213 | 238 | +12% | 69.8 | 62.5 | -11% | 110.7 | 96.3 | -13% | 138.3 | 110.6 | -20% |
| groups (parallel off) (async events) (dbs shared) | 212 | 223 | +5% | 69.0 | 67.6 | -2% | 110.2 | 101.6 | -8% | 142.9 | 122.4 | -14% |
| groups (parallel off) (async events) (dbs split) | 207 | 229 | +11% | 71.7 | 65.1 | -9% | 115.8 | 100.9 | -13% | 146.4 | 110.4 | -25% |
| groups (parallel on) (async outbox) (dbs shared) | 232 | 245 | +5% | 66.1 | 62.7 | -5% | 99.3 | 93.2 | -6% | 112.7 | 103.2 | -8% |
| groups (parallel on) (async outbox) (dbs split) | 215 | 245 | +14% | 71.2 | 63.0 | -12% | 108.6 | 93.4 | -14% | 122.5 | 115.1 | -6% |
| groups (parallel on) (async events) (dbs shared) | 226 | 239 | +6% | 66.2 | 62.9 | -5% | 101.0 | 91.2 | -10% | 109.4 | 104.2 | -5% |
| groups (parallel on) (async events) (dbs split) | 226 | 251 | +11% | 66.1 | 60.7 | -8% | 102.6 | 89.9 | -12% | 114.9 | 102.7 | -11% |

## Parallel quotes effect (off to on)

Higher orders/s and lower latencies are better.

| mode | orders/s off | on | change | p50 off | on | change | p95 off | on | change | p99 off | on | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (cache off) (async outbox) (dbs shared) | 214 | 232 | +9% | 70.8 | 66.1 | -7% | 108.9 | 99.3 | -9% | 140.6 | 112.7 | -20% |
| groups (cache off) (async outbox) (dbs split) | 213 | 215 | +1% | 69.8 | 71.2 | +2% | 110.7 | 108.6 | -2% | 138.3 | 122.5 | -11% |
| groups (cache off) (async events) (dbs shared) | 212 | 226 | +7% | 69.0 | 66.2 | -4% | 110.2 | 101.0 | -8% | 142.9 | 109.4 | -23% |
| groups (cache off) (async events) (dbs split) | 207 | 226 | +9% | 71.7 | 66.1 | -8% | 115.8 | 102.6 | -11% | 146.4 | 114.9 | -22% |
| groups (cache on) (async outbox) (dbs shared) | 229 | 245 | +7% | 66.3 | 62.7 | -5% | 100.0 | 93.2 | -7% | 116.4 | 103.2 | -11% |
| groups (cache on) (async outbox) (dbs split) | 238 | 245 | +3% | 62.5 | 63.0 | +1% | 96.3 | 93.4 | -3% | 110.6 | 115.1 | +4% |
| groups (cache on) (async events) (dbs shared) | 223 | 239 | +7% | 67.6 | 62.9 | -7% | 101.6 | 91.2 | -10% | 122.4 | 104.2 | -15% |
| groups (cache on) (async events) (dbs split) | 229 | 251 | +10% | 65.1 | 60.7 | -7% | 100.9 | 89.9 | -11% | 110.4 | 102.7 | -7% |

## Follow-up delivery effect (outbox to events)

Higher orders/s and lower latencies are better.

| mode | orders/s outbox | events | change | p50 outbox | events | change | p95 outbox | events | change | p99 outbox | events | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (cache off) (parallel off) (dbs shared) | 214 | 212 | -1% | 70.8 | 69.0 | -3% | 108.9 | 110.2 | +1% | 140.6 | 142.9 | +2% |
| groups (cache off) (parallel off) (dbs split) | 213 | 207 | -3% | 69.8 | 71.7 | +3% | 110.7 | 115.8 | +5% | 138.3 | 146.4 | +6% |
| groups (cache off) (parallel on) (dbs shared) | 232 | 226 | -3% | 66.1 | 66.2 | +0% | 99.3 | 101.0 | +2% | 112.7 | 109.4 | -3% |
| groups (cache off) (parallel on) (dbs split) | 215 | 226 | +5% | 71.2 | 66.1 | -7% | 108.6 | 102.6 | -6% | 122.5 | 114.9 | -6% |
| groups (cache on) (parallel off) (dbs shared) | 229 | 223 | -2% | 66.3 | 67.6 | +2% | 100.0 | 101.6 | +2% | 116.4 | 122.4 | +5% |
| groups (cache on) (parallel off) (dbs split) | 238 | 229 | -4% | 62.5 | 65.1 | +4% | 96.3 | 100.9 | +5% | 110.6 | 110.4 | -0% |
| groups (cache on) (parallel on) (dbs shared) | 245 | 239 | -2% | 62.7 | 62.9 | +0% | 93.2 | 91.2 | -2% | 103.2 | 104.2 | +1% |
| groups (cache on) (parallel on) (dbs split) | 245 | 251 | +3% | 63.0 | 60.7 | -4% | 93.4 | 89.9 | -4% | 115.1 | 102.7 | -11% |

## Database layout effect (one instance to one per deployable unit)

Higher orders/s and lower latencies are better.

| mode | orders/s shared | split | change | p50 shared | split | change | p95 shared | split | change | p99 shared | split | change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| groups (cache off) (parallel off) (async outbox) | 214 | 213 | -0% | 70.8 | 69.8 | -1% | 108.9 | 110.7 | +2% | 140.6 | 138.3 | -2% |
| groups (cache off) (parallel off) (async events) | 212 | 207 | -2% | 69.0 | 71.7 | +4% | 110.2 | 115.8 | +5% | 142.9 | 146.4 | +2% |
| groups (cache off) (parallel on) (async outbox) | 232 | 215 | -7% | 66.1 | 71.2 | +8% | 99.3 | 108.6 | +9% | 112.7 | 122.5 | +9% |
| groups (cache off) (parallel on) (async events) | 226 | 226 | -0% | 66.2 | 66.1 | -0% | 101.0 | 102.6 | +2% | 109.4 | 114.9 | +5% |
| groups (cache on) (parallel off) (async outbox) | 229 | 238 | +4% | 66.3 | 62.5 | -6% | 100.0 | 96.3 | -4% | 116.4 | 110.6 | -5% |
| groups (cache on) (parallel off) (async events) | 223 | 229 | +2% | 67.6 | 65.1 | -4% | 101.6 | 100.9 | -1% | 122.4 | 110.4 | -10% |
| groups (cache on) (parallel on) (async outbox) | 245 | 245 | -0% | 62.7 | 63.0 | +0% | 93.2 | 93.4 | +0% | 103.2 | 115.1 | +11% |
| groups (cache on) (parallel on) (async events) | 239 | 251 | +5% | 62.9 | 60.7 | -4% | 91.2 | 89.9 | -1% | 104.2 | 102.7 | -1% |

## Bytes on the wire per order

HTTP calls only (calls inside one process are not counted). Raw is the JSON-RPC envelope as JSON; wire is what crossed the connection. Request and response sizes are bytes per call, raw to wire.

| run | HTTP calls/order | JSON bytes/order | wire bytes/order | wire / JSON | request/call | response/call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| groups cache off parallel off async outbox dbs shared | 11.0 | 3,819 | 3,819 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel off async outbox dbs split | 11.0 | 3,818 | 3,818 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel off async events dbs shared | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel off async events dbs split | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel on async outbox dbs shared | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel on async outbox dbs split | 11.0 | 3,816 | 3,816 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel on async events dbs shared | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| groups cache off parallel on async events dbs split | 10.9 | 3,796 | 3,796 | 100% | 145 to 145 | 201 to 201 |
| groups cache on parallel off async outbox dbs shared | 7.8 | 2,840 | 2,840 | 100% | 165 to 165 | 200 to 200 |
| groups cache on parallel off async outbox dbs split | 7.8 | 2,840 | 2,840 | 100% | 165 to 165 | 200 to 200 |
| groups cache on parallel off async events dbs shared | 7.3 | 2,686 | 2,686 | 100% | 170 to 170 | 199 to 199 |
| groups cache on parallel off async events dbs split | 7.3 | 2,686 | 2,686 | 100% | 170 to 170 | 199 to 199 |
| groups cache on parallel on async outbox dbs shared | 8.4 | 3,039 | 3,039 | 100% | 160 to 160 | 200 to 200 |
| groups cache on parallel on async outbox dbs split | 8.4 | 3,037 | 3,037 | 100% | 160 to 160 | 200 to 200 |
| groups cache on parallel on async events dbs shared | 7.9 | 2,883 | 2,883 | 100% | 164 to 164 | 200 to 200 |
| groups cache on parallel on async events dbs split | 7.9 | 2,883 | 2,883 | 100% | 164 to 164 | 200 to 200 |

## Mean ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups cache off parallel off async outbox dbs shared | groups cache off parallel off async outbox dbs split | groups cache off parallel off async events dbs shared | groups cache off parallel off async events dbs split | groups cache off parallel on async outbox dbs shared | groups cache off parallel on async outbox dbs split | groups cache off parallel on async events dbs shared | groups cache off parallel on async events dbs split | groups cache on parallel off async outbox dbs shared | groups cache on parallel off async outbox dbs split | groups cache on parallel off async events dbs shared | groups cache on parallel off async events dbs split | groups cache on parallel on async outbox dbs shared | groups cache on parallel on async outbox dbs split | groups cache on parallel on async events dbs shared | groups cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 2.4 | 2.4 | 2.4 | 2.5 | 2.6 | 2.9 | 2.7 | 2.8 | 1.9 | 1.8 | 1.9 | 1.9 | 2.0 | 2.0 | 2.0 | 1.9 |
| orders -> inventory.reserve | 1.7 | 1.7 | 1.8 | 1.7 | 1.9 | 2.0 | 2.0 | 1.9 | 1.9 | 1.6 | 1.9 | 1.9 | 2.0 | 1.9 | 2.0 | 2.0 |
| orders -> shipping.quote | 1.9 | 1.9 | 2.0 | 2.0 | 2.3 | 2.5 | 2.5 | 2.5 | 1.1 | 1.1 | 1.2 | 1.1 | 1.9 | 2.0 | 2.0 | 2.0 |
| orders -> pricing.quote | 1.7 | 1.8 | 1.8 | 1.8 | 2.0 | 2.2 | 2.1 | 2.1 | 1.6 | 1.6 | 1.7 | 1.7 | 1.6 | 1.7 | 1.7 | 1.7 |
| orders -> payments.authorize | 1.5 | 1.5 | 1.5 | 1.6 | 1.7 | 1.8 | 1.7 | 1.7 | 1.7 | 1.7 | 1.8 | 1.7 | 1.7 | 1.8 | 1.8 | 1.7 |
| orders -> notifications.send | 1.1 | 1.1 | 1.4 | 1.5 | 1.1 | 1.2 | 1.7 | 1.7 | 0.9 | 0.9 | 0.9 | 0.9 | 0.9 | 0.9 | 1.0 | 1.0 |
| orders -> payments.capture | 1.0 | 1.0 | 1.0 | 1.0 | 1.1 | 1.2 | 1.1 | 1.1 | 1.2 | 1.2 | 1.2 | 1.2 | 1.2 | 1.2 | 1.2 | 1.2 |
| shipping -> customers.getCustomer | 0.6 | 0.6 | 0.6 | 0.7 | 0.7 | 0.8 | 0.8 | 0.8 | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 | 0.3 | 0.4 | 0.3 |
| shipping -> inventory.getStock | 0.4 | 0.4 | 0.4 | 0.4 | 0.5 | 0.5 | 0.5 | 0.5 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.3 |
| pricing -> customers.getCustomer | 0.6 | 0.7 | 0.7 | 0.7 | 0.8 | 0.9 | 0.8 | 0.8 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 | 0.7 |
| orders -> customers.getCustomer | 0.6 | 0.6 | 0.6 | 0.7 | 0.7 | 0.8 | 0.8 | 0.8 | 0.5 | 0.5 | 0.6 | 0.6 | 0.6 | 0.6 | 0.6 | 0.6 |
| notifications -> customers.getCustomer | 0.5 | 0.5 | 0.5 | 0.6 | 0.5 | 0.5 | 0.7 | 0.7 | 0.3 | 0.3 | 0.0 | 0.0 | 0.3 | 0.3 | 0.0 | 0.0 |
| shipping -> inventory.getReservation | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.3 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 |
| inventory -> catalog.productExists | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.3 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 |
| pricing -> catalog.getProduct | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 | 0.1 |
| payments -> customers.getPaymentMethod | 0.2 | 0.2 | 0.2 | 0.2 | 0.3 | 0.3 | 0.3 | 0.3 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 | 0.2 |
| orders -> inventory.release | 2.1 | 2.2 | 1.8 | 1.8 | 2.1 | 2.2 | 2.0 | 1.9 | 2.2 | 2.3 | 1.9 | 1.7 | 2.3 | 2.1 | 2.0 | 1.9 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## p95 ms per module-to-module call

Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). Ordered by total time spent.

| call | groups cache off parallel off async outbox dbs shared | groups cache off parallel off async outbox dbs split | groups cache off parallel off async events dbs shared | groups cache off parallel off async events dbs split | groups cache off parallel on async outbox dbs shared | groups cache off parallel on async outbox dbs split | groups cache off parallel on async events dbs shared | groups cache off parallel on async events dbs split | groups cache on parallel off async outbox dbs shared | groups cache on parallel off async outbox dbs split | groups cache on parallel off async events dbs shared | groups cache on parallel off async events dbs split | groups cache on parallel on async outbox dbs shared | groups cache on parallel on async outbox dbs split | groups cache on parallel on async events dbs shared | groups cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment | 3.6 | 3.9 | 3.7 | 4.1 | 4.2 | 4.7 | 4.3 | 4.6 | 3.0 | 3.0 | 2.9 | 2.8 | 3.1 | 3.3 | 3.1 | 3.0 |
| orders -> inventory.reserve | 2.6 | 2.7 | 2.7 | 2.8 | 3.0 | 3.4 | 3.0 | 3.3 | 2.8 | 2.8 | 3.0 | 2.9 | 3.2 | 3.1 | 3.2 | 3.1 |
| orders -> shipping.quote | 3.3 | 3.3 | 3.3 | 3.5 | 4.0 | 4.5 | 4.4 | 4.3 | 2.1 | 2.1 | 2.2 | 2.1 | 3.8 | 3.8 | 4.0 | 3.8 |
| orders -> pricing.quote | 2.9 | 2.9 | 2.8 | 3.1 | 3.4 | 3.9 | 3.7 | 3.7 | 3.1 | 3.1 | 3.3 | 3.2 | 3.2 | 3.2 | 3.5 | 3.2 |
| orders -> payments.authorize | 2.4 | 2.3 | 2.4 | 2.8 | 2.9 | 3.1 | 2.9 | 2.9 | 2.8 | 2.7 | 3.1 | 3.1 | 2.8 | 3.1 | 3.1 | 3.0 |
| orders -> notifications.send | 2.2 | 2.2 | 2.1 | 2.3 | 2.1 | 2.2 | 2.8 | 2.8 | 1.7 | 1.8 | 1.3 | 1.3 | 1.6 | 1.7 | 1.5 | 1.5 |
| orders -> payments.capture | 1.5 | 1.5 | 1.4 | 1.7 | 1.7 | 2.0 | 1.7 | 1.8 | 1.8 | 1.9 | 1.9 | 1.9 | 1.9 | 2.1 | 1.9 | 1.9 |
| shipping -> customers.getCustomer | 1.1 | 1.1 | 1.1 | 1.3 | 1.3 | 1.5 | 1.4 | 1.5 | 0.0 | 0.0 | 0.0 | 0.0 | 1.3 | 1.4 | 1.4 | 1.4 |
| shipping -> inventory.getStock | 0.7 | 0.7 | 0.7 | 0.7 | 0.8 | 0.9 | 0.9 | 0.9 | 0.6 | 0.6 | 0.6 | 0.6 | 0.7 | 0.7 | 0.7 | 0.7 |
| pricing -> customers.getCustomer | 1.1 | 1.1 | 1.1 | 1.2 | 1.3 | 1.5 | 1.5 | 1.5 | 1.7 | 1.6 | 1.8 | 1.8 | 1.5 | 1.6 | 1.6 | 1.6 |
| orders -> customers.getCustomer | 1.1 | 1.0 | 1.0 | 1.2 | 1.4 | 1.6 | 1.4 | 1.4 | 1.3 | 1.2 | 1.4 | 1.4 | 1.4 | 1.6 | 1.5 | 1.5 |
| notifications -> customers.getCustomer | 1.1 | 1.1 | 1.0 | 1.1 | 1.0 | 1.1 | 1.3 | 1.3 | 0.9 | 0.9 | 0.0 | 0.0 | 0.9 | 0.9 | 0.0 | 0.0 |
| shipping -> inventory.getReservation | 0.5 | 0.5 | 0.5 | 0.5 | 0.6 | 0.6 | 0.6 | 0.6 | 0.5 | 0.5 | 0.5 | 0.5 | 0.6 | 0.6 | 0.6 | 0.5 |
| inventory -> catalog.productExists | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.5 | 0.4 | 0.5 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| pricing -> catalog.getProduct | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4 | 0.4 | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 | 0.3 |
| payments -> customers.getPaymentMethod | 0.4 | 0.4 | 0.4 | 0.4 | 0.4 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 | 0.5 |
| orders -> inventory.release | 2.8 | 3.0 | 2.8 | 3.3 | 2.8 | 3.3 | 3.9 | 3.5 | 3.0 | 4.0 | 3.3 | 2.6 | 3.0 | 3.2 | 3.7 | 2.5 |
| (calls per order) | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 | 19.9 |

## Cache hit rate

Share of calls to each `@Cached` method answered from the cache.

| method | groups cache off parallel off async outbox dbs shared | groups cache off parallel off async outbox dbs split | groups cache off parallel off async events dbs shared | groups cache off parallel off async events dbs split | groups cache off parallel on async outbox dbs shared | groups cache off parallel on async outbox dbs split | groups cache off parallel on async events dbs shared | groups cache off parallel on async events dbs split | groups cache on parallel off async outbox dbs shared | groups cache on parallel off async outbox dbs split | groups cache on parallel off async events dbs shared | groups cache on parallel off async events dbs split | groups cache on parallel on async outbox dbs shared | groups cache on parallel on async outbox dbs split | groups cache on parallel on async events dbs shared | groups cache on parallel on async events dbs split |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| catalog.getProduct | - | - | - | - | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% |
| catalog.productExists | - | - | - | - | - | - | - | - | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% | 78.0% |
| customers.getCustomer | - | - | - | - | - | - | - | - | 63.8% | 63.8% | 74.0% | 74.0% | 50.7% | 50.8% | 61.0% | 61.0% |
| customers.getPaymentMethod | - | - | - | - | - | - | - | - | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% | 35.4% |

## Calls by binding

Count and mean latency of the module-to-module calls, by how each was served.

| run | cache | local | loopback-codec | loopback-http | remote |
| --- | ---: | ---: | ---: | ---: | ---: |
| groups cache off parallel off async outbox dbs shared | - | 13500 calls, 0.35 ms | - | - | 16419 calls, 1.22 ms |
| groups cache off parallel off async outbox dbs split | - | 13500 calls, 0.35 ms | - | - | 16419 calls, 1.23 ms |
| groups cache off parallel off async events dbs shared | - | 13500 calls, 0.39 ms | - | - | 16419 calls, 1.24 ms |
| groups cache off parallel off async events dbs split | - | 13500 calls, 0.40 ms | - | - | 16419 calls, 1.27 ms |
| groups cache off parallel on async outbox dbs shared | - | 13500 calls, 0.40 ms | - | - | 16419 calls, 1.38 ms |
| groups cache off parallel on async outbox dbs split | - | 13500 calls, 0.43 ms | - | - | 16419 calls, 1.51 ms |
| groups cache off parallel on async events dbs shared | - | 13500 calls, 0.47 ms | - | - | 16419 calls, 1.45 ms |
| groups cache off parallel on async events dbs split | - | 13500 calls, 0.48 ms | - | - | 16419 calls, 1.45 ms |
| groups cache on parallel off async outbox dbs shared | 9988 calls, 0.01 ms | 8261 calls, 0.42 ms | - | - | 11670 calls, 1.40 ms |
| groups cache on parallel off async outbox dbs split | 9986 calls, 0.01 ms | 8261 calls, 0.40 ms | - | - | 11672 calls, 1.34 ms |
| groups cache on parallel off async events dbs shared | 10747 calls, 0.01 ms | 8261 calls, 0.42 ms | - | - | 10911 calls, 1.50 ms |
| groups cache on parallel off async events dbs split | 10747 calls, 0.01 ms | 8261 calls, 0.41 ms | - | - | 10911 calls, 1.47 ms |
| groups cache on parallel on async outbox dbs shared | 9013 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12645 calls, 1.50 ms |
| groups cache on parallel on async outbox dbs split | 9022 calls, 0.01 ms | 8261 calls, 0.43 ms | - | - | 12636 calls, 1.52 ms |
| groups cache on parallel on async events dbs shared | 9780 calls, 0.01 ms | 8261 calls, 0.46 ms | - | - | 11878 calls, 1.62 ms |
| groups cache on parallel on async events dbs split | 9778 calls, 0.01 ms | 8261 calls, 0.44 ms | - | - | 11880 calls, 1.55 ms |

## Detail per run

The same figures one run at a time, with every module-to-module call and each call's binding.

### groups (cache off, parallel quotes off, async outbox, dbs shared)

1500 orders in 7.0 s = 213.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 35.4 | 74.6 | 70.8 | 100.3 | 108.9 | 140.6 | 173.1 |
| placeOrder | declined | 27 | 49.5 | 67.4 | 62.8 | 82.3 | 88.6 | 93.9 | 93.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.4 | 2.2 | 3.6 | 4.3 | 6.9 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.3 | 4.7 | 8.1 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.7 | 1.6 | 2.6 | 3.4 | 5.9 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 2.9 | 3.9 | 9.0 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.5 | 1.4 | 2.4 | 3.1 | 6.2 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.6 | 0.6 | 1.1 | 1.4 | 4.3 |
| orders -> notifications.send [local] | 1473 |  | 1.1 | 1.0 | 2.2 | 3.1 | 3.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 0.9 | 1.5 | 1.8 | 7.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.9 | 4.9 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.6 | 1.1 | 1.4 | 3.1 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.5 | 1.1 | 1.3 | 40.5 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.1 | 1.8 | 2.0 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 4.3 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 0.9 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.9 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 1.9 | 2.8 | 3.0 | 3.0 |

By binding: local 13500 calls, mean 0.35 ms, remote 16419 calls, mean 1.22 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes off, async outbox, dbs split)

1500 orders in 7.0 s = 212.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.7 | 74.9 | 69.8 | 103.4 | 110.7 | 138.3 | 164.2 |
| placeOrder | declined | 27 | 47.1 | 68.3 | 62.4 | 90.3 | 97.9 | 99.4 | 99.4 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.4 | 2.2 | 3.9 | 4.7 | 6.9 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.3 | 4.1 | 7.6 |
| orders -> pricing.quote [remote] | 1500 |  | 1.8 | 1.6 | 2.9 | 3.6 | 48.0 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.7 | 1.6 | 2.7 | 3.3 | 4.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.5 | 1.4 | 2.3 | 3.2 | 7.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.6 | 0.6 | 1.1 | 1.4 | 3.9 |
| orders -> notifications.send [local] | 1473 |  | 1.1 | 1.0 | 2.2 | 3.0 | 3.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 0.9 | 1.5 | 1.9 | 47.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.8 | 3.1 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.1 | 1.4 | 4.1 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.5 | 1.0 | 1.4 | 3.7 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.1 | 1.8 | 2.2 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.6 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.2 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.5 | 0.7 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 3.0 | 3.1 | 3.1 |

By binding: local 13500 calls, mean 0.35 ms, remote 16419 calls, mean 1.23 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes off, async events, dbs shared)

1500 orders in 7.1 s = 212.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 32.8 | 75.2 | 69.0 | 98.8 | 110.2 | 142.9 | 161.0 |
| placeOrder | declined | 27 | 52.2 | 69.4 | 60.6 | 96.2 | 96.2 | 111.1 | 111.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.4 | 2.2 | 3.7 | 4.3 | 11.8 |
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.3 | 4.1 | 42.3 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.8 | 1.7 | 2.7 | 3.3 | 42.8 |
| orders -> pricing.quote [remote] | 1500 |  | 1.8 | 1.6 | 2.8 | 3.7 | 42.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.5 | 1.4 | 2.4 | 3.0 | 7.5 |
| orders -> notifications.send [local] | 1473 |  | 1.4 | 1.3 | 2.1 | 2.6 | 7.8 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.6 | 0.6 | 1.1 | 1.3 | 7.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 0.9 | 1.4 | 1.8 | 11.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.8 | 2.5 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.1 | 1.4 | 3.3 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.6 | 0.5 | 1.0 | 1.4 | 6.8 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.5 | 1.0 | 1.2 | 3.9 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.9 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.7 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.3 |
| orders -> inventory.release [remote] | 27 |  | 1.8 | 1.6 | 2.8 | 3.8 | 3.8 |

By binding: local 13500 calls, mean 0.39 ms, remote 16419 calls, mean 1.24 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes off, async events, dbs split)

1500 orders in 7.2 s = 207.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 33.0 | 77.0 | 71.7 | 105.1 | 115.8 | 146.4 | 162.2 |
| placeOrder | declined | 27 | 48.4 | 71.1 | 63.9 | 95.8 | 101.5 | 140.1 | 140.1 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.5 | 2.3 | 4.1 | 5.0 | 6.6 |
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.5 | 4.7 | 5.7 |
| orders -> pricing.quote [remote] | 1500 |  | 1.8 | 1.7 | 3.1 | 4.0 | 5.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.7 | 1.5 | 2.8 | 3.9 | 6.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.6 | 1.5 | 2.8 | 3.8 | 12.3 |
| orders -> notifications.send [local] | 1473 |  | 1.5 | 1.4 | 2.3 | 3.0 | 9.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.6 | 1.3 | 1.7 | 3.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.0 | 0.9 | 1.7 | 2.1 | 3.1 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.4 | 0.7 | 0.9 | 1.6 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.2 | 1.5 | 3.3 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.5 | 1.2 | 1.8 | 8.2 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.6 | 0.5 | 1.1 | 1.6 | 8.0 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 0.8 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 1.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.7 | 3.1 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.2 | 0.2 | 0.4 | 0.6 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 1.8 | 1.4 | 3.3 | 3.9 | 3.9 |

By binding: local 13500 calls, mean 0.40 ms, remote 16419 calls, mean 1.27 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async outbox, dbs shared)

1500 orders in 6.5 s = 232.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 35.7 | 68.6 | 66.1 | 89.6 | 99.3 | 112.7 | 127.5 |
| placeOrder | declined | 27 | 46.6 | 61.3 | 51.4 | 78.7 | 80.3 | 93.0 | 93.0 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.6 | 2.4 | 4.2 | 5.5 | 9.9 |
| orders -> shipping.quote [remote] | 1500 |  | 2.3 | 2.1 | 4.0 | 5.1 | 8.4 |
| orders -> pricing.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.4 | 4.7 | 7.9 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.0 | 4.0 | 6.8 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.9 | 4.0 | 9.9 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.7 | 0.7 | 1.3 | 1.8 | 3.7 |
| orders -> notifications.send [local] | 1473 |  | 1.1 | 1.0 | 2.1 | 3.1 | 4.9 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.2 | 3.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.4 | 0.8 | 1.1 | 4.0 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.3 | 1.9 | 5.5 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.7 | 0.6 | 1.4 | 2.1 | 5.9 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.4 | 1.0 | 1.7 | 2.4 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 3.4 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.3 | 0.4 | 2.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.7 | 2.0 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.2 | 0.4 | 0.6 | 2.5 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 2.1 | 2.8 | 2.9 | 2.9 |

By binding: local 13500 calls, mean 0.40 ms, remote 16419 calls, mean 1.38 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async outbox, dbs split)

1500 orders in 7.0 s = 215.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.4 | 74.0 | 71.2 | 102.1 | 108.6 | 122.5 | 128.3 |
| placeOrder | declined | 27 | 46.3 | 68.7 | 59.8 | 99.1 | 99.7 | 113.9 | 113.9 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.9 | 2.6 | 4.7 | 6.0 | 7.1 |
| orders -> shipping.quote [remote] | 1500 |  | 2.5 | 2.3 | 4.5 | 5.7 | 10.3 |
| orders -> pricing.quote [remote] | 1500 |  | 2.2 | 2.0 | 3.9 | 4.9 | 9.3 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 3.4 | 4.1 | 5.6 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 3.1 | 4.0 | 8.4 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.5 | 2.1 | 7.4 |
| orders -> notifications.send [local] | 1473 |  | 1.2 | 1.1 | 2.2 | 3.1 | 3.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 2.0 | 2.6 | 4.7 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.5 | 0.9 | 1.2 | 3.4 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.9 | 0.7 | 1.5 | 2.3 | 7.1 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.6 | 2.3 | 7.2 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.5 | 0.5 | 1.1 | 1.7 | 2.7 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.3 | 0.2 | 0.5 | 0.7 | 3.1 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 3.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.8 | 2.1 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.2 | 0.5 | 0.7 | 2.0 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.2 | 3.3 | 3.5 | 3.5 |

By binding: local 13500 calls, mean 0.43 ms, remote 16419 calls, mean 1.51 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async events, dbs shared)

1500 orders in 6.6 s = 226.4 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 31.8 | 70.4 | 66.2 | 93.5 | 101.0 | 109.4 | 125.4 |
| placeOrder | declined | 27 | 47.8 | 65.7 | 60.5 | 90.4 | 94.0 | 95.8 | 95.8 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.7 | 2.5 | 4.3 | 5.5 | 6.5 |
| orders -> shipping.quote [remote] | 1500 |  | 2.5 | 2.2 | 4.4 | 5.8 | 8.9 |
| orders -> pricing.quote [remote] | 1500 |  | 2.1 | 1.9 | 3.7 | 4.9 | 8.2 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 3.0 | 4.1 | 9.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.9 | 3.8 | 9.5 |
| orders -> notifications.send [local] | 1473 |  | 1.7 | 1.5 | 2.8 | 3.9 | 8.2 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.4 | 2.0 | 5.4 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.7 | 2.3 | 3.6 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.5 | 0.9 | 1.2 | 4.6 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.2 | 5.3 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.6 | 1.4 | 2.1 | 4.1 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.7 | 0.6 | 1.3 | 1.8 | 4.6 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.6 | 4.3 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 1.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.8 | 1.6 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.2 | 0.5 | 0.7 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 1.7 | 3.9 | 4.8 | 4.8 |

By binding: local 13500 calls, mean 0.47 ms, remote 16419 calls, mean 1.45 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache off, parallel quotes on, async events, dbs split)

1500 orders in 6.7 s = 225.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 28.4 | 70.7 | 66.1 | 96.3 | 102.6 | 114.9 | 122.4 |
| placeOrder | declined | 27 | 47.0 | 64.3 | 57.0 | 86.9 | 87.9 | 103.6 | 103.6 |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.8 | 2.5 | 4.6 | 5.6 | 7.2 |
| orders -> shipping.quote [remote] | 1500 |  | 2.5 | 2.2 | 4.3 | 5.5 | 7.6 |
| orders -> pricing.quote [remote] | 1500 |  | 2.1 | 1.9 | 3.7 | 4.5 | 6.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.3 | 4.0 | 5.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.9 | 3.5 | 4.5 |
| orders -> notifications.send [local] | 1473 |  | 1.7 | 1.6 | 2.8 | 3.4 | 5.6 |
| shipping -> customers.getCustomer [remote] | 2973 |  | 0.8 | 0.7 | 1.5 | 2.0 | 4.1 |
| orders -> payments.capture [remote] | 1473 |  | 1.1 | 1.0 | 1.8 | 2.7 | 6.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.5 | 0.5 | 0.9 | 1.2 | 3.7 |
| pricing -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.5 | 2.0 | 4.3 |
| orders -> customers.getCustomer [remote] | 1500 |  | 0.8 | 0.7 | 1.4 | 1.9 | 3.8 |
| notifications -> customers.getCustomer [remote] | 1473 |  | 0.7 | 0.6 | 1.3 | 1.9 | 3.2 |
| inventory -> catalog.productExists [local] | 3018 |  | 0.2 | 0.2 | 0.5 | 0.6 | 3.5 |
| pricing -> catalog.getProduct [local] | 3018 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.4 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.4 | 0.6 | 0.8 | 2.3 |
| payments -> customers.getPaymentMethod [local] | 1500 |  | 0.3 | 0.2 | 0.5 | 0.6 | 2.7 |
| orders -> inventory.release [remote] | 27 |  | 1.9 | 1.4 | 3.5 | 4.0 | 4.0 |

By binding: local 13500 calls, mean 0.48 ms, remote 16419 calls, mean 1.45 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-off-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async outbox, dbs shared)

1500 orders in 6.6 s = 228.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 26.3 | 69.7 | 66.3 | 92.4 | 100.0 | 116.4 | 133.3 |
| placeOrder | declined | 27 | 49.7 | 66.6 | 61.5 | 85.7 | 89.9 | 95.4 | 95.4 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4749 | 63.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 2.8 | 3.6 | 5.7 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.0 | 4.1 | 8.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 2.8 | 3.5 | 4.5 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.1 | 4.3 | 5.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.8 | 2.3 | 7.4 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 1.0 | 2.1 | 2.7 | 5.3 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.7 | 3.1 | 4.4 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.8 | 2.5 | 3.9 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.6 | 0.8 | 3.1 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.7 | 1.4 | 1.9 | 3.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 0.9 |
| notifications -> customers.getCustomer [remote] | 759 |  | 0.6 | 0.5 | 1.4 | 1.9 | 2.5 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.6 | 1.4 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.7 | 1.4 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.5 | 2.5 |
| orders -> inventory.release [remote] | 27 |  | 2.2 | 2.1 | 3.0 | 3.4 | 3.4 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 714 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9988 calls, mean 0.01 ms, local 8261 calls, mean 0.42 ms, remote 11670 calls, mean 1.40 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-off-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async outbox, dbs split)

1500 orders in 6.3 s = 237.7 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 39.9 | 67.0 | 62.5 | 90.7 | 96.3 | 110.6 | 142.9 |
| placeOrder | declined | 27 | 46.1 | 63.5 | 57.6 | 87.9 | 89.1 | 90.1 | 90.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4747 | 63.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.8 | 1.6 | 3.0 | 4.2 | 6.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.7 | 3.7 | 9.7 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.6 | 1.5 | 2.8 | 3.7 | 5.9 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.4 | 3.1 | 4.3 | 9.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.7 | 4.4 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 0.9 | 2.1 | 2.9 | 4.1 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.8 | 3.2 | 7.6 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.4 | 8.2 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.8 | 1.4 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.7 | 1.4 | 2.1 | 5.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.3 | 0.3 | 0.5 | 0.7 | 1.3 |
| notifications -> customers.getCustomer [remote] | 761 |  | 0.6 | 0.5 | 1.3 | 2.0 | 2.5 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.8 | 7.8 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.7 | 1.0 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.6 | 0.8 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.2 | 4.0 | 4.1 | 4.1 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.3 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 712 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9986 calls, mean 0.01 ms, local 8261 calls, mean 0.40 ms, remote 11672 calls, mean 1.34 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-off-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async events, dbs shared)

1500 orders in 6.7 s = 223.5 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 24.1 | 71.3 | 67.6 | 93.0 | 101.6 | 122.4 | 147.4 |
| placeOrder | declined | 27 | 49.3 | 67.4 | 63.3 | 88.2 | 90.8 | 96.7 | 96.7 |

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
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.8 | 3.0 | 3.7 | 6.1 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 2.9 | 3.8 | 8.9 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 3.1 | 4.2 | 8.6 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 3.3 | 4.3 | 6.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.6 | 7.3 |
| orders -> shipping.quote [remote] | 1500 |  | 1.2 | 1.0 | 2.2 | 3.0 | 5.0 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.3 | 1.9 | 2.3 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.9 | 2.6 | 4.7 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.6 | 0.8 | 3.4 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.5 | 2.4 | 2.8 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 1.4 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.9 | 2.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.7 | 1.2 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.6 | 3.1 |
| orders -> inventory.release [remote] | 27 |  | 1.9 | 1.6 | 3.3 | 3.4 | 3.4 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.2 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10747 calls, mean 0.01 ms, local 8261 calls, mean 0.42 ms, remote 10911 calls, mean 1.50 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-off-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes off, async events, dbs split)

1500 orders in 6.6 s = 228.9 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 31.9 | 69.7 | 65.1 | 93.6 | 100.9 | 110.4 | 120.2 |
| placeOrder | declined | 27 | 45.5 | 63.3 | 61.0 | 83.0 | 83.2 | 96.5 | 96.5 |

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
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.7 | 2.9 | 3.7 | 9.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.7 | 2.8 | 3.7 | 7.2 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 3.1 | 4.0 | 5.8 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.5 | 3.2 | 4.1 | 7.3 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.6 | 8.8 |
| orders -> shipping.quote [remote] | 1500 |  | 1.1 | 1.0 | 2.1 | 2.9 | 9.5 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.9 | 1.3 | 1.7 | 5.9 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.9 | 2.4 | 5.4 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.2 | 0.6 | 0.8 | 3.3 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.8 | 0.7 | 1.5 | 2.1 | 2.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 1.3 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.8 | 1.1 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.7 | 4.4 |
| inventory -> catalog.productExists [local] | 664 |  | 0.2 | 0.2 | 0.4 | 0.5 | 2.9 |
| orders -> inventory.release [remote] | 27 |  | 1.7 | 1.5 | 2.6 | 3.0 | 3.0 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.2 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2973 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 10747 calls, mean 0.01 ms, local 8261 calls, mean 0.41 ms, remote 10911 calls, mean 1.47 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-off-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox, dbs shared)

1500 orders in 6.1 s = 245.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 27.5 | 65.1 | 62.7 | 86.1 | 93.2 | 103.2 | 107.7 |
| placeOrder | declined | 27 | 43.3 | 59.9 | 57.6 | 82.5 | 84.0 | 89.1 | 89.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3774 | 50.7% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 3.2 | 4.2 | 8.7 |
| orders -> shipping.quote [remote] | 1500 |  | 1.9 | 1.8 | 3.8 | 5.2 | 8.0 |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.8 | 3.1 | 4.2 | 8.6 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.6 | 2.8 | 3.7 | 8.3 |
| orders -> pricing.quote [remote] | 1500 |  | 1.6 | 1.5 | 3.2 | 4.3 | 7.8 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.6 | 4.0 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.6 | 2.7 | 3.5 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.0 | 2.6 |
| shipping -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.7 | 2.6 | 7.0 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.4 | 7.0 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.6 | 2.7 | 4.5 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.7 | 1.1 |
| notifications -> customers.getCustomer [remote] | 767 |  | 0.5 | 0.5 | 1.3 | 1.8 | 2.1 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.5 | 0.7 | 6.3 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.8 | 1.0 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.5 | 1.0 |
| orders -> inventory.release [remote] | 27 |  | 2.3 | 2.4 | 3.0 | 3.2 | 3.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.3 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| notifications -> customers.getCustomer [cache] | 706 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9013 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12645 calls, mean 1.50 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-on-async-outbox-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async outbox, dbs split)

1500 orders in 6.1 s = 244.6 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 27.7 | 65.2 | 63.0 | 85.4 | 93.4 | 115.1 | 134.6 |
| placeOrder | declined | 27 | 40.0 | 60.7 | 57.3 | 82.9 | 84.3 | 97.1 | 97.1 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 3783 | 50.8% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.8 | 3.8 | 5.1 | 10.2 |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.8 | 3.3 | 4.5 | 7.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 1.9 | 1.7 | 3.1 | 4.2 | 9.4 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.6 | 3.1 | 4.0 | 6.8 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.5 | 3.2 | 4.6 | 10.0 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 2.1 | 3.0 | 7.4 |
| orders -> notifications.send [local] | 1473 |  | 0.9 | 0.8 | 1.7 | 2.8 | 6.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.0 | 2.4 |
| shipping -> customers.getCustomer [remote] | 967 |  | 1.0 | 0.9 | 1.7 | 2.8 | 8.4 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.8 | 3.0 | 8.0 |
| orders -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.8 | 1.8 | 3.1 | 5.7 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.9 | 1.4 |
| notifications -> customers.getCustomer [remote] | 758 |  | 0.6 | 0.5 | 1.3 | 1.9 | 3.7 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.9 | 1.1 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.8 | 1.5 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.7 | 1.6 |
| orders -> inventory.release [remote] | 27 |  | 2.1 | 2.1 | 3.2 | 3.2 | 3.2 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.1 | 0.3 |
| shipping -> customers.getCustomer [cache] | 2006 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| notifications -> customers.getCustomer [cache] | 715 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9022 calls, mean 0.01 ms, local 8261 calls, mean 0.43 ms, remote 12636 calls, mean 1.52 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-on-async-outbox-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events, dbs shared)

1500 orders in 6.3 s = 239.0 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 30.1 | 66.6 | 62.9 | 86.4 | 91.2 | 104.2 | 118.0 |
| placeOrder | declined | 27 | 45.4 | 63.4 | 60.3 | 83.1 | 92.2 | 94.6 | 94.6 |

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
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.9 | 4.0 | 5.6 | 9.3 |
| orders -> shipping.createShipment [remote] | 1473 |  | 2.0 | 1.9 | 3.1 | 4.1 | 5.8 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.9 | 3.2 | 4.0 | 6.1 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.8 | 1.7 | 3.1 | 4.0 | 6.8 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 3.5 | 4.6 | 8.2 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.1 | 1.9 | 2.6 | 6.3 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 1.0 | 1.5 | 2.1 | 7.8 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.4 | 0.3 | 0.7 | 1.0 | 3.3 |
| pricing -> customers.getCustomer [remote] | 967 |  | 1.1 | 1.0 | 1.8 | 3.2 | 7.3 |
| shipping -> customers.getCustomer [remote] | 969 |  | 1.1 | 1.0 | 1.9 | 3.0 | 7.2 |
| orders -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.8 | 1.7 | 2.6 | 7.0 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.6 | 0.7 | 1.2 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.8 | 1.1 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.8 | 1.7 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.4 | 0.7 | 3.7 |
| orders -> inventory.release [remote] | 27 |  | 2.0 | 2.0 | 3.7 | 3.9 | 3.9 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.2 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.1 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 533 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9780 calls, mean 0.01 ms, local 8261 calls, mean 0.46 ms, remote 11878 calls, mean 1.62 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-on-async-events-dbs-shared` (`orders.csv`, `calls.csv`, `summary.json`).

### groups (cache on, parallel quotes on, async events, dbs split)

1500 orders in 6.0 s = 251.1 orders/s; 1473 ok, 27 declined (expected), 0 failed.

#### placeOrder round trip (client side), ms

| call | result | n | min | mean | p50 | p90 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| placeOrder | ok | 1473 | 25.3 | 63.4 | 60.7 | 84.7 | 89.9 | 102.7 | 109.7 |
| placeOrder | declined | 27 | 42.0 | 61.0 | 60.0 | 81.2 | 82.6 | 89.3 | 89.3 |

#### Cache hit rate (`@Cached` methods)

| method | calls | hits | hit rate |
| --- | --- | ---: | ---: |
| catalog.getProduct | 3018 | 2354 | 78.0% |
| catalog.productExists | 3018 | 2354 | 78.0% |
| customers.getCustomer | 7446 | 4539 | 61.0% |
| customers.getPaymentMethod | 1500 | 531 | 35.4% |

#### Module-to-module calls, ms

Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.

| call [binding] | n | errors | mean | p50 | p95 | p99 | max |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote [remote] | 1500 |  | 2.0 | 1.9 | 3.8 | 4.7 | 6.4 |
| orders -> inventory.reserve [remote] | 1500 |  | 2.0 | 1.8 | 3.1 | 4.1 | 6.7 |
| orders -> shipping.createShipment [remote] | 1473 |  | 1.9 | 1.8 | 3.0 | 3.7 | 6.3 |
| orders -> payments.authorize [remote] | 1500 | 27 | 1.7 | 1.5 | 3.0 | 3.9 | 4.9 |
| orders -> pricing.quote [remote] | 1500 |  | 1.7 | 1.6 | 3.2 | 4.2 | 5.7 |
| orders -> payments.capture [remote] | 1473 |  | 1.2 | 1.0 | 1.9 | 2.6 | 8.4 |
| orders -> notifications.send [local] | 1473 |  | 1.0 | 0.9 | 1.5 | 1.9 | 3.7 |
| shipping -> inventory.getStock [local] | 3018 |  | 0.3 | 0.3 | 0.7 | 1.0 | 3.1 |
| shipping -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.5 | 3.1 |
| pricing -> customers.getCustomer [remote] | 969 |  | 1.0 | 0.9 | 1.7 | 2.3 | 3.9 |
| orders -> customers.getCustomer [remote] | 969 |  | 0.9 | 0.8 | 1.7 | 2.6 | 3.1 |
| shipping -> inventory.getReservation [local] | 1473 |  | 0.4 | 0.3 | 0.5 | 0.7 | 0.9 |
| payments -> customers.getPaymentMethod [local] | 969 |  | 0.3 | 0.3 | 0.6 | 0.7 | 1.2 |
| inventory -> catalog.productExists [local] | 664 |  | 0.3 | 0.3 | 0.5 | 0.7 | 0.9 |
| pricing -> catalog.getProduct [local] | 664 |  | 0.3 | 0.2 | 0.5 | 0.6 | 0.9 |
| orders -> inventory.release [remote] | 27 |  | 1.9 | 1.9 | 2.5 | 3.6 | 3.6 |
| pricing -> catalog.getProduct [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.1 | 0.2 |
| inventory -> catalog.productExists [cache] | 2354 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.3 |
| payments -> customers.getPaymentMethod [cache] | 531 |  | 0.0 | 0.0 | 0.1 | 0.2 | 0.3 |
| notifications -> customers.getCustomer [cache] | 1473 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| shipping -> customers.getCustomer [cache] | 2004 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| orders -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |
| pricing -> customers.getCustomer [cache] | 531 |  | 0.0 | 0.0 | 0.0 | 0.0 | 0.0 |

By binding: cache 9778 calls, mean 0.01 ms, local 8261 calls, mean 0.44 ms, remote 11880 calls, mean 1.55 ms. 19.9 module-to-module calls per order.

Raw samples: `groups-cache-on-parallel-on-async-events-dbs-split` (`orders.csv`, `calls.csv`, `summary.json`).

## Notes

- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.
- Tracing adds overhead; compare with `--no-trace` for pure client round trips.
- Client and servers share one machine, so CPU contention is part of every figure.
