# Spring Modulith events instead of the hand-built outbox

Branch `spike/spring-modulith-events`. The orders module's outbox and relay are replaced by Spring Modulith 2.1.2's own model:
domain events, the **event publication registry**, and asynchronous transactional listeners. Everything else (the synchronous saga,
`@RpcApi` calls, per-module schemas and roles) is unchanged.

## What was built

- `OrderEvents`: three events (`ReleaseReservation`, `VoidPayment`, `OrderConfirmed`) published by `OrderStore` inside the transaction
  that changes the order, and a `Listeners` component that calls the other module through its `@RpcApi`. Because it goes through
  `@RpcApi` it works in every binding, including split processes.
- Modulith's registry stores each publication in `orders.event_publication` in that same transaction, marks it
  PUBLISHED / PROCESSING / COMPLETED / FAILED / RESUBMITTED, and `PersistentApplicationEventMulticaster` runs the listener after commit.
- `OrdersEventListener`: `@Async` + `@TransactionalEventListener`, i.e. Modulith's `@ApplicationModuleListener` without its
  `@Transactional(REQUIRES_NEW)` (see "friction").
- `OrderEventRedelivery`: decides **when** a failed publication is resubmitted (exponential backoff, max attempts).
- `OrdersEventPublicationRepository`: the event store over the orders pool (see "friction").
- Flyway `V2__event_publication.sql` drops `outbox` and creates Modulith's schema v2 table in the orders schema.
- Removed: `OutboxRelay`, `OutboxRepository`, `OutboxEntryEntity`, `OutboxSignal`, `OutboxCommand`, the outbox table.

## Measured

Realistic profile, 2,000 orders, 4 clients, cache on, same flags on `main` (outbox) and on this branch:

| Mode | Orders/s outbox / events | p50 ms outbox / events | p99 ms outbox / events |
| --- | --- | --- | --- |
| `modulith` | 351 / 333 | 11.1 / 11.7 | 18.1 / 20.6 |
| `groups` | 210 / 213 | 18.4 / 18.3 | 31.2 / 30.6 |
| `microservices` | 156 / 154 | 24.7 / 24.7 | 42.7 / 47.5 |

The request path is the same, so the round trip is within noise (modulith about 3-5% slower on this run; one run each, not repeated).

**Delivery of the asynchronous work is very different.** 1,000 orders placed flat out, then the time until every notification
reached `notifications.message`: events **0.1 s** after the last order, outbox (`main`) **48 s**. The old relay claims 20 entries per
one-second poll (about 20 per second); that is a tuning limit of our relay, not of the outbox pattern, but the registry plus an async
executor needs no such tuning.

## Friction (what it cost to fit Modulith's model to this project)

1. **One transaction manager per module.** The registry's own methods are `@Transactional` with no qualifier, so with eight
   transaction managers the context fails. `ordersTm` is now `@Primary`. That is correct only because orders is the one module that
   publishes events; a second publishing module would need a second registry (Modulith assumes one).
2. **Modulith's JDBC repository does not fit.** It is package-private, wants a single primary `DataSource` and opens a connection at
   start-up to detect the database. Our pools never connect at start-up (an unreachable database must not stop the process; a test
   enforces it). So `OrdersEventPublicationRepository` reimplements the repository (same table, same statements, completion mode UPDATE only)
   over the orders pool.
3. **`@ApplicationModuleListener` cannot name a transaction manager**, and our listeners only call other modules, so holding an orders
   connection open while they call would waste the pool. We use the same annotation without the `@Transactional`.
4. **Every process that contains orders-impl gets Modulith's event auto-configuration,** which refuses to start without a repository.
   `EventPublicationStandIn` (module-support) provides a stand-in that fails loudly when orders is not hosted. (A first attempt that
   excluded the auto-configuration from an `EnvironmentPostProcessor` broke on test property timing, and one placed in the orders package
   was filtered out by `@ApplicationModuleTest`.)
5. **The retry policy is ours.** The registry stores, marks failures and can resubmit, but it has no backoff, no maximum attempts and no
   schedule. `OrderEventRedelivery` supplies them. To keep "an idle module holds no database connection" it scans only after a failure, shortly
   after start (crash recovery) and on a slow idle scan; Modulith's staleness monitor and republish-on-restart were not used because they query
   the database on a timer or at start-up.
6. **Multi-instance:** `markResubmitted` is a compare-and-set claim, so two instances do not resubmit the same publication, but redelivery to
   a listener whose first attempt is still running is possible; targets are idempotent by request id, as before.
7. **Noise:** every failed delivery logs an ERROR with a stack trace from Spring's async exception handler.
8. **Events do not cross processes by themselves.** Here that does not matter because the listeners call `@RpcApi`; moving the
   notification into the notifications module as a listener (so orders no longer depends on it) would need externalization to a broker.

## Verdict

Equal on the critical path, far better at draining asynchronous work, and the saga stays explicit. Costs: a custom repository, a custom redelivery
policy, a primary transaction manager and a stand-in bean, i.e. Modulith's registry is usable here but only after adapting it to the
one-schema-per-module design. If a second module needs events, the primary-transaction-manager approach stops working.
