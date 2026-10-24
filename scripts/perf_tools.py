#!/usr/bin/env python3
"""Performance driver behind scripts/perf.sh (Python 3 standard library only).

Drives orders.placeOrder against the running demo mode with N concurrent clients and reports
  1. the client round trip of every placeOrder (throughput, error rate, min/mean/p50/p90/p95/p99/max), and
  2. the latency of every module-to-module call inside those orders, from the /demo/trace records of every process,
     grouped by caller -> callee.method and binding.

  perf_tools.py [--orders N | --duration S] [--concurrency C] [--warmup W] [--settle S] [--drain S] [--modes M,M|all] [--no-trace] [--no-stock] [--out DIR]
"""
import argparse
import collections
import concurrent.futures
import csv
import http.client
import itertools
import json
import math
import os
import queue
import subprocess
import sys
import threading
import time
import urllib.parse
import uuid
from datetime import datetime, timezone
from pathlib import Path

import demo_tools

DEMO = demo_tools.DEMO
ROOT = DEMO.parent
CUSTOMERS = ["cust-standard", "cust-gold"]          # cust-declined and the sku-oos / sku-undeliverable products are failure fixtures
SKUS = ["sku-widget", "sku-gadget"]
ALL_MODES = ["modulith", "macro-monolith", "groups", "microservices", "microservices-macro", "loopback-codec", "loopback-http", "split-dbs"]


def percentile(sorted_values, p):
    """Nearest-rank percentile of an ascending list."""
    if not sorted_values:
        return float("nan")
    return sorted_values[max(0, math.ceil(p / 100 * len(sorted_values)) - 1)]


def summarize(values):
    values = sorted(values)
    return {"count": len(values), "min": values[0], "mean": sum(values) / len(values), "p50": percentile(values, 50),
            "p90": percentile(values, 90), "p95": percentile(values, 95), "p99": percentile(values, 99), "max": values[-1]}


