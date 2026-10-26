#!/usr/bin/env python3
"""Table for docs/performance/jvm-settings.md from two saved perf runs that differ only in the JVM flags (Python 3 standard library only).

  jvm_report.py --c1 <stamp> --c2 <stamp> [--rerun <stamp>] [--out docs/performance/_generated]
  jvm_report.py --matrix <file written by jvm_matrix.sh> [--out docs/performance/_generated]

--rerun replaces the C2 runs of the modes it contains with those of a later rerun of just those modes (marked with a dagger in the table).

The matrix tables are means over the repetitions of each heap x collector, after dropping outliers: a repetition is dropped for a mode when its
orders/s is more than 2 scaled MADs (1.4826 x the median absolute deviation) and more than 5% from the median of that mode's repetitions
(the 5% keeps tightly clustered repetitions from losing runs that differ by a percent or two).
"""
import argparse
import json
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PERF = ROOT / ".demo" / "perf"
MODE_ORDER = ["modulith", "macro-modulith", "loopback-codec", "loopback-http", "groups", "split-dbs", "microservices", "microservices-macro"]


def runs(stamp):
    return [r for r in json.loads((PERF / stamp / "comparison.json").read_text()) if "roundTripMs" in r]


def pick(rs, mode, cache):
    return next(r for r in rs if r["mode"] == mode and r["cache"] == cache)


def keep(values):
    """Indexes of the values that are not outliers (2 scaled MADs from the median)."""
    med = statistics.median(values)
    mad = 1.4826 * statistics.median(abs(v - med) for v in values)
    return [i for i, v in enumerate(values) if abs(v - med) <= max(2 * mad, 0.05 * med)]


def matrix(path, out):
    cfgs, data = [], {}
    for line in Path(path).read_text().split("\n"):
        if not line.strip():
            continue
        cfg, rep, stamp = line.split()
        if cfg not in cfgs:
            cfgs.append(cfg)
        for r in runs(stamp):
            if r["cache"] == "on":
                data.setdefault((r["mode"], cfg), []).append((rep, r))
    cells, dropped = {}, []
    for (mode, cfg), rs in data.items():
        idx = keep([r["ordersPerSecond"] for _, r in rs])
        kept = [rs[i][1] for i in idx]
        dropped += [(mode, cfg, rep, r["ordersPerSecond"]) for i, (rep, r) in enumerate(rs) if i not in idx]
        cells[mode, cfg] = {
            "n": len(rs), "kept": len(kept),
            "ops": statistics.mean(r["ordersPerSecond"] for r in kept),
            "p50": statistics.mean(r["roundTripMs"]["p50"] for r in kept),
            "p99": statistics.mean(r["roundTripMs"]["p99"] for r in kept),
        }
    def tbl(title_key, fmt):
        head = ["mode"] + cfgs
        lines = ["| " + " | ".join(head) + " |", "| --- |" + " ---: |" * len(cfgs)]
        for mode in MODE_ORDER:
            lines.append("| " + " | ".join([mode] + [fmt(cells[mode, c][title_key]) for c in cfgs]) + " |")
        return "\n".join(lines) + "\n"
    reps = max(c["n"] for c in cells.values())
    text = {
        "jvm-matrix-throughput.md": tbl("ops", lambda v: f"{v:.0f}"),
        "jvm-matrix-p50.md": tbl("p50", lambda v: f"{v:.1f}"),
        "jvm-matrix-p99.md": tbl("p99", lambda v: f"{v:.1f}"),
        "jvm-matrix-dropped.md": (
            f"{len(dropped)} of {len(cells) * reps} mode runs were dropped as outliers.\n\n"
            + ("| mode | config | repetition | orders/s |\n| --- | --- | ---: | ---: |\n"
               + "\n".join(f"| {m} | {c} | {r} | {v:.0f} |" for m, c, r, v in sorted(dropped)) + "\n" if dropped else "")),
    }
    for name, body in text.items():
        Path(out, name).write_text(body)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--c1")
    p.add_argument("--c2")
    p.add_argument("--matrix")
    p.add_argument("--rerun")
    p.add_argument("--out", default=str(ROOT / "docs" / "performance" / "_generated"))
    a = p.parse_args()
    if a.matrix:
        return matrix(a.matrix, a.out)
    c1, c2 = runs(a.c1), runs(a.c2)
    redone = set()
    if a.rerun:
        again = runs(a.rerun)
        redone = {r["mode"] for r in again}
        c2 = [r for r in c2 if r["mode"] not in redone] + again
    header = ["mode", "cache", "orders/s C1", "C2", "change", "p50 ms C1", "C2", "p99 ms C1", "C2"]
    rows = []
    for mode in MODE_ORDER:
        for cache in ("off", "on"):
            x, y = pick(c1, mode, cache), pick(c2, mode, cache)
            xo, yo = x["ordersPerSecond"], y["ordersPerSecond"]
            rows.append([mode + (" †" if mode in redone else ""), cache, f"{xo:.0f}", f"{yo:.0f}", f"{(yo - xo) / xo * 100:+.0f}%",
                         f"{x['roundTripMs']['p50']:.1f}", f"{y['roundTripMs']['p50']:.1f}",
                         f"{x['roundTripMs']['p99']:.1f}", f"{y['roundTripMs']['p99']:.1f}"])
    out = ["| " + " | ".join(header) + " |", "| --- | --- |" + " ---: |" * (len(header) - 2)]
    out += ["| " + " | ".join(r) + " |" for r in rows]
    text = '<div class="wide-table" markdown>\n\n' + "\n".join(out) + "\n\n</div>\n"
    Path(a.out, "jvm-settings.md").write_text(text)


if __name__ == "__main__":
    main()
