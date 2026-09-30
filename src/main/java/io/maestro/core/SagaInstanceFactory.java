package io.maestro.core;

import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.core.saga.Saga;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entry point for starting sagas: holds one {@link SagaManager} per saga definition.
 *
 * <p>Prefer the constructor that takes the application's saga definitions. Managers
 * subscribe to their reply channel when they are created, so a manager that only comes
 * into existence on the first {@code createSagaInstance} call is a manager that was not
 * listening for the replies of sagas already in flight &mdash; after a restart, those
 * replies would be dropped until something happened to start a new saga of that type.
 */
public class SagaInstanceFactory {

    private final ConcurrentHashMap<Saga<?>, SagaManager<?>> sagaManagers;
    private final SagaManagerFactory sagaManagerFactory;

    /**
     * Registers every saga up front, so all reply channels are subscribed before any
     * saga runs.
     */
    public SagaInstanceFactory(SagaManagerFactory sagaManagerFactory,
                               Collection<? extends Saga<?>> sagas) {
        this.sagaManagers = new ConcurrentHashMap<>();
        this.sagaManagerFactory = sagaManagerFactory;
        for (Saga<?> saga : sagas) {
            this.sagaManagers.computeIfAbsent(saga, this::createSagaManager);
        }
    }

    /**
     * Registers sagas lazily, on first use.
     *
     * @deprecated replies for in-flight sagas are dropped until the saga type is first
     *             used; pass the application's saga definitions to
     *             {@link #SagaInstanceFactory(SagaManagerFactory, Collection)} instead.
     */
    @Deprecated
    public SagaInstanceFactory(SagaManagerFactory sagaManagerFactory) {
        this(sagaManagerFactory, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public <D> SagaInstance createSagaInstance(Saga<D> saga, D sagaData) {
        SagaManager<D> sagaManager
                = (SagaManager<D>) sagaManagers.computeIfAbsent(saga, this::createSagaManager);
        return sagaManager.create(sagaData);
    }

    private SagaManager<?> createSagaManager(Saga<?> saga) {
        return sagaManagerFactory.createSagaManager(saga);
    }
 }
