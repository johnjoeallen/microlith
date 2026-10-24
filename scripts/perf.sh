#!/usr/bin/env bash
# Drives orders against the running demo mode and reports client round trips plus the latency of every
# module-to-module call (from the /demo/trace records of every process).
#   scripts/perf.sh [--orders N | --duration S] [--concurrency C] [--warmup W] [--settle S] [--drain S] [--modes M,M|all] [--no-trace] [--no-stock] [--out DIR]
#   --modes groups,microservices | all   start, measure and stop each mode in turn on a clean database, then compare them
#   without --modes a demo mode must already be running: scripts/demo.sh <mode> start
# Raw samples (orders.csv, calls.csv) and summary.json are written under .demo/perf/<timestamp>/.
export PYTHONDONTWRITEBYTECODE=1   # scripts/__pycache__ is tracked; do not dirty it
exec python3 "$(dirname "$0")/perf_tools.py" "$@"
