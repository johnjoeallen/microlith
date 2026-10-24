| Order size | Mode | HTTP calls/order | JSON bytes/order | JSON bytes/call | gzip | bson | bson-gzip |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 to 4 lines (the realistic mix) | loopback-http | 13.7 | 4,340 | 318 | 89% | 107% | 98% |
| 1 to 4 lines (the realistic mix) | groups | 8.0 | 2,911 | 364 | 85% | 107% | 94% |
| 1 to 4 lines (the realistic mix) | microservices | 14.3 | 4,533 | 316 | 89% | 107% | 98% |
| 40 lines | loopback-http | 55.3 | 21,075 | 381 | 63% | 112% | 71% |
| 40 lines | groups | 8.3 | 11,013 | 1327 | 31% | 116% | 39% |
| 40 lines | microservices | 56.1 | 21,300 | 380 | 63% | 112% | 71% |
| 150 lines | groups | 8.6 | 34,521 | 4029 | 17% | 119% | 24% |
| 150 lines | microservices | 168.0 | 68,833 | 410 | 57% | 114% | 64% |
