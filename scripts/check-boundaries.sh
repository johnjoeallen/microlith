#!/usr/bin/env bash
# Proves the Maven boundary rules bite: injects forbidden dependencies into a throw-away copy of the
# repository and expects the build to fail with a banned-dependency message.
set -u
root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
status=0

expect_failure() { # description pom-relative-path old new
  local desc="$1" pom="$2" old="$3" new="$4"
  rm -rf "$work/repo" && mkdir "$work/repo"
  (cd "$root" && tar --exclude=target --exclude=.git -cf - .) | tar -xf - -C "$work/repo"
  python3 -I - "$work/repo/$pom" "$old" "$new" <<'PY'
import sys
p, old, new = sys.argv[1:]
s = open(p).read()
assert old in s, f"marker not found in {p}"
open(p, "w").write(s.replace(old, old + new, 1))
PY
  local out
  if out="$(cd "$work/repo" && mvn -q -B verify -DskipTests 2>&1)"; then
    echo "FAIL: $desc -- build unexpectedly passed"; status=1
  elif grep -qE "Found Banned Dependency|may depend only on|must not depend on|may contain only" <<<"$out"; then
    echo "OK:   $desc -- build failed as expected"
    grep -E "Found Banned Dependency|may depend only on|must not depend on|may contain only" <<<"$out" | head -2 | sed 's/^/        /'
  else
    echo "FAIL: $desc -- build failed for another reason"; echo "$out" | tail -15; status=1
  fi
}

mod='<dependency><groupId>com.acme.macro</groupId><artifactId>module-support</artifactId></dependency>'
expect_failure "orders-impl -> inventory-impl" modules/orders/orders-impl/pom.xml "$mod" \
  '<dependency><groupId>com.acme.macro</groupId><artifactId>inventory-impl</artifactId></dependency>'
expect_failure "inventory-api -> module-support (compile)" modules/inventory/inventory-api/pom.xml \
  '<artifactId>rpc-api</artifactId></dependency>' \
  '<dependency><groupId>com.acme.macro</groupId><artifactId>module-support</artifactId></dependency>'
expect_failure "app-inventory-service -> orders-impl" apps/app-inventory-service/pom.xml \
  '<artifactId>inventory-impl</artifactId></dependency>' \
  '<dependency><groupId>com.acme.macro</groupId><artifactId>orders-impl</artifactId></dependency>'
exit $status
