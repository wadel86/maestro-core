package io.maestro.core.instance;

import io.maestro.core.exception.InconsistentSagaStateException;

public class SagaExecutionState {
    private int pointer;
    private SagaState state;

    public SagaExecutionState(int pointer, SagaState state) {
        this.pointer = pointer;
        this.state = state;
    }

    public static SagaExecutionState initialize(){
        return new SagaExecutionState(-1, SagaState.CREATED);
    }

    public void putInStartMode(){
        this.pointer = 0;
        this.state = SagaState.EXECUTING;
    }

    public void putInTerminateMode(){
        this.state = SagaState.TERMINATED;
    }

    public void stepUp(){
        if(SagaState.COMPENSATING.equals(this.state)){
            throw new InconsistentSagaStateException
                    ("Saga is compensating, can't step up");
        }
        this.pointer += 1;
    }

    public void reverseToCompensation(){
        if(SagaState.COMPENSATING.equals(this.state)){
            throw new InconsistentSagaStateException
                    ("Saga is already in compensating state");
        }
        this.pointer -= 1;
        this.state = SagaState.COMPENSATING;
    }

    public void stepDown(){
        if(SagaState.EXECUTING.equals(this.state)){
            throw new InconsistentSagaStateException
                    ("Saga is executing, can't step down");
        }
        this.pointer -= 1;
    }

    public SagaState getState() {
        return state;
    }

    public int getPointer() {
        return pointer;
    }
}
