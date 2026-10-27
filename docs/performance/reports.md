# Raw reports

Every `scripts/perf.sh` run writes a timestamped markdown report with the modes side by side, followed by the detail of each run (round trip, every
module-to-module call and its binding, cache hit rates). These are the reports behind the pages in this section, unedited apart from local file paths.

| Report | Command | Used for |
| --- | --- | --- |
| [Modes, cache off and on](reports/modes-cache.md) | `--modes all --cache both --orders 1500 --warmup 200 --concurrency 16` | [the eight modes](modes.md), [the cache](switches.md#result-cache) |
| [Parallel quotes, remote-capable modes](reports/parallel.md) | `--modes loopback-http,split-dbs,microservices-macro --parallel both --orders 1500 --warmup 200 --concurrency 16` | [parallel quotes](switches.md#parallel-quotes) |
| [Outbox and events, with and without parallel](reports/async-parallel.md) | `--modes modulith,groups,microservices --async both --parallel both --orders 1500 --warmup 200 --concurrency 16` | [follow-up delivery](load.md#on-the-request-path), [parallel quotes](switches.md#parallel-quotes) |
| [JVM 2 GB, C1 and serial GC](reports/jvm-c1-2g.md) | `--modes all --cache both --orders 1500 --warmup 200 --concurrency 16`, `DEMO_JAVA_OPTS="-Xmx2g -XX:+UseSerialGC -XX:TieredStopAtLevel=1"` | [JVM settings](jvm-settings.md) |
| [JVM 2 GB, C2 and parallel GC](reports/jvm-c2-parallel.md) | same with `DEMO_JAVA_OPTS="-Xmx2g -XX:+UseParallelGC"` | [JVM settings](jvm-settings.md) |
| [JVM 2 GB, C2, `split-dbs` rerun](reports/jvm-c2-parallel-rerun-split-dbs.md) | `--modes split-dbs --cache both ...`, same flags | [JVM settings](jvm-settings.md) |

The headline table and charts are the mean of five runs of `--modes all --cache on --orders 1500 --warmup 200 --concurrency 16`: the cache-on rows of the first report above and four further runs, generated with `scripts/jvm_report.py --reps`. Those four reports are not published (about 800 lines each).

The three JVM reports above were made with the former demo flags and before the `VACUUM ANALYZE` step; see [JVM settings](jvm-settings.md).

The delivery-speed test (978 orders placed flat out, then the time until every notification arrived) was run with `MODULES_ORDERS_ASYNC=outbox` and `=events`
by an ad-hoc script that is not in the repository, so `scripts/perf.sh` cannot repeat it; its two result lines are on [the delivery page](load.md). It predates the 2 GB parallel-GC default
and the `VACUUM ANALYZE` step.

The tables and charts in this section are generated from the saved summaries of these runs by `scripts/perf_site.py` and `scripts/jvm_report.py`.
