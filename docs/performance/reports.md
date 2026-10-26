# Raw reports

Every `scripts/perf.sh` run writes a timestamped markdown report with the modes side by side, followed by the detail of each run (round trip, every
module-to-module call and its binding, cache hit rates). These are the reports behind the pages in this section, unedited apart from local file paths.

| Report | Command | Used for |
| --- | --- | --- |
| [Modes, cache off and on](reports/modes-cache.md) | `--modes all --cache both --orders 1200 --warmup 200 --concurrency 4` | [the eight modes](modes.md), [the cache](switches.md#result-cache) |
| [Parallel quotes, remote-capable modes](reports/parallel.md) | `--modes loopback-http,split-dbs,microservices-macro --parallel both ...` | [parallel quotes](switches.md#parallel-quotes) |
| [Outbox and events, with and without parallel](reports/async-parallel.md) | `--modes modulith,groups,microservices --async both --parallel both ...` | [follow-up delivery](switches.md#follow-up-delivery-outbox-or-events), [parallel quotes](switches.md#parallel-quotes) |
| [4 clients, no tracing](reports/load-4-clients.md) | `--modes modulith,groups,microservices --no-trace --orders 3000 --warmup 300 --concurrency 4` | [load](load.md) |
| [16 clients, no tracing](reports/load-16-clients.md) | same with `--concurrency 16` | [load](load.md) |
| [JVM 2 GB, C1 and serial GC](reports/jvm-c1-2g.md) | `--modes all --cache both --orders 1500 --warmup 200 --concurrency 16`, `DEMO_JAVA_OPTS="-Xmx2g -XX:+UseSerialGC -XX:TieredStopAtLevel=1"` | [JVM settings](jvm-settings.md) |
| [JVM 2 GB, C2 and parallel GC](reports/jvm-c2-parallel.md) | same with `DEMO_JAVA_OPTS="-Xmx2g -XX:+UseParallelGC"` | [JVM settings](jvm-settings.md) |
| [JVM 2 GB, C2, `split-dbs` rerun](reports/jvm-c2-parallel-rerun-split-dbs.md) | `--modes split-dbs --cache both ...`, same flags | [JVM settings](jvm-settings.md) |

The delivery-speed test (978 orders placed flat out, then the time until every notification arrived) is a separate script run with
`MODULES_ORDERS_ASYNC=outbox` and `=events`; its two result lines are on [the load page](load.md#delivery-of-follow-ups).

The tables and charts in this section are generated from the saved summaries of these runs by `scripts/perf_site.py`.