def top_up_stock(mode, needed):
    """The demo seeds 100 widgets and 50 gadgets; make sure a long run does not end in OutOfStock."""
    instance = "pg-b" if mode == "split-dbs" else "pg-a"   # inventory lives on B in split-dbs, otherwise on A
    sql = ("update inventory.stock set available = greatest(available, %d) where sku in ('sku-widget', 'sku-gadget')" % needed)
    compose = ["docker", "compose", "-f", str(ROOT / "docker-compose.demo.yml")]
    result = subprocess.run(compose + ["exec", "-T", instance, "psql", "-U", "postgres", "-d", "macro", "-qAtc", sql],
                            capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit("could not top up stock (use --no-stock to skip): " + result.stderr.strip())


class Client:
    """One persistent HTTP connection per worker thread, so connection setup is not part of the measured round trip."""

    def __init__(self, base_url):
        parsed = urllib.parse.urlparse(base_url)
        self.host, self.port = parsed.hostname, parsed.port
        self.local = threading.local()

    def post(self, path, body, cid):
        for attempt in (1, 2):
            connection = getattr(self.local, "connection", None)
            if connection is None:
                connection = self.local.connection = http.client.HTTPConnection(self.host, self.port, timeout=60)
            try:
                connection.request("POST", path, body, {"Content-Type": "application/json", "X-Correlation-Id": cid})
                response = connection.getresponse()
                return response.status, response.read().decode()
            except (http.client.HTTPException, OSError):
                connection.close()
                self.local.connection = None
                if attempt == 2:
                    raise


def place_order(client, run_id, seq):
    cid = f"perf-{run_id}-{seq}"
    lines = [{"sku": SKUS[seq % len(SKUS)], "quantity": 1 + seq % 2}]
    body = json.dumps({"jsonrpc": "2.0", "method": "orders.placeOrder", "id": seq,
                       "params": {"requestId": cid, "customerId": CUSTOMERS[seq % len(CUSTOMERS)], "lines": lines}})
    started = time.perf_counter()
    try:
        status, text = client.post("/rpc/orders", body, cid)
        outcome = "ok" if status == 200 and '"error"' not in text else f"error:HTTP {status} {text[:80]}"
        if status == 200 and '"error"' in text:
            outcome = "error:" + str(json.loads(text)["error"].get("code"))
    except Exception as error:                       # connection-level failure
        outcome = f"error:{type(error).__name__}"
    return cid, (time.perf_counter() - started) * 1000, outcome


def run_load(client, args, run_id, on_done=None):
    """Closed loop: each of C workers sends its next order as soon as the previous one returned."""
    counter = itertools.count()
    results, lock = [], threading.Lock()
    start = time.perf_counter()
    deadline = start + args.duration if args.duration else None

    def worker():
        while True:
            seq = next(counter)
            if deadline is not None:
                if time.perf_counter() >= deadline:
                    return
            elif seq >= args.orders:
                return
            cid, rtt, outcome = place_order(client, run_id, args.warmup_orders + seq)
            with lock:
                results.append({"cid": cid, "startS": time.perf_counter() - start - rtt / 1000, "rttMs": rtt, "outcome": outcome})
            if on_done:
                on_done(cid)

    threads = [threading.Thread(target=worker, daemon=True) for _ in range(args.concurrency)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    return results, time.perf_counter() - start


class TraceCollector:
    def __init__(self, settle, workers=4):
        self.settle = settle
        self.pending = queue.Queue()
        self.pool = concurrent.futures.ThreadPoolExecutor(max_workers=workers)
        self.futures = []
        self.stop = False
        self.thread = threading.Thread(target=self.schedule, daemon=True)
        self.thread.start()

    def add(self, cid):
        self.pending.put((time.monotonic() + self.settle, cid))

    def schedule(self):
        while True:
            try:
                ready, cid = self.pending.get(timeout=0.2)
            except queue.Empty:
                if self.stop:
                    return
                continue
            delay = ready - time.monotonic()
            if delay > 0:
                time.sleep(delay)
            self.futures.append(self.pool.submit(lambda c=cid: (c, demo_tools.collect(c))))

    def finish(self, drain):
        """Waits for the in-flight reads, then re-reads every order until no new calls appear (the outbox relay delivers
        notifications at its own pace) or `drain` seconds passed. Records seen earlier are kept if a buffer has since wrapped."""
        self.stop = True
        self.thread.join()
        collected = {}

        def merge(cid, records):
            known = collected.setdefault(cid, {})
            for r in records:
                known[(r["startedAtMicros"], r["caller"], r["callee"], r["method"])] = r

        for future in self.futures:
            merge(*future.result())
        deadline = time.monotonic() + drain
        total = sum(len(v) for v in collected.values())
        while time.monotonic() < deadline:
            time.sleep(2)
            for cid, records in self.pool.map(lambda c: (c, demo_tools.collect(c)), list(collected)):
                merge(cid, records)
            now = sum(len(v) for v in collected.values())
            if now == total:
                break
            total = now
        self.pool.shutdown()
        return {cid: sorted(v.values(), key=lambda r: (r["startedAtMicros"], r["depth"])) for cid, v in collected.items()}


def print_table(title, rows, header):
    print(f"\n{title}")
    widths = [max(len(str(r[i])) for r in [header] + rows) for i in range(len(header))]
    for index, row in enumerate([header] + rows):
        print("  " + "  ".join(str(cell).ljust(widths[i]) if i < 2 else str(cell).rjust(widths[i]) for i, cell in enumerate(row)))
        if index == 0:
            print("  " + "  ".join("-" * w for w in widths))


def fmt(value):
    return f"{value:.1f}"


def main():
    parser = argparse.ArgumentParser(description="Performance driver for the running demo mode")
    parser.add_argument("--orders", type=int, default=200, help="orders to place (default 200)")
    parser.add_argument("--duration", type=float, help="run for this many seconds instead of a fixed number of orders")
    parser.add_argument("--concurrency", type=int, default=4, help="concurrent clients (default 4)")
    parser.add_argument("--warmup", type=int, default=20, dest="warmup_orders", help="orders sent first and not measured (default 20)")
    parser.add_argument("--settle", type=float, default=3.0, help="seconds to wait after an order before reading its trace (default 3)")
    parser.add_argument("--drain", type=float, default=30.0, help="max seconds to keep re-reading traces for late async calls (default 30)")
    parser.add_argument("--no-trace", action="store_true", help="measure client round trips only (no per-call statistics, least overhead)")
    parser.add_argument("--no-stock", action="store_true", help="do not raise the demo stock before the run")
    parser.add_argument("--out", help="directory for the raw samples and summary (default .demo/perf/<timestamp>)")
    parser.add_argument("--modes", help="comma-separated modes to start, measure and stop in turn (each on a clean database), or 'all'; "
                                        "without it the mode that is already running is measured")
    args = parser.parse_args()
    if args.modes:
        return run_modes(args)
    summary = measure(args, None)
    return 0 if summary["clean"] else 1


def measure(args, out_dir):
    """One measurement of the running mode; returns its summary (also written to summary.json)."""
    if not (DEMO / "mode").exists():
        sys.exit("no demo is running (scripts/demo.sh <mode> start)")
    mode = (DEMO / "mode").read_text().strip()
    orders_url = (DEMO / "orders.url").read_text().strip()
    client = Client(orders_url)
    run_id = uuid.uuid4().hex[:8]
    planned = args.orders if not args.duration else int(args.duration * 200)   # generous stock bound for a timed run
    if not args.no_stock:
        top_up_stock(mode, 2 * (planned + args.warmup_orders) + 100)

    print(f"mode {mode}, {orders_url}, concurrency {args.concurrency}, "
          + (f"{args.duration:g} s" if args.duration else f"{args.orders} orders") + f", warmup {args.warmup_orders}")
    if args.warmup_orders:
        warm = argparse.Namespace(orders=args.warmup_orders, duration=None, concurrency=args.concurrency, warmup_orders=0)
        run_load(client, warm, run_id)
        time.sleep(args.settle)                      # let the warmup's async work finish so it does not bleed into the run

    collector = None if args.no_trace else TraceCollector(args.settle)
    results, elapsed = run_load(client, args, run_id, collector.add if collector else None)
    if collector:
        print(f"collecting call traces of {len(results)} orders from {len(demo_tools.processes())} process(es) ...")
        traces = collector.finish(args.drain)
    else:
        traces = {}

    ok = [r for r in results if r["outcome"] == "ok"]
    failed = collections.Counter(r["outcome"] for r in results if r["outcome"] != "ok")
    print(f"\nplaceOrder round trip (client side): {len(results)} orders in {elapsed:.1f} s = {len(results) / elapsed:.1f} orders/s, "
          f"{len(ok)} ok, {len(results) - len(ok)} failed")
    if failed:
        for outcome, count in failed.most_common(5):
            print(f"  {count} x {outcome}")
    summary = {"mode": mode, "concurrency": args.concurrency, "orders": len(results), "ok": len(ok), "elapsedS": elapsed,
               "ordersPerSecond": len(results) / elapsed, "clean": len(ok) == len(results)}
    if not ok:
        return summary
    stats = summarize([r["rttMs"] for r in ok])
    print_table("Round trip, ms", [[ "placeOrder", stats["count"], *[fmt(stats[k]) for k in ("min", "mean", "p50", "p90", "p95", "p99", "max")]]],
                ["call", "n", "min", "mean", "p50", "p90", "p95", "p99", "max"])

    edge_rows, edge_summary = [], {}
    if traces:
        groups = collections.defaultdict(list)
        errors = collections.Counter()
        empty = 0
        for cid, records in traces.items():
            if not records:
                empty += 1
            for record in records:
                key = (f"{record['caller']} -> {record['callee']}.{record['method']}", record["binding"])
                groups[key].append(record["durationMs"])
                if record["outcome"] != "ok":
                    errors[key] += 1
        for key, values in sorted(groups.items(), key=lambda item: -sum(item[1])):
            s = summarize(values)
            edge_summary[f"{key[0]} [{key[1]}]"] = {**s, "errors": errors[key]}
            edge_rows.append([key[0], key[1], s["count"], errors[key] or "", *[fmt(s[k]) for k in ("mean", "p50", "p95", "p99", "max")]])
        print_table("Module-to-module calls, ms (recorded on the caller's side; ordered by total time spent)", edge_rows,
                    ["call", "binding", "n", "errors", "mean", "p50", "p95", "p99", "max"])
        per_binding = collections.defaultdict(list)
        for (_, binding), values in groups.items():
            per_binding[binding].extend(values)
        summary["byBinding"] = {b: {"calls": len(v), "meanMs": sum(v) / len(v)} for b, v in per_binding.items()}
        print("\nBy binding: " + ", ".join(f"{b} {len(v)} calls mean {sum(v) / len(v):.1f} ms" for b, v in sorted(per_binding.items())))
        calls_per_order = sum(len(v) for v in groups.values()) / max(1, len(traces) - empty)
        summary["callsPerOrder"] = calls_per_order
        print(f"{calls_per_order:.1f} module-to-module calls per order")
        if empty:
            summary["ordersWithoutTrace"] = empty
            print(f"WARNING: {empty} of {len(traces)} orders had no trace records (buffer overwritten by a too-fast run, "
                  f"or --settle too short); raise demo.trace.capacity or lower --concurrency for complete call statistics")

    out = out_dir or (Path(args.out) if args.out else DEMO / "perf" / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ"))
    out.mkdir(parents=True, exist_ok=True)
    with open(out / "orders.csv", "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["correlationId", "startS", "rttMs", "outcome"])
        writer.writerows([[r["cid"], f"{r['startS']:.4f}", f"{r['rttMs']:.3f}", r["outcome"]] for r in results])
    if traces:
        with open(out / "calls.csv", "w", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["correlationId", "process", "caller", "callee", "method", "binding", "depth", "durationMs", "outcome"])
            for cid, records in traces.items():
                writer.writerows([[cid, r["process"], r["caller"], r["callee"], r["method"], r["binding"], r["depth"],
                                   r["durationMs"], r["outcome"]] for r in records])
    summary.update({"roundTripMs": stats, "calls": edge_summary})
    (out / "summary.json").write_text(json.dumps(summary, indent=2))
    print(f"\nraw samples and summary: {out}")
    return summary


def run_modes(args):
    """Starts each mode on a clean database, measures it, stops it, then prints one comparison table."""
    modes = ALL_MODES if args.modes == "all" else [m.strip() for m in args.modes.split(",") if m.strip()]
    unknown = [m for m in modes if m not in ALL_MODES]
    if unknown or not modes:
        sys.exit(f"unknown mode(s) {unknown or args.modes!r}; modes: {', '.join(ALL_MODES)}, or 'all'")
    if (DEMO / "mode").exists():
        sys.exit(f"a demo is already running (mode {(DEMO / 'mode').read_text().strip()}); stop it first: scripts/demo.sh <mode> stop")
    base = Path(args.out) if args.out else DEMO / "perf" / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    demo_sh = str(ROOT / "scripts" / "demo.sh")
    summaries = []
    for mode in modes:
        print(f"\n================ {mode} ================", flush=True)
        started = subprocess.run([demo_sh, mode, "start"], capture_output=True, text=True)
        if started.returncode != 0:
            print(f"{mode} did not start:\n" + (started.stdout + started.stderr)[-1500:])
            subprocess.run([demo_sh, mode, "stop"], capture_output=True)
            summaries.append({"mode": mode, "clean": False, "error": "did not start"})
            continue
        try:
            summaries.append(measure(args, base / mode))
        except BaseException:
            summaries.append({"mode": mode, "clean": False, "error": "measurement failed"})
            raise
        finally:
            subprocess.run([demo_sh, mode, "stop"], capture_output=True)

    rows = []
    for m in summaries:
        rt, bb = m.get("roundTripMs"), m.get("byBinding", {})
        rows.append([m["mode"], f"{m['ordersPerSecond']:.0f}" if "ordersPerSecond" in m else "-",
                     fmt(rt["p50"]) if rt else "-", fmt(rt["p95"]) if rt else "-", fmt(rt["p99"]) if rt else "-",
                     f"{m['callsPerOrder']:.1f}" if "callsPerOrder" in m else "-",
                     "  ".join(f"{b} {v['meanMs']:.1f}" for b, v in sorted(bb.items())) or "-",
                     "PASS" if m["clean"] else "FAIL" + (f" ({m['error']})" if "error" in m else "")])
    print("\n================ comparison ================")
    print_table(f"placeOrder round trip in ms, {args.concurrency} clients; mean call latency per binding in ms", rows,
                ["mode", "orders/s", "p50", "p95", "p99", "calls/order", "mean call ms by binding", "result"])
    base.mkdir(parents=True, exist_ok=True)
    (base / "comparison.json").write_text(json.dumps(summaries, indent=2))
    print(f"\nper-mode samples and comparison.json: {base}")
    return 0 if all(m["clean"] for m in summaries) else 1


if __name__ == "__main__":
    sys.exit(main())
