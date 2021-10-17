package io.maestro.core.dsl;

import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.SagaStep;

import java.util.LinkedList;

public class SagaDefinitionBuilder<Data> {
    private LinkedList<SagaStep<Data>> steps = new LinkedList<>();

    public SagaDefinitionBuilder() {
    }

    public void addStep(SagaStep<Data> step) {
        steps.add(step);
    }

    public SagaDefinition<Data> build() {
        return new SagaDefinition<Data>(steps);
    }

}
