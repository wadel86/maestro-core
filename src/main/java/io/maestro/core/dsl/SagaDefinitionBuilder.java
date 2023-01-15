package io.maestro.core.dsl;

import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.SagaStep;

import java.util.LinkedList;

public class SagaDefinitionBuilder<D> {
    private LinkedList<SagaStep<D>> steps = new LinkedList<>();

    public SagaDefinitionBuilder() {
    }

    public void addStep(SagaStep<D> step) {
        steps.add(step);
    }

    public SagaDefinition<D> build() {
        return new SagaDefinition<>(steps);
    }

}
