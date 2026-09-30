package io.maestro.core;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.exception.BadSagaTypeException;
import io.maestro.common.exception.InconsistentSagaStateException;
import io.maestro.common.port.ReplyConsumer;
import io.maestro.common.port.SagaDataGateway;
import io.maestro.common.reply.Message;
import io.maestro.common.reply.MessageHandler;
import io.maestro.common.reply.MessageHeaders;
import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaSerializedData;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.saga.Saga;
import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.LocalStep;
import io.maestro.core.saga.definition.step.LocalStepOutcome;
import io.maestro.core.saga.definition.step.SagaStep;
import io.maestro.core.saga.definition.step.StepOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the orchestrator end to end against in-memory adapters, so that what is
 * asserted is the observable behaviour of a saga running: which participant actions
 * fired and in what order, what was handed to the gateway, and where the instance's
 * execution state ended up.
 */
class SagaManagerImplTests {

    /** Everything the saga's steps and the adapters did, in order. */
    private final List<String> trace = new ArrayList<>();

    private FakeSagaDataGateway gateway;
    private FakeReplyConsumer replyConsumer;

    @BeforeEach
    void setUp() {
        trace.clear();
        gateway = new FakeSagaDataGateway(null);
        replyConsumer = new FakeReplyConsumer();
    }

    private <D> SagaManager<D> manage(Saga<D> saga) {
        //the gateway is told the saga type separately because SagaInstance exposes no
        //getter for it, so an adapter cannot read it off the instance it is handed
        gateway = new FakeSagaDataGateway(saga.getSagaType());
        return new SagaManagerFactory(gateway, replyConsumer).createSagaManager(saga);
    }

    // ---------------------------------------------------------------- wiring

    @Test
    void createSagaManager_shouldSubscribeToTheSagaTypesReplyChannel() {
        manage(new LocalOnlySaga());

        assertEquals(List.of("local-only-reply-channel"), replyConsumer.subscribedChannels);
    }

    @Test
    void constructor_whenTheSagaDeclaresNoSagaType_shouldThrowBadSagaType() {
        BadSagaTypeException exception = assertThrows(
                BadSagaTypeException.class,
                () -> manage(new UntypedSaga()));

        assertEquals("A saga must declare a saga type before it can be managed",
                     exception.getMessage());
        assertTrue(replyConsumer.subscribedChannels.isEmpty());
    }

    // ------------------------------------------------------- forward execution

    @Test
    void create_whenEveryStepIsLocal_shouldRunThemAllInOrderAndTerminate() {
        SagaInstance sagaInstance = manage(new LocalOnlySaga()).create(new OrderData());

        assertEquals(List.of("A.do", "B.do", "C.do"), participantCalls());
        assertEquals(SagaState.TERMINATED, sagaInstance.getSagaExecutionState().getState());
        assertEquals(3, sagaInstance.getSagaExecutionState().getPointer());
    }

    @Test
    void create_shouldPersistTheSagaBeforeRunningAnyStep() {
        manage(new LocalOnlySaga()).create(new OrderData());

        List<String> afterSubscribe = trace.subList(1, trace.size());
        assertEquals("save(CREATED,-1)", afterSubscribe.get(0));
        assertEquals("A.do", afterSubscribe.get(1));
    }

    @Test
    void create_shouldReturnAnInstanceCarryingTheIdAssignedByTheGateway() {
        SagaInstance sagaInstance = manage(new LocalOnlySaga()).create(new OrderData());

        assertNotNull(sagaInstance.getId());
        assertEquals("saga-1", sagaInstance.getId());
    }

    @Test
    void create_shouldPersistSagaDataMutatedByTheSteps() {
        manage(new LocalOnlySaga()).create(new OrderData());

        assertTrue(gateway.stored.getSerializedData().getJson().contains("\"visited\":3"),
                   "expected the data mutated by all three steps, got: "
                   + gateway.stored.getSerializedData().getJson());
    }

    // --------------------------------------------------------- remote dispatch

