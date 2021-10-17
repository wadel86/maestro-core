package io.maestro.core.dsl;

public interface SagaDefinitionDsl<Data> {
    default StepBuilder<Data> step() {
        SagaDefinitionBuilder<Data> sagaDefinitionBuilder = new SagaDefinitionBuilder<Data>();
        return new StepBuilder<>(sagaDefinitionBuilder);
    }
}
