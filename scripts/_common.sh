# Shared by the demo scripts (sourced, not run).
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
demo_dir="$root/.demo"
compose="docker compose -f $root/docker-compose.demo.yml"
compose_all="$compose --profile per-module"      # also the five extra instances of --dbs per-module
version="0.1.0-SNAPSHOT"

jar() { echo "$root/apps/$1/target/$1-$version.jar"; }

ensure_built() {
  local missing=0 app
  for app in app-modulith app-macro app-catalog-service app-customers-service app-pricing-service app-inventory-service \
             app-payments-service app-shipping-service app-notifications-service app-orders-service; do
    [ -f "$(jar "$app")" ] || missing=1
  done
  if [ $missing -eq 1 ]; then
    echo "building the application jars (first run only) ..."
    (cd "$root" && ./mvnw -q -DskipTests package)
  fi
}
