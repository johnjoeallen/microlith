#!/usr/bin/env python3
"""Performance driver behind scripts/perf.sh (Python 3 standard library only).

Drives orders.placeOrder against the running demo mode with N concurrent clients and reports
  1. the client round trip of every placeOrder (throughput, error rate, min/mean/p50/p90/p95/p99/max), and
  2. the latency of every module-to-module call inside those orders, from the /demo/trace records of every process,
     grouped by caller -> callee.method and binding.

  perf_tools.py [--orders N | --duration S] [--concurrency C] [--warmup W] [--settle S] [--drain S] [--modes M,M|all] [--cache on|off|both] [--parallel on|off|both] [--no-trace] [--no-stock] [--out DIR]
"""
import argparse
import bisect
import collections
import concurrent.futures
import csv
import http.client
import itertools
import json
import math
import os
import queue
import random
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
PROFILES = {                        # customers, SKUs and how skewed their popularity is (Zipf exponent; 0 = uniform)
    "small": dict(customers=2, skus=2, customer_skew=0.0, sku_skew=0.0),
    "realistic": dict(customers=10_000, skus=2_000, customer_skew=0.8, sku_skew=1.0),
    "large": dict(customers=100_000, skus=20_000, customer_skew=0.8, sku_skew=1.0),
}
LINES = ([1, 2, 3, 4], [40, 30, 20, 10])     # lines per order (mean 2.0) and their weights
QUANTITY = ([1, 2, 3], [70, 20, 10])         # units per line (mean 1.4)
GOLD_PERCENT, DECLINED_PERCENT = 8, 2        # share of seeded customers: gold tier, and payment token that is always declined
DECLINED = "declined"                        # a customer whose payment is always declined: an expected business outcome, not a failure
STARTED = datetime.now(timezone.utc)
STAMP = STARTED.strftime("%Y%m%dT%H%M%SZ")
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


def cumulative(n, skew):
    weights = [1.0 / (rank ** skew) for rank in range(1, n + 1)]
    return list(itertools.accumulate(weights)), weights[0] / sum(weights)


class Workload:
    """Who orders what. The small profile is the two customers and two SKUs of the demo data; the others use seeded
    perf-c-<rank> customers and perf-s-<rank> SKUs, drawn with a Zipf popularity (rank 1 is the most popular)."""

    def __init__(self, profile, customers, skus, customer_skew, sku_skew, seed):
        self.profile, self.customers, self.skus = profile, customers, skus
        self.customer_skew, self.sku_skew, self.seed = customer_skew, sku_skew, seed
        self.small = profile == "small"
        self.customer_cum, _ = cumulative(customers, customer_skew)
        self.sku_cum, self.top_sku_share = cumulative(skus, sku_skew)

    def order(self, seq):
        """The customer and lines of order number seq; the same for the same seed whatever the thread timing."""
        if self.small:
            return ["cust-standard", "cust-gold"][seq % 2], [{"sku": ["sku-widget", "sku-gadget"][seq % 2], "quantity": 1 + seq % 2}]
        rng = random.Random(f"{self.seed}:{seq}")
        customer = bisect.bisect_left(self.customer_cum, rng.random() * self.customer_cum[-1]) + 1
        wanted = min(rng.choices(*LINES)[0], self.skus)
        chosen = []
        while len(chosen) < wanted:
            sku = bisect.bisect_left(self.sku_cum, rng.random() * self.sku_cum[-1]) + 1
            if sku not in chosen:
                chosen.append(sku)
        return f"perf-c-{customer}", [{"sku": f"perf-s-{sku}", "quantity": rng.choices(*QUANTITY)[0]} for sku in chosen]

    def describe(self):
        if self.small:
            return "small: 2 customers, 2 SKUs, 1 line per order, uniform"
        return (f"{self.profile}: {self.customers:,} customers (Zipf {self.customer_skew:g}), {self.skus:,} SKUs (Zipf {self.sku_skew:g}), "
                f"1-4 lines per order (mean 2.0), 1-3 units per line, {GOLD_PERCENT}% gold and {DECLINED_PERCENT}% always-declined customers, "
                f"seed {self.seed}")


