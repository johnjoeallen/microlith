#!/usr/bin/env python3
"""Tables for docs/performance/wire-formats.md from the saved wire-format perf runs and the WireCostBenchmark output (Python 3 standard library only).

  wire_report.py --small <stamp> --lines40 <stamp> --lines150 <stamp> --bench <benchmark output file> [--out docs/performance/_generated]
"""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PERF = ROOT / ".demo" / "perf"
FORMATS = ["json", "gzip", "bson", "bson-gzip"]


def runs(stamp):
    return [r for r in json.loads((PERF / stamp / "comparison.json").read_text()) if "roundTripMs" in r]


def pick(rs, mode, wire):
    return next((r for r in rs if r["mode"] == mode and r.get("wire") == wire), None)


def pct(new, base):
    return f"{new / base * 100:.0f}%"


def delta(new, base):
    return f"{(new - base) / base * 100:+.0f}%"


def table(header, rows, left=2):
    out = ["| " + " | ".join(header) + " |", "|" + " --- |" * left + " ---: |" * (len(header) - left)]
    out += ["| " + " | ".join(str(c) for c in row) + " |" for row in rows]
    text = "\n".join(out) + "\n"
    return '<div class="wide-table" markdown>\n\n' + text + "\n</div>\n" if len(header) >= 9 else text


def main():
    p = argparse.ArgumentParser()
    for k in ("small", "lines40", "lines150", "bench"):
        p.add_argument(f"--{k}", required=True)
    p.add_argument("--out", default=str(ROOT / "docs" / "performance" / "_generated"))
    a = p.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    sets = [("1 to 4 lines (the realistic mix)", runs(a.small)), ("40 lines", runs(a.lines40)), ("150 lines", runs(a.lines150))]

    # bytes on the wire per order, by order size and mode
    rows = []
    for size, rs in sets:
        for mode in ("loopback-http", "groups", "microservices"):
            base = pick(rs, mode, "json")
            if not base or not base.get("wireStats"):
                continue
            w = base["wireStats"]
            cells = [pct(pick(rs, mode, f)["wireStats"]["wireBytesPerOrder"], w["wireBytesPerOrder"]) for f in FORMATS[1:]]
            rows.append([size, mode, f"{w['httpCallsPerOrder']:.1f}", f"{w['wireBytesPerOrder']:,.0f}", f"{w['wireBytesPerCall']:.0f}", *cells])
    (out / "wire-bytes.md").write_text(table(["Order size", "Mode", "HTTP calls/order", "JSON bytes/order", "JSON bytes/call", "gzip", "bson", "bson-gzip"], rows))

    # throughput and median latency against json
    rows = []
    for size, rs in sets:
        for mode in ("loopback-http", "groups", "microservices"):
            base = pick(rs, mode, "json")
            if not base:
                continue
            row = [size, mode, f"{base['ordersPerSecond']:.0f}"]
            for f in FORMATS[1:]:
                alt = pick(rs, mode, f)
                row += [f"{alt['ordersPerSecond']:.0f} ({delta(alt['ordersPerSecond'], base['ordersPerSecond'])})"]
            row += [f"{base['roundTripMs']['p50']:.1f}"]
            for f in FORMATS[1:]:
                alt = pick(rs, mode, f)
                row += [f"{alt['roundTripMs']['p50']:.1f} ({delta(alt['roundTripMs']['p50'], base['roundTripMs']['p50'])})"]
            rows.append(row)
    (out / "wire-throughput.md").write_text(table(
        ["Order size", "Mode", "orders/s json", "gzip", "bson", "bson-gzip", "p50 ms json", "gzip", "bson", "bson-gzip"], rows))

    # the CPU benchmark, as a table and as the raw output
    text = Path(a.bench).read_text()
    block = re.search(r"lines  envelope.*?\n\n", text, re.S).group(0)
    rows = []
    for line in block.splitlines()[1:]:
        m = re.match(r"\s*(\d+)\s+(\d+)\s+(\S+)\s+(\d+)\s+(\d+)%\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+(.*)$", line)
        if m:
            lines, envelope, fmt, size, pc, enc, dec, extra, be = m.groups()
            be = be.strip()
            if be not in ("-", "never (no bytes saved)"):
                be = f"{float(be):,.1f} MB/s ({float(be) * 8:,.0f} Mbit/s)" if be != "any link" else be
            rows.append([lines, f"{int(envelope):,}", fmt, f"{int(size):,}", f"{pc}%", enc, dec, extra, be])
    (out / "wire-cost.md").write_text(table(
        ["Order lines", "JSON bytes", "Format", "Bytes", "vs JSON", "Encode µs", "Decode µs", "Extra µs vs JSON", "Break-even link speed"], rows))
    (out / "wire-cost.txt").write_text(block)
    print("wrote", len(list(out.glob("wire-*"))), "files")


if __name__ == "__main__":
    main()
