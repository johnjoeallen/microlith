<div class="wide-table" markdown>

| Databases | Cache | Parallel | Follow-ups | modulith | macro-modulith | loopback-codec | loopback-http | groups | split-dbs | microservices | microservices-macro |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| shared | on | on | outbox | 34.1 | 34.2 | 33.3 | 46.4 | 62.7 | - | 96.4 | 94.4 |
| shared | on | on | events | 35.0 | 33.9 | 35.1 | 47.7 | 62.9 | - | 99.3 | 101.2 |
| shared | on | off | outbox | 34.3 | 33.4 | 34.0 | 47.4 | 66.3 | - | 107.8 | 107.2 |
| shared | on | off | events | 35.7 | 35.2 | 35.8 | 48.7 | 67.6 | - | 111.7 | 114.8 |
| shared | off | on | outbox | 35.9 | 35.9 | 36.0 | 52.6 | 66.1 | - | 112.4 | 114.5 |
| shared | off | on | events | 37.9 | 37.7 | 37.7 | 55.1 | 66.2 | - | 117.4 | 118.7 |
| shared | off | off | outbox | 37.7 | 37.6 | 38.8 | 59.0 | 70.8 | - | 130.0 | 132.3 |
| shared | off | off | events | 39.8 | 40.0 | 40.2 | 60.3 | 69.0 | - | 134.9 | 137.6 |
| split | on | on | outbox | - | - | - | - | 63.0 | 61.3 | 96.1 | 97.5 |
| split | on | on | events | - | - | - | - | 60.7 | 61.5 | 102.4 | 101.7 |
| split | on | off | outbox | - | - | - | - | 62.5 | 65.1 | 109.1 | 111.6 |
| split | on | off | events | - | - | - | - | 65.1 | 65.4 | 113.8 | 114.0 |
| split | off | on | outbox | - | - | - | - | 71.2 | 66.9 | 114.6 | 116.3 |
| split | off | on | events | - | - | - | - | 66.1 | 66.2 | 117.2 | 118.0 |
| split | off | off | outbox | - | - | - | - | 69.8 | 72.9 | 134.5 | 134.2 |
| split | off | off | events | - | - | - | - | 71.7 | 72.3 | 136.6 | 139.1 |
| per-module | on | on | outbox | 34.7 | 34.5 | 36.0 | 52.7 | 60.8 | - | 96.1 | 97.5 |
| per-module | on | on | events | 36.0 | 36.1 | 36.3 | 51.0 | 63.1 | - | 102.4 | 101.7 |
| per-module | on | off | outbox | 34.0 | 33.8 | 38.0 | 53.0 | 67.6 | - | 109.1 | 111.6 |
| per-module | on | off | events | 36.4 | 36.6 | 36.9 | 52.1 | 64.3 | - | 113.8 | 114.0 |
| per-module | off | on | outbox | 37.2 | 37.4 | 38.3 | 56.5 | 64.1 | - | 114.6 | 116.3 |
| per-module | off | on | events | 38.5 | 38.7 | 43.8 | 65.3 | 67.0 | - | 117.2 | 118.0 |
| per-module | off | off | outbox | 39.1 | 39.5 | 42.3 | 63.1 | 93.2 | - | 134.5 | 134.2 |
| per-module | off | off | events | 41.9 | 41.3 | 43.0 | 78.2 | 74.7 | - | 136.6 | 139.1 |

</div>
