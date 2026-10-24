| Mode | Orders/s | vs modulith | p50 ms | p95 ms | p99 ms | Calls/order | Calls by binding (mean latency) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| modulith | 336 | 100% | 11.6 | 16.4 | 18.3 | 20.0 | cache 7,050 (0.03 ms), local 16,965 (0.69 ms) |
| macro-monolith | 331 | 98% | 11.6 | 16.3 | 18.8 | 20.0 | cache 7,050 (0.03 ms), local 16,965 (0.71 ms) |
| loopback-codec | 314 | 93% | 12.4 | 17.3 | 20.2 | 20.0 | cache 7,050 (0.00 ms), loopback-codec 16,965 (0.77 ms) |
| loopback-http | 238 | 71% | 16.3 | 24.3 | 27.9 | 20.0 | cache 7,059 (0.00 ms), loopback-http 16,956 (1.31 ms) |
| groups | 225 | 67% | 17.4 | 24.9 | 28.7 | 20.0 | cache 7,073 (0.02 ms), local 6,788 (0.54 ms), remote 10,154 (1.66 ms) |
| split-dbs | 230 | 68% | 16.7 | 25.0 | 28.0 | 20.0 | cache 7,075 (0.02 ms), local 6,788 (0.54 ms), remote 10,152 (1.63 ms) |
| microservices | 166 | 49% | 22.7 | 34.9 | 77.0 | 20.0 | cache 6,722 (0.01 ms), remote 17,293 (1.98 ms) |
| microservices-macro | 178 | 53% | 21.5 | 33.7 | 44.7 | 20.0 | cache 6,726 (0.01 ms), remote 17,289 (1.88 ms) |
