package io.maestro.core.saga.definition.step;

import io.maestro.core.instance.SagaExecutionState;
import io.maestro.core.instance.SagaInstance;
import io.maestro.core.instance.SagaState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class LocalStepTests {

    private boolean actionExecuted = false;
    private boolean compensationExecuted = false;
    private boolean exceptionHandlerExecuted = false;

    @Test
    public void executeStep_whenSagaIsExecuting_ShouldExecuteAction(){
        //given
        LocalStep<TestSagaData> localStep
                = new LocalStep<>(this::localActionToExecute, Optional.empty(), new HashMap<>());
        SagaExecutionState sagaExecutionState = new SagaExecutionState(1, SagaState.EXECUTING);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        //when
        StepOutcome<TestSagaData> stepOutcome = localStep.execute(sagaInstance, new TestSagaData());
        //then
        assertTrue(stepOutcome.isSuccessful());
        assertTrue(actionExecuted);
    }

    @Test
    public void executeStep_whenSagaIsCompensating_ShouldExecuteCompensation(){
        //given
        LocalStep<TestSagaData> localStep
                = new LocalStep<>(this::localActionToExecute, Optional.of(this::localCompensationToExecute),
                                  new HashMap<>());
        SagaExecutionState sagaExecutionState = new SagaExecutionState(1, SagaState.COMPENSATING);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        //when
        StepOutcome<TestSagaData> stepOutcome = localStep.execute(sagaInstance, new TestSagaData());
        //then
        assertTrue(stepOutcome.isSuccessful());
        assertTrue(compensationExecuted);
    }

    @Test
    public void executeStep_whenActionThrowsAnException_ShouldExecuteExceptionHandler(){
        //given
        Map<String, Consumer<TestSagaData>> exceptionHandlers = new HashMap<>();
        exceptionHandlers.put(FirstException.class.getName(), this::firstExceptionHandler);
        exceptionHandlers.put(SecondException.class.getName(), this::secondExceptionHandler);
        LocalStep<TestSagaData> localStep
                = new LocalStep<>(this::localActionThrowsException, Optional.of(this::localCompensationToExecute),
                                  exceptionHandlers);
        SagaExecutionState sagaExecutionState = new SagaExecutionState(1, SagaState.EXECUTING);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        //when
        StepOutcome<TestSagaData> stepOutcome = localStep.execute(sagaInstance, new TestSagaData());
        //then
        assertFalse(stepOutcome.isSuccessful());
        assertTrue(exceptionHandlerExecuted);
    }

    protected static class TestSagaData {}

    public void localActionToExecute(TestSagaData testSagaData){
        actionExecuted = true;
    }

    public void localActionThrowsException(TestSagaData testSagaData) {
        throw new FirstException();
    }

    public void firstExceptionHandler(TestSagaData testSagaData) {
        exceptionHandlerExecuted = true;
    }

    public void secondExceptionHandler(TestSagaData testSagaData) {}

    public void localCompensationToExecute(TestSagaData testSagaData){
        compensationExecuted = true;
    }

    private static class FirstException extends RuntimeException {}
    private static class SecondException extends RuntimeException {}
}