    @Test
    void create_whenAStepIsRemote_shouldDispatchItsCommandExactlyOnce() {
        RemoteSaga saga = new RemoteSaga();

        manage(saga).create(new OrderData());

        assertEquals(List.of("A.do", "R.invoke"), participantCalls());
        assertEquals(1, gateway.sentCommands.size());
        assertSame(saga.command, gateway.sentCommands.get(0),
                   "the gateway must be handed the very command the step produced");
    }

    @Test
    void create_whenAStepIsRemote_shouldParkTheSagaOnThatStepAwaitingTheReply() {
        SagaInstance sagaInstance = manage(new RemoteSaga()).create(new OrderData());

        // the pointer stays on the remote step: it is in flight, not complete
        assertEquals(1, sagaInstance.getSagaExecutionState().getPointer());
        assertEquals(SagaState.EXECUTING, sagaInstance.getSagaExecutionState().getState());
    }

    @Test
    void create_whenAStepIsRemote_shouldNotRunTheStepsBehindIt() {
        manage(new RemoteSaga()).create(new OrderData());

        assertFalse(participantCalls().contains("C.do"));
    }

    // ------------------------------------------------------------ reply routing

    @Test
    void reply_whenTheParticipantSucceeds_shouldRunTheRemainingStepsAndTerminate() {
        manage(new RemoteSaga()).create(new OrderData());

        replyConsumer.deliver(reply("with-remote", MessageHeaders.SUCCESS));

        assertEquals(List.of("A.do", "R.invoke", "C.do"), participantCalls());
        assertEquals(SagaState.TERMINATED, gateway.stored.getSagaExecutionState().getState());
        assertEquals(3, gateway.stored.getSagaExecutionState().getPointer());
    }

    @Test
    void reply_shouldInvokeTheHandlerRegisteredForThatReplyType() {
        manage(new RemoteSaga()).create(new OrderData());

        replyConsumer.deliver(replyOfType("with-remote", MessageHeaders.SUCCESS,
                                          Reply.class.getName(), "{\"detail\":\"shipped\"}"));

        assertTrue(participantCalls().contains("R.onReply(shipped)"), "got: " + participantCalls());
    }

    @Test
    void reply_forAnotherSagaType_shouldBeIgnored() {
        manage(new RemoteSaga()).create(new OrderData());
        List<String> callsBeforeReply = participantCalls();

        replyConsumer.deliver(reply("some-other-saga", MessageHeaders.SUCCESS));

        assertEquals(callsBeforeReply, participantCalls());
        assertEquals(SagaState.EXECUTING, gateway.stored.getSagaExecutionState().getState());
    }

    @Test
    void reply_whenTheParticipantFails_shouldCompensateOnlyTheCompletedStepsInReverse() {
        manage(new RemoteSaga()).create(new OrderData());

        replyConsumer.deliver(reply("with-remote", MessageHeaders.FAILURE));

        // the remote step itself failed, so it has nothing to undo; A does
        assertEquals(List.of("A.do", "R.invoke", "A.undo"), participantCalls());
        assertEquals(SagaState.TERMINATED, gateway.stored.getSagaExecutionState().getState());
        assertEquals(-1, gateway.stored.getSagaExecutionState().getPointer());
    }

    // ------------------------------------------------------------ compensation

    @Test
    void create_whenAStepFails_shouldCompensateEveryCompletedStepInReverseOrder() {
        manage(new FailingLocalSaga()).create(new OrderData());

        assertEquals(List.of("A.do", "B.do", "C.do", "B.undo", "A.undo"), participantCalls());
    }

    @Test
    void create_whenAStepFails_shouldTerminateOnceEverythingIsUndone() {
        SagaInstance sagaInstance = manage(new FailingLocalSaga()).create(new OrderData());

        assertEquals(SagaState.TERMINATED, sagaInstance.getSagaExecutionState().getState());
        assertEquals(-1, sagaInstance.getSagaExecutionState().getPointer(),
                     "a fully compensated saga must land back on the -1 sentinel");
    }

    @Test
    void create_whenTheVeryFirstStepFails_shouldTerminateWithNothingToCompensate() {
        SagaInstance sagaInstance = manage(new FirstStepFailsSaga()).create(new OrderData());

        assertEquals(List.of("A.do"), participantCalls());
        assertEquals(SagaState.TERMINATED, sagaInstance.getSagaExecutionState().getState());
        assertEquals(-1, sagaInstance.getSagaExecutionState().getPointer());
    }

