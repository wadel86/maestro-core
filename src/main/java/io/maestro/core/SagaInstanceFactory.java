package io.maestro.core;

import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.core.saga.Saga;

import java.util.concurrent.ConcurrentHashMap;

public class SagaInstanceFactory {
    private ConcurrentHashMap<Saga<?>, SagaManager<?>> sagaManagers;
    private final SagaManagerFactory sagaManagerFactory;

    public SagaInstanceFactory(SagaManagerFactory sagaManagerFactory) {
        this.sagaManagers = new ConcurrentHashMap<>();
        this.sagaManagerFactory = sagaManagerFactory;
    }

    public <Data> SagaInstance createSagaInstance(Saga<Data> saga, Data sagaData) {
        SagaManager sagaManager =  sagaManagers.computeIfAbsent(saga, this::createSagaManager);
        return sagaManager.create(sagaData);
    }

    private <Data> SagaManager<Data> createSagaManager(Saga<Data> saga) {
        return sagaManagerFactory.createSagaManager(saga);
    }
 }
