package io.maestro.core.saga;

import io.maestro.core.dsl.SagaDefinitionDsl;
import io.maestro.core.exception.InconsistentSagaStateException;
import io.maestro.core.instance.SagaExecutionState;
import io.maestro.core.instance.SagaInstance;
import io.maestro.core.instance.SagaSerializedData;
import io.maestro.core.instance.SagaState;
import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SagaTests implements SagaDefinitionDsl<SagaTests.TestSagaData> {

    private final SagaSerializedData sagaSerializedData
            = new SagaSerializedData(TestSagaData.class.getName(), "test");
    private final SagaInstance sagaInstance
            = new SagaInstance
            ("id", "test-saga", new SagaExecutionState(3, SagaState.EXECUTING),
             sagaSerializedData);

    @Mock
    private final SagaDefinition<TestSagaData> definition = step().invokeLocalParticipant(this::localParticipantAction)
                                                                  .build();

    @Mock
    private final RemoteStep<TestSagaData> remoteStep = new RemoteStepImpl<>();

    @Test
    public void getNextSteps_ShouldCallRightMethods() {
        //given
        Saga<TestSagaData> testSaga = new TestSaga(definition);
        //when
        when(definition.getNextSteps(sagaInstance)).thenReturn(new ArrayList<>());
        List<SagaStep<TestSagaData>> nextSteps = testSaga.getNextSteps(sagaInstance);
        //then
        assertEquals(0, nextSteps.size());
        verify(definition, times(1)).getNextSteps(sagaInstance);
    }

    @Test
    public void getStepToCompensate_ShouldCallRightMethods() {
        //given
        Saga<TestSagaData> testSaga = new TestSaga(definition);
        //when
        when(definition.getStepsToCompensate(sagaInstance)).thenReturn(new ArrayList<>());
        List<SagaStep<TestSagaData>> stepsToCompensate = testSaga.getStepsToCompensate(sagaInstance);
        //then
        assertEquals(0, stepsToCompensate.size());
        verify(definition, times(1)).getStepsToCompensate(sagaInstance);
    }

    @Test
    public void handleReply_ShouldHandleReplyCorrectly() {
        //given
        TestSagaData sagaData = new TestSagaData();
        Saga<TestSagaData> testSaga = new TestSaga(definition);
        StepOutcome<TestSagaData> successfulStepOutcome = new RemoteStepOutcome<>(true);
        //when
        when(definition.getStepInExecution(sagaInstance)).thenReturn(remoteStep);
        when(remoteStep.handleReply(sagaInstance, sagaData, null)).thenReturn(successfulStepOutcome);
        StepOutcome<TestSagaData> stepOutcome = testSaga.handleReply(sagaInstance, sagaData, null);
        //then
        assertEquals(successfulStepOutcome, stepOutcome);
        verify(definition, times(1)).getStepInExecution(sagaInstance);
        verify(remoteStep, times(1)).handleReply(sagaInstance, sagaData, null);

    }

    @Test
    public void handleReply_whenALocalStepIsTheCurrentStep_ShouldThrowsInconsistentSagaStateException() {
        //given
        TestSagaData sagaData = new TestSagaData();
        Saga<TestSagaData> testSaga = new TestSaga(definition);
        LocalStep<TestSagaData> localStep = new LocalStep<>();
        //when
        when(definition.getStepInExecution(sagaInstance)).thenReturn(localStep);

        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class,
                               () -> testSaga.handleReply(sagaInstance, sagaData, null));
        //then
        assertEquals("Can't handle reply for local step", exception.getMessage());
        verify(definition, times(1)).getStepInExecution(sagaInstance);
        verify(remoteStep, times(0)).handleReply(sagaInstance, sagaData, null);

    }

    private static class TestSaga extends Saga<TestSagaData> {
        public TestSaga(SagaDefinition<TestSagaData> definition) {
            super.setDefinition(definition);
        }
    }

    protected static class TestSagaData {}

    public void localParticipantAction(TestSagaData data){}

}
