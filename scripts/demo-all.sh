#!/usr/bin/env bash
# Every demo mode in turn on a clean database: up, smoke, down. Prints a summary table and exits non-zero if any mode failed.
#   scripts/demo-all.sh [mode ...]
set -uo pipefail
source "$(dirname "$0")/_common.sh"

modes=("$@"); [ ${#modes[@]} -gt 0 ] || modes=(modulith macro-monolith groups microservices microservices-macro loopback-codec loopback-http split-dbs)
ensure_built
mkdir -p "$demo_dir"
rows=(); failed=0
for mode in "${modes[@]}"; do
  echo; echo "================ $mode ================"
  result="PASS"; bindings="-"; processes="-"
  if "$root/scripts/demo.sh" "$mode" start; then
    processes=$(wc -l < "$demo_dir/processes" | tr -d ' ')
    rm -f "$demo_dir/last-bindings"
    if "$root/scripts/smoke.sh"; then :; else result="FAIL"; fi
    [ -f "$demo_dir/last-bindings" ] && bindings=$(cat "$demo_dir/last-bindings")
  else
    result="FAIL (did not start)"
  fi
  "$root/scripts/demo.sh" "$mode" stop >/dev/null
  [ "$result" = "PASS" ] || failed=1
  rows+=("$(printf '%-20s %-9s %-34s %s' "$mode" "$processes" "$bindings" "$result")")
done
"$root/scripts/db.sh" down >/dev/null
echo; echo "================ summary ================"
printf '%-20s %-9s %-34s %s\n' mode processes "bindings seen (from traces)" result
for row in "${rows[@]}"; do echo "$row"; done
exit $failed
