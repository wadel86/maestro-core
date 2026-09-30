package io.maestro.core;

import io.maestro.common.port.ReplyConsumer;
import io.maestro.common.port.SagaDataGateway;
import io.maestro.core.saga.Saga;

/**
 * Builds a ready-to-use {@link SagaManager} for a saga definition: the returned manager
 * is already subscribed to its saga type's reply channel.
 */
public class SagaManagerFactory {

    private final SagaDataGateway sagaDataGateway;
    private final ReplyConsumer replyConsumer;

    public SagaManagerFactory(SagaDataGateway sagaDataGateway, ReplyConsumer replyConsumer) {
        this.sagaDataGateway = sagaDataGateway;
        this.replyConsumer = replyConsumer;
    }

    public <D> SagaManager<D> createSagaManager(Saga<D> saga) {
        SagaManagerImpl<D> sagaManager
                = new SagaManagerImpl<>(sagaDataGateway, replyConsumer, saga);
        sagaManager.subscribeToReplyChannel();
        return sagaManager;
    }
}
