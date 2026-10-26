#!/usr/bin/env bash
# Repeats the mode comparison for every heap size x collector, interleaved (repetition-major) so that drift on the machine hits every config alike.
#   scripts/jvm_matrix.sh [--reps N] [--out FILE]      FILE gets one line per run: <config> <rep> <perf stamp>
# Summarise with scripts/jvm_report.py --matrix FILE.
set -euo pipefail
cd "$(dirname "$0")/.."
export PYTHONDONTWRITEBYTECODE=1
reps=5; out=.demo/jvm-matrix.txt
while [ $# -gt 0 ]; do case "$1" in --reps) reps="$2"; shift 2;; --out) out="$2"; shift 2;; *) echo "unknown $1" >&2; exit 2;; esac; done
mkdir -p "$(dirname "$out")"; : > "$out"
for rep in $(seq 1 "$reps"); do
  for heap in 1g 2g; do
    for gc in Serial Parallel; do
      cfg="$heap-$gc"
      DEMO_JAVA_OPTS="-Xmx$heap -XX:+Use${gc}GC" scripts/perf.sh --modes all --cache on --orders 1500 --warmup 200 --concurrency 16 > ".demo/jvm-matrix-$cfg-$rep.log" 2>&1 || echo "run failed: $cfg $rep" >&2
      stamp=$(grep -o 'perf/[0-9TZ]*' ".demo/jvm-matrix-$cfg-$rep.log" | tail -1 | cut -d/ -f2)
      echo "$cfg $rep $stamp" >> "$out"
    done
  done
done
