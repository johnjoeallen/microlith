# microlith: the macro service

One Spring Boot 4 / Java 25 codebase that runs as a monolith or as several services, chosen by configuration. Modules talk to each other only
through `@RpcApi` interfaces: a direct call in the same process, JSON-RPC 2.0 when split. It ships with an order-fulfilment demo that runs in
**eight deployment shapes**, and a load driver that measures what each costs.

**Documentation: <https://johnjoeallen.github.io/microlith/>** (purpose, architecture, the eight modes, performance analysis, how to run it).
The source is in [`docs/`](docs/); preview it with `pip install -r requirements-docs.txt && mkdocs serve`.

## Quick start

Needs a JDK 25, Docker, `curl` and `python3` (Maven comes from `./mvnw`).

```bash
scripts/demo.sh modulith start     # or: groups, microservices, ... (any of the eight modes); starts PostgreSQL too
# then browse http://localhost:9181/shop  (also /swagger-ui and /demo/topology)
scripts/smoke.sh                   # the scenarios, checked in the running mode
scripts/perf.sh                    # a load run against it, with per-call latencies
scripts/demo.sh modulith stop
```

`./mvnw clean verify` builds and runs the tests, including the module-boundary rules.

## More

* [Developer guide](docs/README-dev.md) and [design](docs/design/macro-service.md)
* [Project reference](docs/reference.md): databases, domain, API documentation, layout, boundary rules

Licence: see [`LICENSE`](LICENSE).
