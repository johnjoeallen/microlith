#!/usr/bin/env bash
# Prints the merged call tree of one correlation id, collected from /demo/trace of every running demo process.
#   scripts/trace.sh <correlationId>
[ $# -eq 1 ] || { echo "usage: $0 <correlationId>" >&2; exit 2; }
exec python3 "$(dirname "$0")/demo_tools.py" trace "$@"
