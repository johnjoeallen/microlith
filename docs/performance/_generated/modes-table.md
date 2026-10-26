| Mode | Orders/s | vs modulith | p50 ms | p95 ms | p99 ms | Calls/order | Calls by binding (mean latency) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| modulith | 444 | 100% | 33.9 | 48.5 | 82.6 | 19.9 | cache 8,994 (0.01 ms), local 20,925 (0.49 ms) |
| macro-modulith | 446 | 100% | 34.0 | 48.8 | 53.3 | 19.9 | cache 8,991 (0.01 ms), local 20,928 (0.49 ms) |
| loopback-codec | 444 | 100% | 34.6 | 45.6 | 50.2 | 19.9 | cache 8,994 (0.00 ms), loopback-codec 20,925 (0.51 ms) |
| loopback-http | 315 | 71% | 47.6 | 70.3 | 77.6 | 19.9 | cache 9,008 (0.00 ms), loopback-http 20,911 (0.91 ms) |
| groups | 248 | 56% | 62.1 | 90.1 | 100.1 | 19.9 | cache 9,011 (0.01 ms), local 8,261 (0.43 ms), remote 12,647 (1.48 ms) |
| split-dbs | 255 | 57% | 60.2 | 86.3 | 93.5 | 19.9 | cache 9,021 (0.01 ms), local 8,261 (0.43 ms), remote 12,637 (1.45 ms) |
| microservices | 160 | 36% | 96.1 | 138.7 | 180.9 | 19.9 | cache 8,635 (0.01 ms), remote 21,284 (2.19 ms) |
| microservices-macro | 160 | 36% | 97.5 | 137.4 | 176.1 | 19.9 | cache 8,638 (0.01 ms), remote 21,281 (2.17 ms) |
