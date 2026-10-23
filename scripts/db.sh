#!/usr/bin/env bash
# PostgreSQL for the demo: three instances (A 5432, B 5433, C 5434) in Docker. Every instance gets the per-module
# schemas and roles from docs/db/bootstrap.sql the first time it starts (it is mounted into the init directory).
#   scripts/db.sh up        start (and wait until healthy)
#   scripts/db.sh down      stop and wipe the data
#   scripts/db.sh reset     down, then up: a clean database for every run
#   scripts/db.sh status
set -euo pipefail
source "$(dirname "$0")/_common.sh"

case "${1:-}" in
  up)
    $compose up -d --wait >/dev/null
    for svc in pg-a pg-b pg-c; do
      roles=$($compose exec -T "$svc" psql -U postgres -d macro -Atc "select count(*) from pg_roles where rolname like '%\_app'")
      [ "$roles" = "8" ] || { echo "$svc: expected 8 module roles from bootstrap.sql, found $roles" >&2; exit 1; }
    done
    echo "PostgreSQL ready: A localhost:5432, B localhost:5433, C localhost:5434 (database 'macro', 8 module schemas' roles)" ;;
  down)   $compose down -v >/dev/null 2>&1 && echo "PostgreSQL stopped and wiped" ;;
  reset)  "$0" down; "$0" up ;;
  status) $compose ps ;;
  *) echo "usage: $0 up|down|reset|status" >&2; exit 2 ;;
esac
