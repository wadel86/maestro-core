package io.maestro.core;

import io.maestro.core.exception.BadSagaTypeException;
import io.maestro.core.instance.SagaInstance;

public interface SagaManager <Data> {
    SagaInstance create(Data sagaData) throws BadSagaTypeException;
}