def psql(instance, sql):
    compose = ["docker", "compose", "-f", str(ROOT / "docker-compose.demo.yml")]
    result = subprocess.run(compose + ["exec", "-T", instance, "psql", "-U", "postgres", "-d", "macro", "-v", "ON_ERROR_STOP=1", "-q"],
                            input=sql, capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"could not load the perf data into {instance} (use --no-seed to skip): " + result.stderr.strip())


def seed_data(mode, workload, planned_orders):
    """Loads the customers, products and stock the workload needs (the demo seeds only 3 customers and 4 SKUs).
    A start of any mode begins on a wiped database, so this runs once per measured mode; it is also safe to repeat."""
    where = ({"customers": "pg-c", "catalog": "pg-b", "inventory": "pg-b"} if mode == "split-dbs"       # layout of the split-dbs profile
             else {"customers": "pg-a", "catalog": "pg-a", "inventory": "pg-a"})
    if workload.small:
        needed = 2 * planned_orders + 100
        psql(where["inventory"], "update inventory.stock set available = greatest(available, %d) where sku in ('sku-widget', 'sku-gadget');" % needed)
        return
    units = planned_orders * sum(w * l for l, w in zip(*LINES)) / sum(LINES[1]) * sum(w * q for q, w in zip(*QUANTITY)) / sum(QUANTITY[1])
    stock = max(100, math.ceil(1.5 * units * workload.top_sku_share) + 50)      # the most popular SKU, with headroom
    customers = (f"with c as (select g, (hashtext(g::text)::bigint & 2147483647) % 100 as h from generate_series(1, {workload.customers}) g) "
                 "insert into customers.customer (customer_id, name, email, tier, line1, city, country, payment_token, payment_brand) "
                 "select 'perf-c-' || g, 'Perf Customer ' || g, 'perf' || g || '@example.com', "
                 f"case when h >= {100 - DECLINED_PERCENT - GOLD_PERCENT} and h < {100 - DECLINED_PERCENT} then 'GOLD' else 'STANDARD' end, "
                 f"'1 Main Street', 'Dublin', 'IE', case when h >= {100 - DECLINED_PERCENT} then 'tok-declined' else 'tok-ok' end, 'VISA' "
                 "from c on conflict do nothing;")
    products = ("insert into catalog.product (sku, name, price_cents) select 'perf-s-' || g, 'Perf Product ' || g, "
                f"500 + (hashtext(g::text)::bigint & 2147483647) % 4500 from generate_series(1, {workload.skus}) g on conflict do nothing;")
    stocks = ("insert into inventory.stock (sku, available, warehouse) select 'perf-s-' || g, "
              f"{stock}, 'wh-1' from generate_series(1, {workload.skus}) g "
              "on conflict (sku) do update set available = greatest(inventory.stock.available, excluded.available);")
    by_instance = collections.defaultdict(list)
    for statement, module in ((customers, "customers"), (products, "catalog"), (stocks, "inventory")):
        by_instance[where[module]].append(statement)
    for instance, statements in by_instance.items():
        psql(instance, "\n".join(statements))
    print(f"loaded {workload.customers:,} customers, {workload.skus:,} SKUs with {stock:,} units each")


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


def place_order(client, run_id, seq, workload):
    cid = f"perf-{run_id}-{seq}"
    customer, lines = workload.order(seq)
    body = json.dumps({"jsonrpc": "2.0", "method": "orders.placeOrder", "id": seq,
                       "params": {"requestId": cid, "customerId": customer, "lines": lines}})
    started = time.perf_counter()
    try:
        status, text = client.post("/rpc/orders", body, cid)
        rtt = (time.perf_counter() - started) * 1000          # the classification below is not part of the round trip
        outcome = classify(status, text)
    except Exception as error:                       # connection-level failure
        rtt = (time.perf_counter() - started) * 1000
        outcome = f"error:{type(error).__name__}"
    return cid, rtt, outcome


def classify(status, text):
    """ok, declined (the saga rejected the order because the customer's payment is always declined), or error:<why>."""
    if status != 200:
        return f"error:HTTP {status} {text[:80]}"
    response = json.loads(text)
    if "error" in response:
        return "error:" + str(response["error"].get("code"))
    result = response.get("result") or {}
    if result.get("status") == "CONFIRMED":
        return "ok"
    if result.get("status") == "REJECTED" and str(result.get("failureReason", "")).startswith("Payment declined"):
        return DECLINED
    return f"error:order {result.get('status')} {result.get('failureReason')}"


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
            cid, rtt, outcome = place_order(client, run_id, args.warmup_orders + seq, args.workload)
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


