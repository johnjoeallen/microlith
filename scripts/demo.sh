#!/usr/bin/env bash
# Runs one demo mode on this machine. Gives every start clean, freshly initialised databases (scripts/db.sh reset) and builds the jars on first use.
#
#   scripts/demo.sh <mode> start [--async outbox|events] [--cache on|off] [--parallel on|off]
#   scripts/demo.sh <mode> stop|status
#
# options (start only; each overrides the environment variable named here, which still works):
#   --async outbox|events   how orders delivers its follow-ups: outbox relay (default) or Spring Modulith events   (MODULES_ORDERS_ASYNC)
#   --cache on|off          the @Cached result cache, default on                                                    (RPC_CACHE_ENABLED)
#   --parallel on|off       orders runs the customer check and the two quotes in parallel, default on               (MODULES_ORDERS_PARALLEL_QUOTES)
#
# modes: modulith  macro-monolith  groups  microservices  microservices-macro  loopback-codec  loopback-http  split-dbs
#
# 'start' wipes and re-creates PostgreSQL (schemas and roles from docs/db/bootstrap.sql; the app migrates and seeds on boot),
# so every run begins clean. 'stop' stops the processes and then PostgreSQL.
# Writes PID files and logs under .demo/ and waits until every process reports ready.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

mode="${1:-}"; action="${2:-}"
opt_async=""; opt_cache=""; opt_parallel=""
modules=(orders catalog pricing inventory customers payments shipping notifications)
declare -A port=([orders]=9181 [catalog]=9182 [pricing]=9183 [inventory]=9184 [customers]=9185 [payments]=9186 [shipping]=9187 [notifications]=9188)
java_opts="${DEMO_JAVA_OPTS:--Xmx320m -XX:+UseSerialGC -XX:TieredStopAtLevel=1}"

usage() {
  echo "usage: $0 <modulith|macro-monolith|groups|microservices|microservices-macro|loopback-codec|loopback-http|split-dbs> start [options] | stop | status" >&2
  echo "options (start only): --async outbox|events   --cache on|off   --parallel on|off" >&2
  exit 2
}
[ -n "$mode" ] && [ -n "$action" ] || usage

# options after the action
shift 2
while [ $# -gt 0 ]; do
  [ "$action" = start ] || { echo "options apply to 'start' only (got $1 with '$action')" >&2; usage; }
  [ $# -ge 2 ] || { echo "$1 needs a value" >&2; usage; }
  case "$1" in
    --async)    [ "$2" = outbox ] || [ "$2" = events ] || { echo "--async must be outbox or events, not '$2'" >&2; usage; }; opt_async="$2" ;;
    --cache)    [ "$2" = on ] || [ "$2" = off ] || { echo "--cache must be on or off, not '$2'" >&2; usage; }; opt_cache="$2" ;;
    --parallel) [ "$2" = on ] || [ "$2" = off ] || { echo "--parallel must be on or off, not '$2'" >&2; usage; }; opt_parallel="$2" ;;
    *) echo "unknown option '$1'" >&2; usage ;;
  esac
  shift 2
done
onoff() { [ "$1" = on ] && echo true || echo false; }
[ -z "$opt_async" ] || export MODULES_ORDERS_ASYNC="$opt_async"
[ -z "$opt_cache" ] || export RPC_CACHE_ENABLED="$(onoff "$opt_cache")"
[ -z "$opt_parallel" ] || export MODULES_ORDERS_PARALLEL_QUOTES="$(onoff "$opt_parallel")"
# the settings in effect, as shown by 'status' (options, else the environment, else the defaults)
settings() {
  printf 'async=%s cache=%s parallel=%s' "${MODULES_ORDERS_ASYNC:-outbox}" \
    "$([ "${RPC_CACHE_ENABLED:-true}" = false ] && echo off || echo on)" \
    "$([ "${MODULES_ORDERS_PARALLEL_QUOTES:-true}" = false ] && echo off || echo on)"
}

