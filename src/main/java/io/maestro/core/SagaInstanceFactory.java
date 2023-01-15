package io.maestro.core;

import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.core.saga.Saga;

import java.util.concurrent.ConcurrentHashMap;

public class SagaInstanceFactory {
    private final ConcurrentHashMap<Saga<?>, SagaManager<?>> sagaManagers;
    private final SagaManagerFactory sagaManagerFactory;

    public SagaInstanceFactory(SagaManagerFactory sagaManagerFactory) {
        this.sagaManagers = new ConcurrentHashMap<>();
        this.sagaManagerFactory = sagaManagerFactory;
    }

    public <D> SagaInstance createSagaInstance(Saga<D> saga, D sagaData) {
        SagaManager sagaManager =  sagaManagers.computeIfAbsent(saga, this::createSagaManager);
        return sagaManager.create(sagaData);
    }

    private <D> SagaManager<D> createSagaManager(Saga<D> saga) {
        return sagaManagerFactory.createSagaManager(saga);
    }
 }
