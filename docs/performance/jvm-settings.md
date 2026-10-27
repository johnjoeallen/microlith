# JVM settings: bigger heaps, C2 and parallel GC

The demo's default JVM is now `-Xmx2g -XX:+UseParallelGC` with the full JIT; this page shows how that default was chosen. It repeats the
[mode comparison](modes.md) on the 128 GB, 32-CPU test machine with 16 concurrent clients, with the earlier small JVMs (`-Xmx320m`, C1-only JIT, serial GC) and with larger ones, to see how much of the result is the JVM settings rather than the architecture.
These runs were made before the `VACUUM ANALYZE` step was added to the measurements, so their absolute numbers are a few percent different from the [headline numbers](index.md#headline-numbers).

**Short answer.** Giving each JVM more memory changes nothing measurable (1 GB and 2 GB agree, over five repetitions each); letting the JIT use C2 does most of the work, and the parallel collector adds a little throughput and a clearly better 99th percentile. Together they make the **in-process shapes 37 to 61% faster**. They leave the shapes that cross a process boundary almost where they were. The ordering of the shapes is unchanged, but the gap between the monolith and eight services widens from about 2.0x to 2.8x.

## What was run

Both runs: `scripts/perf.sh --modes all --cache both --orders 1500 --warmup 200 --concurrency 16`, one run per configuration, differing only in `DEMO_JAVA_OPTS`.

| Run | JVM flags per process | Report |
| --- | --- | --- |
| **C1** | `-Xmx2g -XX:+UseSerialGC -XX:TieredStopAtLevel=1` (the former demo defaults, with 2 GB) | [report](reports/jvm-c1-2g.md) |
| **C2** | `-Xmx2g -XX:+UseParallelGC` (full tiered JIT) | [report](reports/jvm-c2-parallel.md), [`split-dbs` rerun](reports/jvm-c2-parallel-rerun-split-dbs.md) |

## Results

Orders per second, median and 99th percentile of the client round trip, per mode. "Change" is C2 against C1 in throughput.

--8<-- "performance/_generated/jvm-settings.md"

† `split-dbs` was rerun on its own after the main C2 run, with the same load and flags ([report](reports/jvm-c2-parallel-rerun-split-dbs.md)), because its first run was an outlier; the table shows the rerun.

## Heap size and collector, repeated

The comparison above changes the JIT and the collector together. To separate heap size and collector, `scripts/jvm_matrix.sh` ran every combination of **1 GB or 2 GB** and **serial or parallel GC**, all with the full C2 JIT,
**five times each**, interleaved (repetition 1 of all four, then repetition 2, and so on) so that drift on the machine hits every configuration alike. Each run is `--modes all --cache on --orders 1500 --warmup 200 --concurrency 16`,
20 runs in all. The tables are means over the repetitions, after dropping outliers: a run is dropped for a mode if its orders/s is more than two scaled MADs *and* more than 5% from the median of that mode's repetitions.

Mean orders per second:

--8<-- "performance/_generated/jvm-matrix-throughput.md"

Mean median (p50) round trip, ms:

--8<-- "performance/_generated/jvm-matrix-p50.md"

Mean 99th percentile round trip, ms:

--8<-- "performance/_generated/jvm-matrix-p99.md"

--8<-- "performance/_generated/jvm-matrix-dropped.md"

* **Heap size makes no measurable difference.** 1 GB and 2 GB agree within the noise for every mode and collector, so 1 GB is enough for these JVMs at this load.
* **The collector barely moves throughput.** Parallel is ahead of serial by 0 to 8%, most visibly on the in-process modes (`modulith` 437 against 470 at 1 GB). Medians are identical.
* **The collector shows in the tail.** The 99th percentile is lower with the parallel collector in almost every row, for example 52 against 144 ms for `modulith` at 1 GB and 167 against 198 ms for `microservices` at 2 GB. The serial collector's
  stop-the-world pauses are a likely cause, but pauses were not measured, and the means include a few slow runs, so treat the size of the gap as indicative.
* **So the earlier jump was the JIT.** Most of the 37 to 61% gain in the first comparison therefore came from C2, not from the collector or the heap.
* The raw reports of the 20 runs are not published (about 800 lines each); the stamps are in `.demo/jvm-matrix.txt` of the run.

## What to take from it

* **In-process shapes gain the most.** `modulith`, `macro-modulith` and `loopback-codec` rise from about 305 to 440 orders/s (cache off) and from about 330 to 465 (cache on). Their p99 also
  tightens, from about 160 ms to about 55 ms in most cases, which suggests the C1 runs' tails were compilation and collection pauses.
* **Process boundaries dominate once the JVM is fast.** `microservices` and `microservices-macro` stay at about 140 (cache off) and 165 to 175 orders/s (cache on) under both settings: each order still pays about 20 calls
  over HTTP, and the code on either side of them was not the bottleneck.
* **The cost of splitting looks larger, not smaller.** With C1, `microservices` ran at 50% of `modulith`; with C2 it runs at 36%. A faster JVM speeds up the work between the calls, so the calls become a bigger share.

## Limits

* One run per configuration, so differences below about 5 to 8% are noise.
* The first comparison changes the JIT and the collector together; the [repeated matrix](#heap-size-and-collector-repeated) varies only heap and collector, and C2 is inferred as the remainder.
* Still one machine, loopback only, and a shared PostgreSQL server; see [the method page](method.md).
* The other pages use the 2 GB parallel-GC default and the `VACUUM ANALYZE` step, so their absolute numbers differ slightly from the C2 figures here.