def print_table(title, rows, header, left=2):
    print(f"\n{title}")
    widths = [max(len(str(r[i])) for r in [header] + rows) for i in range(len(header))]
    for index, row in enumerate([header] + rows):
        print("  " + "  ".join(str(cell).ljust(widths[i]) if i < left else str(cell).rjust(widths[i]) for i, cell in enumerate(row)))
        if index == 0:
            print("  " + "  ".join("-" * w for w in widths))


def fmt(value):
    return f"{value:.1f}"


def md_table(header, rows, left=2):
    lines = ["| " + " | ".join(header) + " |", "|" + "|".join(" --- " if i < left else " ---: " for i in range(len(header))) + "|"]
    lines += ["| " + " | ".join(str(c) for c in row) + " |" for row in rows]
    return "\n".join(lines)


def git_state():
    def git(*a):
        return subprocess.run(["git", "-C", str(ROOT), *a], capture_output=True, text=True).stdout.strip()
    commit = git("rev-parse", "--short", "HEAD") or "unknown"
    return commit + (" (uncommitted changes)" if git("status", "--porcelain") else "")


def write_report(directory, args, summaries):
    """perf-<timestamp>.md next to the raw samples: parameters, comparison, and per-mode round trips and call latencies."""
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"perf-{STAMP}.md"
    L = [f"# Performance run {STAMP}", "",
         f"- Started: {STARTED.isoformat(timespec='seconds')}",
         f"- Commit: {git_state()}",
         f"- Host: {os.cpu_count()} CPUs, {os.uname().sysname} {os.uname().release}, Python {sys.version.split()[0]}",
         "- Load: " + (f"{args.duration:g} s" if args.duration else f"{args.orders} orders") + f", {args.concurrency} concurrent clients "
         f"(closed loop, persistent connections), {args.warmup_orders} warmup orders not measured",
         f"- Workload: {summaries[0].get('workload', args.profile)}",
         f"- Call tracing: {'off (round trips only)' if args.no_trace else f'on, settle {args.settle:g} s, drain {args.drain:g} s'}",
         f"- Command line: `{' '.join(sys.argv[1:]) or '(defaults)'}`", ""]
    for title, note, header, rows, left in comparison_tables(summaries):
        L += [f"## {title}", "", note, "", md_table(header, rows, left), ""]
    L += ["## Detail per run", "", "The same figures one run at a time, with every module-to-module call and each call's binding.", ""]
    for m in summaries:
        title = m["mode"] + (f" (cache {m['cache']}, parallel quotes {m['parallel']})" if "cache" in m else "")
        L += [f"### {title}", ""]
        if "roundTripMs" not in m:
            L += [f"No measurement: {m.get('error', 'every order failed')}.", ""]
            continue
        rt = m["roundTripMs"]
        L += [f"{m['orders']} orders in {m['elapsedS']:.1f} s = {m['ordersPerSecond']:.1f} orders/s; {m['ok']} ok, {m.get('declined', 0)} declined "
              f"(expected), {m['orders'] - m['ok'] - m.get('declined', 0)} failed.", "",
              "#### placeOrder round trip (client side), ms", "",
              md_table(["call", "result", "n", "min", "mean", "p50", "p90", "p95", "p99", "max"],
                       [["placeOrder", "ok", rt["count"], *[fmt(rt[k]) for k in ("min", "mean", "p50", "p90", "p95", "p99", "max")]]]
                       + ([["placeOrder", "declined", m["declinedRoundTripMs"]["count"],
                            *[fmt(m["declinedRoundTripMs"][k]) for k in ("min", "mean", "p50", "p90", "p95", "p99", "max")]]]
                          if "declinedRoundTripMs" in m else [])), ""]
        if m.get("cacheHitRate"):
            L += ["#### Cache hit rate (`@Cached` methods)", "",
                  md_table(["method", "calls", "hits", "hit rate"],
                           [[k, v["calls"], v["hits"], f"{v['rate']:.1%}"] for k, v in sorted(m["cacheHitRate"].items())]), ""]
        if m.get("calls"):
            ordered = sorted(m["calls"].items(), key=lambda kv: -kv[1]["count"] * kv[1]["mean"])
            L += ["#### Module-to-module calls, ms", "",
                  "Recorded on the caller's side, ordered by total time spent. Binding `cache` is a hit in the `@Cached` layer.", "",
                  md_table(["call [binding]", "n", "errors", "mean", "p50", "p95", "p99", "max"],
                           [[k, v["count"], v["errors"] or "", *[fmt(v[x]) for x in ("mean", "p50", "p95", "p99", "max")]] for k, v in ordered]), ""]
            L += ["By binding: " + ", ".join(f"{b} {v['calls']} calls, mean {v['meanMs']:.2f} ms" for b, v in sorted(m["byBinding"].items()))
                  + f". {m['callsPerOrder']:.1f} module-to-module calls per order.", ""]
        if m.get("ordersWithoutTrace"):
            L += [f"**Warning:** {m['ordersWithoutTrace']} orders had no trace records (buffer overwritten or settle too short); "
                  "call statistics are incomplete.", ""]
        L += [f"Raw samples: `{m['outDir']}` (`orders.csv`, `calls.csv`, `summary.json`).", ""]
    L += ["## Notes", "",
          *(["- The `small` profile uses two customers and two SKUs, so the `@Cached` hit rate is far higher than real traffic would give."]
            if args.profile == "small" else []),
          "- A cache is only as good as the key reuse inside its TTL: compare hit rates across profiles, and across run lengths.",
          "- Tracing adds overhead; compare with `--no-trace` for pure client round trips.",
          "- Client and servers share one machine, so CPU contention is part of every figure.", ""]
    path.write_text("\n".join(L))
    return path


