package io.maestro.core.ports;

import io.maestro.core.instance.SagaInstance;

public interface SagaDataGateway {
    SagaInstance saveSaga(SagaInstance saga);
    SagaInstance findSaga(String sagaId, String sagaType);
}
