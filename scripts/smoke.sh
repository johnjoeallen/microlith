#!/usr/bin/env bash
# Runs every scenario of section 16.5 against whichever process hosts orders, checks outcomes, exception types and the
# binding on every cross-module edge, and prints the merged call tree of one order. Exits non-zero on any difference.
#   scripts/smoke.sh            (a demo mode must be running: scripts/demo.sh <mode> up)
exec python3 "$(dirname "$0")/demo_tools.py" smoke "$@"