VARIED = set()      # which of "cache" / "parallel" differ between the runs being compared; only those appear in labels


def label(m):
    return m["mode"] + "".join(f" {k} {m[k]}" for k in ("cache", "parallel") if k in VARIED and k in m)


def change(before, after):
    return f"{(after - before) / before * 100:+.0f}%" if before else "-"


def comparison_tables(summaries):
    """Every table puts the measured modes side by side: [(title, note, header, rows, left-aligned columns)]."""
    measured = [m for m in summaries if "roundTripMs" in m]
    VARIED.clear()
    VARIED.update(k for k in ("cache", "parallel") if len({m.get(k) for m in summaries}) > 1)
    tables = []
    rows = []
    for m in summaries:
        rt = m.get("roundTripMs")
        rows.append([label(m), f"{m['ordersPerSecond']:.0f}" if "ordersPerSecond" in m else "-", m.get("ok", "-"), m.get("declined", "-"),
                     *([fmt(rt[k]) for k in ("min", "mean", "p50", "p90", "p95", "p99", "max")] if rt else ["-"] * 7),
                     f"{m['callsPerOrder']:.1f}" if "callsPerOrder" in m else "-",
                     "PASS" if m["clean"] else "FAIL" + (f" ({m['error']})" if "error" in m else "")])
    tables.append(("placeOrder round trip", "Client side, ms. Declined orders (expected) are excluded from the latencies.",
                   ["run", "orders/s", "ok", "declined", "min", "mean", "p50", "p90", "p95", "p99", "max", "calls/order", "result"], rows, 1))

    for dimension, title in (("cache", "Cache effect (off to on)"), ("parallel", "Parallel quotes effect (off to on)")):
        other = "parallel" if dimension == "cache" else "cache"
        groups = collections.defaultdict(dict)
        for m in measured:
            if dimension in m:
                groups[(m["mode"], m.get(other))][m[dimension]] = m
        pairs = [(key, v["off"], v["on"]) for key, v in groups.items() if "off" in v and "on" in v]
        if not pairs:
            continue
        rows = []
        for (mode, fixed), off, on in pairs:
            name = mode + (f" ({other} {fixed})" if other in VARIED else "")
            row = [name, f"{off['ordersPerSecond']:.0f}", f"{on['ordersPerSecond']:.0f}", change(off["ordersPerSecond"], on["ordersPerSecond"])]
            for k in ("p50", "p95", "p99"):
                a, b = off["roundTripMs"][k], on["roundTripMs"][k]
                row += [fmt(a), fmt(b), change(a, b)]
            rows.append(row)
        tables.append((title, "Higher orders/s and lower latencies are better.",
                       ["mode", "orders/s off", "on", "change", "p50 off", "on", "change", "p95 off", "on", "change", "p99 off", "on", "change"], rows, 1))

    columns = [label(m) for m in measured if m.get("edges")]
    with_edges = [m for m in measured if m.get("edges")]
    if with_edges:
        total = collections.Counter()
        for m in with_edges:
            for edge, v in m["edges"].items():
                total[edge] += v["count"] * v["mean"]
        order = [edge for edge, _ in total.most_common()]
        for key, name in (("mean", "Mean ms per module-to-module call"), ("p95", "p95 ms per module-to-module call")):
            rows = [[edge, *[fmt(m["edges"][edge][key]) if edge in m["edges"] else "-" for m in with_edges]] for edge in order]
            rows.append(["(calls per order)", *[f"{m['callsPerOrder']:.1f}" for m in with_edges]])
            tables.append((name, "Recorded on the caller's side, over every binding the call used (a cache hit counts as a call). "
                           "Ordered by total time spent.", ["call", *columns], rows, 1))
        methods = sorted({k for m in with_edges for k in m.get("cacheHitRate", {})})
        if methods:
            rows = [[k, *[f"{m['cacheHitRate'][k]['rate']:.1%}" if k in m.get("cacheHitRate", {}) else "-" for m in with_edges]] for k in methods]
            tables.append(("Cache hit rate", "Share of calls to each `@Cached` method answered from the cache.", ["method", *columns], rows, 1))
        rows = []
        for m in with_edges:
            rows.append([label(m), *[f"{m['byBinding'][b]['calls']} calls, {m['byBinding'][b]['meanMs']:.2f} ms" if b in m["byBinding"] else "-"
                                     for b in ("cache", "local", "loopback-codec", "loopback-http", "remote")]])
        tables.append(("Calls by binding", "Count and mean latency of the module-to-module calls, by how each was served.",
                       ["run", "cache", "local", "loopback-codec", "loopback-http", "remote"], rows, 1))
    return tables


