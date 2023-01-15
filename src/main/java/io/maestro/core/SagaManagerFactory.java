package io.maestro.core;

import io.maestro.common.port.CommandProducer;
import io.maestro.common.port.ReplyConsumer;
import io.maestro.common.port.SagaDataGateway;
import io.maestro.core.saga.Saga;

public class SagaManagerFactory {
    private final SagaDataGateway sagaDataGateway;
    private final CommandProducer commandProducer;
    private final ReplyConsumer replyConsumer;

    public SagaManagerFactory(SagaDataGateway sagaDataGateway, CommandProducer commandProducer,
                              ReplyConsumer replyConsumer) {
        this.sagaDataGateway = sagaDataGateway;
        this.commandProducer = commandProducer;
        this.replyConsumer = replyConsumer;
    }

    public <D> SagaManager<D> createSagaManager(Saga<D> saga) {
        return new SagaManagerImpl<>(sagaDataGateway, commandProducer, replyConsumer, saga);
    }
}
