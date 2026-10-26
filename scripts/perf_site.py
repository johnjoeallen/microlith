#!/usr/bin/env python3
"""Builds the tables and charts of the documentation's performance pages from saved perf runs (Python 3 standard library only).

  perf_site.py --modes <stamp> --parallel <stamp> --switches <stamp> --load4 <stamp> --load16 <stamp> --drain <log> [--out ...]

Each <stamp> is a directory under .demo/perf/ written by scripts/perf.sh. The pages include the generated files (pymdownx.snippets), so
the numbers on the site are the numbers in the saved summaries, never retyped.
"""
import argparse
import collections
import html
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PERF = ROOT / ".demo" / "perf"
MODE_ORDER = ["modulith", "macro-modulith", "loopback-codec", "loopback-http", "groups", "split-dbs", "microservices", "microservices-macro"]


def load(stamp):
    """The runs of one perf.sh --modes invocation, with their cache / parallel / async labels (comparison.json)."""
    runs = json.loads((PERF / stamp / "comparison.json").read_text())
    for r in runs:
        r.setdefault("async", "outbox")
    return [r for r in runs if "roundTripMs" in r]


def pick(runs, **want):
    for r in runs:
        if all(r.get(k) == v for k, v in want.items()):
            return r
    return None


def nice(v, digits=0):
    return f"{v:,.{digits}f}"


def change(a, b):
    return f"{(b - a) / a * 100:+.0f}%" if a else "-"


def md_table(header, rows, left=1, last_left=False):
    """Markdown table; the first `left` columns are left-aligned, the rest right-aligned (numbers), the last one left-aligned for text."""
    last = len(header) - 1
    out = ["| " + " | ".join(header) + " |",
           "|" + "|".join(" --- " if i < left or (last_left and i == last) else " ---: " for i in range(len(header))) + "|"]
    out += ["| " + " | ".join(str(c) for c in row) + " |" for row in rows]
    table = "\n".join(out) + "\n"
    if len(header) >= 9:      # wide tables get a compact style (docs/stylesheets/extra.css)
        return '<div class="wide-table" markdown>\n\n' + table + "\n</div>\n"
    return table


# ---------------------------------------------------------------------------------------------------------------- charts

def nice_ticks(maximum):
    step = 10 ** (len(str(int(maximum))) - 1)
    for mult in (1, 2, 5, 10):
        if maximum / (step * mult) <= 5:
            step *= mult
            break
    ticks, v = [], 0
    while v < maximum + step * 0.0001:
        ticks.append(v)
        v += step
    if ticks[-1] < maximum:
        ticks.append(ticks[-1] + step)
    return ticks


def bar_path(x, y, w, h, r=4):
    """A horizontal bar growing from a square baseline at x with a rounded data end."""
    r = min(r, w / 2, h / 2)
    if r < 0.5:
        return f"M{x:.1f},{y:.1f}h{max(w, 0.5):.1f}v{h:.1f}h-{max(w, 0.5):.1f}z"
    return (f"M{x:.1f},{y:.1f}h{w - r:.1f}a{r:.1f},{r:.1f} 0 0 1 {r:.1f},{r:.1f}v{h - 2 * r:.1f}a{r:.1f},{r:.1f} 0 0 1 -{r:.1f},{r:.1f}"
            f"h-{w - r:.1f}z")