def main():
    parser = argparse.ArgumentParser(description="Performance driver for the running demo mode")
    parser.add_argument("--orders", type=int, default=200, help="orders to place (default 200)")
    parser.add_argument("--duration", type=float, help="run for this many seconds instead of a fixed number of orders")
    parser.add_argument("--concurrency", type=int, default=4, help="concurrent clients (default 4)")
    parser.add_argument("--warmup", type=int, default=20, dest="warmup_orders", help="orders sent first and not measured (default 20)")
    parser.add_argument("--settle", type=float, default=3.0, help="seconds to wait after an order before reading its trace (default 3)")
    parser.add_argument("--drain", type=float, default=30.0, help="max seconds to keep re-reading traces for late async calls (default 30)")
    parser.add_argument("--no-trace", action="store_true", help="measure client round trips only (no per-call statistics, least overhead)")
    parser.add_argument("--profile", choices=sorted(PROFILES), default="realistic",
                        help="customers, SKUs and popularity skew of the workload (default realistic: 10,000 customers, 2,000 SKUs, Zipf)")
    parser.add_argument("--customers", type=int, help="number of customers (overrides the profile)")
    parser.add_argument("--skus", type=int, help="number of SKUs (overrides the profile)")
    parser.add_argument("--customer-skew", type=float, help="Zipf exponent of customer popularity, 0 = uniform (overrides the profile)")
    parser.add_argument("--sku-skew", type=float, help="Zipf exponent of SKU popularity, 0 = uniform (overrides the profile)")
    parser.add_argument("--seed", type=int, default=42, help="seed of the order generator (default 42; same seed, same orders)")
    parser.add_argument("--no-seed", "--no-stock", dest="no_seed", action="store_true",
                        help="do not load the profile's customers, products and stock before the run (already loaded)")
    parser.add_argument("--out", help="directory for the raw samples and summary (default .demo/perf/<timestamp>)")
    parser.add_argument("--cache", choices=["on", "off", "both"], default="on",
                        help="with --modes: run with the @Cached layer on (default), off, or both, to compare (rpc.cache.enabled)")
    parser.add_argument("--parallel", choices=["on", "off", "both"], default="on",
                        help="with --modes: orders runs its customer check and two quotes in parallel (default on), sequentially (off), or both")
    parser.add_argument("--modes", help="comma-separated modes to start, measure and stop in turn (each on a clean database), or 'all'; "
                                        "without it the mode that is already running is measured")
    args = parser.parse_args()
    if args.modes:
        return run_modes(args)
    summary = measure(args, None)
    print("report: " + str(write_report(Path(summary["outDir"]), args, [summary])))
    return 0 if summary["clean"] else 1


