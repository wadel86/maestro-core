package io.maestro.core.instance;

import io.maestro.core.exception.InconsistentSagaStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SagaExecutionStateTests {

    @Test
    public void whenInitializeSagaExecutionState_ThenStateShouldCorrectlyInitialized(){
        //when
        SagaExecutionState sagaExecutionState = SagaExecutionState.initialize();
        //then
        assertNotNull(sagaExecutionState);
        assertEquals(SagaState.CREATED, sagaExecutionState.getState());
        assertEquals(-1, sagaExecutionState.getPointer());
    }

    @Test
    public void whePutCreatedSagaStateInStartMode_ThenStateShouldBeInStartedMode(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(-1, SagaState.CREATED);
        //when
        sagaExecutionState.putInStartMode();
        //then
        assertEquals(SagaState.EXECUTING, sagaExecutionState.getState());
        assertEquals(0, sagaExecutionState.getPointer());
    }

    @Test
    public void whenPutExecutingSagaStateInStartMode_ThenShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.EXECUTING);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::putInStartMode);
        //then
        assertEquals("In order to be started, a saga must be in CREATED state", exception.getMessage());
    }

    @Test
    public void whenPutCreatedSagaStateInTerminateMode_ThenStateShouldBeInTerminatedMode(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(2, SagaState.EXECUTING);
        //when
        sagaExecutionState.putInTerminateMode();
        //then
        assertEquals(SagaState.TERMINATED, sagaExecutionState.getState());
        assertEquals(2, sagaExecutionState.getPointer());
    }

   @Test
        public void whenPutCreatedSagaStateInTerminateMode_ThenShouldExpectInconsistentStateException(){
            //given
            SagaExecutionState sagaExecutionState
                    = new SagaExecutionState(0, SagaState.CREATED);
            //when
            InconsistentSagaStateException exception
                    = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::putInTerminateMode);
            //then
            assertEquals("In order to be terminated, a saga must be in EXECUTING or COMPENSATION state",
                         exception.getMessage());
    }

    @Test
    public void whenStepUpAnExecutingSaga_ThenPointerShouldPointToTheNextStep(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(2, SagaState.EXECUTING);
        //when
        sagaExecutionState.stepUp();
        //then
        assertEquals(SagaState.EXECUTING, sagaExecutionState.getState());
        assertEquals(3, sagaExecutionState.getPointer());
    }

    @Test
    public void whenStepUpACompensatingSaga_ThenShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.COMPENSATING);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::stepUp);
        //then
        assertEquals("Can't step up a non executing saga", exception.getMessage());
    }

    @Test
    public void whenStepUpACreatedSaga_ThenShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.CREATED);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::stepUp);
        //then
        assertEquals("Can't step up a non executing saga", exception.getMessage());
    }

    @Test
    public void whenStepUpATerminatedSaga_ThenShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.TERMINATED);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::stepUp);
        //then
        assertEquals("Can't step up a non executing saga", exception.getMessage());
    }

    @Test
    public void whenReverseAnExecutingSaga_ShouldExpectAConsistentState(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(2, SagaState.EXECUTING);
        //when
        sagaExecutionState.reverseToCompensation();
        //then
        assertEquals(SagaState.COMPENSATING, sagaExecutionState.getState());
        assertEquals(1, sagaExecutionState.getPointer());
    }

    @Test
    public void whenReverseACompensatingSaga_ShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(2, SagaState.COMPENSATING);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::reverseToCompensation);
        //then
        assertEquals("Can't reverse a non executing saga", exception.getMessage());
    }

    @Test
    public void whenReverseACreatedSaga_ShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.CREATED);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::reverseToCompensation);
        //then
        assertEquals("Can't reverse a non executing saga", exception.getMessage());
    }

    @Test
    public void whenReverseATerminatedSaga_ShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(2, SagaState.TERMINATED);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::reverseToCompensation);
        //then
        assertEquals("Can't reverse a non executing saga", exception.getMessage());
    }

    @Test
    public void whenStepDownACompensatingSaga_ThenPointerShouldPointToThePreviousStep(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(2, SagaState.COMPENSATING);
        //when
        sagaExecutionState.stepDown();
        //then
        assertEquals(SagaState.COMPENSATING, sagaExecutionState.getState());
        assertEquals(1, sagaExecutionState.getPointer());
    }

    @Test
    public void whenStepDownAnExecutingSaga_ThenShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.EXECUTING);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::stepDown);
        //then
        assertEquals("Can't step down a non compensating saga", exception.getMessage());
    }

    @Test
    public void whenStepDownACreatedSaga_ThenShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.CREATED);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::stepDown);
        //then
        assertEquals("Can't step down a non compensating saga", exception.getMessage());
    }

    @Test
    public void whenStepDownATerminatedSaga_ThenShouldExpectInconsistentStateException(){
        //given
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(0, SagaState.TERMINATED);
        //when
        InconsistentSagaStateException exception
                = assertThrows(InconsistentSagaStateException.class, sagaExecutionState::stepDown);
        //then
        assertEquals("Can't step down a non compensating saga", exception.getMessage());
    }
}
