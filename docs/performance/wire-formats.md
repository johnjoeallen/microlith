# Wire formats: gzip and BSON over HTTP

Does compressing the JSON-RPC envelope, or sending it as BSON, make calls between processes cheaper? Tested on this project's HTTP calls only. Local calls and the
in-memory `loopback-codec` binding never use these formats, because there is no network to save and nothing to compress for.

**Short answer.** Keep plain JSON as the default. **gzip** shrinks big envelopes a lot (to 17 to 31% of their size between groups at 40 to 150 order lines) but only shows a latency
benefit on constrained links, never on loopback, and it does little for the small envelopes of normal orders. **BSON** is *larger* than JSON for these messages in every case measured.

!!! warning "What these measurements can and cannot show"
    Everything ran on one machine over **loopback**, with no TLS and no bandwidth limit, so a smaller message cannot arrive any sooner. The end-to-end runs therefore show the
    *cost* of the formats (and bytes saved), never a latency win. Where compression could win is worked out separately, from a CPU benchmark and the bytes saved
    ([break-even](#where-compression-pays)). One run per configuration, so differences below about 5 to 8% are noise.

## What was built

* `rpc.client.wire` (`demo.sh --wire`, `perf.sh --wire`) chooses how the HTTP transport encodes the envelope: `json` (default), `gzip`, `bson` or `bson-gzip`.
* The `/rpc/{module}` endpoint accepts all four and answers in the format the caller asks for (`Accept`, `Accept-Encoding`); plain JSON callers are unchanged.
* The BSON conversion is checked against the MongoDB library itself (the tests build and read BSON with it), so it is real BSON.
* Each process counts the envelope bytes it sends and receives, before and after encoding, so the runs report **bytes on the wire per order**, not estimates.

!!! note "A default that would have corrupted the baseline"
    Spring 7's JDK HTTP client has compression **on by default**: it adds `Accept-Encoding: gzip, deflate` to every request and silently decompresses the reply. The platform's transport
    had been sending that header all along, harmlessly, since no server compressed. Once a server honours it, the "plain JSON" baseline would have been compressed too, and the real compressed sizes
    would have been invisible. The transport now turns that off and chooses the encoding itself.

## Bytes on the wire

Per order, from the counters of every process. "JSON bytes/call" is the average call (its request and response envelopes together); the last three columns are the share of the JSON bytes that crossed the wire.

--8<-- "performance/_generated/wire-bytes.md"

* **At realistic order sizes (1 to 4 lines, envelopes of about 150 to 200 bytes) gzip saves 11 to 15%, BSON adds 7%, BSON+gzip saves 2 to 6%.** Too little data per call for gzip to find much to remove.
* **Big envelopes compress very well.** Between `groups` the calls that carry the order lines average 1.3 KB at 40 lines and 4 KB at 150 lines, and gzip takes them to 31% and 17%.
* **`microservices` and `loopback-http` compress less (63% at 40 lines, 57% at 150 lines for `microservices`).** There most of the calls are tiny per-line lookups (a product, a stock level) of about 400 bytes whatever the order size, and only a few carry the lines.
* **BSON is larger than JSON at every size (107 to 119%).** Field names are kept, every value gets a type tag and every string a length, and arrays carry index keys; JSON's digits and brackets are already compact.

## Throughput and latency

Median order (p50) and throughput against plain JSON, four clients. These wire-format runs were made earlier, with the former small demo JVMs and without the `VACUUM ANALYZE` step, and were not repeated at 16 clients; compare the formats with each other, not with the other pages. Loopback, so no bandwidth to save:

--8<-- "performance/_generated/wire-throughput.md"

There is no consistent effect: most cells are within noise (a few percent). The one pattern that looks like a cost, `microservices` at 150 lines (gzip +8%, BSON +10%, BSON+gzip +14% on p50), **cannot be the formats**:
at about 168 HTTP calls per order and a few microseconds of codec per call (next section), the total CPU cost is 1 to 2 ms of a 221 ms order. It is the run-to-run variation of the eight-process mode, and the run should be repeated before concluding anything from it.

## What each format costs in CPU

Measured with no network, database or web server in the way: encode an object tree to bytes on the sender and decode it on the receiver, for envelopes shaped like the demo's order calls (a request carrying *n* order lines).
Median of warmed batches, microseconds per envelope.

--8<-- "performance/_generated/wire-cost.md"

* **gzip adds 4 to 14 µs** per envelope (more for bigger ones). BSON as a codec costs about the same as JSON text (0.2 to 5.7 µs more) but produces larger output, so there is nothing to recover.
* The platform's transport currently passes JSON text between the proxy and the wire, so a BSON call there also pays a text parse and print that a native BSON path would skip. That extra cost is **not** in this table.
* gzip is the plain `GZIPOutputStream` per call (a new compressor each time, default level, no dictionary). Reusing compressors, or a dictionary trained on the envelopes, could make small messages compress better and cost less. Not tried.

## Where compression pays

Compression helps when sending the bytes it saves would take longer than compressing and decompressing them. The break-even column is `bytes saved ÷ extra CPU`, in MB/s: **below that link speed gzip wins, above it
JSON is faster**. Read with the bytes columns above:

| Envelope | Break-even link speed | What it means |
| --- | --- | --- |
| about 130 bytes (1 line) | 0.5 MB/s (4 Mbit/s) | only a very slow link; gzip saves 2% |
| about 235 bytes (4 lines) | 18 MB/s (145 Mbit/s) | slow or shared links only |
| about 440 bytes (10 lines) | 53 MB/s (425 Mbit/s) | helps below roughly 400 Mbit/s, not on gigabit |
| about 1.5 KB (40 lines) | 169 MB/s (1.35 Gbit/s) | helps on 1 GbE, loses on 10 GbE |
| about 5 KB (150 lines) | 324 MB/s (2.6 Gbit/s) | helps on 1 GbE, loses on 10 GbE |

* **Small envelopes never justify compression for latency** on a datacentre network. **Large ones do on gigabit and slower links**, and not on 10 GbE.
* This is the *latency* case, for calls made one after the other. Bytes also have a cost of their own (cross-zone or egress charges, shared links, TLS work per byte), which is not measured here and favours compression.
* BSON never saves bytes, so it has no break-even.

## Conclusions

1. **Default stays `json`.** The normal order calls are about 150 to 200 bytes, where gzip saves about 12% and BSON costs 7% more.
2. **If you enable compression, do it by size.** Compress envelopes above roughly 1 KB, where it pays on gigabit links, and leave the small ones alone. A minimum-size threshold is not implemented yet (it is the obvious next step; the setting would be per client).
3. **Do not use BSON for JSON-RPC envelopes of this shape.** It is larger, slightly slower, and its advantages (binary values, exact typed numbers, partial reads) are not used here.
4. **Measure on the real network.** The case for compression depends on bandwidth, TLS, cost per byte and payload sizes that this one-machine demo does not have. The tooling to do it is `perf.sh --wire all`, with `--lines N` for larger orders.

## Method

* `perf.sh --modes loopback-http,groups,microservices --wire all --async events ...`, four clients, result cache and parallel quotes on. Events delivery so each run's follow-ups drain at once; the wire format is independent of that choice.
* Order sizes: the realistic mix (1 to 4 lines, 1,200 orders), `--lines 40` (500 orders) and `--lines 150` (250 orders). More lines mean more calls too (about 134 and 464 module calls per order).
* The CPU table: `WireCostBenchmark` (a test that only runs with `-Dwire.bench=true`).
* Raw reports: [realistic orders](reports/wire-small.md), [40 lines](reports/wire-lines40.md), [150 lines](reports/wire-lines150.md).
* Limits: loopback, no TLS, one run per configuration, JSON text between the proxy and the transport, a new compressor per call.
