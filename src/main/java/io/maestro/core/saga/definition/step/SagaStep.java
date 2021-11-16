package io.maestro.core.saga.definition.step;

import io.maestro.common.saga.instance.SagaInstance;

public interface SagaStep<Data> {
    StepOutcome<Data> execute(SagaInstance sagaInstance, Data data);
}
