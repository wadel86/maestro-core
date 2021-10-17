package io.maestro.core.instance;

import io.maestro.core.exception.InconsistentSagaStateException;

public class SagaInstance {
    private String id;
    private String sagaType;
    private SagaExecutionState sagaExecutionState;
    private SagaSerializedData serializedData;

    public SagaInstance(String id, String sagaType, SagaExecutionState sagaExecutionState, SagaSerializedData serializedData){
        this.id = id;
        this.sagaType = sagaType;
        this.sagaExecutionState = sagaExecutionState;
        this.serializedData = serializedData;
    }

    public String getId() {
        return id;
    }

    public SagaSerializedData getSerializedData() {
        return serializedData;
    }

    public void setSerializedData(SagaSerializedData serializedData) {
        this.serializedData = serializedData;
    }

    public SagaExecutionState getSagaExecutionState() {
        return sagaExecutionState;
    }

    public void start(){
        if(sagaExecutionState.getPointer() != -1){
            throw new InconsistentSagaStateException
                    ("Can't start an already started saga!");
        }
        this.sagaExecutionState.putInStartMode();
    }

    public void terminate(){
        if(!SagaState.EXECUTING.equals(sagaExecutionState.getState())
                && !SagaState.COMPENSATING.equals(sagaExecutionState.getState())){
            throw new InconsistentSagaStateException
                    ("Can't terminates a non started saga!");
        }
        this.sagaExecutionState.putInTerminateMode();
    }

    public void reverseToCompensationState(){
        this.sagaExecutionState.reverseToCompensation();
    }

    public void stepUp(){
        this.sagaExecutionState.stepUp();
    }

    public void stepDown(){
       this.sagaExecutionState.stepDown();
    }
}
