package io.maestro.core.dsl;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.saga.Saga;
import io.maestro.core.saga.definition.step.LocalStep;
import io.maestro.core.saga.definition.step.RemoteStep;
import io.maestro.core.saga.definition.step.SagaStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the saga in README.md honest. It is the shape a newcomer copies first, so if the
 * DSL moves under it this fails rather than the README quietly going stale.
 */
class ReadmeExampleTests {

    static class OrderSagaData {
        public String orderId;
        public String reservationId;
        public String failureReason;
    }

    public static class StockReserved {
        public String reservationId;
    }

    static class ReserveStock {
        ReserveStock(String orderId) {
        }
    }

    static class ReleaseStock {
        ReleaseStock(String reservationId) {
        }
    }

    static class OrderNotFoundException extends RuntimeException {
    }

    /** A stand-in for the README's injected repository. */
    static class Orders {
        void markPending(String orderId) {
        }

        void markRejected(String orderId) {
        }

        void markConfirmed(String orderId) {
        }
    }

    static class CreateOrderSaga extends Saga<OrderSagaData> {

        private final Orders orders = new Orders();

        CreateOrderSaga() {
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

    @Test
    void theDocumentedSaga_shouldBuildThreeStepsInTheDocumentedOrder() {
        CreateOrderSaga saga = new CreateOrderSaga();

        assertEquals("create-order", saga.getSagaType());
        assertEquals(3, saga.getSagaSize());
    }

    @Test
    void theDocumentedSaga_shouldStopItsFirstPassOnTheRemoteStep() {
        CreateOrderSaga saga = new CreateOrderSaga();
        SagaInstance justStarted = new SagaInstance(
                "id", "create-order", new SagaExecutionState(0, SagaState.EXECUTING), null);

        List<SagaStep<OrderSagaData>> firstPass = saga.getNextSteps(justStarted);

        //the local step runs now, the remote one ends the pass
        assertEquals(2, firstPass.size());
        assertTrue(firstPass.get(0) instanceof LocalStep);
        assertTrue(firstPass.get(1) instanceof RemoteStep);
    }
}
