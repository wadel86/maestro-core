package io.maestro.core.saga.definition;

import io.maestro.common.exception.InconsistentSagaStateException;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.saga.definition.step.LocalStep;
import io.maestro.core.saga.definition.step.SagaStep;

import java.util.ArrayList;
import java.util.List;

public class SagaDefinition<D> {

    private final List<SagaStep<D>> steps;

    public SagaDefinition(List<SagaStep<D>> steps) {
        this.steps = steps;
    }

    public List<SagaStep<D>> getNextSteps(SagaInstance sagaInstance){
        if(!SagaState.EXECUTING.equals(sagaInstance.getSagaExecutionState().getState())){
            throw new InconsistentSagaStateException
                    ("Can't get next steps of none executing saga!");
        }
        List<SagaStep<D>> nextSteps = new ArrayList<>();
        int nextStepIndex = sagaInstance.getSagaExecutionState().getPointer();
        while (nextStepIndex < steps.size()){
            nextSteps.add(steps.get(nextStepIndex));
            if(!(steps.get(nextStepIndex) instanceof LocalStep)){
                break;
            }
            nextStepIndex++;
        }
        return nextSteps;
    }

    public List<SagaStep<D>> getStepsToCompensate(SagaInstance sagaInstance){
        if(!SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
            throw new InconsistentSagaStateException
                    ("Can't get steps to compensate of a non compensating saga!");
        }
        List<SagaStep<D>> startingSteps = new ArrayList<>();
        int previousStepIndex = sagaInstance.getSagaExecutionState().getPointer() - 1;
        while (previousStepIndex > -1){
            startingSteps.add(steps.get(previousStepIndex));
            previousStepIndex--;
        }
        return startingSteps;
    }

    public SagaStep<D> getStepInExecution(SagaInstance sagaInstance){
        if(!SagaState.EXECUTING.equals(sagaInstance.getSagaExecutionState().getState())){
            if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState()))
            {
                throw new InconsistentSagaStateException
                        ("Saga is compensating, step by step execution is not allowed!");
            }
            throw new InconsistentSagaStateException
                    ("Saga is not started yet, can't get step in execution!");
        }
        return steps.get(sagaInstance.getSagaExecutionState().getPointer());
    }


    public int getSize(){
        return this.steps.size();
    }


}
