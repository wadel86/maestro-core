package io.maestro.core.saga.definition;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.exception.InconsistentSagaStateException;
import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaSerializedData;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.dsl.SagaDefinitionDsl;

import io.maestro.core.saga.definition.step.LocalStep;
import io.maestro.core.saga.definition.step.RemoteStep;
import io.maestro.core.saga.definition.step.SagaStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SagaDefinitionTests implements SagaDefinitionDsl<SagaDefinitionTests.SagaData> {

    private final SagaDefinition<SagaData> sagaDefinition
            = step().invokeLocalParticipant(this::localParticipantAction)
                    .step()
                    .invokeLocalParticipant(this::localParticipantAction)
                    .step()
                    .invokeRemoteParticipant(this::remoteParticipantAction)
                    .step()
                    .invokeRemoteParticipant(this::remoteParticipantAction)
                    .build();

    @Test
    void getNextSteps_WhenSagaStateIsExecuting_thenShouldReturnsAllLocalStepsAndTheFirstRemoteStep(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(1, SagaState.EXECUTING),
                 sagaSerializedData);
        //when
        List<SagaStep<SagaData>> nextSteps
                = sagaDefinition.getNextSteps(sagaInstance);
        //then
        assertEquals(2, nextSteps.size());
        assertTrue(nextSteps.get(0) instanceof LocalStep);
        assertTrue(nextSteps.get(1) instanceof RemoteStep);
    }

    @Test
    void getNextSteps_whenAllStepsAreExecuted_thenShouldReturnsEmptyStepList(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(4, SagaState.EXECUTING),
                 sagaSerializedData);
        //when
        List<SagaStep<SagaData>> nextSteps
                = sagaDefinition.getNextSteps(sagaInstance);
        //then
        assertEquals(0, nextSteps.size());
    }

    @Test
    void getNextSteps_whenSagaStateIsCreated_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(2, SagaState.CREATED),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, () -> sagaDefinition.getNextSteps(sagaInstance));
        //then
        assertEquals("Can't get next steps of none executing saga!", exception.getMessage());
    }

    @Test
    void getNextSteps_whenSagaStateIsCompensating_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(2, SagaState.COMPENSATING),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, () -> sagaDefinition.getNextSteps(sagaInstance));
        //then
        assertEquals("Can't get next steps of none executing saga!", exception.getMessage());
    }

    @Test
    void getNextSteps_whenSagaStateIsTerminated_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(2, SagaState.TERMINATED),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, () -> sagaDefinition.getNextSteps(sagaInstance));
        //then
        assertEquals("Can't get next steps of none executing saga!", exception.getMessage());
    }

    @Test
    void getNextSteps_whenRemainingStepsAreOnlyRemote_thenShouldReturnsOnlyOneRemoteStep(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(2, SagaState.EXECUTING),
                 sagaSerializedData);
        //when
        List<SagaStep<SagaData>> nextSteps
                = sagaDefinition.getNextSteps(sagaInstance);
        //then
        assertEquals(1, nextSteps.size());
        assertTrue(nextSteps.get(0) instanceof RemoteStep);
    }

    @Test
    void getStepsToCompensate_shouldReturnsAllExecutedSteps(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(3, SagaState.COMPENSATING),
                 sagaSerializedData);
        //when
        List<SagaStep<SagaData>> nextSteps
                = sagaDefinition.getStepsToCompensate(sagaInstance);
        //then
        assertEquals(3, nextSteps.size());
        assertTrue(nextSteps.get(0) instanceof RemoteStep);
        assertTrue(nextSteps.get(1) instanceof LocalStep);
        assertTrue(nextSteps.get(2) instanceof LocalStep);
    }

    @Test
    void getStepsToCompensate_whenSagaIsExecuting_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(3, SagaState.EXECUTING),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class,
                               () -> sagaDefinition.getStepsToCompensate(sagaInstance));
        //then
        assertEquals("Can't get steps to compensate of a non compensating saga!", exception.getMessage());
    }

    @Test
    void getStepsToCompensate_whenSagaIsCreated_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(0, SagaState.CREATED),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class,
                               () -> sagaDefinition.getStepsToCompensate(sagaInstance));
        //then
        assertEquals("Can't get steps to compensate of a non compensating saga!", exception.getMessage());
    }

    @Test
    void getStepsToCompensate_whenSagaIsTerminated_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(0, SagaState.TERMINATED),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class,
                               () -> sagaDefinition.getStepsToCompensate(sagaInstance));
        //then
        assertEquals("Can't get steps to compensate of a non compensating saga!", exception.getMessage());
    }

    @Test
    void getStepInException_whenSagaIsExecuting_thenReturnsTheRightStep(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(2, SagaState.EXECUTING),
                 sagaSerializedData);
        //when
        SagaStep<SagaData> stepInExecution = sagaDefinition.getStepInExecution(sagaInstance);

        //then
        assertTrue(stepInExecution instanceof RemoteStep);
    }

    @Test
    void getStepInException_whenSagaIsCreated_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(0, SagaState.CREATED),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class,
                               () -> sagaDefinition.getStepInExecution(sagaInstance));
        //then
        assertEquals("Saga is not started yet, can't get step in execution!", exception.getMessage());
    }

    @Test
    void getStepInException_whenSagaIsCompensating_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(2, SagaState.COMPENSATING),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class,
                               () -> sagaDefinition.getStepInExecution(sagaInstance));
        //then
        assertEquals("Saga is compensating, step by step execution is not allowed!", exception.getMessage());
    }

    @Test
    void getStepInException_whenSagaIsTerminated_thenExpectInconsistentSagaStateException(){
        //given
        SagaSerializedData sagaSerializedData = new SagaSerializedData(SagaData.class.getName(), "test");
        SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(3, SagaState.TERMINATED),
                 sagaSerializedData);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class,
                               () -> sagaDefinition.getStepInExecution(sagaInstance));
        //then
        assertEquals("Saga is not started yet, can't get step in execution!", exception.getMessage());
    }

    private void localParticipantAction(SagaData data){}
    private CommandWithDestination remoteParticipantAction(SagaData data){return null;}
    static class SagaData{}

}
