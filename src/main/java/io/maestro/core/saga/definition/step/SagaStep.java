package io.maestro.core.saga.definition.step;

import io.maestro.common.saga.instance.SagaInstance;

public interface SagaStep<D> {
    StepOutcome<D> execute(SagaInstance sagaInstance, D data);
}
