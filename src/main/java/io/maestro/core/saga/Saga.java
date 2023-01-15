package io.maestro.core.saga;

import io.maestro.common.exception.InconsistentSagaStateException;
import io.maestro.common.reply.Message;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.core.dsl.SagaDefinitionDsl;
import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.RemoteStep;
import io.maestro.core.saga.definition.step.SagaStep;
import io.maestro.core.saga.definition.step.StepOutcome;

import java.util.List;

public abstract class Saga <D> implements SagaDefinitionDsl<D> {
    private String sagaType;
    private SagaDefinition<D> definition;

    public String getSagaType
            (){
        return sagaType;
    }

    public void setSagaType
            (String sagaType){
        this.sagaType = sagaType;
    }

    public SagaDefinition<D> getDefinition
            (){
        return definition;
    }

    public void setDefinition
            (SagaDefinition<D> definition){
        this.definition = definition;
    }

    public List<SagaStep<D>> getNextSteps
            (SagaInstance sagaInstance) {
        return definition.getNextSteps(sagaInstance);
    }

    public List<SagaStep<D>> getStepsToCompensate
            (SagaInstance sagaInstance) {
        return definition.getStepsToCompensate(sagaInstance);
    }

    public StepOutcome<D> handleReply
            (SagaInstance sagaInstance, D sagaData, Message message){
        SagaStep<D> stepInExecution = definition.getStepInExecution(sagaInstance);
        if(!(stepInExecution instanceof RemoteStep)){
            throw new InconsistentSagaStateException
                    ("Can't handle reply for local step");
        }
        RemoteStep<D> remoteStep = (RemoteStep<D>)stepInExecution;
        return remoteStep.handleReply(sagaInstance, sagaData, message);
    }

    public int getSagaSize(){
        return this.definition.getSize();
    }
}
