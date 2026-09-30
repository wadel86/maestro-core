# maestro-core

A saga orchestration engine for Java services that talk to each other over messages.

A saga is a sequence of local transactions across services, where each step has a
compensating action, and failure at step *n* means undoing steps *n-1 … 0* in reverse.
Maestro holds that sequence for you: it persists where each running saga has got to,
dispatches commands to remote participants, routes their replies back, and unwinds the
saga when a step reports failure.

It orchestrates over infrastructure you already run. There is no server to operate — the
engine is a library, and saga state lives in your own database behind a gateway interface.

```
maestro-common      model and ports shared by everything
maestro-core        this module: the engine and the definition DSL
maestro-data-jdbc   persistence with a transactional outbox
maestro/            participant runtime, in-JVM messaging, end-to-end tests
```

## Defining a saga

```java
public class CreateOrderSaga extends Saga<OrderSagaData> {

    public CreateOrderSaga() {
        setSagaType("create-order");
        setDefinition(
            step().invokeLocalParticipant(data -> orders.markPending(data.orderId))
                  .withCompensation(data -> orders.markRejected(data.orderId))
                  .onException(OrderNotFoundException.class, data -> data.failureReason = "gone")

            .step().invokeRemoteParticipant(data ->
                       CommandWithDestination.to("inventory-service", new ReserveStock(data.orderId)))
                   .onReply(StockReserved.class, (data, reply) -> data.reservationId = reply.reservationId)
                   .withRemoteCompensation(data ->
                       CommandWithDestination.to("inventory-service", new ReleaseStock(data.reservationId)))

            .step().invokeLocalParticipant(data -> orders.markConfirmed(data.orderId))
            .build());
    }
}
```

Starting one:

```java
SagaManagerFactory managers = new SagaManagerFactory(sagaDataGateway, replyConsumer);
SagaInstanceFactory sagas = new SagaInstanceFactory(managers, List.of(new CreateOrderSaga()));

SagaInstance instance = sagas.createSagaInstance(createOrderSaga, sagaData);
```

Pass every saga definition to `SagaInstanceFactory` up front. Managers subscribe to their
reply channel when they are created, so a saga type registered lazily on first use is one
whose in-flight replies were being dropped until then.

## The execution model

A running saga is a row: its type, its serialized data, a state, and a **pointer** into
the step list. The pointer has one meaning in both directions — *the index of the step
this saga is currently concerned with*.

| State | Pointer |
| --- | --- |
| `CREATED` | `-1`, nothing has run |
| `EXECUTING` | the step running or awaiting a reply; steps `0 … pointer-1` are done |
| `COMPENSATING` | the next step to undo, counting down |
| `TERMINATED` | wherever it finished: the step count if it completed, `-1` if it unwound |

Both ends of a saga's life sit at `-1`, which is what makes "everything has been undone"
cheap to assert.

Going forward, consecutive local steps are batched and run in one pass — only a remote
step ends the pass, because the saga then has to wait. A five-step saga with one remote
call at position 2 does two passes, not five.

```
create   A  B  R ──command──▶ inventory-service      pointer parked at 2
                                      │
reply    ◀────────────────────────────┘               pointer 2 → 3
         C  D                                         pointer 5 == size → TERMINATED
```

Failure at `C` reverses the pointer and walks back down:

```
         A  B  R  C✗                                  reverseToCompensation → pointer 2
         ◀── undo R ──command──▶ inventory-service     parked at 2, COMPENSATING
         ◀── undo B ◀── undo A                         pointer → -1 → TERMINATED
```

A step that fails is not itself compensated: it did not complete.

## Compensation

Local steps undo with `withCompensation`, which runs in place.

Remote steps have a choice. `withCompensation` is for an undo that is this service's own
business. `withRemoteCompensation` is for the usual case — the participant that did the
work is the one that has to undo it — and makes compensation a full round trip: the saga
sends the compensating command, parks on that step, and unwinds no further until the
participant confirms. A participant that reports it *could not* undo raises
`InconsistentSagaStateException` rather than letting the saga quietly skip past it; that
is a case needing a human, and the saga row records exactly which step it stopped on.

## Ports

Three interfaces in `maestro-common`, implemented by the adapters:

- **`SagaDataGateway`** — persist and load saga instances. Its
  `saveSagaAndSendCommand(saga, command)` must be **atomic**: saving the saga and handing
  off its command cannot come apart. Doing them separately is a dual write, and a crash in
  between either strands a saga waiting on a command that was never sent, or leaves a
  participant acting on one the saga has no record of. Implement it as a transactional
  outbox — saga row and outbox row in one transaction, with a relay publishing from the
  outbox. The engine never publishes anything itself.
- **`CommandProducer`** — publish a command. Driven by that relay, not by the engine.
  It stamps `Saga-ID`, `Saga-Type` and `Saga-Step` (from the instance it is saved beside)
  onto the outgoing message; participants echo them back.
- **`ReplyConsumer`** — deliver replies arriving on `<saga-type>-reply-channel`.

`MessageHeaders` holds the wire contract. A reply needs `Saga-ID`, `Saga-Type`,
`reply-outcome` (`success` or anything else), and `reply-type` naming the payload class so
the matching `onReply` handler gets it. Echoing `Saga-Step` is optional but wanted: it is
how the engine tells a reply it is waiting for from one the broker has redelivered.
Replies for a step already passed, or for a saga that has finished, are dropped.

## Building

```sh
git clone https://github.com/wadel86/maestro-common && (cd maestro-common && mvn install)
mvn verify
```

Java 17 (`maestro-common` targets 11). 59 tests; `mvn verify` writes a JaCoCo report to
`target/site/jacoco`.

## What this does not do

Worth knowing before you reach for it:

- **No timeouts.** There is no timer anywhere in the engine. A participant that never
  replies parks its saga forever. This is the most commonly wanted missing feature.
- **Linear definitions only.** The definition is a list and the pointer is an index into
  it: no branching, no loops, no fan-out, no parallel steps.
- **In-flight sagas do not survive a definition change.** Because the pointer is an index,
  deploying a version that inserts a step leaves every running instance pointing at the
  wrong one, silently. Drain before changing a definition.
- **No optimistic locking.** `SagaDataGateway` carries no version, so two replies for the
  same saga arriving at once can both read, both advance, and one can overwrite the other.
  Serialize replies per saga id, or add a version column before you rely on this under
  concurrency. Retrying a lost update is not simply a matter of re-reading, since re-running
  a reply re-runs local steps that may not be idempotent.
- **No recovery sweep.** Nothing scans for sagas stranded mid-flight. A crash between the
  initial save and the first step leaves an instance in `CREATED` with nothing to resume it.
- **`TERMINATED` does not say how it ended.** Completed and fully-compensated sagas share
  a state; the pointer is what distinguishes them.
- **`reply-type` is resolved with `Class.forName`**, so the sender chooses which class is
  loaded and deserialized. Restrict it to the registered `onReply` types before exposing a
  reply channel to anything untrusted.
- **Exception handlers match the exact class.** `onException(RuntimeException.class, …)`
  will not catch an `IllegalStateException`.

If you need timeouts, branching or safe versioning, a durable-execution runtime
(Temporal, Restate) solves those properly and is the better choice. Maestro's case is that
it adds no infrastructure, rides the message broker you already have, and is small enough
to read in an afternoon — when it misbehaves, the whole state of a saga is four columns in
your own database.
