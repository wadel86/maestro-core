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
        if(!SagaState.CREATED.equals(this.state)){
            throw new InconsistentSagaStateException
                    ("In order to be started, a saga must be in CREATED state");
        }
        this.pointer = 0;
        this.state = SagaState.EXECUTING;
    }

    public void putInTerminateMode(){
        if(SagaState.CREATED.equals(this.state)
                || SagaState.TERMINATED.equals(this.state) ){
            throw new InconsistentSagaStateException
                    ("In order to be terminated, a saga must be in EXECUTING or COMPENSATION state");
        }
        this.state = SagaState.TERMINATED;
    }

    public void stepUp(){
        if(!SagaState.EXECUTING.equals(this.state)){
            throw new InconsistentSagaStateException
                    ("Can't step up a non executing saga");
        }
        this.pointer += 1;
    }

    public void reverseToCompensation(){
        if(!SagaState.EXECUTING.equals(this.state)){
            throw new InconsistentSagaStateException
                    ("Can't reverse a non executing saga");
        }
        this.pointer -= 1;
        this.state = SagaState.COMPENSATING;
    }

    public void stepDown(){
        if(!SagaState.COMPENSATING.equals(this.state)){
            throw new InconsistentSagaStateException
                    ("Can't step down a non compensating saga");
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