def hbar(title, subtitle, categories, series, unit, fmt=lambda v: f"{v:,.0f}"):
    """A horizontal bar chart: series is [(name, [value per category])]. At most two series, in the fixed order s1, s2."""
    n = len(series)
    label_w = max(len(c) for c in categories) * 6.6 + 18
    width, right = 760, 64
    plot_w = width - label_w - right
    bar_h = 16 if n > 1 else 20
    row_h = n * bar_h + (n - 1) * 2 + 16
    top, bottom = 6, 26
    height = top + len(categories) * row_h + bottom
    maximum = max(v for _, vals in series for v in vals) or 1
    ticks = nice_ticks(maximum)
    scale = plot_w / ticks[-1]
    parts = [f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{html.escape(title)}: {html.escape(subtitle)}">']
    for t in ticks:
        x = label_w + t * scale
        parts.append(f'<line class="grid" x1="{x:.1f}" y1="{top}" x2="{x:.1f}" y2="{height - bottom}"/>')
        parts.append(f'<text class="tick" x="{x:.1f}" y="{height - 8}" text-anchor="middle">{fmt(t)}</text>')
    parts.append(f'<line class="axis" x1="{label_w}" y1="{top}" x2="{label_w}" y2="{height - bottom}"/>')
    for i, category in enumerate(categories):
        y0 = top + i * row_h + 8
        tip = f"{category}: " + ", ".join(f"{name} {fmt(vals[i])} {unit}" for name, vals in series)
        parts.append(f'<g class="row"><title>{html.escape(tip)}</title>')
        parts.append(f'<rect class="hit" x="0" y="{top + i * row_h}" width="{width}" height="{row_h}"/>')
        parts.append(f'<text class="cat" x="{label_w - 8}" y="{y0 + (n * bar_h + (n - 1) * 2) / 2 + 4:.1f}" text-anchor="end">{html.escape(category)}</text>')
        for j, (name, vals) in enumerate(series):
            y = y0 + j * (bar_h + 2)
            w = vals[i] * scale
            parts.append(f'<path class="mark s{j + 1}" d="{bar_path(label_w, y, w, bar_h)}"><title>{html.escape(name)}: {fmt(vals[i])} {unit}</title></path>')
            parts.append(f'<text class="val" x="{label_w + w + 6:.1f}" y="{y + bar_h / 2 + 4:.1f}">{fmt(vals[i])}</text>')
        parts.append("</g>")
    parts.append("</svg>")
    legend = ""
    if n > 1:
        legend = '<ul class="viz-legend">' + "".join(f'<li><span class="sw s{j + 1}"></span>{html.escape(name)}</li>' for j, (name, _) in enumerate(series)) + "</ul>"
    rows = "".join(f"<tr><td>{html.escape(c)}</td>" + "".join(f"<td>{fmt(vals[i])}</td>" for _, vals in series) + "</tr>" for i, c in enumerate(categories))
    head = "<tr><th></th>" + "".join(f"<th>{html.escape(name)} ({unit})</th>" for name, _ in series) + "</tr>"
    table = f'<details class="viz-data"><summary>Show the numbers</summary><table><thead>{head}</thead><tbody>{rows}</tbody></table></details>'
    # no blank lines inside: a blank line would end the HTML block in markdown
    return (f'<figure class="viz"><figcaption><strong>{html.escape(title)}</strong><span>{html.escape(subtitle)}</span></figcaption>'
            f'{legend}{"".join(parts)}{table}</figure>\n')


# ---------------------------------------------------------------------------------------------------------------- content

def modes_fragments(runs, out):
    base = {m: pick(runs, mode=m, cache="on") for m in MODE_ORDER}
    base = {m: r for m, r in base.items() if r}
    rows = []
    ref = base["modulith"]["ordersPerSecond"]
    for m, r in base.items():
        rt = r["roundTripMs"]
        bb = r.get("byBinding", {})
        calls = ", ".join(f"{b} {v['calls']:,} ({v['meanMs']:.2f} ms)" for b, v in sorted(bb.items()))
        rows.append([f"{m}", nice(r["ordersPerSecond"]), f"{r['ordersPerSecond'] / ref * 100:.0f}%", f"{rt['p50']:.1f}", f"{rt['p95']:.1f}",
                     f"{rt['p99']:.1f}", f"{r['callsPerOrder']:.1f}", calls])
    (out / "modes-table.md").write_text(md_table(
        ["Mode", "Orders/s", "vs modulith", "p50 ms", "p95 ms", "p99 ms", "Calls/order", "Calls by binding (mean latency)"], rows, last_left=True))
    (out / "modes-throughput.md").write_text(hbar("Throughput by mode", "orders per second, 4 clients, result cache on; longer is better",
                                                  list(base), [("orders/s", [r["ordersPerSecond"] for r in base.values()])], "orders/s"))
    (out / "modes-latency.md").write_text(hbar("Round-trip latency by mode", "milliseconds per order, 4 clients; shorter is better",
                                               list(base), [("p50", [r["roundTripMs"]["p50"] for r in base.values()]),
                                                            ("p99", [r["roundTripMs"]["p99"] for r in base.values()])], "ms", lambda v: f"{v:.1f}"))
    # where the time goes: mean ms per module-to-module call
    shown = [m for m in ("modulith", "loopback-codec", "loopback-http", "groups", "microservices") if m in base]
    edges = collections.Counter()
    for m in shown:
        for edge, v in base[m]["edges"].items():
            edges[edge] += v["count"] * v["mean"]
    order = [e for e, _ in edges.most_common(12)]
    rows = [[f"{e}", *[f"{base[m]['edges'][e]['mean']:.2f}" if e in base[m]["edges"] else "-" for m in shown]] for e in order]
    (out / "calls-table.md").write_text(md_table(["Call (mean ms, all bindings)", *[f"{m}" for m in shown]], rows))
    # cache effect over all modes
    rows, cats, off, on = [], [], [], []
    for m in MODE_ORDER:
        a, b = pick(runs, mode=m, cache="off"), pick(runs, mode=m, cache="on")
        if not (a and b):
            continue
        cats.append(m)
        off.append(a["ordersPerSecond"])
        on.append(b["ordersPerSecond"])
        ra, rb = a["roundTripMs"], b["roundTripMs"]
        rows.append([f"{m}", nice(a["ordersPerSecond"]), nice(b["ordersPerSecond"]), change(a["ordersPerSecond"], b["ordersPerSecond"]),
                     f"{ra['p50']:.1f}", f"{rb['p50']:.1f}", change(ra["p50"], rb["p50"]), f"{ra['p99']:.1f}", f"{rb['p99']:.1f}", change(ra["p99"], rb["p99"])])
    (out / "cache-table.md").write_text(md_table(["Mode", "orders/s off", "on", "Δ", "p50 off", "on", "Δ", "p99 off", "on", "Δ"], rows))
    (out / "cache-chart.md").write_text(hbar("Result cache: throughput", "orders per second, 4 clients; longer is better", cats,
                                             [("cache off", off), ("cache on", on)], "orders/s"))
    methods = sorted({k for r in runs if r.get("cache") == "on" for k in r.get("cacheHitRate", {})})
    shown = [m for m in MODE_ORDER if pick(runs, mode=m, cache="on") and pick(runs, mode=m, cache="on").get("cacheHitRate")]
    rows = [[f"{k}", *[f"{pick(runs, mode=m, cache='on')['cacheHitRate'][k]['rate']:.0%}" if k in pick(runs, mode=m, cache='on')["cacheHitRate"] else "-"
                         for m in shown]] for k in methods]
    (out / "cache-hits.md").write_text(md_table(["Cached method", *[f"{m}" for m in shown]], rows))
    return base


def effect_fragments(parallel_runs, switch_runs, out):
    """Parallel quotes (off to on) for every mode we have both for; async (outbox to events) from the switch run."""
    rows, cats, off, on = [], [], [], []
    pool = parallel_runs + switch_runs
    for m in MODE_ORDER:
        a = pick(pool, mode=m, parallel="off", cache="on", **{"async": "outbox"})
        b = pick(pool, mode=m, parallel="on", cache="on", **{"async": "outbox"})
        if not (a and b):
            continue
        cats.append(m)
        off.append(a["ordersPerSecond"])
        on.append(b["ordersPerSecond"])
        ra, rb = a["roundTripMs"], b["roundTripMs"]
        rows.append([f"{m}", nice(a["ordersPerSecond"]), nice(b["ordersPerSecond"]), change(a["ordersPerSecond"], b["ordersPerSecond"]),
                     f"{ra['p50']:.1f}", f"{rb['p50']:.1f}", change(ra["p50"], rb["p50"]), f"{ra['p99']:.1f}", f"{rb['p99']:.1f}", change(ra["p99"], rb["p99"])])
    (out / "parallel-table.md").write_text(md_table(["Mode", "orders/s off", "on", "Δ", "p50 off", "on", "Δ", "p99 off", "on", "Δ"], rows))
    (out / "parallel-chart.md").write_text(hbar("Parallel quotes: throughput", "orders per second, 4 clients; longer is better", cats,
                                                [("sequential", off), ("parallel", on)], "orders/s"))
    rows, cats, ob, ev = [], [], [], []
    for m in MODE_ORDER:
        a = pick(switch_runs, mode=m, parallel="on", **{"async": "outbox"})
        b = pick(switch_runs, mode=m, parallel="on", **{"async": "events"})
        if not (a and b):
            continue
        cats.append(m)
        ob.append(a["ordersPerSecond"])
        ev.append(b["ordersPerSecond"])
        ra, rb = a["roundTripMs"], b["roundTripMs"]
        rows.append([f"{m}", nice(a["ordersPerSecond"]), nice(b["ordersPerSecond"]), change(a["ordersPerSecond"], b["ordersPerSecond"]),
                     f"{ra['p50']:.1f}", f"{rb['p50']:.1f}", change(ra["p50"], rb["p50"]), f"{ra['p99']:.1f}", f"{rb['p99']:.1f}", change(ra["p99"], rb["p99"])])
    (out / "async-table.md").write_text(md_table(["Mode", "orders/s outbox", "events", "Δ", "p50 outbox", "events", "Δ", "p99 outbox", "events", "Δ"], rows))
    (out / "async-chart.md").write_text(hbar("Follow-up delivery: throughput", "orders per second, 4 clients; longer is better", cats,
                                             [("outbox", ob), ("events", ev)], "orders/s"))


def load_fragments(runs4, runs16, out):
    cats, four, sixteen, rows = [], [], [], []
    for m in ("modulith", "groups", "microservices"):
        a, b = pick(runs4, mode=m), pick(runs16, mode=m)
        if not (a and b):
            continue
        cats.append(m)
        four.append(a["ordersPerSecond"])
        sixteen.append(b["ordersPerSecond"])
        rows.append([f"{m}", nice(a["ordersPerSecond"]), f"{a['roundTripMs']['p50']:.1f}", f"{a['roundTripMs']['p99']:.1f}",
                     nice(b["ordersPerSecond"]), f"{b['roundTripMs']['p50']:.1f}", f"{b['roundTripMs']['p99']:.1f}",
                     change(a["ordersPerSecond"], b["ordersPerSecond"])])
    (out / "load-table.md").write_text(md_table(["Mode", "4 clients: orders/s", "p50 ms", "p99 ms", "16 clients: orders/s", "p50 ms", "p99 ms", "throughput change"], rows))
    (out / "load-chart.md").write_text(hbar("Throughput under more load", "orders per second, no call tracing; longer is better", cats,
                                            [("4 clients", four), ("16 clients", sixteen)], "orders/s"))


def drain_fragments(log, out):
    """Parses the lines written by the delivery test: '<n> orders placed in Xs; <m> of <n> notifications delivered Ys after the last order'."""
    text = Path(log).read_text()
    found = {}
    mode = None
    for line in text.splitlines():
        if line.startswith("== "):
            mode = line[3:].strip()
        m = re.search(r"(\d+) orders placed in ([\d.]+)s; (\d+) of (\d+) notifications delivered ([\d.]+)s after", line)
        if m and mode:
            found[mode] = {"orders": int(m.group(1)), "placed": float(m.group(2)), "delivered": int(m.group(3)), "lag": float(m.group(5))}
    rows = [[f"{k}", v["orders"], f"{v['placed']:.1f} s", f"{v['delivered']} of {v['orders']}", f"{v['lag']:.1f} s"] for k, v in found.items()]
    (out / "drain-table.md").write_text(md_table(["Delivery", "Orders placed", "Time to place them", "Notifications delivered", "Last one arrived after"], rows))
    if found:
        (out / "drain-chart.md").write_text(hbar("Time until every notification is delivered", "seconds after the last order was placed; shorter is better",
                                                 list(found), [("lag", [v["lag"] for v in found.values()])], "s", lambda v: f"{v:.1f}"))
    return found


def main():
    p = argparse.ArgumentParser()
    for k in ("modes", "parallel", "switches", "load4", "load16"):
        p.add_argument(f"--{k}", required=True)
    p.add_argument("--drain", required=True)
    p.add_argument("--out", default=str(ROOT / "docs" / "performance" / "_generated"))
    a = p.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    base = modes_fragments(load(a.modes), out)
    effect_fragments(load(a.parallel), load(a.switches), out)
    load_fragments(load(a.load4), load(a.load16), out)
    drain_fragments(a.drain, out)
    print("wrote", len(list(out.glob("*.md"))), "fragments to", out)


if __name__ == "__main__":
    main()
