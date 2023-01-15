package io.maestro.core.dsl;

public interface SagaDefinitionDsl<D> {
    default StepBuilder<D> step() {
        SagaDefinitionBuilder<D> sagaDefinitionBuilder = new SagaDefinitionBuilder<>();
        return new StepBuilder<>(sagaDefinitionBuilder);
    }
}
