package io.maestro.core.saga;

import io.maestro.core.instance.SagaState;
import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.RemoteStep;
import io.maestro.core.saga.definition.step.SagaStep;
import io.maestro.core.saga.definition.step.StepOutcome;
import io.maestro.core.dsl.SagaDefinitionDsl;
import io.maestro.core.exception.InconsistentSagaStateException;
import io.maestro.core.instance.SagaInstance;
import io.maestro.core.reply.Message;

import java.util.List;

public abstract class Saga <Data> implements SagaDefinitionDsl<Data> {
    private String sagaType;
    private SagaDefinition<Data> definition;

    public String getSagaType
            (){
        return sagaType;
    }

    public void setSagaType
            (String sagaType) {
        this.sagaType = sagaType;
    }

    public SagaDefinition<Data> getDefinition
            () {
        return definition;
    }

    public void setDefinition
            (SagaDefinition<Data> definition) {
        this.definition = definition;
    }

    public StepOutcome<Data> handleReply
            (SagaInstance sagaInstance, Data sagaData, Message message){
        SagaStep<Data> stepInExecution = definition.getStepInExecution(sagaInstance);
        if(!(stepInExecution instanceof RemoteStep)){
            throw new InconsistentSagaStateException
                    ("Can't handle reply for local step");
        }
        RemoteStep<Data> remoteStep = (RemoteStep<Data>)stepInExecution;
        return remoteStep.handleReply(sagaInstance, sagaData, message);
    }

    public List<SagaStep<Data>> getNextSteps
            (SagaInstance sagaInstance) {
        if(!SagaState.EXECUTING.equals(sagaInstance.getSagaExecutionState().getState())
                || !SagaState.CREATED.equals(sagaInstance.getSagaExecutionState().getState())){
            throw new InconsistentSagaStateException
                    ("Can't get next steps, saga is not in execution state!");
        }
        return definition.getNextSteps(sagaInstance);
    }

    public List<SagaStep<Data>> getStepsToCompensate
            (SagaInstance sagaInstance){
        if(!SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
            throw new InconsistentSagaStateException
                    ("Can't get steps to compensate, saga is not in compensation state!");
        }
        return definition.getStepsToCompensate(sagaInstance);
    }

    public int getSagaSize(){
        return this.definition.getSize();
    }
}
