# microlith: the macro service

**A macro service: one team's modular Spring Boot service, with module boundaries the build enforces, deployable as one unit or as several by configuration, and a way to measure what each choice costs.**

Most systems pick their shape early: a monolith that is hard to split later, or microservices with their full operational cost from day one.
microlith keeps that decision open. It is not a "big monolith": a macro service is one team's bounded context, split into enforced modules, and a company is
many of them connected by published contracts ([principles](principles.md#1-what-a-macro-service-is-and-is-not)). The code is organised as modules with strict boundaries; whether two modules share a process or talk over
the network is a **deployment setting**, not a code change. It ships with a small order-fulfilment domain and the scripts to run it in
**eight shapes** on one machine, so the trade-offs can be seen and measured instead of argued about.

## What it demonstrates

<div class="grid cards" markdown>

-   **One codebase, many shapes**

    The same modules run as a single process, as three groups, or as eight services. The module code and the API interfaces never change.
    [:octicons-arrow-right-24: The eight modes](modes/index.md)

-   **A single way for modules to talk**

    A module reaches another only through its `@RpcApi` interface: a direct call in the same process, JSON-RPC 2.0 when split. No shared tables,
    no shared state, no hidden channels. [:octicons-arrow-right-24: Architecture](architecture.md)

-   **Boundaries enforced by the build**

    Maven rules, ArchUnit and Spring Modulith verification fail the build when a module reaches into another. Each module owns a database
    schema, with separate migration and runtime roles.

-   **Real measurements**

    A load driver reports client round trips and the latency of *every* module-to-module call, per mode, with and without a result cache,
    parallel calls and two follow-up delivery mechanisms. [:octicons-arrow-right-24: Performance](performance/index.md)

</div>

## The idea in one picture

=== "In one process"

    ```mermaid
    graph LR
      subgraph jvm["one process"]
        O[orders] -->|"direct method call"| P[pricing]
      end
    ```

=== "Split across processes"

    ```mermaid
    graph LR
      subgraph a["process A"]
        O[orders]
      end
      subgraph b["process B"]
        P[pricing]
      end
      O -->|"JSON-RPC 2.0 over HTTP"| P
    ```

The call `pricing.quote(...)` reads the same in both cases. Setting `modules.pricing.enabled=false` and `modules.pricing.url=…` in the orders process
turns the method call into an HTTP request; nothing in the module code changes. It is a setting per module, so any grouping is possible.

## The demo domain

An order-fulfilment system of eight modules (`catalog`, `customers`, `pricing`, `inventory`, `payments`, `shipping`, `notifications`, `orders`).
`orders.placeOrder` is a saga with compensation: it validates, prices, reserves stock, authorizes payment, ships and captures; a declined
payment releases the reservation, a shipping failure voids the payment and releases the reservation, and the confirmation message goes out
through a durable follow-up so that a notifications outage never fails the order. A one-line order makes 17 module-to-module calls (about 20 on average in the benchmark, which uses 1 to 4 lines), with
fan-out, three-deep chains and diamonds, which makes it a useful stand-in for real inter-service chatter.

## Where to go next

| If you want to... | Read |
| --- | --- |
| Understand how it is built | [Architecture](architecture.md) |
| See the argument for not starting with microservices | [The case](case.md) |
| Decide how to start, where to draw boundaries and what to deploy | [Principles](principles.md) |
| See what each of the eight shapes is, and when you would pick it | [Deployment modes](modes/index.md) |
| See what the shapes cost | [Performance summary](performance/index.md) |
| Run it yourself | [Running it](running.md) |
| Look up a term | [Glossary](glossary.md) |
| Read the original design | [Design document](design/macro-service.md) |

!!! info "Which code this site describes"
    The site documents the branch it was built from. The core (eight modes, the `@RpcApi` model, persistence, the saga, the load driver and the
    `@Cached` result cache) is on `main`. Parallel quotes and the switchable follow-up delivery (outbox or Spring Modulith events) were developed on
    the branches `discuss/spring-modulith-async` and `spike/spring-modulith-events` and are combined in `spike/switchable-async`; the
    [feature pages](features/follow-ups.md) say which is which.

!!! note "Stack"
    Java 25, Spring Boot 4.1, Spring Modulith 2.1, PostgreSQL, Maven. Everything runs on localhost with PostgreSQL in Docker.
