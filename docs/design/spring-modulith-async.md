# Discussion: Spring Modulith async calls and events

Status: for discussion, nothing is decided. Branch `discuss/spring-modulith-async`.

## Where we are

- Modules talk only through `@RpcApi` interfaces: a direct call in-process, JSON-RPC when split. Request/reply, one transport model.
- The order saga (`OrdersService`) is explicit and synchronous: reserve, authorize, ship, capture. There is **no cross-module event**
  and no cross-module transaction, by design.
- The only asynchronous work is hand-built: compensations and the confirmation notification are written to an `outbox` table in the
  order's transaction, and `OutboxRelay` delivers them later by calling the target module's `@RpcApi` (idempotent by request id,
  retried with backoff). Because it goes through `@RpcApi`, it works unchanged in every binding.
- Spring Modulith 2.1.2 is already in the BOM (`spring-modulith-api` in `module-support`), used today for module structure and docs, not for events.

## What Spring Modulith offers

- `@ApplicationModuleListener`: an `@Async`, transactional (`REQUIRES_NEW`) event listener; the module-to-module event idiom.
- **Event publication registry** (JDBC): publications are stored in the publisher's transaction and marked complete when the listener
  finishes; incomplete ones are re-submitted (on restart, or on demand). This is the outbox pattern as a library.
- **Event externalization** (`@Externalized`): the same event is also sent to Kafka, AMQP, JMS and so on, which is how events cross processes.
- Integration-test support for modules (`Scenario`), and verification that listeners respect module boundaries.

## What "async" could mean here (they are different things)

1. **Replace our outbox with the event publication registry.** Same guarantee, less code. `OutboxRelay` is a candidate.
2. **Events instead of calls for side effects.** `OrderConfirmed` published by orders; notifications (and later others) listen.
   Orders would no longer depend on `NotificationsApi`.
3. **Parallel or non-blocking request/reply** (futures, virtual threads) inside the saga. This is *not* a Modulith feature, but it is
   where the latency is: see below.
4. **Full choreography** (each step reacts to the previous step's event). Removes the orchestrator.

## What the measurements say

From `scripts/perf.sh` (realistic profile, `microservices`, cache off): p50 round trip about 65 ms. The serial calls the saga makes
average: `shipping.quote` 5.6 ms, `createShipment` 4.2, `pricing.quote` 4.0, `payments.authorize` 2.7, `inventory.reserve` 2.1,
`payments.capture` 1.4, `customers.getCustomer` 0.9. The notification (about 3 ms) is already off the request path.

- Option 2 (and 1) therefore **do not shorten the critical path**; they change structure and reliability, not latency.
- Option 3 could: `pricing.quote`, `shipping.quote` and `getCustomer` are independent and could run in parallel.
- `@Cached` already removes some read latency (+20-30% orders/s in the split modes).

## Tensions with the current design

- **Process boundaries.** Modulith events are in-JVM. In `groups` / `microservices` an event crosses processes only via externalization
  (a broker) or by delivering it as an `@RpcApi` call, which is what the outbox does today. A broker is a new moving part for a demo that
  today needs only PostgreSQL.
- **One database schema per module with separate migrator and runtime roles.** The JDBC registry wants an `event_publication` table;
  here it would have to live in each publishing module's schema, with grants for both roles, and use that module's `DataSource` and
  transaction manager (we have one `EntityManagerFactory` and transaction manager per module, not a shared one).
- **Explicit ordering and compensation.** The saga's state machine is readable and testable; choreography spreads it across listeners.
- **The "no cross-module event" rule** is currently a stated property (and checked by the architecture tests). Events would need a rule of their own.
- **Tracing and perf tooling.** `/demo/trace`, `CallTracer` and `perf.sh` record `@RpcApi` calls; an event hand-off would need the same.

## Options

| Option | Effect | Cost / risk |
| --- | --- | --- |
| A. Keep as is | Nothing changes | None; hand-built outbox stays |
| B. Registry replaces `OutboxRelay` | Less code, standard behaviour | Per-module schema and transaction manager wiring; retry/backoff semantics differ |
| C. Events for notifications (+ externalization if split) | Orders no longer depends on notifications | Needs a broker or RPC-delivered events in split modes |
| D. Parallel reads in the saga (futures / virtual threads) | Real latency win on the critical path | Context propagation (MDC, trace depth, transactions); not Modulith |
| E. Choreography | No orchestrator | Hardest to reason about and to compensate; loses request/reply semantics |

## Questions for the discussion

1. Is the goal reliability and less code (B), decoupling (C), or latency (D)? They need different work.
2. Do we accept a broker for split modes, or must the demo stay PostgreSQL-only?
3. Should events be a second, explicit channel next to `@RpcApi`, and what stops it growing into hidden coupling?
4. Is the explicit saga a feature we want to keep? (I would keep it.)
5. Does each module keep owning its own event table and roles?

## Suggested spike

On this branch: switch only the notification hand-off to an `@ApplicationModuleListener` with the JDBC registry in `modulith` and
`macro-monolith` modes, behind a property, keep `OutboxRelay` for the split modes, and compare with `scripts/perf.sh --modes`
(including the failure behaviour when notifications is down). Separately, prototype D on `pricing.quote` + `shipping.quote` and measure.
