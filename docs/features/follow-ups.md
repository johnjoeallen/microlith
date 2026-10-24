# Follow-up delivery: outbox or Spring Modulith events

After an order changes state, `orders` still has to tell other modules something: release a reservation, void a payment, confirm to the customer.
These are **follow-ups**. They cannot be done atomically with the database commit (they are calls to another module), and they must not be lost.

!!! abstract "The pattern"
    1. In the **same transaction** as the order change, store a record that says "this follow-up must happen".
    2. After the commit, **deliver** it by calling the other module through its `@RpcApi`.
    3. If delivery fails, **retry** until it succeeds. Delivery is at-least-once, so the targets are idempotent (keyed by request id).

Because delivery goes through `@RpcApi`, it works in every [mode](../modes/index.md). A notifications outage never fails the order.

## Two implementations, one switch

`modules.orders.async` picks the mechanism (`OrderStore` publishes through a small `FollowUps` port):

| | `outbox` (default) | `events` |
| --- | --- | --- |
| Record | a row in the module's `outbox` table | a Spring Modulith event in `orders.event_publication` |
| Delivery | `OutboxRelay` polls the table and calls the target | an asynchronous listener runs right after the commit |
| Retry | exponential backoff, 30 attempts, built in | exponential backoff and maximum attempts supplied by `OrderEventRedelivery` (the registry has none) |
| Code we own | the relay, repository, entity, signal (about 340 lines) | a repository adapter, the redelivery policy, a stand-in bean (about 440 lines) |
| Delivery speed | 20 rows per second (one batch of 20 per one-second poll) | immediately |
| Idle cost | none (scans only when woken, with a slow idle scan) | none (scans only after a failure, shortly after start, and on a slow idle scan) |

```bash
MODULES_ORDERS_ASYNC=events scripts/demo.sh modulith start         # or: modules.orders.async=events
scripts/perf.sh --modes modulith,groups --async both
```

An unknown value fails at start-up with a clear message. Both tables always exist; only the selected mechanism's components are created.

## What it took to fit Spring Modulith's registry to one-schema-per-module

Spring Modulith's event publication registry is the library version of the outbox. Fitting it to a design where **every module has its own pool,
transaction manager and database roles** needed adapting:

* The registry's own methods are `@Transactional` without naming a manager, and there are eight. `ordersTm` is `@Primary`. That works only because
  orders is the one module that publishes events.
* Modulith's JDBC repository is package-private, wants a single `DataSource` and opens a connection at start-up. Module pools must not connect at
  start-up, so `OrdersEventPublicationRepository` implements the same table and statements over the orders pool.
* `@ApplicationModuleListener` includes `@Transactional(REQUIRES_NEW)`, which cannot name a manager and would hold an orders connection during the
  remote call; the listeners use `@Async` + `@TransactionalEventListener` instead.
* Every process that contains the orders jar gets Modulith's event auto-configuration, which refuses to start without a repository; a stand-in
  bean covers processes that do not host orders.
* Retry timing is not part of the library.

The full list, with the reasoning, is in the [spike write-up](../design/spring-modulith-events.md).

## Which is better?

On the request path they are indistinguishable. The difference is how fast the follow-ups drain under load, which is
[measured here](../performance/load.md#delivery-of-follow-ups): events deliver immediately, the outbox at the speed of its relay. That speed is a tuning limit
of this relay (a larger batch would close most of the gap), not a limit of the pattern.

Where it lives: the outbox is on `main`; the events variant is on `spike/spring-modulith-events`; the switch is on `spike/switchable-async`.
