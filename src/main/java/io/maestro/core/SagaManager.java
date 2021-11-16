package io.maestro.core;

import io.maestro.common.exception.BadSagaTypeException;
import io.maestro.common.saga.instance.SagaInstance;

public interface SagaManager <Data> {
    SagaInstance create(Data sagaData) throws BadSagaTypeException;
}