def measure(args, out_dir):
    """One measurement of the running mode; returns its summary (also written to summary.json)."""
    if not (DEMO / "mode").exists():
        sys.exit("no demo is running (scripts/demo.sh <mode> start)")
    mode = (DEMO / "mode").read_text().strip()
    orders_url = (DEMO / "orders.url").read_text().strip()
    client = Client(orders_url)
    run_id = uuid.uuid4().hex[:8]
    base = PROFILES[args.profile]
    workload = Workload(args.profile, args.customers or base["customers"], args.skus or base["skus"],
                        base["customer_skew"] if args.customer_skew is None else args.customer_skew,
                        base["sku_skew"] if args.sku_skew is None else args.sku_skew, args.seed)
    args.workload = workload
    planned = args.orders if not args.duration else int(args.duration * 400)   # generous stock bound for a timed run
    if not args.no_seed:
        seed_data(mode, workload, planned + args.warmup_orders)
    print("workload " + workload.describe())

    print(f"mode {mode}, {orders_url}, concurrency {args.concurrency}, "
          + (f"{args.duration:g} s" if args.duration else f"{args.orders} orders") + f", warmup {args.warmup_orders}")
    if args.warmup_orders:
        warm = argparse.Namespace(orders=args.warmup_orders, duration=None, concurrency=args.concurrency, warmup_orders=0,
                                  workload=args.workload)
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
    declined = [r for r in results if r["outcome"] == DECLINED]
    failed = collections.Counter(r["outcome"] for r in results if r["outcome"] not in ("ok", DECLINED))
    print(f"\nplaceOrder round trip (client side): {len(results)} orders in {elapsed:.1f} s = {len(results) / elapsed:.1f} orders/s, "
          f"{len(ok)} ok, {len(declined)} declined (expected), {sum(failed.values())} failed")
    if failed:
        for outcome, count in failed.most_common(5):
            print(f"  {count} x {outcome}")
    out = out_dir or (Path(args.out) if args.out else DEMO / "perf" / STAMP)
    summary = {"mode": mode, "outDir": str(out), "concurrency": args.concurrency, "orders": len(results), "ok": len(ok), "elapsedS": elapsed,
               "ordersPerSecond": len(results) / elapsed, "clean": not failed, "declined": len(declined),
               "workload": workload.describe()}
    if not ok:
        return summary
    stats = summarize([r["rttMs"] for r in ok])
    rows = [["placeOrder", stats["count"], *[fmt(stats[k]) for k in ("min", "mean", "p50", "p90", "p95", "p99", "max")]]]
    if declined:
        d = summarize([r["rttMs"] for r in declined])
        summary["declinedRoundTripMs"] = d
        rows.append(["placeOrder (declined)", d["count"], *[fmt(d[k]) for k in ("min", "mean", "p50", "p90", "p95", "p99", "max")]])
    print_table("Round trip, ms", rows, ["call", "n", "min", "mean", "p50", "p90", "p95", "p99", "max"])

    edge_rows, edge_summary = [], {}
    if traces:
        groups = collections.defaultdict(list)
        errors = collections.Counter()
        method_calls, method_hits = collections.Counter(), collections.Counter()
        empty = 0
        for cid, records in traces.items():
            if not records:
                empty += 1
            for record in records:
                key = (f"{record['caller']} -> {record['callee']}.{record['method']}", record["binding"])
                groups[key].append(record["durationMs"])
                method_calls[f"{record['callee']}.{record['method']}"] += 1
                if record["binding"] == "cache":
                    method_hits[f"{record['callee']}.{record['method']}"] += 1
                if record["outcome"] != "ok":
                    errors[key] += 1
        for key, values in sorted(groups.items(), key=lambda item: -sum(item[1])):
            s = summarize(values)
            edge_summary[f"{key[0]} [{key[1]}]"] = {**s, "errors": errors[key]}
            edge_rows.append([key[0], key[1], s["count"], errors[key] or "", *[fmt(s[k]) for k in ("mean", "p50", "p95", "p99", "max")]])
        print_table("Module-to-module calls, ms (recorded on the caller's side; ordered by total time spent)", edge_rows,
                    ["call", "binding", "n", "errors", "mean", "p50", "p95", "p99", "max"])
        per_binding = collections.defaultdict(list)
        merged, edge_hits = collections.defaultdict(list), collections.Counter()
        for (edge, binding), values in groups.items():
            per_binding[binding].extend(values)
            merged[edge].extend(values)
            if binding == "cache":
                edge_hits[edge] += len(values)
        summary["edges"] = {edge: {**summarize(values), "hits": edge_hits[edge]} for edge, values in merged.items()}
        summary["byBinding"] = {b: {"calls": len(v), "meanMs": sum(v) / len(v)} for b, v in per_binding.items()}
        print("\nBy binding: " + ", ".join(f"{b} {len(v)} calls mean {sum(v) / len(v):.1f} ms" for b, v in sorted(per_binding.items())))
        if method_hits:
            summary["cacheHitRate"] = {m: {"calls": method_calls[m], "hits": h, "rate": h / method_calls[m]} for m, h in method_hits.items()}
            print("Cache hit rate: " + ", ".join(f"{m} {v['rate']:.1%} of {v['calls']}" for m, v in sorted(summary["cacheHitRate"].items())))
        calls_per_order = sum(len(v) for v in groups.values()) / max(1, len(traces) - empty)
        summary["callsPerOrder"] = calls_per_order
        print(f"{calls_per_order:.1f} module-to-module calls per order")
        if empty:
            summary["ordersWithoutTrace"] = empty
            print(f"WARNING: {empty} of {len(traces)} orders had no trace records (buffer overwritten by a too-fast run, "
                  f"or --settle too short); raise demo.trace.capacity or lower --concurrency for complete call statistics")

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
    base = Path(args.out) if args.out else DEMO / "perf" / STAMP
    demo_sh = str(ROOT / "scripts" / "demo.sh")
    summaries = []
    cache_variants = ["off", "on"] if args.cache == "both" else [args.cache]
    parallel_variants = ["off", "on"] if args.parallel == "both" else [args.parallel]
    for mode, variant, parallel in [(m, c, p) for m in modes for c in cache_variants for p in parallel_variants]:
        label = mode + (f"-cache-{variant}" if len(cache_variants) > 1 else "") + (f"-parallel-{parallel}" if len(parallel_variants) > 1 else "")
        print(f"\n================ {mode} (cache {variant}, parallel quotes {parallel}) ================", flush=True)
        env = dict(os.environ, RPC_CACHE_ENABLED="true" if variant == "on" else "false",
                   MODULES_ORDERS_PARALLEL_QUOTES="true" if parallel == "on" else "false")
        started = subprocess.run([demo_sh, mode, "start"], capture_output=True, text=True, env=env)
        if started.returncode != 0:
            print(f"{mode} did not start:\n" + (started.stdout + started.stderr)[-1500:])
            subprocess.run([demo_sh, mode, "stop"], capture_output=True)
            summaries.append({"mode": mode, "cache": variant, "parallel": parallel, "clean": False, "error": "did not start"})
            continue
        try:
            summary = measure(args, base / label)
            summary["cache"] = variant
            summary["parallel"] = parallel
            summaries.append(summary)
        except BaseException:
            summaries.append({"mode": mode, "cache": variant, "parallel": parallel, "clean": False, "error": "measurement failed"})
            raise
        finally:
            subprocess.run([demo_sh, mode, "stop"], capture_output=True)

    print("\n================ comparison ================")
    for title, note, header, rows, left in comparison_tables(summaries):
        print_table(f"{title}. {note}".replace("`", ""), rows, header, left)
    base.mkdir(parents=True, exist_ok=True)
    (base / "comparison.json").write_text(json.dumps(summaries, indent=2))
    print(f"\nper-mode samples and comparison.json: {base}")
    print("report: " + str(write_report(base, args, summaries)))
    return 0 if all(m["clean"] for m in summaries) else 1


if __name__ == "__main__":
    sys.exit(main())
