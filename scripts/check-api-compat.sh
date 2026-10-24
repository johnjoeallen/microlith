#!/usr/bin/env bash
# Proves the API compatibility check (japicmp) bites. The inventory-api jar of the current tree is installed as a
# throw-away baseline (groupId com.acme.macro.baseline, removed again on exit); throw-away copies of the repository are
# then changed in compatible and incompatible ways and compared with it.
#
# Real use needs no script: mvn verify -Dapi.compat.skip=false -Dapi.baseline.version=<last release>
set -u
root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
status=0
baseline_group=com.acme.macro.baseline
baseline_version=0.0.0-baseline
m2="${HOME}/.m2/repository/com/acme/macro/baseline"
trap 'rm -rf "$work" "$m2"' EXIT

(cd "$root" && ./mvnw -q -B -DskipTests package -pl modules/inventory/inventory-api -am) || { echo "build failed"; exit 1; }
jar="$(ls "$root"/modules/inventory/inventory-api/target/inventory-api-*.jar | grep -v -- '-sources\|-tests' | head -1)"
(cd "$root" && ./mvnw -q -B install:install-file -Dfile="$jar" -DgroupId="$baseline_group" -DartifactId=inventory-api \
  -Dversion="$baseline_version" -Dpackaging=jar -DgeneratePom=true) || { echo "cannot install the baseline"; exit 1; }

api=modules/inventory/inventory-api/src/main/java/com/acme/inventory/api
run() { # description expected(pass|fail) file old new   (new replaces old; file relative to the repo)
  local desc="$1" expected="$2" file="${3:-}" old="${4:-}" new="${5:-}"
  rm -rf "$work/repo" && mkdir "$work/repo"
  (cd "$root" && tar --exclude=target --exclude=.git -cf - .) | tar -xf - -C "$work/repo"
  if [ -n "$file" ]; then
    python3 -I - "$work/repo/$file" "$old" "$new" <<'PY'
import sys
p, old, new = sys.argv[1:]
if old == "@@NEW":
    open(p, "w").write(new)
else:
    s = open(p).read()
    assert old in s, f"marker not found in {p}"
    open(p, "w").write(s.replace(old, new, 1))
PY
  fi
  local out result
  if out="$(cd "$work/repo" && ./mvnw -q -B verify -DskipTests -pl modules/inventory/inventory-api -am \
      -Dapi.compat.skip=false -Dapi.baseline.groupId="$baseline_group" -Dapi.baseline.version="$baseline_version" 2>&1)"; then
    result=pass
  else
    result=fail
  fi
  if [ "$result" != "$expected" ]; then
    echo "FAIL: $desc -- expected $expected, got $result"; echo "$out" | tail -15; status=1
  elif [ "$result" = fail ] && ! grep -q "japicmp" <<<"$out"; then
    echo "FAIL: $desc -- failed, but not in japicmp"; echo "$out" | tail -15; status=1
  else
    echo "OK:   $desc -- $result as expected"
  fi
}

run "unchanged API"                              pass ""
run "method added to an @RpcApi interface"       pass "$api/InventoryApi.java" "    @Idempotent
    @Operation(summary = \"Gets a reservation\")" "    @Idempotent
    int count();

    @Idempotent
    @Operation(summary = \"Gets a reservation\")"
run "exception type added"                       pass "$api/InventoryClosedException.java" "@@NEW" "package com.acme.inventory.api;

public class InventoryClosedException extends RuntimeException {
}"
run "method removed"                             fail "$api/InventoryApi.java" "    @Idempotent
    @Operation(summary = \"Gets a reservation\")
    Reservation getReservation(String reservationId);" ""
run "parameter type changed"                     fail "$api/InventoryApi.java" "StockLevel getStock(String sku)" "StockLevel getStock(Integer sku)"
run "return type changed"                        fail "$api/InventoryApi.java" "Reservation getReservation(String reservationId)" "String getReservation(String reservationId)"
run "record component renamed"                   fail "$api/StockLevel.java" "int available" "int onHand"
run "record component added (constructor changes)" fail "$api/StockLevel.java" "String warehouse" "String warehouse, String bin"
run "exception type removed (no longer public)"  fail "$api/OutOfStockException.java" "@@NEW" "package com.acme.inventory.api;

class OutOfStockException extends RuntimeException {
}"
exit $status
