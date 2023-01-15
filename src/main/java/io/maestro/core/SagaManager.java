package io.maestro.core;

import io.maestro.common.exception.BadSagaTypeException;
import io.maestro.common.saga.instance.SagaInstance;

public interface SagaManager <D> {
    SagaInstance create(D sagaData) throws BadSagaTypeException;
}