    @Test
    void create_whenAStepFails_shouldNotCompensateTheStepThatFailed() {
        manage(new FailingLocalSaga()).create(new OrderData());

        assertFalse(participantCalls().contains("C.undo"));
    }

    @Test
    void create_whenCompensationItselfFails_shouldThrowInconsistentSagaState() {
        InconsistentSagaStateException exception = assertThrows(
                InconsistentSagaStateException.class,
                () -> manage(new UncompensatableSaga()).create(new OrderData()));

        assertEquals("Compensation failed, nothing to do", exception.getMessage());
    }

    // ------------------------------------------------------------------ helpers

    /** The trace with the adapters' noise filtered out, leaving what the saga did. */
    private List<String> participantCalls() {
        List<String> calls = new ArrayList<>();
        for (String entry : trace) {
            if (!entry.startsWith("save") && !entry.startsWith("subscribe")) {
                calls.add(entry);
            }
        }
        return calls;
    }

    private Message reply(String sagaType, String outcome) {
        Map<String, String> headers = new HashMap<>();
        headers.put(MessageHeaders.SAGA_ID, "saga-1");
        headers.put(MessageHeaders.SAGA_TYPE, sagaType);
        headers.put(MessageHeaders.REPLY_OUTCOME, outcome);
        return new Message(sagaType, headers, "{}");
    }

    private Message replyOfType(String sagaType, String outcome, String replyType, String payload) {
        Map<String, String> headers = new HashMap<>();
        headers.put(MessageHeaders.SAGA_ID, "saga-1");
        headers.put(MessageHeaders.SAGA_TYPE, sagaType);
        headers.put(MessageHeaders.REPLY_OUTCOME, outcome);
        headers.put(MessageHeaders.REPLY_TYPE, replyType);
        return new Message(sagaType, headers, payload);
    }

    // -------------------------------------------------------------------- fakes

    private class FakeSagaDataGateway implements SagaDataGateway {

        private final List<CommandWithDestination> sentCommands = new ArrayList<>();
        private final String sagaType;
        private SagaInstance stored;

        FakeSagaDataGateway(String sagaType) {
            this.sagaType = sagaType;
        }

        @Override
        public SagaInstance saveSaga(SagaInstance saga) {
            SagaInstance toStore = saga.getId() == null
                    ? new SagaInstance("saga-1", sagaType,
                                       saga.getSagaExecutionState(), saga.getSerializedData())
                    : saga;
            this.stored = toStore;
            SagaExecutionState state = toStore.getSagaExecutionState();
            trace.add("save(" + state.getState() + "," + state.getPointer() + ")");
            return toStore;
        }

        @Override
        public SagaInstance findSaga(String sagaId, String sagaType) {
            return stored;
        }

        @Override
        public SagaInstance saveSagaAndSendCommand(SagaInstance saga, CommandWithDestination command) {
            sentCommands.add(command);
            trace.add("saveAndSend");
            return saveSaga(saga);
        }
    }

    private class FakeReplyConsumer implements ReplyConsumer {

        private final List<String> subscribedChannels = new ArrayList<>();
        private final List<MessageHandler> handlers = new ArrayList<>();

        @Override
        public void subscribe(String channelId, MessageHandler messageHandler) {
            subscribedChannels.add(channelId);
            handlers.add(messageHandler);
            trace.add("subscribe(" + channelId + ")");
        }

        /** Pushes a reply through every subscribed handler, as the broker would. */
        void deliver(Message message) {
            for (MessageHandler handler : handlers) {
                handler.accept(message);
            }
        }
    }

    // ------------------------------------------------------------- saga fixtures

    public static class OrderData {
        public int visited;
        public String lastReplyDetail;
    }

    public static class Reply {
        public String detail;
    }

    private class LocalOnlySaga extends Saga<OrderData> {
        LocalOnlySaga() {
            setSagaType("local-only");
            setDefinition(
                step().invokeLocalParticipant(data -> record("A.do", data))
                .step().invokeLocalParticipant(data -> record("B.do", data))
                .step().invokeLocalParticipant(data -> record("C.do", data))
                .build());
        }
    }

