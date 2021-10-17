package io.maestro.core.saga.definition;

import io.maestro.core.saga.definition.step.LocalStep;
import io.maestro.core.saga.definition.step.SagaStep;
import io.maestro.core.instance.SagaInstance;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SagaDefinition<Data> {

    private final LinkedList<SagaStep<Data>> steps;

    public SagaDefinition(LinkedList<SagaStep<Data>> steps) {
        this.steps = steps;
    }

    public List<SagaStep<Data>> getNextSteps(SagaInstance sagaInstance){
        List<SagaStep<Data>> startingSteps = new ArrayList<>();
        int i = sagaInstance.getSagaExecutionState().getPointer();
        while (i < steps.size()){
            startingSteps.add(steps.get(i));
            if(!(steps.get(i) instanceof LocalStep)){
                break;
            }
            i++;
        }
        return startingSteps;
    }

    public List<SagaStep<Data>> getStepsToCompensate(SagaInstance sagaInstance){
        List<SagaStep<Data>> startingSteps = new ArrayList<>();
        int i = sagaInstance.getSagaExecutionState().getPointer();
        while (i > -1){
            startingSteps.add(steps.get(i));
            if(!(steps.get(i) instanceof LocalStep)){
                break;
            }
            i--;
        }
        return startingSteps;
    }

    public SagaStep<Data> getStepInExecution(SagaInstance sagaInstance){
        return null;
    }


    public int getSize(){
        return this.steps.size();
    }


}
