#!/usr/bin/env python3
"""Builds the tables and charts of the documentation's performance pages from saved perf runs (Python 3 standard library only).

  perf_site.py --modes <stamp> --matrix <dir> --drain <log> [--out ...]

Each <stamp> is a directory under .demo/perf/ written by scripts/perf.sh; --modes gives the baseline table (cache on, parallel on, outbox) and --matrix a directory
with one perf.sh invocation per mode sub-directory, run with every switch combination (--cache both --parallel both --async both --dbs both). The pages include the generated files (pymdownx.snippets), so
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
    """The runs of one perf.sh --modes invocation, with their cache / parallel / async / dbs labels (comparison.json). A directory without a
    comparison.json of its own is a set of invocations (one sub-directory each) and is merged."""
    own = PERF / stamp / "comparison.json"
    files = [own] if own.exists() else sorted((PERF / stamp).glob("*/comparison.json"))
    runs = [r for f in files for r in json.loads(f.read_text())]
    for r in runs:
        r.setdefault("async", "outbox")
        r.setdefault("dbs", "split" if r.get("mode") == "split-dbs" else "shared")
        if r.get("mode") == "macro-monolith":  # runs saved before the rename
            r["mode"] = "macro-modulith"
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

def clients(runs):
    return f"{runs[0]['concurrency']} clients"


def modes_fragments(runs, out):
    c = clients(runs)
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
    # where the time goes: mean ms per module-to-module call
    shown = [m for m in ("modulith", "loopback-codec", "loopback-http", "groups", "microservices") if m in base]
    edges = collections.Counter()
    for m in shown:
        for edge, v in base[m]["edges"].items():
            edges[edge] += v["count"] * v["mean"]
    order = [e for e, _ in edges.most_common(12)]
    rows = [[f"{e}", *[f"{base[m]['edges'][e]['mean']:.2f}" if e in base[m]["edges"] else "-" for m in shown]] for e in order]
    (out / "calls-table.md").write_text(md_table(["Call (mean ms, all bindings)", *[f"{m}" for m in shown]], rows))
    return base


DEFAULTS = {"cache": "on", "parallel": "on", "async": "outbox"}


def find(runs, mode, **want):
    """pick() that knows the services have one instance per module under split, so their per-module run is their split run."""
    if want.get("dbs") == "per-module" and mode.startswith("microservices"):
        want = {**want, "dbs": "split"}
    return pick(runs, mode=mode, **want)


def std(runs, mode, **over):
    """The run of a mode with the default switches (cache on, parallel quotes on, outbox, shared databases; split-dbs is always split),
    with the switches named in `over` changed."""
    want = {**DEFAULTS, "dbs": "split" if mode == "split-dbs" else "shared", **over}
    return find(runs, mode, **want)


def effect_rows(runs, dimension, before, after, modes=MODE_ORDER, **over):
    """(mode, run with the switch at `before`, run with it at `after`) for the modes that have both."""
    rows = []
    for m in modes:
        a, b = std(runs, m, **{**over, dimension: before}), std(runs, m, **{**over, dimension: after})
        if a and b:
            rows.append((m, a, b))
    return rows


def effect_table(pairs, before, after, name):
    rows = []
    for m, a, b in pairs:
        ra, rb = a["roundTripMs"], b["roundTripMs"]
        rows.append([f"{m}", nice(a["ordersPerSecond"]), nice(b["ordersPerSecond"]), change(a["ordersPerSecond"], b["ordersPerSecond"]),
                     f"{ra['p50']:.1f}", f"{rb['p50']:.1f}", change(ra["p50"], rb["p50"]), f"{ra['p99']:.1f}", f"{rb['p99']:.1f}", change(ra["p99"], rb["p99"])])
    return md_table(["Mode", f"orders/s {before}", after, "Δ", f"p50 {before}", after, "Δ", f"p99 {before}", after, "Δ"], rows)


def dbs_fragments(runs, out):
    """Database layouts side by side (cache on, parallel quotes on, outbox): one instance, one per deployable unit (split: 3 for the groups,
    8 for the services) and one per module (8 in any mode). A dash is a layout that would repeat another for that mode."""
    c = clients(runs)
    rows, cats, series = [], [], ([], [], [])
    for m in MODE_ORDER:
        if m == "split-dbs":
            continue
        got = [find(runs, m, **{**DEFAULTS, "dbs": d}) for d in ("shared", "split", "per-module")]
        if not got[0]:
            continue
        base = got[0]["ordersPerSecond"]
        cells = [nice(r["ordersPerSecond"]) + (f" ({change(base, r['ordersPerSecond'])})" if r is not got[0] else "") if r else "-" for r in got]
        p50 = [f"{r['roundTripMs']['p50']:.1f}" if r else "-" for r in got]
        rows.append([f"{m}", *cells, *p50])
        cats.append(m)
        for i, r in enumerate(got):
            series[i].append((r or got[0])["ordersPerSecond"])
    (out / "dbs-table.md").write_text(md_table(["Mode", "orders/s shared", "split", "per-module", "p50 shared", "split", "per-module"], rows))
    (out / "dbs-chart.md").write_text(hbar("Database layout: throughput", f"orders per second, {c}; larger is better", cats,
                                           [("one instance", series[0]), ("one per deployable unit", series[1]), ("eight, one per module", series[2])], "orders/s"))


def matrix_fragments(runs, out):
    """The effect of each switch (the others at their defaults) and every combination, from one run of each."""
    c = clients(runs)
    for name, dim, before, after, labels, extra in (("cache", "cache", "off", "on", ("cache off", "cache on"), {}),
                                                    ("parallel", "parallel", "off", "on", ("sequential", "parallel"), {}),
                                                    ("async", "async", "outbox", "events", ("outbox", "events"), {})):
        modes = MODE_ORDER
        pairs = effect_rows(runs, dim, before, after, modes)
        (out / f"{name}-table.md").write_text(effect_table(pairs, before, after, name))
        title = {"cache": "Result cache", "parallel": "Parallel quotes", "async": "Follow-up delivery"}[name]
        (out / f"{name}-chart.md").write_text(hbar(f"{title}: throughput", f"orders per second, {c}; larger is better", [m for m, _, _ in pairs],
                                                   [(labels[0], [a["ordersPerSecond"] for _, a, _ in pairs]),
                                                    (labels[1], [b["ordersPerSecond"] for _, _, b in pairs])], "orders/s"))
    dbs_fragments(runs, out)
    on = {m: std(runs, m) for m in MODE_ORDER}
    methods = sorted({k for r in on.values() if r for k in r.get("cacheHitRate", {})})
    shown = [m for m in MODE_ORDER if on[m] and on[m].get("cacheHitRate")]
    rows = [[f"{k}", *[f"{on[m]['cacheHitRate'][k]['rate']:.0%}" if k in on[m]["cacheHitRate"] else "-" for m in shown]] for k in methods]
    (out / "cache-hits.md").write_text(md_table(["Cached method", *[f"{m}" for m in shown]], rows))
    # every combination: one row per (databases, cache, parallel quotes, follow-ups), one column per mode
    DBS = ("shared", "split", "per-module")
    for name, value in (("throughput", lambda r: nice(r["ordersPerSecond"])), ("p50", lambda r: f"{r['roundTripMs']['p50']:.1f}")):
        rows = []
        for dbs in DBS:
            for cache in ("on", "off"):
                for parallel in ("on", "off"):
                    for asynchronous in ("outbox", "events"):
                        cells = []
                        for m in MODE_ORDER:
                            r = find(runs, m, cache=cache, parallel=parallel, dbs=dbs, **{"async": asynchronous})
                            cells.append(value(r) if r else "-")
                        rows.append([dbs, cache, parallel, asynchronous, *cells])
        (out / f"matrix-{name}.md").write_text(md_table(["Databases", "Cache", "Parallel", "Follow-ups", *MODE_ORDER], rows, left=4))


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
        (out / "drain-chart.md").write_text(hbar("Time until every notification is delivered", "seconds after the last order was placed; smaller is better",
                                                 list(found), [("lag", [v["lag"] for v in found.values()])], "s", lambda v: f"{v:.1f}"))
    return found


def main():
    p = argparse.ArgumentParser()
    for k in ("modes", "matrix"):
        p.add_argument(f"--{k}", required=True)
    p.add_argument("--drain", required=True)
    p.add_argument("--out", default=str(ROOT / "docs" / "performance" / "_generated"))
    a = p.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    base = modes_fragments(load(a.modes), out)
    matrix_fragments(load(a.matrix), out)
    drain_fragments(a.drain, out)
    print("wrote", len(list(out.glob("*.md"))), "fragments to", out)


if __name__ == "__main__":
    main()
