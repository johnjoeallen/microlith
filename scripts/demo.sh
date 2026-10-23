#!/usr/bin/env bash
# Runs one demo mode on this machine. Needs the databases (scripts/db.sh up) and the built jars (built on first use).
#
#   scripts/demo.sh <mode> up|down|status
#
# modes: modulith  macro-monolith  groups  microservices  microservices-macro  loopback-codec  loopback-http  split-dbs
#
# Writes PID files and logs under .demo/ and waits until every process reports ready.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

mode="${1:-}"; action="${2:-}"
modules=(orders catalog pricing inventory customers payments shipping notifications)
declare -A port=([orders]=8081 [catalog]=8082 [pricing]=8083 [inventory]=8084 [customers]=8085 [payments]=8086 [shipping]=8087 [notifications]=8088)
java_opts="${DEMO_JAVA_OPTS:--Xmx320m -XX:+UseSerialGC -XX:TieredStopAtLevel=1}"

usage() { echo "usage: $0 <modulith|macro-monolith|groups|microservices|microservices-macro|loopback-codec|loopback-http|split-dbs> up|down|status" >&2; exit 2; }
[ -n "$mode" ] && [ -n "$action" ] || usage

# the processes of a mode: "<name> <jar-app> <port> <profiles>"
plan() {
  local m g
  case "$mode" in
    modulith)       echo "modulith app-modulith 8080 demo" ;;
    macro-monolith) echo "macro app-macro 8080 monolith,demo" ;;
    loopback-codec) echo "macro app-macro 8080 loopback-codec,demo" ;;
    loopback-http)  echo "macro app-macro 8080 loopback-http,demo" ;;
    groups)         echo "front app-macro 8081 front,demo"; echo "fulfilment app-macro 8082 fulfilment,demo"; echo "accounts app-macro 8083 accounts,demo" ;;
    split-dbs)      echo "front app-macro 8081 front,split-dbs,demo"; echo "fulfilment app-macro 8082 fulfilment,split-dbs,demo"; echo "accounts app-macro 8083 accounts,split-dbs,demo" ;;
    microservices)       for m in "${modules[@]}"; do echo "$m app-$m-service ${port[$m]} demo"; done ;;
    microservices-macro) for m in "${modules[@]}"; do echo "$m app-macro ${port[$m]} $m,demo"; done ;;
    *) usage ;;
  esac
}

orders_port() { case "$mode" in modulith|macro-monolith|loopback-codec|loopback-http) echo 8080 ;; *) echo 8081 ;; esac; }

ready() { curl -fs "http://127.0.0.1:$1/actuator/health/readiness" 2>/dev/null | grep -q '"UP"'; }

up() {
  [ -e "$demo_dir/mode" ] && { echo "a demo is already running (mode $(cat "$demo_dir/mode")); run: $0 $(cat "$demo_dir/mode") down" >&2; exit 1; }
  ensure_built
  mkdir -p "$demo_dir/logs"; : > "$demo_dir/processes"
  echo "$mode" > "$demo_dir/mode"
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
      kill -0 "$pid" 2>/dev/null || { echo "$name died during startup; last log lines:" >&2; tail -25 "$demo_dir/logs/$name.log" >&2; down; exit 1; }
      [ $SECONDS -lt $deadline ] || { echo "$name was not ready in time (see $demo_dir/logs/$name.log)" >&2; down; exit 1; }
      sleep 1
    done
  done < "$demo_dir/processes"
  echo "ready:"
  while read -r name prt pid; do echo "  $name   pid $pid   http://127.0.0.1:$prt/demo/topology"; done < "$demo_dir/processes"
  echo "orders entry point: $(cat "$demo_dir/orders.url")/rpc/orders"
}

down() {
  [ -e "$demo_dir/processes" ] || { rm -rf "$demo_dir/mode" "$demo_dir/orders.url"; echo "nothing to stop"; return 0; }
  local name prt pid
  while read -r name prt pid; do kill "$pid" 2>/dev/null || true; done < "$demo_dir/processes"
  for _ in $(seq 1 30); do
    local alive=0
    while read -r name prt pid; do kill -0 "$pid" 2>/dev/null && alive=1; done < "$demo_dir/processes"
    [ $alive -eq 0 ] && break; sleep 1
  done
  while read -r name prt pid; do kill -9 "$pid" 2>/dev/null || true; done < "$demo_dir/processes"
  rm -f "$demo_dir/processes" "$demo_dir/mode" "$demo_dir/orders.url"
  echo "stopped"
}

status() {
  [ -e "$demo_dir/mode" ] || { echo "no demo is running"; return 0; }
  echo "mode: $(cat "$demo_dir/mode")"
  local name prt pid state
  while read -r name prt pid; do
    if ! kill -0 "$pid" 2>/dev/null; then state="DEAD"; elif ready "$prt"; then state="ready"; else state="starting / not ready"; fi
    printf '  %-14s pid %-8s port %-5s %s\n' "$name" "$pid" "$prt" "$state"
  done < "$demo_dir/processes"
}

case "$action" in up) up ;; down) down ;; status) status ;; *) usage ;; esac