    private class RemoteSaga extends Saga<OrderData> {

        private final CommandWithDestination command = CommandWithDestination.to("order-service", "reserve-stock");

        RemoteSaga() {
            setSagaType("with-remote");
            setDefinition(
                step().invokeLocalParticipant(data -> record("A.do", data))
                      .withCompensation(data -> trace.add("A.undo"))
                .step().invokeRemoteParticipant(data -> {
                            record("R.invoke", data);
                            return command;
                        })
                       .onReply(Reply.class, (data, reply) -> {
                            data.lastReplyDetail = reply.detail;
                            trace.add("R.onReply(" + reply.detail + ")");
                        })
                       .withCompensation(data -> trace.add("R.undo"))
                .step().invokeLocalParticipant(data -> record("C.do", data))
                .build());
        }
    }

    private class FailingLocalSaga extends Saga<OrderData> {
        FailingLocalSaga() {
            setSagaType("failing-local");
            setDefinition(
                step().invokeLocalParticipant(data -> record("A.do", data))
                      .withCompensation(data -> trace.add("A.undo"))
                .step().invokeLocalParticipant(data -> record("B.do", data))
                      .withCompensation(data -> trace.add("B.undo"))
                .step().invokeLocalParticipant(data -> {
                            record("C.do", data);
                            throw new IllegalStateException("participant C refused");
                        })
                      .withCompensation(data -> trace.add("C.undo"))
                .build());
        }
    }

    private class FirstStepFailsSaga extends Saga<OrderData> {
        FirstStepFailsSaga() {
            setSagaType("first-step-fails");
            setDefinition(
                step().invokeLocalParticipant(data -> {
                            record("A.do", data);
                            throw new IllegalStateException("participant A refused");
                        })
                      .withCompensation(data -> trace.add("A.undo"))
                .step().invokeLocalParticipant(data -> record("B.do", data))
                .build());
        }
    }

    /**
     * A step that reports an unsuccessful outcome for the phase it is told to fail in.
     *
     * <p>Extends {@link LocalStep} rather than implementing {@link SagaStep} directly
     * for two reasons: {@code SagaDefinition.getNextSteps} only batches steps that are
     * {@code LocalStep} instances, and a {@code LocalStep} compensation can only signal
     * failure by throwing, which propagates instead of reaching the orchestrator's
     * failed-compensation branch.
     */
    private class FailingStep extends LocalStep<OrderData> {

        private final String name;
        private final boolean failWhenCompensating;

        FailingStep(String name, boolean failWhenCompensating) {
            this.name = name;
            this.failWhenCompensating = failWhenCompensating;
        }

        @Override
        public StepOutcome<OrderData> execute(SagaInstance sagaInstance, OrderData data) {
            boolean compensating = SagaState.COMPENSATING
                    .equals(sagaInstance.getSagaExecutionState().getState());
            boolean successful = compensating != failWhenCompensating;
            trace.add(name + (compensating ? ".undo" : ".do") + (successful ? "" : ".fails"));
            return new LocalStepOutcome<>(successful, Optional.empty());
        }
    }

    /** Runs forward fine, then refuses to be undone. */
    private class UncompensatableSaga extends Saga<OrderData> {
        UncompensatableSaga() {
            setSagaType("uncompensatable");
            List<SagaStep<OrderData>> steps = new ArrayList<>();
            steps.add(new FailingStep("A", true));
            steps.add(new FailingStep("B", false));
            setDefinition(new SagaDefinition<>(steps));
        }
    }

    private static class UntypedSaga extends Saga<OrderData> {
        UntypedSaga() {
            setDefinition(step().invokeLocalParticipant(data -> {
            }).build());
        }
    }

    private void record(String call, OrderData data) {
        trace.add(call);
        data.visited++;
    }

    @Test
    void fakeGateway_shouldHandBackTheSameInstanceOnceItHasAnId() {
        // guards the fixture itself: the reply path relies on findSaga returning the
        // instance the manager has been mutating
        SagaInstance sagaInstance = manage(new RemoteSaga()).create(new OrderData());

        assertSame(sagaInstance, gateway.findSaga("saga-1", "with-remote"));
    }
}
