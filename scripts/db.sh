#!/usr/bin/env bash
# PostgreSQL for the demo: three instances (A 5432, B 5433, C 5434) in Docker, or eight (D to H, 5435 to 5439) with 'per-module'. Every instance gets the per-module
# schemas and roles from docs/db/bootstrap.sql the first time it starts (it is mounted into the init directory).
#   scripts/db.sh up [per-module]      start (and wait until healthy); per-module also starts D to H, one instance per module
#   scripts/db.sh down      stop and wipe the data (all eight instances)
#   scripts/db.sh reset [per-module]   down, then up: a clean database for every run
#   scripts/db.sh status
set -euo pipefail
source "$(dirname "$0")/_common.sh"

case "${1:-}" in
  up)
    services="pg-a pg-b pg-c"; up="$compose"
    if [ "${2:-}" = per-module ]; then services="pg-a pg-b pg-c pg-d pg-e pg-f pg-g pg-h"; up="$compose_all"; fi
    $up up -d --wait >/dev/null
    for svc in $services; do
      roles=$($up exec -T "$svc" psql -U postgres -d macro -Atc "select count(*) from pg_roles where rolname like '%\_app'")
      [ "$roles" = "8" ] || { echo "$svc: expected 8 module roles from bootstrap.sql, found $roles" >&2; exit 1; }
    done
    echo "PostgreSQL ready: ${services// /, } (A 5432, B 5433, C 5434, D to H 5435 to 5439; database 'macro', schemas and roles of all 8 modules on each)" ;;
  down)   $compose_all down -v >/dev/null 2>&1 && echo "PostgreSQL stopped and wiped" ;;
  reset)  "$0" down; "$0" up "${2:-}" ;;
  status) $compose_all ps ;;
  *) echo "usage: $0 up|down|reset|status [per-module]" >&2; exit 2 ;;
esac