# the processes of a mode: "<name> <jar-app> <port> <profiles>"
plan() {
  local m g
  case "$mode" in
    modulith)       echo "modulith app-modulith 9181 demo" ;;
    macro-monolith) echo "macro app-macro 9181 monolith,demo" ;;
    loopback-codec) echo "macro app-macro 9181 loopback-codec,demo" ;;
    loopback-http)  echo "macro app-macro 9181 loopback-http,demo" ;;
    groups)         echo "front app-macro 9181 front,demo"; echo "fulfilment app-macro 9182 fulfilment,demo"; echo "accounts app-macro 9183 accounts,demo" ;;
    split-dbs)      echo "front app-macro 9181 front,split-dbs,demo"; echo "fulfilment app-macro 9182 fulfilment,split-dbs,demo"; echo "accounts app-macro 9183 accounts,split-dbs,demo" ;;
    microservices)       for m in "${modules[@]}"; do echo "$m app-$m-service ${port[$m]} demo"; done ;;
    microservices-macro) for m in "${modules[@]}"; do echo "$m app-macro ${port[$m]} $m,demo"; done ;;
    *) usage ;;
  esac
}

orders_port() { echo 9181; }

ready() { curl -fs "http://127.0.0.1:$1/actuator/health/readiness" 2>/dev/null | grep -q '"UP"'; }

start() {
  [ -e "$demo_dir/mode" ] && { echo "a demo is already running (mode $(cat "$demo_dir/mode")); run: $0 $(cat "$demo_dir/mode") stop" >&2; exit 1; }
  ensure_built
  "$root/scripts/db.sh" reset
  mkdir -p "$demo_dir/logs"; : > "$demo_dir/processes"
  echo "$mode" > "$demo_dir/mode"
  settings > "$demo_dir/settings"
  echo "http://127.0.0.1:$(orders_port)" > "$demo_dir/orders.url"
  local name app prt profiles
  while read -r name app prt profiles; do
    # shellcheck disable=SC2086
    nohup java $java_opts -jar "$(jar "$app")" --spring.profiles.active="$profiles" --server.port="$prt" --server.address=127.0.0.1 \
      > "$demo_dir/logs/$name.log" 2>&1 &
    echo "$name $prt $!" >> "$demo_dir/processes"
  done < <(plan)
  echo "started $(wc -l < "$demo_dir/processes") process(es) for mode '$mode'; waiting until they are ready ..."
  local deadline=$((SECONDS + ${DEMO_START_TIMEOUT:-240})) pid
  while read -r name prt pid; do
    until ready "$prt"; do
      kill -0 "$pid" 2>/dev/null || { echo "$name died during startup; last log lines:" >&2; tail -25 "$demo_dir/logs/$name.log" >&2; stop; exit 1; }
      [ $SECONDS -lt $deadline ] || { echo "$name was not ready in time (see $demo_dir/logs/$name.log)" >&2; stop; exit 1; }
      sleep 1
    done
  done < "$demo_dir/processes"
  echo "ready:"
  while read -r name prt pid; do echo "  $name   pid $pid   http://127.0.0.1:$prt/demo/topology"; done < "$demo_dir/processes"
  echo "orders entry point: $(cat "$demo_dir/orders.url")/rpc/orders"
}

stop() {
  [ -e "$demo_dir/processes" ] || { rm -rf "$demo_dir/mode" "$demo_dir/orders.url" "$demo_dir/settings"; echo "nothing to stop"; return 0; }
  local name prt pid
  while read -r name prt pid; do kill "$pid" 2>/dev/null || true; done < "$demo_dir/processes"
  for _ in $(seq 1 30); do
    local alive=0
    while read -r name prt pid; do kill -0 "$pid" 2>/dev/null && alive=1; done < "$demo_dir/processes"
    [ $alive -eq 0 ] && break; sleep 1
  done
  while read -r name prt pid; do kill -9 "$pid" 2>/dev/null || true; done < "$demo_dir/processes"
  rm -f "$demo_dir/processes" "$demo_dir/mode" "$demo_dir/orders.url" "$demo_dir/settings"
  echo "stopped"
}

stop_all() {
  stop
  $compose stop >/dev/null 2>&1 && echo "PostgreSQL stopped"
}

status() {
  [ -e "$demo_dir/mode" ] || { echo "no demo is running"; return 0; }
  echo "mode: $(cat "$demo_dir/mode")   ($(cat "$demo_dir/settings" 2>/dev/null || echo "settings unknown"))"
  local name prt pid state
  while read -r name prt pid; do
    if ! kill -0 "$pid" 2>/dev/null; then state="DEAD"; elif ready "$prt"; then state="ready"; else state="starting / not ready"; fi
    printf '  %-14s pid %-8s port %-5s %s\n' "$name" "$pid" "$prt" "$state"
  done < "$demo_dir/processes"
}

case "$action" in start) start ;; stop) stop_all ;; status) status ;; *) usage ;; esac
