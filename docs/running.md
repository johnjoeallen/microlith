# Running it

## Requirements

JDK 25, Docker (PostgreSQL runs in containers), `curl` and `python3`. Maven comes from `./mvnw`.

## Start a mode

```bash
scripts/demo.sh modulith start        # any mode: modulith macro-modulith groups split-dbs microservices microservices-macro loopback-codec loopback-http
scripts/demo.sh modulith status
scripts/demo.sh modulith stop         # stops the processes, then PostgreSQL
```

Four switches can be set when starting (they are ordinary properties, so environment variables and config files work too):

| Option | Values | What it does |
| --- | --- | --- |
| `--async` | `outbox` (default), `events` | how orders delivers its follow-ups: the [outbox relay or Spring Modulith events](features/follow-ups.md) |
| `--cache` | `on` (default), `off` | the [`@Cached` result cache](features/cache.md) |
| `--parallel` | `on` (default), `off` | the [parallel customer check and quotes](features/parallel.md) |
| `--dbs` | `shared` (default), `split` | `shared`: one PostgreSQL instance with a schema per module. `split`: every deployable unit gets its own instance, so the layout follows the mode: `groups` 3 (A 5432 for `front`, B 5433 for `fulfilment`, C 5434 for `accounts`), `microservices` and `microservices-macro` 8 (one per module, ports 5432 to 5439). A one-process mode is one unit, so it stays on one instance. `split-dbs` is `groups --dbs split` |

```bash
scripts/demo.sh groups start --async events --cache off --parallel off
scripts/demo.sh groups status         # mode: groups   (async=events cache=off parallel=off wire=json dbs=shared)
```

`start` resets PostgreSQL to a clean, freshly initialised state every time (schemas and roles from `docs/db/bootstrap.sql`, migrations and demo data
when the app boots), builds the jars on first use, starts the mode and waits until every process is ready. Only one mode runs at a time.
Logs and PID files are under `.demo/`.

=== "Look around"

    | URL | What |
    | --- | --- |
    | <http://localhost:9181/shop> | the demo shop |
    | <http://localhost:9181/swagger-ui> | generated API documentation |
    | <http://localhost:9181/demo/topology> | modules, bindings and datasources of this process |
    | <http://localhost:9181/actuator/health> | health |

=== "Place an order"

    ```bash
    curl -s -X POST localhost:9181/rpc/orders -H 'Content-Type: application/json' -d '{
      "jsonrpc":"2.0","method":"orders.placeOrder","id":1,
      "params":{"requestId":"r-1","customerId":"cust-standard",
                "lines":[{"sku":"sku-widget","quantity":1}]}}'
    ```

    Demo data: customers `cust-standard`, `cust-gold`, `cust-declined`; SKUs `sku-widget`, `sku-gadget`, `sku-oos` (no stock),
    `sku-undeliverable`.

=== "Trace it"

    ```bash
    scripts/trace.sh <correlationId>       # the call tree of one order across every process
    scripts/smoke.sh                       # seven scenarios, results and bindings asserted
    scripts/demo-all.sh                    # every mode on a clean database, with a summary table
    ```

## Measure it

```bash
# a mode that is already running
scripts/perf.sh --orders 2000 --warmup 200 --concurrency 8

# start, measure and stop modes, with the result cache off and on, and compare
scripts/perf.sh --modes groups,microservices --cache both --duration 120 --warmup 500 --concurrency 8

# every switch: cache, parallel quotes, follow-up delivery
scripts/perf.sh --modes modulith,groups,microservices --cache both --parallel both --async both

# one shared PostgreSQL instance against one instance per deployable unit (both = shared and split)
scripts/perf.sh --modes groups,microservices --dbs both
```

Each run writes a timestamped markdown report with the modes side by side to `.demo/perf/<timestamp>/`. How the numbers are produced is in
[Method](performance/method.md).

## Build and test

```bash
./mvnw clean verify          # everything, including boundary rules and the per-binding contract tests
```

The suite includes tests that start all eight services against real PostgreSQL containers, cut connections mid-call, and restart a process with
work pending.

## Build this documentation

```bash
pip install -r requirements-docs.txt
mkdocs serve                                  # preview at http://127.0.0.1:8000
mike deploy --push --update-aliases 0.1 latest      # publish a version to the gh-pages branch
mike set-default --push latest
```

`mike` keeps several versions on the `gh-pages` branch. That branch is its output: change the documents under `docs/`, never the built pages.
Enable GitHub Pages for the repository with the source set to the `gh-pages` branch.
