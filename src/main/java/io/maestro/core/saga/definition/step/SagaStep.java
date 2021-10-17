package io.maestro.core.saga.definition.step;

import io.maestro.core.instance.SagaInstance;

public interface SagaStep<Data> {
    StepOutcome<Data> execute(SagaInstance sagaInstance, Data data);
}
